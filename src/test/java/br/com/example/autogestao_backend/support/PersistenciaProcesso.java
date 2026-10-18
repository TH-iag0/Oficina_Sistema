package br.com.example.autogestao_backend.support;

import br.com.example.autogestao_backend.AutogestaoBackendApplication;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.SpringApplication;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Executado em JVMs distintas pelo teste de reinício, sempre contra banco isolado. */
public final class PersistenciaProcesso {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(10)).build();
    private final String base;

    private PersistenciaProcesso(int porta) { base = "http://127.0.0.1:" + porta; }

    public static void main(String[] args) throws Exception {
        var app = new SpringApplication(AutogestaoBackendApplication.class);
        try (var contexto = app.run(java.util.Arrays.copyOfRange(args, 2, args.length))) {
            var teste = new PersistenciaProcesso(Integer.parseInt(contexto.getEnvironment().getProperty("local.server.port")));
            try { teste.executar(args[0], Path.of(args[1])); }
            finally { teste.http.close(); }
        }
    }

    private void executar(String modo, Path arquivo) throws Exception {
        if ("gravar".equals(modo)) {
            var rotas = cadastrarDados();
            Files.writeString(arquivo, mapper.writeValueAsString(Map.of("rotas", rotas, "valores", consultar(rotas))));
            System.out.println("DADOS_GRAVADOS_PID=" + ProcessHandle.current().pid());
            return;
        }
        var esperado = mapper.readTree(Files.readString(arquivo));
        var rotas = new ArrayList<String>();
        for (var rota : esperado.get("rotas")) rotas.add(rota.asString());
        var atual = mapper.readTree(mapper.writeValueAsString(consultar(rotas)));
        if (!esperado.get("valores").equals(atual)) throw new AssertionError("Dados mudaram após reinício: " + atual);
        System.out.println("PERSISTENCIA_VALIDADA_PID=" + ProcessHandle.current().pid());
    }

    private List<JsonNode> consultar(List<String> rotas) throws Exception {
        var valores = new ArrayList<JsonNode>();
        for (var rota : rotas) valores.add(chamar("GET", rota, null));
        return valores;
    }

    private List<String> cadastrarDados() throws Exception {
        String cliente = id(chamar("POST", "/api/clientes", "{\"tipo\":\"PF\",\"nome\":\"Cliente persistente\",\"telefone\":\"11999999999\"}"));
        String veiculo = id(chamar("POST", "/api/veiculos", "{\"clienteId\":\"" + cliente
            + "\",\"placa\":\"ABC1234\",\"marca\":\"VW\",\"modelo\":\"Golf\",\"quilometragem\":12345}"));
        String categoria = id(chamar("POST", "/api/servicos/categorias", "{\"nome\":\"Limpeza persistente\"}"));
        String servico = id(chamar("POST", "/api/servicos", "{\"nome\":\"Serviço persistente\",\"categoria\":\"OUTRO\",\"preco\":200,\"custo\":80,\"categoriaId\":\"" + categoria + "\"}"));
        String funcionario = cadastrarFuncionario();
        String ordem = cadastrarOrdem(cliente, veiculo, servico);
        cadastrarDespesa();
        return List.of("/api/clientes/" + cliente, "/api/veiculos/" + veiculo, "/api/servicos/" + servico,
            "/api/servicos/categorias", "/api/funcionarios?tamanho=100", "/api/funcionarios/" + funcionario + "/jornadas",
            "/api/ordens/" + ordem, "/api/financeiro/pagamentos?ordemId=" + ordem,
            "/api/financeiro/despesas?tamanho=100", "/api/dashboard", "/api/financeiro/analises?mes=" + LocalDate.now().toString().substring(0,7));
    }

    private String cadastrarFuncionario() throws Exception {
        String funcionario = id(chamar("POST", "/api/funcionarios", "{\"nome\":\"Funcionário persistente\",\"matricula\":\"PERSIST-001\",\"pin\":\"123456\"}"));
        chamar("POST", "/api/funcionarios/" + funcionario + "/jornadas/semana", """
            {"encerrarAnteriores":false,"jornadas":[{"dia":"MONDAY","entrada":"08:00","saida":"17:00",
             "intervaloMinutos":60,"toleranciaMinutos":5,"vigenciaInicio":"2026-10-01","intervaloAutomatico":true}]}
            """);
        return funcionario;
    }

    private String cadastrarOrdem(String cliente, String veiculo, String servico) throws Exception {
        var ordem = chamar("POST", "/api/ordens", "{\"clienteId\":\"" + cliente + "\",\"veiculoId\":\"" + veiculo
            + "\",\"situacaoInicial\":\"DIAGNOSTICO\",\"servicoInicial\":{\"servicoId\":\"" + servico + "\",\"quantidade\":1,\"preco\":200,\"custo\":80}}");
        String id = id(ordem), rota = "/api/ordens/" + id;
        chamar("PUT", rota + "/status", "{\"status\":\"AGUARDANDO_APROVACAO\"}");
        chamar("PUT", rota + "/aprovacao", "{\"consentimento\":\"Aprovado para teste de persistência\"}");
        chamar("PUT", rota + "/status", "{\"status\":\"EM_SERVICO\"}");
        chamar("PUT", "/api/ordens/itens/" + ordem.get("itens").get(0).get("id").asString() + "/conclusao",
            "{\"custoReal\":80,\"relato\":\"Trabalho registrado e preservado após reinício\"}");
        chamar("PUT", rota + "/status", "{\"status\":\"FINALIZADO\"}");
        chamar("POST", "/api/financeiro/pagamentos", "{\"ordemServicoId\":\"" + id
            + "\",\"valor\":200,\"forma\":\"PIX\",\"requisicaoId\":\"" + UUID.randomUUID() + "\"}");
        return id;
    }

    private String cadastrarDespesa() throws Exception {
        String data = LocalDate.now().toString();
        String despesa = id(chamar("POST", "/api/financeiro/despesas", "{\"descricao\":\"Despesa persistente\",\"categoria\":\"Operação\",\"valor\":25,\"competencia\":\""
            + data + "\",\"vencimento\":\"" + data + "\"}"));
        chamar("PUT", "/api/financeiro/despesas/" + despesa + "/pagamento", null);
        return despesa;
    }

    private JsonNode chamar(String metodo, String rota, String corpo) throws Exception {
        var pedido = HttpRequest.newBuilder(URI.create(base + rota)).timeout(Duration.ofSeconds(20));
        if (!"GET".equals(metodo)) {
            var csrf = chamar("GET", "/api/csrf", null);
            pedido.header(csrf.get("headerName").asString(), csrf.get("token").asString());
        }
        if (corpo != null) pedido.header("Content-Type", "application/json");
        pedido.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
        var resposta = http.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
        if (resposta.statusCode() < 200 || resposta.statusCode() >= 300) {
            throw new AssertionError(metodo + " " + rota + ": " + resposta.statusCode() + " " + resposta.body());
        }
        return resposta.body().isBlank() ? null : mapper.readTree(resposta.body());
    }

    private String id(JsonNode registro) { return registro.get("id").asString(); }
}
