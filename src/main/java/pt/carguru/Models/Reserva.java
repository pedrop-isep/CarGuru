package pt.carguru.Models;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Reserva {
    private int id;
    private int veiculoId;
    private int locatarioId;
    private int proprietarioId;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private double total;
    private String estado; // "pendente" | "confirmada" | "cancelada" | "concluida"
    private Integer kmInicial;
    private Integer kmFinal;
    private Double custoCombustivel;
    private Integer avaliacao;
    private String comentarioAvaliacao;

    // Joined fields for display
    private String veiculoNome;
    private String locatarioNome;
    private String proprietarioNome;

    public Reserva() {}

    public Reserva(int veiculoId, int locatarioId, int proprietarioId,
                   LocalDate dataInicio, LocalDate dataFim, double total) {
        this.veiculoId = veiculoId;
        this.locatarioId = locatarioId;
        this.proprietarioId = proprietarioId;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.total = total;
        this.estado = "pendente";
    }

    public long getNumeroDias() {
        if (dataInicio == null || dataFim == null) return 0;
        return ChronoUnit.DAYS.between(dataInicio, dataFim);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getVeiculoId() { return veiculoId; }
    public void setVeiculoId(int veiculoId) { this.veiculoId = veiculoId; }
    public int getLocatarioId() { return locatarioId; }
    public void setLocatarioId(int locatarioId) { this.locatarioId = locatarioId; }
    public int getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(int proprietarioId) { this.proprietarioId = proprietarioId; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Integer getKmInicial() { return kmInicial; }
    public void setKmInicial(Integer kmInicial) { this.kmInicial = kmInicial; }
    public Integer getKmFinal() { return kmFinal; }
    public void setKmFinal(Integer kmFinal) { this.kmFinal = kmFinal; }
    public Double getCustoCombustivel() { return custoCombustivel; }
    public void setCustoCombustivel(Double custoCombustivel) { this.custoCombustivel = custoCombustivel; }
    public Integer getAvaliacao() { return avaliacao; }
    public void setAvaliacao(Integer avaliacao) { this.avaliacao = avaliacao; }
    public String getComentarioAvaliacao() { return comentarioAvaliacao; }
    public void setComentarioAvaliacao(String comentarioAvaliacao) { this.comentarioAvaliacao = comentarioAvaliacao; }
    public String getVeiculoNome() { return veiculoNome; }
    public void setVeiculoNome(String veiculoNome) { this.veiculoNome = veiculoNome; }
    public String getLocatarioNome() { return locatarioNome; }
    public void setLocatarioNome(String locatarioNome) { this.locatarioNome = locatarioNome; }
    public String getProprietarioNome() { return proprietarioNome; }
    public void setProprietarioNome(String proprietarioNome) { this.proprietarioNome = proprietarioNome; }
}
