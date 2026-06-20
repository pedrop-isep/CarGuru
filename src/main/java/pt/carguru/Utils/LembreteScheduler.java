package pt.carguru.Utils;

import pt.carguru.Services.ReservaService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler de lembretes de aluguer por email.
 *
 * <p>Uma vez por dia, verifica se há reservas cujo aluguer começa ou termina
 * "amanhã" e envia o lembrete correspondente ao locatário (ver
 * {@link ReservaService#enviarLembretesDoDia()}):
 * <ul>
 *   <li><b>Lembrete de início</b>: aluguer está prestes a começar.</li>
 *   <li><b>Lembrete de devolução</b>: aluguer está prestes a terminar.</li>
 * </ul>
 *
 * <p>Cada lembrete só é enviado uma vez por reserva (flags
 * {@code lembrete_inicio_enviado} / {@code lembrete_fim_enviado} na BD),
 * mesmo que a aplicação seja reiniciada várias vezes no mesmo dia.
 *
 * <p>Iniciado uma única vez em {@link pt.carguru.App#start} e encerrado com {@link #parar()}.
 */
public class LembreteScheduler {

    private static final int INTERVALO_VERIFICACAO_MIN = 60;

    private static LembreteScheduler instancia;
    private ScheduledExecutorService executor;
    private final ReservaService reservaService = new ReservaService();

    private LembreteScheduler() {}

    public static synchronized LembreteScheduler getInstance() {
        if (instancia == null) instancia = new LembreteScheduler();
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
            Thread t = new Thread(r, "carguru-lembrete-scheduler");
            t.setDaemon(true);
            return t;
        });

        // Execução imediata no arranque (delay=0), depois a cada hora — como as
        // flags de "já enviado" ficam persistidas na BD, repetir a verificação
        // várias vezes por dia é seguro e garante que novas reservas aceites
        // durante o dia ainda recebem o lembrete a tempo.
        executor.scheduleAtFixedRate(this::ciclo, 0, INTERVALO_VERIFICACAO_MIN, TimeUnit.MINUTES);

        System.out.println("[LembreteScheduler] Iniciado — verificação a cada "
                + INTERVALO_VERIFICACAO_MIN + " min.");
    }

    /** Para o scheduler graciosamente. */
    public synchronized void parar() {
        if (executor != null) {
            executor.shutdownNow();
            System.out.println("[LembreteScheduler] Parado.");
        }
    }

    private void ciclo() {
        try {
            reservaService.enviarLembretesDoDia();
        } catch (Exception e) {
            System.err.println("[LembreteScheduler] Erro no ciclo: " + e.getMessage());
        }
    }
}
