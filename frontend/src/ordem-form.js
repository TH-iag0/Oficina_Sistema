import { montarSeletor } from './seletor-cadastro.js';
import { abrirCadastro } from './cadastros.js';
import { abrirAtendimento } from './atendimento.js';
import { camposAbertura, dadosAbertura } from './lib/ordem.js';

export function abrirOrdem(contexto, anterior = {}) {
    const rascunho = { clienteId: '', veiculoId: '', situacaoInicial: 'RECEBIDO', ...anterior };
    contexto.abrirModal('Nova ordem de serviço', camposAbertura(rascunho, contexto.estado.modo === 'demo'));
    const form = document.querySelector('#ordem-form');
    montarIdentificacao(form, contexto, rascunho);
    montarPrimeiroServico(form, contexto, rascunho);
    const guardar = () => Object.assign(rascunho, Object.fromEntries(new FormData(form)), { incluirServico: form.elements.incluirServico.checked });
    form.addEventListener('input', guardar);
    form.addEventListener('change', guardar);
    form.addEventListener('click', event => {
        const tipo = event.target.closest('[data-os-novo]')?.dataset.osNovo;
        if (!tipo) return;
        guardar();
        abrirCadastro(contexto, tipo, { clienteId: rascunho.clienteId,
            aoCancelar: () => abrirOrdem(contexto, rascunho), aoSalvar: registro => {
                rascunho.clienteId = tipo === 'cliente' ? registro.id : registro.clienteId;
                rascunho.veiculoId = tipo === 'cliente' ? '' : registro.id;
                abrirOrdem(contexto, rascunho);
            } });
    });
    form.addEventListener('submit', event => { event.preventDefault(); guardar(); salvarOrdem(form, contexto, rascunho); });
}

function montarIdentificacao(form, contexto, rascunho) {
    const veiculos = montarSeletor(form.querySelector('[data-os-veiculo]'), contexto, 'veiculo', {
        compacto: true, valor: rascunho.veiculoId, clienteId: () => rascunho.clienteId,
        aoSelecionar: id => { rascunho.veiculoId = id; }
    });
    montarSeletor(form.querySelector('[data-os-cliente]'), contexto, 'cliente', {
        compacto: true, valor: rascunho.clienteId, aoSelecionar: id => {
            rascunho.clienteId = id; rascunho.veiculoId = '';
            form.querySelector('[data-os-novo="veiculo"]').disabled = !id;
            veiculos.atualizar();
        }
    });
}

function montarPrimeiroServico(form, contexto, rascunho) {
    const fieldset = form.querySelector('[data-os-primeiro-servico]');
    let montado = false;
    function atualizar() {
        const ativo = form.elements.incluirServico.checked;
        fieldset.hidden = fieldset.disabled = !ativo;
        if (!ativo || montado) return;
        montado = true;
        montarSeletor(fieldset.querySelector('[data-os-servico]'), contexto, 'servico', {
            compacto: true, valor: rascunho.servicoId, aoSelecionar: id => {
                const servico = contexto.estado.dados.servicos.find(s => s.id === id);
                form.elements.preco.value = servico?.preco ?? '';
                form.elements.custo.value = servico?.custo ?? '';
            }
        });
    }
    form.elements.incluirServico.addEventListener('change', atualizar);
    atualizar();
}

async function salvarOrdem(form, contexto, rascunho) {
    const { estado, aviso, mensagemErro, carregar } = contexto, botao = form.querySelector('[type=submit]');
    if (estado.modo !== 'real' || botao.disabled) return;
    form.querySelector(':scope>.form-error').textContent = '';
    if ([...form.querySelectorAll('[data-picker-loading="true"]')].some(el => !el.closest('fieldset:disabled'))) {
        form.querySelector(':scope>.form-error').textContent = 'Aguarde os cadastros terminarem de carregar.'; return;
    }
    botao.disabled = true;
    try {
        const ordem = await estado.api.ordens.abrir(dadosAbertura(rascunho));
        document.querySelector('#modal').close();
        estado.pagina = 'operacao'; aviso('Ordem criada. Atualize a situação e os serviços no acompanhamento.');
        await carregar(); await abrirAtendimento(contexto, ordem.id);
    } catch (erro) { if (form.isConnected) form.querySelector(':scope>.form-error').textContent = mensagemErro(erro); }
    finally { botao.disabled = false; }
}
