package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.CategoriaCatalogoDtos;
import br.com.example.autogestao_backend.service.CategoriaCatalogoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servicos/categorias")
@PreAuthorize("hasRole('ADMIN')")
public class CategoriaCatalogoController {
    private final CategoriaCatalogoService service;
    public CategoriaCatalogoController(CategoriaCatalogoService service) { this.service = service; }

    @GetMapping
    public List<CategoriaCatalogoDtos.Resposta> listar() { return service.listar(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaCatalogoDtos.Resposta cadastrar(@Valid @RequestBody CategoriaCatalogoDtos.Entrada dados) {
        return service.cadastrar(dados);
    }
}
