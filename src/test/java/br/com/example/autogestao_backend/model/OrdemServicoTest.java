package br.com.example.autogestao_backend.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.example.autogestao_backend.model.ModelFixtures.*;

class OrdemServicoTest {
    @Test void impedeOsComVeiculoDeOutroClienteOuNumeroInvalido() {
        var cliente = cliente();
        assertThrows(IllegalArgumentException.class, () -> new OrdemServico(1, cliente, veiculo(cliente()), RELOGIO));
        assertThrows(IllegalArgumentException.class, () -> new OrdemServico(0, cliente, veiculo(cliente), RELOGIO));
    }
    @Test void mantemPrecoHistoricoECalculaTotaisSemPontoFlutuante() {
        var os = os(); var catalogo = servico(CategoriaServico.MANUTENCAO);
        var item = os.adicionarServico(catalogo, 3, valor("0.10"), valor("0.03"));
        catalogo.atualizarValores(valor("999"), valor("500"));
        assertEquals(valor("0.30"), os.getTotalServicos());
        assertEquals(valor("0.09"), os.getTotalCustosDiretos());
        assertEquals(valor("0.10"), item.getPrecoUnitario());
        assertThrows(UnsupportedOperationException.class, () -> os.getItens().clear());
        assertThrows(UnsupportedOperationException.class, () -> os.getHistorico().clear());
    }
    @ParameterizedTest @ValueSource(strings = {"-1", "0.001", "10000000000000.00"})
    void rejeitaValoresQueNaoPodemSerArmazenadosSemPerda(String valorInvalido) {
        var os = os();
        assertThrows(IllegalArgumentException.class, () -> os.adicionarServico(servico(CategoriaServico.MANUTENCAO), 1, valor(valorInvalido), valor("0")));
        assertTrue(os.getItens().isEmpty());
    }
    @Test void rejeitaQuantidadeInvalidaServicoInativoETotalAcimaDoLimite() {
        var os = os(); var servico = servico(CategoriaServico.MANUTENCAO);
        assertThrows(IllegalArgumentException.class, () -> os.adicionarServico(servico, 0, valor("1"), valor("0")));
        assertThrows(IllegalArgumentException.class, () -> os.adicionarServico(servico, 2, valor("9999999999999.99"), valor("0")));
        os.adicionarServico(servico, 1, valor("9999999999999.99"), valor("0"));
        assertThrows(IllegalArgumentException.class, () -> os.adicionarServico(servico, 1, valor("0.01"), valor("0")));
        assertEquals(1, os.getItens().size());
        servico.desativar();
        assertThrows(IllegalStateException.class, () -> os.adicionarServico(servico, 1, valor("0"), valor("0")));
    }
    @Test void registraFluxoCompletoAprovacaoECustosReaisNoFechamento() {
        var os = os(); var item = item(os, CategoriaServico.MANUTENCAO);
        iniciar(os);
        item.registrarCustoReal(valor("60")); item.registrarRetrabalho("Refazer ajuste.");
        item.concluir(depois(3600)); os.transicionar(StatusOrdemServico.FINALIZADO, depois(3600));
        assertEquals(valor("150.00"), os.getReceitaFinalizada());
        assertEquals(valor("60.00"), os.getCustoDiretoFinalizado());
        assertEquals(valor("90.00"), os.getLucroBrutoFinalizado());
        assertTrue(item.isRetrabalho()); assertEquals(INSTANTE, os.getAprovadaEm());
        assertNotNull(os.getConsentimentoAprovacao());
        os.transicionar(StatusOrdemServico.ENTREGUE, depois(7200));
        assertEquals(INSTANTE.plusSeconds(7200), os.getEntregueEm());
        assertEquals(8, os.getHistorico().size());
        assertThrows(IllegalStateException.class, () -> item.registrarCustoReal(valor("10")));
        assertThrows(IllegalStateException.class, () -> os.transicionar(StatusOrdemServico.CANCELADO, depois(7201)));
    }
    @Test void impedePularEtapasOuAprovarSemConsentimentoEItens() {
        var os = os();
        assertThrows(IllegalStateException.class, () -> os.transicionar(StatusOrdemServico.EM_SERVICO, RELOGIO));
        assertThrows(IllegalStateException.class, () -> os.aprovar("Autorizado", RELOGIO));
        item(os, CategoriaServico.MANUTENCAO);
        os.transicionar(StatusOrdemServico.RECEBIDO, RELOGIO);
        os.transicionar(StatusOrdemServico.DIAGNOSTICO, RELOGIO);
        os.transicionar(StatusOrdemServico.AGUARDANDO_APROVACAO, RELOGIO);
        assertThrows(IllegalArgumentException.class, () -> os.aprovar(" ", RELOGIO));
        assertThrows(IllegalStateException.class, () -> os.transicionar(StatusOrdemServico.APROVADO, RELOGIO));
        assertThrows(IllegalStateException.class, () -> item(os, CategoriaServico.MANUTENCAO));
    }
    @Test void impedeFecharServicoPendenteEHorariosRegressivos() {
        var os = os(); var item = item(os, CategoriaServico.MANUTENCAO); iniciar(os);
        assertThrows(IllegalStateException.class, () -> os.transicionar(StatusOrdemServico.FINALIZADO, depois(100)));
        assertThrows(IllegalArgumentException.class, () -> item.concluir(depois(-1)));
        item.concluir(depois(100));
        assertThrows(IllegalStateException.class, () -> item.concluir(depois(200)));
        assertThrows(IllegalArgumentException.class, () -> os.transicionar(StatusOrdemServico.FINALIZADO, depois(99)));
        assertNull(os.getReceitaFinalizada()); assertEquals(StatusOrdemServico.EM_SERVICO, os.getStatus());
    }
    @Test void permiteEsperaDePecaMasCongelaOrdemCancelada() {
        var os = os(); item(os, CategoriaServico.MANUTENCAO); iniciar(os);
        os.transicionar(StatusOrdemServico.AGUARDANDO_PECA, depois(100));
        assertThrows(IllegalArgumentException.class, () -> os.transicionar(StatusOrdemServico.EM_SERVICO, depois(99)));
        os.transicionar(StatusOrdemServico.EM_SERVICO, depois(200));
        os.transicionar(StatusOrdemServico.CANCELADO, depois(300));
        assertThrows(IllegalStateException.class, () -> os.transicionar(StatusOrdemServico.RECEBIDO, depois(400)));
        assertNull(os.getReceitaFinalizada());
    }
}
