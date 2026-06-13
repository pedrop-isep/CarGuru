package pt.carguru.Repositories;

import pt.carguru.Models.Veiculo;
import pt.carguru.Utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VeiculoRepository {

    public Veiculo save(Veiculo v) throws SQLException {
        String sql = "INSERT INTO veiculos (proprietario_id, marca, modelo, ano, combustivel, transmissao, localizacao, preco_por_dia, consumo, descricao, estado, data_criacao) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, v.getProprietarioId());
            ps.setString(2, v.getMarca());
            ps.setString(3, v.getModelo());
            ps.setInt(4, v.getAno());
            ps.setString(5, v.getCombustivel());
            ps.setString(6, v.getTransmissao());
            ps.setString(7, v.getLocalizacao());
            ps.setDouble(8, v.getPrecoPorDia());
            ps.setDouble(9, v.getConsumo());
            ps.setString(10, v.getDescricao());
            ps.setString(11, "pendente");
            ps.setDate(12, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) v.setId(keys.getInt(1));
        }
        return v;
    }

    public void update(Veiculo v) throws SQLException {
        String sql = "UPDATE veiculos SET marca=?, modelo=?, ano=?, combustivel=?, transmissao=?, localizacao=?, preco_por_dia=?, consumo=?, descricao=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.getMarca());
            ps.setString(2, v.getModelo());
            ps.setInt(3, v.getAno());
            ps.setString(4, v.getCombustivel());
            ps.setString(5, v.getTransmissao());
            ps.setString(6, v.getLocalizacao());
            ps.setDouble(7, v.getPrecoPorDia());
            ps.setDouble(8, v.getConsumo());
            ps.setString(9, v.getDescricao());
            ps.setInt(10, v.getId());
            ps.executeUpdate();
        }
    }

    public void updateEstado(int id, String estado) throws SQLException {
        String sql = "UPDATE veiculos SET estado=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM veiculos WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<Veiculo> findById(int id) throws SQLException {
        String sql = "SELECT v.*, u.nome as proprietario_nome FROM veiculos v JOIN utilizadores u ON v.proprietario_id=u.id WHERE v.id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        }
        return Optional.empty();
    }

    public List<Veiculo> findByProprietario(int proprietarioId) throws SQLException {
        List<Veiculo> list = new ArrayList<>();
        String sql = "SELECT v.*, u.nome as proprietario_nome FROM veiculos v JOIN utilizadores u ON v.proprietario_id=u.id WHERE v.proprietario_id=? ORDER BY v.marca";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, proprietarioId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Veiculo> findAprovados(String marca, String combustivel, String transmissao, String localizacao, double precoMax) throws SQLException {
        List<Veiculo> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT v.*, u.nome as proprietario_nome FROM veiculos v JOIN utilizadores u ON v.proprietario_id=u.id WHERE v.estado='aprovado'");
        List<Object> params = new ArrayList<>();
        if (marca != null && !marca.isBlank()) { sql.append(" AND v.marca LIKE ?"); params.add("%" + marca + "%"); }
        if (combustivel != null && !combustivel.isBlank()) { sql.append(" AND v.combustivel=?"); params.add(combustivel); }
        if (transmissao != null && !transmissao.isBlank()) { sql.append(" AND v.transmissao=?"); params.add(transmissao); }
        if (localizacao != null && !localizacao.isBlank()) { sql.append(" AND v.localizacao LIKE ?"); params.add("%" + localizacao + "%"); }
        if (precoMax > 0) { sql.append(" AND v.preco_por_dia <= ?"); params.add(precoMax); }
        sql.append(" ORDER BY v.marca, v.modelo");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Veiculo> findPendentes() throws SQLException {
        List<Veiculo> list = new ArrayList<>();
        String sql = "SELECT v.*, u.nome as proprietario_nome FROM veiculos v JOIN utilizadores u ON v.proprietario_id=u.id WHERE v.estado='pendente' ORDER BY v.data_criacao";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    private Veiculo mapRow(ResultSet rs) throws SQLException {
        Veiculo v = new Veiculo();
        v.setId(rs.getInt("id"));
        v.setProprietarioId(rs.getInt("proprietario_id"));
        v.setProprietarioNome(rs.getString("proprietario_nome"));
        v.setMarca(rs.getString("marca"));
        v.setModelo(rs.getString("modelo"));
        v.setAno(rs.getInt("ano"));
        v.setCombustivel(rs.getString("combustivel"));
        v.setTransmissao(rs.getString("transmissao"));
        v.setLocalizacao(rs.getString("localizacao"));
        v.setPrecoPorDia(rs.getDouble("preco_por_dia"));
        v.setConsumo(rs.getDouble("consumo"));
        v.setDescricao(rs.getString("descricao"));
        v.setEstado(rs.getString("estado"));
        Date d = rs.getDate("data_criacao");
        if (d != null) v.setDataCriacao(d.toLocalDate());
        return v;
    }
}
