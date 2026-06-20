package pt.carguru.Repositories;

import pt.carguru.Models.Bloqueio;
import pt.carguru.Utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BloqueioRepository {

    /** Regista uma ação de bloqueio/desbloqueio no histórico. */
    public Bloqueio save(Bloqueio b) throws SQLException {
        String sql = "INSERT INTO bloqueios_utilizador (utilizador_id, admin_id, acao, motivo) VALUES (?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getUtilizadorId());
            ps.setInt(2, b.getAdminId());
            ps.setString(3, b.getAcao());
            ps.setString(4, b.getMotivo());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) b.setId(keys.getInt(1));
        }
        return b;
    }

    /** Histórico de bloqueios/desbloqueios de um utilizador específico, mais recente primeiro. */
    public List<Bloqueio> findByUtilizador(int utilizadorId) throws SQLException {
        List<Bloqueio> list = new ArrayList<>();
        String sql = "SELECT bu.*, a.nome AS admin_nome " +
                "FROM bloqueios_utilizador bu " +
                "JOIN utilizadores a ON a.id = bu.admin_id " +
                "WHERE bu.utilizador_id=? ORDER BY bu.data DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, utilizadorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Histórico global de bloqueios/desbloqueios de todos os utilizadores, mais recente primeiro. */
    public List<Bloqueio> findAll() throws SQLException {
        List<Bloqueio> list = new ArrayList<>();
        String sql = "SELECT bu.*, a.nome AS admin_nome " +
                "FROM bloqueios_utilizador bu " +
                "JOIN utilizadores a ON a.id = bu.admin_id " +
                "ORDER BY bu.data DESC";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private Bloqueio map(ResultSet rs) throws SQLException {
        Bloqueio b = new Bloqueio();
        b.setId(rs.getInt("id"));
        b.setUtilizadorId(rs.getInt("utilizador_id"));
        b.setAdminId(rs.getInt("admin_id"));
        b.setAcao(rs.getString("acao"));
        b.setMotivo(rs.getString("motivo"));
        Timestamp d = rs.getTimestamp("data");
        if (d != null) b.setData(d.toLocalDateTime());
        b.setAdminNome(rs.getString("admin_nome"));
        return b;
    }
}
