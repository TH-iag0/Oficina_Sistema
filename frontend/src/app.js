import { confirmarExclusao } from './exclusao-cadastro.js';
import { adiarPesquisa, redesenharPesquisa } from './lib/pesquisa.js';
import { carregarCatalogos, estadoCatalogo } from './catalogos.js';
import { abrirCadastro } from './cadastros.js';
import { abrirOrdem } from './ordem-form.js';
import { carregarOrdens, filtroOrdens } from './ordens-lista.js';
import { limparGraficos, montarGraficos, modoGrafico, salvarModoGrafico, selecionarGrafico } from './charts.js';
import { criarApi } from './services/api.js';
import { demonstracao } from './lib/demo.js';
import { escapar as e, intervaloMes, itens, confirmacaoPonto } from './lib/ui.js';
import { estrutura, telaPonto } from './views.js';
import { configurarJornada, encerrarJornada } from './jornada-form.js';
import { backendPonto } from './lib/ponto.js';
import { abrirAtendimento } from './atendimento.js';

const app = document.querySelector('#app');
const modal = document.querySelector('#modal');
const estado = {
    pagina: 'geral', modo: 'demo', graficoModo: modoGrafico(), mes: `${new Date().getFullYear()}-${String(new Date().getMonth() + 1).padStart(2, '0')}`,
    baseUrl: 'http://localhost:8080', dados: null, api: null, erro: null,
    resumo: null, jornadas: [], funcionarioId: null, carregando: false, financeiroMeses: 6
};
let versao = 0;
let avisoTimer;

function aviso(texto) {
    const notice = document.querySelector('#notice');
    notice.textContent = texto;
    notice.classList.add('show');
    clearTimeout(avisoTimer);
    avisoTimer = setTimeout(() => notice.classList.remove('show'), 6000);
}

function mensagemErro(erro) {
    if (erro.status === 401 || erro.status === 403) return 'A gestão está disponível somente no computador da oficina. Recarregue a página se o token expirou.';
    if (erro instanceof TypeError) return 'Não foi possível acessar o backend. Verifique se o servidor está ligado e se o endereço e o CORS estão corretos.';
    const campos = erro.problema?.erros ?? erro.problema?.errors;
    return erro.message + (campos ? ' ' + JSON.stringify(campos) : '');
}

function desenhar() {
    limparGraficos();
    app.innerHTML = estrutura(estado);
    montarGraficos(app, estado.graficoModo);
}

async function carregar() {
    const atual = ++versao;
    estado.erro = null;
    if (estado.modo === 'demo') {
        estado.dados = demonstracao(estado.mes, estado.pagina === 'financeiro' ? estado.financeiroMeses : 6);
        await carregarCatalogos(contextoCadastro());
        await carregarOrdens(contextoCadastro());
        estado.carregando = false;
        estado.resumo = estado.dados.resumo;
        estado.funcionarioId = estado.dados.funcionarios[0]?.id;
        estado.jornadas = estado.dados.jornadas;
        desenhar();
        return;
    }
    estado.carregando = true;
    desenhar();
    try {
        const api = estado.api;
        const meses = estado.pagina === 'financeiro' ? estado.financeiroMeses : 6;
        const [dashboard, indicadores, clientes, veiculos, servicos, ordens, funcionarios, despesas, banco, analiseFinanceira] = await Promise.all([
            api.dashboard(estado.mes), api.indicadores({ mes: estado.mes, meses }),
            api.clientes({ tamanho: 100 }), api.veiculos.listar({ tamanho: 100 }),
            api.servicos.listar({ tamanho: 100 }), api.ordens.listar({ tamanho: 100 }),
            api.funcionarios.listar({ tamanho: 100 }), api.financeiro.despesas({ tamanho: 100 }), api.financeiro.extrato(estado.mes),
            estado.pagina === 'financeiro' ? api.financeiro.analises({ mes: estado.mes, meses }) : null
        ]);
        if (atual !== versao) return;
        estado.dados = { dashboard, indicadores, banco, analiseFinanceira, clientes: itens(clientes), veiculos: itens(veiculos),
            servicos: itens(servicos), ordens: itens(ordens), funcionarios: itens(funcionarios), despesas: itens(despesas) };
        await carregarCatalogos(contextoCadastro());
        await carregarOrdens(contextoCadastro());
        estado.resumo = null;
        if (estado.pagina === 'ponto') await carregarPonto(atual);
    } catch (erro) {
        if (atual === versao) estado.erro = mensagemErro(erro);
    } finally {
        if (atual === versao) { estado.carregando = false; desenhar(); }
    }
}

