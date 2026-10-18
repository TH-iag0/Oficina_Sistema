package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.repository.MarcacaoPontoRepository;
import br.com.example.autogestao_backend.support.IntegrationApiTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@IntegrationApiTest
class FluxosOficinaIntegrationTest {
    @Test
    void listagemDeOrdensCarregaColecoesEmLotes(@Autowired EntityManager em,
        @Autowired EntityManagerFactory factory) throws Exception {
        roteiro("MANUTENCAO");
        roteiro("DIAGNOSTICO");
        roteiro("REMAP_ECU");
        em.flush();
        em.clear();
        var estatisticas = factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean habilitadas = estatisticas.isStatisticsEnabled();
        try {
            estatisticas.setStatisticsEnabled(true);
            estatisticas.clear();
            assertEquals(3, consultar("/api/ordens").get("total").asInt());
            assertTrue(estatisticas.getPrepareStatementCount() <= 4,
                "Itens e histórico devem ser carregados em lote, sem duas consultas extras por OS.");
        } finally {
            estatisticas.setStatisticsEnabled(habilitadas);
        }
    }

    private static final AtomicInteger PLACAS = new AtomicInteger();

    @Test
    void horariosConfiguradosAlimentamCargaPrevistaEPreservamVigencias() throws Exception {
        String id = funcionario();
        String dados = "{\"dia\":\"MONDAY\",\"entrada\":\"08:00\",\"saida\":\"17:00\","
            + "\"intervaloMinutos\":60,\"toleranciaMinutos\":5,\"vigenciaInicio\":\"2026-10-05\"}";
        var jornada = chamar(post("/api/funcionarios/" + id + "/jornadas"), dados);
        var resumo = consultar("/api/ponto/resumo?funcionarioId=" + id + "&inicio=2026-10-05&fim=2026-10-06");
        assertEquals(480, resumo.get("dias").get(0).get("minutosPrevistos").asInt());
        falha(post("/api/funcionarios/" + id + "/jornadas"), dados, 409);
        chamar(put("/api/funcionarios/jornadas/" + jornada.get("id").asString() + "/encerramento?fim=2026-10-11"), null);
        String nova = dados.replace("08:00", "09:00").replace("2026-10-05", "2026-10-12");
        chamar(post("/api/funcionarios/" + id + "/jornadas"), nova);
        assertEquals(2, consultar("/api/funcionarios/" + id + "/jornadas").size());
        var antigo = consultar("/api/ponto/resumo?funcionarioId=" + id + "&inicio=2026-10-05&fim=2026-10-06");
        var atual = consultar("/api/ponto/resumo?funcionarioId=" + id + "&inicio=2026-10-12&fim=2026-10-13");
        assertEquals(480, antigo.get("dias").get(0).get("minutosPrevistos").asInt());
        assertEquals(420, atual.get("dias").get(0).get("minutosPrevistos").asInt());
    }
    private final MockMvc mvc;
    @Test
    void pontoPublicoNaoUsaPermissaoAdministrativaParaDispensarPin() throws Exception {
        long antes = marcacoes.count();
        mvc.perform(post("/api/ponto/marcacoes").with(csrf()).with(user("admin").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"matricula\":\"inexistente\",\"tipo\":\"ENTRADA\",\"requisicaoId\":\"" + UUID.randomUUID() + "\"}"))
            .andExpect(status().isBadRequest());
        assertEquals(antes, marcacoes.count());
    }

    @Test
    void operacaoAdministrativaContinuaExigindoCsrf() throws Exception {
        mvc.perform(post("/api/clientes").with(user("admin").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"tipo\":\"PF\",\"nome\":\"Cliente sem CSRF\"}"))
            .andExpect(status().isForbidden());
    }

