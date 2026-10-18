package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.Cliente;
import br.com.example.autogestao_backend.model.OrdemServico;
import br.com.example.autogestao_backend.model.Servico;
import br.com.example.autogestao_backend.model.StatusOrdemServico;
import br.com.example.autogestao_backend.model.TipoCliente;
import br.com.example.autogestao_backend.model.Veiculo;
import br.com.example.autogestao_backend.repository.ClienteRepository;
import br.com.example.autogestao_backend.repository.OrdemServicoRepository;
import br.com.example.autogestao_backend.repository.ServicoRepository;
import br.com.example.autogestao_backend.repository.VeiculoRepository;
import br.com.example.autogestao_backend.support.IntegrationApiTest;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
class FluxoAberturaOrdemIntegrationTest {
    private final MockMvc mvc;
    private final OrdemServicoRepository ordens;
    private final ClienteRepository clientes;
    private final VeiculoRepository veiculos;
    private final ServicoRepository servicos;
    private Cliente cliente;
    private Veiculo veiculo;
    private Servico servico;

    @Autowired
    FluxoAberturaOrdemIntegrationTest(MockMvc mvc, OrdemServicoRepository ordens,
        ClienteRepository clientes, VeiculoRepository veiculos, ServicoRepository servicos) {
        this.mvc = mvc;
        this.ordens = ordens;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.servicos = servicos;
    }

    @BeforeEach
    void preparar() {
        cliente = clientes.saveAndFlush(new Cliente(TipoCliente.PF, "Cliente da ordem", null, null, Clock.systemUTC()));
        veiculo = veiculos.saveAndFlush(new Veiculo(cliente, "ABC1234", "Fiat", "Uno", 100));
        servico = servicos.saveAndFlush(new Servico("Revisão preventiva", CategoriaServico.MANUTENCAO, new BigDecimal("120"), BigDecimal.TEN));
    }

    private String abertura(String situacao, int quantidade) {
        return """
            {"clienteId":"%s","veiculoId":"%s","situacaoInicial":"%s","previsaoEntrega":"2099-12-01T12:00:00Z",
            "servicoInicial":{"servicoId":"%s","quantidade":%d,"preco":120,"custo":10}}
            """.formatted(cliente.getId(), veiculo.getId(), situacao, servico.getId(), quantidade);
    }

    @Test
    void criaAtendimentoCompletoEExigeConclusaoAntesDeFinalizar() throws Exception {
        String resposta = mvc.perform(post("/api/ordens").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(abertura("DIAGNOSTICO", 2)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DIAGNOSTICO"))
            .andExpect(jsonPath("$.total").value(240)).andExpect(jsonPath("$.historico[1].status").value("RECEBIDO"))
            .andExpect(jsonPath("$.previsaoEntrega").value("2099-12-01T12:00:00Z"))
            .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(resposta, "$.id"), item = JsonPath.read(resposta, "$.itens[0].id");
        mudar(id, "AGUARDANDO_APROVACAO");
        mvc.perform(put("/api/ordens/" + id + "/aprovacao").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"consentimento\":\"Aprovado por telefone\"}")).andExpect(status().isOk());
        mudar(id, "EM_SERVICO");
        mvc.perform(put("/api/ordens/" + id + "/status").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"FINALIZADO\"}")).andExpect(status().isConflict());
        mvc.perform(put("/api/ordens/itens/" + item + "/conclusao").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"custoReal\":10,\"relato\":\"Revisão realizada e testada\"}")).andExpect(status().isOk());
        mudar(id, "FINALIZADO");
        assertEquals(1, ordens.count());
    }

    @Test
    void aberturaInvalidaNaoDeixaOrdemParcial() throws Exception {
        mvc.perform(post("/api/ordens").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(abertura("FINALIZADO", 1))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/ordens").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(abertura("RECEBIDO", 0))).andExpect(status().isBadRequest());
        assertEquals(0, ordens.count());
    }

    @Test
    void pesquisaEPaginaTodasAsOrdensComFiltroDeSituacao() throws Exception {
        for (int i = 0; i < 105; i++) {
            var ordem = new OrdemServico(5000 + i, cliente, veiculo, Clock.systemUTC());
            if (i == 104) {
                ordem.transicionar(StatusOrdemServico.RECEBIDO, Clock.systemUTC());
                ordem.adicionarServico(servico, 1, new BigDecimal("120"), BigDecimal.TEN);
            }
            ordens.saveAndFlush(ordem);
        }
        mvc.perform(get("/api/ordens").param("pagina", "5").param("tamanho", "20"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(105))
            .andExpect(jsonPath("$.itens.length()").value(5));
        mvc.perform(get("/api/ordens").param("busca", "5104").param("status", "RECEBIDO"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/ordens").param("busca", "ABC-1234"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(105));
        mvc.perform(get("/api/ordens").param("busca", "CLIENTE DA ORDEM").param("status", "RASCUNHO"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(104));
        mvc.perform(get("/api/ordens").param("busca", "Revisão preventiva"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }

    private void mudar(String id, String situacao) throws Exception {
        mvc.perform(put("/api/ordens/" + id + "/status").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"" + situacao + "\"}")).andExpect(status().isOk());
    }
}
