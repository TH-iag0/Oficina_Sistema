import { moeda } from './ui.js';
import { smoothPath } from './chart-geometry.js';
import { escalaFinanceira, rotuloMes, seriesFinanceiras } from './financeiro.js';
const observadores = new WeakMap();
let sequencia = 0;
const ns = 'http://www.w3.org/2000/svg';
function node(tag, attrs, texto) {
  const el = document.createElementNS(ns, tag);
  for (const [nome, valor] of Object.entries(attrs ?? {})) el.setAttribute(nome, valor);
  if (texto !== undefined) {
    el.textContent = texto;
  }
  return el;
}
function preparar(container, tipo, dados) {
  observadores.get(container)?.disconnect();
  container.replaceChildren();
  const largura = Math.max(340, container.clientWidth - 32),
    altura = 220;
  const series = seriesFinanceiras(tipo, dados);
  const escala = escalaFinanceira(series.flatMap(s => s.valores));
  const svg = node('svg', {
    viewBox: `0 0 ${largura} ${altura}`,
    class: 'finance-svg',
    role: 'group',
    'aria-label': tipo === 'cashflow' ? 'Fluxo de caixa mensal' : 'Faturamento e custos diretos por mês'
  });
  const geo = {
    largura,
    altura,
    esquerda: 62,
    direita: largura - 16,
    topo: 18,
    base: altura - 32,
    y: valor => 18 + (escala.maximo - valor) / (escala.maximo - escala.minimo) * (altura - 50)
  };
  desenharEixos(svg, geo, escala, dados);
  container.append(svg);
  const observer = new ResizeObserver(() => {
    if (Math.abs(Math.max(340, container.clientWidth - 32) - largura) > 1) {
      desenhar(container, tipo, dados);
    }
  });
  observadores.set(container, observer);
  observer.observe(container);
  return {
    svg,
    geo,
    series
  };
}
function abreviar(valor) {
  return Math.abs(valor) >= 1000 ? `R$ ${new Intl.NumberFormat('pt-BR', {
    maximumFractionDigits: 1
  }).format(valor / 1000)} mil` : moeda(valor);
}
function desenharEixos(svg, g, escala, dados) {
  for (let i = 0; i <= Math.round((escala.maximo - escala.minimo) / escala.passo); i++) {
    const valor = escala.minimo + i * escala.passo,
      y = g.y(valor);
    svg.append(node('line', {
      x1: g.esquerda,
      x2: g.direita,
      y1: y,
      y2: y,
      class: valor === 0 ? 'finance-zero' : 'chart-grid'
    }), node('text', {
      x: g.esquerda - 8,
      y: y + 3,
      'text-anchor': 'end',
      class: 'chart-label'
    }, abreviar(valor)));
  }
  dados.forEach((item, i) => {
    if (dados.length > 6 && i % 2 && i !== dados.length - 1) {
      return;
    }
    svg.append(node('text', {
      x: centro(g, dados.length, i),
      y: g.altura - 10,
      'text-anchor': 'middle',
      class: 'chart-label'
    }, rotuloMes(item.mes)));
  });
}
function centro(g, quantidade, indice) {
  return g.esquerda + (g.direita - g.esquerda) * (indice + .5) / quantidade;
}
function alvo(classe, serie, item, valor) {
  const label = `${serie.nome} · ${rotuloMes(item.mes)}: ${moeda(valor)}`;
  const grupo = node('g', {
    class: classe,
    tabindex: 0,
    role: 'button',
    'aria-pressed': false,
    'aria-label': label
  });
  grupo.append(node('title', {}, label));
  return grupo;
}
function linhas(svg, g, series, dados) {
  const origem = g.y(0);
  for (const serie of series) {
    const camada = node('g', {
      class: 'financial-series',
      'aria-label': serie.nome
    });
    const pontos = serie.valores.map((valor, i) => [centro(g, dados.length, i), g.y(valor)]);
    const curva = smoothPath(pontos);
    camada.append(node('path', {
      d: `${curva} L ${pontos.at(-1)[0]} ${origem} L ${pontos[0][0]} ${origem} Z`,
      fill: serie.cor,
      opacity: .055,
      'pointer-events': 'none'
    }), node('path', {
      d: curva,
      fill: 'none',
      stroke: serie.cor,
      'stroke-width': 6,
      opacity: .16,
      transform: 'translate(0 3)',
      class: 'financial-depth',
      'pointer-events': 'none'
    }), node('path', {
      d: curva,
      fill: 'none',
      stroke: serie.cor,
      'stroke-width': 2.5,
      'pointer-events': 'stroke'
    }));
    pontos.forEach(([x, y], i) => {
      const grupo = alvo('financial-point', serie, dados[i], serie.valores[i]);
      grupo.append(node('circle', {
        cx: x,
        cy: y,
        r: 10,
        fill: serie.cor,
        class: 'finance-halo'
      }), node('circle', {
        cx: x,
        cy: y,
        r: 3.5,
        fill: serie.cor,
        stroke: '#d7f5ff',
        'stroke-width': 1.2
      }));
      camada.append(grupo);
    });
    svg.append(camada);
  }
}
function barras(svg, g, series, dados) {
  const faixa = (g.direita - g.esquerda) / dados.length,
    largura = Math.min(23, faixa * .29),
    profundidade = Math.min(5, largura * .22);
  const gradientes = gradientesBarras(svg, series);
  dados.forEach((item, i) => series.forEach((serie, s) => {
    const valor = serie.valores[i],
      y = g.y(valor),
      base = g.y(0);
    const x = centro(g, dados.length, i) + (s - .5) * (largura + 3) - largura / 2;
    const topo = Math.min(base, y),
      altura = Math.abs(base - y);
    const grupo = alvo('financial-bar', serie, item, valor);
    grupo.append(node('rect', {
      x,
      y: topo,
      width: largura,
      height: Math.max(1, altura),
      rx: 2,
      fill: `url(#${gradientes[s]})`
    }));
    if (altura > 0) {
      grupo.append(node('polygon', {
        points: `${x},${topo} ${x + profundidade},${topo - profundidade} ${x + largura + profundidade},${topo - profundidade} ${x + largura},${topo}`,
        fill: serie.cor,
        class: 'financial-top financial-depth'
      }), node('polygon', {
        points: `${x + largura},${topo} ${x + largura + profundidade},${topo - profundidade} ${x + largura + profundidade},${topo + altura - profundidade} ${x + largura},${topo + altura}`,
        fill: serie.cor,
        class: 'financial-side financial-depth'
      }));
    }
    svg.append(grupo);
  }));
}
function gradientesBarras(svg, series) {
  const defs = node('defs');
  svg.append(defs);
  return series.map(serie => {
    const id = `finance-bar-${++sequencia}`;
    const gradiente = node('linearGradient', {
      id,
      x1: 0,
      x2: 1,
      y1: 0,
      y2: 0
    });
    gradiente.append(node('stop', {
      offset: 0,
      'stop-color': serie.cor,
      'stop-opacity': .65
    }), node('stop', {
      offset: .65,
      'stop-color': serie.cor
    }), node('stop', {
      offset: 1,
      'stop-color': serie.cor,
      'stop-opacity': .85
    }));
    defs.append(gradiente);
    return id;
  });
}
function desenhar(container, tipo, dados) {
  const {
    svg,
    geo,
    series
  } = preparar(container, tipo, dados);
  if (tipo === 'cashflow') {
    linhas(svg, geo, series, dados);
  } else {
    barras(svg, geo, series, dados);
  }
}
export const FinanceCharts = {
  cashflow: (container, dados) => desenhar(container, 'cashflow', dados),
  revenue: (container, dados) => desenhar(container, 'revenue', dados),
  destroy(container) {
    observadores.get(container)?.disconnect();
  }
};