async function carregarPonto(atual = versao) {
    if (estado.modo !== 'real') return;
    if (!estado.dados.funcionarios.some(f => f.id === estado.funcionarioId)) estado.funcionarioId = estado.dados.funcionarios[0]?.id;
    if (!estado.funcionarioId) return;
    const funcionarioId = estado.funcionarioId;
    const [resumo, jornadas] = await Promise.all([
        estado.api.resumoPonto({ funcionarioId, ...intervaloMes(estado.mes) }),
        estado.api.funcionarios.jornadas(funcionarioId)
    ]);
    if (atual === versao && estado.funcionarioId === funcionarioId) { estado.resumo = resumo; estado.jornadas = jornadas; }
}

function abrirModal(titulo, corpo) {
    modal.innerHTML = `<div class="modal-heading"><h2>${e(titulo)}</h2><button class="icon-button" data-close aria-label="Fechar">×</button></div>${corpo}`;
    modal.showModal();
}
modal.addEventListener('close', () => { if (!modal.open) modal.innerHTML = ''; });
modal.addEventListener('click', event => { if (event.target.closest('[data-close]')) modal.close(); });

async function conectar() {
    estado.api = criarApi({ baseUrl: estado.baseUrl });
    estado.modo = 'real';
    await carregar();
}

const campo = (nome, titulo, tipo = 'text', extra = '') => `<label>${titulo}<input name="${nome}" type="${tipo}" ${extra}></label>`;
function configurarFormularios() {
    return {
        funcionario: { titulo: 'Novo funcionário', campos: campo('nome', 'Nome', 'text', 'required maxlength="120"') + campo('matricula', 'Matrícula', 'text', 'required maxlength="32"') + campo('pin', 'PIN individual', 'password', 'required inputmode="numeric" pattern="[0-9]{4,12}" minlength="4" maxlength="12" autocomplete="off"'), enviar: dados => estado.api.funcionarios.cadastrar(dados) },
        despesa: { titulo: 'Nova despesa', campos: campo('descricao', 'Descrição', 'text', 'required maxlength="160"') + campo('categoria', 'Categoria', 'text', 'required maxlength="80"') + campo('valor', 'Valor (R$)', 'number', 'required min="0.01" step="0.01"') + campo('competencia', 'Competência', 'date', 'required') + campo('vencimento', 'Vencimento', 'date', 'required'), enviar: dados => estado.api.financeiro.criarDespesa({ ...dados, valor: Number(dados.valor) }) }
    };
}

function contextoCadastro() {
    return { estado, abrirModal, aviso, mensagemErro, carregar };
}

function formulario(tipo) {
    if (['cliente', 'veiculo', 'servico'].includes(tipo)) return abrirCadastro(contextoCadastro(), tipo);
    if (tipo === 'ordem') return abrirOrdem(contextoCadastro());
    if (estado.modo !== 'real') { aviso('Conecte o backend para salvar registros reais. Os dados demonstrativos são somente leitura.'); conectar(); return; }
    const config = configurarFormularios()[tipo];
    if (!config) return;
    abrirModal(config.titulo, `<form id="record-form">${config.campos}<p class="form-error" role="alert"></p><button class="button primary wide">Salvar</button></form>`);
    const form = modal.querySelector('form');
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const botao = form.querySelector('button');
        botao.disabled = true;
        form.querySelector('.form-error').textContent = '';
        try {
            await config.enviar(Object.fromEntries(new FormData(form)));
            modal.close();
            aviso('Registro salvo no backend.');
            await carregar();
        } catch (erro) { form.querySelector('.form-error').textContent = mensagemErro(erro); }
        finally { botao.disabled = false; }
    });
}

