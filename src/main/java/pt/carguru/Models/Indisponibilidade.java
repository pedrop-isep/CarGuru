package pt.carguru.Models;

import java.time.LocalDate;

public class Indisponibilidade {
    private int id;
    private int veiculoId;
    private LocalDate inicio;
    private LocalDate fim;

    public Indisponibilidade() {}
    public Indisponibilidade(int veiculoId, LocalDate inicio, LocalDate fim) {
        this.veiculoId = veiculoId; this.inicio = inicio; this.fim = fim;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getVeiculoId() { return veiculoId; }
    public void setVeiculoId(int v) { this.veiculoId = v; }
    public LocalDate getInicio() { return inicio; }
    public void setInicio(LocalDate i) { this.inicio = i; }
    public LocalDate getFim() { return fim; }
    public void setFim(LocalDate f) { this.fim = f; }
}
