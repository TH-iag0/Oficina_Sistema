package br.com.example.autogestao_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** O serviço deve validar o somatório de estornos sob bloqueio transacional do pagamento. */
@Entity
@Table(name = "estorno_pagamento", uniqueConstraints = @UniqueConstraint(name = "uk_estorno_confirmacao", columnNames = "requisicao_id"),
    indexes = @Index(name = "idx_estorno_instante", columnList = "instante"))
public class EstornoPagamento extends Entidade {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "pagamento_id", nullable = false, updatable = false)
    private Pagamento pagamento;
    @Column(nullable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal valor;
    @Column(nullable = false, updatable = false, length = 500)
    private String motivo;
    @Column(nullable = false, updatable = false)
    private Instant instante;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "requisicao_id", nullable = false, updatable = false, length = 36)
    private UUID requisicaoId;
    protected EstornoPagamento() {}
    public EstornoPagamento(Pagamento pagamento, BigDecimal valor, String motivo, UUID requisicaoId, Clock relogio) {
        this.pagamento = Objects.requireNonNull(pagamento);
        this.valor = RegrasModel.positivo(valor);
        if (this.valor.compareTo(pagamento.getValor()) > 0) {
            throw new IllegalArgumentException("Estorno supera o pagamento.");
        }
        this.motivo = RegrasModel.texto(motivo, 500, "Motivo");
        this.requisicaoId = Objects.requireNonNull(requisicaoId);
        instante = Objects.requireNonNull(relogio).instant();
        RegrasModel.cronologia(pagamento.getRecebidoEm(), instante);
    }

    public Pagamento getPagamento() {
        return pagamento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getInstante() {
        return instante;
    }

    public UUID getRequisicaoId() {
        return requisicaoId;
    }
}
