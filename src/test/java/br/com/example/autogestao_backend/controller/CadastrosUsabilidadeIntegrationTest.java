package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.Cliente;
import br.com.example.autogestao_backend.model.Servico;
import br.com.example.autogestao_backend.model.TipoCliente;
import br.com.example.autogestao_backend.model.Veiculo;
import br.com.example.autogestao_backend.repository.ClienteRepository;
import br.com.example.autogestao_backend.repository.ServicoRepository;
import br.com.example.autogestao_backend.repository.VeiculoRepository;
import br.com.example.autogestao_backend.support.IntegrationApiTest;
import java.math.BigDecimal;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationApiTest
class CadastrosUsabilidadeIntegrationTest {
    private final MockMvc mvc;
    private final ClienteRepository clientes;
    private final VeiculoRepository veiculos;
    private final ServicoRepository servicos;

    @Autowired
    CadastrosUsabilidadeIntegrationTest(MockMvc mvc, ClienteRepository clientes,
        VeiculoRepository veiculos, ServicoRepository servicos) {
        this.mvc = mvc;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.servicos = servicos;
    }

    private Cliente cliente(String nome) {
        return clientes.saveAndFlush(new Cliente(TipoCliente.PF, nome, null, null, Clock.systemUTC()));
    }

    @Test
    void clientesAlemDoCentesimoSaoPesquisaveisEPaginaveis() throws Exception {
        for (int i = 0; i < 105; i++) cliente(String.format("Cliente %03d", i));
        mvc.perform(get("/api/clientes").param("pagina", "5").param("tamanho", "20"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(105))
            .andExpect(jsonPath("$.itens.length()").value(5)).andExpect(jsonPath("$.totalPaginas").value(6));
        mvc.perform(get("/api/clientes").param("nome", "cliente 104"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.itens[0].nome").value("Cliente 104"));
    }

    @Test
    void pesquisaVeiculoPorPlacaMarcaModeloEClienteSemLimiteDe100() throws Exception {
        var primeiro = cliente("Primeiro");
        var segundo = cliente("Segundo");
        for (int i = 0; i < 105; i++) veiculos.save(new Veiculo(i < 101 ? primeiro : segundo,
            String.format("ABC%04d", i), "Volkswagen", "Golf", i));
        veiculos.flush();
        mvc.perform(get("/api/veiculos").param("busca", "ABC-0104"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].clienteId").value(segundo.getId().toString()));
        mvc.perform(get("/api/veiculos").param("clienteId", primeiro.getId().toString())
            .param("ativo", "true").param("busca", "golf").param("pagina", "5").param("tamanho", "20"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(101))
            .andExpect(jsonPath("$.itens.length()").value(1));
        mvc.perform(get("/api/veiculos").param("busca", "VOLKSWAGEN").param("tamanho", "100"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(105));
    }

    @Test
    void servicosSaoPesquisaveisAlemDaPrimeiraPaginaEValidamParametros() throws Exception {
        for (int i = 0; i < 105; i++) servicos.save(new Servico(String.format("Revisao %03d", i),
            CategoriaServico.MANUTENCAO, new BigDecimal("100"), new BigDecimal("20")));
        servicos.flush();
        mvc.perform(get("/api/servicos").param("busca", "revisao 104").param("ativo", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/servicos").param("pagina", "5").param("tamanho", "20"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(5));
        mvc.perform(get("/api/servicos").param("tamanho", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/veiculos").param("busca", "a".repeat(161))).andExpect(status().isBadRequest());
    }

    @Test
    void edicaoAtualizaCadastroExistenteSemTrocarPlacaOuCliente() throws Exception {
        var cliente = cliente("Cliente original");
        var veiculo = veiculos.saveAndFlush(new Veiculo(cliente, "ABC1234", "VW", "Golf", 100));
        mvc.perform(put("/api/clientes/" + cliente.getId()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"tipo\":\"PF\",\"nome\":\"Cliente corrigido\",\"telefone\":\"11999999999\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.telefone").value("11999999999"));
        mvc.perform(put("/api/veiculos/" + veiculo.getId()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"marca\":\"Volkswagen\",\"modelo\":\"Golf GTI\",\"quilometragem\":120}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.placa").value("ABC1234"))
            .andExpect(jsonPath("$.clienteId").value(cliente.getId().toString()));
        assertEquals(1, clientes.count());
        assertEquals(1, veiculos.count());
        mvc.perform(put("/api/veiculos/" + veiculo.getId()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"marca\":\"VW\",\"modelo\":\"Golf\",\"quilometragem\":110}"))
            .andExpect(status().isBadRequest());
    }
}
