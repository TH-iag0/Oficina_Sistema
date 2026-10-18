package br.com.example.autogestao_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Locale;
import java.text.Normalizer;

@Entity
@Table(name = "categoria_servico")
public class CategoriaCatalogo extends Entidade {
    @Column(nullable = false, length = 80)
    private String nome;
    @Column(name = "nome_normalizado", nullable = false, unique = true, length = 80)
    private String nomeNormalizado;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CategoriaServico classificacao = CategoriaServico.OUTRO;
    @Column(nullable = false)
    private boolean padrao;

    protected CategoriaCatalogo() {}

    public CategoriaCatalogo(String nome) {
        String normalizado = nome == null ? null : Normalizer.normalize(nome, Normalizer.Form.NFC)
            .replaceAll("(?U)\\s+", " ");
        this.nome = RegrasModel.texto(normalizado, 80, "Categoria");
        this.nomeNormalizado = this.nome.toLowerCase(Locale.ROOT);
    }

    public String getNome() { return nome; }
    public String getNomeNormalizado() { return nomeNormalizado; }
    public CategoriaServico getClassificacao() { return classificacao; }
    public boolean isPadrao() { return padrao; }
}
