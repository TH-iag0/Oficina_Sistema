package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.service.ExclusaoCadastroService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class ExclusaoCadastroController {
    private final ExclusaoCadastroService service;

    public ExclusaoCadastroController(ExclusaoCadastroService service) { this.service = service; }

    @DeleteMapping("/api/clientes/{id}/exclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cliente(@PathVariable UUID id) { service.excluirCliente(id); }

    @DeleteMapping("/api/veiculos/{id}/exclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void veiculo(@PathVariable UUID id) { service.excluirVeiculo(id); }

    @DeleteMapping("/api/servicos/{id}/exclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void servico(@PathVariable UUID id) { service.excluirServico(id); }
}
