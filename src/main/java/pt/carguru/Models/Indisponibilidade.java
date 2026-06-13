package pt.carguru.Models;

import java.time.LocalDate;

public class Indisponibilidade {
    private int id;
    private int veiculoId;
    private LocalDate inicio;
    private LocalDate fim;

    public Indisponibilidade() {}
    public Indisponibilidade(int veiculoId, LocalDate inicio, LocalDate fim) {
        this.veiculoId = veiculoId;
        this.inicio = inicio;
        this.fim = fim;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getVeiculoId() { return veiculoId; }
    public void setVeiculoId(int veiculoId) { this.veiculoId = veiculoId; }
    public LocalDate getInicio() { return inicio; }
    public void setInicio(LocalDate inicio) { this.inicio = inicio; }
    public LocalDate getFim() { return fim; }
    public void setFim(LocalDate fim) { this.fim = fim; }
}
