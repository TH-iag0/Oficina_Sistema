package br.com.example.autogestao_backend.model;

import java.time.*;
import java.util.Objects;
import jakarta.persistence.*;

/** Um período de trabalho efetivo em um serviço, distinto das marcações de presença. */
@Entity
@Table(name = "execucao_servico", indexes = @Index(name = "idx_execucao_funcionario_inicio", columnList = "funcionario_id, inicio"))
public class ExecucaoServico extends Entidade {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "item_os_id", nullable = false, updatable = false)
    private ItemOrdemServico item;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "funcionario_id", nullable = false, updatable = false)
    private Funcionario funcionario;
    @Column(nullable = false, updatable = false) private Instant inicio;
    @Column private Instant fim;
    protected ExecucaoServico() {}
    public ExecucaoServico(ItemOrdemServico item, Funcionario funcionario, Clock relogio) {
        this.item = Objects.requireNonNull(item); this.funcionario = Objects.requireNonNull(funcionario);
        item.exigirEmServico();
        if (!funcionario.isAtivo()) throw new IllegalStateException("Funcionário inativo.");
        if (item.getConcluidoEm() != null) throw new IllegalStateException("Serviço já concluído.");
        inicio = Objects.requireNonNull(relogio).instant();
        RegrasModel.cronologia(item.getOrdemServico().getHistorico().getLast().getInstante(), inicio);
    }
    public void encerrar(Clock relogio) {
        if (fim != null) throw new IllegalStateException("Período de execução já encerrado.");
        Instant agora = Objects.requireNonNull(relogio).instant();
        RegrasModel.cronologia(inicio, agora);
        if (item.getConcluidoEm() != null && agora.isAfter(item.getConcluidoEm()))
            throw new IllegalStateException("Encerre os períodos de execução antes de concluir o serviço.");
        fim = agora;
    }
    /** Uma execução aberta não tem duração final; o dashboard pode mostrá-la em andamento. */
    public Duration getDuracao() { return fim == null ? null : Duration.between(inicio, fim); }
    public ItemOrdemServico getItem() { return item; }
    public Funcionario getFuncionario() { return funcionario; }
    public Instant getInicio() { return inicio; }
    public Instant getFim() { return fim; }
}
