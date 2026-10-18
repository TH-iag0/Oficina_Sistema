package br.com.example.autogestao_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "numerador_os")
public class NumeradorOs {
    @Id
    private int id;
    @Column(nullable = false)
    private long valor;
    protected NumeradorOs() {}
    public long proximo() {
        return ++valor;
    }
}
