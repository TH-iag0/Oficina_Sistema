package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.ClienteEntrada;
import br.com.example.autogestao_backend.dto.ClienteResposta;
import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.service.ClienteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class ClienteController {
    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ClienteResposta> cadastrar(@Valid @RequestBody ClienteEntrada entrada) {
        ClienteResposta cliente = service.cadastrar(entrada);
        return ResponseEntity.created(URI.create("/api/clientes/" + cliente.id())).body(cliente);
    }

    @GetMapping
    public PaginaResposta<ClienteResposta> listar(@RequestParam(defaultValue = "") @Size(max = 160) String nome,
        @RequestParam(defaultValue = "0") @Min(0) int pagina,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return service.listar(nome, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public ClienteResposta buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public ClienteResposta atualizar(@PathVariable UUID id, @Valid @RequestBody ClienteEntrada entrada) {
        return service.atualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        service.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
