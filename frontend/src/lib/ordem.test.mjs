import test from 'node:test';
import assert from 'node:assert/strict';
import { camposAbertura, dadosAbertura } from './ordem.js';
import { acoesSituacao } from './atendimento.js';
import { detalhesAtendimento } from '../atendimento.js';
import { ordensDemo, carregarOrdens, filtroOrdens, paginacaoOrdens } from '../ordens-lista.js';
import { demonstracao } from './demo.js';

test('setas da paginação mantêm nomes acessíveis e bloqueiam os limites', () => {
    const estado = {};
    const primeira = paginacaoOrdens(estado, { total: 40, totalPaginas: 2 });
    assert.match(primeira, /disabled aria-label="Página anterior"/);
    assert.ok(primeira.includes('<span aria-hidden="true">‹</span>'));
    assert.ok(primeira.includes('<span aria-hidden="true">›</span>'));
    filtroOrdens(estado).pagina = 1;
    const ultima = paginacaoOrdens(estado, { total: 40, totalPaginas: 2 });
    assert.match(ultima, /disabled aria-label="Próxima página"/);
    assert.ok(!ultima.includes('>Anterior</button>'));
});

test('abertura envia atendimento e primeiro serviço juntos, mantendo preço e custo zero', () => {
    const dados = dadosAbertura({ clienteId: 'c', veiculoId: 'v', situacaoInicial: 'DIAGNOSTICO',
        previsaoEntrega: '2099-12-01T12:00:00Z', incluirServico: true, servicoId: 's', quantidade: '2', preco: '0', custo: '0' });
    assert.equal(dados.situacaoInicial, 'DIAGNOSTICO');
    assert.equal(dados.previsaoEntrega, '2099-12-01T12:00:00.000Z');
    assert.deepEqual(dados.servicoInicial, { servicoId: 's', quantidade: 2, preco: 0, custo: 0 });
    assert.equal(dadosAbertura({ clienteId: 'c', veiculoId: 'v', situacaoInicial: 'RECEBIDO' }).servicoInicial, undefined);
});

test('abertura recusa pular aprovação e concluir sem executar o atendimento', () => {
    const base = { clienteId: 'c', veiculoId: 'v', situacaoInicial: 'RECEBIDO' };
    for (const situacaoInicial of ['EM_SERVICO', 'FINALIZADO', 'APROVADO']) assert.throws(() => dadosAbertura({ ...base, situacaoInicial }));
    assert.throws(() => dadosAbertura({ ...base, incluirServico: true, servicoId: 's', quantidade: '1.5', preco: '10', custo: '1' }));
    assert.throws(() => dadosAbertura({ ...base, clienteId: '' }));
    assert.throws(() => dadosAbertura({ ...base, previsaoEntrega: 'inválido' }));
});

test('formulário de criação apresenta atendimento e preserva rascunho ao retornar de cadastros', () => {
    const html = camposAbertura({ clienteId: 'c', situacaoInicial: 'DIAGNOSTICO', previsaoEntrega: '2099-12-01T12:30', incluirServico: true, quantidade: 2, preco: 120, custo: 10 });
    assert.ok(html.includes('Situação inicial'));
    assert.ok(html.includes('Previsão de entrega'));
    assert.ok(html.includes('Criar ordem e acompanhar'));
    assert.ok(html.includes('value="DIAGNOSTICO" selected'));
    assert.ok(html.includes('value="2099-12-01T12:30"'));
    assert.ok(html.includes('value="120"'));
    assert.ok(!html.includes('data-order-search'));
});

test('situação oferece somente as próximas etapas válidas e explica finalização bloqueada', () => {
    assert.equal(acoesSituacao({ status: 'RECEBIDO', itens: [] })[0].destino, 'DIAGNOSTICO');
    assert.equal(acoesSituacao({ status: 'DIAGNOSTICO', itens: [] })[0].destino, 'AGUARDANDO_APROVACAO');
    const ordem = { ...demonstracao('2026-10').ordens[0], status: 'EM_SERVICO' };
    assert.equal(acoesSituacao(ordem).find(a => a.destino === 'FINALIZADO').bloqueada, true);
    ordem.itens = [{ ...ordem.itens[0], concluidoEm: '2026-10-15T12:00:00Z' }];
    assert.equal(acoesSituacao(ordem).find(a => a.destino === 'FINALIZADO').bloqueada, false);
    assert.deepEqual(acoesSituacao({ status: 'CANCELADO', itens: [] }), []);
    const html = detalhesAtendimento(ordem, demonstracao('2026-10'));
    assert.ok(html.includes('Situação do veículo'));
    assert.ok(html.includes('Histórico das situações e horários'));
});

test('pesquisa de ordens encontra cliente, placa, serviço e registro após os primeiros cem', () => {
    const dados = demonstracao('2026-10');
    dados.ordens = Array.from({ length: 105 }, (_, i) => ({ ...dados.ordens[0], id: String(i), numero: 5000 + i, status: i === 104 ? 'DIAGNOSTICO' : 'RECEBIDO' }));
    assert.equal(ordensDemo(dados, { pagina: 5 }).itens.length, 5);
    assert.equal(ordensDemo(dados, { busca: '5104', status: 'DIAGNOSTICO' }).total, 1);
    assert.equal(ordensDemo(dados, { busca: 'FEL-2A21' }).total, 105);
    assert.equal(ordensDemo(dados, { busca: 'RAFAEL' }).total, 105);
    assert.equal(ordensDemo(dados, { busca: 'Remap ECU' }).total, 105);
    assert.equal(ordensDemo(dados, { busca: '5104', status: 'FINALIZADO' }).total, 0);
});

test('resposta antiga da pesquisa de ordens não substitui a pesquisa mais recente', async () => {
    const pendentes = [], dados = demonstracao('2026-10');
    const estado = { pagina: 'operacao', modo: 'real', dados, api: { ordens: { listar: () => new Promise(resolve => pendentes.push(resolve)) } } };
    const contexto = { estado, mensagemErro: e => e.message };
    const antiga = carregarOrdens(contexto);
    filtroOrdens(estado).busca = 'Nova';
    const nova = carregarOrdens(contexto);
    pendentes[1]({ itens: [], total: 0, totalPaginas: 0 });
    await nova;
    pendentes[0]({ itens: [dados.ordens[0]], total: 1, totalPaginas: 1 });
    await antiga;
    assert.equal(filtroOrdens(estado).resultado.total, 0);
});
