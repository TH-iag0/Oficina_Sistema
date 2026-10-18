package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.repository.ClienteRepository;
import br.com.example.autogestao_backend.repository.DespesaRepository;
import br.com.example.autogestao_backend.repository.DiagnosticoRepository;
import br.com.example.autogestao_backend.repository.EstornoPagamentoRepository;
import br.com.example.autogestao_backend.repository.ItemOrdemServicoRepository;
import br.com.example.autogestao_backend.repository.OrdemServicoRepository;
import br.com.example.autogestao_backend.repository.PagamentoRepository;
import br.com.example.autogestao_backend.repository.RemapRepository;
import br.com.example.autogestao_backend.repository.VeiculoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndicadoresServiceTest {
    private final OrdemServicoRepository ordens = mock(OrdemServicoRepository.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final VeiculoRepository veiculos = mock(VeiculoRepository.class);
    private final DespesaRepository despesas = mock(DespesaRepository.class);
    private final PagamentoRepository pagamentos = mock(PagamentoRepository.class);
    private final EstornoPagamentoRepository estornos = mock(EstornoPagamentoRepository.class);
    private final IndicadoresService service = new IndicadoresService(ordens, clientes, veiculos, despesas, pagamentos, estornos,
        mock(DiagnosticoRepository.class), mock(RemapRepository.class), mock(ItemOrdemServicoRepository.class),
        FluxoFixtures.CLOCK.getZone(), FluxoFixtures.CLOCK);

    @BeforeEach
    void prepararIndicadores() {
        when(ordens.somarFaturamento(any(), any())).thenReturn(new BigDecimal("100"));
        when(ordens.somarCustosDiretos(any(), any())).thenReturn(new BigDecimal("40"));
        when(despesas.somarPorCompetencia(any(), any())).thenReturn(new BigDecimal("20"));
        when(pagamentos.somarRecebimentos(any(), any())).thenReturn(new BigDecimal("150"));
        when(estornos.somarEstornos(any(), any())).thenReturn(BigDecimal.TEN);
        when(ordens.contarClientesAtendidos(any(), any())).thenReturn(2L);
        when(ordens.contarRecorrentes(any(), any())).thenReturn(1L);
        when(ordens.findTemposConclusao(any(), any(), any())).thenReturn(new SliceImpl<>(List.of()));
        when(ordens.findIntervalosVisitas(any(), any(), any())).thenReturn(new SliceImpl<>(List.of()));
        when(veiculos.findRevisoesAte(any(), any())).thenReturn(Page.empty());
    }

    @Test
    void separaCompetenciaDeCaixaECobreSemanasSemLacunas() {
        var resultado = service.indicadores(YearMonth.of(2026, 10), 1);
        var mes = resultado.evolucao().getFirst();
        assertEquals(0, mes.lucroBruto().compareTo(new BigDecimal("60")));
        assertEquals(0, mes.resultadoOperacional().compareTo(new BigDecimal("40")));
        assertEquals(0, mes.recebimentos().compareTo(new BigDecimal("150")));
        assertEquals(1, mes.clientesNovos());
        var semanas = resultado.atendimentosSemanais();
        assertEquals(LocalDate.of(2026, 10, 1), semanas.getFirst().inicio());
        assertEquals(LocalDate.of(2026, 11, 1), semanas.getLast().fimExclusivo());
        for (int i = 1; i < semanas.size(); i++) {
            assertEquals(semanas.get(i - 1).fimExclusivo(), semanas.get(i).inicio());
        }
    }

    @Test
    void mediasPaginasIgnoramPrimeiraVisitaEUsamTodasAsPaginas() {
        Instant inicio = Instant.parse("2026-10-01T03:00:00Z");
        var visitas = List.<OrdemServicoRepository.IntervaloAtendimento>of(
            new Intervalo(null, inicio), new Intervalo(inicio, inicio.plusSeconds(2 * 86400)));
        when(ordens.findIntervalosVisitas(any(), any(), any())).thenReturn(
            new SliceImpl<>(visitas, PageRequest.of(0, 100), true))
            .thenReturn(new SliceImpl<>(List.of(new Intervalo(inicio, inicio.plusSeconds(4 * 86400)))));
        when(ordens.findTemposConclusao(any(), any(), any())).thenReturn(new SliceImpl<>(List.of(
            new Intervalo(inicio, inicio.plusSeconds(5400)), new Intervalo(inicio, inicio.plusSeconds(1800)))));
        var resultado = service.indicadores(YearMonth.of(2026, 10), 1);
        assertEquals(new BigDecimal("3.00"), resultado.diasMediosEntreVisitas());
        assertEquals(new BigDecimal("3600.00"), resultado.segundosMediosConclusao());
        verify(ordens, times(2)).findIntervalosVisitas(any(), any(), any());
    }

    @Test
    void impedeConsultasSemLimiteDeMeses() {
        var mes2 = YearMonth.of(2026, 10);
        assertThrows(IllegalArgumentException.class, () -> service.indicadores(mes2, 25));
        var mes3 = YearMonth.of(2026, 10);
        assertThrows(IllegalArgumentException.class, () -> service.indicadores(mes3, 0));
    }

    private record Intervalo(Instant inicio, Instant fim) implements OrdemServicoRepository.IntervaloAtendimento {
        @Override
        public Instant getInicio() {
            return inicio;
        }

        @Override
        public Instant getFim() {
            return fim;
        }
    }
}
