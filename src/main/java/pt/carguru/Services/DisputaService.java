package pt.carguru.Services;

import pt.carguru.Models.Disputa;
import pt.carguru.Repositories.DisputaRepository;
import pt.carguru.Utils.Session;

import java.sql.SQLException;
import java.util.List;

public class DisputaService {

    private final DisputaRepository disputaRepo = new DisputaRepository();

    /**
     * Abre uma disputa para a reserva indicada.
     * Só pode ser aberta pelo locatário, e só após o aluguer estar concluído com incidente.
     *
     * @param reservaId  ID da reserva (a que teve incidente)
     * @param descricao  Descrição do problema apresentado pelo locatário
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

    /**
     * Admin marca a disputa como "Em Análise".
     */
    public void iniciarAnalise(int disputaId) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        disputaRepo.iniciarAnalise(disputaId, Session.getUser().getId());
    }

    /**
     * Admin resolve a disputa a favor do proprietário:
     * a caução (ou parte) fica retida / transferida para o proprietário como penalização.
     *
     * @param disputaId    ID da disputa
     * @param resolucao    Texto da decisão
     * @param penalizacao  Valor da caução transferido para o proprietário (0 a valor total da caução)
     */
    public void resolverFavorProprietario(int disputaId, String resolucao, double penalizacao) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        validarResolucao(resolucao);
        disputaRepo.resolver(disputaId, Session.getUser().getId(),
                "RESOLVIDA_PROPRIETARIO", resolucao, 0.0, penalizacao);
        Session.refresh();
    }

    /**
     * Admin resolve a disputa a favor do locatário:
     * a caução é devolvida ao locatário.
     *
     * @param disputaId   ID da disputa
     * @param resolucao   Texto da decisão
     * @param reembolso   Valor da caução a devolver (0 a valor total da caução)
     */
    public void resolverFavorLocatario(int disputaId, String resolucao, double reembolso) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        validarResolucao(resolucao);
        disputaRepo.resolver(disputaId, Session.getUser().getId(),
                "RESOLVIDA_LOCATARIO", resolucao, reembolso, 0.0);
        Session.refresh();
    }

    /**
     * Admin encerra a disputa sem consequências financeiras (ex: situação esclarecida sem culpa).
     */
    public void encerrar(int disputaId, String resolucao) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        validarResolucao(resolucao);
        disputaRepo.resolver(disputaId, Session.getUser().getId(),
                "ENCERRADA", resolucao, 0.0, 0.0);
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

    private void validarResolucao(String resolucao) {
        if (resolucao == null || resolucao.isBlank())
            throw new IllegalArgumentException("A descrição da resolução não pode estar vazia.");
    }
}
