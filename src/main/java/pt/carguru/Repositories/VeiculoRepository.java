package pt.carguru.Repositories;

import pt.carguru.Models.Veiculo;
import pt.carguru.Utils.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VeiculoRepository {

    public Veiculo save(Veiculo v) throws SQLException {
        String sql = "INSERT INTO veiculos (proprietario_id, matricula, marca, modelo, ano, tipo_combustivel, consumo_medio, tipo_transmissao, lotacao, quilometragem, preco_dia_base, cidade, distrito, codigo_postal) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, v.getProprietarioId());
            ps.setString(2, v.getMatricula() != null ? v.getMatricula() : "XX-00-XX");
            ps.setString(3, v.getMarca());
            ps.setString(4, v.getModelo());
            ps.setInt(5, v.getAno());
            ps.setString(6, normCombustivel(v.getCombustivel()));
            ps.setBigDecimal(7, BigDecimal.valueOf(v.getConsumo() > 0 ? v.getConsumo() : 6.0));
            ps.setString(8, normTransmissao(v.getTransmissao()));
            ps.setInt(9, v.getLotacao() > 0 ? v.getLotacao() : 5);
            ps.setInt(10, v.getQuilometragem());
            ps.setBigDecimal(11, BigDecimal.valueOf(v.getPrecoPorDia()));
            ps.setString(12, v.getLocalizacao() != null ? v.getLocalizacao() : "");
            ps.setString(13, v.getDistrito() != null ? v.getDistrito() : v.getLocalizacao());
            ps.setString(14, v.getCodigoPostal() != null ? v.getCodigoPostal() : "0000-000");
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) v.setId(keys.getInt(1));
        }
        return v;
    }

    public void update(Veiculo v) throws SQLException {
        String sql = "UPDATE veiculos SET matricula=?, marca=?, modelo=?, ano=?, tipo_combustivel=?, consumo_medio=?, tipo_transmissao=?, lotacao=?, quilometragem=?, preco_dia_base=?, cidade=?, distrito=?, codigo_postal=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.getMatricula() != null ? v.getMatricula() : "XX-00-XX");
            ps.setString(2, v.getMarca());
            ps.setString(3, v.getModelo());
            ps.setInt(4, v.getAno());
            ps.setString(5, normCombustivel(v.getCombustivel()));
            ps.setBigDecimal(6, BigDecimal.valueOf(v.getConsumo() > 0 ? v.getConsumo() : 6.0));
            ps.setString(7, normTransmissao(v.getTransmissao()));
            ps.setInt(8, v.getLotacao() > 0 ? v.getLotacao() : 5);
            ps.setInt(9, v.getQuilometragem());
            ps.setBigDecimal(10, BigDecimal.valueOf(v.getPrecoPorDia()));
            ps.setString(11, v.getLocalizacao());
            ps.setString(12, v.getDistrito() != null ? v.getDistrito() : v.getLocalizacao());
            ps.setString(13, v.getCodigoPostal() != null ? v.getCodigoPostal() : "0000-000");
            ps.setInt(14, v.getId());
            ps.executeUpdate();
        }
    }

    public void updateEstado(int id, String estado) throws SQLException {
        boolean validado = "DISPONIVEL".equals(estado);
        String sql = "UPDATE veiculos SET estado=?, validado=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setBoolean(2, validado);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    // Soft delete — estado = REMOVIDO
    public void delete(int id) throws SQLException { updateEstado(id, "REMOVIDO"); }

    public Optional<Veiculo> findById(int id) throws SQLException {
        String sql = "SELECT v.*, u.nome AS proprietario_nome FROM veiculos v JOIN utilizadores u ON v.proprietario_id=u.id WHERE v.id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        }
        return Optional.empty();
    }

    public List<Veiculo> findByProprietario(int proprietarioId) throws SQLException {
        List<Veiculo> list = new ArrayList<>();
        String sql = "SELECT v.*, u.nome AS proprietario_nome FROM veiculos v JOIN utilizadores u ON v.proprietario_id=u.id WHERE v.proprietario_id=? AND v.estado!='REMOVIDO' ORDER BY v.marca";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, proprietarioId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Veiculo> findAprovados(String marca, String combustivel, String transmissao,
                                        String localizacao, double precoMax) throws SQLException {
        List<Veiculo> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT v.*, u.nome AS proprietario_nome FROM veiculos v JOIN utilizadores u ON v.proprietario_id=u.id WHERE v.estado='DISPONIVEL' AND v.validado=1");
        List<Object> params = new ArrayList<>();
        if (marca != null && !marca.isBlank()) {
            sql.append(" AND (v.marca LIKE ? OR v.modelo LIKE ?)");
            params.add("%" + marca + "%"); params.add("%" + marca + "%");
        }
        if (combustivel != null && !combustivel.isBlank()) {
            sql.append(" AND v.tipo_combustivel=?"); params.add(combustivel.toUpperCase());
        }
        if (transmissao != null && !transmissao.isBlank()) {
            sql.append(" AND v.tipo_transmissao=?"); params.add(transmissao.toUpperCase());
        }
        if (localizacao != null && !localizacao.isBlank()) {
            sql.append(" AND (v.cidade LIKE ? OR v.distrito LIKE ?)");
            params.add("%" + localizacao + "%"); params.add("%" + localizacao + "%");
        }
        if (precoMax > 0) { sql.append(" AND v.preco_dia_base<=?"); params.add(precoMax); }
        sql.append(" ORDER BY v.avaliacao_media DESC, v.preco_dia_base ASC");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Veiculo> findPendentes() throws SQLException {
        List<Veiculo> list = new ArrayList<>();
        String sql = "SELECT v.*, u.nome AS proprietario_nome FROM veiculos v JOIN utilizadores u ON v.proprietario_id=u.id WHERE v.estado='PENDENTE_VALIDACAO' ORDER BY v.data_criacao";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Veiculo> findDisponiveisPorDatas(String marca, String combustivel, String transmissao,
            String localizacao, double precoMax, java.time.LocalDate dataInicio, java.time.LocalDate dataFim) throws SQLException {
        List<Veiculo> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT v.*, u.nome AS proprietario_nome FROM veiculos v JOIN utilizadores u ON v.proprietario_id=u.id " +
            "WHERE v.estado='DISPONIVEL' AND v.validado=1 " +
            // Sem indisponibilidade que sobreponha as datas
            "AND NOT EXISTS (SELECT 1 FROM indisponibilidades i WHERE i.veiculo_id=v.id AND i.data_inicio < ? AND i.data_fim > ?) " +
            // Sem reserva ACEITE/CONFIRMADA que sobreponha as datas
            "AND NOT EXISTS (SELECT 1 FROM reservas r WHERE r.veiculo_id=v.id AND r.estado IN ('ACEITE','confirmada','CONFIRMADA') AND r.data_inicio < ? AND r.data_fim > ?)");
        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(dataFim));
        params.add(Date.valueOf(dataInicio));
        params.add(Date.valueOf(dataFim));
        params.add(Date.valueOf(dataInicio));
        if (marca != null && !marca.isBlank()) {
            sql.append(" AND (v.marca LIKE ? OR v.modelo LIKE ?)");
            params.add("%" + marca + "%"); params.add("%" + marca + "%");
        }
        if (combustivel != null && !combustivel.isBlank()) {
            sql.append(" AND v.tipo_combustivel=?"); params.add(combustivel.toUpperCase());
        }
        if (transmissao != null && !transmissao.isBlank()) {
            sql.append(" AND v.tipo_transmissao=?"); params.add(transmissao.toUpperCase());
        }
        if (localizacao != null && !localizacao.isBlank()) {
            sql.append(" AND (v.cidade LIKE ? OR v.distrito LIKE ?)");
            params.add("%" + localizacao + "%"); params.add("%" + localizacao + "%");
        }
        if (precoMax > 0) { sql.append(" AND v.preco_dia_base<=?"); params.add(precoMax); }
        sql.append(" ORDER BY v.avaliacao_media DESC, v.preco_dia_base ASC");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String) ps.setString(i + 1, (String) p);
                else if (p instanceof Double) ps.setDouble(i + 1, (Double) p);
                else if (p instanceof Date) ps.setDate(i + 1, (Date) p);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Veiculo> findAll() throws SQLException {
        List<Veiculo> list = new ArrayList<>();
        String sql = "SELECT v.*, u.nome AS proprietario_nome FROM veiculos v JOIN utilizadores u ON v.proprietario_id=u.id WHERE v.estado!='REMOVIDO' ORDER BY v.data_criacao DESC";
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private Veiculo map(ResultSet rs) throws SQLException {
        Veiculo v = new Veiculo();
        v.setId(rs.getInt("id"));
        v.setProprietarioId(rs.getInt("proprietario_id"));
        v.setProprietarioNome(rs.getString("proprietario_nome"));
        v.setMatricula(rs.getString("matricula"));
        v.setMarca(rs.getString("marca"));
        v.setModelo(rs.getString("modelo"));
        v.setAno(rs.getInt("ano"));
        v.setCombustivel(rs.getString("tipo_combustivel"));
        v.setTransmissao(rs.getString("tipo_transmissao"));
        v.setLocalizacao(rs.getString("cidade"));
        v.setDistrito(rs.getString("distrito"));
        v.setCodigoPostal(rs.getString("codigo_postal"));
        v.setLotacao(rs.getInt("lotacao"));
        v.setQuilometragem(rs.getInt("quilometragem"));
        BigDecimal preco = rs.getBigDecimal("preco_dia_base");
        v.setPrecoPorDia(preco != null ? preco.doubleValue() : 0);
        BigDecimal consumo = rs.getBigDecimal("consumo_medio");
        v.setConsumo(consumo != null ? consumo.doubleValue() : 6.0);
        v.setEstado(rs.getString("estado"));
        BigDecimal aval = rs.getBigDecimal("avaliacao_media");
        v.setAvaliacaoMedia(aval != null ? aval.doubleValue() : 0);
        v.setNAvaliacoes(rs.getInt("n_avaliacoes"));
        Timestamp dc = rs.getTimestamp("data_criacao");
        if (dc != null) v.setDataCriacao(dc.toLocalDateTime().toLocalDate());
        // Imagem guardada localmente como ficheiro, path derivado do id
        return v;
    }

    /** Utilitário: devolve o caminho local da imagem dum veículo (se existir) */
    public static String resolverPathImagem(int veiculoId) {
        return System.getProperty("user.home") + "/.carguru/veiculo_" + veiculoId + ".jpg";
    }

    private String normCombustivel(String c) {
        if (c == null) return "GASOLINA";
        return switch (c.toUpperCase()) {
            case "GASOLEO", "DIESEL" -> "GASOLEO";
            case "ELETRICO", "ELÉTRICO", "ELECTRICO" -> "ELETRICO";
            case "GPL" -> "GPL";
            default -> "GASOLINA";
        };
    }

    private String normTransmissao(String t) {
        if (t == null) return "MANUAL";
        return t.toUpperCase().contains("AUTO") ? "AUTOMATICA" : "MANUAL";
    }
}
