package pt.carguru.Services;

import pt.carguru.Models.Reserva;
import pt.carguru.Repositories.IndisponibilidadeRepository;
import pt.carguru.Repositories.ReservaRepository;
import pt.carguru.Repositories.VeiculoRepository;
import pt.carguru.Models.Veiculo;
import pt.carguru.Utils.Session;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReservaService {
    private final ReservaRepository reservaRepo = new ReservaRepository();
    private final VeiculoRepository veiculoRepo = new VeiculoRepository();
    private final IndisponibilidadeRepository indispRepo = new IndisponibilidadeRepository();

    public Reserva criarReserva(int veiculoId, LocalDate inicio, LocalDate fim) throws SQLException {
        if (inicio == null || fim == null || !fim.isAfter(inicio))
            throw new IllegalArgumentException("Datas inválidas. A data de fim deve ser após a de início.");
        if (inicio.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("A data de início não pode ser no passado.");

        Veiculo v = veiculoRepo.findById(veiculoId)
            .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado."));

        if (!"DISPONIVEL".equals(v.getEstado()))
            throw new IllegalStateException("Este veículo não está disponível.");
        if (v.getProprietarioId() == Session.getUser().getId())
            throw new IllegalStateException("Não podes reservar o teu próprio veículo.");
        if (reservaRepo.existeSobreposicao(veiculoId, inicio, fim))
            throw new IllegalStateException("Já existe uma reserva para este período. Escolhe outras datas.");
        if (indispRepo.estaIndisponivel(veiculoId, inicio, fim))
            throw new IllegalStateException("O proprietário marcou este período como indisponível.");

        long dias = java.time.temporal.ChronoUnit.DAYS.between(inicio, fim);
        double total = dias * v.getPrecoPorDia();

        if (Session.getUser().getSaldo() < total * 0.2)
            throw new IllegalStateException(String.format("Saldo insuficiente para a caução (%.2f€).", total * 0.2));

        Reserva r = new Reserva();
        r.setVeiculoId(veiculoId);
        r.setLocatarioId(Session.getUser().getId());
        r.setDataInicio(inicio);
        r.setDataFim(fim);
        r.setTotal(total);
        r.setEstado("pendente");
        return reservaRepo.save(r);
    }

    public void aprovarReserva(int reservaId) throws SQLException {
        Reserva r = reservaRepo.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        if (r.getProprietarioId() != Session.getUser().getId())
            throw new IllegalStateException("Sem permissão.");
        if (!"pendente".equals(r.getEstado()))
            throw new IllegalStateException("Só é possível aprovar reservas pendentes.");
        reservaRepo.updateEstado(reservaId, "ACEITE");
    }

    public void cancelarReserva(int reservaId) throws SQLException {
        Reserva r = reservaRepo.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        int userId = Session.getUser().getId();
        if (r.getLocatarioId() != userId && r.getProprietarioId() != userId && !Session.isAdmin())
            throw new IllegalStateException("Sem permissão.");
        if ("cancelada".equals(r.getEstado()) || "concluida".equals(r.getEstado()))
            throw new IllegalStateException("Esta reserva não pode ser cancelada.");
        reservaRepo.updateEstado(reservaId, "CANCELADA");
    }

    public void registarKmInicial(int reservaId, int km) throws SQLException {
        Reserva r = reservaRepo.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        if (r.getLocatarioId() != Session.getUser().getId())
            throw new IllegalStateException("Sem permissão.");
        if (!"confirmada".equals(r.getEstado()))
            throw new IllegalStateException("Reserva não está confirmada.");
        if (km < 0) throw new IllegalArgumentException("Km inválido.");
        reservaRepo.updateKmInicial(reservaId, km);
    }

    public void registarKmFinalELiquidar(int reservaId, int kmFinal, double precoCombustivel, double consumo) throws SQLException {
        Reserva r = reservaRepo.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        if (r.getLocatarioId() != Session.getUser().getId())
            throw new IllegalStateException("Sem permissão.");
        if (r.getKmInicial() == null)
            throw new IllegalStateException("Regista primeiro o Km inicial.");
        if (kmFinal <= r.getKmInicial())
            throw new IllegalArgumentException("Km final deve ser maior que o inicial.");
        reservaRepo.updateKmFinalELiquidacao(reservaId, kmFinal, precoCombustivel);
    }

    public void avaliarReserva(int reservaId, int estrelas, String comentario) throws SQLException {
        if (estrelas < 1 || estrelas > 5)
            throw new IllegalArgumentException("Avaliação deve ser entre 1 e 5.");
        reservaRepo.updateAvaliacao(reservaId, estrelas, comentario);
    }

    public List<Reserva> minhasReservasComoLocatario() throws SQLException {
        return reservaRepo.findByLocatario(Session.getUser().getId());
    }

    public List<Reserva> minhasReservasComoProprietario() throws SQLException {
        return reservaRepo.findByProprietario(Session.getUser().getId());
    }

    public List<Reserva> listarTodas() throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        return reservaRepo.findAll();
    }
}
