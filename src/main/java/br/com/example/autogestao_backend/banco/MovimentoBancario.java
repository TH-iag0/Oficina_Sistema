package br.com.example.autogestao_backend.banco;

import br.com.example.autogestao_backend.model.Entidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "movimento_bancario", uniqueConstraints = @UniqueConstraint(
    name = "uk_movimento_origem", columnNames = {"conta_id", "origem_id"}))
public class MovimentoBancario extends Entidade {
    @Column(name = "conta_id", nullable = false, length = 120)
    private String contaId;
    @Column(name = "origem_id", nullable = false, length = 200)
    private String origemId;
    @Column(nullable = false, length = 1000)
    private String descricao;
    @Column(nullable = false)
    private Instant instante;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;
    @Column(nullable = false)
    private boolean confirmado;

    protected MovimentoBancario() {}

    public MovimentoBancario(String contaId, String origemId) {
        this.contaId = Objects.requireNonNull(contaId);
        this.origemId = Objects.requireNonNull(origemId);
    }

    public void atualizar(String descricao, Instant instante, BigDecimal valor, boolean confirmado) {
        this.descricao = Objects.requireNonNull(descricao);
        this.instante = Objects.requireNonNull(instante);
        this.valor = Objects.requireNonNull(valor).setScale(2, java.math.RoundingMode.UNNECESSARY);
        this.confirmado = confirmado;
    }

    public void retirar() { confirmado = false; }
    public String getOrigemId() { return origemId; }
    public String getDescricao() { return descricao; }
    public Instant getInstante() { return instante; }
    public BigDecimal getValor() { return valor; }
    public boolean isConfirmado() { return confirmado; }
}
