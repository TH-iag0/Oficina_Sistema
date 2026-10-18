package br.com.example.autogestao_backend.banco;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
public class PluggyClient {
    private static final String STATUS = "status";
    private static final String ACCOUNT_ID = "accountId";
    public record Movimento(String origem, String descricao, Instant instante, BigDecimal valor, boolean confirmado) {}
    public record Conta(String id, List<Movimento> movimentos) {}
    private final RestClient http;
    private final PluggyConfig config;

    @org.springframework.beans.factory.annotation.Autowired
    public PluggyClient(PluggyConfig config) {
        this(config, cliente());
    }

    PluggyClient(PluggyConfig config, RestClient http) {
        this.config = config;
        this.http = http;
    }

    private static RestClient cliente() {
        var transporte = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        var factory = new JdkClientHttpRequestFactory(transporte);
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().baseUrl("https://api.pluggy.ai").requestFactory(factory).build();
    }

    public List<Conta> ler(LocalDate desde) {
        long limite = System.nanoTime() + Duration.ofMinutes(4).toNanos();
        try {
            var auth = http.post().uri("/auth").body(Map.of("clientId", config.clientId,
                "clientSecret", config.clientSecret)).retrieve().body(JsonNode.class);
            String token = texto(auth, "apiKey");
            String versao = validarItem(token, limite);
            List<Conta> resultado = contas(token, desde, limite);
            if (!versao.equals(validarItem(token, limite))) throw new IllegalStateException("O extrato mudou durante a leitura.");
            if (resultado.isEmpty()) throw new IllegalStateException("Nenhuma conta bancária BRL disponível no consentimento.");
            return resultado;
        } catch (org.springframework.web.client.RestClientResponseException erro) {
            throw falhaHttp(erro);
        } catch (RestClientException erro) {
            throw new FalhaPluggy("A Pluggy não respondeu. A consulta será repetida sem alterar os dados anteriores.", Duration.ofMinutes(1));
        }
    }

    private JsonNode consultar(String rota, String token, long limite) {
        if (System.nanoTime() >= limite) throw new IllegalStateException("Tempo máximo de consulta excedido.");
        var resposta = http.get().uri(rota).header("X-API-KEY", token).retrieve().body(JsonNode.class);
        if (System.nanoTime() >= limite) throw new IllegalStateException("Tempo máximo de consulta excedido.");
        if (resposta == null) throw new IllegalStateException("Resposta bancária vazia.");
        return resposta;
    }

    private String validarItem(String token, long limite) {
        var item = consultar("/items/" + java.net.URLEncoder.encode(config.itemId,
            java.nio.charset.StandardCharsets.UTF_8), token, limite);
        if (!"UPDATED".equals(texto(item, STATUS)) || !"SUCCESS".equals(texto(item, "executionStatus"))) {
            throw new FalhaPluggy("A conexão bancária não está pronta. Confira a autorização no Meu Pluggy.", Duration.ofMinutes(30));
        }
        var detalhe = item.path("statusDetail");
        if (!detalhe.isNull() && !detalhe.isMissingNode()) {
            throw new FalhaPluggy("O provedor informou uma coleta parcial. Os dados anteriores foram preservados.", Duration.ofMinutes(30));
        }
        return texto(item, "lastUpdatedAt");
    }

    private List<Conta> contas(String token, LocalDate desde, long limite) {
        String base = "/accounts?itemId=" + java.net.URLEncoder.encode(config.itemId, java.nio.charset.StandardCharsets.UTF_8);
        List<Conta> contas = new ArrayList<>();
        Set<String> identificadores = new HashSet<>();
        int totalPaginas = 1;
        for (int numero = 1; numero <= totalPaginas; numero++) {
            var pagina = consultar(base + (numero == 1 ? "" : "&page=" + numero), token, limite);
            totalPaginas = pagina.path("totalPages").asInt(1);
            if (totalPaginas < 1 || totalPaginas > 100 || pagina.path("page").asInt(numero) != numero) {
                throw new IllegalStateException("Paginação de contas inválida.");
            }
            for (var conta : resultados(pagina)) {
                if (!"BANK".equals(texto(conta, "type")) || !"BRL".equals(texto(conta, "currencyCode"))) continue;
                String id = texto(conta, "id");
                if (!identificadores.add(id)) throw new IllegalStateException("Conta repetida na resposta bancária.");
                contas.add(new Conta(id, movimentos(id, token, desde, limite)));
            }
        }
        return contas;
    }

