package br.com.example.autogestao_backend.model;

import java.math.BigDecimal;
import java.util.Objects;
import jakarta.persistence.*;

@Entity
@Table(name = "servico")
public class Servico extends Entidade {
    @Column(nullable = false, length = 120) private String nome;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private CategoriaServico categoria;
    @Column(name = "preco_sugerido", nullable = false, precision = 15, scale = 2) private BigDecimal precoSugerido;
    @Column(name = "custo_direto_estimado", nullable = false, precision = 15, scale = 2) private BigDecimal custoDiretoEstimado;
    @Column(nullable = false) private boolean ativo = true;

    protected Servico() {}
    public Servico(String nome, CategoriaServico categoria, BigDecimal precoSugerido, BigDecimal custoDiretoEstimado) {
        this.nome = RegrasModel.texto(nome, 120, "Serviço");
        this.categoria = Objects.requireNonNull(categoria);
        atualizarValores(precoSugerido, custoDiretoEstimado);
    }
    public void atualizarValores(BigDecimal preco, BigDecimal custo) {
        BigDecimal novoPreco = RegrasModel.dinheiro(preco);
        BigDecimal novoCusto = RegrasModel.dinheiro(custo);
        precoSugerido = novoPreco; custoDiretoEstimado = novoCusto;
    }
    public void desativar() { ativo = false; }
    public String getNome() { return nome; }
    public CategoriaServico getCategoria() { return categoria; }
    public BigDecimal getPrecoSugerido() { return precoSugerido; }
    public BigDecimal getCustoDiretoEstimado() { return custoDiretoEstimado; }
    public boolean isAtivo() { return ativo; }
}
