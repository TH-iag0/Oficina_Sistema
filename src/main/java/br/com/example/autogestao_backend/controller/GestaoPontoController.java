package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.service.AjustePontoService;
import br.com.example.autogestao_backend.service.ResumoPontoService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class GestaoPontoController {
    private final AjustePontoService ajustes;
    private final ResumoPontoService resumos;
    public GestaoPontoController(AjustePontoService ajustes, ResumoPontoService resumos) {
        this.ajustes = ajustes;
        this.resumos = resumos;
    }

    @PostMapping("/api/ponto/ajustes")
    public OficinaDtos.AjusteResposta ajustar(@Valid @RequestBody OficinaDtos.AjustePontoEntrada dados, Principal usuario) {
        return ajustes.ajustar(dados, usuario.getName());
    }

    @GetMapping("/api/ponto/ajustes/{id}")
    public OficinaDtos.AjusteResposta ajuste(@PathVariable UUID id) {
        return ajustes.buscar(id);
    }

    @GetMapping("/api/ponto/resumo")
    public OficinaDtos.ResumoPonto resumo(@RequestParam UUID funcionarioId, @RequestParam LocalDate inicio, @RequestParam LocalDate fim) {
        return resumos.resumo(funcionarioId, inicio, fim);
    }
}
