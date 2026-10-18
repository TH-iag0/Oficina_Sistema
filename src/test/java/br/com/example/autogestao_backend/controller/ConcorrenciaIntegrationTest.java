package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.ClienteEntrada;
import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.dto.PontoEntrada;
import br.com.example.autogestao_backend.model.CategoriaServico;
import br.com.example.autogestao_backend.model.FormaPagamento;
import br.com.example.autogestao_backend.model.StatusOrdemServico;
import br.com.example.autogestao_backend.model.TipoCliente;
import br.com.example.autogestao_backend.model.TipoMarcacao;
import br.com.example.autogestao_backend.repository.MarcacaoPontoRepository;
import br.com.example.autogestao_backend.repository.PagamentoRepository;
import br.com.example.autogestao_backend.service.CatalogoService;
import br.com.example.autogestao_backend.service.ClienteService;
import br.com.example.autogestao_backend.service.FinanceiroService;
import br.com.example.autogestao_backend.service.FuncionarioService;
import br.com.example.autogestao_backend.service.OrdemServicoService;
import br.com.example.autogestao_backend.service.PontoService;
import br.com.example.autogestao_backend.service.VeiculoService;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest(properties = {
    "spring.datasource.url=${AUTOGESTAO_TEST_DB_URL}_concurrency?currentSchema=autogestao",
    "spring.datasource.username=postgres",
    "spring.datasource.password=${AUTOGESTAO_TEST_DB_PASSWORD:}",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "AUTOGESTAO_ADMIN_PASSWORD="
})
@EnabledIfEnvironmentVariable(named = "AUTOGESTAO_TEST_DB_URL",
    matches = "jdbc:postgresql://127\\.0\\.0\\.1:\\d+/autogestao_migration_test")
class ConcorrenciaIntegrationTest {
    private static final AtomicInteger PLACAS = new AtomicInteger();
    private final ClienteService clientes;
    private final VeiculoService veiculos;
    private final CatalogoService catalogo;
    private final OrdemServicoService ordens;
    private final FuncionarioService funcionarios;
    private final FinanceiroService financeiro;
    private final PontoService ponto;
    private final MarcacaoPontoRepository marcacoes;
    private final PagamentoRepository pagamentos;

    @Autowired
    ConcorrenciaIntegrationTest(ClienteService clientes, VeiculoService veiculos, CatalogoService catalogo,
        OrdemServicoService ordens, FuncionarioService funcionarios, FinanceiroService financeiro,
        PontoService ponto, MarcacaoPontoRepository marcacoes, PagamentoRepository pagamentos) {
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.catalogo = catalogo;
        this.ordens = ordens;
        this.funcionarios = funcionarios;
        this.financeiro = financeiro;
        this.ponto = ponto;
        this.marcacoes = marcacoes;
        this.pagamentos = pagamentos;
    }

    @Test
    void duasConfirmacoesSimultaneasGeramUmaMarcacao() throws Exception {
        String matricula = UUID.randomUUID().toString().substring(0, 16);
        var funcionario = funcionarios.cadastrar(new OficinaDtos.FuncionarioEntrada("Teste", matricula, "123456"));
        var dados = new PontoEntrada(matricula, "123456", TipoMarcacao.ENTRADA, UUID.randomUUID());
        var resultados = simultaneamente(() -> ponto.registrar(dados).id());
        assertEquals(resultados.getFirst(), resultados.getLast());
        assertEquals(1, marcacoes.findByFuncionario_IdAndRequisicaoId(funcionario.id(), dados.requisicaoId()).stream().count());
    }

    @Test
    void numeracaoPermaneceUnicaComAberturasSimultaneas() throws Exception {
        var dados = cadastro();
        var numeros = simultaneamente(() -> ordens.abrir(dados).numero());
        assertNotEquals(numeros.getFirst(), numeros.getLast());
        assertEquals(1, Math.abs(numeros.getFirst() - numeros.getLast()));
    }

    @Test
    void pagamentosSimultaneosNaoUltrapassamSaldo() throws Exception {
        UUID os = ordemRecebida();
        var resultados = simultaneamente(() -> tentarPagamento(os));
        assertEquals(1, resultados.stream().filter(Boolean::booleanValue).count());
        assertEquals(0, pagamentos.somarPorOrdemServico(os).compareTo(new BigDecimal("80")));
    }

    private boolean tentarPagamento(UUID os) {
        try {
            financeiro.receber(new OficinaDtos.PagamentoEntrada(os, new BigDecimal("80"), FormaPagamento.PIX, UUID.randomUUID()));
            return true;
        } catch (ErroApi erro) {
            assertEquals(409, erro.getStatus().value());
            return false;
        }
    }

    private OficinaDtos.OsEntrada cadastro() {
        var cliente = clientes.cadastrar(new ClienteEntrada(TipoCliente.PF, "Concorrência", null, null));
        String placa = "CNC" + String.format("%04d", PLACAS.incrementAndGet());
        var veiculo = veiculos.cadastrar(new OficinaDtos.VeiculoEntrada(cliente.id(), placa, "VW", "Golf", 100));
        return new OficinaDtos.OsEntrada(cliente.id(), veiculo.id(), null);
    }

    private UUID ordemRecebida() {
        UUID os = ordens.abrir(cadastro()).id();
        var servico = catalogo.cadastrar(new OficinaDtos.ServicoEntrada("Teste", CategoriaServico.MANUTENCAO,
            new BigDecimal("100"), new BigDecimal("30")));
        ordens.adicionar(os, new OficinaDtos.ItemEntrada(servico.id(), 1, new BigDecimal("100"), new BigDecimal("30")));
        ordens.status(os, new OficinaDtos.StatusEntrada(StatusOrdemServico.RECEBIDO));
        return os;
    }

    private <T> List<T> simultaneamente(Callable<T> operacao) throws Exception {
        var preparados = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<T> tarefa = () -> {
                preparados.countDown();
                if (!iniciar.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timeout da sincronização");
                }
                return operacao.call();
            };
            var primeira = executor.submit(tarefa);
            var segunda = executor.submit(tarefa);
            if (!preparados.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Threads não iniciaram");
            }
            iniciar.countDown();
            return List.of(primeira.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS));
        }
    }
}
