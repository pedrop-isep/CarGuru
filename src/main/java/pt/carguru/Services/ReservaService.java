package pt.carguru.Services;

import pt.carguru.Models.Reserva;
import pt.carguru.Models.RendimentoVeiculo;
import pt.carguru.Repositories.IndisponibilidadeRepository;
import pt.carguru.Repositories.ReservaRepository;
import pt.carguru.Repositories.VeiculoRepository;
import pt.carguru.Models.Veiculo;
import pt.carguru.Utils.EmailSender;
import pt.carguru.Utils.Session;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReservaService {
    private final ReservaRepository reservaRepo = new ReservaRepository();
    private final VeiculoRepository veiculoRepo = new VeiculoRepository();
    private final IndisponibilidadeRepository indispRepo = new IndisponibilidadeRepository();

    public Reserva criarReserva(int veiculoId, LocalDate inicio, LocalDate fim) throws SQLException {
        if (inicio == null || fim == null || !fim.isAfter(inicio))
            throw new IllegalArgumentException("Datas inválidas. A data de fim deve ser após a de início.");
        if (inicio.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("A data de início não pode ser no passado.");

        Veiculo v = veiculoRepo.findById(veiculoId)
            .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado."));

        if (!"DISPONIVEL".equals(v.getEstado()))
            throw new IllegalStateException("Este veículo não está disponível.");
        if (v.getProprietarioId() == Session.getUser().getId())
            throw new IllegalStateException("Não podes reservar o teu próprio veículo.");
        if (reservaRepo.existeSobreposicao(veiculoId, inicio, fim))
            throw new IllegalStateException("Já existe uma reserva para este período. Escolhe outras datas.");
        if (indispRepo.estaIndisponivel(veiculoId, inicio, fim))
            throw new IllegalStateException("O proprietário marcou este período como indisponível.");

        long dias = java.time.temporal.ChronoUnit.DAYS.between(inicio, fim);
        double precoDinamico = calcularPrecoDinamico(v.getPrecoPorDia(), inicio, fim, dias);
        double total = precoDinamico * dias;
        double caucao = total * 0.20;

        // A validação tem de considerar o saldo DISPONÍVEL (saldo total menos cauções
        // já comprometidas noutras reservas pendentes/aceites), e não o saldo total —
        // caso contrário seria possível reservar vários veículos com o mesmo dinheiro.
        double caucoesAtivas = reservaRepo.getCaucoesAtivas(Session.getUser().getId());
        double saldoDisponivel = Session.getUser().getSaldo() - caucoesAtivas;
        if (saldoDisponivel < caucao)
            throw new IllegalStateException(String.format(
                "Saldo disponível insuficiente para a caução (%.2f€). Disponível: %.2f€ (Saldo: %.2f€ − Cauções ativas: %.2f€)",
                caucao, saldoDisponivel, Session.getUser().getSaldo(), caucoesAtivas));

        Reserva r = new Reserva();
        r.setVeiculoId(veiculoId);
        r.setLocatarioId(Session.getUser().getId());
        r.setDataInicio(inicio);
        r.setDataFim(fim);
        r.setTotal(total);
        r.setEstado("pendente");
        return reservaRepo.save(r);
    }

    public void aprovarReserva(int reservaId) throws SQLException {
        Reserva r = reservaRepo.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        if (r.getProprietarioId() != Session.getUser().getId())
            throw new IllegalStateException("Sem permissão.");
        if (!"pendente".equals(r.getEstado()))
            throw new IllegalStateException("Só é possível aprovar reservas pendentes.");
        reservaRepo.updateEstado(reservaId, "ACEITE");

        // Km inicial é definido automaticamente com a quilometragem atual do veículo
        // (a que o proprietário registou/atualizou nos detalhes do carro), em vez de
        // ser pedido manualmente ao locatário.
        Veiculo v = veiculoRepo.findById(r.getVeiculoId())
            .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado."));
        reservaRepo.updateKmInicial(reservaId, v.getQuilometragem());

        // Notificar o locatário (assíncrono — falha de email não impede a aprovação)
        if (r.getLocatarioEmail() != null && !r.getLocatarioEmail().isBlank()) {
            new Thread(() -> EmailSender.enviarReservaAceite(
                    r.getLocatarioEmail(),
                    r.getLocatarioNome() != null ? r.getLocatarioNome() : "Utilizador",
                    r.getVeiculoNome(),
                    r.getDataInicio().toString(),
                    r.getDataFim().toString(),
                    r.getTotal())).start();
        }
    }

    /**
     * Rejeita o pedido de reserva com motivo obrigatório, distinguindo-se de
     * {@link #cancelarReserva} (que serve para cancelamentos genéricos de reservas
     * já aceites). Apenas reservas pendentes podem ser rejeitadas. Notifica o
     * locatário por email com o motivo da rejeição.
     */
    public void rejeitarReserva(int reservaId, String motivo) throws SQLException {
        if (motivo == null || motivo.isBlank())
            throw new IllegalArgumentException("O motivo da rejeição é obrigatório.");
        Reserva r = reservaRepo.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        if (r.getProprietarioId() != Session.getUser().getId() && !Session.isAdmin())
            throw new IllegalStateException("Sem permissão.");
        if (!"pendente".equals(r.getEstado()))
            throw new IllegalStateException("Só é possível rejeitar reservas pendentes.");
        reservaRepo.rejeitar(reservaId, motivo.trim());

        if (r.getLocatarioEmail() != null && !r.getLocatarioEmail().isBlank()) {
            new Thread(() -> EmailSender.enviarReservaRejeitada(
                    r.getLocatarioEmail(),
                    r.getLocatarioNome() != null ? r.getLocatarioNome() : "Utilizador",
                    r.getVeiculoNome(),
                    r.getDataInicio().toString(),
                    r.getDataFim().toString(),
                    motivo.trim())).start();
        }
    }

    public void cancelarReserva(int reservaId) throws SQLException {
        Reserva r = reservaRepo.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        int userId = Session.getUser().getId();
        if (r.getLocatarioId() != userId && r.getProprietarioId() != userId && !Session.isAdmin())
            throw new IllegalStateException("Sem permissão.");
        if ("cancelada".equals(r.getEstado()) || "rejeitada".equals(r.getEstado()) || "concluida".equals(r.getEstado()))
            throw new IllegalStateException("Esta reserva não pode ser cancelada.");
        reservaRepo.updateEstado(reservaId, "CANCELADA");
    }

    /**
     * Envia os lembretes de email pendentes (início e devolução de aluguer) para o
     * dia de amanhã — assim o locatário recebe o aviso com antecedência suficiente
     * para se preparar. Chamado periodicamente pelo {@link pt.carguru.Utils.LembreteScheduler}.
     * Cada reserva só recebe cada lembrete uma vez (flags lembrete_inicio_enviado /
     * lembrete_fim_enviado), mesmo que este método seja chamado várias vezes.
     */
    public void enviarLembretesDoDia() throws SQLException {
        LocalDate amanha = LocalDate.now().plusDays(1);

        for (Reserva r : reservaRepo.findParaLembreteInicio(amanha)) {
            if (r.getLocatarioEmail() == null || r.getLocatarioEmail().isBlank()) continue;
            Veiculo v = veiculoRepo.findById(r.getVeiculoId()).orElse(null);
            EmailSender.enviarLembreteInicioAluguer(
                    r.getLocatarioEmail(),
                    r.getLocatarioNome() != null ? r.getLocatarioNome() : "Utilizador",
                    r.getVeiculoNome(),
                    r.getDataInicio().toString(),
                    v != null ? v.getLocalizacao() : null);
            reservaRepo.marcarLembreteInicioEnviado(r.getId());
        }

        for (Reserva r : reservaRepo.findParaLembreteFim(amanha)) {
            if (r.getLocatarioEmail() == null || r.getLocatarioEmail().isBlank()) continue;
            Veiculo v = veiculoRepo.findById(r.getVeiculoId()).orElse(null);
            EmailSender.enviarLembreteDevolucao(
                    r.getLocatarioEmail(),
                    r.getLocatarioNome() != null ? r.getLocatarioNome() : "Utilizador",
                    r.getVeiculoNome(),
                    r.getDataFim().toString(),
                    v != null ? v.getLocalizacao() : null);
            reservaRepo.marcarLembreteFimEnviado(r.getId());
        }
    }


    /** @deprecated Desde que o registo passou a ser automático (ver {@link #aprovarReserva}),
     *  este método já não é usado pela interface. Mantido apenas por compatibilidade. */
    @Deprecated
    public void registarKmInicial(int reservaId, int km) throws SQLException {
        Reserva r = reservaRepo.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        if (r.getLocatarioId() != Session.getUser().getId())
            throw new IllegalStateException("Sem permissão.");
        if (!"confirmada".equals(r.getEstado()))
            throw new IllegalStateException("Reserva não está confirmada.");
        if (km < 0) throw new IllegalArgumentException("Km inválido.");
        reservaRepo.updateKmInicial(reservaId, km);
    }

    /** Regista o km final e liquida o aluguer. Assume-se que não há incidentes (caução devolvida). */
    public void registarKmFinalELiquidar(int reservaId, int kmFinal) throws SQLException {
        registarKmFinalELiquidar(reservaId, kmFinal, false);
    }

    /**
     * Regista o km final e liquida o aluguer.
     * Se {@code incidente} for true, a caução NÃO é devolvida automaticamente (fica retida para análise).
     */
    public void registarKmFinalELiquidar(int reservaId, int kmFinal, boolean incidente) throws SQLException {
        Reserva r = reservaRepo.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        if (r.getLocatarioId() != Session.getUser().getId())
            throw new IllegalStateException("Sem permissão.");
        if (!"confirmada".equals(r.getEstado()))
            throw new IllegalStateException("Reserva não está confirmada.");
        if (r.getKmInicial() == null)
            throw new IllegalStateException("Regista primeiro o Km inicial.");
        if (kmFinal <= r.getKmInicial())
            throw new IllegalArgumentException("Km final deve ser maior que o inicial (" + r.getKmInicial() + " km).");
        // Buscar preço atual do combustível do tipo do veículo
        double precoCombustivel = reservaRepo.getPrecoAtualCombustivel(r.getCombustivelVeiculo());
        reservaRepo.updateKmFinalELiquidacao(reservaId, kmFinal, precoCombustivel, incidente);
        // Os quilómetros percorridos passam a ser a nova quilometragem do veículo,
        // para que a próxima reserva já arranque com o km inicial correto.
        veiculoRepo.updateQuilometragem(r.getVeiculoId(), kmFinal);
        // O saldo do locatário (e do proprietário, se for o utilizador atual) é alterado
        // diretamente na base de dados; recarregar a sessão evita que a UI mostre um saldo
        // desatualizado até um novo login.
        Session.refresh();
    }

    /** Assinatura de compatibilidade (para chamadas legadas com preço explícito). */
    public void registarKmFinalELiquidar(int reservaId, int kmFinal, double precoCombustivel, double consumo) throws SQLException {
        Reserva r = reservaRepo.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        if (r.getLocatarioId() != Session.getUser().getId())
            throw new IllegalStateException("Sem permissão.");
        if (r.getKmInicial() == null)
            throw new IllegalStateException("Regista primeiro o Km inicial.");
        if (kmFinal <= r.getKmInicial())
            throw new IllegalArgumentException("Km final deve ser maior que o inicial.");
        reservaRepo.updateKmFinalELiquidacao(reservaId, kmFinal, precoCombustivel, false);
        veiculoRepo.updateQuilometragem(r.getVeiculoId(), kmFinal);
        Session.refresh();
    }

    /** Avaliação do locatário (sobre a experiência): guarda como tipo LOCATARIO. */
    public void avaliarReserva(int reservaId, int estrelas, String comentario) throws SQLException {
        if (estrelas < 1 || estrelas > 5)
            throw new IllegalArgumentException("Avaliação deve ser entre 1 e 5.");
        reservaRepo.updateAvaliacao(reservaId, estrelas, comentario);
    }

    /** Avaliação do proprietário ao locatário: guarda como tipo PROPRIETARIO. */
    public void avaliarLocatario(int reservaId, int estrelas, String comentario) throws SQLException {
        if (estrelas < 1 || estrelas > 5)
            throw new IllegalArgumentException("Avaliação deve ser entre 1 e 5.");
        reservaRepo.avaliarComoProprietario(reservaId, estrelas, comentario);
    }

    /** Devolve o preço corrente do combustível (tabela precos_combustivel) para o tipo indicado. */
    public double getPrecoAtualCombustivel(String tipoCombustivel) {
        return reservaRepo.getPrecoAtualCombustivel(tipoCombustivel);
    }

    /**
     * Preço dinâmico por dia:
     *  +20% se algum fim-de-semana (Sáb/Dom) cair no período
     *  +30% época alta (Jun-Ago + Dez)
     *  -10% se duração >= 7 dias
     */
    private double calcularPrecoDinamico(double precoBase, java.time.LocalDate inicio,
                                          java.time.LocalDate fim, long dias) {
        double fator = 1.0;

        // Verificar época alta (Junho-Agosto + Dezembro)
        boolean epocaAlta = false;
        java.time.LocalDate d = inicio;
        while (!d.isAfter(fim)) {
            int mes = d.getMonthValue();
            if (mes == 6 || mes == 7 || mes == 8 || mes == 12) { epocaAlta = true; break; }
            d = d.plusDays(1);
        }
        if (epocaAlta) fator += 0.30;

        // Verificar fim-de-semana
        boolean temFimSemana = false;
        d = inicio;
        while (!d.isAfter(fim)) {
            java.time.DayOfWeek dow = d.getDayOfWeek();
            if (dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY) {
                temFimSemana = true; break;
            }
            d = d.plusDays(1);
        }
        if (temFimSemana) fator += 0.20;

        // Desconto >= 7 dias
        if (dias >= 7) fator -= 0.10;

        return precoBase * fator;
    }

    public List<Reserva> minhasReservasComoLocatario() throws SQLException {
        return reservaRepo.findByLocatario(Session.getUser().getId());
    }

    public List<Reserva> minhasReservasComoProprietario() throws SQLException {
        return reservaRepo.findByProprietario(Session.getUser().getId());
    }

    /**
     * Devolve as reservas do utilizador como locatário, opcionalmente filtradas por período.
     * Passa null em qualquer dos parâmetros de data para não aplicar esse limite.
     */
    public List<Reserva> minhasReservasComoLocatarioFiltrado(LocalDate de, LocalDate ate) throws SQLException {
        return reservaRepo.findByLocatarioFiltrado(Session.getUser().getId(), de, ate);
    }

    /**
     * Devolve as reservas do utilizador como proprietário, opcionalmente filtradas por período.
     */
    public List<Reserva> minhasReservasComoProprietarioFiltrado(LocalDate de, LocalDate ate) throws SQLException {
        return reservaRepo.findByProprietarioFiltrado(Session.getUser().getId(), de, ate);
    }

    /**
     * Devolve as reservas do utilizador como proprietário, filtradas por período e,
     * opcionalmente, por um veículo específico (veiculoId == null devolve todos os veículos).
     */
    public List<Reserva> minhasReservasComoProprietarioFiltrado(LocalDate de, LocalDate ate, Integer veiculoId) throws SQLException {
        return reservaRepo.findByProprietarioFiltrado(Session.getUser().getId(), de, ate, veiculoId);
    }

    /**
     * Calcula o rendimento total por veículo do proprietário autenticado, considerando
     * apenas os alugueres já CONCLUÍDOS dentro do período (de/ate) indicado.
     * A lista devolvida inclui todos os veículos do proprietário, mesmo os que não tiveram
     * nenhum aluguer no período (com receita 0), para que o resumo seja completo.
     */
    public List<RendimentoVeiculo> calcularRendimentoPorVeiculo(LocalDate de, LocalDate ate) throws SQLException {
        List<Reserva> reservas = reservaRepo.findByProprietarioFiltrado(Session.getUser().getId(), de, ate, null);
        List<Veiculo> veiculos = veiculoRepo.findByProprietario(Session.getUser().getId());

        Map<Integer, RendimentoVeiculo> porVeiculo = new LinkedHashMap<>();
        for (Veiculo v : veiculos) {
            porVeiculo.put(v.getId(), new RendimentoVeiculo(v.getId(), v.getNomeCompleto()));
        }

        for (Reserva r : reservas) {
            RendimentoVeiculo rv = porVeiculo.computeIfAbsent(r.getVeiculoId(),
                    id -> new RendimentoVeiculo(id, r.getVeiculoNome()));
            rv.setNumeroAlugueresTotal(rv.getNumeroAlugueresTotal() + 1);
            if ("concluida".equals(r.getEstado())) {
                rv.setReceitaTotal(rv.getReceitaTotal() + r.getTotal());
                rv.setNumeroAlugueresConcluidos(rv.getNumeroAlugueresConcluidos() + 1);
            }
        }

        List<RendimentoVeiculo> resultado = new ArrayList<>(porVeiculo.values());
        resultado.sort((a, b) -> Double.compare(b.getReceitaTotal(), a.getReceitaTotal()));
        return resultado;
    }

    public List<Reserva> listarTodas() throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        return reservaRepo.findAll();
    }

    /**
     * Lista todas as reservas para o administrador com filtros opcionais.
     * Qualquer parâmetro pode ser null para não aplicar esse filtro.
     *
     * @param de          data de início mínima (inclusive)
     * @param ate         data de fim máxima (inclusive)
     * @param locatarioId id do utilizador locatário
     * @param veiculoId   id do veículo
     * @param estado      "pendente" | "confirmada" | "cancelada" | "concluida"
     */
    public List<Reserva> listarTodasFiltrado(LocalDate de, LocalDate ate,
                                              Integer locatarioId, Integer veiculoId,
                                              String estado) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        return reservaRepo.findAllFiltrado(de, ate, locatarioId, veiculoId, estado);
    }
}
