package br.com.example.autogestao_backend.model;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import jakarta.persistence.*;

@Entity
@Table(name = "ordem_servico", uniqueConstraints = @UniqueConstraint(name = "uk_os_numero", columnNames = "numero"),
    indexes = {@Index(name = "idx_os_status", columnList = "status"), @Index(name = "idx_os_finalizada", columnList = "finalizada_em")})
public class OrdemServico extends Entidade {
    @Column(nullable = false, updatable = false) private long numero;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "veiculo_id", nullable = false, updatable = false)
    private Veiculo veiculo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private StatusOrdemServico status;
    @Column(name = "aberta_em", nullable = false, updatable = false) private Instant abertaEm;
    @Column(name = "recebida_em") private Instant recebidaEm;
    @Column(name = "aprovada_em") private Instant aprovadaEm;
    @Column(name = "consentimento_aprovacao", length = 1000) private String consentimentoAprovacao;
    @Column(name = "finalizada_em") private Instant finalizadaEm;
    @Column(name = "entregue_em") private Instant entregueEm;
    @Column(name = "previsao_entrega") private Instant previsaoEntrega;
    @Column(name = "receita_finalizada", precision = 15, scale = 2) private BigDecimal receitaFinalizada;
    @Column(name = "custo_direto_finalizado", precision = 15, scale = 2) private BigDecimal custoDiretoFinalizado;
    @OneToMany(mappedBy = "ordemServico", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("instante ASC") private List<HistoricoStatusOs> historico = new ArrayList<>();
    @OneToMany(mappedBy = "ordemServico", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<ItemOrdemServico> itens = new ArrayList<>();

    protected OrdemServico() {}
    /** O número sequencial deve ser reservado pelo serviço dentro de uma transação. */
    public OrdemServico(long numero, Cliente cliente, Veiculo veiculo, Clock relogio) {
        if (numero <= 0) throw new IllegalArgumentException("Número da OS deve ser positivo.");
        this.cliente = Objects.requireNonNull(cliente);
        this.veiculo = Objects.requireNonNull(veiculo);
        if (!veiculo.getCliente().getId().equals(cliente.getId())) throw new IllegalArgumentException("Veículo pertence a outro cliente.");
        this.numero = numero; this.abertaEm = Objects.requireNonNull(relogio).instant();
        this.status = StatusOrdemServico.RASCUNHO;
        historico.add(new HistoricoStatusOs(this, status, abertaEm));
    }
    public ItemOrdemServico adicionarServico(Servico servico, int quantidade, BigDecimal precoUnitario, BigDecimal custoUnitario) {
        if (status != StatusOrdemServico.RASCUNHO && status != StatusOrdemServico.RECEBIDO && status != StatusOrdemServico.DIAGNOSTICO)
            throw new IllegalStateException("Itens só podem ser incluídos antes de solicitar aprovação.");
        var item = new ItemOrdemServico(this, servico, quantidade, precoUnitario, custoUnitario);
        // Também garante que o total agregado cabe no banco antes de modificar a coleção.
        RegrasModel.dinheiro(getTotalServicos().add(item.getTotalPreco()));
        RegrasModel.dinheiro(getTotalCustosDiretos().add(item.getTotalCusto()));
        itens.add(item); return item;
    }
    public void aprovar(String consentimento, Clock relogio) {
        String texto = RegrasModel.texto(consentimento, 1000, "Consentimento");
        if (itens.isEmpty()) throw new IllegalStateException("Não é possível aprovar OS sem serviços.");
        mudarStatus(StatusOrdemServico.APROVADO, Objects.requireNonNull(relogio).instant());
        consentimentoAprovacao = texto; aprovadaEm = historico.getLast().getInstante();
    }
    public void transicionar(StatusOrdemServico destino, Clock relogio) {
        if (destino == StatusOrdemServico.APROVADO) throw new IllegalStateException("Use aprovar para registrar o consentimento.");
        mudarStatus(destino, Objects.requireNonNull(relogio).instant());
    }
    private void mudarStatus(StatusOrdemServico destino, Instant instante) {
        if (!status.permite(destino)) throw new IllegalStateException("Transição de OS inválida.");
        RegrasModel.cronologia(historico.getLast().getInstante(), instante);
        if (destino == StatusOrdemServico.FINALIZADO) {
            if (itens.isEmpty() || itens.stream().anyMatch(i -> i.getConcluidoEm() == null))
                throw new IllegalStateException("Conclua os serviços antes de finalizar a OS.");
            if (itens.stream().anyMatch(i -> instante.isBefore(i.getConcluidoEm())))
                throw new IllegalArgumentException("Finalização anterior à conclusão de um serviço.");
            receitaFinalizada = getTotalServicos(); custoDiretoFinalizado = getTotalCustosDiretos(); finalizadaEm = instante;
        }
        if (destino == StatusOrdemServico.RECEBIDO) recebidaEm = instante;
        if (destino == StatusOrdemServico.ENTREGUE) entregueEm = instante;
        status = destino; historico.add(new HistoricoStatusOs(this, destino, instante));
    }
    public void definirPrevisaoEntrega(Instant previsao) {
        RegrasModel.cronologia(abertaEm, previsao); previsaoEntrega = previsao;
    }
    public BigDecimal getTotalServicos() { return itens.stream().map(ItemOrdemServico::getTotalPreco).reduce(new BigDecimal("0.00"), BigDecimal::add); }
    public BigDecimal getTotalCustosDiretos() { return itens.stream().map(ItemOrdemServico::getTotalCusto).reduce(new BigDecimal("0.00"), BigDecimal::add); }
    public BigDecimal getLucroBrutoFinalizado() { return receitaFinalizada == null ? null : receitaFinalizada.subtract(custoDiretoFinalizado); }
    public long getNumero() { return numero; }
    public Cliente getCliente() { return cliente; }
    public Veiculo getVeiculo() { return veiculo; }
    public StatusOrdemServico getStatus() { return status; }
    public Instant getAbertaEm() { return abertaEm; }
    public Instant getRecebidaEm() { return recebidaEm; }
    public Instant getAprovadaEm() { return aprovadaEm; }
    public String getConsentimentoAprovacao() { return consentimentoAprovacao; }
    public Instant getFinalizadaEm() { return finalizadaEm; }
    public Instant getEntregueEm() { return entregueEm; }
    public Instant getPrevisaoEntrega() { return previsaoEntrega; }
    public BigDecimal getReceitaFinalizada() { return receitaFinalizada; }
    public BigDecimal getCustoDiretoFinalizado() { return custoDiretoFinalizado; }
    public List<ItemOrdemServico> getItens() { return Collections.unmodifiableList(itens); }
    public List<HistoricoStatusOs> getHistorico() { return Collections.unmodifiableList(historico); }
}
