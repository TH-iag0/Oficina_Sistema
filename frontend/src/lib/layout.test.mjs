import test from 'node:test';
import assert from 'node:assert/strict';
import { estrutura } from '../views.js';
import { demonstracao } from './demo.js';

function estado(pagina, dados = demonstracao('2026-10')) {
    return { pagina, dados, mes: '2026-10', modo: 'demo', graficoModo: '3d', resumo: dados.resumo };
}

test('marca usa logo nova e preserva o título da tela sem o texto Local', () => {
    const html = estrutura(estado('financeiro'));
    assert.ok(html.includes('class="brand-logo"'));
    assert.ok(html.includes('src="/src/assets/forged-logo.png'));
    assert.ok(html.includes('<h1>Financeiro</h1>'));
    assert.ok(!html.includes('AUTOGESTÃO LOCAL'));
    assert.ok(!html.includes('AutoGestão Local'));
});

test('erro de conexão não anuncia backend conectado e permite abrir demonstração', () => {
    const base = { ...estado('ponto'), modo: 'real' };
    const html = estrutura({ ...base, erro: 'Falha de comunicação' });
    assert.ok(html.includes('Backend indisponível'));
    assert.ok(!html.includes('Backend conectado'));
    assert.ok(!html.includes('connection real'));
    assert.ok(html.includes('data-demo>Abrir demonstração'));
    assert.ok(estrutura({ ...base, carregando: true }).includes('Conectando…'));
    const demo = estrutura(estado('ponto'));
    assert.ok(demo.includes('data-form="funcionario"'));
    assert.ok(demo.includes('Modo demonstrativo'));
});

test('visão geral limita a prévia a quatro ordens e mantém acesso à operação completa', () => {
    const dados = demonstracao('2026-10');
    dados.ordens.push({ ...dados.ordens[0], id: 'quinta-ordem', numero: 9999 });
    const antes = JSON.stringify(dados);
    const resumo = estrutura(estado('geral', dados));
    const operacao = estrutura(estado('operacao', dados));
    assert.equal((resumo.match(/data-atendimento=/g) || []).length, 4);
    assert.ok(resumo.includes('data-page="operacao">Ver operação'));
    assert.ok(!resumo.includes('#9999'));
    assert.ok(operacao.includes('data-atendimento="quinta-ordem"'));
    assert.equal(JSON.stringify(dados), antes);
});

test('distribuição mantém todas as situações e o estado vazio não inventa ordens', () => {
    const dados = demonstracao('2026-10');
    dados.dashboard.ordensPorStatus = { RECEBIDO: 0, EM_SERVICO: 3, CANCELADO: 2 };
    dados.ordens = [];
    const html = estrutura(estado('geral', dados));
    assert.ok(html.includes('&quot;name&quot;:&quot;Cancelado&quot;,&quot;value&quot;:2'));
    assert.ok(html.includes('&quot;name&quot;:&quot;Recebido&quot;,&quot;value&quot;:0'));
    assert.ok(html.includes('Nenhum registro por enquanto'));
    assert.ok(!html.includes('data-atendimento='));
});

test('telas reorganizadas preservam ações de atendimento, financeiro e jornada', () => {
    assert.ok(estrutura(estado('operacao')).includes('data-form="ordem"'));
    assert.ok(estrutura(estado('financeiro')).includes('data-form="despesa"'));
    const ponto = estrutura(estado('ponto'));
    assert.ok(ponto.includes('data-jornada='));
    assert.ok(ponto.includes('data-encerrar-jornada='));
    assert.ok(ponto.includes('Abrir página do funcionário'));
    const tecnico = estrutura(estado('tecnico'));
    assert.ok(tecnico.includes('data-chart="weekly"'));
    assert.ok(tecnico.includes('data-chart="bars"'));
});
