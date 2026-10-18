package br.com.example.autogestao_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Recebimento efetivo. Não altera o valor dos serviços nem comprova liquidação bancária. */
@Entity
@Table(name = "pagamento", uniqueConstraints = @UniqueConstraint(name = "uk_pagamento_confirmacao", columnNames = "requisicao_id"),
    indexes = @Index(name = "idx_pagamento_recebido", columnList = "recebido_em"))
public class Pagamento extends Entidade {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ordem_servico_id", nullable = false, updatable = false)
    private OrdemServico ordemServico;
    @Column(nullable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal valor;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 24)
    private FormaPagamento forma;
    @Column(name = "recebido_em", nullable = false, updatable = false)
    private Instant recebidoEm;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "requisicao_id", nullable = false, updatable = false, length = 36)
    private UUID requisicaoId;
    protected Pagamento() {}
    public Pagamento(OrdemServico os, BigDecimal valor, FormaPagamento forma, UUID requisicaoId, Clock relogio) {
        this.ordemServico = Objects.requireNonNull(os);
        if (os.getStatus() == StatusOrdemServico.RASCUNHO || os.getStatus() == StatusOrdemServico.CANCELADO)
            throw new IllegalStateException("OS não permite receber pagamento.");
        this.valor = RegrasModel.positivo(valor);
        this.forma = Objects.requireNonNull(forma);
        this.requisicaoId = Objects.requireNonNull(requisicaoId);
        this.recebidoEm = Objects.requireNonNull(relogio).instant();
        RegrasModel.cronologia(os.getRecebidaEm(), recebidoEm);
    }

    public OrdemServico getOrdemServico() {
        return ordemServico;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public FormaPagamento getForma() {
        return forma;
    }

    public Instant getRecebidoEm() {
        return recebidoEm;
    }

    public UUID getRequisicaoId() {
        return requisicaoId;
    }
}
