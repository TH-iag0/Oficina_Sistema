package br.com.example.autogestao_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "servico")
public class Servico extends Entidade {
    @Column(nullable = false, length = 120)
    private String nome;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private CategoriaServico categoria;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_catalogo_id")
    private CategoriaCatalogo categoriaCatalogo;
    @Column(name = "preco_sugerido", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoSugerido;
    @Column(name = "custo_direto_estimado", nullable = false, precision = 15, scale = 2)
    private BigDecimal custoDiretoEstimado;
    @Column(nullable = false)
    private boolean ativo = true;

    protected Servico() {}
    public Servico(String nome, CategoriaServico categoria, BigDecimal precoSugerido, BigDecimal custoDiretoEstimado) {
        this.nome = RegrasModel.texto(nome, 120, "Serviço");
        this.categoria = Objects.requireNonNull(categoria);
        atualizarValores(precoSugerido, custoDiretoEstimado);
    }

    public void atualizarValores(BigDecimal preco, BigDecimal custo) {
        BigDecimal novoPreco = RegrasModel.dinheiro(preco);
        BigDecimal novoCusto = RegrasModel.dinheiro(custo);
        precoSugerido = novoPreco;
        custoDiretoEstimado = novoCusto;
    }

    public void desativar() {
        ativo = false;
    }

    public void atualizarCadastro(String nome, CategoriaServico categoria, BigDecimal preco, BigDecimal custo) {
        String novoNome = RegrasModel.texto(nome, 120, "Serviço");
        CategoriaServico novaCategoria = Objects.requireNonNull(categoria);
        atualizarValores(preco, custo);
        this.nome = novoNome;
        this.categoria = novaCategoria;
        this.categoriaCatalogo = null;
    }

    public String getNome() {
        return nome;
    }

    public CategoriaServico getCategoria() {
        return categoria;
    }

    public void definirCategoriaCatalogo(CategoriaCatalogo categoria) {
        this.categoriaCatalogo = categoria;
        if (categoria != null) this.categoria = categoria.getClassificacao();
    }

    public CategoriaCatalogo getCategoriaCatalogo() { return categoriaCatalogo; }

    public BigDecimal getPrecoSugerido() {
        return precoSugerido;
    }

    public BigDecimal getCustoDiretoEstimado() {
        return custoDiretoEstimado;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
