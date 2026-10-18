import test from 'node:test';
import assert from 'node:assert/strict';
import { escalaFinanceira, seriesFinanceiras, variacaoFinanceira, analiseDemonstrativa } from './financeiro.js';
import { demonstracao } from './demo.js';
import { estrutura } from '../views.js';
import { criarApi } from '../services/api.js';

test('escala financeira inclui zero, centavos e movimento negativo sem truncar valores', () => {
    for (const valores of [[0, 0], [-950, 1500, 70], [.01, .03], [-5, -1]]) {
        const escala = escalaFinanceira(valores);
        assert.ok(escala.minimo <= Math.min(0, ...valores));
        assert.ok(escala.maximo >= Math.max(0, ...valores));
        assert.ok(escala.maximo > escala.minimo);
        assert.ok(Number.isFinite(escala.passo) && escala.passo > 0);
    }
});

test('séries preservam saldo negativo e distinguem faturamento de recebimento', () => {
    const caixa = seriesFinanceiras('cashflow', [{ entradas: '50.25', saidas: '65.15', saldo: '-14.90' }]);
    assert.equal(caixa[0].valores[0], 50.25);
    assert.equal(caixa[2].valores[0], -14.9);
    const receita = seriesFinanceiras('revenue', [{ receita: '100.50', custosDiretos: '80.20', recebimentos: '20' }]);
    assert.equal(receita[0].valores[0], 100.5);
    assert.equal(receita[1].valores[0], 80.2);
});

test('demonstração mantém caixa pago separado de despesas da competência', () => {
    const resultado = analiseDemonstrativa([{ mes: '2026-10', recebimentos: 10, estornos: 5 }], [
        { competencia: '2026-10-01', categoria: 'Estrutura', valor: 100, pagaEm: null },
        { competencia: '2026-09-01', categoria: 'Anterior', valor: 20, pagaEm: '2026-10-02T12:00:00Z' }
    ], '2026-10');
    assert.equal(resultado.fluxoCaixa[0].saidas, 25);
    assert.equal(resultado.fluxoCaixa[0].saldo, -15);
    assert.deepEqual(resultado.despesasPorCategoria, [{ nome: 'Estrutura', valor: 100 }]);
});

test('períodos de 3, 6 e 12 meses terminam no mês selecionado e conservam total de despesas', () => {
    for (const quantidade of [3, 6, 12]) {
        const dados = demonstracao('2027-01', quantidade);
        const fluxo = dados.analiseFinanceira.fluxoCaixa;
        assert.equal(fluxo.length, quantidade);
        assert.equal(fluxo.at(-1).mes, '2027-01');
        assert.equal(new Set(fluxo.map(m => m.mes)).size, quantidade);
        assert.equal(dados.analiseFinanceira.despesasPorCategoria.reduce((total, c) => total + c.valor, 0), 7200);
        assert.equal(fluxo.at(-1).despesasPagas, 6060);
    }
});

test('comparação não inventa crescimento quando não existe base e usa módulo da base negativa', () => {
    assert.equal(variacaoFinanceira(100, 0), null);
    assert.equal(variacaoFinanceira(100, null), null);
    assert.equal(variacaoFinanceira(-50, -100), 50);
    assert.equal(variacaoFinanceira(80, 100), -20);
});

test('financeiro 2D e 3D conserva as séries e escapa categorias vindas do servidor', () => {
    const dados = demonstracao('2026-10');
    dados.analiseFinanceira.despesasPorCategoria[0].nome = '<img src=x onerror=alert(1)>';
    const estado = { pagina: 'financeiro', mes: '2026-10', financeiroMeses: 6, dados, modo: 'real' };
    const plano = estrutura({ ...estado, graficoModo: '2d' });
    const profundo = estrutura({ ...estado, graficoModo: '3d' });
    const series = html => [...html.matchAll(/data-values="([^"]+)"/g)].map(m => m[1]);
    assert.deepEqual(series(plano), series(profundo));
    assert.ok(plano.includes('data-chart="cashflow"'));
    assert.ok(plano.includes('data-chart="revenue"'));
    assert.ok(plano.includes('data-chart="donut"'));
    assert.ok(!plano.includes('<img src=x'));
    assert.ok(plano.includes('&lt;img src=x onerror=alert(1)&gt;'));
    assert.ok(plano.includes('data-form="despesa"'));
});

test('consulta financeira envia período ao backend sem baixar todas as despesas', async () => {
    const chamadas = [];
    const api = criarApi({ fetchImpl: async (url, opcoes) => { chamadas.push({ url, opcoes }); return Response.json({}); } });
    await api.financeiro.analises({ mes: '2026-10', meses: 12 });
    assert.equal(chamadas[0].url, 'http://localhost:8080/api/financeiro/analises?mes=2026-10&meses=12');
    assert.equal(chamadas[0].opcoes.method, 'GET');
});
