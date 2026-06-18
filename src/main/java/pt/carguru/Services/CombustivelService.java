package pt.carguru.Services;

import pt.carguru.Models.PrecoCombustivel;
import pt.carguru.Repositories.CombustivelRepository;
import pt.carguru.Utils.Session;

import java.sql.SQLException;
import java.util.List;

public class CombustivelService {

    private final CombustivelRepository repo = new CombustivelRepository();

    /** Tipos suportados, por ordem de apresentação. */
    public static final String[] TIPOS = {"GASOLINA", "GASOLEO", "GPL", "ELETRICO"};

    // ── Leitura ───────────────────────────────────────────────────────────────

    /** Todos os preços correntes. */
    public List<PrecoCombustivel> listarPrecos() throws SQLException {
        return repo.findAll();
    }

    /**
     * Preço corrente para um tipo — usado nos cálculos de custo de combustível.
     * Fallback para valor padrão se ainda não estiver configurado.
     */
    public double getPrecoCorrente(String tipo) {
        try {
            return repo.findByTipo(tipo)
                       .map(PrecoCombustivel::getPrecoCorrente)
                       .orElse(precoDefault(tipo));
        } catch (Exception e) {
            return precoDefault(tipo);
        }
    }

    // ── Admin: definir preço base ─────────────────────────────────────────────

    /**
     * Define ou atualiza o preço base de um tipo de combustível.
     * Apenas administradores podem fazê-lo.
     *
     * @param tipo       GASOLINA | GASOLEO | GPL | ELETRICO
     * @param precoBase  €/L ou €/kWh (> 0)
     */
    public void definirPrecoBase(String tipo, double precoBase) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        if (precoBase <= 0) throw new IllegalArgumentException("O preço base deve ser positivo.");
        if (!isTipoValido(tipo)) throw new IllegalArgumentException("Tipo de combustível inválido: " + tipo);
        repo.upsertPrecoBase(tipo.toUpperCase(), precoBase, Session.getUser().getId());
    }

    // ── Scheduler: calcular nova variação e persistir ─────────────────────────

    /**
     * Calcula um novo preço corrente para cada tipo e persiste na BD.
     * Variação aleatória de ±5% em torno do preço base (capped para nunca ficar ≤ 0).
     * Chamado pelo {@link pt.carguru.Utils.CombustivelScheduler} a cada 10 minutos.
     */
    public void atualizarPrecosCorrentes() {
        try {
            List<PrecoCombustivel> precos = repo.findAll();
            for (PrecoCombustivel p : precos) {
                double variacao = 1.0 + (Math.random() * 0.10 - 0.05); // ±5%
                double novoPreco = Math.max(0.01, p.getPrecoBase() * variacao);
                // Arredondar a 4 casas decimais
                novoPreco = Math.round(novoPreco * 10000.0) / 10000.0;
                repo.atualizarPrecoCorrente(p.getTipoCombustivel(), novoPreco);
            }
        } catch (Exception e) {
            System.err.println("[CombustivelService] Erro ao atualizar preços correntes: " + e.getMessage());
        }
    }

    /**
     * Regista o preço corrente de cada tipo no histórico horário.
     * Chamado pelo scheduler a cada hora (e uma vez no arranque).
     */
    public void registarHistoricoHorario() {
        try {
            List<PrecoCombustivel> precos = repo.findAll();
            for (PrecoCombustivel p : precos) {
                repo.registarHistorico(p.getTipoCombustivel(), p.getId(), p.getPrecoCorrente());
            }
        } catch (Exception e) {
            System.err.println("[CombustivelService] Erro ao registar histórico: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isTipoValido(String tipo) {
        for (String t : TIPOS) if (t.equalsIgnoreCase(tipo)) return true;
        return false;
    }

    private double precoDefault(String tipo) {
        if (tipo == null) return 1.70;
        return switch (tipo.toUpperCase()) {
            case "GASOLEO"  -> 1.60;
            case "ELETRICO" -> 0.20;
            case "GPL"      -> 0.90;
            default         -> 1.70; // GASOLINA
        };
    }
}
