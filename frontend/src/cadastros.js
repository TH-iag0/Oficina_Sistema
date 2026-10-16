import { camposCadastro, dadosCadastro, nomes, colecoes, guardarCadastro } from './lib/cadastros.js';
import { montarSeletor } from './seletor-cadastro.js';
import { montarCategoriasServico } from './categorias-servico.js';

export async function abrirCadastro(contexto, tipo, { id, clienteId = '', aoSalvar, aoCancelar } = {}) {
    const { estado, abrirModal, mensagemErro, aviso } = contexto;
    const api = estado.api, modo = estado.modo;
    try {
        const [item, categorias] = await Promise.all([
            id ? buscar(contexto, tipo, id) : { clienteId },
            tipo === 'servico' && modo === 'real' ? api.servicos.categorias() : []
        ]);
        if (estado.api !== api || estado.modo !== modo) return;
        abrirModal(`${id ? 'Editar' : 'Novo'} ${nomes[tipo]}`, `<form id="cadastro-form">${camposCadastro(tipo, item, categorias)}<p class="form-error" role="alert"></p><button class="button primary wide" type="submit" ${estado.modo === 'demo' ? 'disabled' : ''}>Salvar ${nomes[tipo]}</button></form>${estado.modo === 'demo' ? '<p class="modal-description">Demonstração: você pode preencher os campos. O salvamento está desativado.</p>' : ''}<button class="text-button" type="button" data-cadastro-cancelar>${aoCancelar ? '← Voltar à ordem sem perder os dados' : 'Cancelar'}</button>`);
        const form = document.querySelector('#cadastro-form');
        if (tipo === 'veiculo' && !id) montarSeletor(form.querySelector('[data-seletor-cliente]'), contexto, 'cliente', { valor: clienteId });
        if (tipo === 'servico') montarCategoriasServico(form, contexto, categorias);
        const eventos = new AbortController();
        const cancelar = () => {
            eventos.abort();
            return aoCancelar ? aoCancelar() : document.querySelector('#modal').close();
        };
        if (aoCancelar) document.querySelector('#modal').addEventListener('cancel', event => {
            event.preventDefault(); cancelar();
        }, { signal: eventos.signal });
        document.querySelector('[data-cadastro-cancelar]').addEventListener('click', cancelar);
        if (aoCancelar) document.querySelector('#modal [data-close]').addEventListener('click', event => {
            event.stopPropagation(); cancelar();
        });
        form.addEventListener('submit', event => salvar(event, contexto, tipo, id, aoSalvar ? registro => {
            eventos.abort();
            return aoSalvar(registro);
        } : undefined));
        form.querySelector('input:not([type=hidden])')?.focus();
    } catch (erro) { aviso(mensagemErro(erro)); }
}

async function buscar({ estado }, tipo, id) {
    if (estado.modo === 'demo') return estado.dados[colecoes[tipo]].find(i => i.id === id);
    return tipo === 'cliente' ? estado.api.cliente(id) : estado.api[colecoes[tipo]].buscar(id);
}

async function salvar(event, contexto, tipo, id, aoSalvar) {
    event.preventDefault();
    const { estado, aviso, mensagemErro, carregar } = contexto;
    if (estado.modo !== 'real') return;
    const form = event.currentTarget, botao = form.querySelector('[type=submit]');
    if (botao.disabled) return;
    if (form.dataset.categoriaLoading === 'true') {
        form.querySelector(':scope>.form-error').textContent = 'Aguarde a categoria terminar de salvar.'; return;
    }
    if (form.querySelector('[data-picker-loading="true"]')) {
        form.querySelector(':scope>.form-error').textContent = 'Aguarde a pesquisa terminar antes de salvar.'; return;
    }
    botao.disabled = true;
    form.querySelector(':scope>.form-error').textContent = '';
    try {
        const dados = dadosCadastro(tipo, Object.fromEntries(new FormData(form)), Boolean(id));
        const registro = await gravar(estado.api, tipo, id, dados);
        guardarCadastro(estado.dados, tipo, registro);
        aviso(`${id ? 'Alteração' : 'Cadastro'} salvo.`);
        if (aoSalvar) await aoSalvar(registro);
        else { document.querySelector('#modal').close(); await carregar(); }
    } catch (erro) { if (form.isConnected) form.querySelector(':scope>.form-error').textContent = mensagemErro(erro); }
    finally { botao.disabled = false; }
}

function gravar(api, tipo, id, dados) {
    if (tipo === 'cliente') return id ? api.atualizarCliente(id, dados) : api.cadastrarCliente(dados);
    return id ? api[colecoes[tipo]].atualizar(id, dados) : api[colecoes[tipo]].cadastrar(dados);
}
