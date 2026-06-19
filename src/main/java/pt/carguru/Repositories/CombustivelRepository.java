package pt.carguru.Repositories;

import pt.carguru.Models.PrecoCombustivel;
import pt.carguru.Utils.DatabaseConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CombustivelRepository {

    // ── Leitura ───────────────────────────────────────────────────────────────

    /** Todos os tipos de combustível (uma linha por tipo). */
    public List<PrecoCombustivel> findAll() throws SQLException {
        List<PrecoCombustivel> list = new ArrayList<>();
        String sql = "SELECT * FROM precos_combustivel ORDER BY FIELD(tipo_combustivel,'GASOLINA','GASOLEO','GPL','ELETRICO')";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Preço corrente de um tipo específico. */
    public Optional<PrecoCombustivel> findByTipo(String tipo) throws SQLException {
        String sql = "SELECT * FROM precos_combustivel WHERE tipo_combustivel=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        }
        return Optional.empty();
    }

    // ── Admin: definir preço base ─────────────────────────────────────────────

    /**
     * Cria ou atualiza o preço base de um tipo.
     * O preço corrente é inicializado igual ao base.
     */
    public void upsertPrecoBase(String tipo, double precoBase, int adminId) throws SQLException {
        String sql =
            "INSERT INTO precos_combustivel (tipo_combustivel, preco_base, preco_corrente, registado_por) " +
            "VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE preco_base=VALUES(preco_base), " +
            "preco_corrente=VALUES(preco_corrente), registado_por=VALUES(registado_por), " +
            "ultima_atualizacao=NOW()";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            BigDecimal bd = BigDecimal.valueOf(precoBase).setScale(4, RoundingMode.HALF_UP);
            ps.setString(1, tipo.toUpperCase());
            ps.setBigDecimal(2, bd);
            ps.setBigDecimal(3, bd);   // corrente = base no momento da definição
            ps.setInt(4, adminId);
            ps.executeUpdate();
        }
    }

    // ── Scheduler: atualizar preço corrente ───────────────────────────────────

    /**
     * Atualiza o preço corrente de todos os tipos na BD.
     * Chamado pelo scheduler a cada 10 minutos.
     *
     * @param novoPrecoCorrente mapa tipo → novo preço
     */
    public void atualizarPrecoCorrente(String tipo, double novoPreco) throws SQLException {
        String sql = "UPDATE precos_combustivel SET preco_corrente=?, ultima_atualizacao=NOW() WHERE tipo_combustivel=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, BigDecimal.valueOf(novoPreco).setScale(4, RoundingMode.HALF_UP));
            ps.setString(2, tipo.toUpperCase());
            ps.executeUpdate();
        }
    }

    // ── Registo horário ───────────────────────────────────────────────────────

    /**
     * Insere uma linha em historico_precos — chamado pelo scheduler a cada hora.
     * Só insere se não existir já um registo na mesma hora para este tipo
     * (idempotente: evita duplicados em reinícios).
     */
    public void registarHistorico(String tipo, int precoId, double preco) throws SQLException {
        String sql =
            "INSERT INTO historico_precos (preco_id, tipo_combustivel, preco) " +
            "SELECT ?, ?, ? FROM DUAL " +
            "WHERE NOT EXISTS (" +
            "  SELECT 1 FROM historico_precos " +
            "  WHERE tipo_combustivel=? AND registado_em >= DATE_FORMAT(NOW(),'%Y-%m-%d %H:00:00')" +
            ")";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, precoId);
            ps.setString(2, tipo.toUpperCase());
            ps.setBigDecimal(3, BigDecimal.valueOf(preco).setScale(4, RoundingMode.HALF_UP));
            ps.setString(4, tipo.toUpperCase());
            ps.executeUpdate();
        }
    }

    /** Histórico das últimas N horas para um tipo (para exibir no admin). */
    public List<double[]> findHistorico(String tipo, int horas) throws SQLException {
        List<double[]> list = new ArrayList<>();
        String sql =
            "SELECT preco, registado_em FROM historico_precos " +
            "WHERE tipo_combustivel=? AND registado_em >= DATE_SUB(NOW(), INTERVAL ? HOUR) " +
            "ORDER BY registado_em ASC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo.toUpperCase());
            ps.setInt(2, horas);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                BigDecimal p = rs.getBigDecimal("preco");
                list.add(new double[]{ p != null ? p.doubleValue() : 0.0,
                                       rs.getTimestamp("registado_em").getTime() });
            }
        }
        return list;
    }

    // ── Helper privado ────────────────────────────────────────────────────────

    private PrecoCombustivel map(ResultSet rs) throws SQLException {
        PrecoCombustivel p = new PrecoCombustivel();
        p.setId(rs.getInt("id"));
        p.setTipoCombustivel(rs.getString("tipo_combustivel"));
        BigDecimal base = rs.getBigDecimal("preco_base");
        p.setPrecoBase(base != null ? base.doubleValue() : 0.0);
        BigDecimal curr = rs.getBigDecimal("preco_corrente");
        p.setPrecoCorrente(curr != null ? curr.doubleValue() : 0.0);
        Timestamp ts = rs.getTimestamp("ultima_atualizacao");
        if (ts != null) p.setUltimaAtualizacao(ts.toLocalDateTime());
        p.setRegistadoPor(rs.getInt("registado_por"));
        return p;
    }
}
