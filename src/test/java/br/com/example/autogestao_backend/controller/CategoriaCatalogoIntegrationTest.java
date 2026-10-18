package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.repository.ServicoRepository;
import br.com.example.autogestao_backend.support.IntegrationApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationApiTest
class CategoriaCatalogoIntegrationTest {
    private final MockMvc mvc;
    private final ServicoRepository servicos;
    @Autowired
    CategoriaCatalogoIntegrationTest(MockMvc mvc, ServicoRepository servicos) {
        this.mvc = mvc;
        this.servicos = servicos;
    }

    @Test
    void mantemCincoPadroesERejeitaNomesDuplicadosEInvalidos() throws Exception {
        mvc.perform(get("/api/servicos/categorias")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(post("/api/servicos/categorias").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\" MANUTENÇÃO \"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/servicos/categorias").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\" \"}")).andExpect(status().isBadRequest());
    }

    @Test
    void categoriaNovaFicaDisponivelEVinculadaAoServicoAposEdicao() throws Exception {
        String resposta = mvc.perform(post("/api/servicos/categorias").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Higienização automotiva\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.padrao").value(false))
            .andReturn().getResponse().getContentAsString();
        String categoria = JsonPath.read(resposta, "$.id");
        String entrada = """
            {"nome":"Limpeza interna","categoria":"OUTRO","preco":120,"custo":20,"categoriaId":"%s"}
            """.formatted(categoria);
        String servico = mvc.perform(post("/api/servicos").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(entrada))
            .andExpect(status().isOk()).andExpect(jsonPath("$.categoriaNome").value("Higienização automotiva"))
            .andExpect(jsonPath("$.categoriaId").value(categoria)).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(servico, "$.id");
        mvc.perform(put("/api/servicos/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(entrada.replace("120", "150")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.categoriaId").value(categoria)).andExpect(jsonPath("$.preco").value(150));
        mvc.perform(get("/api/servicos/categorias")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6));
        mvc.perform(get("/api/servicos/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.categoriaNome").value("Higienização automotiva"));
        assertEquals(1, servicos.count());
    }

    @Test
    void categoriaInexistenteNaoCriaServicoParcial() throws Exception {
        mvc.perform(post("/api/servicos").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
            {"nome":"Teste","categoria":"OUTRO","preco":10,"custo":0,"categoriaId":"ffffffff-ffff-ffff-ffff-ffffffffffff"}
            """))
            .andExpect(status().isNotFound());
        assertEquals(0, servicos.count());
    }
}
