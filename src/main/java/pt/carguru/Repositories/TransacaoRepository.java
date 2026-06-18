package pt.carguru.Repositories;

import pt.carguru.Models.Transacao;
import pt.carguru.Utils.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransacaoRepository {

    /** Regista um movimento na conta do utilizador. */
    public void registar(int utilizadorId, String tipo, double valor, String descricao,
                          double saldoApos, Integer referenciaId, String referenciaTipo) throws SQLException {
        String sql = "INSERT INTO transacoes (utilizador_id, tipo, valor, descricao, saldo_apos, referencia_id, referencia_tipo) " +
                     "VALUES (?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
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

    /** Histórico completo (sem limite) com contraparte. */
    public List<Transacao> findByUtilizador(int utilizadorId) throws SQLException {
        return findByUtilizador(utilizadorId, 0, null, null, null);
    }

    /** Histórico com limite (compatibilidade legada). */
    public List<Transacao> findByUtilizador(int utilizadorId, int limit) throws SQLException {
        return findByUtilizador(utilizadorId, limit, null, null, null);
    }

    /**
     * Histórico com filtros opcionais.
     *
     * @param utilizadorId  ID do utilizador
     * @param limit         0 = sem limite
     * @param tipo          null = todos os tipos; ex: "DEPOSITO"
     * @param dataInicio    null = sem limite inferior
     * @param dataFim       null = sem limite superior
     */
    public List<Transacao> findByUtilizador(int utilizadorId, int limit,
                                             String tipo, LocalDate dataInicio, LocalDate dataFim) throws SQLException {
        List<Transacao> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            // Subquery para obter o nome da contraparte via reserva ligada à transação:
            // - Para PAGAMENTO_ALUGUER / CAUCAO_RETIDA / CAUCAO_DEVOLVIDA: contraparte = proprietário
            // - Para RECEITA_ALUGUER / REEMBOLSO: contraparte = locatário
            // - Para DEPOSITO / LEVANTAMENTO / PENALIZACAO: sem contraparte
            "SELECT t.*, " +
            "CASE " +
            "  WHEN t.tipo IN ('PAGAMENTO_ALUGUER','CAUCAO_RETIDA','CAUCAO_DEVOLVIDA') " +
            "    THEN (SELECT up.nome FROM reservas r JOIN veiculos v ON r.veiculo_id=v.id " +
            "          JOIN utilizadores up ON v.proprietario_id=up.id WHERE r.id=t.referencia_id LIMIT 1) " +
            "  WHEN t.tipo IN ('RECEITA_ALUGUER','REEMBOLSO') " +
            "    THEN (SELECT ul.nome FROM reservas r JOIN utilizadores ul ON r.locatario_id=ul.id " +
            "          WHERE r.id=t.referencia_id LIMIT 1) " +
            "  ELSE NULL " +
            "END AS contraparte " +
            "FROM transacoes t " +
            "WHERE t.utilizador_id=?"
        );

        List<Object> params = new ArrayList<>();
        params.add(utilizadorId);

        if (tipo != null && !tipo.isBlank()) {
            sql.append(" AND t.tipo=?");
            params.add(tipo);
        }
        if (dataInicio != null) {
            sql.append(" AND DATE(t.data) >= ?");
            params.add(Date.valueOf(dataInicio));
        }
        if (dataFim != null) {
            sql.append(" AND DATE(t.data) <= ?");
            params.add(Date.valueOf(dataFim));
        }

        sql.append(" ORDER BY t.data DESC, t.id DESC");
        if (limit > 0) sql.append(" LIMIT ").append(limit);

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer)   ps.setInt(i + 1, (Integer) p);
                else if (p instanceof String) ps.setString(i + 1, (String) p);
                else if (p instanceof Date)   ps.setDate(i + 1, (Date) p);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private Transacao map(ResultSet rs) throws SQLException {
        Transacao t = new Transacao();
        t.setId(rs.getInt("id"));
        t.setUtilizadorId(rs.getInt("utilizador_id"));
        t.setTipo(rs.getString("tipo"));
        BigDecimal valor = rs.getBigDecimal("valor");
        t.setValor(valor != null ? valor.doubleValue() : 0.0);
        t.setDescricao(rs.getString("descricao"));
        Timestamp data = rs.getTimestamp("data");
        if (data != null) t.setData(data.toLocalDateTime());
        BigDecimal saldoApos = rs.getBigDecimal("saldo_apos");
        t.setSaldoApos(saldoApos != null ? saldoApos.doubleValue() : 0.0);
        int refId = rs.getInt("referencia_id");
        if (!rs.wasNull()) t.setReferenciaId(refId);
        t.setReferenciaTipo(rs.getString("referencia_tipo"));
        try { t.setContraparte(rs.getString("contraparte")); } catch (Exception ignored) {}
        return t;
    }
}
