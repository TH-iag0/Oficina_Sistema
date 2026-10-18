import { camposSemana, semanaEntrada, minutosPrevistos, dataValida } from './lib/jornada.js';
import { escapar as e, horas } from './lib/ui.js';
export function configurarJornada(contexto, funcionarioId) {
  const {
    estado,
    abrirModal,
    conectar,
    carregar,
    aviso,
    mensagemErro
  } = contexto;
  const funcionario = estado.dados.funcionarios.find(f => f.id === funcionarioId);
  if (!funcionario) {
    return;
  }
  const hoje = new Date().toLocaleDateString('sv-SE');
  abrirModal('Configurar horários', `<p class="modal-description">${e(funcionario.nome)} · Configure todos os dias nesta janela. Cada dia pode ter seus próprios horários e intervalo.</p><form id="schedule-form">${camposSemana(hoje)}<p class="form-error" role="alert"></p><button class="button primary wide" ${estado.modo === 'demo' ? 'disabled' : ''}>Salvar todos os dias</button></form>${estado.modo === 'demo' ? '<p class="modal-description">Prévia do formulário. Conecte o backend para salvar a jornada da oficina.</p><button class="button wide" data-connect-schedule>Conectar backend</button>' : ''}`);
  const form = document.querySelector('#schedule-form');
  const preview = form.querySelector('#schedule-preview');
  const diasMarcados = () => [...form.querySelectorAll('input[name=dias]:checked')].map(input => input.value);
  const atualizarCarga = () => {
    for (const campo of form.querySelectorAll('[data-day]')) campo.disabled = !campo.querySelector('input[name=dias]').checked;
    try {
      const dados = semanaEntrada(Object.fromEntries(new FormData(form)), diasMarcados());
      const minutos = dados.jornadas.reduce((soma, dia) => soma + minutosPrevistos(dia.entrada, dia.saida, dia.intervaloMinutos), 0);
      preview.textContent = dados.jornadas.length + ' dia(s) selecionado(s) · Carga semanal prevista: ' + horas(minutos);
    } catch (error_) {
      preview.textContent = error_.message;
    }
  };
  form.addEventListener('input', atualizarCarga);
  form.addEventListener('change', atualizarCarga);
  form.querySelector('[data-weekdays]').addEventListener('click', () => {
    for (const [i, input] of [...form.querySelectorAll('input[name=dias]')].entries()) input.checked = i < 5;
    atualizarCarga();
  });
  form.querySelector('[data-copy-hours]').addEventListener('click', () => {
    const dias = diasMarcados(),
      origem = dias[0];
    if (!origem) {
      return;
    }
    for (const dia of dias.slice(1)) for (const nome of ['entrada', 'saida', 'intervaloMinutos', 'toleranciaMinutos', 'intervaloAutomatico']) {
      form.elements[dia + '.' + nome].value = form.elements[origem + '.' + nome].value;
    }
    atualizarCarga();
  });
  atualizarCarga();
  document.querySelector('[data-connect-schedule]')?.addEventListener('click', () => {
    document.querySelector('#modal').close();
    conectar();
  });
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (estado.modo !== 'real') {
      return;
    }
    const button = form.querySelector('button.primary');
    if (button.disabled) {
      return;
    }
    button.disabled = true;
    form.querySelector('.form-error').textContent = '';
    try {
      await estado.api.funcionarios.configurarSemana(funcionarioId, semanaEntrada(Object.fromEntries(new FormData(form)), diasMarcados()));
      estado.funcionarioId = funcionarioId;
      document.querySelector('#modal').close();
      aviso('Horários salvos. O resumo será recalculado pelo servidor.');
      await carregar();
    } catch (error_) {
      form.querySelector('.form-error').textContent = mensagemErro(error_);
    } finally {
      button.disabled = false;
    }
  });
}
export function encerrarJornada(contexto, id) {
  const {
    estado,
    abrirModal,
    carregar,
    aviso,
    mensagemErro
  } = contexto;
  const jornada = estado.jornadas.find(j => j.id === id);
  if (!jornada) {
    return;
  }
  abrirModal('Encerrar vigência', `<p class="modal-description">Informe o último dia em que estes horários serão válidos. Depois, cadastre os novos horários a partir da data seguinte.</p><form id="end-schedule-form"><label>Válida até<input name="fim" type="date" min="${e(jornada.vigenciaInicio)}" required></label><p class="form-error" role="alert"></p><button class="button primary wide" ${estado.modo === 'demo' ? 'disabled' : ''}>Encerrar vigência</button></form>`);
  const form = document.querySelector('#end-schedule-form');
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (estado.modo !== 'real') {
      return;
    }
    const button = form.querySelector('button');
    button.disabled = true;
    try {
      const fim = form.elements.fim.value;
      if (!dataValida(fim) || fim < jornada.vigenciaInicio) {
        throw new Error('O fim deve ser igual ou posterior ao início da vigência.');
      }
      await estado.api.funcionarios.encerrarJornada(id, fim);
      document.querySelector('#modal').close();
      aviso('Vigência encerrada. Você pode cadastrar os novos horários.');
      await carregar();
    } catch (error_) {
      form.querySelector('.form-error').textContent = mensagemErro(error_);
    } finally {
      button.disabled = false;
    }
  });
}
