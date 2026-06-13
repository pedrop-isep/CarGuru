package pt.carguru.Models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {
    private int id;
    private String nome;
    private String email;
    private String passwordHash;
    private String nif;
    private String nCartaConducao;
    private LocalDate validadeCarta;
    private String role;
    private double saldo;
    private boolean bloqueado;
    private LocalDateTime dataRegisto;
    private String tokenRecuperacao;
    private LocalDateTime tokenExpiraEm;

    public User() {}

    public User(int id, String nome, String email, String passwordHash, String nif,
                String nCartaConducao, LocalDate validadeCarta, String role,
                double saldo, boolean bloqueado, LocalDateTime dataRegisto) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nif = nif;
        this.nCartaConducao = nCartaConducao;
        this.validadeCarta = validadeCarta;
        this.role = role;
        this.saldo = saldo;
        this.bloqueado = bloqueado;
        this.dataRegisto = dataRegisto;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }
    public String getNCartaConducao() { return nCartaConducao; }
    public void setNCartaConducao(String nCartaConducao) { this.nCartaConducao = nCartaConducao; }
    public LocalDate getValidadeCarta() { return validadeCarta; }
    public void setValidadeCarta(LocalDate validadeCarta) { this.validadeCarta = validadeCarta; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
    public LocalDateTime getDataRegisto() { return dataRegisto; }
    public void setDataRegisto(LocalDateTime dataRegisto) { this.dataRegisto = dataRegisto; }
    public String getTokenRecuperacao() { return tokenRecuperacao; }
    public void setTokenRecuperacao(String tokenRecuperacao) { this.tokenRecuperacao = tokenRecuperacao; }
    public LocalDateTime getTokenExpiraEm() { return tokenExpiraEm; }
    public void setTokenExpiraEm(LocalDateTime tokenExpiraEm) { this.tokenExpiraEm = tokenExpiraEm; }

    public boolean isAtivo() { return !bloqueado; }

    public String getInitials() {
        if (nome == null || nome.isBlank()) return "?";
        String[] parts = nome.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) sb.append(parts[i].charAt(0));
        return sb.toString().toUpperCase();
    }

    @Override
    public String toString() { return nome + " <" + email + ">"; }
}