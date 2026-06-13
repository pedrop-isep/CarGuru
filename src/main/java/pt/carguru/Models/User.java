package pt.carguru.Models;

public class User {
    private int id;
    private String nome;
    private String email;
    private String passwordHash;
    private String nif;
    private String role; // "utilizador" | "admin"
    private double saldo;
    private boolean ativo;

    public User() {}

    public User(int id, String nome, String email, String passwordHash, String nif, String role, double saldo, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nif = nif;
        this.role = role;
        this.saldo = saldo;
        this.ativo = ativo;
    }

    // Getters and Setters
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
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

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
