package pt.carguru.Models;

import java.time.LocalDate;

public class Reserva {
    private int id;
    private int veiculoId;
    private int locatarioId;
    private int proprietarioId;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String estado; // pendente|confirmada|cancelada|concluida (normalizado)
    private double total;
    private Integer kmInicial;
    private Integer kmFinal;
    private Integer avaliacao;           // avaliação do locatário ao proprietário
    private Integer avaliacaoProprietario; // avaliação do proprietário ao locatário
    private String comentarioAvaliacao;
    private double caucao;
    private String combustivelVeiculo;

    // JOIN helpers
    private String veiculoNome;
    private String locatarioNome;
    private String proprietarioNome;
    private String locatarioEmail;
    private String proprietarioEmail;

    public Reserva() {}

    public int getNumeroDias() {
        if (dataInicio == null || dataFim == null) return 1;
        long d = java.time.temporal.ChronoUnit.DAYS.between(dataInicio, dataFim);
        return d > 0 ? (int) d : 1;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getVeiculoId() { return veiculoId; }
    public void setVeiculoId(int v) { this.veiculoId = v; }
    public int getLocatarioId() { return locatarioId; }
    public void setLocatarioId(int l) { this.locatarioId = l; }
    public int getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(int p) { this.proprietarioId = p; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate d) { this.dataInicio = d; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate d) { this.dataFim = d; }
    public String getEstado() { return estado; }
    public void setEstado(String e) { this.estado = e; }
    public double getTotal() { return total; }
    public void setTotal(double t) { this.total = t; }
    public Integer getKmInicial() { return kmInicial; }
    public void setKmInicial(Integer k) { this.kmInicial = k; }
    public Integer getKmFinal() { return kmFinal; }
    public void setKmFinal(Integer k) { this.kmFinal = k; }
    public Integer getAvaliacao() { return avaliacao; }
    public void setAvaliacao(Integer a) { this.avaliacao = a; }
    public Integer getAvaliacaoProprietario() { return avaliacaoProprietario; }
    public void setAvaliacaoProprietario(Integer a) { this.avaliacaoProprietario = a; }
    public String getComentarioAvaliacao() { return comentarioAvaliacao; }
    public void setComentarioAvaliacao(String c) { this.comentarioAvaliacao = c; }
    public String getVeiculoNome() { return veiculoNome; }
    public void setVeiculoNome(String v) { this.veiculoNome = v; }
    public String getLocatarioNome() { return locatarioNome; }
    public void setLocatarioNome(String n) { this.locatarioNome = n; }
    public String getProprietarioNome() { return proprietarioNome; }
    public void setProprietarioNome(String n) { this.proprietarioNome = n; }
    public String getLocatarioEmail() { return locatarioEmail; }
    public void setLocatarioEmail(String e) { this.locatarioEmail = e; }
    public String getProprietarioEmail() { return proprietarioEmail; }
    public void setProprietarioEmail(String e) { this.proprietarioEmail = e; }
    public double getCaucao() { return caucao; }
    public void setCaucao(double c) { this.caucao = c; }
    public String getCombustivelVeiculo() { return combustivelVeiculo; }
    public void setCombustivelVeiculo(String c) { this.combustivelVeiculo = c; }
}
