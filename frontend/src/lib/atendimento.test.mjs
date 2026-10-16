import test from 'node:test';
import assert from 'node:assert/strict';

test('consulta informativa mantém dados e histórico sem ações de alteração', () => {
    const dados = demonstracao('2026-10');
    for (const status of ['EM_SERVICO', 'RECEBIDO', 'AGUARDANDO_APROVACAO', 'FINALIZADO']) {
        const ordem = { ...dados.ordens[0], status };
        const html = detalhesAtendimento(ordem, dados, true);
        assert.ok(!html.includes('<button'));
        assert.ok(!html.includes('data-work='));
        assert.ok(!html.includes('Use a ação abaixo'));
        assert.ok(!html.includes('habilitar a finalização'));
        assert.ok(html.includes('Histórico das situações e horários'));
        assert.ok(html.includes('Total dos serviços'));
        assert.ok(html.includes(ordem.itens[0].descricao));
    }
});
import { dadosConclusao, podeFinalizar } from './atendimento.js';
import { detalhesAtendimento } from '../atendimento.js';
import { demonstracao } from './demo.js';
import { confirmacaoIdempotente } from './ui.js';

test('texto livre conserva detalhes e novas linhas sem exigir campos técnicos', () => {
    assert.deepEqual(dadosConclusao({ relato: '  Troquei o óleo.\nTeste de rodagem OK.  ', custoReal: '80.50', motivoRetrabalho: '' }), {
        relato: 'Troquei o óleo.\nTeste de rodagem OK.', custoReal: 80.5, motivoRetrabalho: null
    });
    assert.equal(dadosConclusao({ relato: 'Inspeção visual', custoReal: '0' }).custoReal, 0);
    for (const valores of [{ relato: '', custoReal: '0' }, { relato: 'a'.repeat(4001), custoReal: '1' }, { relato: 'Troca', custoReal: '-1' }]) assert.throws(() => dadosConclusao(valores));
});

test('finalização exige todos os serviços concluídos', () => {
    assert.equal(podeFinalizar({ status: 'EM_SERVICO', itens: [] }), false);
    assert.equal(podeFinalizar({ status: 'EM_SERVICO', itens: [{ concluidoEm: null }] }), false);
    assert.equal(podeFinalizar({ status: 'EM_SERVICO', itens: [{ concluidoEm: '2026-10-13T10:00Z' }] }), true);
});

test('relato salvo é exibido com escape e permite escrita rápida em serviço pendente', () => {
    const dados = demonstracao('2026-10');
    const ordem = dados.ordens[0];
    ordem.itens[0].relato = '<script>alert(1)</script>';
    const html = detalhesAtendimento(ordem, dados);
    assert.ok(html.includes('Escrever o que fiz'));
    assert.ok(html.includes('&lt;script&gt;'));
    assert.ok(!html.includes('<script>'));
});

test('recebimento com retorno incerto conserva identificador em retry', async () => {
    const chamadas = [];
    const confirmar = confirmacaoIdempotente(async dados => {
        chamadas.push(dados);
        if (chamadas.length === 1) throw new TypeError('Offline');
        return dados;
    }, () => 'uuid-pagamento');
    const dados = { valor: 80, forma: 'PIX' };
    await assert.rejects(confirmar(dados));
    await assert.rejects(confirmar({ ...dados, valor: 100 }), /Repita o envio anterior/);
    await confirmar(dados);
    assert.deepEqual(chamadas[0], chamadas[1]);
});
