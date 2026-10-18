package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.model.Funcionario;
import br.com.example.autogestao_backend.repository.FuncionarioRepository;
import br.com.example.autogestao_backend.support.IntegrationApiTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationApiTest
class ApiIntegrationTest {
    private final MockMvc mvc;
    private final FuncionarioRepository funcionarios;

    @Autowired
    ApiIntegrationTest(MockMvc mvc, FuncionarioRepository funcionarios) {
        this.mvc = mvc;
        this.funcionarios = funcionarios;
    }

    @Test
    void protegeDashboardClientesEHistorico() throws Exception {
        mvc.perform(get("/api/dashboard").header("CF-Connecting-IP", "203.0.113.1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/clientes").header("Forwarded", "for=203.0.113.1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/ponto/marcacoes").header("X-Forwarded-For", "203.0.113.1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard").with(user("funcionario").roles("USER"))).andExpect(status().isForbidden());
    }

    @Test
    void permitePreflightSomenteDaOrigemConfigurada() throws Exception {
        mvc.perform(options("/api/clientes").header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/clientes").header("Origin", "https://origem-invalida.example")
            .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }

    @Test
    void cadastraEConsultaClienteSemExporEntidade() throws Exception {
        mvc.perform(post("/api/clientes").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"PF\",\"nome\":\"João\"}"))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.nome").value("João")).andExpect(jsonPath("$.versao").doesNotExist());
        mvc.perform(get("/api/clientes").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void exigeCsrfEValidaEntradasComProblemDetails() throws Exception {
        mvc.perform(post("/api/clientes").with(user("admin").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"PF\",\"nome\":\"João\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/clientes").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"PF\",\"nome\":\"\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    void rejeitaPaginaInvalidaEClienteAusente() throws Exception {
        mvc.perform(get("/api/clientes?tamanho=101").with(user("admin").roles("ADMIN")))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/clientes/" + UUID.randomUUID()).with(user("admin").roles("ADMIN")))
            .andExpect(status().isNotFound());
    }

    @Test
    void dashboardSemDadosNaoDividePorZero() throws Exception {
        mvc.perform(get("/api/dashboard?mes=2026-10").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.faturamento").value(0))
            .andExpect(jsonPath("$.ordensConcluidas").value(0));
    }

    @Test
    void pontoPublicoAutenticaPinESuportaRepeticaoSemDuplicar() throws Exception {
        funcionarios.saveAndFlush(new Funcionario("Carlos", "TEST-001", new BCryptPasswordEncoder(4).encode("123456")));
        String corpo = "{\"matricula\":\"TEST-001\",\"pin\":\"123456\",\"tipo\":\"ENTRADA\",\"requisicaoId\":\""
            + UUID.randomUUID() + "\"}";
        var primeira = mvc.perform(post("/api/ponto/marcacoes").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isOk()).andExpect(jsonPath("$.pin").doesNotExist())
            .andExpect(jsonPath("$.pinHash").doesNotExist()).andReturn();
        mvc.perform(post("/api/ponto/marcacoes").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .json(primeira.getResponse().getContentAsString()));
        mvc.perform(post("/api/ponto/marcacoes").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(corpo.replace("123456", "999999"))).andExpect(status().isUnauthorized());
    }

    @Test
    void forneceTokenCsrfParaFrontAutenticado() throws Exception {
        mvc.perform(get("/api/csrf").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void pontoRecusaTokenCsrfAusenteOuInvalido() throws Exception {
        String corpo = "{\"matricula\":\"TEST-CSRF\",\"pin\":\"123456\",\"tipo\":\"ENTRADA\",\"requisicaoId\":\""
            + UUID.randomUUID() + "\"}";
        mvc.perform(post("/api/ponto/marcacoes").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/ponto/marcacoes").with(csrf().useInvalidToken())
            .contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isForbidden());
    }

    @Test
    void pontoRemotoUsaTokenRealSemPermissaoAdministrativa() throws Exception {
        funcionarios.saveAndFlush(new Funcionario("Ana", "TEST-CSRF", new BCryptPasswordEncoder(4).encode("123456")));
        var token = mvc.perform(get("/api/csrf").with(request -> {
            request.setRemoteAddr("192.0.2.10");
            return request;
        })).andExpect(status().isOk()).andReturn();
        var json = new tools.jackson.databind.ObjectMapper().readTree(token.getResponse().getContentAsString());
        String corpo = "{\"matricula\":\"TEST-CSRF\",\"pin\":\"123456\",\"tipo\":\"ENTRADA\",\"requisicaoId\":\""
            + UUID.randomUUID() + "\"}";
        var sessao = (org.springframework.mock.web.MockHttpSession) token.getRequest().getSession(false);
        mvc.perform(post("/api/ponto/marcacoes").with(request -> {
            request.setRemoteAddr("192.0.2.10");
            return request;
        }).session(sessao).header(json.get("headerName").asString(), json.get("token").asString())
            .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isOk());
        mvc.perform(get("/api/funcionarios").with(request -> {
            request.setRemoteAddr("192.0.2.10");
            return request;
        }).session(sessao)).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenRealObtidoPeloFrontAutorizaCadastroNaMesmaSessao() throws Exception {
        var token = mvc.perform(get("/api/csrf").with(user("admin").roles("ADMIN"))).andReturn();
        var json = new tools.jackson.databind.ObjectMapper().readTree(token.getResponse().getContentAsString());
        mvc.perform(post("/api/clientes").with(user("admin").roles("ADMIN"))
            .session((org.springframework.mock.web.MockHttpSession) token.getRequest().getSession(false))
            .header(json.get("headerName").asString(), json.get("token").asString())
            .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"PF\",\"nome\":\"Maria\"}"))
            .andExpect(status().isCreated());
    }
}
