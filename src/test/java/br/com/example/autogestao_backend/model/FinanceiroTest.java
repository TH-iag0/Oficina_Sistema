package br.com.example.autogestao_backend.model;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.example.autogestao_backend.model.ModelFixtures.*;

class FinanceiroTest {
    @Test void recebimentoParcialNaoSeConfundeComFaturamentoDaOs() {
        var os = os(); item(os, CategoriaServico.MANUTENCAO);
        os.transicionar(StatusOrdemServico.RECEBIDO, RELOGIO);
        var requisicao = UUID.randomUUID();
        var pagamento = new Pagamento(os, valor("50"), FormaPagamento.PIX, requisicao, depois(100));
        assertEquals(valor("50.00"), pagamento.getValor());
        assertEquals(valor("150.00"), os.getTotalServicos());
        assertNull(os.getReceitaFinalizada());
        assertEquals(requisicao, pagamento.getRequisicaoId());
        assertEquals(INSTANTE.plusSeconds(100), pagamento.getRecebidoEm());
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "0.001"})
    void rejeitaRecebimentoSemValorPositivoExato(String invalido) {
        var os = os(); os.transicionar(StatusOrdemServico.RECEBIDO, RELOGIO);
        assertThrows(IllegalArgumentException.class, () -> new Pagamento(os, valor(invalido), FormaPagamento.PIX, UUID.randomUUID(), RELOGIO));
    }
    @Test void rejeitaRecebimentoDeRascunhoCanceladoOuAnteriorAoRecebimento() {
        var os = os();
        assertThrows(IllegalStateException.class, () -> new Pagamento(os, valor("10"), FormaPagamento.PIX, UUID.randomUUID(), RELOGIO));
        os.transicionar(StatusOrdemServico.RECEBIDO, RELOGIO);
        assertThrows(IllegalArgumentException.class, () -> new Pagamento(os, valor("10"), FormaPagamento.PIX, UUID.randomUUID(), depois(-1)));
        os.transicionar(StatusOrdemServico.CANCELADO, RELOGIO);
        assertThrows(IllegalStateException.class, () -> new Pagamento(os, valor("10"), FormaPagamento.PIX, UUID.randomUUID(), RELOGIO));
    }
    @Test void estornoPreservaPagamentoOriginalEExigeValorMotivoECronologia() {
        var os = os(); os.transicionar(StatusOrdemServico.RECEBIDO, RELOGIO);
        var pagamento = new Pagamento(os, valor("50"), FormaPagamento.PIX, UUID.randomUUID(), RELOGIO);
        var estorno = new EstornoPagamento(pagamento, valor("20"), "Cancelamento parcial", UUID.randomUUID(), depois(100));
        assertSame(pagamento, estorno.getPagamento());
        assertEquals(valor("20.00"), estorno.getValor());
        assertEquals(valor("50.00"), pagamento.getValor());
        assertThrows(IllegalArgumentException.class, () -> new EstornoPagamento(pagamento, valor("51"), "Motivo", UUID.randomUUID(), RELOGIO));
        assertThrows(IllegalArgumentException.class, () -> new EstornoPagamento(pagamento, valor("10"), " ", UUID.randomUUID(), RELOGIO));
        assertThrows(IllegalArgumentException.class, () -> new EstornoPagamento(pagamento, valor("10"), "Motivo", UUID.randomUUID(), depois(-1)));
    }
    @Test void despesaSeparaCompetenciaVencimentoEPagamentoEfetivo() {
        var competencia = LocalDate.of(2026, 10, 1); var vencimento = LocalDate.of(2026, 11, 5);
        var despesa = new Despesa("Aluguel", "Instalações", valor("2000"), competencia, vencimento);
        assertNull(despesa.getPagaEm());
        assertEquals(competencia, despesa.getCompetencia()); assertEquals(vencimento, despesa.getVencimento());
        despesa.confirmarPagamento(RELOGIO);
        assertEquals(INSTANTE, despesa.getPagaEm());
        assertThrows(IllegalStateException.class, () -> despesa.confirmarPagamento(depois(100)));
        assertThrows(IllegalArgumentException.class, () -> new Despesa(" ", "Aluguel", valor("10"), competencia, vencimento));
        assertThrows(IllegalArgumentException.class, () -> new Despesa("Aluguel", "Instalações", valor("-1"), competencia, vencimento));
    }
}
