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
import java.time.Instant;

@Entity
@Table(name = "historico_status_os", indexes = @Index(name = "idx_historico_os_instante", columnList = "ordem_servico_id, instante"))
public class HistoricoStatusOs extends Entidade {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ordem_servico_id", nullable = false, updatable = false)
    private OrdemServico ordemServico;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 24)
    private StatusOrdemServico status;
    @Column(nullable = false, updatable = false)
    private Instant instante;
    protected HistoricoStatusOs() {}
    HistoricoStatusOs(OrdemServico os, StatusOrdemServico status, Instant instante) {
        this.ordemServico = os;
        this.status = status;
        this.instante = instante;
    }

    public StatusOrdemServico getStatus() {
        return status;
    }

    public Instant getInstante() {
        return instante;
    }
}
