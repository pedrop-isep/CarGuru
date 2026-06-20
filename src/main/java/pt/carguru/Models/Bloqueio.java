package pt.carguru.Models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Representa uma ação de bloqueio ou desbloqueio de um utilizador, com motivo e data. */
public class Bloqueio {
    private int id;
    private int utilizadorId;
    private int adminId;
    private String acao; // BLOQUEIO | DESBLOQUEIO
    private String motivo;
    private LocalDateTime data;

    // JOIN helpers
    private String adminNome;

    public Bloqueio() {}

    public Bloqueio(int utilizadorId, int adminId, String acao, String motivo) {
        this.utilizadorId = utilizadorId;
        this.adminId = adminId;
        this.acao = acao;
        this.motivo = motivo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUtilizadorId() { return utilizadorId; }
    public void setUtilizadorId(int utilizadorId) { this.utilizadorId = utilizadorId; }

    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }

    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public LocalDateTime getData() { return data; }
    public void setData(LocalDateTime data) { this.data = data; }

    public String getAdminNome() { return adminNome; }
    public void setAdminNome(String adminNome) { this.adminNome = adminNome; }

    public boolean isBloqueio() { return "BLOQUEIO".equals(acao); }

    /** Etiqueta legível da ação (ex: "🚫 Bloqueio" / "✅ Desbloqueio"). */
    public String getAcaoLabel() {
        return isBloqueio() ? "🚫 Bloqueio" : "✅ Desbloqueio";
    }

    /** Data formatada para apresentação na UI (ex: "19/06/2026 14:32"). */
    public String getDataStr() {
        if (data == null) return "-";
        return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
