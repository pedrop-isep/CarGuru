package pt.carguru.Models;

/**
 * Rendimento de um veículo num mês específico (apenas alugueres CONCLUIDOS).
 * Usado pelo gráfico de "Rendimento mensal por veículo" no painel de estatísticas do admin.
 */
public class RendimentoMensalVeiculo {
    private String mes;          // formato "yyyy-MM", ex: "2026-06"
    private int veiculoId;
    private String veiculoNome;
    private double receita;
    private int numeroAlugueres;

    public RendimentoMensalVeiculo() {}

    public RendimentoMensalVeiculo(String mes, int veiculoId, String veiculoNome, double receita, int numeroAlugueres) {
        this.mes = mes;
        this.veiculoId = veiculoId;
        this.veiculoNome = veiculoNome;
        this.receita = receita;
        this.numeroAlugueres = numeroAlugueres;
    }

    public String getMes() { return mes; }
    public void setMes(String mes) { this.mes = mes; }
    public int getVeiculoId() { return veiculoId; }
    public void setVeiculoId(int veiculoId) { this.veiculoId = veiculoId; }
    public String getVeiculoNome() { return veiculoNome; }
    public void setVeiculoNome(String veiculoNome) { this.veiculoNome = veiculoNome; }
    public double getReceita() { return receita; }
    public void setReceita(double receita) { this.receita = receita; }
    public int getNumeroAlugueres() { return numeroAlugueres; }
    public void setNumeroAlugueres(int numeroAlugueres) { this.numeroAlugueres = numeroAlugueres; }
}
