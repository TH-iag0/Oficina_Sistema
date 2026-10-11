package br.com.example.autogestao_backend.repository;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import br.com.example.autogestao_backend.model.*;
import jakarta.persistence.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

/** Consultas reais e transações isoladas; não usa mocks de JpaRepository. */
class RepositoriesTest {
    private static final Instant INICIO = Instant.parse("2026-10-01T03:00:00Z");
    private static final Instant FIM = Instant.parse("2026-11-01T03:00:00Z");
    private static final PageRequest PAGINA = PageRequest.of(0, 10);
    private static final String HASH = new BCryptPasswordEncoder(4).encode("123456");
    private static SessionFactory sessionFactory;
    private static Flyway flyway;
    private EntityManager em;
    private JpaRepositoryFactory factory;

    @BeforeAll static void iniciarBanco() {
        String url = System.getenv("AUTOGESTAO_TEST_DB_URL");
        boolean postgres = url != null;
        if (postgres && !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/autogestao_migration_test"))
            throw new IllegalArgumentException("Integração PostgreSQL exige banco local dedicado autogestao_migration_test.");
        if (!postgres) url = "jdbc:h2:mem:repositories;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        String usuario = postgres ? "postgres" : "sa";
        String senha = postgres ? Objects.requireNonNullElse(System.getenv("AUTOGESTAO_TEST_DB_PASSWORD"), "") : "";
        flyway = Flyway.configure().dataSource(url, usuario, senha)
            .schemas("autogestao").defaultSchema("autogestao")
            .locations("classpath:db/migration").baselineOnMigrate(false).cleanDisabled(true).load();
        flyway.migrate();
        var config = new Configuration();
        config.setProperty("hibernate.connection.driver_class", postgres ? "org.postgresql.Driver" : "org.h2.Driver");
        config.setProperty("hibernate.connection.url", url);
        config.setProperty("hibernate.connection.username", usuario);
        config.setProperty("hibernate.connection.password", senha);
        config.setProperty("hibernate.default_schema", "autogestao");
        config.setProperty("hibernate.hbm2ddl.auto", "validate");
        config.setProperty("hibernate.jdbc.time_zone", "UTC");
        config.setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl");
        config.setProperty("jakarta.persistence.validation.mode", "none");
        for (Class<?> model : List.of(Cliente.class, Veiculo.class, Funcionario.class, Servico.class,
            OrdemServico.class, ItemOrdemServico.class, HistoricoStatusOs.class, Pagamento.class,
            EstornoPagamento.class, Despesa.class, MarcacaoPonto.class, JornadaTrabalho.class,
            ExecucaoServico.class, Remap.class, Diagnostico.class)) config.addAnnotatedClass(model);
        sessionFactory = config.buildSessionFactory();
    }
    @AfterAll static void fecharBanco() { if (sessionFactory != null) sessionFactory.close(); }
    @BeforeEach void abrirTransacao() {
        em = sessionFactory.createEntityManager();
        em.getTransaction().begin();
        factory = new JpaRepositoryFactory(em);
    }
    @AfterEach void desfazerTransacao() {
        if (em != null) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close();
        }
    }
    private <R> R repo(Class<R> tipo) { return factory.getRepository(tipo); }
    private Clock relogio(Instant instante) { return Clock.fixed(instante, ZoneOffset.UTC); }
    private BigDecimal valor(String valor) { return new BigDecimal(valor); }
    private Cliente cliente(String nome) {
        return repo(ClienteRepository.class).save(new Cliente(TipoCliente.PF, nome, null, "11999999999", relogio(INICIO)));
    }
    private Funcionario funcionario() { return repo(FuncionarioRepository.class).save(new Funcionario("Carlos", "OF-001", HASH)); }
    private OrdemServico os(long numero, Cliente cliente, Instant abertaEm) {
        var veiculo = repo(VeiculoRepository.class).save(new Veiculo(cliente, "ABC" + String.format("%04d", numero), "VW", "Golf", 50000));
        return repo(OrdemServicoRepository.class).save(new OrdemServico(numero, cliente, veiculo, relogio(abertaEm)));
    }
    private ItemOrdemServico adicionar(OrdemServico os, CategoriaServico categoria, int quantidade, String preco, String custo) {
        var servico = repo(ServicoRepository.class).save(new Servico("Serviço " + categoria, categoria, valor(preco), valor(custo)));
        var item = os.adicionarServico(servico, quantidade, valor(preco), valor(custo));
        repo(OrdemServicoRepository.class).saveAndFlush(os);
        return em.find(ItemOrdemServico.class, item.getId());
    }
    private void iniciar(OrdemServico os, Instant instante) {
        var clock = relogio(instante);
        os.transicionar(StatusOrdemServico.RECEBIDO, clock);
        os.transicionar(StatusOrdemServico.DIAGNOSTICO, clock);
        os.transicionar(StatusOrdemServico.AGUARDANDO_APROVACAO, clock);
        os.aprovar("Aprovado pelo cliente.", clock);
        os.transicionar(StatusOrdemServico.EM_SERVICO, clock);
    }
    private void finalizar(OrdemServico os, Instant instante) {
        iniciar(os, instante);
        os.getItens().forEach(i -> i.concluir(relogio(instante)));
        os.transicionar(StatusOrdemServico.FINALIZADO, relogio(instante));
        repo(OrdemServicoRepository.class).saveAndFlush(os);
    }
    private void assertValor(String esperado, BigDecimal resultado) { assertEquals(0, valor(esperado).compareTo(resultado)); }

