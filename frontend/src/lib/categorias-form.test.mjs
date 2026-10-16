import test from 'node:test';
import assert from 'node:assert/strict';
import { montarCategoriasServico } from '../categorias-servico.js';

function formulario(api, modo = 'real') {
    const handlers = {};
    const campo = { value: '  Higienização  ', addEventListener() {}, focus() {} };
    const botao = { disabled: false, addEventListener: (nome, handler) => { handlers[nome] = handler; } };
    const erro = { textContent: '' }, detalhes = { open: true };
    const categoria = { value: 'OUTRO', innerHTML: '', focus() {} };
    const elementos = { '[name=novaCategoria]': campo, '[data-adicionar-categoria]': botao,
        '[data-categoria-error]': erro, '[data-nova-categoria]': detalhes };
    const form = { isConnected: true, dataset: {}, elements: { categoria,
        nome: { value: 'Limpeza completa' }, preco: { value: '150' }, custo: { value: '20' } },
        querySelector: seletor => elementos[seletor] };
    const estado = { modo, api: { servicos: { cadastrarCategoria: api } } };
    montarCategoriasServico(form, { estado, aviso() {}, mensagemErro: e => e.message }, []);
    return { form, estado, campo, erro, botao, clicar: () => handlers.click() };
}

test('criar categoria seleciona a opção sem apagar o rascunho e impede duplo envio', async () => {
    let resolver, chamadas = 0;
    const ui = formulario(() => { chamadas++; return new Promise(resolve => { resolver = resolve; }); });
    const primeira = ui.clicar();
    await ui.clicar();
    assert.equal(chamadas, 1);
    assert.equal(ui.form.dataset.categoriaLoading, 'true');
    resolver({ id: 'nova', nome: 'Higienização', padrao: false });
    await primeira;
    assert.equal(ui.form.elements.categoria.value, 'nova');
    assert.equal(ui.form.elements.nome.value, 'Limpeza completa');
    assert.equal(ui.form.elements.preco.value, '150');
    assert.equal(ui.form.elements.custo.value, '20');
    assert.equal(ui.botao.disabled, false);
    assert.equal(ui.form.dataset.categoriaLoading, undefined);
});

test('erro preserva o nome digitado e resposta atrasada não altera formulário fechado', async () => {
    const falha = formulario(async () => { throw new Error('Categoria já cadastrada.'); });
    await falha.clicar();
    assert.equal(falha.erro.textContent, 'Categoria já cadastrada.');
    assert.equal(falha.campo.value, '  Higienização  ');
    assert.equal(falha.botao.disabled, false);
    let resolver;
    const fechada = formulario(() => new Promise(resolve => { resolver = resolve; }));
    const pendente = fechada.clicar();
    fechada.form.isConnected = false;
    resolver({ id: 'nova', nome: 'Higienização', padrao: false });
    await pendente;
    assert.equal(fechada.form.elements.categoria.value, 'OUTRO');
});
