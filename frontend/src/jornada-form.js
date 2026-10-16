import { camposJornada, jornadaEntrada, minutosPrevistos, dataValida } from './lib/jornada.js';
import { escapar as e, horas } from './lib/ui.js';

export function configurarJornada(contexto, funcionarioId) {
    const { estado, abrirModal, conectar, carregar, aviso, mensagemErro } = contexto;
    const funcionario = estado.dados.funcionarios.find(f => f.id === funcionarioId);
    if (!funcionario) return;
    const hoje = new Date().toLocaleDateString('sv-SE');
    abrirModal('Configurar horários', `<p class="modal-description">${e(funcionario.nome)} · Configure cada dia de trabalho. A carga prevista desconta o intervalo; a tolerância entra no cálculo de atraso.</p><form id="schedule-form">${camposJornada(hoje)}<p class="form-error" role="alert"></p><button class="button primary wide" ${estado.modo === 'demo' ? 'disabled' : ''}>Salvar jornada</button></form>${estado.modo === 'demo' ? '<p class="modal-description">Prévia do formulário. Conecte o backend para salvar a jornada da oficina.</p><button class="button wide" data-connect-schedule>Conectar backend</button>' : ''}`);
    const form = document.querySelector('#schedule-form');
    const preview = form.querySelector('#schedule-preview');
    const atualizarCarga = () => {
        try {
            const minutos = minutosPrevistos(form.elements.entrada.value, form.elements.saida.value, Number(form.elements.intervaloMinutos.value));
            preview.textContent = `Carga diária prevista: ${horas(minutos)}${form.elements.saida.value < form.elements.entrada.value ? ' · saída no dia seguinte' : ''}`;
        } catch (erro) { preview.textContent = erro.message; }
    };
    form.addEventListener('input', atualizarCarga);
    atualizarCarga();
    document.querySelector('[data-connect-schedule]')?.addEventListener('click', () => { document.querySelector('#modal').close(); conectar(); });
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (estado.modo !== 'real') return;
        const button = form.querySelector('button');
        button.disabled = true;
        form.querySelector('.form-error').textContent = '';
        try {
            await estado.api.funcionarios.criarJornada(funcionarioId, jornadaEntrada(Object.fromEntries(new FormData(form))));
            estado.funcionarioId = funcionarioId;
            document.querySelector('#modal').close();
            aviso('Horários salvos. O resumo será recalculado pelo servidor.');
            await carregar();
        } catch (erro) { form.querySelector('.form-error').textContent = mensagemErro(erro); }
        finally { button.disabled = false; }
    });
}

export function encerrarJornada(contexto, id) {
    const { estado, abrirModal, carregar, aviso, mensagemErro } = contexto;
    const jornada = estado.jornadas.find(j => j.id === id);
    if (!jornada) return;
    abrirModal('Encerrar vigência', `<p class="modal-description">Informe o último dia em que estes horários serão válidos. Depois, cadastre os novos horários a partir da data seguinte.</p><form id="end-schedule-form"><label>Válida até<input name="fim" type="date" min="${e(jornada.vigenciaInicio)}" required></label><p class="form-error" role="alert"></p><button class="button primary wide" ${estado.modo === 'demo' ? 'disabled' : ''}>Encerrar vigência</button></form>`);
    const form = document.querySelector('#end-schedule-form');
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (estado.modo !== 'real') return;
        const button = form.querySelector('button');
        button.disabled = true;
        try {
            const fim = form.elements.fim.value;
            if (!dataValida(fim) || fim < jornada.vigenciaInicio) throw new Error('O fim deve ser igual ou posterior ao início da vigência.');
            await estado.api.funcionarios.encerrarJornada(id, fim);
            document.querySelector('#modal').close();
            aviso('Vigência encerrada. Você pode cadastrar os novos horários.');
            await carregar();
        } catch (erro) { form.querySelector('.form-error').textContent = mensagemErro(erro); }
        finally { button.disabled = false; }
    });
}