    @Test void migrationPodeSerExecutadaNovamenteSemRecriarTabelas() {
        assertEquals(0, flyway.migrate().migrationsExecuted);
        assertEquals("1", flyway.info().current().getVersion().toString());
        assertTrue(flyway.validateWithResult().validationSuccessful);
    }

    @Test void bancoRejeitaDespesaNegativaMesmoSemPassarPeloModel() {
        assertThrows(PersistenceException.class, () -> em.createNativeQuery("""
            insert into autogestao.despesa(id, descricao, categoria, valor, competencia, vencimento)
            values (:id, 'Teste', 'Teste', -1, DATE '2026-10-01', DATE '2026-10-05')
            """).setParameter("id", UUID.randomUUID().toString()).executeUpdate());
    }

    @Test void bancoRejeitaMarcacaoDeFuncionarioInexistente() {
        assertThrows(PersistenceException.class, () -> em.createNativeQuery("""
            insert into autogestao.marcacao_ponto(id, funcionario_id, tipo, instante, data_registro, requisicao_id)
            values (:id, :funcionario, 'ENTRADA', CURRENT_TIMESTAMP, CURRENT_DATE, :requisicao)
            """).setParameter("id", UUID.randomUUID().toString())
            .setParameter("funcionario", UUID.randomUUID().toString())
            .setParameter("requisicao", UUID.randomUUID().toString()).executeUpdate());
    }

