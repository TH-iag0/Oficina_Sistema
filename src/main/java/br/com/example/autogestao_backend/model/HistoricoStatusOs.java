package br.com.example.autogestao_backend.model;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "historico_status_os", indexes = @Index(name = "idx_historico_os_instante", columnList = "ordem_servico_id, instante"))
public class HistoricoStatusOs extends Entidade {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ordem_servico_id", nullable = false, updatable = false)
    private OrdemServico ordemServico;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 24) private StatusOrdemServico status;
    @Column(nullable = false, updatable = false) private Instant instante;
    protected HistoricoStatusOs() {}
    HistoricoStatusOs(OrdemServico os, StatusOrdemServico status, Instant instante) {
        this.ordemServico = os; this.status = status; this.instante = instante;
    }
    public StatusOrdemServico getStatus() { return status; }
    public Instant getInstante() { return instante; }
}
