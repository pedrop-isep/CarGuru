package pt.carguru.Services;

import pt.carguru.Models.Reserva;
import pt.carguru.Models.User;
import pt.carguru.Models.Veiculo;
import pt.carguru.Repositories.IndisponibilidadeRepository;
import pt.carguru.Repositories.ReservaRepository;
import pt.carguru.Repositories.UserRepository;
import pt.carguru.Repositories.VeiculoRepository;
import pt.carguru.Utils.Session;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservaService {
    private final ReservaRepository reservaRepo = new ReservaRepository();
    private final VeiculoRepository veiculoRepo = new VeiculoRepository();
    private final UserRepository userRepo = new UserRepository();
    private final IndisponibilidadeRepository indispRepo = new IndisponibilidadeRepository();

    /** CAR-8: Pedido de reserva */
    public Reserva criarReserva(int veiculoId, LocalDate inicio, LocalDate fim) throws SQLException {
        if (inicio == null || fim == null || !fim.isAfter(inicio))
            throw new IllegalArgumentException("Datas inválidas.");
        if (inicio.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("A data de início não pode ser no passado.");

        Veiculo v = veiculoRepo.findById(veiculoId).orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado."));
        if (!"aprovado".equals(v.getEstado())) throw new IllegalArgumentException("Veículo não disponível.");

        User locatario = Session.getUser();
        if (v.getProprietarioId() == locatario.getId()) throw new IllegalArgumentException("Não podes reservar o teu próprio veículo.");

        // CAR-12: Validação de sobreposição
        if (reservaRepo.existeSobreposicao(veiculoId, inicio, fim))
            throw new IllegalArgumentException("O veículo já tem uma reserva nesse período.");
        if (indispRepo.estaIndisponivel(veiculoId, inicio, fim))
            throw new IllegalArgumentException("O veículo está marcado como indisponível nesse período.");

        long dias = ChronoUnit.DAYS.between(inicio, fim);
        double total = dias * v.getPrecoPorDia();

        if (locatario.getSaldo() < total)
            throw new IllegalArgumentException(String.format("Saldo insuficiente. Precisas de %.2f€.", total));

        // Debitar saldo
        locatario.setSaldo(locatario.getSaldo() - total);
        userRepo.update(locatario);
        Session.setUser(locatario);

        return reservaRepo.save(new Reserva(veiculoId, locatario.getId(), v.getProprietarioId(), inicio, fim, total));
    }

    /** CAR-10: Aprovação de reserva pelo proprietário */
    public void aprovarReserva(int reservaId) throws SQLException {
        Reserva r = getReservaDoProprietario(reservaId);
        if (!"pendente".equals(r.getEstado())) throw new IllegalArgumentException("Reserva não está pendente.");
        reservaRepo.updateEstado(reservaId, "confirmada");
    }

    /** CAR-10 / CAR-21: Cancelar reserva */
    public void cancelarReserva(int reservaId) throws SQLException {
        Reserva r = reservaRepo.findById(reservaId).orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        User current = Session.getUser();
        boolean isProprietario = r.getProprietarioId() == current.getId();
        boolean isLocatario = r.getLocatarioId() == current.getId();
        if (!isProprietario && !isLocatario && !Session.isAdmin())
            throw new IllegalStateException("Sem permissão para cancelar esta reserva.");
        if ("concluida".equals(r.getEstado())) throw new IllegalArgumentException("Reserva já concluída.");

        reservaRepo.updateEstado(reservaId, "cancelada");

        // Devolver saldo ao locatário
        User locatario = userRepo.findById(r.getLocatarioId()).orElseThrow();
        locatario.setSaldo(locatario.getSaldo() + r.getTotal());
        userRepo.update(locatario);
        if (locatario.getId() == current.getId()) Session.setUser(locatario);
    }

    /** CAR-14: Registo de quilometragem inicial */
    public void registarKmInicial(int reservaId, int km) throws SQLException {
        Reserva r = getReservaDoLocatario(reservaId);
        if (!"confirmada".equals(r.getEstado())) throw new IllegalArgumentException("Reserva não está confirmada.");
        if (km < 0) throw new IllegalArgumentException("Quilometragem inválida.");
        reservaRepo.updateKmInicial(reservaId, km);
    }

    /** CAR-18: Registo de quilometragem final e liquidação */
    public void registarKmFinalELiquidar(int reservaId, int kmFinal, double precoCombustivel, double consumoL100) throws SQLException {
        Reserva r = getReservaDoLocatario(reservaId);
        if (!"confirmada".equals(r.getEstado())) throw new IllegalArgumentException("Reserva não está confirmada.");
        if (r.getKmInicial() == null) throw new IllegalArgumentException("Km inicial não registado.");
        if (kmFinal <= r.getKmInicial()) throw new IllegalArgumentException("Km final deve ser maior que km inicial.");

        int kmsPercorridos = kmFinal - r.getKmInicial();
        double custoCombustivel = Math.round((kmsPercorridos / 100.0) * consumoL100 * precoCombustivel * 100.0) / 100.0;

        reservaRepo.updateKmFinalELiquidacao(reservaId, kmFinal, custoCombustivel);

        // Creditar proprietário (renda)
        User proprietario = userRepo.findById(r.getProprietarioId()).orElseThrow();
        proprietario.setSaldo(proprietario.getSaldo() + r.getTotal());
        userRepo.update(proprietario);
    }

    public List<Reserva> minhasReservasComoLocatario() throws SQLException {
        return reservaRepo.findByLocatario(Session.getUser().getId());
    }

    public List<Reserva> minhasReservasComoProprietario() throws SQLException {
        return reservaRepo.findByProprietario(Session.getUser().getId());
    }

    public List<Reserva> listarTodas() throws SQLException { return reservaRepo.findAll(); }

    public void avaliarReserva(int reservaId, int estrelas, String comentario) throws SQLException {
        Reserva r = getReservaDoLocatario(reservaId);
        if (!"concluida".equals(r.getEstado())) throw new IllegalArgumentException("Apenas reservas concluídas podem ser avaliadas.");
        if (r.getAvaliacao() != null) throw new IllegalArgumentException("Esta reserva já foi avaliada.");
        if (estrelas < 1 || estrelas > 5) throw new IllegalArgumentException("Avaliação deve ser entre 1 e 5 estrelas.");
        reservaRepo.updateAvaliacao(reservaId, estrelas, comentario);
    }

    private Reserva getReservaDoProprietario(int reservaId) throws SQLException {
        Reserva r = reservaRepo.findById(reservaId).orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        if (r.getProprietarioId() != Session.getUser().getId()) throw new IllegalStateException("Sem permissão.");
        return r;
    }

    private Reserva getReservaDoLocatario(int reservaId) throws SQLException {
        Reserva r = reservaRepo.findById(reservaId).orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        if (r.getLocatarioId() != Session.getUser().getId()) throw new IllegalStateException("Sem permissão.");
        return r;
    }
}
