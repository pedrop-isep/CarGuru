package pt.carguru.Utils;

import pt.carguru.Services.CombustivelService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler de preços de combustível.
 *
 * <p>Executa duas tarefas independentes:
 * <ul>
 *   <li>A cada <b>10 minutos</b>: varia o preço corrente de cada tipo em ±5% em torno do preço base.</li>
 *   <li>A cada <b>1 hora</b>: regista o preço corrente no histórico ({@code historico_precos}).</li>
 * </ul>
 *
 * <p>Iniciado uma única vez em {@link pt.carguru.App#start} e encerrado com {@link #parar()}.
 */
public class CombustivelScheduler {

    private static final int INTERVALO_VARIACAO_MIN = 10;
    private static final int INTERVALO_HISTORICO_MIN = 60;

    private static CombustivelScheduler instancia;
    private ScheduledExecutorService executor;
    private final CombustivelService service = new CombustivelService();

    /** Contador de execuções da tarefa de variação — usado para decidir quando registar o histórico. */
    private final AtomicInteger ciclosVariacao = new AtomicInteger(0);

    private CombustivelScheduler() {}

    public static synchronized CombustivelScheduler getInstance() {
        if (instancia == null) instancia = new CombustivelScheduler();
        return instancia;
    }

    /**
     * Inicia o scheduler.
     * Seguro para chamar várias vezes — ignora chamadas subsequentes.
     */
    public synchronized void iniciar() {
        if (executor != null && !executor.isShutdown()) return;

        // Thread daemon para não impedir o encerramento da JVM
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "carguru-combustivel-scheduler");
            t.setDaemon(true);
            return t;
        });

        // Execução imediata no arranque (delay=0), depois a cada 10 minutos
        executor.scheduleAtFixedRate(this::ciclo, 0, INTERVALO_VARIACAO_MIN, TimeUnit.MINUTES);

        System.out.println("[CombustivelScheduler] Iniciado — variação a cada "
                + INTERVALO_VARIACAO_MIN + " min, histórico a cada " + INTERVALO_HISTORICO_MIN + " min.");
    }

    /** Para o scheduler graciosamente. */
    public synchronized void parar() {
        if (executor != null) {
            executor.shutdownNow();
            System.out.println("[CombustivelScheduler] Parado.");
        }
    }

    // ── Tarefa principal ──────────────────────────────────────────────────────

    private void ciclo() {
        try {
            // 1. Variar preço corrente (sempre)
            service.atualizarPrecosCorrentes();

            // 2. Registar histórico a cada hora (a cada N ciclos de 10 min = 6 ciclos)
            int ciclo = ciclosVariacao.incrementAndGet();
            int ciclosPorHora = INTERVALO_HISTORICO_MIN / INTERVALO_VARIACAO_MIN;
            if (ciclo % ciclosPorHora == 0 || ciclo == 1) {
                // ciclo==1 garante registo imediato no arranque
                service.registarHistoricoHorario();
                System.out.println("[CombustivelScheduler] Histórico registado (ciclo " + ciclo + ").");
            }

        } catch (Exception e) {
            System.err.println("[CombustivelScheduler] Erro no ciclo: " + e.getMessage());
        }
    }
}
