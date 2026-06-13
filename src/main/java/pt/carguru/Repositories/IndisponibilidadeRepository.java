package pt.carguru.Repositories;

import pt.carguru.Models.Indisponibilidade;
import pt.carguru.Utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class IndisponibilidadeRepository {

    public void save(Indisponibilidade ind) throws SQLException {
        String sql = "INSERT INTO indisponibilidades (veiculo_id, inicio, fim) VALUES (?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ind.getVeiculoId());
            ps.setDate(2, Date.valueOf(ind.getInicio()));
            ps.setDate(3, Date.valueOf(ind.getFim()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) ind.setId(keys.getInt(1));
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM indisponibilidades WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Indisponibilidade> findByVeiculo(int veiculoId) throws SQLException {
        List<Indisponibilidade> list = new ArrayList<>();
        String sql = "SELECT * FROM indisponibilidades WHERE veiculo_id=? ORDER BY inicio";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, veiculoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Indisponibilidade ind = new Indisponibilidade();
                ind.setId(rs.getInt("id"));
                ind.setVeiculoId(veiculoId);
                ind.setInicio(rs.getDate("inicio").toLocalDate());
                ind.setFim(rs.getDate("fim").toLocalDate());
                list.add(ind);
            }
        }
        return list;
    }

    public boolean estaIndisponivel(int veiculoId, LocalDate inicio, LocalDate fim) throws SQLException {
        String sql = "SELECT COUNT(*) FROM indisponibilidades WHERE veiculo_id=? AND inicio < ? AND fim > ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, veiculoId);
            ps.setDate(2, Date.valueOf(fim));
            ps.setDate(3, Date.valueOf(inicio));
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}
