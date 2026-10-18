import test from 'node:test';
import assert from 'node:assert/strict';
import { botaoExcluir, executarExclusao, confirmarExclusao } from '../exclusao-cadastro.js';
import { criarApi } from '../services/api.js';

test('lixeira tem nome acessível, não submete formulários e escapa os dados', () => {
    const html = botaoExcluir('cliente', { id: '1', nome: '<img src=x>' });
    assert.ok(html.includes('type="button"'));
    assert.ok(html.includes('aria-label="Excluir &lt;img src=x&gt;"'));
    assert.ok(!html.includes('<img'));
    assert.ok(!html.includes('<mask'));
});

test('exclusão utiliza a rota definitiva correta e CSRF nos três cadastros', async () => {
    const chamadas = [];
    const api = criarApi({ fetchImpl: async (url, options) => {
        chamadas.push({ url, ...options });
        return url.endsWith('/csrf') ? { ok: true, status: 200, json: async () => ({ headerName: 'X-CSRF-TOKEN', token: 'teste' }) }
            : { ok: true, status: 204 };
    } });
    for (const tipo of ['cliente', 'veiculo', 'servico', 'funcionario']) await executarExclusao(api, tipo, 'abc');
    const exclusoes = chamadas.filter(c => c.method === 'DELETE');
    assert.deepEqual(exclusoes.map(c => new URL(c.url).pathname), ['/api/clientes/abc/exclusao', '/api/veiculos/abc/exclusao', '/api/servicos/abc/exclusao', '/api/funcionarios/abc/exclusao']);
    assert.ok(exclusoes.every(c => c.headers['X-CSRF-TOKEN'] === 'teste' && c.credentials === 'include'));
    await assert.rejects(executarExclusao(api, 'invalido', 'abc'));
});

test('conflito do banco é propagado sem simular exclusão bem sucedida', async () => {
    const erro = Object.assign(new Error('Vinculado'), { status: 409 });
    await assert.rejects(executarExclusao({ excluirCliente: async () => { throw erro; } }, 'cliente', '1'), e => e === erro);
});

function prepararConfirmacao(t, excluirCliente, modo = 'real') {
    const eventos = {}, confirmar = { isConnected: true, addEventListener: (tipo, fn) => { eventos.confirmar = fn; } };
    const cancelar = { addEventListener: (tipo, fn) => { eventos.cancelar = fn; } };
    const erro = { isConnected: true, textContent: '' };
    const elementos = { '[data-delete-confirm]': confirmar, '[data-delete-cancel]': cancelar, '[data-delete-error]': erro };
    let fechou = false, recarregou = false;
    const modal = { close() { fechou = true; }, querySelector: seletor => elementos[seletor] };
    const anterior = globalThis.document;
    globalThis.document = { querySelector: () => modal };
    t.after(() => { globalThis.document = anterior; });
    const estado = { modo, api: { excluirCliente }, dados: { clientes: [{ id: '1', nome: 'Cliente teste' }] }, catalogos: { cliente: { pagina: 2 } } };
    confirmarExclusao({ estado, abrirModal(titulo, html) { confirmar.disabled = html.includes('data-delete-confirm disabled'); },
        aviso() {}, carregar: async () => { recarregou = true; }, mensagemErro: e => e.message }, 'cliente', '1');
    return { eventos, confirmar, erro, estado, fechou: () => fechou, recarregou: () => recarregou };
}

test('confirmação impede duplo envio e atualiza a lista apenas depois do sucesso', async t => {
    let resolver, chamadas = 0;
    const ui = prepararConfirmacao(t, () => { chamadas++; return new Promise(resolve => { resolver = resolve; }); });
    assert.equal(chamadas, 0);
    const primeira = ui.eventos.confirmar();
    await ui.eventos.confirmar();
    assert.equal(chamadas, 1);
    assert.equal(ui.estado.dados.clientes.length, 1);
    resolver(); await primeira;
    assert.equal(ui.estado.dados.clientes.length, 0);
    assert.equal(ui.estado.catalogos.cliente.pagina, 0);
    assert.equal(ui.fechou(), true);
    assert.equal(ui.recarregou(), true);
});

test('cancelamento e demonstração não enviam exclusão', async t => {
    let chamadas = 0;
    const ui = prepararConfirmacao(t, async () => { chamadas++; }, 'demo');
    ui.eventos.cancelar();
    await ui.eventos.confirmar();
    assert.equal(chamadas, 0);
    assert.equal(ui.estado.dados.clientes.length, 1);
});

test('exclusão bloqueada mantém o registro e mostra a explicação', async t => {
    const ui = prepararConfirmacao(t, async () => { throw Object.assign(new Error('FK'), { status: 409 }); });
    await ui.eventos.confirmar();
    assert.equal(ui.estado.dados.clientes.length, 1);
    assert.equal(ui.fechou(), false);
    assert.equal(ui.confirmar.disabled, false);
    assert.match(ui.erro.textContent, /preservar os registros/);
});
