import test from 'node:test';
import assert from 'node:assert/strict';
import { abrirRegistro } from '../registro-form.js';

test('novo funcionário abre na demonstração e não envia dados mesmo com submit manual', async () => {
    let html, submit, envios = 0, recargas = 0, foco = false;
    const botao = { disabled: false }, erro = { textContent: '' };
    const form = { isConnected: true, querySelector: seletor => seletor === '[type=submit]' ? botao
        : seletor === 'input' ? { focus() { foco = true; } } : erro,
        addEventListener: (evento, callback) => { submit = callback; } };
    const estado = { modo: 'demo', api: null };
    const config = { titulo: 'Novo funcionário', campos: '<input name="nome"><input name="matricula"><input name="pin">',
        enviar: async () => { envios++; } };
    abrirRegistro({ estado, abrirModal: (titulo, corpo) => { assert.equal(titulo, 'Novo funcionário'); html = corpo; },
        aviso() {}, mensagemErro: e => e.message, carregar: async () => { recargas++; } }, config, { querySelector: () => form });
    assert.ok(html.includes('name="matricula"'));
    assert.ok(html.includes('name="pin"'));
    assert.ok(html.includes('disabled>Salvar'));
    assert.ok(html.includes('salvamento está desativado'));
    assert.equal(foco, true);
    await submit({ preventDefault() {} });
    estado.modo = 'real'; estado.api = {};
    await submit({ preventDefault() {} });
    assert.equal(envios, 0);
    assert.equal(recargas, 0);
});