    private final MarcacaoPontoRepository marcacoes;
    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    FluxosOficinaIntegrationTest(MockMvc mvc, MarcacaoPontoRepository marcacoes) {
        this.mvc = mvc;
        this.marcacoes = marcacoes;
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/veiculos", "/api/servicos", "/api/funcionarios", "/api/ordens",
        "/api/financeiro/pagamentos", "/api/financeiro/estornos", "/api/financeiro/despesas",
        "/api/tecnico/remaps", "/api/tecnico/diagnosticos", "/api/ponto/ajustes"})
    void protegeTodosOsCadastrosEValidaDto(String caminho) throws Exception {
        mvc.perform(post(caminho).header("Forwarded", "for=203.0.113.1").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post(caminho).with(user("funcionario").roles("USER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(post(caminho).with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test void fluxoCompletoFinalizaOsRecebeEAtualizaDashboard() throws Exception {
        Roteiro r = roteiro("MANUTENCAO");
        emServico(r);
        chamar(put("/api/ordens/itens/" + r.item + "/conclusao"), "{\"custoReal\":80}");
        chamar(put("/api/ordens/" + r.os + "/status"), "{\"status\":\"FINALIZADO\"}");
        String pedido = pagamento(r.os, 200);
        JsonNode recebido = chamar(post("/api/financeiro/pagamentos"), pedido);
        assertEquals(recebido.get("id").asString(), chamar(post("/api/financeiro/pagamentos"), pedido).get("id").asString());
        var resumo = consultar("/api/dashboard?mes=" + YearMonth.now(ZoneId.of("America/Sao_Paulo")));
        assertEquals(200.0, resumo.get("faturamento").asDouble());
        assertEquals(120.0, resumo.get("lucroBruto").asDouble());
        var indicadores = consultar("/api/dashboard/indicadores?meses=1");
        assertEquals(1, indicadores.get("evolucao").size());
        assertEquals(1, indicadores.get("servicosRentaveis").size());
    }

    @Test
    void descricaoDoTrabalhoFicaPersistidaNaOrdem(@Autowired EntityManager em) throws Exception {
        Roteiro r = roteiro("MANUTENCAO");
        emServico(r);
        chamar(put("/api/ordens/itens/" + r.item + "/conclusao"),
            "{\"custoReal\":80,\"relato\":\"Troca de óleo e filtro; teste de rodagem concluído.\"}");
        em.flush();
        em.clear();
        var ordem = consultar("/api/ordens/" + r.os);
        assertEquals("Troca de óleo e filtro; teste de rodagem concluído.",
            ordem.get("itens").get(0).get("relato").asString());
    }

    @Test void osNaoPodeUsarVeiculoDeOutroCliente() throws Exception {
        var r = roteiro("MANUTENCAO");
        var outro = chamar(post("/api/clientes"), "{\"tipo\":\"PF\",\"nome\":\"Outro\"}").get("id").asString();
        falha(post("/api/ordens"), "{\"clienteId\":\"" + outro + "\",\"veiculoId\":\"" + r.veiculo + "\"}", 400);
    }

    @Test void numeraOrdensSemRepetir() throws Exception {
        var primeira = roteiro("MANUTENCAO");
        var segunda = roteiro("MANUTENCAO");
        var a = consultar("/api/ordens/" + primeira.os).get("numero").asLong();
        var b = consultar("/api/ordens/" + segunda.os).get("numero").asLong();
        assertTrue(b > a);
    }

    @Test void pagamentoEEstornoRespeitamSaldos() throws Exception {
        var r = roteiro("MANUTENCAO");
        chamar(put("/api/ordens/" + r.os + "/status"), "{\"status\":\"RECEBIDO\"}");
        var recebido = chamar(post("/api/financeiro/pagamentos"), pagamento(r.os, 100)).get("id").asString();
        falha(post("/api/financeiro/pagamentos"), pagamento(r.os, 101), 409);
        chamar(post("/api/financeiro/estornos"), estorno(recebido, 80));
        falha(post("/api/financeiro/estornos"), estorno(recebido, 21), 409);
    }

    @Test void execucaoDeveEncerrarAntesDaConclusao() throws Exception {
        var r = roteiro("MANUTENCAO");
        emServico(r);
        String funcionario = funcionario();
        var execucao = chamar(post("/api/ordens/execucoes"), "{\"itemId\":\"" + r.item + "\",\"funcionarioId\":\"" + funcionario + "\"}").get("id").asString();
        falha(put("/api/ordens/itens/" + r.item + "/conclusao"), "{\"custoReal\":80}", 409);
        chamar(put("/api/ordens/execucoes/" + execucao + "/encerramento"), null);
        chamar(put("/api/ordens/itens/" + r.item + "/conclusao"), "{\"custoReal\":80}");
    }

    @Test void registraInformacoesTecnicasDeRemapEDiagnostico() throws Exception {
        var r = roteiro("REMAP_ECU");
        emServico(r);
        var remap = chamar(post("/api/tecnico/remaps"), "{\"itemId\":\"" + r.item + "\",\"tipo\":\"Stage 1\",\"fabricante\":\"Bosch\",\"modeloModulo\":\"EDC17\",\"objetivo\":\"Resposta\"}").get("id").asString();
        var alterado = chamar(put("/api/tecnico/remaps/" + remap), "{\"hardware\":\"HW1\",\"software\":\"SW1\",\"calibracao\":\"CAL1\",\"alteracoes\":\"Ajustes\"}");
        assertEquals("SW1", alterado.get("software").asString());
        var d = roteiro("DIAGNOSTICO");
        emServico(d);
        var diagnostico = chamar(post("/api/tecnico/diagnosticos"), "{\"itemId\":\"" + d.item + "\",\"sintomas\":\"Falha na partida\"}").get("id").asString();
        chamar(put("/api/tecnico/diagnosticos/" + diagnostico), "{\"testes\":\"Teste bateria\",\"causa\":\"Bateria\"}");
        assertEquals("Bateria", consultar("/api/tecnico/diagnosticos/" + diagnostico).get("causa").asString());
    }

    @Test void despesaPagaNaoDuplicaPagamento() throws Exception {
        var despesa = chamar(post("/api/financeiro/despesas"), "{\"descricao\":\"Energia\",\"categoria\":\"Operacional\",\"valor\":100,\"competencia\":\"2026-10-01\",\"vencimento\":\"2026-10-10\"}").get("id").asString();
        var primeira = chamar(put("/api/financeiro/despesas/" + despesa + "/pagamento"), null);
        var segunda = chamar(put("/api/financeiro/despesas/" + despesa + "/pagamento"), null);
        assertEquals(primeira.get("pagaEm").asString(), segunda.get("pagaEm").asString());
    }

    @Test void ajustaPontoSemApagarOriginalECalculaHoras() throws Exception {
        String f = funcionario();
        String id = UUID.randomUUID().toString();
        var ajuste = chamar(post("/api/ponto/ajustes"), ajuste(f, "ENTRADA", "2026-10-01T11:00:00Z", id));
        chamar(post("/api/ponto/ajustes"), ajuste(f, "SAIDA", "2026-10-01T19:00:00Z", UUID.randomUUID().toString()));
        var resumo = consultar("/api/ponto/resumo?funcionarioId=" + f + "&inicio=2026-10-01&fim=2026-10-02");
        assertEquals(480, resumo.get("minutosTrabalhados").asLong());
        String original = ajuste.get("substitutaId").asString();
        chamar(post("/api/ponto/ajustes"), "{\"funcionarioId\":\"" + f + "\",\"originalId\":\"" + original + "\",\"motivo\":\"Duplicidade\",\"requisicaoId\":\"" + UUID.randomUUID() + "\"}");
        assertTrue(marcacoes.findById(UUID.fromString(original)).isPresent());
        var corrigido = consultar("/api/ponto/resumo?funcionarioId=" + f + "&inicio=2026-10-01&fim=2026-10-02");
        assertEquals(0, corrigido.get("minutosTrabalhados").asLong());
        assertTrue(corrigido.get("dias").get(0).get("incompleto").asBoolean());
    }

    @Test void acessoLocalDispensaSenhaEEndpointDeSenhaFoiDesativado() throws Exception {
        mvc.perform(get("/api/csrf")).andExpect(status().isOk());
        mvc.perform(get("/api/dashboard")).andExpect(status().isOk());
        mvc.perform(put("/api/acesso/senha").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
    }

    private Roteiro roteiro(String categoria) throws Exception {
        var cliente = chamar(post("/api/clientes"), "{\"tipo\":\"PF\",\"nome\":\"João\"}").get("id").asString();
        String placa = "ABC" + String.format("%04d", PLACAS.incrementAndGet());
        var veiculo = chamar(post("/api/veiculos"), "{\"clienteId\":\"" + cliente + "\",\"placa\":\"" + placa + "\",\"marca\":\"VW\",\"modelo\":\"Golf\",\"quilometragem\":1000}").get("id").asString();
        var servico = chamar(post("/api/servicos"), "{\"nome\":\"Teste\",\"categoria\":\"" + categoria + "\",\"preco\":200,\"custo\":80}").get("id").asString();
        var os = chamar(post("/api/ordens"), "{\"clienteId\":\"" + cliente + "\",\"veiculoId\":\"" + veiculo + "\"}").get("id").asString();
        var itens = chamar(post("/api/ordens/" + os + "/itens"), "{\"servicoId\":\"" + servico + "\",\"quantidade\":1,\"preco\":200,\"custo\":80}").get("itens");
        return new Roteiro(cliente, veiculo, servico, os, itens.get(0).get("id").asString());
    }

    private void emServico(Roteiro r) throws Exception {
        for (String status : new String[]{"RECEBIDO", "DIAGNOSTICO", "AGUARDANDO_APROVACAO"}) {
            chamar(put("/api/ordens/" + r.os + "/status"), "{\"status\":\"" + status + "\"}");
        }
        chamar(put("/api/ordens/" + r.os + "/aprovacao"), "{\"consentimento\":\"Aprovado pelo cliente\"}");
        chamar(put("/api/ordens/" + r.os + "/status"), "{\"status\":\"EM_SERVICO\"}");
    }

    private String funcionario() throws Exception {
        return chamar(post("/api/funcionarios"), "{\"nome\":\"Carlos\",\"matricula\":\"" + UUID.randomUUID().toString().substring(0,16) + "\",\"pin\":\"123456\"}").get("id").asString();
    }

    private String pagamento(String os, int valor) {
        return "{\"ordemServicoId\":\"" + os + "\",\"valor\":" + valor + ",\"forma\":\"PIX\",\"requisicaoId\":\"" + UUID.randomUUID() + "\"}";
    }

    private String estorno(String pagamento, int valor) {
        return "{\"pagamentoId\":\"" + pagamento + "\",\"valor\":" + valor + ",\"motivo\":\"Ajuste\",\"requisicaoId\":\"" + UUID.randomUUID() + "\"}";
    }

    private String ajuste(String funcionario, String tipo, String instante, String pedido) {
        return "{\"funcionarioId\":\"" + funcionario + "\",\"tipo\":\"" + tipo + "\",\"instante\":\"" + instante + "\",\"motivo\":\"Complementação\",\"requisicaoId\":\"" + pedido + "\"}";
    }

    private JsonNode chamar(MockHttpServletRequestBuilder request, String corpo) throws Exception {
        request.with(user("admin").roles("ADMIN")).with(csrf());
        if (corpo != null) request.contentType(MediaType.APPLICATION_JSON).content(corpo);
        var resposta = mvc.perform(request).andReturn().getResponse();
        assertTrue(resposta.getStatus() >= 200 && resposta.getStatus() < 300, resposta.getContentAsString());
        return resposta.getContentAsString().isEmpty() ? null : mapper.readTree(resposta.getContentAsString());
    }

    private JsonNode consultar(String caminho) throws Exception {
        return chamar(get(caminho), null);
    }

    private void falha(MockHttpServletRequestBuilder request, String corpo, int status) throws Exception {
        mvc.perform(request.with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().is(status));
    }

    private record Roteiro(String cliente, String veiculo, String servico, String os, String item) {}
}
