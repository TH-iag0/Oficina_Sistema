import test from 'node:test';
import assert from 'node:assert/strict';
import { modoGrafico, salvarModoGrafico, graficoMarkup, selecionarGrafico } from '../charts.js';
import { axisMax, smoothPath, lineLabelLayout } from './chart-geometry.js';
import { demonstracao } from './demo.js';
import { estrutura } from '../views.js';

test('seleção por clique e teclado mantém indicação acessível e Escape limpa o destaque', () => {
    const classes = new Set(), atributos = new Map([['aria-label', 'Agosto: R$ 23.450,00']]);
    const legenda = { classList: { contains: nome => nome === 'chart-selection' }, textContent: '' };
    const container = { nextElementSibling: legenda, querySelectorAll: () => classes.has('is-selected') ? [alvo] : [] };
    const alvo = { closest: () => container,
        classList: { contains: nome => classes.has(nome), remove: nome => classes.delete(nome),
            toggle: (nome, ativo) => ativo ? classes.add(nome) : classes.delete(nome) },
        setAttribute: (nome, valor) => atributos.set(nome, valor), getAttribute: nome => atributos.get(nome) };
    const event = { type: 'click', target: { closest: () => alvo } };
    assert.equal(selecionarGrafico(event), true);
    assert.equal(atributos.get('aria-pressed'), 'true');
    assert.match(legenda.textContent, /Agosto/);
    selecionarGrafico(event);
    assert.equal(atributos.get('aria-pressed'), 'false');
    let prevenido = false;
    selecionarGrafico({ ...event, type: 'keydown', key: 'Enter', preventDefault() { prevenido = true; } });
    assert.equal(prevenido, true);
    assert.equal(atributos.get('aria-pressed'), 'true');
    selecionarGrafico({ ...event, type: 'keydown', key: 'Escape', preventDefault() {} });
    assert.equal(atributos.get('aria-pressed'), 'false');
});

test('etiquetas de máximo e mínimo deixam espaço para o valor mesmo no teto da escala', () => {
    for (const pointY of [52, 55.95, 115, 210]) {
        const layout = lineLabelLayout(pointY);
        assert.ok(layout.badgeTop >= 0, 'selo deve ficar dentro do gráfico');
        assert.ok(layout.badgeTop + layout.badgeHeight < layout.valueY - 11, 'selo não deve invadir o texto de 11px');
        assert.ok(layout.valueY < pointY - 5, 'valor deve ficar acima do marcador');
        assert.ok(layout.badgeY > layout.badgeTop && layout.badgeY < layout.badgeTop + layout.badgeHeight);
    }
});

test('preferência 2D persiste e armazenamento indisponível não bloqueia o painel', () => {
    const dados = new Map();
    const storage = { getItem: chave => dados.get(chave), setItem: (chave, valor) => dados.set(chave, valor) };
    assert.equal(modoGrafico(storage), '3d');
    salvarModoGrafico(storage, '2d');
    assert.equal(modoGrafico(storage), '2d');
    salvarModoGrafico(storage, 'invalido');
    assert.equal(modoGrafico(storage), '2d');
    assert.equal(modoGrafico({ getItem() { throw new Error('Bloqueado'); } }), '3d');
    assert.doesNotThrow(() => salvarModoGrafico({ setItem() { throw new Error('Bloqueado'); } }, '3d'));
});

test('rótulos do gráfico não podem injetar HTML e valores conservam precisão', () => {
    const html = graficoMarkup('bars', [{ label: '"><img src=x onerror=alert(1)>', records: 12.34 }]);
    assert.ok(!html.includes('<img'));
    assert.ok(html.includes('&lt;img'));
    assert.ok(html.includes('12.34'));
});

test('geometria ONVIO mantém escala e curva para série vazia, única e constante', () => {
    assert.equal(axisMax(0), 5);
    assert.ok(axisMax(48750) >= 48750);
    assert.equal(smoothPath([]), '');
    assert.equal(smoothPath([[10, 20]]), 'M 10 20');
    const curva = smoothPath([[0, 20], [30, 20], [60, 20]]);
    assert.ok(!curva.includes('NaN'));
    assert.ok(!curva.includes('Infinity'));
    assert.ok(curva.endsWith('60 20'));
});

test('modos 2D e 3D conservam dados e o gráfico de remaps usa ECU e TCU', () => {
    const dados = demonstracao('2026-10');
    const estado = { pagina: 'geral', mes: '2026-10', modo: 'demo', dados };
    const html2d = estrutura({ ...estado, graficoModo: '2d' });
    const html3d = estrutura({ ...estado, graficoModo: '3d' });
    const series = html => [...html.matchAll(/data-values="([^"]+)"/g)].map(m => m[1]);
    assert.deepEqual(series(html2d), series(html3d));
    assert.ok(html2d.includes('data-chart-mode="2d" aria-pressed="true"'));
    assert.ok(html3d.includes('data-chart-mode="3d" aria-pressed="true"'));
    const remaps = estrutura({ ...estado, pagina: 'tecnico' });
    assert.ok(remaps.includes('data-chart="weekly"'));
    assert.ok(remaps.includes('&quot;received&quot;'));
    assert.ok(remaps.includes('&quot;completed&quot;'));
});
