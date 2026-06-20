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

    /** Rejeita o pedido de reserva, guardando o motivo (reutiliza a coluna motivo_cancelamento). */
    public void rejeitar(int id, String motivo) throws SQLException {
        String sql = "UPDATE reservas SET estado='REJEITADA', data_cancelamento=NOW(), motivo_cancelamento=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, motivo);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Reservas ACEITES cujo aluguer começa exatamente na data indicada e cujo
     * lembrete de início ainda não foi enviado. Usado pelo LembreteScheduler.
     */
    public List<Reserva> findParaLembreteInicio(LocalDate data) throws SQLException {
        String sql = buildJoin("r.estado='ACEITE' AND r.data_inicio=? AND r.lembrete_inicio_enviado=0");
        List<Reserva> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(data));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /**
     * Reservas ACEITES cujo aluguer termina exatamente na data indicada e cujo
     * lembrete de devolução ainda não foi enviado. Usado pelo LembreteScheduler.
     */
    public List<Reserva> findParaLembreteFim(LocalDate data) throws SQLException {
        String sql = buildJoin("r.estado='ACEITE' AND r.data_fim=? AND r.lembrete_fim_enviado=0");
        List<Reserva> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(data));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void marcarLembreteInicioEnviado(int id) throws SQLException {
        marcarFlag(id, "lembrete_inicio_enviado");
    }

    public void marcarLembreteFimEnviado(int id) throws SQLException {
        marcarFlag(id, "lembrete_fim_enviado");
    }

    private void marcarFlag(int id, String coluna) throws SQLException {
        String sql = "UPDATE reservas SET " + coluna + "=1 WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
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

    public void updateKmFinalELiquidacao(int id, int kmFinal, double precoComb, boolean incidente) throws SQLException {
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
        // A caução só é devolvida automaticamente se não houver incidente reportado.
        boolean caucaoDevolvida = !incidente;

        // 3. Guardar km_final, registo do aluguer e marcar CONCLUIDA
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
                        "custo_combustivel, custo_total, preco_combustivel_fim, caucao_devolvida, incidente) " +
                        "VALUES (?,?,?,?,?,?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE km_final=VALUES(km_final), custo_combustivel=VALUES(custo_combustivel), " +
                        "custo_total=VALUES(custo_total), preco_combustivel_fim=VALUES(preco_combustivel_fim), " +
                        "caucao_devolvida=VALUES(caucao_devolvida), incidente=VALUES(incidente)")) {
                    ps.setInt(1, id);
                    ps.setInt(2, kmInicial);
                    ps.setInt(3, kmFinal);
                    ps.setBigDecimal(4, java.math.BigDecimal.valueOf(custoRenda));
                    ps.setBigDecimal(5, java.math.BigDecimal.valueOf(custoCombustivel));
                    ps.setBigDecimal(6, java.math.BigDecimal.valueOf(custoTotal));
                    ps.setBigDecimal(7, java.math.BigDecimal.valueOf(precoComb));
                    ps.setBoolean(8, caucaoDevolvida);
                    ps.setBoolean(9, incidente);
                    ps.executeUpdate();
                }

                // 3c. Débito ao locatário: custo_total (renda + combustível)
                double saldoLocatarioApos;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE utilizadores SET saldo = saldo - ? WHERE id=?")) {
                    ps.setBigDecimal(1, java.math.BigDecimal.valueOf(custoTotal));
                    ps.setInt(2, locatarioId);
                    ps.executeUpdate();
                }
                saldoLocatarioApos = lerSaldo(c, locatarioId);
                inserirTransacao(c, locatarioId, "PAGAMENTO_ALUGUER", custoTotal,
                        String.format("Pagamento do aluguer #%d (renda + combustível)", id),
                        saldoLocatarioApos, id, "reserva");

                // 3d. Crédito ao proprietário: custo_renda (sem combustível, que fica com o locatário)
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE utilizadores SET saldo = saldo + ? WHERE id=?")) {
                    ps.setBigDecimal(1, java.math.BigDecimal.valueOf(custoRenda));
                    ps.setInt(2, proprietarioId);
                    ps.executeUpdate();
                }
                inserirTransacao(c, proprietarioId, "RECEITA_ALUGUER", custoRenda,
                        String.format("Receita do aluguer #%d", id),
                        lerSaldo(c, proprietarioId), id, "reserva");

                // 3e. Devolver caução ao locatário — apenas se não houver incidente reportado.
                //     Em caso de incidente, a caução fica retida (para análise/disputa).
                if (caucaoDevolvida) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE utilizadores SET saldo = saldo + ? WHERE id=?")) {
                        ps.setBigDecimal(1, java.math.BigDecimal.valueOf(caucao));
                        ps.setInt(2, locatarioId);
                        ps.executeUpdate();
                    }
                    inserirTransacao(c, locatarioId, "CAUCAO_DEVOLVIDA", caucao,
                            String.format("Devolução da caução do aluguer #%d", id),
                            lerSaldo(c, locatarioId), id, "reserva");
                } else {
                    inserirTransacao(c, locatarioId, "CAUCAO_RETIDA", caucao,
                            String.format("Caução retida — incidente reportado no aluguer #%d", id),
                            lerSaldo(c, locatarioId), id, "reserva");
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

    /** Lê o saldo atual de um utilizador usando a MESMA ligação/transação (evita leituras fora da transação). */
    private double lerSaldo(Connection c, int utilizadorId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT saldo FROM utilizadores WHERE id=?")) {
            ps.setInt(1, utilizadorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                BigDecimal v = rs.getBigDecimal("saldo");
                return v != null ? v.doubleValue() : 0.0;
            }
        }
        return 0.0;
    }

    /** Insere uma linha no histórico de transações, usando a MESMA ligação/transação do chamador. */
    private void inserirTransacao(Connection c, int utilizadorId, String tipo, double valor,
                                   String descricao, double saldoApos, Integer referenciaId, String referenciaTipo) throws SQLException {
        String sql = "INSERT INTO transacoes (utilizador_id, tipo, valor, descricao, saldo_apos, referencia_id, referencia_tipo) " +
                     "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, utilizadorId);
            ps.setString(2, tipo);
            ps.setBigDecimal(3, BigDecimal.valueOf(valor));
            ps.setString(4, descricao);
            ps.setBigDecimal(5, BigDecimal.valueOf(saldoApos));
            if (referenciaId != null) ps.setInt(6, referenciaId); else ps.setNull(6, Types.INTEGER);
            ps.setString(7, referenciaTipo);
            ps.executeUpdate();
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

    /** Avaliação do proprietário ao locatário (tipo PROPRIETARIO). */
    public void avaliarComoProprietario(int reservaId, int estrelas, String comentario) throws SQLException {
        try {
            String sql = "INSERT IGNORE INTO avaliacoes (aluguer_id, avaliador_id, avaliado_id, estrelas, comentario, tipo) " +
                         "SELECT al.id, v.proprietario_id, r.locatario_id, ?, ?, 'PROPRIETARIO' " +
                         "FROM reservas r JOIN alugueres al ON al.reserva_id=r.id JOIN veiculos v ON v.id=r.veiculo_id WHERE r.id=?";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, estrelas);
                ps.setString(2, comentario != null ? comentario : "");
                ps.setInt(3, reservaId);
                ps.executeUpdate();
            }
            // Recalcular avaliação_média do locatário (cache na tabela utilizadores não existe
            // — a média é calculada on-the-fly via query no UserRepository)
        } catch (Exception e) {
            throw new SQLException("Erro ao registar avaliação do proprietário: " + e.getMessage(), e);
        }
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

    /** Obtém o preço corrente do combustível via CombustivelService (tabela precos_combustivel). */
    public double getPrecoAtualCombustivel(String tipoCombustivel) {
        return new pt.carguru.Services.CombustivelService().getPrecoCorrente(tipoCombustivel);
    }

    /** Soma das cauções de reservas ainda "ativas" (pendentes ou aceites) de um locatário —
     *  representa o valor que está reservado/comprometido e não está livre para levantamento. */
    public double getCaucoesAtivas(int locatarioId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(caucao),0) FROM reservas WHERE locatario_id=? AND estado IN ('PENDENTE','ACEITE')";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, locatarioId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                BigDecimal v = rs.getBigDecimal(1);
                return v != null ? v.doubleValue() : 0.0;
            }
        }
        return 0.0;
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

    /**
     * Devolve reservas do locatário filtradas por período (data_inicio >= de AND data_fim <= ate).
     * Passa null em qualquer dos campos de data para não filtrar por esse limite.
     */
    public List<Reserva> findByLocatarioFiltrado(int locatarioId, LocalDate de, LocalDate ate) throws SQLException {
        return queryFiltrado("r.locatario_id", locatarioId, de, ate, null);
    }

    /**
     * Devolve reservas do proprietário filtradas por período.
     */
    public List<Reserva> findByProprietarioFiltrado(int proprietarioId, LocalDate de, LocalDate ate) throws SQLException {
        return queryFiltrado("v.proprietario_id", proprietarioId, de, ate, null);
    }

    /**
     * Devolve reservas do proprietário filtradas por período e, opcionalmente, por um veículo
     * específico (passa null em veiculoId para incluir todos os veículos do proprietário).
     */
    public List<Reserva> findByProprietarioFiltrado(int proprietarioId, LocalDate de, LocalDate ate, Integer veiculoId) throws SQLException {
        return queryFiltrado("v.proprietario_id", proprietarioId, de, ate, veiculoId);
    }

    private List<Reserva> queryFiltrado(String coluna, int userId,
                                         LocalDate de, LocalDate ate, Integer veiculoId) throws SQLException {
        StringBuilder where = new StringBuilder(coluna + "=?");
        if (de  != null) where.append(" AND r.data_inicio >= ?");
        if (ate != null) where.append(" AND r.data_fim <= ?");
        if (veiculoId != null) where.append(" AND r.veiculo_id = ?");
        String sql = buildJoin(where.toString()) + " ORDER BY r.data_pedido DESC";

        List<Reserva> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, userId);
            if (de  != null) ps.setDate(idx++, Date.valueOf(de));
            if (ate != null) ps.setDate(idx++, Date.valueOf(ate));
            if (veiculoId != null) ps.setInt(idx, veiculoId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
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

    /**
     * Pesquisa de reservas para o painel de administração, com filtros opcionais:
     *  - período (data_inicio >= de AND data_fim <= ate)
     *  - locatário por id
     *  - veículo por id
     *  - estado (ex: "pendente", "confirmada", "cancelada", "concluida")
     * Passa null em qualquer parâmetro para não aplicar esse filtro.
     */
    public List<Reserva> findAllFiltrado(LocalDate de, LocalDate ate,
                                          Integer locatarioId, Integer veiculoId,
                                          String estado) throws SQLException {
        StringBuilder where = new StringBuilder("1=1");
        if (de         != null) where.append(" AND r.data_inicio >= ?");
        if (ate        != null) where.append(" AND r.data_fim <= ?");
        if (locatarioId != null) where.append(" AND r.locatario_id = ?");
        if (veiculoId  != null) where.append(" AND r.veiculo_id = ?");
        if (estado     != null && !estado.isBlank()) {
            // Mapear estado normalizado → valor(es) na BD
            String dbEstado = switch (estado.toLowerCase()) {
                case "pendente"   -> "PENDENTE";
                case "confirmada" -> "ACEITE";
                case "rejeitada"  -> "REJEITADA";
                case "cancelada"  -> "CANCELADA";
                case "concluida"  -> "CONCLUIDA";
                default           -> estado.toUpperCase();
            };
            where.append(" AND r.estado = '").append(dbEstado.replace("'", "''")).append("'");
        }

        String sql = buildJoin(where.toString()) + " ORDER BY r.data_pedido DESC";
        List<Reserva> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            if (de          != null) ps.setDate(idx++, Date.valueOf(de));
            if (ate         != null) ps.setDate(idx++, Date.valueOf(ate));
            if (locatarioId != null) ps.setInt(idx++, locatarioId);
            if (veiculoId   != null) ps.setInt(idx, veiculoId);
            ResultSet rs = ps.executeQuery();
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
               "ul.nome AS locatario_nome, ul.email AS locatario_email, " +
               "up.nome AS proprietario_nome, up.email AS proprietario_email, " +
               "al.km_final, " +
               "av_loc.estrelas AS avaliacao_estrelas, " +
               "av_prop.estrelas AS avaliacao_proprietario_estrelas " +
               "FROM reservas r " +
               "JOIN veiculos v ON r.veiculo_id=v.id " +
               "JOIN utilizadores ul ON r.locatario_id=ul.id " +
               "JOIN utilizadores up ON v.proprietario_id=up.id " +
               "LEFT JOIN alugueres al ON al.reserva_id=r.id " +
               "LEFT JOIN avaliacoes av_loc ON av_loc.aluguer_id=al.id AND av_loc.tipo='LOCATARIO' " +
               "LEFT JOIN avaliacoes av_prop ON av_prop.aluguer_id=al.id AND av_prop.tipo='PROPRIETARIO' " +
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
        try { int avP = rs.getInt("avaliacao_proprietario_estrelas"); if (!rs.wasNull()) r.setAvaliacaoProprietario(avP); } catch (Exception ignored) {}
        r.setVeiculoNome(rs.getString("marca") + " " + rs.getString("modelo") + " (" + rs.getInt("ano") + ")");
        r.setLocatarioNome(rs.getString("locatario_nome"));
        r.setProprietarioNome(rs.getString("proprietario_nome"));
        try { r.setLocatarioEmail(rs.getString("locatario_email")); } catch (Exception ignored) {}
        try { r.setProprietarioEmail(rs.getString("proprietario_email")); } catch (Exception ignored) {}
        return r;
    }

    private String normalizar(String estado) {
        if (estado == null) return "pendente";
        return switch (estado.toUpperCase()) {
            case "PENDENTE"  -> "pendente";
            case "ACEITE"    -> "confirmada";
            case "REJEITADA" -> "rejeitada";
            case "CANCELADA", "EXPIRADA" -> "cancelada";
            case "CONCLUIDA" -> "concluida";
            default -> estado.toLowerCase();
        };
    }
}
