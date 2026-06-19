package pt.carguru.Models;

/**
 * Representa o rendimento agregado de um veículo do proprietário,
 * já considerando o período (e eventual filtro de veículo) aplicado.
 *
 * "receitaConcluida" soma apenas alugueres já CONCLUIDOS (dinheiro efetivamente
 * recebido); "numeroAlugueres" conta o total de reservas no período (qualquer
 * estado), para o proprietário ter contexto da procura pelo veículo.
 */
public class RendimentoVeiculo {
    private int veiculoId;
    private String veiculoNome;
    private double receitaTotal;       // soma de custo_renda das reservas CONCLUIDAS
    private int numeroAlugueresConcluidos;
    private int numeroAlugueresTotal;   // inclui pendentes/confirmadas/canceladas

    public RendimentoVeiculo() {}

    public RendimentoVeiculo(int veiculoId, String veiculoNome) {
        this.veiculoId = veiculoId;
        this.veiculoNome = veiculoNome;
    }

    public int getVeiculoId() { return veiculoId; }
    public void setVeiculoId(int veiculoId) { this.veiculoId = veiculoId; }
    public String getVeiculoNome() { return veiculoNome; }
    public void setVeiculoNome(String veiculoNome) { this.veiculoNome = veiculoNome; }
    public double getReceitaTotal() { return receitaTotal; }
    public void setReceitaTotal(double receitaTotal) { this.receitaTotal = receitaTotal; }
    public int getNumeroAlugueresConcluidos() { return numeroAlugueresConcluidos; }
    public void setNumeroAlugueresConcluidos(int n) { this.numeroAlugueresConcluidos = n; }
    public int getNumeroAlugueresTotal() { return numeroAlugueresTotal; }
    public void setNumeroAlugueresTotal(int n) { this.numeroAlugueresTotal = n; }
}
