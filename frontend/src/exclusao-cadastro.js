import { escapar as e } from './lib/ui.js';
import { colecoes as colecoesCadastro, nomes as nomesCadastro } from './lib/cadastros.js';
const colecoes = { ...colecoesCadastro, funcionario: 'funcionarios' };
const nomes = { ...nomesCadastro, funcionario: 'funcionário' };
export function botaoExcluir(tipo, registro) {
  const nome = registro.nome || registro.placa;
  return `<button type="button" class="delete-button" data-excluir-cadastro="${e(tipo)}" data-registro-id="${e(registro.id)}" aria-label="Excluir ${e(nome)}" title="Excluir ${e(nome)}">
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 6h18M9 6V4h6v2M5 6l1 14h12l1-14M10 10v6M14 10v6"/></svg>
    </button>`;
}
export async function executarExclusao(api, tipo, id) {
  if (!Object.hasOwn(colecoes, tipo)) {
    throw new Error('Cadastro inválido.');
  }
  return tipo === 'cliente' ? api.excluirCliente(id) : api[colecoes[tipo]].excluir(id);
}
export function confirmarExclusao(contexto, tipo, id) {
  const {
    estado,
    abrirModal,
    aviso,
    carregar,
    mensagemErro
  } = contexto;
  if (!Object.hasOwn(colecoes, tipo)) {
    return;
  }
  const registro = estado.dados[colecoes[tipo]].find(item => item.id === id);
  if (!registro) {
    return;
  }
  const api = estado.api;
  abrirModal(`Excluir ${nomes[tipo]}`, `<p>Excluir <strong>${e(registro.nome || registro.placa)}</strong> permanentemente?</p>
        <p class="modal-description">Cadastros com histórico vinculado não podem ser excluídos, incluindo ponto, jornadas e serviços de funcionários.</p>
        <p class="form-error" role="alert" data-delete-error></p><div class="work-actions"><button type="button" class="button danger" data-delete-confirm ${estado.modo !== 'real' ? 'disabled' : ''}>Excluir definitivamente</button><button type="button" class="button" data-delete-cancel>Cancelar</button></div>
        ${estado.modo !== 'real' ? '<p class="modal-description">Exclusão desativada na demonstração.</p>' : ''}`);
  const modal = document.querySelector('#modal'),
    confirmar = modal.querySelector('[data-delete-confirm]');
  const cancelar = modal.querySelector('[data-delete-cancel]'),
    erro = modal.querySelector('[data-delete-error]');
  cancelar.addEventListener('click', () => modal.close());
  confirmar.addEventListener('click', async () => {
    if (confirmar.disabled || estado.modo !== 'real' || estado.api !== api) {
      return;
    }
    confirmar.disabled = cancelar.disabled = true;
    erro.textContent = '';
    try {
      await executarExclusao(api, tipo, id);
      if (estado.api !== api || estado.modo !== 'real') {
        return;
      }
      estado.dados[colecoes[tipo]] = estado.dados[colecoes[tipo]].filter(item => item.id !== id);
      if (tipo === 'funcionario' && estado.funcionarioId === id) {
        estado.funcionarioId = undefined;
        estado.jornadas = [];
        estado.resumo = undefined;
      }
      if (estado.catalogos?.[tipo]) {
        estado.catalogos[tipo].pagina = 0;
      }
      if (confirmar.isConnected) {
        modal.close();
      }
      aviso('Cadastro excluído.');
      await carregar();
    } catch (error_) {
      if (erro.isConnected) {
        erro.textContent = error_.status === 409 ? 'Este cadastro possui vínculos ou foi alterado. A exclusão foi bloqueada para preservar os registros; atualize a lista.' : mensagemErro(error_);
      }
    } finally {
      confirmar.disabled = estado.modo !== 'real';
      cancelar.disabled = false;
    }
  });
}
