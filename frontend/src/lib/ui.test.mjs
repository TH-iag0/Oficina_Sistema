import test from 'node:test';
import assert from 'node:assert/strict';
import { escapar, moeda, percentual, horas, intervaloMes, confirmacaoPonto } from './ui.js';
import { demonstracao } from './demo.js';
import { estrutura, telaPonto } from '../views.js';

test('conteúdo de clientes é escapado antes de inserir no HTML', () => {
    const dados = demonstracao('2026-10');
    dados.clientes[0].nome = '<img src=x onerror="alert(1)">';
    const html = estrutura({ pagina: 'clientes', mes: '2026-10', modo: 'demo', dados });
    assert.ok(html.includes('&lt;img'));
    assert.ok(!html.includes('<img src=x'));
    assert.equal(escapar("&<'\""), '&amp;&lt;&#39;&quot;');
});

test('valores sem comparação não inventam crescimento e valores negativos são preservados', () => {
    assert.equal(percentual(null), 'Sem comparação');
    assert.equal(percentual(-12.8), '-12,8%');
    assert.equal(moeda(null), '—');
    assert.equal(horas(485), '8h 05min');
});

test('intervalo mensal usa fim exclusivo em dezembro e ano bissexto', () => {
    assert.deepEqual(intervaloMes('2026-12'), { inicio: '2026-12-01', fim: '2027-01-01' });
    assert.deepEqual(intervaloMes('2024-02'), { inicio: '2024-02-01', fim: '2024-03-01' });
    assert.throws(() => intervaloMes('2026-13'));
});

test('retry de ponto depois de erro de rede conserva UUID e payload', async () => {
    const chamadas = [];
    const confirmar = confirmacaoPonto(async dados => {
        chamadas.push({ ...dados });
        if (chamadas.length === 1) throw new TypeError('Offline');
        return { id: 'registro' };
    }, () => 'uuid-teste');
    const dados = { matricula: '001', pin: '0123', tipo: 'ENTRADA' };
    await assert.rejects(confirmar(dados));
    await assert.rejects(confirmar({ ...dados, tipo: 'SAIDA' }), /Repita a marcação/);
    await confirmar(dados);
    assert.deepEqual(chamadas[0], chamadas[1]);
    assert.equal(chamadas[1].pin, '0123');
});

test('erro 4xx libera nova confirmação e sucesso gera novo identificador', async () => {
    let sequencia = 0, rejeitar = true;
    const confirmar = confirmacaoPonto(async dados => {
        if (rejeitar) { rejeitar = false; throw Object.assign(new Error('PIN inválido'), { status: 401 }); }
        return dados;
    }, () => String(++sequencia));
    await assert.rejects(confirmar({ pin: '1234' }));
    assert.equal((await confirmar({ pin: '5678' })).requisicaoId, '2');
    assert.equal((await confirmar({ pin: '5678' })).requisicaoId, '3');
});

test('modo real com falha não renderiza dados demonstrativos antigos', () => {
    const html = estrutura({ pagina: 'geral', mes: '2026-10', modo: 'real', dados: demonstracao('2026-10'), erro: 'Offline' });
    assert.ok(html.includes('Não foi possível carregar'));
    assert.ok(!html.includes('48.750'));
    assert.ok(!html.includes('Dados fictícios'));
});

test('prévia percorre sete telas e ponto público não pede senha administrativa', () => {
    const dados = demonstracao('2026-10');
    for (const pagina of ['geral', 'operacao', 'clientes', 'servicos', 'tecnico', 'financeiro', 'ponto']) {
        assert.ok(estrutura({ pagina, mes: '2026-10', modo: 'demo', dados }).includes('Modo demonstrativo'));
    }
    const ponto = telaPonto('real');
    assert.ok(ponto.includes('name="pin"'));
    assert.ok(!ponto.includes('name="senha"'));
    assert.ok(!ponto.includes('id="point-demo" type="checkbox" checked'));
});

test('série demonstrativa acompanha o mês e atravessa a virada do ano', () => {
    const serie = demonstracao('2027-02').indicadores.evolucao;
    assert.equal(serie[0].mes, '2026-09');
    assert.equal(serie.at(-1).mes, '2027-02');
});


test('painel permite escolher demonstração e dados reais e separa extrato do faturamento', () => {
    const dados = demonstracao('2026-10');
    let html = estrutura({ pagina: 'financeiro', mes: '2026-10', modo: 'demo', dados });
    assert.ok(html.includes('data-demo'));
    assert.ok(html.includes('data-connect'));
    assert.ok(html.includes('Extrato bancário · demonstração'));
    dados.banco = { configurado: true, ultimaConsulta: null, entradas: 200, saidas: 100,
        saldoDoPeriodo: 100, movimentos: [{ descricao: '<script>1</script>', instante: '2026-10-14T12:00:00Z', valor: -100 }] };
    html = estrutura({ pagina: 'financeiro', mes: '2026-10', modo: 'real', dados });
    assert.ok(html.includes('&lt;script&gt;'));
    assert.ok(html.includes('não são faturamento e lucro'));
    assert.ok(html.includes('data-bank-sync'));
});
