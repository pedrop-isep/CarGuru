package pt.carguru.Services;

import pt.carguru.Models.Disputa;
import pt.carguru.Models.User;
import pt.carguru.Repositories.DisputaRepository;
import pt.carguru.Repositories.UserRepository;
import pt.carguru.Utils.EmailSender;
import pt.carguru.Utils.Session;

import java.sql.SQLException;
import java.util.List;

public class DisputaService {

    private final DisputaRepository disputaRepo = new DisputaRepository();
    private final UserRepository    userRepo    = new UserRepository();

    /**
     * Abre uma disputa para a reserva indicada.
     * Só pode ser aberta pelo locatário, e só após o aluguer estar concluído com incidente.
     */
    public Disputa abrirDisputa(int reservaId, String descricao) throws SQLException {
        if (descricao == null || descricao.isBlank())
            throw new IllegalArgumentException("A descrição da disputa não pode estar vazia.");

        if (disputaRepo.existeParaReserva(reservaId))
            throw new IllegalStateException("Já existe uma disputa aberta para este aluguer.");

        int aluguerIdInterno = disputaRepo.getAluguerIdParaReserva(reservaId)
                .orElseThrow(() -> new IllegalStateException(
                        "Não foi encontrado nenhum registo de aluguer para esta reserva. " +
                        "A disputa só pode ser aberta após o aluguer estar concluído."));

        int iniciadorId = Session.getUser().getId();
        return disputaRepo.abrir(aluguerIdInterno, iniciadorId, descricao.trim());
    }

    /** Admin marca a disputa como "Em Análise". */
    public void iniciarAnalise(int disputaId) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        disputaRepo.iniciarAnalise(disputaId, Session.getUser().getId());
    }

    /**
     * Admin resolve a disputa a favor do proprietário.
     * A caução (ou parte) é transferida para o proprietário como penalização.
     */
    public void resolverFavorProprietario(int disputaId, String resolucao, double penalizacao) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        validarResolucao(resolucao);

        Disputa d = encontrarDisputa(disputaId);
        disputaRepo.resolver(disputaId, Session.getUser().getId(),
                "RESOLVIDA_PROPRIETARIO", resolucao, 0.0, penalizacao);
        Session.refresh();

        if (d != null) {
            final Disputa disputa = d;
            final double pen = penalizacao;
            new Thread(() -> notificarPartesResolucao(disputa, resolucao,
                    "RESOLVIDA_PROPRIETARIO", 0.0, pen)).start();
        }
    }

    /**
     * Admin resolve a disputa a favor do locatário.
     * A caução é devolvida ao locatário.
     */
    public void resolverFavorLocatario(int disputaId, String resolucao, double reembolso) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        validarResolucao(resolucao);

        Disputa d = encontrarDisputa(disputaId);
        disputaRepo.resolver(disputaId, Session.getUser().getId(),
                "RESOLVIDA_LOCATARIO", resolucao, reembolso, 0.0);
        Session.refresh();

        if (d != null) {
            final Disputa disputa = d;
            final double ree = reembolso;
            new Thread(() -> notificarPartesResolucao(disputa, resolucao,
                    "RESOLVIDA_LOCATARIO", ree, 0.0)).start();
        }
    }

    /** Admin encerra a disputa sem consequências financeiras. */
    public void encerrar(int disputaId, String resolucao) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        validarResolucao(resolucao);

        Disputa d = encontrarDisputa(disputaId);
        disputaRepo.resolver(disputaId, Session.getUser().getId(),
                "ENCERRADA", resolucao, 0.0, 0.0);

        if (d != null) {
            final Disputa disputa = d;
            new Thread(() -> notificarPartesResolucao(disputa, resolucao,
                    "ENCERRADA", 0.0, 0.0)).start();
        }
    }

    /** Lista todas as disputas (admin). */
    public List<Disputa> listarTodas() throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        return disputaRepo.findAll();
    }

    /** Lista disputas pendentes de resolução (admin). */
    public List<Disputa> listarPendentes() throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        return disputaRepo.findPendentes();
    }

    /** Disputas relacionadas com o utilizador atual (locatário ou proprietário). */
    public List<Disputa> minhasDisputas() throws SQLException {
        return disputaRepo.findByUtilizador(Session.getUser().getId());
    }

    /** Verifica se já existe disputa para a reserva. */
    public boolean existeDisputa(int reservaId) throws SQLException {
        return disputaRepo.existeParaReserva(reservaId);
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private void validarResolucao(String resolucao) {
        if (resolucao == null || resolucao.isBlank())
            throw new IllegalArgumentException("A descrição da resolução não pode estar vazia.");
    }

    /** Carrega a disputa da BD para obter os IDs necessários para o email. */
    private Disputa encontrarDisputa(int disputaId) {
        try {
            return disputaRepo.findAll().stream()
                    .filter(x -> x.getId() == disputaId)
                    .findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Envia emails de notificação ao locatário e ao proprietário após a resolução.
     * Corre em thread separada — falhas de email são registadas na consola mas não propagadas.
     */
    private void notificarPartesResolucao(Disputa d, String resolucao,
                                           String estadoFinal,
                                           double reembolso, double penalizacao) {
        try {
            User locatario    = userRepo.findById(d.getLocatarioId()).orElse(null);
            User proprietario = userRepo.findById(d.getProprietarioId()).orElse(null);

            if (locatario != null && locatario.getEmail() != null) {
                double valorLoc = "RESOLVIDA_LOCATARIO".equals(estadoFinal) ? reembolso : penalizacao;
                EmailSender.enviarResolucaoDisputa(
                        locatario.getEmail(), locatario.getNome(),
                        d.getId(), d.getVeiculoNome(),
                        resolucao, estadoFinal, valorLoc, "locatário");
            }

            if (proprietario != null && proprietario.getEmail() != null) {
                double valorProp = "RESOLVIDA_PROPRIETARIO".equals(estadoFinal) ? penalizacao : 0.0;
                EmailSender.enviarResolucaoDisputa(
                        proprietario.getEmail(), proprietario.getNome(),
                        d.getId(), d.getVeiculoNome(),
                        resolucao, estadoFinal, valorProp, "proprietário");
            }
        } catch (Exception e) {
            System.err.println("[DisputaService] Erro ao enviar emails de resolução: " + e.getMessage());
        }
    }
}
