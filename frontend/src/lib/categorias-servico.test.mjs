import test from 'node:test';
import assert from 'node:assert/strict';
import { categoriasPadrao, dadosCategoriaServico, opcoesCategorias } from './categorias-servico.js';
import { camposCadastro, dadosCadastro } from './cadastros.js';

const id = '12345678-1234-1234-1234-123456789abc';

test('mantém as cinco categorias padrão e acrescenta categorias cadastradas', () => {
    const opcoes = opcoesCategorias([{ id: 'padrao', nome: 'Manutenção', padrao: true },
        { id, nome: 'Higienização', padrao: false }]);
    assert.deepEqual(opcoes.slice(0, 5), categoriasPadrao);
    assert.deepEqual(opcoes[5], [id, 'Higienização']);
    assert.equal(opcoes.length, 6);
});

test('categoria personalizada usa seu identificador no cadastro e na edição', () => {
    assert.deepEqual(dadosCategoriaServico(id), { categoria: 'OUTRO', categoriaId: id });
    const dados = dadosCadastro('servico', { nome: 'Limpeza', categoria: id, preco: '120', custo: '30' });
    assert.equal(dados.categoriaId, id);
    const html = camposCadastro('servico', { categoriaId: id }, [{ id, nome: 'Higienização', padrao: false }]);
    assert.ok(html.includes(`value="${id}" selected`));
});

test('rejeita identificador inválido e preserva categorias antigas', () => {
    assert.throws(() => dadosCategoriaServico('categoria-inexistente'));
    assert.deepEqual(dadosCategoriaServico('REMAP_ECU'), { categoria: 'REMAP_ECU' });
});

test('escapa nomes de categorias e oferece cadastro sem formulário aninhado', () => {
    const html = camposCadastro('servico', {}, [{ id, nome: '<img src=x>', padrao: false }]);
    assert.ok(!html.includes('<img'));
    assert.ok(html.includes('data-adicionar-categoria'));
    assert.ok(!html.includes('<form'));
});
