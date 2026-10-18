package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.AnaliseFinanceiraResposta;
import br.com.example.autogestao_backend.service.AnaliseFinanceiraService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.YearMonth;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/financeiro/analises")
@PreAuthorize("hasRole('ADMIN')")
public class AnaliseFinanceiraController {
    private final AnaliseFinanceiraService service;

    public AnaliseFinanceiraController(AnaliseFinanceiraService service) {
        this.service = service;
    }

    @GetMapping
    public AnaliseFinanceiraResposta consultar(@RequestParam YearMonth mes,
        @RequestParam(defaultValue = "6") @Min(1) @Max(24) int meses) {
        return service.consultar(mes, meses);
    }
}