    private List<Movimento> movimentos(String conta, String token, LocalDate desde, long limite) {
        String rota = "/v2/transactions?accountId=" + java.net.URLEncoder.encode(conta,
            java.nio.charset.StandardCharsets.UTF_8) + "&dateFrom=" + desde;
        List<Movimento> registros = new ArrayList<>();
        Set<String> paginas = new HashSet<>();
        while (rota != null) {
            if (!paginas.add(rota) || paginas.size() > 1000) throw new IllegalStateException("Paginação inválida no extrato.");
            var pagina = consultar(rota, token, limite);
            for (var item : resultados(pagina)) {
                if (!conta.equals(texto(item, ACCOUNT_ID))) throw new IllegalStateException("Conta inesperada na movimentação.");
                registros.add(movimento(item));
            }
            rota = proximaPagina(pagina, conta, desde);
        }
        return registros;
    }

    static String proximaPagina(JsonNode pagina, String conta, LocalDate desde) {
        if (pagina == null) throw new IllegalStateException("Resposta bancária vazia.");
        var next = pagina.path("next");
        if (next.isMissingNode()) throw new IllegalStateException("Resposta bancária sem confirmação de fim da paginação.");
        if (next.isNull() || next.asString().isEmpty()) return null;
        String cursor = next.asString();
        if (!cursor.startsWith("?") || cursor.contains("://") || cursor.contains("#")) {
            throw new IllegalStateException("Cursor inválido no extrato.");
        }
        var query = org.springframework.web.util.UriComponentsBuilder.fromUriString("/v2/transactions" + cursor)
            .build().getQueryParams();
        var contas = query.get(ACCOUNT_ID);
        String depois = query.getFirst("after");
        if (contas == null || contas.size() != 1
            || !conta.equals(contas.getFirst()) || depois == null
            || depois.isBlank() || query.containsKey("dateTo")
            || (query.containsKey("dateFrom") && !desde.toString().equals(query.getFirst("dateFrom")))) {
            throw new IllegalStateException("O cursor mudou os filtros do extrato.");
        }
        return "/v2/transactions" + cursor;
    }

    private FalhaPluggy falhaHttp(org.springframework.web.client.RestClientResponseException erro) {
        int status = erro.getStatusCode().value();
        if (status == 429) {
            var headers = erro.getResponseHeaders();
            String header = headers == null ? null : headers.getFirst("Retry-After");
            long segundos = 60;
            try { segundos = Math.max(60, Long.parseLong(header)); }
            catch (NumberFormatException ignorada) { segundos = 60; }
            return new FalhaPluggy("Limite da Pluggy atingido. A próxima consulta respeitará a espera informada.", Duration.ofSeconds(Math.min(segundos, 86400)));
        }
        if (status == 401 || status == 403) {
            return new FalhaPluggy("Acesso bancário recusado. Confira as credenciais e renove o consentimento no Meu Pluggy.", Duration.ofHours(1));
        }
        return new FalhaPluggy("Falha na consulta bancária. Os dados anteriores foram preservados.", Duration.ofMinutes(1));
    }

    static Movimento movimento(JsonNode item) {
        if (!"BRL".equals(texto(item, "currencyCode"))) throw new IllegalStateException("Moeda inesperada no extrato.");
        String tipo = texto(item, "type");
        if (!"CREDIT".equals(tipo) && !"DEBIT".equals(tipo)) throw new IllegalStateException("Tipo bancário desconhecido.");
        String status = texto(item, STATUS);
        if (!"POSTED".equals(status) && !"PENDING".equals(status)) throw new IllegalStateException("Status bancário desconhecido.");
        BigDecimal valor = new BigDecimal(texto(item, "amount")).abs();
        if ("DEBIT".equals(tipo)) valor = valor.negate();
        String origem = item.path("providerId").asString("");
        origem = origem.isBlank() ? "pluggy:" + texto(item, "id") : "banco:" + origem;
        return new Movimento(origem, texto(item, "description"), Instant.parse(texto(item, "date")),
            valor, "POSTED".equals(status));
    }

    private static JsonNode resultados(JsonNode pagina) {
        if (pagina == null || !pagina.path("results").isArray()) throw new IllegalStateException("Resposta de extrato inválida.");
        return pagina.path("results");
    }

    private static String texto(JsonNode node, String campo) {
        if (node == null || node.path(campo).isMissingNode() || node.path(campo).isNull()) {
            throw new IllegalStateException("Dados incompletos na resposta bancária.");
        }
        return node.path(campo).asString();
    }
}
