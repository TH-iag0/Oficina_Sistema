import { escapar as e } from './ui.js';

export const diasSemana = [
    ['MONDAY', 'Segunda-feira'], ['TUESDAY', 'Terça-feira'], ['WEDNESDAY', 'Quarta-feira'],
    ['THURSDAY', 'Quinta-feira'], ['FRIDAY', 'Sexta-feira'], ['SATURDAY', 'Sábado'], ['SUNDAY', 'Domingo']
];
export const diaSemana = dia => diasSemana.find(([valor]) => valor === dia)?.[1] ?? dia;

export function minutosPrevistos(entrada, saida, intervalo = 0) {
    const minutos = hora => {
        if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(hora)) throw new Error('Informe horários válidos com horas e minutos.');
        const [h, m] = hora.split(':').map(Number);
        return h * 60 + m;
    };
    const periodo = (minutos(saida) - minutos(entrada) + 1440) % 1440;
    if (!periodo) throw new Error('Entrada e saída devem ser diferentes.');
    if (!Number.isInteger(intervalo) || intervalo < 0 || intervalo >= periodo) throw new Error('O intervalo deve ser menor que o período da jornada.');
    return periodo - intervalo;
}

export function dataValida(data) {
    return /^\d{4}-\d{2}-\d{2}$/.test(data) && !Number.isNaN(Date.parse(data))
        && new Date(data).toISOString().slice(0, 10) === data;
}

export function jornadaEntrada(valores) {
    const intervaloMinutos = /^\d+$/.test(valores.intervaloMinutos) ? Number(valores.intervaloMinutos) : NaN;
    const toleranciaMinutos = /^\d+$/.test(valores.toleranciaMinutos) ? Number(valores.toleranciaMinutos) : NaN;
    const previstos = minutosPrevistos(valores.entrada, valores.saida, intervaloMinutos);
    if (!diasSemana.some(([dia]) => dia === valores.dia)) throw new Error('Selecione um dia da semana.');
    if (!Number.isInteger(toleranciaMinutos) || toleranciaMinutos < 0 || toleranciaMinutos > previstos + intervaloMinutos) {
        throw new Error('A tolerância deve estar dentro do período da jornada.');
    }
    if (!dataValida(valores.vigenciaInicio) || (valores.vigenciaFim && (!dataValida(valores.vigenciaFim) || valores.vigenciaFim < valores.vigenciaInicio))) {
        throw new Error('Confira o início e o fim da vigência.');
    }
    return { dia: valores.dia, entrada: valores.entrada, saida: valores.saida,
        intervaloMinutos, toleranciaMinutos, vigenciaInicio: valores.vigenciaInicio, vigenciaFim: valores.vigenciaFim || null };
}

export function camposJornada(inicio) {
    return `<label>Dia da semana<select name="dia" required>${diasSemana.map(([valor, nome]) => `<option value="${valor}">${nome}</option>`).join('')}</select></label>
        <div class="form-row"><label>Entrada prevista<input type="time" name="entrada" value="08:00" required step="60"></label><label>Saída prevista<input type="time" name="saida" value="17:00" required step="60"></label></div>
        <div class="form-row"><label>Intervalo (minutos)<input type="number" name="intervaloMinutos" value="60" min="0" step="1" required></label><label>Tolerância de atraso (minutos)<input type="number" name="toleranciaMinutos" value="5" min="0" step="1" required></label></div>
        <div class="form-row"><label>Válida a partir de<input type="date" name="vigenciaInicio" value="${e(inicio)}" required></label><label>Até (opcional)<input type="date" name="vigenciaFim"></label></div>
        <p class="schedule-preview" id="schedule-preview" role="status" aria-live="polite"></p>`;
}
