import { escapar as e } from './lib/ui.js';
import { colecoes, nomes, guardarCadastro, pesquisarCadastro, paginaDemo } from './lib/cadastros.js';
import { adiarPesquisa } from './lib/pesquisa.js';

export function montarSeletor(root, contexto, tipo, { valor = '', clienteId, aoSelecionar = () => {}, compacto = false } = {}) {
    const { estado, mensagemErro } = contexto;
    const nome = nomes[tipo];
    let pagina = 0, busca = '', selecionado = valor, resposta = null, erro = '', versao = 0;
    const label = item => tipo === 'veiculo' ? `${item.placa} · ${item.marca} ${item.modelo}` : item.nome;
    const pesquisar = adiarPesquisa(() => { if (root.isConnected) carregar(); });

    function desenhar(carregando = false) {
        const campo = root.querySelector('[data-picker-search]');
        const focado = campo && campo === document.activeElement;
        const valor = campo?.value, inicio = campo?.selectionStart, fim = campo?.selectionEnd;
        const itens = resposta?.itens || [];
        const atual = estado.dados[colecoes[tipo]].find(i => i.id === selecionado);
        const opcoes = atual && !itens.some(i => i.id === atual.id) ? [atual, ...itens] : itens;
        root.innerHTML = `<label>Buscar ${nome}<div class="search-row"><svg class="search-icon" width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4.5 4.5"/></svg><input data-picker-search value="${e(busca)}" maxlength="160" placeholder="${tipo === 'veiculo' ? 'Placa, marca ou modelo' : 'Digite o nome'}"></div></label>
            <label>${tipo === 'veiculo' ? 'Veículo do cliente' : nome === 'cliente' ? 'Cliente' : 'Serviço'}<select name="${tipo}Id" required ${carregando ? 'disabled' : ''}><option value="">${carregando ? 'Buscando…' : opcoes.length ? 'Selecione' : tipo === 'veiculo' && !clienteId?.() ? 'Escolha o cliente primeiro' : 'Nenhum cadastro encontrado'}</option>${opcoes.map(i => `<option value="${e(i.id)}" ${i.id === selecionado ? 'selected' : ''}>${e(label(i))}</option>`).join('')}</select></label>
            <div class="pagination"><button type="button" class="text-button" data-picker-action="anterior" ${carregando || pagina === 0 ? 'disabled' : ''} aria-label="Página anterior" title="Página anterior"><span aria-hidden="true">‹</span></button><span>${resposta ? `${resposta.total} encontrados · página ${pagina + 1} de ${Math.max(1, resposta.totalPaginas)}` : ''}</span><button type="button" class="text-button" data-picker-action="proxima" ${carregando || !resposta || pagina + 1 >= resposta.totalPaginas ? 'disabled' : ''} aria-label="Próxima página" title="Próxima página"><span aria-hidden="true">›</span></button></div>${erro ? `<p class="form-error" role="alert">${e(erro)}</p>` : ''}`;
        if (focado) {
            const novo = root.querySelector('[data-picker-search]');
            novo.value = valor; novo.focus({ preventScroll: true });
            novo.setSelectionRange(inicio, fim);
        }
    }

    function compactar() {
        const focado = root.querySelector('[data-picker-search]') === document.activeElement;
        const pesquisa = root.firstElementChild, selecao = pesquisa.nextElementSibling;
        const detalhes = document.createElement('details');
        detalhes.className = 'picker-more'; detalhes.open = Boolean(busca || pagina || erro);
        const resumo = document.createElement('summary');
        resumo.textContent = `Pesquisar outro ${nome}`;
        detalhes.append(resumo, pesquisa);
        while (selecao.nextSibling) detalhes.append(selecao.nextSibling);
        root.replaceChildren(selecao, detalhes);
        if (focado) { detalhes.open = true; root.querySelector('[data-picker-search]').focus({ preventScroll: true }); }
    }

    async function carregar() {
        const atual = ++versao, api = estado.api, modo = estado.modo;
        root.dataset.pickerLoading = 'true';
        erro = '';
        desenhar(true);
        try {
            const parametros = { busca, pagina, tamanho: 10, ativo: true, clienteId: clienteId?.() };
            const dados = tipo === 'veiculo' && !parametros.clienteId ? { itens: [], total: 0, totalPaginas: 0 }
                : modo === 'demo' ? paginaDemo(estado.dados, tipo, parametros) : await pesquisarCadastro(api, tipo, parametros);
            if (atual !== versao || !root.isConnected || modo !== estado.modo || api !== estado.api) return;
            resposta = dados;
            if (modo === 'real') dados.itens.forEach(item => guardarCadastro(estado.dados, tipo, item));
        } catch (falha) { if (atual === versao) erro = mensagemErro(falha); }
        if (atual === versao && root.isConnected) {
            root.dataset.pickerLoading = 'false'; desenhar();
            if (compacto) compactar();
            root.querySelector('select').setAttribute('aria-label', tipo === 'veiculo' ? 'Veículo do cliente' : tipo === 'cliente' ? 'Cliente' : 'Serviço');
        }
    }

    root.addEventListener('click', event => {
        const acao = event.target.closest('[data-picker-action]')?.dataset.pickerAction;
        if (!acao) return;
        if (acao === 'buscar') { busca = root.querySelector('[data-picker-search]').value.trim(); pagina = 0; }
        else pagina += acao === 'proxima' ? 1 : -1;
        carregar();
    });
    root.addEventListener('keydown', event => {
        if (event.key === 'Enter' && event.target.matches('[data-picker-search]')) {
            event.preventDefault(); pesquisar.cancelar();
            busca = event.target.value.trim(); pagina = 0; carregar();
        }
    });
    root.addEventListener('input', event => {
        if (!event.target.matches('[data-picker-search]') || event.isComposing) return;
        busca = event.target.value.trim(); pagina = 0; ++versao; pesquisar();
    });
    root.addEventListener('compositionend', event => {
        if (!event.target.matches('[data-picker-search]')) return;
        busca = event.target.value.trim(); pagina = 0; ++versao; pesquisar();
    });
    root.addEventListener('change', event => {
        if (event.target.matches('select')) { selecionado = event.target.value; aoSelecionar(selecionado); }
    });
    carregar();
    return { atualizar(novo = '') { pesquisar.cancelar(); selecionado = novo; pagina = 0; busca = ''; return carregar(); } };
}
