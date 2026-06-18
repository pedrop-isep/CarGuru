package pt.carguru.Models;

import java.time.LocalDateTime;

/** Representa uma disputa aberta sobre a caução de um aluguer. */
public class Disputa {
    private int id;
    private int aluguerIdInterno;   // FK para alugueres.id
    private int reservaId;          // para referência rápida na UI
    private int iniciadorId;
    private Integer adminId;
    private String descricao;
    private String estado;          // ABERTA | EM_ANALISE | RESOLVIDA_PROPRIETARIO | RESOLVIDA_LOCATARIO | ENCERRADA
    private String resolucao;
    private Double reembolsoForcado;
    private Double penalizacao;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataResolucao;

    // JOIN helpers
    private String iniciadorNome;
    private String adminNome;
    private String veiculoNome;
    private String locatarioNome;
    private String proprietarioNome;
    private double caucao;
    private int locatarioId;
    private int proprietarioId;

    public Disputa() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAluguerIdInterno() { return aluguerIdInterno; }
    public void setAluguerIdInterno(int aluguerIdInterno) { this.aluguerIdInterno = aluguerIdInterno; }

    public int getReservaId() { return reservaId; }
    public void setReservaId(int reservaId) { this.reservaId = reservaId; }

    public int getIniciadorId() { return iniciadorId; }
    public void setIniciadorId(int iniciadorId) { this.iniciadorId = iniciadorId; }

    public Integer getAdminId() { return adminId; }
    public void setAdminId(Integer adminId) { this.adminId = adminId; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getResolucao() { return resolucao; }
    public void setResolucao(String resolucao) { this.resolucao = resolucao; }

    public Double getReembolsoForcado() { return reembolsoForcado; }
    public void setReembolsoForcado(Double reembolsoForcado) { this.reembolsoForcado = reembolsoForcado; }

    public Double getPenalizacao() { return penalizacao; }
    public void setPenalizacao(Double penalizacao) { this.penalizacao = penalizacao; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public LocalDateTime getDataResolucao() { return dataResolucao; }
    public void setDataResolucao(LocalDateTime dataResolucao) { this.dataResolucao = dataResolucao; }

    public String getIniciadorNome() { return iniciadorNome; }
    public void setIniciadorNome(String iniciadorNome) { this.iniciadorNome = iniciadorNome; }

    public String getAdminNome() { return adminNome; }
    public void setAdminNome(String adminNome) { this.adminNome = adminNome; }

    public String getVeiculoNome() { return veiculoNome; }
    public void setVeiculoNome(String veiculoNome) { this.veiculoNome = veiculoNome; }

    public String getLocatarioNome() { return locatarioNome; }
    public void setLocatarioNome(String locatarioNome) { this.locatarioNome = locatarioNome; }

    public String getProprietarioNome() { return proprietarioNome; }
    public void setProprietarioNome(String proprietarioNome) { this.proprietarioNome = proprietarioNome; }

    public double getCaucao() { return caucao; }
    public void setCaucao(double caucao) { this.caucao = caucao; }

    public int getLocatarioId() { return locatarioId; }
    public void setLocatarioId(int locatarioId) { this.locatarioId = locatarioId; }

    public int getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(int proprietarioId) { this.proprietarioId = proprietarioId; }

    /** Etiqueta legível do estado. */
    public String getEstadoLabel() {
        if (estado == null) return "-";
        return switch (estado.toUpperCase()) {
            case "ABERTA"                -> "🔴 Aberta";
            case "EM_ANALISE"            -> "🟡 Em Análise";
            case "RESOLVIDA_PROPRIETARIO"-> "🟢 Resolvida (favor proprietário)";
            case "RESOLVIDA_LOCATARIO"   -> "🟢 Resolvida (favor locatário)";
            case "ENCERRADA"             -> "⚫ Encerrada";
            default                      -> estado;
        };
    }

    public boolean isResolvida() {
        if (estado == null) return false;
        String s = estado.toUpperCase();
        return s.startsWith("RESOLVIDA") || s.equals("ENCERRADA");
    }
}
