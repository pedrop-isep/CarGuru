package pt.carguru.Models;

import java.time.LocalDateTime;

/** Representa o preço de um tipo de combustível (linha única por tipo na tabela precos_combustivel). */
public class PrecoCombustivel {
    private int id;
    private String tipoCombustivel;   // GASOLINA | GASOLEO | GPL | ELETRICO
    private double precoBase;         // €/L ou €/kWh definido pelo admin
    private double precoCorrente;     // varia ±5% a cada 10 minutos
    private LocalDateTime ultimaAtualizacao;
    private int registadoPor;         // id do admin

    public PrecoCombustivel() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTipoCombustivel() { return tipoCombustivel; }
    public void setTipoCombustivel(String t) { this.tipoCombustivel = t; }

    public double getPrecoBase() { return precoBase; }
    public void setPrecoBase(double precoBase) { this.precoBase = precoBase; }

    public double getPrecoCorrente() { return precoCorrente; }
    public void setPrecoCorrente(double precoCorrente) { this.precoCorrente = precoCorrente; }

    public LocalDateTime getUltimaAtualizacao() { return ultimaAtualizacao; }
    public void setUltimaAtualizacao(LocalDateTime ultimaAtualizacao) { this.ultimaAtualizacao = ultimaAtualizacao; }

    public int getRegistadoPor() { return registadoPor; }
    public void setRegistadoPor(int registadoPor) { this.registadoPor = registadoPor; }

    /** Etiqueta legível com unidade. */
    public String getUnidade() {
        return "ELETRICO".equals(tipoCombustivel) ? "€/kWh" : "€/L";
    }

    public String getTipoLabel() {
        if (tipoCombustivel == null) return "-";
        return switch (tipoCombustivel) {
            case "GASOLINA" -> "⛽ Gasolina";
            case "GASOLEO"  -> "🛢️ Gasóleo";
            case "GPL"      -> "🔵 GPL";
            case "ELETRICO" -> "⚡ Elétrico";
            default -> tipoCombustivel;
        };
    }
}
