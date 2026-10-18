import { escapar as e } from './ui.js';
export const diasSemana = [['MONDAY', 'Segunda-feira'], ['TUESDAY', 'Terça-feira'], ['WEDNESDAY', 'Quarta-feira'], ['THURSDAY', 'Quinta-feira'], ['FRIDAY', 'Sexta-feira'], ['SATURDAY', 'Sábado'], ['SUNDAY', 'Domingo']];
export const diaSemana = dia => diasSemana.find(([valor]) => valor === dia)?.[1] ?? dia;
export function minutosPrevistos(entrada, saida, intervalo = 0) {
  const minutos = hora => {
    if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(hora)) {
      throw new Error('Informe horários válidos com horas e minutos.');
    }
    const [h, m] = hora.split(':').map(Number);
    return h * 60 + m;
  };
  const periodo = (minutos(saida) - minutos(entrada) + 1440) % 1440;
  if (!periodo) {
    throw new Error('Entrada e saída devem ser diferentes.');
  }
  if (!Number.isInteger(intervalo) || intervalo < 0 || intervalo >= periodo) {
    throw new Error('O intervalo deve ser menor que o período da jornada.');
  }
  return periodo - intervalo;
}
export function dataValida(data) {
  return /^\d{4}-\d{2}-\d{2}$/.test(data) && !Number.isNaN(Date.parse(data)) && new Date(data).toISOString().slice(0, 10) === data;
}
export function jornadaEntrada(valores) {
  const intervaloMinutos = /^\d+$/.test(valores.intervaloMinutos) ? Number(valores.intervaloMinutos) : Number.NaN;
  const toleranciaMinutos = /^\d+$/.test(valores.toleranciaMinutos) ? Number(valores.toleranciaMinutos) : Number.NaN;
  const previstos = minutosPrevistos(valores.entrada, valores.saida, intervaloMinutos);
  if (!diasSemana.some(([dia]) => dia === valores.dia)) {
    throw new Error('Selecione um dia da semana.');
  }
  if (!Number.isInteger(toleranciaMinutos) || toleranciaMinutos < 0 || toleranciaMinutos > previstos + intervaloMinutos) {
    throw new Error('A tolerância deve estar dentro do período da jornada.');
  }
  if (!dataValida(valores.vigenciaInicio) || valores.vigenciaFim && (!dataValida(valores.vigenciaFim) || valores.vigenciaFim < valores.vigenciaInicio)) {
    throw new Error('Confira o início e o fim da vigência.');
  }
  return {
    dia: valores.dia,
    entrada: valores.entrada,
    saida: valores.saida,
    intervaloMinutos,
    toleranciaMinutos,
    vigenciaInicio: valores.vigenciaInicio,
    vigenciaFim: valores.vigenciaFim || null,
    intervaloAutomatico: valores.intervaloAutomatico !== 'false'
  };
}
export function camposJornada(inicio) {
  const _diasSemanaMarkup = diasSemana.map(([valor, nome]) => {
    return `<option value="${valor}">${nome}</option>`;
  }).join('');
  return `<label>Dia da semana<select name="dia" required>${_diasSemanaMarkup}</select></label>
        <div class="form-row"><label>Entrada prevista<input type="time" name="entrada" value="08:00" required step="60"></label><label>Saída prevista<input type="time" name="saida" value="17:00" required step="60"></label></div>
        <div class="form-row"><label>Intervalo de almoço (minutos)<input type="number" name="intervaloMinutos" value="60" min="0" step="1" required></label><label>Tolerância de atraso (minutos)<input type="number" name="toleranciaMinutos" value="5" min="0" step="1" required></label></div>
        <label>Exigir ponto no almoço?<select name="intervaloAutomatico"><option value="true">Não</option><option value="false">Sim</option></select></label><p class="modal-description">Não: registre apenas entrada e saída; o sistema desconta os minutos de almoço configurados. Sim: registre também saída e retorno do almoço; o sistema usa o intervalo registrado. Se o almoço for marcado, vale o intervalo registrado, sem desconto em dobro. Use 0 minutos quando não houver intervalo.</p>
        <div class="form-row"><label>Válida a partir de<input type="date" name="vigenciaInicio" value="${e(inicio)}" required></label><label>Até (opcional)<input type="date" name="vigenciaFim"></label></div>
        <p class="schedule-preview" id="schedule-preview" role="status" aria-live="polite"></p>`;
}
export function semanaEntrada(valores, dias) {
  if (!dias.length || new Set(dias).size !== dias.length) {
    throw new Error('Selecione pelo menos um dia, sem repetições.');
  }
  return {
    encerrarAnteriores: valores.encerrarAnteriores === 'on',
    jornadas: dias.map(dia => jornadaEntrada({
      dia,
      entrada: valores[`${dia}.entrada`],
      saida: valores[`${dia}.saida`],
      intervaloMinutos: valores[`${dia}.intervaloMinutos`],
      toleranciaMinutos: valores[`${dia}.toleranciaMinutos`],
      intervaloAutomatico: valores[`${dia}.intervaloAutomatico`],
      vigenciaInicio: valores.vigenciaInicio,
      vigenciaFim: valores.vigenciaFim
    }))
  };
}
export function camposSemana(inicio) {
  const campo = (dia, nome, titulo, tipo, valor, extra = '') => {
    return `<label>${titulo}<input name="${dia}.${nome}" type="${tipo}" value="${valor}" required ${extra}></label>`;
  };
  const _diasSemanaMarkup2 = diasSemana.map(([dia, nome], i) => {
    return `<fieldset class="schedule-day" data-day="${dia}" ${i > 4 ? 'disabled' : ''}>
            <legend><label><input type="checkbox" name="dias" value="${dia}" ${i < 5 ? 'checked' : ''}>${nome}</label></legend>
            <div class="schedule-day-fields">${campo(dia, 'entrada', 'Entrada', 'time', '08:00', 'step="60"')}${campo(dia, 'saida', 'Saída', 'time', '17:00', 'step="60"')}
            ${campo(dia, 'intervaloMinutos', 'Almoço (min)', 'number', '60', 'min="0" step="1"')}${campo(dia, 'toleranciaMinutos', 'Tolerância (min)', 'number', '5', 'min="0" step="1"')}
            <label>Exigir ponto no almoço?<select name="${dia}.intervaloAutomatico"><option value="true">Não</option><option value="false">Sim</option></select></label></div>
        </fieldset>`;
  }).join('');
  return `<p class="modal-description">Marque os dias de trabalho e ajuste os horários abaixo. Dias desmarcados não serão alterados.</p>
        <div class="schedule-shortcuts"><button type="button" class="button small" data-weekdays>Segunda a sexta</button><button type="button" class="button small" data-copy-hours>Copiar horários do primeiro dia marcado</button></div>
        <div class="weekly-schedules">${_diasSemanaMarkup2}</div>
        <p class="modal-description">Não: o funcionário registra apenas entrada e saída; os minutos de almoço configurados são descontados. Sim: ele registra também saída e retorno do almoço; vale o intervalo registrado, sem desconto em dobro.</p>
        <div class="form-row"><label>Válida a partir de<input type="date" name="vigenciaInicio" value="${e(inicio)}" required></label><label>Até (opcional)<input type="date" name="vigenciaFim"></label></div>
        <label class="schedule-replace"><input type="checkbox" name="encerrarAnteriores" checked>Encerrar jornadas anteriores abertas no dia anterior à nova vigência</label>
        <p class="modal-description">O histórico é preservado. Para substituir uma jornada, escolha uma data posterior ao início dela.</p>
        <p class="schedule-preview" id="schedule-preview" role="status" aria-live="polite"></p>`;
}
