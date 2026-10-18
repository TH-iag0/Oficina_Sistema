package br.com.example.autogestao_backend.model;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static br.com.example.autogestao_backend.model.ModelFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class CadastrosTest {
    @Test void permiteDocumentoOpcionalENormalizaDocumentoQuandoInformado() {
        assertNull(cliente().getDocumento());
        var pf = new Cliente(TipoCliente.PF, "  João  ", "123.456.789-09", null, RELOGIO);
        var pj = new Cliente(TipoCliente.PJ, "Oficina", "12.345.678/0001-95", null, RELOGIO);
        assertEquals("João", pf.getNome());
        assertEquals("12345678909", pf.getDocumento());
        assertEquals("12345678000195", pj.getDocumento());
        assertEquals(INSTANTE, pf.getCadastradoEm());
        pf.desativar();
        assertFalse(pf.isAtivo());
    }

    @Test void rejeitaCadastroSemNomeOuComDocumentoIncompativel() {
        assertThrows(IllegalArgumentException.class, () -> new Cliente(TipoCliente.PF, " ", null, null, RELOGIO));
        assertThrows(IllegalArgumentException.class, () -> new Cliente(TipoCliente.PJ, "Oficina", "12345678909", null, RELOGIO));
        assertThrows(IllegalArgumentException.class, () -> new Cliente(TipoCliente.PF, "João", "abc12345678909", null, RELOGIO));
    }

    @Test void aceitaPlacasAntigaEMercosulEImpedeQuilometragemRegressiva() {
        var antigo = new Veiculo(cliente(), "abc-1234", "VW", "Golf", 50000);
        assertEquals("ABC1234", antigo.getPlaca());
        assertEquals("ABC1D23", veiculo(cliente()).getPlaca());
        antigo.atualizarQuilometragem(51000);
        assertThrows(IllegalArgumentException.class, () -> antigo.atualizarQuilometragem(50000));
        assertEquals(51000, antigo.getQuilometragem());
        antigo.desativar();
        assertFalse(antigo.isAtivo());
    }

    @ParameterizedTest @ValueSource(strings = {"ABC", "1234567", "ABC-DDDD", "ABC1D2!"})
    void rejeitaPlacasInvalidas(String placa) {
        var cliente2 = cliente();
        assertThrows(IllegalArgumentException.class, () -> new Veiculo(cliente2, placa, "VW", "Golf", 0));
    }

    @Test void programaRevisaoPorDataOuKmSemInventarQuilometragem() {
        var veiculo = veiculo(cliente());
        var data = LocalDate.of(2027, 1, 10);
        veiculo.programarRevisao(data, null);
        assertEquals(data, veiculo.getProximaRevisaoData());
        assertNull(veiculo.getProximaRevisaoKm());
        veiculo.programarRevisao(null, 60000L);
        assertEquals(60000L, veiculo.getProximaRevisaoKm());
        assertThrows(IllegalArgumentException.class, () -> veiculo.programarRevisao(null, null));
        assertThrows(IllegalArgumentException.class, () -> veiculo.programarRevisao(data, 50000L));
        assertEquals(60000L, veiculo.getProximaRevisaoKm());
    }

    @Test void atualizacaoInvalidaDoCatalogoNaoAlteraValoresAnteriores() {
        var servico = servico(CategoriaServico.MANUTENCAO);
        var valorTeste = valor("200");
        var valorTeste2 = valor("-1");
        assertThrows(IllegalArgumentException.class, () -> servico.atualizarValores(valorTeste, valorTeste2));
        assertEquals(valor("150.00"), servico.getPrecoSugerido());
        assertEquals(valor("40.00"), servico.getCustoDiretoEstimado());
        servico.desativar();
        assertFalse(servico.isAtivo());
    }
}
