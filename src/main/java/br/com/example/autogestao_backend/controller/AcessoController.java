package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.service.AdminService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AcessoController {
    private final AdminService service;
    public AcessoController(AdminService service) {
        this.service = service;
    }

    @PutMapping("/api/acesso/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterar(@Valid @RequestBody OficinaDtos.SenhaEntrada dados, Principal usuario) {
        service.alterarSenha(usuario.getName(), dados);
    }
}
