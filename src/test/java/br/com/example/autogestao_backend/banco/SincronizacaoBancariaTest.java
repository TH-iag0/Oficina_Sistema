package br.com.example.autogestao_backend.banco;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SincronizacaoBancariaTest {
    private final Instant agora = Instant.parse("2026-10-14T12:00:00Z");

    @Test
    void impedeConcorrenciaRecuperaLeaseExpiradoERejeitaConclusaoAntiga() {
        var estado = new SincronizacaoBancaria();
        String antigo = estado.iniciar("item", agora);
        assertNull(estado.iniciar("item", agora.plusSeconds(30)));
        String novo = estado.iniciar("item", agora.plusSeconds(301));
        assertNotNull(novo);
        var dados = agora.plusSeconds(302);
        assertThrows(IllegalStateException.class, () -> estado.concluir(antigo, "item", dados, null));
        estado.concluir(novo, "item", agora.plusSeconds(302), null);
    }

    @Test
    void aplica24HorasEPedeConciliacaoAoTrocarConexao() {
        var estado = new SincronizacaoBancaria();
        String execucao = estado.iniciar("item", agora);
        estado.concluir(execucao, "item", agora.plusSeconds(1), null);
        assertNull(estado.iniciar("item", agora.plusSeconds(86399)));
        assertNull(estado.iniciar("outro", agora.plusSeconds(90000)));
        assertNotNull(estado.getAviso());
        assertNotNull(estado.iniciar("item", agora.plusSeconds(90000)));
    }

    @Test
    void retryRespeitaServidorEBackoffESobreviveNoEstadoPersistido() {
        var estado = new SincronizacaoBancaria();
        String execucao = estado.iniciar("item", agora);
        estado.falhar(execucao, agora, Duration.ofSeconds(180), "Limite");
        assertEquals(agora.plusSeconds(180), estado.getProximaTentativa());
        assertNull(estado.iniciar("item", agora.plusSeconds(179)));
        String segunda = estado.iniciar("item", agora.plusSeconds(180));
        estado.falhar(segunda, agora.plusSeconds(180), Duration.ZERO, "Rede");
        assertEquals(agora.plusSeconds(300), estado.getProximaTentativa());
        assertNull(estado.getSucesso());
    }
}
