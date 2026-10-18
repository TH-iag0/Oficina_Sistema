package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.service.VeiculoService;
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
@RequestMapping("/api/veiculos")
@PreAuthorize("hasRole('ADMIN')")
public class VeiculoController {
    private final VeiculoService service;
    public VeiculoController(VeiculoService service) {
        this.service = service;
    }

    @PostMapping
    public OficinaDtos.VeiculoResposta cadastrar(@Valid @RequestBody OficinaDtos.VeiculoEntrada dados) {
        return service.cadastrar(dados);
    }

    @GetMapping
    public PaginaResposta<OficinaDtos.VeiculoResposta> listar(
        @RequestParam(defaultValue = "") @jakarta.validation.constraints.Size(max = 160) String busca,
        @RequestParam(required = false) UUID clienteId,
        @RequestParam(required = false) Boolean ativo,
        @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int pagina,
        @RequestParam(defaultValue = "20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int tamanho) {
        return service.listar(busca, clienteId, ativo, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public OficinaDtos.VeiculoResposta buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public OficinaDtos.VeiculoResposta atualizar(@PathVariable UUID id, @Valid @RequestBody OficinaDtos.VeiculoEdicao dados) {
        return service.atualizar(id, dados);
    }

    @PutMapping("/{id}/revisao")
    public OficinaDtos.VeiculoResposta revisao(@PathVariable UUID id, @Valid @RequestBody OficinaDtos.RevisaoEntrada dados) {
        return service.revisao(id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable UUID id) {
        service.desativar(id);
    }
}
