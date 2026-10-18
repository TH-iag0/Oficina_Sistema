package br.com.example.autogestao_backend.banco;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExtratoAgenda {
    private final ExtratoService service;
    public ExtratoAgenda(ExtratoService service) { this.service = service; }

    @Scheduled(initialDelay = 30000, fixedDelay = 60000)
    public void consultarQuandoNecessario() {
        service.sincronizar();
    }
}
