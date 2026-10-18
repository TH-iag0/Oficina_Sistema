package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.CategoriaCatalogoDtos;
import br.com.example.autogestao_backend.model.CategoriaCatalogo;
import br.com.example.autogestao_backend.repository.CategoriaCatalogoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoriaCatalogoService {
    private final CategoriaCatalogoRepository categorias;

    public CategoriaCatalogoService(CategoriaCatalogoRepository categorias) { this.categorias = categorias; }

    public List<CategoriaCatalogoDtos.Resposta> listar() {
        return categorias.findAll(Sort.by("nome", "id")).stream().map(this::resposta).toList();
    }

    @Transactional
    public CategoriaCatalogoDtos.Resposta cadastrar(CategoriaCatalogoDtos.Entrada dados) {
        var categoria = new CategoriaCatalogo(dados.nome());
        if (categorias.existsByNomeNormalizado(categoria.getNomeNormalizado())) {
            throw new ErroApi(HttpStatus.CONFLICT, "Já existe uma categoria com esse nome.");
        }
        return resposta(categorias.saveAndFlush(categoria));
    }

    private CategoriaCatalogoDtos.Resposta resposta(CategoriaCatalogo c) {
        return new CategoriaCatalogoDtos.Resposta(c.getId(), c.getNome(), c.getClassificacao(), c.isPadrao());
    }
}
