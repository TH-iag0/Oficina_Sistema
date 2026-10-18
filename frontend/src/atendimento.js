import { montarSeletor } from './seletor-cadastro.js';
import { escapar as e, moeda, rotulo, confirmacaoIdempotente } from './lib/ui.js';
import { acoesSituacao, dadosConclusao, podeFinalizar } from './lib/atendimento.js';
import { dadosItemServico, valorMonetario } from './lib/validacao.js';
const pagamentosPendentes = new Map();
const campo = (nome, titulo, valor = '', tipo = 'number', extra = '') => {
  return `<label>${titulo}<input name="${nome}" type="${tipo}" value="${e(valor)}" ${extra}></label>`;
};
const opcoes = (nome, titulo, registros) => {
  const _registrosMarkup = registros.map(([id, nome]) => {
    return `<option value="${e(id)}">${e(nome)}</option>`;
  }).join('');
  return `<label>${titulo}<select name="${nome}" required><option value="">Selecione</option>${_registrosMarkup}</select></label>`;
};
export function detalhesAtendimento(ordem, dados, somenteLeitura = false) {
  const cliente = dados.clientes.find(c => c.id === ordem.clienteId);
  const veiculo = dados.veiculos.find(v => v.id === ordem.veiculoId);
  const editavel = !somenteLeitura && ['RASCUNHO', 'RECEBIDO', 'DIAGNOSTICO', 'AGUARDANDO_APROVACAO'].includes(ordem.status);
  const _lengthMarkup = ordem.itens.length ? ordem.itens.map(item => {
    const _relatoMarkup = item.relato ? `<p class="work-report">${e(item.relato)}</p>` : '';
    const _conteudoMarkup = !somenteLeitura && ordem.status === 'EM_SERVICO' && !item.concluidoEm ? `<button class="button small primary" data-work="concluir" data-item="${e(item.id)}">Escrever o que fiz</button>` : '';
    return `<article class="work-item"><div><strong>${e(item.descricao)}</strong><small>${item.quantidade} × ${moeda(item.preco)} · ${item.concluidoEm ? 'Concluído' : 'Pendente'}</small></div>${_relatoMarkup}${_conteudoMarkup}</article>`;
  }).join('') : '<p class="modal-description">Nenhum serviço registrado.</p>';
  return `<div id="work-detail"><p class="modal-description">${e(cliente?.nome || 'Cliente')} · ${e(veiculo ? veiculo.placa + ' · ' + veiculo.modelo : 'Veículo')}<br>Situação: <strong>${e(rotulo(ordem.status))}</strong></p>
        ${situacaoAtendimento(ordem, somenteLeitura)}
        <div class="work-total"><span>Total dos serviços<strong>${moeda(ordem.total)}</strong></span><span>Custos diretos<strong>${moeda(ordem.custo)}</strong></span></div>
        <h3>O que foi feito no veículo?</h3><p class="modal-description">${somenteLeitura ? 'Serviços e relatos registrados nesta ordem.' : 'Adicione os serviços e registre a descrição do trabalho ao concluí-los.'}</p>
        <div class="work-items">${_lengthMarkup}</div>
        <div class="work-actions">${editavel ? '<button class="button" data-work="adicionar">Adicionar serviço</button>' : ''}
        ${!somenteLeitura && ordem.status !== 'CANCELADO' ? '<button class="button" data-work="pagamento">Registrar recebimento</button>' : ''}</div>
        <p class="modal-description">Serviços de ordens finalizadas alimentam o faturamento. Recebimentos e despesas alimentam o financeiro.</p></div>`;
}
export function situacaoAtendimento(ordem, somenteLeitura = false) {
  const acoes = acoesSituacao(ordem);
  const historico = (ordem.historico ?? []).map(h => {
    return `<li><strong>${e(rotulo(h.status))}</strong><time>${e(new Date(h.instante).toLocaleString('pt-BR'))}</time></li>`;
  }).join('');
  if (somenteLeitura) {
    return `<section class="work-status"><h3>Situação do veículo</h3><span class="badge ${e(ordem.status)}">${e(rotulo(ordem.status))}</span><details class="work-history"><summary>Histórico das situações e horários</summary><ol>${historico || '<li>Nenhuma etapa registrada.</li>'}</ol></details></section>`;
  }
  const _conteudoMarkup2 = acoes.map(a => {
    return `<button class="button ${a.destino === 'AGUARDANDO_PECA' ? '' : 'primary'}" data-work="${a.acao}" data-destino="${a.destino}" ${a.bloqueada ? 'disabled' : ''}>${e(a.titulo)}</button>`;
  }).join('') || '<p class="modal-description">Nenhuma mudança de situação disponível para esta ordem.</p>';
  return `<section class="work-status"><h3>Situação do veículo</h3><span class="badge ${e(ordem.status)}">${e(rotulo(ordem.status))}</span>
        <p class="modal-description">Use a ação abaixo quando a etapa mudar. A data e a hora serão salvas pelo servidor.</p>
        <div class="work-status-actions">${_conteudoMarkup2}</div>
        ${ordem.status === 'EM_SERVICO' && !podeFinalizar(ordem) ? '<p class="form-hint">Conclua todos os serviços em “Escrever o que fiz” para habilitar a finalização.</p>' : ''}
        ${ordem.status === 'AGUARDANDO_APROVACAO' && !ordem.itens.length ? '<p class="form-hint">Adicione ao menos um serviço para registrar a aprovação.</p>' : ''}
        <details class="work-history"><summary>Histórico das situações e horários</summary><ol>${historico || '<li>Os horários aparecerão quando esta ordem tiver etapas registradas.</li>'}</ol></details></section>`;
}
export async function abrirAtendimento(contexto, id, {
  somenteLeitura = false
} = {}) {
  const {
    estado,
    abrirModal,
    aviso,
    mensagemErro
  } = contexto;
  try {
    const ordem = estado.modo === 'real' ? await estado.api.ordens.buscar(id) : estado.dados.ordens.find(o => o.id === id);
    if (!ordem) {
      return;
    }
    abrirModal(`Atendimento · OS #${ordem.numero}`, detalhesAtendimento(ordem, estado.dados, somenteLeitura));
    if (somenteLeitura) {
      return;
    }
    document.querySelector('#work-detail').addEventListener('click', event => {
      const button = event.target.closest('[data-work]');
      if (button) {
        formularioTrabalho(contexto, ordem, button.dataset.work, button.dataset.item, button.dataset.destino);
      }
    });
  } catch (error_) {
    aviso(mensagemErro(error_));
  }
}
function configurarTrabalho(contexto, ordem, acao, itemId, destino) {
  const {
    estado
  } = contexto;
  const item = ordem.itens.find(i => i.id === itemId);
  switch (acao) {
    case 'adicionar':
      return {
        titulo: 'Adicionar serviço',
        campos: '<div data-work-servico></div>' + campo('quantidade', 'Quantidade', 1, 'number', 'required min="1" step="1"') + '<div class="form-row">' + campo('preco', 'Preço por unidade (R$)', '', 'number', 'required min="0" step="0.01"') + campo('custo', 'Custo por unidade (R$)', '', 'number', 'required min="0" step="0.01"') + '</div>',
        enviar: valores => estado.api.ordens.adicionarItem(ordem.id, dadosItemServico(valores))
      };
    case 'concluir':
      return {
        titulo: 'Registrar trabalho realizado',
        campos: `<p class="modal-description">${e(item.descricao)} · ${item.quantidade} unidade(s)</p><label>O que você fez?<textarea name="relato" rows="5" maxlength="4000" required placeholder="Ex.: Troquei óleo e filtro, conferi os níveis e fiz um teste de rodagem."></textarea></label>` + campo('custoReal', 'Custo direto por unidade (R$)', item.custo, 'number', 'required min="0" step="0.01"') + '<details class="optional-fields"><summary>Foi um retrabalho?</summary><label>Motivo (opcional)<textarea name="motivoRetrabalho" rows="2" maxlength="500"></textarea></label></details>',
        enviar: valores => estado.api.ordens.concluirItem(itemId, dadosConclusao(valores))
      };
    case 'aprovar':
      return {
        titulo: 'Registrar aprovação do cliente',
        campos: '<label>Como o cliente autorizou o serviço?<textarea name="consentimento" rows="3" required maxlength="1000" placeholder="Ex.: Cliente aprovou o orçamento pelo WhatsApp."></textarea></label>',
        enviar: valores => estado.api.ordens.aprovar(ordem.id, valores)
      };
    case 'status':
      return {
        titulo: acoesSituacao(ordem).find(a => a.destino === destino)?.titulo ?? 'Atualizar situação',
        campos: `<p class="modal-description">Confirme a passagem de ${e(rotulo(ordem.status))} para ${e(rotulo(destino))}. O horário será registrado automaticamente.</p>`,
        enviar: () => estado.api.ordens.status(ordem.id, {
          status: destino
        })
      };
    case 'pagamento':
      return configurarPagamento(estado, ordem);
    default:
      throw new Error('Ação de atendimento inválida.');
  }
}
function configurarPagamento(estado, ordem) {
  const pendente = pagamentosPendentes.get(ordem.id);
  return {
    titulo: 'Registrar recebimento',
    campos: campo('valor', 'Valor recebido (R$)', pendente?.dados?.valor ?? ordem.total, 'number', 'required min="0.01" step="0.01"') + opcoes('forma', 'Forma de pagamento', [['PIX', 'Pix'], ['DINHEIRO', 'Dinheiro'], ['DEBITO', 'Débito'], ['CREDITO', 'Crédito'], ['TRANSFERENCIA', 'Transferência'], ['OUTRO', 'Outra']]),
    enviar: async valores => {
      const valor = valorMonetario(valores.valor, 'Recebimento');
      if (valor === 0) {
        throw new Error('O recebimento deve ser maior que zero.');
      }
      let envio = pagamentosPendentes.get(ordem.id);
      if (!envio) {
        envio = {
          dados: {
            valor,
            forma: valores.forma
          }
        };
        envio.confirmar = confirmacaoIdempotente(dados => estado.api.financeiro.receber({
          ...dados,
          ordemServicoId: ordem.id
        }));
        pagamentosPendentes.set(ordem.id, envio);
      }
      try {
        const resposta = await envio.confirmar({
          valor,
          forma: valores.forma
        });
        pagamentosPendentes.delete(ordem.id);
        return resposta;
      } catch (error_) {
        if (error_.status >= 400 && error_.status < 500) {
          pagamentosPendentes.delete(ordem.id);
        }
        throw error_;
      }
    }
  };
}
function formularioTrabalho(contexto, ordem, acao, itemId, destino) {
  const {
    estado,
    abrirModal,
    aviso,
    carregar,
    mensagemErro
  } = contexto;
  const config = configurarTrabalho(contexto, ordem, acao, itemId, destino);
  abrirModal(config.titulo, `<form id="work-form">${config.campos}<p class="form-error" role="alert"></p><button class="button primary wide" ${estado.modo === 'real' ? '' : 'disabled'}>Salvar registro</button></form>${estado.modo === 'demo' ? '<p class="modal-description">Prévia: você pode preencher o formulário. Para salvar registros reais, conecte o backend.</p>' : ''}<button class="text-button" data-back-work>← Voltar ao atendimento</button>`);
  const form = document.querySelector('#work-form');
  form.querySelector('textarea')?.focus();
  if (acao === 'pagamento' && pagamentosPendentes.get(ordem.id)?.dados) {
    form.elements.forma.value = pagamentosPendentes.get(ordem.id).dados.forma;
  }
  if (acao === 'adicionar') {
    montarSeletor(form.querySelector('[data-work-servico]'), contexto, 'servico', {
      aoSelecionar: id => {
        const servico = estado.dados.servicos.find(s => s.id === id);
        form.elements.preco.value = servico?.preco ?? '';
        form.elements.custo.value = servico?.custo ?? '';
      }
    });
  }
  document.querySelector('[data-back-work]').addEventListener('click', () => abrirAtendimento(contexto, ordem.id));
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (estado.modo !== 'real') {
      return;
    }
    const button = form.querySelector('button.primary');
    if (button.disabled) {
      return;
    }
    if (form.querySelector('[data-picker-loading="true"]')) {
      form.querySelector(':scope>.form-error').textContent = 'Aguarde a pesquisa terminar antes de salvar.';
      return;
    }
    button.disabled = true;
    form.querySelector(':scope>.form-error').textContent = '';
    try {
      await config.enviar(Object.fromEntries(new FormData(form)));
      document.querySelector('#modal').close();
      aviso('Registro salvo. Os indicadores serão atualizados.');
      await carregar();
      await abrirAtendimento(contexto, ordem.id);
    } catch (error_) {
      form.querySelector(':scope>.form-error').textContent = mensagemErro(error_);
    } finally {
      button.disabled = false;
    }
  });
}
