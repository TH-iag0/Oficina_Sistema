package br.com.example.autogestao_backend.model;

import java.math.BigDecimal;
import java.time.*;
import java.util.Objects;
import jakarta.persistence.*;

@Entity
@Table(name = "item_ordem_servico", indexes = @Index(name = "idx_item_conclusao", columnList = "concluido_em"))
public class ItemOrdemServico extends Entidade {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ordem_servico_id", nullable = false, updatable = false)
    private OrdemServico ordemServico;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "servico_id", nullable = false, updatable = false)
    private Servico servico;
    @Column(name = "descricao_servico", nullable = false, updatable = false, length = 120) private String descricaoServico;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 24) private CategoriaServico categoria;
    @Column(nullable = false, updatable = false) private int quantidade;
    @Column(name = "preco_unitario", nullable = false, updatable = false, precision = 15, scale = 2) private BigDecimal precoUnitario;
    @Column(name = "custo_direto_unitario", nullable = false, precision = 15, scale = 2) private BigDecimal custoDiretoUnitario;
    @Column(name = "concluido_em") private Instant concluidoEm;
    @Column(nullable = false) private boolean retrabalho;
    @Column(name = "motivo_retrabalho", length = 500) private String motivoRetrabalho;

    protected ItemOrdemServico() {}
    ItemOrdemServico(OrdemServico os, Servico servico, int quantidade, BigDecimal preco, BigDecimal custo) {
        this.ordemServico = Objects.requireNonNull(os); this.servico = Objects.requireNonNull(servico);
        if (!servico.isAtivo()) throw new IllegalStateException("Serviço inativo.");
        if (quantidade <= 0) throw new IllegalArgumentException("Quantidade deve ser positiva.");
        this.quantidade = quantidade; this.precoUnitario = RegrasModel.dinheiro(preco); this.custoDiretoUnitario = RegrasModel.dinheiro(custo);
        RegrasModel.dinheiro(getTotalPreco()); RegrasModel.dinheiro(getTotalCusto());
        descricaoServico = servico.getNome(); categoria = servico.getCategoria();
    }
    public void registrarCustoReal(BigDecimal custo) {
        exigirEmServico();
        if (concluidoEm != null) throw new IllegalStateException("Serviço já concluído.");
        BigDecimal novo = RegrasModel.dinheiro(custo);
        RegrasModel.dinheiro(ordemServico.getTotalCustosDiretos().subtract(getTotalCusto()).add(novo.multiply(BigDecimal.valueOf(quantidade))));
        custoDiretoUnitario = novo;
    }
    public void registrarRetrabalho(String motivo) {
        exigirEmServico();
        if (concluidoEm != null) throw new IllegalStateException("Serviço já concluído.");
        motivoRetrabalho = RegrasModel.texto(motivo, 500, "Motivo do retrabalho"); retrabalho = true;
    }
    public void concluir(Clock relogio) {
        exigirEmServico();
        if (concluidoEm != null) throw new IllegalStateException("Serviço já concluído.");
        Instant agora = Objects.requireNonNull(relogio).instant();
        RegrasModel.cronologia(ordemServico.getHistorico().getLast().getInstante(), agora);
        concluidoEm = agora;
    }
    void exigirEmServico() {
        if (ordemServico.getStatus() != StatusOrdemServico.EM_SERVICO) throw new IllegalStateException("OS precisa estar em serviço.");
    }
    public BigDecimal getTotalPreco() { return precoUnitario.multiply(BigDecimal.valueOf(quantidade)); }
    public BigDecimal getTotalCusto() { return custoDiretoUnitario.multiply(BigDecimal.valueOf(quantidade)); }
    public OrdemServico getOrdemServico() { return ordemServico; }
    public Servico getServico() { return servico; }
    public String getDescricaoServico() { return descricaoServico; }
    public CategoriaServico getCategoria() { return categoria; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public BigDecimal getCustoDiretoUnitario() { return custoDiretoUnitario; }
    public Instant getConcluidoEm() { return concluidoEm; }
    public boolean isRetrabalho() { return retrabalho; }
    public String getMotivoRetrabalho() { return motivoRetrabalho; }
}
