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
        // 1. Buscar dados da reserva numa única ligação transacional
        String sqlSelect =
            "SELECT r.km_inicial, r.custo_renda, r.caucao, r.locatario_id, v.proprietario_id, v.consumo_medio " +
            "FROM reservas r JOIN veiculos v ON r.veiculo_id = v.id WHERE r.id=?";

        int kmInicial; double custoRenda; double caucao; int locatarioId; int proprietarioId; double consumo;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sqlSelect)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new SQLException("Reserva não encontrada.");
            kmInicial      = rs.getInt("km_inicial");
            custoRenda     = rs.getBigDecimal("custo_renda").doubleValue();
            caucao         = rs.getBigDecimal("caucao").doubleValue();
            locatarioId    = rs.getInt("locatario_id");
            proprietarioId = rs.getInt("proprietario_id");
            consumo        = rs.getBigDecimal("consumo_medio").doubleValue();
        }

        // 2. Calcular custo combustível: ((km_final - km_inicial) / 100) × consumo × preço_combustível
        int kmsPercorridos = kmFinal - kmInicial;
        double custoCombustivel = (kmsPercorridos / 100.0) * consumo * precoComb;
        double custoTotal = custoRenda + custoCombustivel;

        // 3. Guardar km_final e marcar CONCLUIDA
        String sqlUpdate = "UPDATE reservas SET estado='CONCLUIDA', km_inicial=km_inicial WHERE id=?";
        // Guardar km_final na tabela alugueres (ou adicionar coluna reservas se não existir)
        // A tabela alugueres tem km_final — registar aluguer se ainda não existir
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // 3a. Atualizar estado da reserva
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE reservas SET estado='CONCLUIDA' WHERE id=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                // 3b. Inserir registo na tabela alugueres (se ainda não existir)
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO alugueres (reserva_id, km_inicial, km_final, custo_renda, " +
                        "custo_combustivel, custo_total, preco_combustivel_fim, caucao_devolvida) " +
                        "VALUES (?,?,?,?,?,?,?,1) " +
                        "ON DUPLICATE KEY UPDATE km_final=VALUES(km_final), custo_combustivel=VALUES(custo_combustivel), " +
                        "custo_total=VALUES(custo_total), preco_combustivel_fim=VALUES(preco_combustivel_fim), caucao_devolvida=1")) {
                    ps.setInt(1, id);
                    ps.setInt(2, kmInicial);
                    ps.setInt(3, kmFinal);
                    ps.setBigDecimal(4, java.math.BigDecimal.valueOf(custoRenda));
                    ps.setBigDecimal(5, java.math.BigDecimal.valueOf(custoCombustivel));
                    ps.setBigDecimal(6, java.math.BigDecimal.valueOf(custoTotal));
                    ps.setBigDecimal(7, java.math.BigDecimal.valueOf(precoComb));
                    ps.executeUpdate();
                }

                // 3c. Débito ao locatário: custo_total + caucao - caucao_devolvida = custo_total
                // (a caução foi retida na criação, agora deduzimos o restante e devolvemos caução)
                // Débito = custo_total (a caução de 20% já foi reservada mas não debitada ainda)
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE utilizadores SET saldo = saldo - ? WHERE id=?")) {
                    ps.setBigDecimal(1, java.math.BigDecimal.valueOf(custoTotal));
                    ps.setInt(2, locatarioId);
                    ps.executeUpdate();
                }

                // 3d. Crédito ao proprietário: custo_renda (sem combustível, que fica com o locatário)
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE utilizadores SET saldo = saldo + ? WHERE id=?")) {
                    ps.setBigDecimal(1, java.math.BigDecimal.valueOf(custoRenda));
                    ps.setInt(2, proprietarioId);
                    ps.executeUpdate();
                }

                // 3e. Devolver caução ao locatário
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE utilizadores SET saldo = saldo + ? WHERE id=?")) {
                    ps.setBigDecimal(1, java.math.BigDecimal.valueOf(caucao));
                    ps.setInt(2, locatarioId);
                    ps.executeUpdate();
                }

                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
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
            // Recalcular e persistir avaliacao_media / n_avaliacoes do veículo,
            // já que o cartão de listagem lê estas colunas (cache) em vez de agregar em tempo real.
            atualizarAvaliacaoCacheVeiculo(id);
        } catch (Exception ignored) {}
    }

    /** Recalcula a média e contagem de avaliações do veículo associado à reserva
     *  e grava o resultado em veiculos.avaliacao_media / veiculos.n_avaliacoes. */
    private void atualizarAvaliacaoCacheVeiculo(int reservaId) throws SQLException {
        String sql =
            "UPDATE veiculos v " +
            "JOIN reservas r ON r.veiculo_id = v.id " +
            "SET v.avaliacao_media = COALESCE((" +
            "      SELECT AVG(av.estrelas) FROM avaliacoes av " +
            "      JOIN alugueres al2 ON av.aluguer_id = al2.id " +
            "      JOIN reservas r2 ON al2.reserva_id = r2.id " +
            "      WHERE r2.veiculo_id = v.id" +
            "    ), 0), " +
            "    v.n_avaliacoes = (" +
            "      SELECT COUNT(*) FROM avaliacoes av " +
            "      JOIN alugueres al2 ON av.aluguer_id = al2.id " +
            "      JOIN reservas r2 ON al2.reserva_id = r2.id " +
            "      WHERE r2.veiculo_id = v.id" +
            "    ) " +
            "WHERE r.id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservaId);
            ps.executeUpdate();
        }
    }

    /** Obtém o preço corrente do combustível para um tipo específico (tabela precos_combustivel).
     *  Devolve valor padrão se a tabela não existir ou estiver vazia. */
    public double getPrecoAtualCombustivel(String tipoCombustivel) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT preco_corrente FROM precos_combustivel WHERE tipo_combustivel=? ORDER BY ultima_atualizacao DESC LIMIT 1")) {
            ps.setString(1, tipoCombustivel != null ? tipoCombustivel.toUpperCase() : "GASOLINA");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal("preco_corrente").doubleValue();
        } catch (Exception ignored) {}
        // Defaults caso a tabela não tenha dados
        if (tipoCombustivel == null) return 1.70;
        return switch (tipoCombustivel.toUpperCase()) {
            case "GASOLEO"  -> 1.60;
            case "ELETRICO" -> 0.20;
            case "GPL"      -> 0.90;
            default         -> 1.70; // GASOLINA
        };
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
        return "SELECT r.*, v.proprietario_id, v.marca, v.modelo, v.ano, v.tipo_combustivel, " +
               "ul.nome AS locatario_nome, up.nome AS proprietario_nome, " +
               "al.km_final, " +
               "av.estrelas AS avaliacao_estrelas " +
               "FROM reservas r " +
               "JOIN veiculos v ON r.veiculo_id=v.id " +
               "JOIN utilizadores ul ON r.locatario_id=ul.id " +
               "JOIN utilizadores up ON v.proprietario_id=up.id " +
               "LEFT JOIN alugueres al ON al.reserva_id=r.id " +
               "LEFT JOIN avaliacoes av ON av.aluguer_id=al.id AND av.avaliador_id=r.locatario_id " +
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
        try { int kmF = rs.getInt("km_final"); if (!rs.wasNull()) r.setKmFinal(kmF); } catch (Exception ignored) {}
        try { BigDecimal cau = rs.getBigDecimal("caucao"); if (cau != null) r.setCaucao(cau.doubleValue()); } catch (Exception ignored) {}
        try { r.setCombustivelVeiculo(rs.getString("tipo_combustivel")); } catch (Exception ignored) {}
        try { int av = rs.getInt("avaliacao_estrelas"); if (!rs.wasNull()) r.setAvaliacao(av); } catch (Exception ignored) {}
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
