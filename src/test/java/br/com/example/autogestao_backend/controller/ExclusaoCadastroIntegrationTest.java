package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.Cliente;
import br.com.example.autogestao_backend.model.OrdemServico;
import br.com.example.autogestao_backend.model.Servico;
import br.com.example.autogestao_backend.model.TipoCliente;
import br.com.example.autogestao_backend.model.Veiculo;
import br.com.example.autogestao_backend.repository.ClienteRepository;
import br.com.example.autogestao_backend.repository.OrdemServicoRepository;
import br.com.example.autogestao_backend.repository.ServicoRepository;
import br.com.example.autogestao_backend.repository.VeiculoRepository;
import br.com.example.autogestao_backend.support.IntegrationApiTest;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationApiTest
class ExclusaoCadastroIntegrationTest {
    private final MockMvc mvc;
    private final ClienteRepository clientes;
    private final VeiculoRepository veiculos;
    private final ServicoRepository servicos;
    private final OrdemServicoRepository ordens;
    private final Clock relogio = Clock.systemUTC();

    @Autowired
    ExclusaoCadastroIntegrationTest(MockMvc mvc, ClienteRepository clientes, VeiculoRepository veiculos,
        ServicoRepository servicos, OrdemServicoRepository ordens) {
        this.mvc = mvc;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.servicos = servicos;
        this.ordens = ordens;
    }

    @Test void excluiCadastrosSemVinculosEExigeCsrf() throws Exception {
        var cliente = cliente();
        var veiculo = veiculo(cliente);
        var servico = servico();
        mvc.perform(delete("/api/servicos/" + servico.getId() + "/exclusao")).andExpect(status().isForbidden());
        excluir("veiculos", veiculo.getId());
        excluir("clientes", cliente.getId());
        excluir("servicos", servico.getId());
        assertFalse(veiculos.existsById(veiculo.getId()));
        assertFalse(clientes.existsById(cliente.getId()));
        assertFalse(servicos.existsById(servico.getId()));
        mvc.perform(delete("/api/clientes/" + UUID.randomUUID() + "/exclusao").with(csrf())).andExpect(status().isNotFound());
    }

    @Test void bloqueiaClienteComVeiculoVinculado() throws Exception {
        var cliente = cliente();
        veiculo(cliente);
        mvc.perform(delete("/api/clientes/" + cliente.getId() + "/exclusao").with(csrf())).andExpect(status().isConflict());
    }

    @Test void bloqueiaServicoUsadoEmOrdem() throws Exception {
        var cliente = cliente();
        var servico = servico();
        var ordem = new OrdemServico(99999, cliente, veiculo(cliente), relogio);
        ordem.adicionarServico(servico, 1, BigDecimal.TEN, BigDecimal.ZERO);
        ordens.saveAndFlush(ordem);
        mvc.perform(delete("/api/servicos/" + servico.getId() + "/exclusao").with(csrf())).andExpect(status().isConflict());
    }

    private void excluir(String tipo, UUID id) throws Exception {
        mvc.perform(delete("/api/" + tipo + "/" + id + "/exclusao").with(csrf())).andExpect(status().isNoContent());
    }

    private Cliente cliente() {
        return clientes.saveAndFlush(new Cliente(TipoCliente.PF, "Cliente teste", null, null, relogio));
    }

    private Veiculo veiculo(Cliente cliente) {
        return veiculos.saveAndFlush(new Veiculo(cliente, "ABC1234", "VW", "Golf", 0));
    }

    private Servico servico() {
        return servicos.saveAndFlush(new Servico("Limpeza", CategoriaServico.OUTRO, BigDecimal.TEN, BigDecimal.ZERO));
    }
}
