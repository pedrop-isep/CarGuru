package pt.carguru.Models;

import java.time.LocalDate;

public class Veiculo {
    private int id;
    private int proprietarioId;
    private String proprietarioNome;
    private String matricula;
    private String marca;
    private String modelo;
    private int ano;
    private String combustivel;
    private String transmissao;
    private String localizacao;   // cidade
    private String distrito;
    private String codigoPostal;
    private double precoPorDia;
    private double consumo;
    private String descricao;
    private String estado;
    private int quilometragem;
    private int lotacao;
    private double avaliacaoMedia;
    private int nAvaliacoes;
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

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(int p) { this.proprietarioId = p; }
    public String getProprietarioNome() { return proprietarioNome; }
    public void setProprietarioNome(String n) { this.proprietarioNome = n; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String m) { this.matricula = m; }
    public String getMarca() { return marca; }
    public void setMarca(String m) { this.marca = m; }
    public String getModelo() { return modelo; }
    public void setModelo(String m) { this.modelo = m; }
    public int getAno() { return ano; }
    public void setAno(int a) { this.ano = a; }
    public String getCombustivel() { return combustivel; }
    public void setCombustivel(String c) { this.combustivel = c; }
    public String getTransmissao() { return transmissao; }
    public void setTransmissao(String t) { this.transmissao = t; }
    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String l) { this.localizacao = l; }
    public String getDistrito() { return distrito; }
    public void setDistrito(String d) { this.distrito = d; }
    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String c) { this.codigoPostal = c; }
    public double getPrecoPorDia() { return precoPorDia; }
    public void setPrecoPorDia(double p) { this.precoPorDia = p; }
    public double getConsumo() { return consumo; }
    public void setConsumo(double c) { this.consumo = c; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String d) { this.descricao = d; }
    public String getEstado() { return estado; }
    public void setEstado(String e) { this.estado = e; }
    public int getQuilometragem() { return quilometragem; }
    public void setQuilometragem(int q) { this.quilometragem = q; }
    public int getLotacao() { return lotacao; }
    public void setLotacao(int l) { this.lotacao = l; }
    public double getAvaliacaoMedia() { return avaliacaoMedia; }
    public void setAvaliacaoMedia(double a) { this.avaliacaoMedia = a; }
    public int getNAvaliacoes() { return nAvaliacoes; }
    public void setNAvaliacoes(int n) { this.nAvaliacoes = n; }
    public LocalDate getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDate d) { this.dataCriacao = d; }

    public String getNomeCompleto() { return marca + " " + modelo + " (" + ano + ")"; }
    public String getAvaliacaoStr() {
        if (nAvaliacoes == 0) return "Sem avaliações";
        return String.format("%.1f★ (%d)", avaliacaoMedia, nAvaliacoes);
    }
}
