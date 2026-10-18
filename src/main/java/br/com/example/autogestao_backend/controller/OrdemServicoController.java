package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.service.OrdemServicoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import br.com.example.autogestao_backend.model.StatusOrdemServico;
import org.springframework.validation.annotation.Validated;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/ordens")
@PreAuthorize("hasRole('ADMIN')")
public class OrdemServicoController {
    private final OrdemServicoService service;
    public OrdemServicoController(OrdemServicoService service) {
        this.service = service;
    }

    @PostMapping
    public OficinaDtos.OsResposta abrir(@Valid @RequestBody OficinaDtos.OsEntrada dados) {
        return service.abrir(dados);
    }

    @GetMapping
    public PaginaResposta<OficinaDtos.OsResposta> listar(
        @RequestParam(defaultValue = "") @Size(max = 160) String busca,
        @RequestParam(required = false) StatusOrdemServico status,
        @RequestParam(defaultValue = "0") @Min(0) int pagina,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return service.listar(busca, status, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public OficinaDtos.OsResposta buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PostMapping("/{id}/itens")
    public OficinaDtos.OsResposta adicionar(@PathVariable UUID id, @Valid @RequestBody OficinaDtos.ItemEntrada dados) {
        return service.adicionar(id, dados);
    }

    @PutMapping("/{id}/status")
    public OficinaDtos.OsResposta status(@PathVariable UUID id, @Valid @RequestBody OficinaDtos.StatusEntrada dados) {
        return service.status(id, dados);
    }

    @PutMapping("/{id}/aprovacao")
    public OficinaDtos.OsResposta aprovar(@PathVariable UUID id, @Valid @RequestBody OficinaDtos.AprovacaoEntrada dados) {
        return service.aprovar(id, dados);
    }

    @PutMapping("/itens/{id}/conclusao")
    public OficinaDtos.ItemResposta concluir(@PathVariable UUID id, @Valid @RequestBody OficinaDtos.ConclusaoEntrada dados) {
        return service.concluirItem(id, dados);
    }

    @PostMapping("/execucoes")
    public OficinaDtos.ExecucaoResposta iniciar(@Valid @RequestBody OficinaDtos.ExecucaoEntrada dados) {
        return service.iniciar(dados);
    }

    @PutMapping("/execucoes/{id}/encerramento")
    public OficinaDtos.ExecucaoResposta encerrar(@PathVariable UUID id) {
        return service.encerrar(id);
    }

    @GetMapping("/itens/{id}/execucoes")
    public PaginaResposta<OficinaDtos.ExecucaoResposta> execucoes(@PathVariable UUID id, @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) { return service.execucoes(id, pagina, tamanho); }
}
