import test from 'node:test';
import assert from 'node:assert/strict';
import { executarAcao } from './acoes-interface.js';

test('ações aceitam atributos vazios e página zero, executando somente a primeira ação correspondente', async () => {
    const chamadas = [];
    const acoes = { refresh: valor => chamadas.push(['refresh', valor]), orderPage: valor => chamadas.push(['pagina', Number(valor)]) };
    assert.equal(await executarAcao({ dataset: { refresh: '', orderPage: '0' } }, acoes), true);
    assert.equal(await executarAcao({ dataset: { orderPage: '0' } }, acoes), true);
    assert.deepEqual(chamadas, [['refresh', ''], ['pagina', 0]]);
});

test('ações ignoram atributos herdados e propagam falhas sem executar outra operação', async () => {
    let chamadas = 0;
    const falha = new Error('Falha de gravação');
    const acoes = { salvar: async () => { chamadas++; throw falha; }, excluir: () => { chamadas++; } };
    assert.equal(await executarAcao({ dataset: Object.create({ salvar: '' }) }, acoes), false);
    await assert.rejects(executarAcao({ dataset: { salvar: '', excluir: '' } }, acoes), falha);
    assert.equal(chamadas, 1);
});
