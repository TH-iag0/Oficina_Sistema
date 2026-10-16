import { escapar as e, rotulo } from './lib/ui.js';

export const situacoesOrdem = ['RASCUNHO', 'RECEBIDO', 'DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'APROVADO', 'EM_SERVICO', 'AGUARDANDO_PECA', 'FINALIZADO', 'ENTREGUE', 'CANCELADO'];
export function filtroOrdens(estado) {
    return estado.ordensLista ??= { busca: '', status: '', pagina: 0, versao: 0, resultado: null, erro: '' };
}

export function ordensDemo(dados, { busca = '', status = '', pagina = 0, tamanho = 20 } = {}) {
    const termo = busca.trim().toLocaleLowerCase('pt-BR').replaceAll('-', '');
    const encontrados = dados.ordens.filter(o => {
        const cliente = dados.clientes.find(c => c.id === o.clienteId), veiculo = dados.veiculos.find(v => v.id === o.veiculoId);
        const texto = [o.numero, cliente?.nome, veiculo?.placa, veiculo?.modelo, ...o.itens.map(i => i.descricao)].join(' ').toLocaleLowerCase('pt-BR').replaceAll('-', '');
        return (!status || o.status === status) && texto.includes(termo);
    });
    return { itens: encontrados.slice(pagina * tamanho, (pagina + 1) * tamanho), total: encontrados.length, totalPaginas: Math.ceil(encontrados.length / tamanho) };
}

export async function carregarOrdens({ estado, mensagemErro }) {
    if (estado.pagina !== 'operacao') return;
    const filtro = filtroOrdens(estado), versao = ++filtro.versao, api = estado.api, modo = estado.modo;
    filtro.erro = '';
    try {
        const parametros = { busca: filtro.busca, status: filtro.status || undefined, pagina: filtro.pagina, tamanho: 20 };
        const resultado = modo === 'demo' ? ordensDemo(estado.dados, parametros) : await api.ordens.listar(parametros);
        const referencias = modo === 'real' ? await carregarReferencias(api, resultado.itens, estado.dados) : [];
        if (versao !== filtro.versao || modo !== estado.modo || api !== estado.api) return;
        for (const [colecao, registro] of referencias) {
            if (!estado.dados[colecao].some(r => r.id === registro.id)) estado.dados[colecao].push(registro);
        }
        filtro.resultado = resultado; filtro.modo = modo;
    } catch (erro) { if (versao === filtro.versao && api === estado.api && modo === estado.modo) filtro.erro = mensagemErro(erro); }
}

async function carregarReferencias(api, ordens, dados) {
    const clientes = [...new Set(ordens.map(o => o.clienteId))].filter(id => !dados.clientes.some(c => c.id === id));
    const veiculos = [...new Set(ordens.map(o => o.veiculoId))].filter(id => !dados.veiculos.some(v => v.id === id));
    return Promise.all([...clientes.map(async id => ['clientes', await api.cliente(id)]),
        ...veiculos.map(async id => ['veiculos', await api.veiculos.buscar(id)])]);
}

export function pesquisaOrdens(estado) {
    const f = filtroOrdens(estado);
    return `<form class="orders-search" data-order-search><label>Pesquisar ordens<div class="search-row"><svg class="search-icon" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4.5 4.5"/></svg><input name="busca" maxlength="160" value="${e(f.busca)}" placeholder="Número da OS, cliente, placa ou serviço"></div></label>
        <label>Situação<select name="status"><option value="">Todas as situações</option>${situacoesOrdem.map(s => `<option value="${s}" ${f.status === s ? 'selected' : ''}>${e(rotulo(s))}</option>`).join('')}</select></label></form>`;
}

export function resultadoOrdens(estado) {
    const f = filtroOrdens(estado);
    return f.modo === estado.modo && f.resultado ? f.resultado : ordensDemo(estado.dados, f);
}

export function paginacaoOrdens(estado, resultado) {
    const f = filtroOrdens(estado);
    return `<div class="pagination"><button class="button small" data-order-page="${f.pagina - 1}" ${f.pagina < 1 ? 'disabled' : ''} aria-label="Página anterior" title="Página anterior"><span aria-hidden="true">‹</span></button><span>${resultado.total} ordens · página ${f.pagina + 1} de ${Math.max(1, resultado.totalPaginas)}</span><button class="button small" data-order-page="${f.pagina + 1}" ${f.pagina + 1 >= resultado.totalPaginas ? 'disabled' : ''} aria-label="Próxima página" title="Próxima página"><span aria-hidden="true">›</span></button></div>`;
}
