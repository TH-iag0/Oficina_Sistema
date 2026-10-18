package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.dto.PontoEntrada;
import br.com.example.autogestao_backend.dto.PontoResposta;
import br.com.example.autogestao_backend.service.LimitePonto;
import br.com.example.autogestao_backend.service.PontoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ponto")
@Validated
public class PontoController {
    private final PontoService service;
    private final LimitePonto limite;

    public PontoController(PontoService service, LimitePonto limite) {
        this.service = service;
        this.limite = limite;
    }

    @PostMapping("/marcacoes")
    public PontoResposta registrar(@Valid @RequestBody PontoEntrada entrada, HttpServletRequest request) {
        limite.verificar(request.getRemoteAddr());
        return service.registrar(entrada);
    }

    @GetMapping("/marcacoes")
    @PreAuthorize("hasRole('ADMIN')")
    public PaginaResposta<PontoResposta> historico(@RequestParam UUID funcionarioId,
        @RequestParam LocalDate inicio, @RequestParam LocalDate fim,
        @RequestParam(defaultValue = "0") @Min(0) int pagina,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return service.historico(funcionarioId, inicio, fim, pagina, tamanho);
    }
}
