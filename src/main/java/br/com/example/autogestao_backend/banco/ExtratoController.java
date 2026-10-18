package br.com.example.autogestao_backend.banco;

import java.time.YearMonth;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro/extrato")
@PreAuthorize("hasRole('ADMIN')")
public class ExtratoController {
    private final ExtratoService service;
    public ExtratoController(ExtratoService service) { this.service = service; }

    @GetMapping
    public ExtratoService.Resposta consultar(@RequestParam YearMonth mes) {
        return service.consultar(mes);
    }

    @PostMapping("/sincronizacao")
    public ExtratoService.Resposta sincronizar(@RequestParam YearMonth mes) {
        service.sincronizar();
        return service.consultar(mes);
    }
}
