package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.repository.FuncionarioRepository;
import br.com.example.autogestao_backend.repository.JornadaTrabalhoRepository;
import br.com.example.autogestao_backend.support.IntegrationApiTest;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationApiTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SemanaJornadaIntegrationTest {
    private final MockMvc mvc;
    private final FuncionarioRepository funcionarios;
    private final JornadaTrabalhoRepository jornadas;
    private UUID id;

    @Autowired
    SemanaJornadaIntegrationTest(MockMvc mvc, FuncionarioRepository funcionarios, JornadaTrabalhoRepository jornadas) {
        this.mvc = mvc;
        this.funcionarios = funcionarios;
        this.jornadas = jornadas;
    }

    @AfterEach void limparSomenteFuncionarioDoTeste() {
        if (id == null) return;
        jornadas.deleteAllById(jornadas.findByFuncionarioId(id).stream()
            .map(br.com.example.autogestao_backend.model.JornadaTrabalho::getId).toList());
        funcionarios.deleteById(id);
    }

    @Test void salvaSemanaESubstituiAbertasPreservandoHistorico() throws Exception {
        criarFuncionario();
        salvar(entrada("MONDAY", "2026-10-01"), false, 200);
        salvar(entrada("MONDAY", "2026-10-15") + "," + entrada("SATURDAY", "2026-10-15"), true, 200);
        mvc.perform(get("/api/funcionarios/" + id + "/jornadas"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[?(@.vigenciaFim == '2026-10-14')]").value(org.hamcrest.Matchers.hasSize(1)));
    }

    @Test void conflitoNoSegundoDiaDesfazTambemOPrimeiroDia() throws Exception {
        criarFuncionario();
        salvar(entrada("MONDAY", "2026-10-01"), false, 200);
        salvar(entrada("TUESDAY", "2026-10-15") + "," + entrada("MONDAY", "2026-10-15"), false, 409);
        mvc.perform(get("/api/funcionarios/" + id + "/jornadas"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test void rejeitaSemanaVaziaDiasRepetidosEHorarioInvalido() throws Exception {
        criarFuncionario();
        salvar("", false, 400);
        salvar(entrada("MONDAY", "2026-10-01") + "," + entrada("MONDAY", "2026-10-01"), false, 400);
        salvar(entrada("MONDAY", "2026-10-01").replace("08:00", "17:00"), false, 400);
    }

    private void criarFuncionario() throws Exception {
        var matricula = UUID.randomUUID().toString().substring(0, 20);
        String resposta = mvc.perform(post("/api/funcionarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Teste semana\",\"matricula\":\"" + matricula + "\",\"pin\":\"123456\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        id = UUID.fromString(JsonPath.read(resposta, "$.id"));
    }

    private void salvar(String entradas, boolean encerrar, int statusEsperado) throws Exception {
        mvc.perform(post("/api/funcionarios/" + id + "/jornadas/semana").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"jornadas\":[" + entradas + "],\"encerrarAnteriores\":" + encerrar + "}"))
            .andExpect(status().is(statusEsperado));
    }

    private String entrada(String dia, String inicio) {
        return """
            {"dia":"%s","entrada":"08:00","saida":"17:00","intervaloMinutos":60,
             "toleranciaMinutos":5,"vigenciaInicio":"%s","intervaloAutomatico":true}
            """.formatted(dia, inicio);
    }
}
