package pt.carguru.Models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {
    private int id;
    private String email;
    private String nome;
    private String nif;
    private String nCartaConducao;
    private LocalDate validadeCarta;
    private String passwordHash;
    private double saldo;
    private String role; // UTILIZADOR | ADMINISTRADOR
    private boolean bloqueado;
    private LocalDateTime dataRegisto;
    private String tokenRecuperacao;
    private LocalDateTime tokenExpiraEm;

    public User() {}

    public User(int id, String nome, String email, String passwordHash, String nif,
                String nCartaConducao, LocalDate validadeCarta, String role,
                double saldo, boolean bloqueado, LocalDateTime dataRegisto) {
        this.id = id; this.nome = nome; this.email = email;
        this.passwordHash = passwordHash; this.nif = nif;
        this.nCartaConducao = nCartaConducao; this.validadeCarta = validadeCarta;
        this.role = role; this.saldo = saldo; this.bloqueado = bloqueado;
        this.dataRegisto = dataRegisto;
    }

    public String getInitials() {
        if (nome == null || nome.isBlank()) return "?";
        String[] parts = nome.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    // Getters & setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }
    public String getNCartaConducao() { return nCartaConducao; }
    public void setNCartaConducao(String n) { this.nCartaConducao = n; }
    public LocalDate getValidadeCarta() { return validadeCarta; }
    public void setValidadeCarta(LocalDate v) { this.validadeCarta = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String h) { this.passwordHash = h; }
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean b) { this.bloqueado = b; }
    public LocalDateTime getDataRegisto() { return dataRegisto; }
    public void setDataRegisto(LocalDateTime d) { this.dataRegisto = d; }
    public String getTokenRecuperacao() { return tokenRecuperacao; }
    public void setTokenRecuperacao(String t) { this.tokenRecuperacao = t; }
    public LocalDateTime getTokenExpiraEm() { return tokenExpiraEm; }
    public void setTokenExpiraEm(LocalDateTime t) { this.tokenExpiraEm = t; }
}
