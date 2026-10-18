package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.repository.DespesaRepository;
import br.com.example.autogestao_backend.dto.AnaliseFinanceiraResposta.Mes;
import br.com.example.autogestao_backend.repository.EstornoPagamentoRepository;
import br.com.example.autogestao_backend.repository.PagamentoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnaliseFinanceiraServiceTest {
    private final PagamentoRepository pagamentos = mock(PagamentoRepository.class);
    private final DespesaRepository despesas = mock(DespesaRepository.class);
    private final EstornoPagamentoRepository estornos = mock(EstornoPagamentoRepository.class);
    private final AnaliseFinanceiraService service = new AnaliseFinanceiraService(pagamentos, despesas, estornos,
        ZoneId.of("America/Sao_Paulo"));

    @Test
    void separaSaidasPagasDeCompetenciaEPreservaSaldoNegativo() {
        when(pagamentos.somarRecebimentos(any(), any())).thenReturn(new BigDecimal("50.25"));
        when(despesas.somarPagas(any(), any())).thenReturn(new BigDecimal("60.10"));
        when(estornos.somarEstornos(any(), any())).thenReturn(new BigDecimal("5.05"));
        var categoria = mock(DespesaRepository.CategoriaTotal.class);
        when(categoria.getNome()).thenReturn("Estrutura");
        when(categoria.getValor()).thenReturn(new BigDecimal("1000.00"));
        when(despesas.somarPorCategoria(any(), any())).thenReturn(List.of(categoria));
        var resultado = service.consultar(YearMonth.of(2026, 10), 1);
        var mes = resultado.fluxoCaixa().getFirst();
        assertEquals(new BigDecimal("65.15"), mes.saidas());
        assertEquals(new BigDecimal("-14.90"), mes.saldo());
        assertEquals(new BigDecimal("1000.00"), resultado.despesasPorCategoria().getFirst().valor());
        verify(pagamentos).somarRecebimentos(Instant.parse("2026-10-01T03:00:00Z"), Instant.parse("2026-11-01T03:00:00Z"));
    }

    @Test
    void incluiMesesVaziosEmOrdemCronologicaNaViradaDoAno() {
        when(pagamentos.somarRecebimentos(any(), any())).thenReturn(BigDecimal.ZERO);
        when(despesas.somarPagas(any(), any())).thenReturn(BigDecimal.ZERO);
        when(estornos.somarEstornos(any(), any())).thenReturn(BigDecimal.ZERO);
        when(despesas.somarPorCategoria(any(), any())).thenReturn(List.of());
        var resultado = service.consultar(YearMonth.of(2027, 1), 2);
        assertEquals(List.of(YearMonth.of(2026, 12), YearMonth.of(2027, 1)),
            resultado.fluxoCaixa().stream().map(Mes::mes).toList());
        assertEquals(BigDecimal.ZERO, resultado.fluxoCaixa().getFirst().saldo());
        assertEquals(List.of(), resultado.despesasPorCategoria());
    }

    @Test
    void recusaPeriodosForaDoLimite() {
        var mes2 = YearMonth.of(2026, 10);
        assertThrows(IllegalArgumentException.class, () -> service.consultar(mes2, 0));
        var mes3 = YearMonth.of(2026, 10);
        assertThrows(IllegalArgumentException.class, () -> service.consultar(mes3, 25));
    }
}
