import test from 'node:test';
import assert from 'node:assert/strict';
import { adiarPesquisa, redesenharPesquisa } from './pesquisa.js';
import { listaCadastro } from '../catalogos.js';
import { pesquisaOrdens } from '../ordens-lista.js';

test('pesquisa espera a pausa na digitação e usa apenas o último termo', t => {
    t.mock.timers.enable({ apis: ['setTimeout'] });
    const termos = [], pesquisar = adiarPesquisa(termo => termos.push(termo));
    pesquisar('óleo'); t.mock.timers.tick(200);
    pesquisar('óleo motor'); t.mock.timers.tick(299);
    assert.deepEqual(termos, []);
    t.mock.timers.tick(1);
    assert.deepEqual(termos, ['óleo motor']);
    pesquisar('outro'); pesquisar.cancelar(); t.mock.timers.tick(300);
    assert.deepEqual(termos, ['óleo motor']);
});

test('atualização preserva o texto, foco e posição do cursor', () => {
    const form = { dataset: { cadastroSearch: 'servico' } };
    const antigo = { matches: () => true, closest: () => form, value: 'óleo novo', selectionStart: 4, selectionEnd: 4 };
    let foco = false, selecao;
    const novo = { value: '', focus() { foco = true; }, setSelectionRange(...args) { selecao = args; } };
    const root = { ownerDocument: { activeElement: antigo }, contains: () => true, querySelector: () => novo };
    let renderizou = false;
    redesenharPesquisa(root, () => { renderizou = true; });
    assert.equal(renderizou, true);
    assert.equal(novo.value, 'óleo novo');
    assert.equal(foco, true);
    assert.deepEqual(selecao, [4, 4]);
});

test('pesquisa de ordens preserva cursor quando o atributo está vazio', () => {
    const form = { dataset: { orderSearch: '' } };
    const antigo = { matches: () => true, closest: () => form, value: '1048', selectionStart: 2, selectionEnd: 4 };
    let seletor, selecao, foco = false;
    const novo = { value: '', focus() { foco = true; }, setSelectionRange(...args) { selecao = args; } };
    const root = {
        ownerDocument: { activeElement: antigo }, contains: () => true,
        querySelector(valor) { seletor = valor; return novo; }
    };
    redesenharPesquisa(root, () => {});
    assert.equal(seletor, '[data-order-search] input[name=busca]');
    assert.equal(novo.value, '1048');
    assert.equal(foco, true);
    assert.deepEqual(selecao, [2, 4]);
});

test('pesquisas de cadastros e ordens usam input com lupa sem botão pesquisar', () => {
    const estado = { modo: 'demo', dados: { servicos: [] } };
    for (const html of [listaCadastro('servico', estado), pesquisaOrdens({})]) {
        assert.ok(html.includes('class="search-icon"'));
        assert.ok(html.includes('name="busca"'));
        assert.ok(!html.includes('>Pesquisar</button>'));
    }
});
