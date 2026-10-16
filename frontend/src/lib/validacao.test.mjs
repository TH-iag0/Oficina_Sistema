import test from 'node:test';
import assert from 'node:assert/strict';
import { dadosItemServico, valorMonetario } from './validacao.js';
import { dadosCadastro } from './cadastros.js';
import { dadosAbertura } from './ordem.js';
import { dadosConclusao } from './atendimento.js';

test('valores monetários aceitam zero e centavos sem arredondamento silencioso', () => {
    assert.equal(valorMonetario('0'), 0);
    assert.equal(valorMonetario(' 120.50 '), 120.5);
    assert.equal(valorMonetario(0), 0);
    for (const valor of ['', ' ', null, undefined, true, '1.001', '-1', 'Infinity', '1e3', '10000000000000']) {
        assert.throws(() => valorMonetario(valor));
    }
});

test('cadastro, abertura e conclusão rejeitam campos vazios e centavos inválidos', () => {
    for (const valor of [' ', null, '1.001']) {
        assert.throws(() => dadosCadastro('servico', { nome: 'Teste', categoria: 'OUTRO', preco: valor, custo: '0' }));
        assert.throws(() => dadosAbertura({ clienteId: 'c', veiculoId: 'v', situacaoInicial: 'RECEBIDO', incluirServico: true,
            servicoId: 's', quantidade: '1', preco: '10', custo: valor }));
        assert.throws(() => dadosConclusao({ relato: 'Conferido', custoReal: valor }));
    }
});

test('quilometragem vazia não se transforma em zero', () => {
    for (const quilometragem of ['', ' ', null, undefined]) {
        assert.throws(() => dadosCadastro('veiculo', { marca: 'VW', modelo: 'Golf', quilometragem }, true));
    }
    assert.equal(dadosCadastro('veiculo', { marca: 'VW', modelo: 'Golf', quilometragem: '0' }, true).quilometragem, 0);
});

test('itens usam o mesmo contrato na abertura e na inclusão de serviços', () => {
    const valores = { servicoId: 's', quantidade: '2', preco: '20.50', custo: '0', campoExtra: 'ignorado' };
    assert.deepEqual(dadosItemServico(valores), { servicoId: 's', quantidade: 2, preco: 20.5, custo: 0 });
    for (const quantidade of ['', '1.5', '-1', '2147483648']) {
        assert.throws(() => dadosItemServico({ ...valores, quantidade }));
    }
});
