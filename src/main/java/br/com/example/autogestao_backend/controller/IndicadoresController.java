package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.IndicadoresResposta;
import br.com.example.autogestao_backend.service.IndicadoresService;
import java.time.Clock;
import java.time.YearMonth;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class IndicadoresController {
    private final IndicadoresService service;
    private final Clock relogio;
    public IndicadoresController(IndicadoresService service, Clock relogio) {
        this.service = service;
        this.relogio = relogio;
    }

    @GetMapping("/api/dashboard/indicadores")
    public IndicadoresResposta indicadores(@RequestParam(required = false) YearMonth mes,
        @RequestParam(defaultValue = "6") int meses) {
        return service.indicadores(mes == null ? YearMonth.now(relogio) : mes, meses);
    }
}
