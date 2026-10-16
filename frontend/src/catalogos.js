import { botaoExcluir } from './exclusao-cadastro.js';
import { escapar as e, moeda, numero, rotulo } from './lib/ui.js';
import { colecoes, guardarCadastro, paginaDemo, pesquisarCadastro } from './lib/cadastros.js';

export function estadoCatalogo(estado, tipo) {
    estado.catalogos ??= {};
    return estado.catalogos[tipo] ??= { busca: '', pagina: 0, versao: 0, resultado: null };
}

export async function carregarCatalogos(contexto) {
    const tipos = contexto.estado.pagina === 'clientes' ? ['cliente', 'veiculo'] : contexto.estado.pagina === 'servicos' ? ['servico'] : [];
    await Promise.all(tipos.map(tipo => carregarCatalogo(contexto, tipo)));
}

async function carregarCatalogo({ estado, mensagemErro }, tipo) {
    const lista = estadoCatalogo(estado, tipo), versao = ++lista.versao;
    const api = estado.api, modo = estado.modo;
    lista.erro = null;
    try {
        const parametros = { busca: lista.busca, pagina: lista.pagina, tamanho: 20 };
        const resultado = modo === 'demo' ? paginaDemo(estado.dados, tipo, parametros)
            : await pesquisarCadastro(api, tipo, parametros);
        if (versao !== lista.versao || api !== estado.api || modo !== estado.modo) return;
        if (modo === 'real') resultado.itens.forEach(item => guardarCadastro(estado.dados, tipo, item));
        lista.resultado = resultado;
        lista.modo = modo;
    } catch (erro) { if (versao === lista.versao) lista.erro = mensagemErro(erro); }
}

export function listaCadastro(tipo, estado) {
    const lista = estadoCatalogo(estado, tipo);
    const pagina = lista.modo === estado.modo && lista.resultado ? lista.resultado
        : paginaDemo(estado.dados, tipo, { busca: lista.busca, pagina: lista.pagina });
    const titulo = { cliente: 'clientes', veiculo: 'veículos', servico: 'serviços' }[tipo];
    const dica = tipo === 'veiculo' ? 'Placa, marca ou modelo' : 'Nome';
    return `<form class="catalog-search" data-cadastro-search="${tipo}"><label>Pesquisar ${titulo}<div class="search-row"><svg class="search-icon" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4.5 4.5"/></svg><input name="busca" value="${e(lista.busca)}" maxlength="160" placeholder="${dica}"></div></label></form>
        ${lista.erro ? `<p class="form-error" role="alert">${e(lista.erro)}</p>` : tabela(tipo, pagina.itens, estado.dados)}
        <div class="pagination"><button type="button" class="button small" data-cadastro-page="${tipo}" data-indice="${lista.pagina - 1}" ${lista.pagina === 0 ? 'disabled' : ''} aria-label="Página anterior" title="Página anterior"><span aria-hidden="true">‹</span></button><span>${pagina.total} registros · página ${lista.pagina + 1} de ${Math.max(1, pagina.totalPaginas)}</span><button type="button" class="button small" data-cadastro-page="${tipo}" data-indice="${lista.pagina + 1}" ${lista.pagina + 1 >= pagina.totalPaginas ? 'disabled' : ''} aria-label="Próxima página" title="Próxima página"><span aria-hidden="true">›</span></button></div>`;
}

function tabela(tipo, itens, dados) {
    if (!itens.length) return '<p class="empty">Nenhum cadastro encontrado. Altere a pesquisa ou crie um novo cadastro.</p>';
    const colunas = { cliente: ['Nome', 'Tipo', 'Telefone', 'Ações'], veiculo: ['Veículo', 'Placa', 'Quilometragem', 'Cliente', 'Ações'], servico: ['Serviço', 'Categoria', 'Preço', 'Custo estimado', 'Situação', 'Ações'] }[tipo];
    return `<div class="table-scroll"><table><thead><tr>${colunas.map(c => `<th>${c}</th>`).join('')}</tr></thead><tbody>${itens.map(item => `<tr>${celulas(tipo, item, dados).map(c => `<td>${c}</td>`).join('')}<td><button type="button" class="text-button" data-editar-cadastro="${tipo}" data-registro-id="${e(item.id)}" aria-label="Editar ${e(item.nome || item.placa)}">Editar</button>${botaoExcluir(tipo, item)}</td></tr>`).join('')}</tbody></table></div>`;
}

function celulas(tipo, item, dados) {
    if (tipo === 'cliente') return [`<strong>${e(item.nome)}</strong>`, e(item.tipo), e(item.telefone || '—')];
    if (tipo === 'veiculo') return [`<strong>${e(item.marca)} ${e(item.modelo)}</strong>`, e(item.placa), `${numero(item.quilometragem)} km`, e(dados.clientes.find(c => c.id === item.clienteId)?.nome || 'Cliente vinculado')];
    return [`<strong>${e(item.nome)}</strong>`, e(item.categoriaNome || rotulo(item.categoria)), moeda(item.preco), moeda(item.custo), item.ativo ? 'Ativo' : 'Inativo'];
}
