package br.com.example.autogestao_backend.banco;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PluggyClientTest {
    @Test
    void paginaNulaProduzErroControladoEmVezDeNullPointerException() {
        var desde = LocalDate.of(2026, 10, 1);
        assertThrows(IllegalStateException.class, () -> PluggyClient.proximaPagina(null, "conta", desde));
    }

    @Test
    void respostaVaziaDeContasNaoProduzExtratoParcial() {
        var builder = RestClient.builder().baseUrl("https://api.pluggy.ai");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.pluggy.ai/auth"))
            .andRespond(withSuccess("{\"apiKey\":\"token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.pluggy.ai/items/item"))
            .andRespond(withSuccess("{\"status\":\"UPDATED\",\"executionStatus\":\"SUCCESS\",\"lastUpdatedAt\":\"2026-10-14T12:00:00Z\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.pluggy.ai/accounts?itemId=item"))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        var client = new PluggyClient(new PluggyConfig("id", "segredo", "item"), builder.build());
        var desde = LocalDate.of(2026, 10, 1);
        assertThrows(IllegalStateException.class, () -> client.ler(desde));
        server.verify();
    }

    @Test
    void resposta429SemCabecalhosUsaEsperaPadrao() {
        var builder = RestClient.builder().baseUrl("https://api.pluggy.ai");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.pluggy.ai/auth")).andRespond(request -> {
            throw new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
        });
        var client = new PluggyClient(new PluggyConfig("id", "segredo", "item"), builder.build());
        var desde = LocalDate.of(2026, 10, 1);
        var erro = assertThrows(FalhaPluggy.class, () -> client.ler(desde));
        assertEquals(java.time.Duration.ofMinutes(1), erro.getEspera());
        server.verify();
    }

    @Test
    void percorreCursorSemForcarAtualizacaoDoBanco() {
        var builder = RestClient.builder().baseUrl("https://api.pluggy.ai");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.pluggy.ai/auth"))
            .andRespond(withSuccess("{\"apiKey\":\"token-teste\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.pluggy.ai/items/item"))
            .andRespond(withSuccess("{\"status\":\"UPDATED\",\"executionStatus\":\"SUCCESS\",\"lastUpdatedAt\":\"2026-10-14T12:00:00Z\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.pluggy.ai/accounts?itemId=item"))
            .andExpect(header("X-API-KEY", "token-teste"))
            .andRespond(withSuccess("{\"results\":[{\"id\":\"conta\",\"type\":\"BANK\",\"currencyCode\":\"BRL\"}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.pluggy.ai/v2/transactions?accountId=conta&dateFrom=2026-10-01"))
            .andRespond(withSuccess("{\"results\":[],\"next\":\"?accountId=conta&after=cursor\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.pluggy.ai/v2/transactions?accountId=conta&after=cursor"))
            .andRespond(withSuccess("{\"results\":[],\"next\":null}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.pluggy.ai/items/item"))
            .andRespond(withSuccess("{\"status\":\"UPDATED\",\"executionStatus\":\"SUCCESS\",\"lastUpdatedAt\":\"2026-10-14T12:00:00Z\"}", MediaType.APPLICATION_JSON));
        var client = new PluggyClient(new PluggyConfig("id", "segredo", "item"), builder.build());
        assertEquals(1, client.ler(LocalDate.of(2026, 10, 1)).size());
        server.verify();
    }

    @Test
    void normalizaSaidaEUsaIdentificadorDoBanco() {
        var node = new ObjectMapper().readTree("{\"id\":\"pluggy1\",\"providerId\":\"banco1\",\"description\":\"Pix\",\"type\":\"DEBIT\",\"amount\":100,\"date\":\"2026-10-14T12:00:00Z\",\"currencyCode\":\"BRL\",\"status\":\"POSTED\"}");
        var dado = PluggyClient.movimento(node);
        assertEquals("banco:banco1", dado.origem());
        assertEquals(0, new BigDecimal("-100").compareTo(dado.valor()));
        var respostaInvalida = new ObjectMapper().readTree("{}");
        assertThrows(IllegalStateException.class, () -> PluggyClient.movimento(respostaInvalida));
    }
    @Test
    void recusaCursorSemFimExplicitoComOutraContaOuOutroServidor() {
        var mapper = new ObjectMapper();
        LocalDate desde = LocalDate.of(2026, 10, 1);
        for (String resposta : new String[] {
            "{}", "{\"next\":\"https://outro.example/?accountId=conta&after=x\"}",
            "{\"next\":\"?accountId=outra&after=x\"}",
            "{\"next\":\"?accountId=conta&after=x&dateFrom=2026-10-10\"}",
            "{\"next\":\"?accountId=conta&accountId=outra&after=x\"}"
        }) {
            var respostaInvalida = mapper.readTree(resposta);
            assertThrows(IllegalStateException.class, () -> PluggyClient.proximaPagina(respostaInvalida, "conta", desde));
        }
    }

    @Test
    void respeitaRetryAfterSemExporCorpoDeErro() {
        var builder = RestClient.builder().baseUrl("https://api.pluggy.ai");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.pluggy.ai/auth"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                .withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "180").body("segredo-no-corpo"));
        var client = new PluggyClient(new PluggyConfig("id", "segredo", "item"), builder.build());
        var data = LocalDate.of(2026, 10, 1);
        var erro = assertThrows(FalhaPluggy.class, () -> client.ler(data));
        assertEquals(java.time.Duration.ofSeconds(180), erro.getEspera());
        org.junit.jupiter.api.Assertions.assertFalse(erro.getMessage().contains("segredo"));
        server.verify();
    }

    private record Cenario(PluggyClient client, MockRestServiceServer server) {}

    private Cenario prepararConta() {
        var builder = RestClient.builder().baseUrl("https://api.pluggy.ai");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.pluggy.ai/auth"))
            .andRespond(withSuccess("{\"apiKey\":\"token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.pluggy.ai/items/item"))
            .andRespond(withSuccess("{\"status\":\"UPDATED\",\"executionStatus\":\"SUCCESS\",\"lastUpdatedAt\":\"2026-10-14T12:00:00Z\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.pluggy.ai/accounts?itemId=item"))
            .andRespond(withSuccess("{\"results\":[{\"id\":\"conta\",\"type\":\"BANK\",\"currencyCode\":\"BRL\"}]}", MediaType.APPLICATION_JSON));
        return new Cenario(new PluggyClient(new PluggyConfig("id", "segredo", "item"), builder.build()), server);
    }

    @Test
    void falhaNaSegundaPaginaImpedeRetornoDeExtratoParcial() {
        var cenario = prepararConta();
        cenario.server().expect(requestTo("https://api.pluggy.ai/v2/transactions?accountId=conta&dateFrom=2026-10-01"))
            .andRespond(withSuccess("{\"results\":[],\"next\":\"?accountId=conta&after=x\"}", MediaType.APPLICATION_JSON));
        cenario.server().expect(requestTo("https://api.pluggy.ai/v2/transactions?accountId=conta&after=x"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());
        var client = cenario.client();
        var data = LocalDate.of(2026, 10, 1);
        assertThrows(FalhaPluggy.class, () -> client.ler(data));
        cenario.server().verify();
    }

    @Test
    void rejeitaColetaQueMudouDuranteAPaginacao() {
        var cenario = prepararConta();
        cenario.server().expect(requestTo("https://api.pluggy.ai/v2/transactions?accountId=conta&dateFrom=2026-10-01"))
            .andRespond(withSuccess("{\"results\":[],\"next\":null}", MediaType.APPLICATION_JSON));
        cenario.server().expect(requestTo("https://api.pluggy.ai/items/item"))
            .andRespond(withSuccess("{\"status\":\"UPDATED\",\"executionStatus\":\"SUCCESS\",\"lastUpdatedAt\":\"2026-10-14T12:01:00Z\"}", MediaType.APPLICATION_JSON));
        var client = cenario.client();
        var data = LocalDate.of(2026, 10, 1);
        assertThrows(IllegalStateException.class, () -> client.ler(data));
        cenario.server().verify();
    }

    @Test
    void naoInterpretaRespostaSemCursorComoExtratoCompleto() {
        var cenario = prepararConta();
        cenario.server().expect(requestTo("https://api.pluggy.ai/v2/transactions?accountId=conta&dateFrom=2026-10-01"))
            .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));
        var client = cenario.client();
        var data = LocalDate.of(2026, 10, 1);
        assertThrows(IllegalStateException.class, () -> client.ler(data));
        cenario.server().verify();
    }

}
