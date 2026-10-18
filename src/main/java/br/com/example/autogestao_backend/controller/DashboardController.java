package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.DashboardResposta;
import br.com.example.autogestao_backend.service.DashboardService;
import java.time.Clock;
import java.time.YearMonth;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {
    private final DashboardService service;
    private final Clock relogio;

    public DashboardController(DashboardService service, Clock relogio) {
        this.service = service;
        this.relogio = relogio;
    }

    @GetMapping
    public DashboardResposta resumo(@RequestParam(required = false) YearMonth mes) {
        return service.resumo(mes == null ? YearMonth.now(relogio) : mes);
    }
}
