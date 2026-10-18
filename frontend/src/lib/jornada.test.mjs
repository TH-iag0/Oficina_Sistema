import test from 'node:test';
import assert from 'node:assert/strict';
import { minutosPrevistos, jornadaEntrada, camposJornada, semanaEntrada, camposSemana } from './jornada.js';
import { linkPonto, backendPonto } from './ponto.js';
import { estrutura, telaPonto } from '../views.js';
import { demonstracao } from './demo.js';

const jornada = { dia: 'MONDAY', entrada: '08:00', saida: '17:00', intervaloMinutos: '60', toleranciaMinutos: '5', vigenciaInicio: '2026-10-01', vigenciaFim: '' };

function renderizarResumo(dias) {
    const dados = demonstracao('2026-10');
    return estrutura({ pagina: 'ponto', modo: 'demo', mes: '2026-10', dados,
        resumo: { ...dados.resumo, dias } }).split('Registros por semana</h3>')[1];
}

test('pastas incluem sete dias corridos, mesmo sem marcações, e exibem dias da semana', () => {
    const dias = Array.from({ length: 14 }, (_, i) => ({ data: '2026-10-' + String(i + 1).padStart(2, '0'),
        minutosTrabalhados: 0, minutosPrevistos: 0, minutosAtraso: 0, incompleto: false }));
    const html = renderizarResumo(dias);
    assert.equal((html.match(/class="point-month-details point-week-folder"/g) || []).length, 2);
    assert.equal((html.match(/<span>7 dias<\/span>/g) || []).length, 2);
    assert.ok(html.includes('01/10/2026 a 07/10/2026'));
    assert.ok(html.includes('08/10/2026 a 14/10/2026'));
    for (const dia of ['segunda-feira', 'terça-feira', 'quarta-feira', 'quinta-feira', 'sexta-feira', 'sábado', 'domingo']) assert.ok(html.includes(dia));
    assert.equal((html.match(/Sem marcações/g) || []).length, 14);
    assert.equal((html.match(/point-week-folder" open/g) || []).length, 1);
    assert.ok(!html.includes('Finalizado'));
});

test('pastas preservam todos os registros e pendências sem alterar a ordem original', () => {
    const dias = Array.from({ length: 10 }, (_, i) => ({ data: '2026-10-' + String(i + 1).padStart(2, '0'),
        minutosTrabalhados: 480, minutosPrevistos: 480, minutosAtraso: 0, incompleto: i === 9 }));
    const html = renderizarResumo(dias);
    assert.ok(html.includes('<span>3 dias</span>'));
    assert.ok(html.includes('<span>7 dias</span>'));
    assert.ok(html.includes('Incompleto'));
    assert.ok(html.includes('01/10/2026'));
    assert.equal(dias[0].data, '2026-10-01');
});

test('opção Sim exige ponto no almoço e Não dispensa as marcações em todos os dias', () => {
    const semana = camposSemana('2026-10-01');
    assert.equal((semana.match(/Exigir ponto no almoço\?/g) || []).length, 7);
    for (const html of [semana, camposJornada('2026-10-01')]) {
        assert.ok(html.includes('<option value="true">Não</option><option value="false">Sim</option>'));
        assert.ok(!html.includes('Desconto automático'));
        assert.ok(!html.includes('Pelas marcações'));
    }
    assert.equal(jornadaEntrada({ ...jornada, intervaloAutomatico: 'true' }).intervaloAutomatico, true);
    assert.equal(jornadaEntrada({ ...jornada, intervaloAutomatico: 'false' }).intervaloAutomatico, false);
});

test('semana envia dias diferentes juntos com almoço e horários independentes', () => {
    const valores = { vigenciaInicio: '2026-10-01', encerrarAnteriores: 'on' };
    for (const dia of ['MONDAY', 'SATURDAY']) for (const nome of ['entrada', 'saida', 'intervaloMinutos', 'toleranciaMinutos']) valores[`${dia}.${nome}`] = jornada[nome];
    valores['SATURDAY.saida'] = '12:00'; valores['SATURDAY.intervaloMinutos'] = '0';
    const dados = semanaEntrada(valores, ['MONDAY', 'SATURDAY']);
    assert.equal(dados.jornadas.length, 2);
    assert.equal(dados.jornadas[1].saida, '12:00');
    assert.equal(dados.jornadas[1].intervaloMinutos, 0);
    assert.equal(dados.encerrarAnteriores, true);
    assert.throws(() => semanaEntrada(valores, []));
    assert.throws(() => semanaEntrada(valores, ['MONDAY', 'MONDAY']));
    assert.equal((camposSemana('2026-10-01').match(/name="dias"/g) || []).length, 7);
    assert.ok(camposSemana('2026-10-01').includes('data-copy-hours'));
});

test('almoço é configurável e o modo automático não exige marcação intermediária', () => {
    assert.equal(jornadaEntrada({ ...jornada, intervaloAutomatico: 'true' }).intervaloAutomatico, true);
    assert.equal(jornadaEntrada({ ...jornada, intervaloAutomatico: 'false' }).intervaloAutomatico, false);
    assert.equal(jornadaEntrada({ ...jornada, intervaloMinutos: '0' }).intervaloMinutos, 0);
    const html = camposJornada('2026-10-16');
    assert.ok(html.includes('Intervalo de almoço (minutos)'));
    assert.ok(html.includes('Exigir ponto no almoço?'));
    assert.ok(html.includes('sem desconto em dobro'));
});

test('jornada calcula carga descontando intervalo e suporta saída no dia seguinte', () => {
    assert.equal(minutosPrevistos('08:00', '17:00', 60), 480);
    assert.equal(minutosPrevistos('22:00', '06:00', 60), 420);
    assert.throws(() => minutosPrevistos('08:00', '08:00', 0));
    assert.throws(() => minutosPrevistos('08:00', '09:00', 60));
    assert.throws(() => minutosPrevistos('25:00', '17:00', 0));
});

test('formulário envia dias e tipos numéricos esperados pelo Spring Boot', () => {
    assert.deepEqual(jornadaEntrada(jornada), { ...jornada, intervaloMinutos: 60, toleranciaMinutos: 5, vigenciaFim: null, intervaloAutomatico: true });
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
    assert.ok(html.includes('data-excluir-cadastro="funcionario"'));
    assert.ok(html.includes('aria-label="Excluir Felipe Martins"'));
    assert.ok(html.includes('Segunda-feira'));
    assert.ok(html.includes('08:00'));
    const publico = telaPonto();
    for (const privado of ['href="/"', 'point-api', 'point-demo', 'Configurar horários', 'Resumo de ponto', 'Endereço do backend']) assert.ok(!publico.includes(privado));
    assert.ok(publico.includes('Confirmar marcação'));
});