    @Test void todosOsRepositoriesInicializamComQueriesValidas() {
        for (Class<?> tipo : List.of(ClienteRepository.class, VeiculoRepository.class, FuncionarioRepository.class,
            ServicoRepository.class, OrdemServicoRepository.class, ItemOrdemServicoRepository.class,
            HistoricoStatusOsRepository.class, PagamentoRepository.class, EstornoPagamentoRepository.class,
            DespesaRepository.class, MarcacaoPontoRepository.class, JornadaTrabalhoRepository.class,
            ExecucaoServicoRepository.class, RemapRepository.class, DiagnosticoRepository.class))
            assertNotNull(repo(tipo), tipo.getSimpleName());
    }
    @Test void buscaClientePorDocumentoNomeETelefoneComPaginacaoESemInativos() {
        var repo = repo(ClienteRepository.class);
        var cliente = repo.save(new Cliente(TipoCliente.PF, "João Silva", "12345678909", "11999999999", relogio(INICIO)));
        var inativo = cliente("João Souza"); inativo.desativar(); em.flush(); em.clear();
        assertEquals(cliente.getId(), repo.findByDocumento("12345678909").orElseThrow().getId());
        assertTrue(repo.existsByDocumento("12345678909"));
        assertEquals(1, repo.findByAtivoTrueAndNomeContainingIgnoreCase("SILVA", PAGINA).getTotalElements());
        assertEquals(1, repo.findByAtivoTrueAndNomeContainingIgnoreCase("joão", PageRequest.of(0, 1)).getTotalElements());
        assertEquals(1, repo.findByAtivoTrueAndTelefoneContaining("9999", PAGINA).getTotalElements());
    }
    @Test void funcionarioServicoEVeiculoPermitemConsultaSemCarregarTodaATabela() {
        var funcionario = funcionario(); var cliente = cliente("João"); var os = os(1, cliente, INICIO);
        var item = adicionar(os, CategoriaServico.MANUTENCAO, 1, "100", "30");
        var funcionarios = repo(FuncionarioRepository.class);
        assertTrue(funcionarios.existsByMatricula("OF-001"));
        assertEquals(funcionario.getId(), funcionarios.findByMatricula("OF-001").orElseThrow().getId());
        assertEquals(1, funcionarios.findByAtivoTrue(PAGINA).getTotalElements());
        assertEquals(funcionario.getId(), funcionarios.findParaAtualizacao(funcionario.getId()).orElseThrow().getId());
        var veiculos = repo(VeiculoRepository.class);
        assertTrue(veiculos.existsByPlaca(os.getVeiculo().getPlaca()));
        assertEquals(os.getVeiculo().getId(), veiculos.findByPlaca("ABC0001").orElseThrow().getId());
        assertEquals(1, veiculos.findByClienteIdAndAtivoTrue(cliente.getId(), PAGINA).getTotalElements());
        var servicos = repo(ServicoRepository.class);
        assertEquals(1, servicos.findByAtivoTrueAndCategoria(CategoriaServico.MANUTENCAO, PAGINA).getTotalElements());
        assertEquals(item.getServico().getId(), servicos.findByAtivoTrueAndNomeContainingIgnoreCase("manutencao", PAGINA).getContent().getFirst().getId());
    }
    @Test void revisoesIncluemVencidasEAtingidasPorKmSemVeiculosInativos() {
        var cliente = cliente("João"); var primeiro = os(1, cliente, INICIO).getVeiculo();
        primeiro.programarRevisao(LocalDate.of(2026, 10, 10), null);
        var segundo = os(2, cliente, INICIO).getVeiculo(); segundo.programarRevisao(null, 51000L); segundo.atualizarQuilometragem(52000);
        var terceiro = os(3, cliente, INICIO).getVeiculo(); terceiro.programarRevisao(LocalDate.of(2026, 10, 1), null); terceiro.desativar();
        em.flush(); em.clear();
        assertEquals(2, repo(VeiculoRepository.class).findRevisoesAte(LocalDate.of(2026, 10, 11), PAGINA).getTotalElements());
    }
    @Test void faturamentoUsaConclusaoComInicioInclusivoEFimExclusivo() {
        var cliente = cliente("João");
        var primeira = os(1, cliente, INICIO); adicionar(primeira, CategoriaServico.MANUTENCAO, 2, "100", "30"); finalizar(primeira, INICIO);
        primeira.transicionar(StatusOrdemServico.ENTREGUE, relogio(INICIO.plusSeconds(60)));
        var segunda = os(2, cliente, INICIO); adicionar(segunda, CategoriaServico.MANUTENCAO, 1, "999", "10"); finalizar(segunda, FIM);
        var aberta = os(3, cliente, INICIO); adicionar(aberta, CategoriaServico.MANUTENCAO, 1, "500", "100");
        var cancelada = os(4, cliente, INICIO); cancelada.transicionar(StatusOrdemServico.CANCELADO, relogio(INICIO));
        em.flush(); em.clear();
        var repo = repo(OrdemServicoRepository.class);
        assertValor("200", repo.somarFaturamento(INICIO, FIM)); assertValor("60", repo.somarCustosDiretos(INICIO, FIM));
        assertEquals(1, repo.contarFinalizadas(INICIO, FIM));
        assertEquals(primeira.getId(), repo.findFinalizadasNoPeriodo(INICIO, FIM, PAGINA).getContent().getFirst().getId());
        assertTrue(repo.existsByNumero(1)); assertEquals(primeira.getId(), repo.findByNumero(1).orElseThrow().getId());
        assertEquals(4, repo.findByClienteId(cliente.getId(), PAGINA).getTotalElements());
        assertEquals(1, repo.findByVeiculoId(primeira.getVeiculo().getId(), PAGINA).getTotalElements());
        assertEquals(1, repo.findByStatusIn(List.of(StatusOrdemServico.RASCUNHO), PAGINA).getTotalElements());
        assertNotNull(repo.findParaAtualizacao(primeira.getId()).orElseThrow());
    }
    @Test void clientesAtendidosSaoDistintosENaoIncluemCancelados() {
        var cliente = cliente("João"); var a = os(1, cliente, INICIO); var b = os(2, cliente, INICIO);
        a.transicionar(StatusOrdemServico.RECEBIDO, relogio(INICIO)); b.transicionar(StatusOrdemServico.RECEBIDO, relogio(INICIO));
        var c = os(3, cliente("Outro"), INICIO); c.transicionar(StatusOrdemServico.RECEBIDO, relogio(INICIO)); c.transicionar(StatusOrdemServico.CANCELADO, relogio(INICIO));
        var d = os(4, cliente("Fora"), INICIO); d.transicionar(StatusOrdemServico.RECEBIDO, relogio(FIM)); em.flush();
        assertEquals(1, repo(OrdemServicoRepository.class).contarClientesAtendidos(INICIO, FIM));
    }
    @Test void rankingContaQuantidadeDosConcluidosSemRetrabalhoOuOsAbertas() {
        var cliente = cliente("João"); var os = os(1, cliente, INICIO);
        var item = adicionar(os, CategoriaServico.MANUTENCAO, 3, "100", "30");
        var retrabalho = adicionar(os, CategoriaServico.DIAGNOSTICO, 1, "50", "10");
        iniciar(os, INICIO); retrabalho.registrarRetrabalho("Refazer teste");
        item.concluir(relogio(INICIO)); retrabalho.concluir(relogio(INICIO)); os.transicionar(StatusOrdemServico.FINALIZADO, relogio(INICIO));
        adicionar(os(2, cliente, INICIO), CategoriaServico.MANUTENCAO, 9, "100", "30"); em.flush(); em.clear();
        var resumo = repo(ItemOrdemServicoRepository.class).findMaisExecutados(INICIO, FIM, PAGINA);
        assertEquals(1, resumo.getNumberOfElements());
        assertEquals(3L, resumo.getContent().getFirst().getQuantidade());
        assertValor("300", resumo.getContent().getFirst().getReceita());
        assertValor("90", resumo.getContent().getFirst().getCustoDireto());
        assertEquals(2, repo(ItemOrdemServicoRepository.class).findByOrdemServicoId(os.getId(), PAGINA).getTotalElements());
        assertEquals(7, repo(HistoricoStatusOsRepository.class).findByOrdemServicoIdOrderByInstanteAsc(os.getId(), PAGINA).getTotalElements());
    }
    @Test void pagamentosEstornosEDespesasSaoSomadosEmSeusPropriosPeriodos() {
        var os = os(1, cliente("João"), INICIO); os.transicionar(StatusOrdemServico.RECEBIDO, relogio(INICIO));
        var pagamentos = repo(PagamentoRepository.class); var requisicao = UUID.randomUUID();
        var pagamento = pagamentos.save(new Pagamento(os, valor("100"), FormaPagamento.PIX, requisicao, relogio(INICIO)));
        pagamentos.save(new Pagamento(os, valor("999"), FormaPagamento.DINHEIRO, UUID.randomUUID(), relogio(FIM)));
        var estornos = repo(EstornoPagamentoRepository.class); var confirmacao = UUID.randomUUID();
        var estorno = estornos.save(new EstornoPagamento(pagamento, valor("20"), "Devolução", confirmacao, relogio(INICIO)));
        estornos.save(new EstornoPagamento(pagamento, valor("10"), "Outra devolução", UUID.randomUUID(), relogio(FIM)));
        var despesas = repo(DespesaRepository.class); var data = LocalDate.of(2026, 10, 1);
        var despesa = despesas.save(new Despesa("Aluguel", "Instalações", valor("50"), data, data.plusDays(5))); despesa.confirmarPagamento(relogio(INICIO));
        despesas.save(new Despesa("Conta futura", "Instalações", valor("999"), LocalDate.of(2026, 11, 1), data));
        em.flush(); em.clear();
        assertValor("100", pagamentos.somarRecebimentos(INICIO, FIM)); assertValor("1099", pagamentos.somarPorOrdemServico(os.getId()));
        assertEquals(pagamento.getId(), pagamentos.findByRequisicaoId(requisicao).orElseThrow().getId());
        assertEquals(2, pagamentos.findByOrdemServicoId(os.getId(), PAGINA).getTotalElements());
        assertNotNull(pagamentos.findParaAtualizacao(pagamento.getId()).orElseThrow());
        assertValor("20", estornos.somarEstornos(INICIO, FIM)); assertValor("30", estornos.somarPorPagamento(pagamento.getId()));
        assertEquals(estorno.getId(), estornos.findByRequisicaoId(confirmacao).orElseThrow().getId());
        assertEquals(2, estornos.findByPagamentoId(pagamento.getId(), PAGINA).getTotalElements());
        assertValor("50", despesas.somarPorCompetencia(data, LocalDate.of(2026, 11, 1))); assertValor("50", despesas.somarPagas(INICIO, FIM));
        assertEquals(1, despesas.findByPagaEmIsNullAndVencimentoLessThanEqual(data.plusDays(10), PAGINA).getTotalElements());
    }
    @Test void agregacoesSemRegistrosRetornamZero() {
        var os = repo(OrdemServicoRepository.class);
        assertValor("0", os.somarFaturamento(INICIO, FIM)); assertValor("0", os.somarCustosDiretos(INICIO, FIM));
        assertEquals(0, os.contarFinalizadas(INICIO, FIM)); assertEquals(0, os.contarClientesAtendidos(INICIO, FIM));
        assertValor("0", repo(PagamentoRepository.class).somarRecebimentos(INICIO, FIM));
        assertValor("0", repo(EstornoPagamentoRepository.class).somarEstornos(INICIO, FIM));
        assertValor("0", repo(DespesaRepository.class).somarPagas(INICIO, FIM));
    }
    @Test void pontoBuscaConfirmacaoUltimaMarcacaoEPeriodoPaginado() {
        var funcionario = funcionario(); var pontos = repo(MarcacaoPontoRepository.class); var requisicao = UUID.randomUUID();
        var entrada = pontos.save(MarcacaoPonto.registrar(funcionario, TipoMarcacao.ENTRADA, requisicao, relogio(INICIO), ZoneId.of("America/Sao_Paulo")));
        var saida = pontos.save(MarcacaoPonto.registrar(funcionario, TipoMarcacao.SAIDA, UUID.randomUUID(), relogio(FIM), ZoneId.of("America/Sao_Paulo")));
        em.flush(); em.clear();
        assertEquals(entrada.getId(), pontos.findByFuncionario_IdAndRequisicaoId(funcionario.getId(), requisicao).orElseThrow().getId());
        assertEquals(saida.getId(), pontos.findFirstByFuncionario_IdOrderByInstanteDesc(funcionario.getId()).orElseThrow().getId());
        assertEquals(1, pontos.findNoPeriodo(funcionario.getId(), INICIO, FIM, PageRequest.of(0, 1)).getTotalElements());
        assertEquals(entrada.getId(), pontos.findNoPeriodo(funcionario.getId(), INICIO, FIM, PAGINA).getContent().getFirst().getId());
    }
    @Test void bancoImpedeRepetirConfirmacaoDePontoDoMesmoFuncionario() {
        var funcionario = funcionario(); var pontos = repo(MarcacaoPontoRepository.class); var requisicao = UUID.randomUUID();
        pontos.saveAndFlush(MarcacaoPonto.registrar(funcionario, TipoMarcacao.ENTRADA, requisicao, relogio(INICIO), ZoneOffset.UTC));
        assertThrows(PersistenceException.class, () -> pontos.saveAndFlush(MarcacaoPonto.registrar(funcionario, TipoMarcacao.SAIDA, requisicao, relogio(FIM), ZoneOffset.UTC)));
    }
    @Test void jornadaSelecionaSomenteDiaEVigenciaSolicitados() {
        var funcionario = funcionario(); var jornadas = repo(JornadaTrabalhoRepository.class);
        var antiga = jornadas.save(new JornadaTrabalho(funcionario, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0), 60, 5, LocalDate.of(2026, 9, 1)));
        antiga.encerrarVigencia(LocalDate.of(2026, 9, 30));
        var vigente = jornadas.save(new JornadaTrabalho(funcionario, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0), 60, 5, LocalDate.of(2026, 10, 1)));
        em.flush(); em.clear();
        var resultado = jornadas.findVigentes(funcionario.getId(), DayOfWeek.MONDAY, LocalDate.of(2026, 10, 5), PAGINA);
        assertEquals(1, resultado.getNumberOfElements()); assertEquals(vigente.getId(), resultado.getContent().getFirst().getId());
        assertTrue(jornadas.findVigentes(funcionario.getId(), DayOfWeek.TUESDAY, LocalDate.of(2026, 10, 6), PAGINA).isEmpty());
    }
    @Test void execucaoIncluiSobreposicaoEPeriodosAbertosSemContarAdjacentes() {
        var funcionario = funcionario(); var os = os(1, cliente("João"), INICIO); var item = adicionar(os, CategoriaServico.MANUTENCAO, 1, "100", "20"); iniciar(os, INICIO);
        var execucoes = repo(ExecucaoServicoRepository.class);
        var concluida = execucoes.save(new ExecucaoServico(item, funcionario, relogio(INICIO))); concluida.encerrar(relogio(INICIO.plusSeconds(3600)));
        var aberta = execucoes.save(new ExecucaoServico(item, funcionario, relogio(INICIO.plusSeconds(3600)))); em.flush(); em.clear();
        assertTrue(execucoes.existsByFuncionarioIdAndFimIsNull(funcionario.getId())); assertTrue(execucoes.existsByItemIdAndFimIsNull(item.getId()));
        assertEquals(2, execucoes.findByItemId(item.getId(), PAGINA).getTotalElements());
        assertEquals(2, execucoes.findSobrepostas(funcionario.getId(), INICIO.plusSeconds(1800), INICIO.plusSeconds(7200), PAGINA).getTotalElements());
        var posteriores = execucoes.findSobrepostas(funcionario.getId(), INICIO.plusSeconds(3600), INICIO.plusSeconds(7200), PAGINA);
        assertEquals(1, posteriores.getTotalElements()); assertEquals(aberta.getId(), posteriores.getContent().getFirst().getId());
    }
    @Test void remapEDiagnosticoSaoConsultadosPorItemECategoria() {
        var os = os(1, cliente("João"), INICIO);
        var ecu = adicionar(os, CategoriaServico.REMAP_ECU, 1, "100", "20");
        var diag = adicionar(os, CategoriaServico.DIAGNOSTICO, 1, "50", "10");
        var remaps = repo(RemapRepository.class); var remap = remaps.save(new Remap(ecu, "Personalizado", "Bosch", "EDC17", "Dirigibilidade"));
        var diagnosticos = repo(DiagnosticoRepository.class); var diagnostico = new Diagnostico(diag, "Falha");
        diagnostico.registrarResultado("Scanner", "P0300", "Teste", "Falha de ignição"); diagnostico = diagnosticos.save(diagnostico); em.flush(); em.clear();
        assertEquals(remap.getId(), remaps.findByItemId(ecu.getId()).orElseThrow().getId());
        assertEquals(1, remaps.findByItemCategoria(CategoriaServico.REMAP_ECU, PAGINA).getTotalElements());
        assertEquals(diagnostico.getId(), diagnosticos.findByItemId(diag.getId()).orElseThrow().getId());
        assertEquals(1, diagnosticos.findByCausaIdentificadaContainingIgnoreCase("FALHA", PAGINA).getTotalElements());
        assertEquals(1, diagnosticos.findByCausaIdentificadaContainingIgnoreCase("ignição", PAGINA).getTotalElements());
    }
}
