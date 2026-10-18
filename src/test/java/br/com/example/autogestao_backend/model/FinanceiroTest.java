package br.com.example.autogestao_backend.model;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static br.com.example.autogestao_backend.model.ModelFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class FinanceiroTest {
    @Test void recebimentoParcialNaoSeConfundeComFaturamentoDaOs() {
        var os = os();
        item(os, CategoriaServico.MANUTENCAO);
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
        var os = os();
        os.transicionar(StatusOrdemServico.RECEBIDO, RELOGIO);
        var valorTeste = valor(invalido);
        var requisicaoId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new Pagamento(os, valorTeste, FormaPagamento.PIX, requisicaoId, RELOGIO));
    }

    @Test void rejeitaRecebimentoDeRascunhoCanceladoOuAnteriorAoRecebimento() {
        var os = os();
        var valorTeste = valor("10");
        var requisicaoId = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> new Pagamento(os, valorTeste, FormaPagamento.PIX, requisicaoId, RELOGIO));
        os.transicionar(StatusOrdemServico.RECEBIDO, RELOGIO);
        var valorTeste2 = valor("10");
        var requisicaoId2 = UUID.randomUUID();
        var relogio = depois(-1);
        assertThrows(IllegalArgumentException.class, () -> new Pagamento(os, valorTeste2, FormaPagamento.PIX, requisicaoId2, relogio));
        os.transicionar(StatusOrdemServico.CANCELADO, RELOGIO);
        var valorTeste3 = valor("10");
        var requisicaoId3 = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> new Pagamento(os, valorTeste3, FormaPagamento.PIX, requisicaoId3, RELOGIO));
    }

    @Test void estornoPreservaPagamentoOriginalEExigeValorMotivoECronologia() {
        var os = os();
        os.transicionar(StatusOrdemServico.RECEBIDO, RELOGIO);
        var pagamento = new Pagamento(os, valor("50"), FormaPagamento.PIX, UUID.randomUUID(), RELOGIO);
        var estorno = new EstornoPagamento(pagamento, valor("20"), "Cancelamento parcial", UUID.randomUUID(), depois(100));
        assertSame(pagamento, estorno.getPagamento());
        assertEquals(valor("20.00"), estorno.getValor());
        assertEquals(valor("50.00"), pagamento.getValor());
        var valorTeste = valor("51");
        var requisicaoId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new EstornoPagamento(pagamento, valorTeste, "Motivo", requisicaoId, RELOGIO));
        var valorTeste2 = valor("10");
        var requisicaoId2 = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new EstornoPagamento(pagamento, valorTeste2, " ", requisicaoId2, RELOGIO));
        var valorTeste3 = valor("10");
        var requisicaoId3 = UUID.randomUUID();
        var relogio = depois(-1);
        assertThrows(IllegalArgumentException.class, () -> new EstornoPagamento(pagamento, valorTeste3, "Motivo", requisicaoId3, relogio));
    }

    @Test void despesaSeparaCompetenciaVencimentoEPagamentoEfetivo() {
        var competencia = LocalDate.of(2026, 10, 1);
        var vencimento = LocalDate.of(2026, 11, 5);
        var despesa = new Despesa("Aluguel", "Instalações", valor("2000"), competencia, vencimento);
        assertNull(despesa.getPagaEm());
        assertEquals(competencia, despesa.getCompetencia());
        assertEquals(vencimento, despesa.getVencimento());
        despesa.confirmarPagamento(RELOGIO);
        assertEquals(INSTANTE, despesa.getPagaEm());
        var relogio = depois(100);
        assertThrows(IllegalStateException.class, () -> despesa.confirmarPagamento(relogio));
        var valorTeste = valor("10");
        assertThrows(IllegalArgumentException.class, () -> new Despesa(" ", "Aluguel", valorTeste, competencia, vencimento));
        var valorTeste2 = valor("-1");
        assertThrows(IllegalArgumentException.class, () -> new Despesa("Aluguel", "Instalações", valorTeste2, competencia, vencimento));
    }
}
