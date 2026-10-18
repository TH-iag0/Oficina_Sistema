import { MonitorCharts } from './lib/onvio-charts.js';
import { escapar } from './lib/ui.js';
import { FinanceCharts } from './lib/financial-charts.js';
import { seriesFinanceiras } from './lib/financeiro.js';
const montados = new Set();
const seletor = '.volume-bar, .line-point, .weekly-day, .onvio-donut-item, .onvio-donut-layer:last-child circle, .financial-point, .financial-bar';
export function modoGrafico(storage) {
  try {
    return (storage ?? globalThis.localStorage)?.getItem('autogestao-graficos') === '2d' ? '2d' : '3d';
  } catch {
    return '3d';
  }
}
export function salvarModoGrafico(storage, modo) {
  if (!['2d', '3d'].includes(modo)) {
    return;
  }
  try {
    (storage ?? globalThis.localStorage)?.setItem('autogestao-graficos', modo);
  } catch {/* Preferência opcional. */}
}
export function graficoMarkup(tipo, dados, unidade = 'count') {
  const _conteudoMarkup = seriesFinanceiras(tipo, []).map(s => {
    return `<span><i style="background:${s.cor}"></i>${s.nome}</span>`;
  }).join('');
  let legenda;
  if (tipo === 'weekly') {
    legenda = '<div class="weekly-legend"><span data-series="received" tabindex="0">ECU</span><span data-series="completed" tabindex="0">TCU</span></div>';
  } else if (['cashflow', 'revenue'].includes(tipo)) {
    legenda = `<div class="finance-chart-legend">${_conteudoMarkup}</div>`;
  } else {
    legenda = '';
  }
  return `${legenda}<div class="ag-chart overview-charts" data-chart="${escapar(tipo)}" data-unit="${escapar(unidade)}" data-values="${escapar(JSON.stringify(dados))}"></div><p class="chart-selection" aria-live="polite">Passe o cursor para explorar. Clique para selecionar um valor.</p>`;
}
export function limparGraficos() {
  for (const container of montados) {
    MonitorCharts.destroy(container);
    FinanceCharts.destroy(container);
  }
  montados.clear();
}
export function montarGraficos(root, modo) {
  for (const container of root.querySelectorAll('[data-chart]')) {
    container.classList.toggle('chart-flat', modo === '2d');
    const valores = JSON.parse(container.dataset.values);
    if (!valores.length || container.dataset.chart === 'donut' && !valores.some(v => v.value > 0)) {
      container.textContent = 'Sem dados no período selecionado.';
      container.classList.add('chart-empty');
      continue;
    }
    if (container.dataset.chart === 'weekly') {
      container.parentElement.classList.add('weekly-panel');
    }
    if (container.dataset.chart === 'donut') {
      container.classList.add('status-chart');
    }
    (FinanceCharts[container.dataset.chart] ?? MonitorCharts[container.dataset.chart])?.(container, valores);
    montados.add(container);
  }
}
export function selecionarGrafico(event) {
  const alvo = event.target.closest?.(seletor);
  const container = alvo?.closest('[data-chart]');
  if (!container) {
    return false;
  }
  if (event.type === 'keydown' && !['Enter', ' ', 'Escape'].includes(event.key)) {
    return false;
  }
  if (event.type === 'keydown') {
    event.preventDefault();
  }
  const ativar = event.key !== 'Escape' && !alvo.classList.contains('is-selected');
  for (const node of container.querySelectorAll('.is-selected')) {
    node.classList.remove('is-selected');
    node.setAttribute('aria-pressed', 'false');
  }
  alvo.classList.toggle('is-selected', ativar);
  alvo.setAttribute('aria-pressed', String(ativar));
  const legenda = container.nextElementSibling;
  if (legenda?.classList.contains('chart-selection')) {
    legenda.textContent = ativar ? `Selecionado: ${alvo.getAttribute('aria-label') || alvo.textContent}` : 'Passe o cursor para explorar. Clique para selecionar um valor.';
  }
  return true;
}
