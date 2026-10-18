package br.com.example.autogestao_backend.banco;

import br.com.example.autogestao_backend.support.IntegrationApiTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@IntegrationApiTest
class ExtratoIntegrationTest {
    private final ExtratoStore store;
    private final MovimentoRepository movimentos;

    @Autowired
    ExtratoIntegrationTest(ExtratoStore store, MovimentoRepository movimentos) {
        this.store = store;
        this.movimentos = movimentos;
    }

    @Test
    void extratoLocalUsaDadosPersistidosEEscritaExigeCsrf(@Autowired org.springframework.test.web.servlet.MockMvc mvc) throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/financeiro/extrato?mes=2026-10"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.configurado").value(false));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/financeiro/extrato/sincronizacao?mes=2026-10"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/financeiro/extrato?mes=2026-10")
            .header("CF-Connecting-IP", "203.0.113.1"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }

    private int importacoes;

    private void importar(List<PluggyClient.Conta> contas, LocalDate desde) {
        Instant agora = Instant.parse("2026-10-14T12:00:00Z").plusSeconds(86400L * importacoes++);
        var tentativa = store.iniciar("item", agora).orElseThrow();
        store.salvar(contas, desde, agora, tentativa, null);
    }

    private PluggyClient.Movimento movimento(String id, String valor) {
        return new PluggyClient.Movimento(id, "Pix igual", Instant.parse("2026-10-14T12:00:00Z"),
            new BigDecimal(valor), true);
    }

    @Test
    void preservaDuasTransacoesIguaisEReimportaSemDuplicar() {
        var conta = new PluggyClient.Conta("conta", List.of(movimento("banco:1", "100.00"), movimento("banco:2", "100.00")));
        var hoje = LocalDate.of(2026, 10, 14);
        importar(List.of(conta), hoje);
        importar(List.of(conta), hoje);
        assertEquals(2, movimentos.count());
    }

    @Test
    void atualizaValorERetiraTransacaoRemovidaSemApagarHistorico() {
        var hoje = LocalDate.of(2026, 10, 14);
        importar(List.of(new PluggyClient.Conta("conta", List.of(movimento("1", "100.00"), movimento("2", "100.00")))), hoje);
        importar(List.of(new PluggyClient.Conta("conta", List.of(movimento("1", "90.00")))), hoje);
        assertEquals(2, movimentos.count());
        assertEquals(new BigDecimal("90.00"), movimentos.findByContaIdAndOrigemId("conta", "1").orElseThrow().getValor());
        assertFalse(movimentos.findByContaIdAndOrigemId("conta", "2").orElseThrow().isConfirmado());
    }
    @Test
    void bloqueiaExecucaoConcorrenteERecuperaAposExpirar() {
        Instant agora = Instant.parse("2026-10-14T12:00:00Z");
        var primeira = store.iniciar("item", agora).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(store.iniciar("item", agora).isEmpty());
        var segunda = store.iniciar("item", agora.plusSeconds(301)).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals(primeira.execucao(), segunda.execucao());
    }

    @Test
    void totaisConsideramMaisDe100RegistrosMasListagemELimitada() {
        var registros = java.util.stream.IntStream.range(0, 110)
            .mapToObj(i -> movimento("id" + i, i % 2 == 0 ? "10.00" : "-5.00")).toList();
        importar(List.of(new PluggyClient.Conta("conta", registros)), LocalDate.of(2026, 10, 1));
        Instant inicio = Instant.parse("2026-10-01T00:00:00Z");
        Instant fim = Instant.parse("2026-11-01T00:00:00Z");
        var totais = movimentos.totais(inicio, fim);
        assertEquals(110, totais.getTotal());
        assertEquals(new BigDecimal("550.00"), totais.getEntradas());
        assertEquals(new BigDecimal("275.00"), totais.getSaidas());
        assertEquals(100, movimentos.findFirst100ByConfirmadoTrueAndInstanteGreaterThanEqualAndInstanteLessThanOrderByInstanteDesc(inicio, fim).size());
    }

    @Test
    void identificadorIgualEmContasDiferentesNaoEliminaMovimentosLegitimos() {
        importar(List.of(new PluggyClient.Conta("conta1", List.of(movimento("id1", "100.00"))),
            new PluggyClient.Conta("conta2", List.of(movimento("id1", "100.00")))), LocalDate.of(2026, 10, 1));
        assertEquals(2, movimentos.count());
    }

    @Test
    void movimentosPendentesNaoAfetamTotaisEPassamAContarQuandoConfirmados() {
        var data = Instant.parse("2026-10-14T12:00:00Z");
        var pendente = new PluggyClient.Movimento("id1", "Pix", data, new BigDecimal("100.00"), false);
        var desde = LocalDate.of(2026, 10, 1);
        importar(List.of(new PluggyClient.Conta("conta", List.of(pendente))), desde);
        assertEquals(0, movimentos.totais(data.minusSeconds(1), data.plusSeconds(1)).getTotal());
        importar(List.of(new PluggyClient.Conta("conta", List.of(movimento("id1", "100.00")))), desde);
        assertEquals(1, movimentos.totais(data.minusSeconds(1), data.plusSeconds(1)).getTotal());
        assertEquals(1, movimentos.count());
    }

    @Test
    void periodoMensalRespeitaFusoDaOficina(@Autowired ExtratoService service) {
        var ultimo = new PluggyClient.Movimento("outubro", "Pix", Instant.parse("2026-11-01T02:59:59Z"), new BigDecimal("100.00"), true);
        var primeiro = new PluggyClient.Movimento("novembro", "Pix", Instant.parse("2026-11-01T03:00:00Z"), new BigDecimal("200.00"), true);
        importar(List.of(new PluggyClient.Conta("conta", List.of(ultimo, primeiro))), LocalDate.of(2026, 10, 1));
        assertEquals(new BigDecimal("100.00"), service.consultar(java.time.YearMonth.of(2026, 10)).entradas());
        assertEquals(new BigDecimal("200.00"), service.consultar(java.time.YearMonth.of(2026, 11)).entradas());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void falhaDuranteGravacaoDesfazTodoLote(@Autowired org.springframework.jdbc.core.JdbcTemplate jdbc) {
        Instant agora = Instant.parse("2026-10-14T12:00:00Z");
        var tentativa = store.iniciar("item", agora).orElseThrow();
        var invalido = movimento("invalido", "1.001");
        var lote = List.of(new PluggyClient.Conta("conta", List.of(movimento("valido", "100.00"), invalido)));
        try {
            var data = LocalDate.of(2026, 10, 1);
            org.junit.jupiter.api.Assertions.assertThrows(ArithmeticException.class,
                () -> store.salvar(lote, data, agora, tentativa, null));
            assertEquals(0, movimentos.count());
            org.junit.jupiter.api.Assertions.assertNull(store.estado().ultima());
        } finally {
            jdbc.update("UPDATE autogestao.sincronizacao_bancaria SET execucao_id = NULL, ocupado_ate = NULL WHERE id = 1");
        }
    }

}
