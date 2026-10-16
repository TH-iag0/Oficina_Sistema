import test from 'node:test';
import assert from 'node:assert/strict';
import { camposCadastro, dadosCadastro, guardarCadastro, paginaDemo, pesquisarCadastro } from './cadastros.js';
import { carregarCatalogos, estadoCatalogo, listaCadastro } from '../catalogos.js';

test('edição de veículo mantém placa e vínculo com o cliente fora do payload', () => {
    const dados = dadosCadastro('veiculo', { marca: ' VW ', modelo: ' Golf ', quilometragem: '123456', placa: 'OUT0000', clienteId: 'outro' }, true);
    assert.deepEqual(dados, { marca: 'VW', modelo: 'Golf', quilometragem: 123456 });
    assert.throws(() => dadosCadastro('veiculo', { quilometragem: '1.5' }, true));
    const html = camposCadastro('veiculo', { id: 'v1', placa: 'ABC1234', marca: 'VW', modelo: 'Golf', quilometragem: 100 });
    assert.ok(!html.includes('name="placa"'));
    assert.ok(html.includes('min="100"'));
});

test('edição normaliza opcionais e aceita preço e custo zero', () => {
    assert.deepEqual(dadosCadastro('cliente', { tipo: 'PF', nome: ' Maria ', documento: '', telefone: ' 11999999999 ' }),
        { tipo: 'PF', nome: 'Maria', documento: null, telefone: '11999999999' });
    assert.deepEqual(dadosCadastro('servico', { nome: ' Teste ', categoria: 'OUTRO', preco: '0', custo: '0' }),
        { nome: 'Teste', categoria: 'OUTRO', preco: 0, custo: 0 });
    assert.throws(() => dadosCadastro('servico', { preco: '', custo: '10' }));
});

test('paginação encontra registros além dos primeiros cem e restringe veículos ao cliente', () => {
    const dados = { clientes: Array.from({ length: 105 }, (_, i) => ({ id: String(i), nome: `Cliente ${i}`, ativo: true })),
        veiculos: [{ id: 'v1', placa: 'ABC1234', clienteId: '1', ativo: true }, { id: 'v2', placa: 'DEF1234', clienteId: '2', ativo: true }] };
    assert.equal(paginaDemo(dados, 'cliente', { pagina: 5 }).itens.length, 5);
    assert.equal(paginaDemo(dados, 'cliente', { busca: 'Cliente 104' }).itens[0].id, '104');
    assert.deepEqual(paginaDemo(dados, 'veiculo', { clienteId: '1' }).itens.map(v => v.id), ['v1']);
    guardarCadastro(dados, 'cliente', { id: '104', nome: 'Corrigido' });
    assert.equal(dados.clientes.length, 105);
    assert.equal(dados.clientes.find(c => c.id === '104').nome, 'Corrigido');
});

test('pesquisa usa paginação e filtros do servidor nos três cadastros', async () => {
    const chamadas = [];
    const api = { clientes: p => chamadas.push(p), veiculos: { listar: p => chamadas.push(p) }, servicos: { listar: p => chamadas.push(p) } };
    pesquisarCadastro(api, 'cliente', { busca: 'Maria', pagina: 4, tamanho: 10 });
    pesquisarCadastro(api, 'veiculo', { busca: 'ABC', clienteId: 'cliente', pagina: 2, tamanho: 10 });
    pesquisarCadastro(api, 'servico', { busca: 'Óleo', pagina: 7, ativo: true });
    assert.equal(chamadas[0].nome, 'Maria');
    assert.equal(chamadas[1].clienteId, 'cliente');
    assert.equal(chamadas[2].pagina, 7);
});

test('resposta atrasada de uma pesquisa não substitui os resultados mais recentes', async () => {
    const pendentes = [];
    const estado = { pagina: 'servicos', modo: 'real', dados: { servicos: [] }, api: {
        servicos: { listar: () => new Promise(resolve => pendentes.push(resolve)) }
    } };
    const contexto = { estado, mensagemErro: e => e.message };
    const antiga = carregarCatalogos(contexto);
    estadoCatalogo(estado, 'servico').busca = 'Nova';
    const nova = carregarCatalogos(contexto);
    pendentes[1]({ itens: [{ id: 'novo', nome: 'Nova' }], total: 1, totalPaginas: 1 });
    await nova;
    pendentes[0]({ itens: [{ id: 'antigo', nome: 'Antiga' }], total: 1, totalPaginas: 1 });
    await antiga;
    assert.equal(estado.catalogos.servico.resultado.itens[0].id, 'novo');
    assert.equal(estado.dados.servicos.length, 1);
});

test('nomes de cadastros são escapados nos formulários e tabelas editáveis', () => {
    const item = { id: '1', nome: '<img src=x onerror=alert(1)>', tipo: 'PF' };
    assert.ok(!camposCadastro('cliente', item).includes('<img'));
    const html = listaCadastro('cliente', { modo: 'demo', dados: { clientes: [item] } });
    assert.ok(!html.includes('<img'));
    assert.ok(html.includes('data-editar-cadastro="cliente"'));
});
