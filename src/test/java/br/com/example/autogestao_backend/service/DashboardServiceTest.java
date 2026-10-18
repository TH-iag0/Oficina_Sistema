package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.repository.ItemOrdemServicoRepository;
import br.com.example.autogestao_backend.repository.OrdemServicoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.SliceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardServiceTest {
    private final OrdemServicoRepository ordens = mock(OrdemServicoRepository.class);
    private final ItemOrdemServicoRepository itens = mock(ItemOrdemServicoRepository.class);
    private final ZoneId fuso = ZoneId.of("America/Sao_Paulo");
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), fuso);
    private final DashboardService service = new DashboardService(ordens, itens, clock, fuso);

    @Test
    void calculaCrescimentoLucroETicketComLimitesNoFusoDaOficina() {
        preparar();
        when(ordens.somarFaturamento(Instant.parse("2026-09-01T03:00:00Z"), Instant.parse("2026-10-01T03:00:00Z")))
            .thenReturn(new BigDecimal("800.00"));
        when(ordens.contarFinalizadas(any(), any())).thenReturn(2L);
        var resposta = service.resumo(YearMonth.of(2026, 10));
        assertEquals(new BigDecimal("25.00"), resposta.variacaoPercentual());
        assertEquals(new BigDecimal("600.00"), resposta.lucroBruto());
        assertEquals(new BigDecimal("60.00"), resposta.margemPercentual());
        assertEquals(new BigDecimal("500.00"), resposta.ticketMedio());
        verify(ordens).contarClientesAtendidos(Instant.parse("2026-10-05T03:00:00Z"), Instant.parse("2026-10-12T03:00:00Z"));
    }

    @Test
    void naoInventaPercentualSemBaseComparativa() {
        preparar();
        when(ordens.somarFaturamento(any(), any())).thenReturn(BigDecimal.ZERO);
        var resposta = service.resumo(YearMonth.of(2026, 10));
        assertNull(resposta.variacaoPercentual());
        assertNull(resposta.margemPercentual());
        assertEquals(new BigDecimal("0.00"), resposta.ticketMedio());
    }

    private void preparar() {
        when(ordens.somarFaturamento(any(), any())).thenReturn(new BigDecimal("1000.00"));
        when(ordens.somarCustosDiretos(any(), any())).thenReturn(new BigDecimal("400.00"));
        when(itens.findMaisExecutados(any(), any(), any())).thenReturn(new SliceImpl<>(List.of()));
    }
}
