package pt.carguru.Models;

/**
 * Número de reservas agregadas por localização (cidade) do veículo.
 * Usado pelo gráfico de "Distribuição geográfica das reservas" no painel de estatísticas do admin.
 */
public class ReservasPorLocalizacao {
    private String localizacao;
    private int numeroReservas;

    public ReservasPorLocalizacao() {}

    public ReservasPorLocalizacao(String localizacao, int numeroReservas) {
        this.localizacao = localizacao;
        this.numeroReservas = numeroReservas;
    }

    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }
    public int getNumeroReservas() { return numeroReservas; }
    public void setNumeroReservas(int numeroReservas) { this.numeroReservas = numeroReservas; }
}
