package pt.carguru.Repositories;

import pt.carguru.Models.Disputa;
import pt.carguru.Utils.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DisputaRepository {

    /**
     * Abre uma nova disputa para o aluguer associado à reserva indicada.
     * Falha (com exceção) se já existir uma disputa para esse aluguer (UNIQUE KEY).
     */
    public Disputa abrir(int aluguerIdInterno, int iniciadorId, String descricao) throws SQLException {
        String sql = "INSERT INTO disputas (aluguer_id, iniciador_id, descricao) VALUES (?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, aluguerIdInterno);
            ps.setInt(2, iniciadorId);
            ps.setString(3, descricao);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (!keys.next()) throw new SQLException("Erro ao criar disputa.");
            Disputa d = new Disputa();
            d.setId(keys.getInt(1));
            d.setAluguerIdInterno(aluguerIdInterno);
            d.setIniciadorId(iniciadorId);
            d.setDescricao(descricao);
            d.setEstado("ABERTA");
            return d;
        }
    }

    /** Marca a disputa como EM_ANALISE e associa o admin. */
    public void iniciarAnalise(int disputaId, int adminId) throws SQLException {
        String sql = "UPDATE disputas SET estado='EM_ANALISE', admin_id=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setInt(2, disputaId);
            ps.executeUpdate();
        }
    }

    /**
     * Resolve a disputa e processa os movimentos financeiros da caução.
     *
     * @param disputaId      ID da disputa
     * @param adminId        ID do admin que resolve
     * @param estado         RESOLVIDA_PROPRIETARIO | RESOLVIDA_LOCATARIO | ENCERRADA
     * @param resolucao      Texto da decisão
     * @param reembolso      Valor a devolver ao locatário (≥ 0)
     * @param penalizacao    Valor a reter do locatário (≥ 0)
     */
    public void resolver(int disputaId, int adminId, String estado,
                         String resolucao, double reembolso, double penalizacao) throws SQLException {

        // 1. Obter dados da disputa + aluguer + reserva
        String sqlSelect =
            "SELECT d.id, d.aluguer_id, r.caucao, r.locatario_id, v.proprietario_id " +
            "FROM disputas d " +
            "JOIN alugueres al ON d.aluguer_id = al.id " +
            "JOIN reservas r ON al.reserva_id = r.id " +
            "JOIN veiculos v ON r.veiculo_id = v.id " +
            "WHERE d.id=?";

        double caucao; int locatarioId; int proprietarioId; int aluguerIdInterno;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sqlSelect)) {
            ps.setInt(1, disputaId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new SQLException("Disputa não encontrada.");
            aluguerIdInterno = rs.getInt("aluguer_id");
            BigDecimal cau = rs.getBigDecimal("caucao");
            caucao = cau != null ? cau.doubleValue() : 0.0;
            locatarioId  = rs.getInt("locatario_id");
            proprietarioId = rs.getInt("proprietario_id");
        }

        // Sanitizar: reembolso e penalização não podem exceder caução
        double reembolsoEfetivo  = Math.min(reembolso, caucao);
        double penalizacaoEfetiva = Math.min(penalizacao, caucao - reembolsoEfetivo);

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // 2. Atualizar estado da disputa
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE disputas SET estado=?, admin_id=?, resolucao=?, " +
                        "reembolso_forcado=?, penalizacao=?, data_resolucao=NOW() WHERE id=?")) {
                    ps.setString(1, estado.toUpperCase());
                    ps.setInt(2, adminId);
                    ps.setString(3, resolucao);
                    ps.setBigDecimal(4, BigDecimal.valueOf(reembolsoEfetivo));
                    ps.setBigDecimal(5, BigDecimal.valueOf(penalizacaoEfetiva));
                    ps.setInt(6, disputaId);
                    ps.executeUpdate();
                }

                // 3. Movimentos financeiros
                if (reembolsoEfetivo > 0) {
                    // Devolver parte (ou totalidade) da caução ao locatário
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE utilizadores SET saldo = saldo + ? WHERE id=?")) {
                        ps.setBigDecimal(1, BigDecimal.valueOf(reembolsoEfetivo));
                        ps.setInt(2, locatarioId);
                        ps.executeUpdate();
                    }
                    double saldoLoc = lerSaldo(c, locatarioId);
                    inserirTransacao(c, locatarioId, "CAUCAO_DEVOLVIDA", reembolsoEfetivo,
                            String.format("Resolução de disputa #%d — caução devolvida por decisão do administrador", disputaId),
                            saldoLoc, disputaId, "disputa");
                }

                if (penalizacaoEfetiva > 0) {
                    // Transferir penalização para o proprietário
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE utilizadores SET saldo = saldo + ? WHERE id=?")) {
                        ps.setBigDecimal(1, BigDecimal.valueOf(penalizacaoEfetiva));
                        ps.setInt(2, proprietarioId);
                        ps.executeUpdate();
                    }
                    double saldoProp = lerSaldo(c, proprietarioId);
                    inserirTransacao(c, proprietarioId, "REEMBOLSO", penalizacaoEfetiva,
                            String.format("Resolução de disputa #%d — compensação transferida pelo administrador", disputaId),
                            saldoProp, disputaId, "disputa");

                    // Registar penalização no histórico do locatário
                    double saldoLoc = lerSaldo(c, locatarioId);
                    inserirTransacao(c, locatarioId, "PENALIZACAO", penalizacaoEfetiva,
                            String.format("Resolução de disputa #%d — penalização aplicada por decisão do administrador", disputaId),
                            saldoLoc, disputaId, "disputa");
                }

                // 4. Atualizar alugueres.caucao_devolvida para refletir a decisão
                boolean caucaoDevolvida = reembolsoEfetivo > 0 || "RESOLVIDA_LOCATARIO".equalsIgnoreCase(estado);
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE alugueres SET caucao_devolvida=? WHERE id=?")) {
                    ps.setBoolean(1, caucaoDevolvida);
                    ps.setInt(2, aluguerIdInterno);
                    ps.executeUpdate();
                }

                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Todas as disputas (para o admin), mais recentes primeiro. */
    public List<Disputa> findAll() throws SQLException {
        return query(buildJoin("1=1") + " ORDER BY d.data_criacao DESC", -1);
    }

    /** Disputas abertas ou em análise (para o admin ver pendentes). */
    public List<Disputa> findPendentes() throws SQLException {
        return query(buildJoin("d.estado IN ('ABERTA','EM_ANALISE')") + " ORDER BY d.data_criacao ASC", -1);
    }

    /** Disputas relacionadas com um utilizador (como locatário ou proprietário). */
    public List<Disputa> findByUtilizador(int utilizadorId) throws SQLException {
        String sql = buildJoin("r.locatario_id=? OR v.proprietario_id=?") + " ORDER BY d.data_criacao DESC";
        List<Disputa> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, utilizadorId);
            ps.setInt(2, utilizadorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Verificar se já existe disputa para o aluguer de uma reserva. */
    public boolean existeParaReserva(int reservaId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM disputas d JOIN alugueres al ON d.aluguer_id=al.id WHERE al.reserva_id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservaId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /** Obtém o aluguer_id interno a partir do reserva_id. */
    public Optional<Integer> getAluguerIdParaReserva(int reservaId) throws SQLException {
        String sql = "SELECT id FROM alugueres WHERE reserva_id=? LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(rs.getInt("id"));
        }
        return Optional.empty();
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private List<Disputa> query(String sql, int param) throws SQLException {
        List<Disputa> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param >= 0) ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private String buildJoin(String where) {
        return "SELECT d.*, " +
               "ui.nome AS iniciador_nome, ua.nome AS admin_nome, " +
               "v.marca, v.modelo, v.ano, " +
               "ul.nome AS locatario_nome, up.nome AS proprietario_nome, " +
               "r.caucao, r.locatario_id, v.proprietario_id, al.reserva_id " +
               "FROM disputas d " +
               "JOIN alugueres al ON d.aluguer_id = al.id " +
               "JOIN reservas r ON al.reserva_id = r.id " +
               "JOIN veiculos v ON r.veiculo_id = v.id " +
               "JOIN utilizadores ui ON d.iniciador_id = ui.id " +
               "JOIN utilizadores ul ON r.locatario_id = ul.id " +
               "JOIN utilizadores up ON v.proprietario_id = up.id " +
               "LEFT JOIN utilizadores ua ON d.admin_id = ua.id " +
               "WHERE " + where;
    }

    private Disputa map(ResultSet rs) throws SQLException {
        Disputa d = new Disputa();
        d.setId(rs.getInt("id"));
        d.setAluguerIdInterno(rs.getInt("aluguer_id"));
        d.setReservaId(rs.getInt("reserva_id"));
        d.setIniciadorId(rs.getInt("iniciador_id"));
        int adminId = rs.getInt("admin_id"); if (!rs.wasNull()) d.setAdminId(adminId);
        d.setDescricao(rs.getString("descricao"));
        d.setEstado(rs.getString("estado"));
        d.setResolucao(rs.getString("resolucao"));
        BigDecimal ree = rs.getBigDecimal("reembolso_forcado"); if (ree != null) d.setReembolsoForcado(ree.doubleValue());
        BigDecimal pen = rs.getBigDecimal("penalizacao");        if (pen != null) d.setPenalizacao(pen.doubleValue());
        Timestamp criacao = rs.getTimestamp("data_criacao");     if (criacao != null) d.setDataCriacao(criacao.toLocalDateTime());
        Timestamp resolv  = rs.getTimestamp("data_resolucao");   if (resolv  != null) d.setDataResolucao(resolv.toLocalDateTime());
        d.setIniciadorNome(rs.getString("iniciador_nome"));
        d.setAdminNome(rs.getString("admin_nome"));
        d.setVeiculoNome(rs.getString("marca") + " " + rs.getString("modelo") + " (" + rs.getInt("ano") + ")");
        d.setLocatarioNome(rs.getString("locatario_nome"));
        d.setProprietarioNome(rs.getString("proprietario_nome"));
        BigDecimal cau = rs.getBigDecimal("caucao"); if (cau != null) d.setCaucao(cau.doubleValue());
        d.setLocatarioId(rs.getInt("locatario_id"));
        d.setProprietarioId(rs.getInt("proprietario_id"));
        return d;
    }

    private double lerSaldo(Connection c, int utilizadorId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT saldo FROM utilizadores WHERE id=?")) {
            ps.setInt(1, utilizadorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) { BigDecimal v = rs.getBigDecimal("saldo"); return v != null ? v.doubleValue() : 0.0; }
        }
        return 0.0;
    }

    private void inserirTransacao(Connection c, int utilizadorId, String tipo, double valor,
                                   String descricao, double saldoApos,
                                   Integer referenciaId, String referenciaTipo) throws SQLException {
        String sql = "INSERT INTO transacoes (utilizador_id, tipo, valor, descricao, saldo_apos, referencia_id, referencia_tipo) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, utilizadorId);
            ps.setString(2, tipo);
            ps.setBigDecimal(3, BigDecimal.valueOf(valor));
            ps.setString(4, descricao);
            ps.setBigDecimal(5, BigDecimal.valueOf(saldoApos));
            if (referenciaId != null) ps.setInt(6, referenciaId); else ps.setNull(6, Types.INTEGER);
            ps.setString(7, referenciaTipo);
            ps.executeUpdate();
        }
    }
}
