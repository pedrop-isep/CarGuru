package pt.carguru.Models;

import java.time.LocalDateTime;

/** Representa um movimento na conta de um utilizador (depósito, levantamento, pagamentos, cauções, etc). */
public class Transacao {
    private int id;
    private int utilizadorId;
    private String tipo; // DEPOSITO | LEVANTAMENTO | PAGAMENTO_ALUGUER | RECEITA_ALUGUER | CAUCAO_RETIDA | CAUCAO_DEVOLVIDA | REEMBOLSO | PENALIZACAO
    private double valor;
    private String descricao;
    private LocalDateTime data;
    private double saldoApos;
    private Integer referenciaId;
    private String referenciaTipo;
    private String contraparte; // Nome do utilizador contraparte (proprietário ou locatário)

    public Transacao() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUtilizadorId() { return utilizadorId; }
    public void setUtilizadorId(int u) { this.utilizadorId = u; }
    public String getTipo() { return tipo; }
    public void setTipo(String t) { this.tipo = t; }
    public double getValor() { return valor; }
    public void setValor(double v) { this.valor = v; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String d) { this.descricao = d; }
    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime d) { this.data = d; }
    public double getSaldoApos() { return saldoApos; }
    public void setSaldoApos(double s) { this.saldoApos = s; }
    public Integer getReferenciaId() { return referenciaId; }
    public void setReferenciaId(Integer r) { this.referenciaId = r; }
    public String getReferenciaTipo() { return referenciaTipo; }
    public void setReferenciaTipo(String r) { this.referenciaTipo = r; }
    public String getContraparte() { return contraparte; }
    public void setContraparte(String c) { this.contraparte = c; }

    /** Sinal do valor para apresentação (+ entradas, - saídas). */
    public boolean isEntrada() {
        return switch (tipo) {
            case "DEPOSITO", "RECEITA_ALUGUER", "CAUCAO_DEVOLVIDA", "REEMBOLSO" -> true;
            default -> false;
        };
    }

    public String getTipoEmoji() {
        return switch (tipo) {
            case "DEPOSITO" -> "💰";
            case "LEVANTAMENTO" -> "🏦";
            case "PAGAMENTO_ALUGUER" -> "🚗";
            case "RECEITA_ALUGUER" -> "💶";
            case "CAUCAO_RETIDA" -> "🔒";
            case "CAUCAO_DEVOLVIDA" -> "🔓";
            case "REEMBOLSO" -> "↩️";
            case "PENALIZACAO" -> "⚠️";
            default -> "•";
        };
    }

    public String getTipoLabel() {
        return switch (tipo) {
            case "DEPOSITO" -> "Depósito";
            case "LEVANTAMENTO" -> "Levantamento";
            case "PAGAMENTO_ALUGUER" -> "Pagamento de aluguer";
            case "RECEITA_ALUGUER" -> "Receita de aluguer";
            case "CAUCAO_RETIDA" -> "Caução retida";
            case "CAUCAO_DEVOLVIDA" -> "Caução devolvida";
            case "REEMBOLSO" -> "Reembolso";
            case "PENALIZACAO" -> "Penalização";
            default -> tipo;
        };
    }
}
