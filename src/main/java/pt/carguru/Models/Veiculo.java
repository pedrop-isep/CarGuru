package pt.carguru.Models;

import java.time.LocalDate;

public class Veiculo {
    private int id;
    private int proprietarioId;
    private String proprietarioNome;
    private String marca;
    private String modelo;
    private int ano;
    private String combustivel;
    private String transmissao;
    private String localizacao;
    private double precoPorDia;
    private double consumo;
    private String descricao;
    private String estado; // "pendente" | "aprovado" | "rejeitado"
    private LocalDate dataCriacao;

    public Veiculo() {}

    public Veiculo(int id, int proprietarioId, String marca, String modelo, int ano,
                   String combustivel, String transmissao, String localizacao,
                   double precoPorDia, double consumo, String descricao, String estado) {
        this.id = id;
        this.proprietarioId = proprietarioId;
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.combustivel = combustivel;
        this.transmissao = transmissao;
        this.localizacao = localizacao;
        this.precoPorDia = precoPorDia;
        this.consumo = consumo;
        this.descricao = descricao;
        this.estado = estado;
        this.dataCriacao = LocalDate.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(int proprietarioId) { this.proprietarioId = proprietarioId; }
    public String getProprietarioNome() { return proprietarioNome; }
    public void setProprietarioNome(String proprietarioNome) { this.proprietarioNome = proprietarioNome; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }
    public String getCombustivel() { return combustivel; }
    public void setCombustivel(String combustivel) { this.combustivel = combustivel; }
    public String getTransmissao() { return transmissao; }
    public void setTransmissao(String transmissao) { this.transmissao = transmissao; }
    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }
    public double getPrecoPorDia() { return precoPorDia; }
    public void setPrecoPorDia(double precoPorDia) { this.precoPorDia = precoPorDia; }
    public double getConsumo() { return consumo; }
    public void setConsumo(double consumo) { this.consumo = consumo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDate getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDate dataCriacao) { this.dataCriacao = dataCriacao; }

    public String getNomeCompleto() { return marca + " " + modelo + " (" + ano + ")"; }
}
