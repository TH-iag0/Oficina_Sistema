package br.com.example.autogestao_backend.model;

import java.math.BigDecimal;
import java.time.*;
import java.util.Objects;
import jakarta.persistence.*;

/** Despesa operacional: custos diretos dos serviços ficam nos itens da OS. */
@Entity
@Table(name = "despesa", indexes = @Index(name = "idx_despesa_competencia", columnList = "competencia"))
public class Despesa extends Entidade {
    @Column(nullable = false, length = 160) private String descricao;
    @Column(nullable = false, length = 80) private String categoria;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal valor;
    @Column(nullable = false) private LocalDate competencia;
    @Column(nullable = false) private LocalDate vencimento;
    @Column(name = "paga_em") private Instant pagaEm;
    protected Despesa() {}
    public Despesa(String descricao, String categoria, BigDecimal valor, LocalDate competencia, LocalDate vencimento) {
        this.descricao = RegrasModel.texto(descricao, 160, "Descrição"); this.categoria = RegrasModel.texto(categoria, 80, "Categoria");
        this.valor = RegrasModel.positivo(valor); this.competencia = Objects.requireNonNull(competencia); this.vencimento = Objects.requireNonNull(vencimento);
    }
    public void confirmarPagamento(Clock relogio) {
        if (pagaEm != null) throw new IllegalStateException("Despesa já paga.");
        pagaEm = Objects.requireNonNull(relogio).instant();
    }
    public String getDescricao() { return descricao; }
    public String getCategoria() { return categoria; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getCompetencia() { return competencia; }
    public LocalDate getVencimento() { return vencimento; }
    public Instant getPagaEm() { return pagaEm; }
}
