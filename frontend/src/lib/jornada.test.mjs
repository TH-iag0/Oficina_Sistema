import test from 'node:test';
import assert from 'node:assert/strict';
import { minutosPrevistos, jornadaEntrada, camposJornada } from './jornada.js';
import { linkPonto, backendPonto } from './ponto.js';
import { estrutura, telaPonto } from '../views.js';
import { demonstracao } from './demo.js';

const jornada = { dia: 'MONDAY', entrada: '08:00', saida: '17:00', intervaloMinutos: '60', toleranciaMinutos: '5', vigenciaInicio: '2026-10-01', vigenciaFim: '' };

test('jornada calcula carga descontando intervalo e suporta saída no dia seguinte', () => {
    assert.equal(minutosPrevistos('08:00', '17:00', 60), 480);
    assert.equal(minutosPrevistos('22:00', '06:00', 60), 420);
    assert.throws(() => minutosPrevistos('08:00', '08:00', 0));
    assert.throws(() => minutosPrevistos('08:00', '09:00', 60));
    assert.throws(() => minutosPrevistos('25:00', '17:00', 0));
});

test('formulário envia dias e tipos numéricos esperados pelo Spring Boot', () => {
    assert.deepEqual(jornadaEntrada(jornada), { ...jornada, intervaloMinutos: 60, toleranciaMinutos: 5, vigenciaFim: null });
    assert.ok(camposJornada('2026-10-01').includes('value="FRIDAY"'));
    assert.ok(camposJornada('2026-10-01').includes('name="entrada"'));
});

test('valida vigência, precisão de minutos e tolerância antes de enviar', () => {
    for (const invalida of [
        { dia: 'INVALIDO' }, { vigenciaInicio: '2026-02-30' }, { vigenciaFim: '2026-09-30' },
        { toleranciaMinutos: '600' }, { toleranciaMinutos: '' }, { intervaloMinutos: '1.5' }, { entrada: '08:00:01' }
    ]) assert.throws(() => jornadaEntrada({ ...jornada, ...invalida }));
});

test('link público leva somente a origem da API, sem credenciais do proprietário', () => {
    const link = linkPonto('https://ponto.oficina.example');
    assert.equal(backendPonto(new URL(link, 'http://localhost').search), 'https://ponto.oficina.example');
    assert.equal(backendPonto(''), 'http://localhost:8080');
    for (const url of ['https://admin:senha@oficina.example', 'javascript:alert(1)', 'https://oficina.example/api', 'https://oficina.example/?senha=x']) {
        assert.throws(() => linkPonto(url));
    }
});

test('admin tem jornada por dia e funcionário recebe apenas a tela de marcação', () => {
    const dados = demonstracao('2026-10');
    const html = estrutura({ pagina: 'ponto', modo: 'demo', mes: '2026-10', dados, jornadas: dados.jornadas, funcionarioId: 'f1', resumo: dados.resumo });
    assert.ok(html.includes('Configurar horários'));
    assert.ok(html.includes('Segunda-feira'));
    assert.ok(html.includes('08:00'));
    const publico = telaPonto();
    for (const privado of ['href="/"', 'point-api', 'point-demo', 'Configurar horários', 'Resumo de ponto', 'Endereço do backend']) assert.ok(!publico.includes(privado));
    assert.ok(publico.includes('Confirmar marcação'));
});
