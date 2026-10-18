package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.FormaPagamento;
import br.com.example.autogestao_backend.model.Pagamento;
import br.com.example.autogestao_backend.repository.DespesaRepository;
import br.com.example.autogestao_backend.repository.EstornoPagamentoRepository;
import br.com.example.autogestao_backend.repository.OrdemServicoRepository;
import br.com.example.autogestao_backend.repository.PagamentoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceiroServiceTest {
    private final OrdemServicoRepository ordens = mock(OrdemServicoRepository.class);
    private final PagamentoRepository pagamentos = mock(PagamentoRepository.class);
    private final EstornoPagamentoRepository estornos = mock(EstornoPagamentoRepository.class);
    private final FinanceiroService service = new FinanceiroService(ordens, pagamentos, estornos, mock(DespesaRepository.class), FluxoFixtures.CLOCK);
    @Test void impedeRecebimentoAcimaDoSaldo() {
        var ordem = FluxoFixtures.ordem(CategoriaServico.MANUTENCAO);
        when(ordens.findParaAtualizacao(ordem.getId())).thenReturn(Optional.of(ordem));
        when(pagamentos.somarPorOrdemServico(ordem.getId())).thenReturn(new BigDecimal("90"));
        when(estornos.somarPorOrdemServico(ordem.getId())).thenReturn(BigDecimal.ZERO);
        var pagamentoEntrada = new OficinaDtos.PagamentoEntrada(ordem.getId(), new BigDecimal("11"), FormaPagamento.PIX, UUID.randomUUID());
        assertThrows(ErroApi.class, () -> service.receber(pagamentoEntrada));
        verify(pagamentos, never()).saveAndFlush(any());
    }

    @Test void repeticaoRetornaPagamentoOriginal() {
        var ordem = FluxoFixtures.ordem(CategoriaServico.MANUTENCAO);
        var id = UUID.randomUUID();
        var p = new Pagamento(ordem, new BigDecimal("50"), FormaPagamento.PIX, id, FluxoFixtures.CLOCK);
        when(ordens.findParaAtualizacao(ordem.getId())).thenReturn(Optional.of(ordem));
        when(pagamentos.findByRequisicaoId(id)).thenReturn(Optional.of(p));
        assertEquals(p.getId(), service.receber(new OficinaDtos.PagamentoEntrada(ordem.getId(), new BigDecimal("50"), FormaPagamento.PIX, id)).id());
        var pagamentoEntrada = new OficinaDtos.PagamentoEntrada(ordem.getId(), new BigDecimal("51"), FormaPagamento.PIX, id);
        assertThrows(ErroApi.class, () -> service.receber(pagamentoEntrada));
    }

    @Test void somatorioDeEstornosNaoPodeSuperarPagamento() {
        var ordem = FluxoFixtures.ordem(CategoriaServico.MANUTENCAO);
        var p = new Pagamento(ordem, new BigDecimal("100"), FormaPagamento.PIX, UUID.randomUUID(), FluxoFixtures.CLOCK);
        when(pagamentos.findById(p.getId())).thenReturn(Optional.of(p));
        when(ordens.findParaAtualizacao(ordem.getId())).thenReturn(Optional.of(ordem));
        when(pagamentos.findParaAtualizacao(p.getId())).thenReturn(Optional.of(p));
        when(estornos.somarPorPagamento(p.getId())).thenReturn(new BigDecimal("80"));
        var estornoEntrada = new OficinaDtos.EstornoEntrada(p.getId(), new BigDecimal("21"), "Ajuste", UUID.randomUUID());
        assertThrows(ErroApi.class, () -> service.estornar(estornoEntrada));
        verify(estornos, never()).saveAndFlush(any());
    }
}
