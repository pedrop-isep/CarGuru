package pt.carguru.Repositories;

import pt.carguru.Models.User;
import pt.carguru.Utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Timestamp;

public class UserRepository {

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM utilizadores WHERE email = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT * FROM utilizadores WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        }
        return Optional.empty();
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM utilizadores ORDER BY nome";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(mapRow(rs));
        }
        return users;
    }

    public User save(User user) throws SQLException {
        String sql = "INSERT INTO utilizadores (nome, email, password_hash, nif, n_carta_conducao, validade_carta, role, saldo, bloqueado) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getNome());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getNif());
            ps.setString(5, user.getNCartaConducao());
            ps.setDate(6, user.getValidadeCarta() != null ? Date.valueOf(user.getValidadeCarta()) : null);
            ps.setString(7, user.getRole() != null ? user.getRole() : "UTILIZADOR");
            ps.setDouble(8, user.getSaldo());
            ps.setBoolean(9, false);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) user.setId(keys.getInt(1));
        }
        return user;
    }

    public void update(User user) throws SQLException {
        String sql = "UPDATE utilizadores SET nome=?, nif=?, saldo=?, ativo=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.getNome());
            ps.setString(2, user.getNif());
            ps.setDouble(3, user.getSaldo());
            ps.setBoolean(4, user.isAtivo());
            ps.setInt(5, user.getId());
            ps.executeUpdate();
        }
    }

    public void updatePassword(int userId, String newHash) throws SQLException {
        String sql = "UPDATE utilizadores SET password_hash=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM utilizadores WHERE email = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public Optional<User> findByResetToken(String token) throws SQLException {
        String sql = "SELECT * FROM utilizadores WHERE token_recuperacao=? AND token_expira_em > NOW()";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        }
        return Optional.empty();
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

    public void clearResetToken(int userId) throws SQLException {
        String sql = "UPDATE utilizadores SET token_recuperacao=NULL, token_expira_em=NULL WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("nif"),
                rs.getString("n_carta_conducao"),
                rs.getDate("validade_carta") != null ? rs.getDate("validade_carta").toLocalDate() : null,
                rs.getString("role"),
                rs.getDouble("saldo"),
                rs.getBoolean("bloqueado"),
                rs.getTimestamp("data_registo") != null ? rs.getTimestamp("data_registo").toLocalDateTime() : null
        );
        user.setTokenRecuperacao(rs.getString("token_recuperacao"));
        Timestamp tokenExpira = rs.getTimestamp("token_expira_em");
        if (tokenExpira != null) user.setTokenExpiraEm(tokenExpira.toLocalDateTime());

        return user;
    }
}
