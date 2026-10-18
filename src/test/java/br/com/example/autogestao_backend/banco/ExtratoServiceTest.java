package br.com.example.autogestao_backend.banco;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExtratoServiceTest {
    private final PluggyClient client = mock(PluggyClient.class);
    private final ExtratoStore store = mock(ExtratoStore.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneOffset.UTC);

    private ExtratoService service(boolean configurado) {
        return new ExtratoService(new PluggyConfig(configurado ? "id" : "", "segredo-teste", "item"),
            client, store, mock(MovimentoRepository.class), clock, ZoneOffset.UTC);
    }

    @Test
    void naoConsultaSemCredenciaisOuSemPermissaoDeTentativa() {
        service(false).sincronizar();
        when(store.iniciar("item", clock.instant())).thenReturn(Optional.empty());
        service(true).sincronizar();
        verify(client, never()).ler(any());
    }

    @Test
    void consultaForaDeTransacaoEConcluiComIdentificadorDaTentativa() {
        var tentativa = new ExtratoStore.Tentativa("execucao", "item", clock.instant().minusSeconds(86400));
        when(store.iniciar("item", clock.instant())).thenReturn(Optional.of(tentativa));
        var dados = List.of(new PluggyClient.Conta("conta", List.of()));
        when(client.ler(any())).thenReturn(dados);
        service(true).sincronizar();
        verify(store).salvar(dados, LocalDate.of(2026, 9, 14), clock.instant(), tentativa, null);
    }

    @Test
    void falhaPreservaDadosEProgramaNovaTentativaSemExporSegredo() {
        var tentativa = new ExtratoStore.Tentativa("execucao", "item", null);
        when(store.iniciar("item", clock.instant())).thenReturn(Optional.of(tentativa));
        when(client.ler(any())).thenThrow(new IllegalStateException("segredo-teste"));
        service(true).sincronizar();
        verify(store, never()).salvar(any(), any(), any(), any(), any());
        verify(store).falhar(eq(tentativa), eq(clock.instant()), any(),
            org.mockito.ArgumentMatchers.argThat(m -> !m.contains("segredo-teste")));
    }

    @Test
    void recuperaIntervaloAposComputadorDesligadoPorMeses() {
        LocalDate hoje = LocalDate.of(2026, 10, 14);
        assertEquals(LocalDate.of(2026, 5, 25), ExtratoService.inicioBusca(Instant.parse("2026-06-01T12:00:00Z"), hoje));
        assertEquals(hoje.minusDays(365), ExtratoService.inicioBusca(null, hoje));
        assertEquals(hoje.minusDays(365), ExtratoService.inicioBusca(Instant.parse("2024-01-01T12:00:00Z"), hoje));
    }
}
