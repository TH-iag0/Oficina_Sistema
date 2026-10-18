package br.com.example.autogestao_backend.dto;

import br.com.example.autogestao_backend.model.CategoriaServico;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class CategoriaCatalogoDtos {
    private CategoriaCatalogoDtos() {}
    public record Entrada(@NotBlank @Size(max = 80) String nome) {}
    public record Resposta(UUID id, String nome, CategoriaServico classificacao, boolean padrao) {}
}
