package pt.carguru.Repositories;

import pt.carguru.Models.Reserva;
import pt.carguru.Utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservaRepository {

    public Reserva save(Reserva r) throws SQLException {
        String sql = "INSERT INTO reservas (veiculo_id, locatario_id, proprietario_id, data_inicio, data_fim, total, estado) VALUES (?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getVeiculoId());
            ps.setInt(2, r.getLocatarioId());
            ps.setInt(3, r.getProprietarioId());
            ps.setDate(4, Date.valueOf(r.getDataInicio()));
            ps.setDate(5, Date.valueOf(r.getDataFim()));
            ps.setDouble(6, r.getTotal());
            ps.setString(7, "pendente");
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) r.setId(keys.getInt(1));
        }
        return r;
    }

    public void updateEstado(int id, String estado) throws SQLException {
        String sql = "UPDATE reservas SET estado=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void updateKmInicial(int id, int km) throws SQLException {
        String sql = "UPDATE reservas SET km_inicial=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, km);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void updateKmFinalELiquidacao(int id, int kmFinal, double custoCombustivel) throws SQLException {
        String sql = "UPDATE reservas SET km_final=?, custo_combustivel=?, estado='concluida' WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, kmFinal);
            ps.setDouble(2, custoCombustivel);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void updateAvaliacao(int id, int estrelas, String comentario) throws SQLException {
        String sql = "UPDATE reservas SET avaliacao=?, comentario_avaliacao=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, estrelas);
            ps.setString(2, comentario);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public List<Reserva> findByLocatario(int locatarioId) throws SQLException {
        return findWithJoin("r.locatario_id=?", locatarioId);
    }

    public List<Reserva> findByProprietario(int proprietarioId) throws SQLException {
        return findWithJoin("r.proprietario_id=?", proprietarioId);
    }

    public List<Reserva> findAll() throws SQLException {
        String sql = buildJoinSql("1=1") + " ORDER BY r.data_inicio DESC";
        List<Reserva> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Optional<Reserva> findById(int id) throws SQLException {
        String sql = buildJoinSql("r.id=?");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        }
        return Optional.empty();
    }

    public boolean existeSobreposicao(int veiculoId, LocalDate inicio, LocalDate fim) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservas WHERE veiculo_id=? AND estado IN ('pendente','confirmada') AND data_inicio < ? AND data_fim > ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, veiculoId);
            ps.setDate(2, Date.valueOf(fim));
            ps.setDate(3, Date.valueOf(inicio));
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private List<Reserva> findWithJoin(String condition, int param) throws SQLException {
        List<Reserva> list = new ArrayList<>();
        String sql = buildJoinSql(condition) + " ORDER BY r.data_inicio DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private String buildJoinSql(String condition) {
        return "SELECT r.*, v.marca, v.modelo, v.ano, ul.nome as locatario_nome, up.nome as proprietario_nome " +
               "FROM reservas r " +
               "JOIN veiculos v ON r.veiculo_id=v.id " +
               "JOIN utilizadores ul ON r.locatario_id=ul.id " +
               "JOIN utilizadores up ON r.proprietario_id=up.id " +
               "WHERE " + condition;
    }

    private Reserva mapRow(ResultSet rs) throws SQLException {
        Reserva r = new Reserva();
        r.setId(rs.getInt("id"));
        r.setVeiculoId(rs.getInt("veiculo_id"));
        r.setLocatarioId(rs.getInt("locatario_id"));
        r.setProprietarioId(rs.getInt("proprietario_id"));
        r.setDataInicio(rs.getDate("data_inicio").toLocalDate());
        r.setDataFim(rs.getDate("data_fim").toLocalDate());
        r.setTotal(rs.getDouble("total"));
        r.setEstado(rs.getString("estado"));
        int kmIni = rs.getInt("km_inicial"); if (!rs.wasNull()) r.setKmInicial(kmIni);
        int kmFin = rs.getInt("km_final"); if (!rs.wasNull()) r.setKmFinal(kmFin);
        double cc = rs.getDouble("custo_combustivel"); if (!rs.wasNull()) r.setCustoCombustivel(cc);
        int aval = rs.getInt("avaliacao"); if (!rs.wasNull()) r.setAvaliacao(aval);
        r.setComentarioAvaliacao(rs.getString("comentario_avaliacao"));
        r.setVeiculoNome(rs.getString("marca") + " " + rs.getString("modelo") + " (" + rs.getInt("ano") + ")");
        r.setLocatarioNome(rs.getString("locatario_nome"));
        r.setProprietarioNome(rs.getString("proprietario_nome"));
        return r;
    }
}
