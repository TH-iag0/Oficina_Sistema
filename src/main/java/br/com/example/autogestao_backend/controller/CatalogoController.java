package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.service.CatalogoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@org.springframework.validation.annotation.Validated
@RestController
@RequestMapping("/api/servicos")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogoController {
    private final CatalogoService service;
    public CatalogoController(CatalogoService service) {
        this.service = service;
    }

    @PostMapping
    public OficinaDtos.ServicoResposta cadastrar(@Valid @RequestBody OficinaDtos.ServicoEntrada dados) {
        return service.cadastrar(dados);
    }

    @GetMapping
    public PaginaResposta<OficinaDtos.ServicoResposta> listar(
        @RequestParam(defaultValue = "") @jakarta.validation.constraints.Size(max = 160) String busca,
        
        @RequestParam(required = false) Boolean ativo,
        @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int pagina,
        @RequestParam(defaultValue = "20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int tamanho) {
        return service.listar(busca, ativo, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public OficinaDtos.ServicoResposta buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public OficinaDtos.ServicoResposta atualizar(@PathVariable UUID id, @Valid @RequestBody OficinaDtos.ServicoEntrada dados) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable UUID id) {
        service.desativar(id);
    }
}
