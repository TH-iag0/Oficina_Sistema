package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LimitePontoTest {
    @Test
    void limitaPorEnderecoELiberaAoFimDaJanela() {
        Clock clock = mock(Clock.class);
        Instant inicio = Instant.parse("2026-10-11T12:00:00Z");
        when(clock.instant()).thenReturn(inicio);
        LimitePonto limite = new LimitePonto(clock);
        for (int i = 0; i < 10; i++) {
            limite.verificar("127.0.0.1");
        }
        assertEquals(429, assertThrows(ErroApi.class, () -> limite.verificar("127.0.0.1")).getStatus().value());
        limite.verificar("127.0.0.2");
        when(clock.instant()).thenReturn(inicio.plusSeconds(60));
        limite.verificar("127.0.0.1");
    }
}