app.addEventListener('keydown', selecionarGrafico);
app.addEventListener('click', async event => {
    if (selecionarGrafico(event)) return;
    const button = event.target.closest('button');
    if (!button) return;
    if (button.dataset.chartMode) {
        estado.graficoModo = button.dataset.chartMode;
        salvarModoGrafico(undefined, estado.graficoModo);
        desenhar();
        app.querySelector('[data-chart-mode="' + estado.graficoModo + '"]')?.focus();
        return;
    }
    if (button.dataset.page) {
        estado.pagina = button.dataset.page;
        if (estado.pagina === 'financeiro') { await carregar(); return; }
        if (estado.pagina === 'ponto' && estado.modo === 'real' && !estado.erro) {
            try { await carregarPonto(); } catch (erro) { estado.erro = mensagemErro(erro); }
        }
        await carregarCatalogos(contextoCadastro());
        await carregarOrdens(contextoCadastro());
        desenhar();
    }
    if (button.hasAttribute('data-connect')) conectar();
    if (button.hasAttribute('data-demo')) {
        estado.modo = 'demo';
        estado.api = null;
        await carregar();
    }
    if (button.hasAttribute('data-bank-sync') && estado.modo === 'real') {
        button.disabled = true;
        try {
            await estado.api.financeiro.sincronizarExtrato(estado.mes);
            await carregar();
        } catch (erro) { aviso(mensagemErro(erro)); }
        finally { button.disabled = false; }
    }
    if (button.dataset.excluirCadastro) confirmarExclusao(contextoCadastro(), button.dataset.excluirCadastro, button.dataset.registroId);
    if (button.dataset.editarCadastro) {
        await abrirCadastro(contextoCadastro(), button.dataset.editarCadastro, { id: button.dataset.registroId });
    }
    if (button.hasAttribute('data-order-page')) {
        filtroOrdens(estado).pagina = Math.max(0, Number(button.dataset.orderPage));
        await carregarOrdens(contextoCadastro()); desenhar();
    }
    if (button.dataset.cadastroPage) {
        estadoCatalogo(estado, button.dataset.cadastroPage).pagina = Math.max(0, Number(button.dataset.indice));
        await carregarCatalogos(contextoCadastro()); desenhar();
    }
    if (button.hasAttribute('data-refresh')) carregar();
    if (button.dataset.form && !estado.erro) formulario(button.dataset.form);
    const contextoJornada = { estado, abrirModal, carregar, conectar, aviso, mensagemErro };
    if (button.dataset.jornada) configurarJornada(contextoJornada, button.dataset.jornada);
    if (button.dataset.encerrarJornada) encerrarJornada(contextoJornada, button.dataset.encerrarJornada);
    if (button.dataset.atendimento) abrirAtendimento(contextoJornada, button.dataset.atendimento, { somenteLeitura: estado.pagina === 'geral' });
});
const pesquisarAoDigitar = adiarPesquisa((form, pagina) => {
    if (estado.pagina !== pagina) return;
    const atual = form.isConnected ? form : app.querySelector(form.hasAttribute('data-order-search')
        ? '[data-order-search]' : `[data-cadastro-search="${form.dataset.cadastroSearch}"]`);
    atual?.requestSubmit();
});
app.addEventListener('input', event => {
    if (event.isComposing || !event.target.matches('input[name=busca]')) return;
    const form = event.target.closest('[data-cadastro-search], [data-order-search]');
    if (form) pesquisarAoDigitar(form, estado.pagina);
});
app.addEventListener('compositionend', event => {
    const form = event.target.closest('[data-cadastro-search], [data-order-search]');
    if (form) pesquisarAoDigitar(form, estado.pagina);
});
app.addEventListener('submit', async event => {
    if (event.target.hasAttribute('data-order-search')) {
        event.preventDefault();
        pesquisarAoDigitar.cancelar();
        const valores = Object.fromEntries(new FormData(event.target));
        Object.assign(filtroOrdens(estado), { ...valores, busca: valores.busca.trim(), pagina: 0 });
        await carregarOrdens(contextoCadastro()); redesenharPesquisa(app, desenhar); return;
    }
    const tipo = event.target.dataset.cadastroSearch;
    if (!tipo) return;
    event.preventDefault();
    pesquisarAoDigitar.cancelar();
    const lista = estadoCatalogo(estado, tipo);
    lista.busca = new FormData(event.target).get('busca').trim();
    lista.pagina = 0;
    await carregarCatalogos(contextoCadastro()); redesenharPesquisa(app, desenhar);
});

app.addEventListener('change', async event => {
    if (event.target.matches('[data-order-search] select')) event.target.closest('form').requestSubmit();
    if (event.target.matches('[data-finance-months]')) {
        const meses = Number(event.target.value);
        if (![3, 6, 12].includes(meses)) return;
        estado.financeiroMeses = meses;
        await carregar();
    }
    if (event.target.id === 'month') { estado.mes = event.target.value || estado.mes; carregar(); }
    if (event.target.id === 'employee') {
        estado.funcionarioId = event.target.value;
        estado.resumo = null;
        estado.jornadas = [];
        desenhar();
        try { await carregarPonto(); desenhar(); } catch (erro) { aviso(mensagemErro(erro)); }
    }
});

function iniciarPonto() {
    app.innerHTML = telaPonto();
    const form = document.querySelector('#point-form');
    const resultado = document.querySelector('#point-result');
    const confirmar = confirmacaoPonto(dados => criarApi({ baseUrl: backendPonto(location.search) }).registrarPonto(dados));
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const botao = form.querySelector('button');
        botao.disabled = true;
        resultado.textContent = '';
        try {
            const resposta = await confirmar(Object.fromEntries(new FormData(form)));
            resultado.textContent = `Marcação confirmada pelo servidor.${resposta.instante ? ' ' + new Date(resposta.instante).toLocaleString('pt-BR', { timeZone: 'America/Sao_Paulo' }) : ''}`;
            form.reset();
        } catch (erro) {
            resultado.textContent = (erro.status === 401 ? 'Matrícula ou PIN inválidos.' : mensagemErro(erro))
                + (erro.status ? '' : ' Em caso de dúvida, repita a mesma marcação; a confirmação usa o mesmo identificador.');
        } finally { botao.disabled = false; }
    });
}

if (location.pathname === '/ponto') iniciarPonto();
else conectar();
