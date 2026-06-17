package pt.carguru.Repositories;

import pt.carguru.Models.User;
import pt.carguru.Utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM utilizadores WHERE email=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT * FROM utilizadores WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        }
        return Optional.empty();
    }

    public List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM utilizadores ORDER BY nome";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM utilizadores WHERE email=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public User save(User u) throws SQLException {
        String sql = "INSERT INTO utilizadores (email,nome,nif,n_carta_conducao,validade_carta,password_hash,role) VALUES (?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getEmail());
            ps.setString(2, u.getNome());
            ps.setString(3, u.getNif() != null ? u.getNif() : "000000000");
            ps.setString(4, u.getNCartaConducao() != null ? u.getNCartaConducao() : "N/A");
            ps.setDate(5, u.getValidadeCarta() != null ? Date.valueOf(u.getValidadeCarta()) : Date.valueOf("2030-01-01"));
            ps.setString(6, u.getPasswordHash());
            ps.setString(7, u.getRole() != null ? u.getRole() : "UTILIZADOR");
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) u.setId(keys.getInt(1));
        }
        return u;
    }

    public void update(User u) throws SQLException {
        String sql = "UPDATE utilizadores SET nome=?, nif=?, saldo=?, bloqueado=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getNome());
            ps.setString(2, u.getNif() != null ? u.getNif() : "000000000");
            ps.setBigDecimal(3, java.math.BigDecimal.valueOf(u.getSaldo()));
            ps.setBoolean(4, u.isBloqueado());
            ps.setInt(5, u.getId());
            ps.executeUpdate();
        }
    }

    /** Adiciona (positivo) ou subtrai (negativo) saldo de forma atómica. */
    public void updateSaldo(int userId, double delta) throws SQLException {
        String sql = "UPDATE utilizadores SET saldo = saldo + ? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, java.math.BigDecimal.valueOf(delta));
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateCarta(User u) throws SQLException {
        String sql = "UPDATE utilizadores SET n_carta_conducao=?, validade_carta=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getNCartaConducao());
            ps.setDate(2, u.getValidadeCarta() != null ? Date.valueOf(u.getValidadeCarta()) : null);
            ps.setInt(3, u.getId());
            ps.executeUpdate();
        }
    }

    public void updatePassword(int userId, String hash) throws SQLException {
        String sql = "UPDATE utilizadores SET password_hash=?, token_recuperacao=NULL, token_expira_em=NULL WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void saveResetToken(int userId, String token, LocalDateTime expiry) throws SQLException {
        String sql = "UPDATE utilizadores SET token_recuperacao=?, token_expira_em=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setTimestamp(2, Timestamp.valueOf(expiry));
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public Optional<User> findByResetToken(String token) throws SQLException {
        String sql = "SELECT * FROM utilizadores WHERE token_recuperacao=? AND token_expira_em > NOW()";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        }
        return Optional.empty();
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setNome(rs.getString("nome"));
        u.setNif(rs.getString("nif"));
        u.setNCartaConducao(rs.getString("n_carta_conducao"));
        Date vc = rs.getDate("validade_carta");
        if (vc != null) u.setValidadeCarta(vc.toLocalDate());
        u.setPasswordHash(rs.getString("password_hash"));
        java.math.BigDecimal saldo = rs.getBigDecimal("saldo");
        u.setSaldo(saldo != null ? saldo.doubleValue() : 0.0);
        u.setRole(rs.getString("role"));
        u.setBloqueado(rs.getBoolean("bloqueado"));
        Timestamp dr = rs.getTimestamp("data_registo");
        if (dr != null) u.setDataRegisto(dr.toLocalDateTime());
        u.setTokenRecuperacao(rs.getString("token_recuperacao"));
        Timestamp te = rs.getTimestamp("token_expira_em");
        if (te != null) u.setTokenExpiraEm(te.toLocalDateTime());
        return u;
    }
}
