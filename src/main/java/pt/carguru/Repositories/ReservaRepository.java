package pt.carguru.Repositories;

import pt.carguru.Models.Reserva;
import pt.carguru.Utils.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservaRepository {

    public Reserva save(Reserva r) throws SQLException {
        long dias = java.time.temporal.ChronoUnit.DAYS.between(r.getDataInicio(), r.getDataFim());
        double precoDia = dias > 0 ? r.getTotal() / dias : r.getTotal();
        double caucao = r.getTotal() * 0.20;
        String sql = "INSERT INTO reservas (veiculo_id, locatario_id, data_inicio, data_fim, preco_dia_dinamico, custo_renda, caucao, data_expiracao) VALUES (?,?,?,?,?,?,?, DATE_ADD(NOW(), INTERVAL 24 HOUR))";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getVeiculoId());
            ps.setInt(2, r.getLocatarioId());
            ps.setDate(3, Date.valueOf(r.getDataInicio()));
            ps.setDate(4, Date.valueOf(r.getDataFim()));
            ps.setBigDecimal(5, BigDecimal.valueOf(precoDia));
            ps.setBigDecimal(6, BigDecimal.valueOf(r.getTotal()));
            ps.setBigDecimal(7, BigDecimal.valueOf(caucao));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) r.setId(keys.getInt(1));
        }
        return r;
    }

    public void updateEstado(int id, String estado) throws SQLException {
        String col = switch (estado.toUpperCase()) {
            case "ACEITE"    -> ", data_aceitacao=NOW()";
            case "CANCELADA" -> ", data_cancelamento=NOW()";
            default          -> "";
        };
        String sql = "UPDATE reservas SET estado=?" + col + " WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, estado.toUpperCase());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void updateKmInicial(int id, int km) throws SQLException {
        String sql = "UPDATE reservas SET km_inicial=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, km); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public void updateKmFinalELiquidacao(int id, int kmFinal, double precoComb) throws SQLException {
        // Marca como concluída na tabela reservas
        updateEstado(id, "CONCLUIDA");
    }

    public void updateAvaliacao(int id, int estrelas, String comentario) throws SQLException {
        // Inserir na tabela avaliacoes se existir
        // Em schemas sem essa tabela: ignorar graciosamente
        try {
            String sql = "INSERT IGNORE INTO avaliacoes (aluguer_id, avaliador_id, avaliado_id, estrelas, comentario, tipo) " +
                         "SELECT al.id, r.locatario_id, v.proprietario_id, ?, ?, 'LOCATARIO' " +
                         "FROM reservas r JOIN alugueres al ON al.reserva_id=r.id JOIN veiculos v ON v.id=r.veiculo_id WHERE r.id=?";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, estrelas);
                ps.setString(2, comentario != null ? comentario : "");
                ps.setInt(3, id);
                ps.executeUpdate();
            }
        } catch (Exception ignored) {}
    }

    public boolean existeSobreposicao(int veiculoId, LocalDate inicio, LocalDate fim) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservas WHERE veiculo_id=? AND estado IN ('PENDENTE','ACEITE') AND data_inicio < ? AND data_fim > ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, veiculoId);
            ps.setDate(2, Date.valueOf(fim));
            ps.setDate(3, Date.valueOf(inicio));
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public Optional<Reserva> findById(int id) throws SQLException {
        String sql = buildJoin("r.id=?");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        }
        return Optional.empty();
    }

    public List<Reserva> findByLocatario(int locatarioId) throws SQLException {
        return query(buildJoin("r.locatario_id=?") + " ORDER BY r.data_pedido DESC", locatarioId);
    }

    public List<Reserva> findByProprietario(int proprietarioId) throws SQLException {
        return query(buildJoin("v.proprietario_id=?") + " ORDER BY r.data_pedido DESC", proprietarioId);
    }

    public List<Reserva> findAll() throws SQLException {
        List<Reserva> list = new ArrayList<>();
        String sql = buildJoin("1=1") + " ORDER BY r.data_pedido DESC";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private List<Reserva> query(String sql, int param) throws SQLException {
        List<Reserva> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private String buildJoin(String where) {
        return "SELECT r.*, v.proprietario_id, v.marca, v.modelo, v.ano, " +
               "ul.nome AS locatario_nome, up.nome AS proprietario_nome " +
               "FROM reservas r " +
               "JOIN veiculos v ON r.veiculo_id=v.id " +
               "JOIN utilizadores ul ON r.locatario_id=ul.id " +
               "JOIN utilizadores up ON v.proprietario_id=up.id " +
               "WHERE " + where;
    }

    private Reserva map(ResultSet rs) throws SQLException {
        Reserva r = new Reserva();
        r.setId(rs.getInt("id"));
        r.setVeiculoId(rs.getInt("veiculo_id"));
        r.setLocatarioId(rs.getInt("locatario_id"));
        r.setProprietarioId(rs.getInt("proprietario_id"));
        r.setDataInicio(rs.getDate("data_inicio").toLocalDate());
        r.setDataFim(rs.getDate("data_fim").toLocalDate());
        r.setEstado(normalizar(rs.getString("estado")));
        BigDecimal renda = rs.getBigDecimal("custo_renda");
        r.setTotal(renda != null ? renda.doubleValue() : 0.0);
        int kmI = rs.getInt("km_inicial"); if (!rs.wasNull()) r.setKmInicial(kmI);
        r.setVeiculoNome(rs.getString("marca") + " " + rs.getString("modelo") + " (" + rs.getInt("ano") + ")");
        r.setLocatarioNome(rs.getString("locatario_nome"));
        r.setProprietarioNome(rs.getString("proprietario_nome"));
        return r;
    }

    private String normalizar(String estado) {
        if (estado == null) return "pendente";
        return switch (estado.toUpperCase()) {
            case "PENDENTE"  -> "pendente";
            case "ACEITE"    -> "confirmada";
            case "REJEITADA", "CANCELADA", "EXPIRADA" -> "cancelada";
            case "CONCLUIDA" -> "concluida";
            default -> estado.toLowerCase();
        };
    }
}
