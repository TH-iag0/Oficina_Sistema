// Modelos SVG reutilizados de EXTRA-O_DADOS_ONVIO/static/monitor-charts.js.
// Mantém camadas da rosca, faces das barras, curvas e paleta do projeto de referência.
import * as OnvioSyncChart from './chart-geometry.js';
let sequence = 0;
function formatValue(container, value) {
  return new Intl.NumberFormat('pt-BR', container.dataset.unit === 'money' ? {
    style: 'currency',
    currency: 'BRL',
    maximumFractionDigits: 2
  } : {
    maximumFractionDigits: 1
  }).format(value);
}
function uniqueIds(container) {
  const suffix = '-ag-' + ++sequence;
  const ids = new Map();
  container.querySelectorAll('[id]').forEach(node => {
    ids.set(node.id, node.id + suffix);
    node.id += suffix;
  });
  container.querySelectorAll('[fill], [filter], [stroke]').forEach(node => {
    for (const attr of ['fill', 'filter', 'stroke']) {
      const value = node.getAttribute(attr);
      if (value?.startsWith('url(#')) {
        const id = value.slice(5, -1);
        if (ids.has(id)) {
          node.setAttribute(attr, 'url(#' + ids.get(id) + ')');
        }
      }
    }
  });
}
/* Adaptado de Analise-de-faturamento/front_end/src/components/Donut3D.tsx
 * e Revenue3D.tsx: segmentos pathLength e barras com faces e profundidade. */
export const MonitorCharts = (() => {
  const ns = 'http://www.w3.org/2000/svg';
  const n = (tag, attrs, text) => {
    const e = document.createElementNS(ns, tag);
    Object.entries(attrs ?? {}).forEach(([k, v]) => e.setAttribute(k, v));
    if (text !== undefined) {
      e.textContent = text;
    }
    return e;
  };
  const barObservers = new WeakMap(),
    lineObservers = new WeakMap(),
    legendEvents = new WeakMap();
  // Camadas, perspectiva e paleta adaptadas de ClientDonut3D.tsx.
  function donut(container, items) {
    const total = items.reduce((s, i) => s + i.value, 0);
    if (!total) {
      return;
    }
    const div = (cls, text) => {
      const e = document.createElement('div');
      e.className = cls;
      if (text !== undefined) {
        e.textContent = text;
      }
      return e;
    };
    const plot = div('onvio-donut-plot'),
      scene = div('onvio-donut-scene');
    plot.append(scene);
    const tones = [['#7dd3fc', '#3b82f6', '#1d4ed8', '#082a78'], ['#5eead4', '#14b8a6', '#0f766e', '#075b57'], ['#fde68a', '#f59e0b', '#b45309', '#78350f'], ['#f0abfc', '#c026d3', '#86198f', '#581c87'], ['#c4b5fd', '#8b5cf6', '#6d28d9', '#4c1d95'], ['#94a3b8', '#475569', '#1e293b', '#0f172a'], ['#bef264', '#65a30d', '#3f6212', '#1a2e05'], ['#fda4af', '#f43f5e', '#be123c', '#881337']];
    let offset = 0;
    const parts = items.filter(i => i.value > 0).sort((a, b) => b.value - a.value).map((item, i) => {
      const p = item.value / total * 100;
      const result = {
        ...item,
        p,
        offset,
        length: Math.max(p - Math.min(1.25, p * .15), .001),
        tone: tones[i % tones.length]
      };
      offset += p;
      return result;
    });
    const slices = [],
      rows = [];
    const tooltip = div('donut-hover-tip');
    tooltip.hidden = true;
    tooltip.setAttribute('role', 'status');
    const reset = () => {
      slices.forEach(({
        node
      }) => node.classList.remove('is-muted', 'is-active'));
      rows.forEach(row => row.classList.remove('is-muted', 'is-active'));
      tooltip.hidden = true;
      summary.replaceChildren(div('', 'TOTAL'), div('onvio-donut-number', formatValue(container, total)));
    };
    const activate = index => {
      const p = parts[index];
      slices.forEach(({
        node,
        index: i
      }) => {
        node.classList.toggle('is-muted', i !== index);
        node.classList.toggle('is-active', i === index);
      });
      rows.forEach((row, i) => {
        row.classList.toggle('is-muted', i !== index);
        row.classList.toggle('is-active', i === index);
      });
      const percent = p.p.toFixed(2).replace('.', ',') + '%';
      summary.replaceChildren(div('', p.name), div('onvio-donut-number', formatValue(container, p.value)), div('', percent));
      tooltip.replaceChildren(div('tip-name', p.name), div('', formatValue(container, p.value) + ' · ' + percent));
      tooltip.hidden = false;
      tooltip.style.left = '12px';
      tooltip.style.top = '12px';
    };
    const position = event => {
      const rect = container.getBoundingClientRect();
      tooltip.style.left = Math.max(8, Math.min(event.clientX - rect.left + 14, container.clientWidth - tooltip.offsetWidth - 8)) + 'px';
      tooltip.style.top = Math.max(8, Math.min(event.clientY - rect.top + 14, container.clientHeight - tooltip.offsetHeight - 8)) + 'px';
    };
    const summary = div('onvio-donut-summary');
    summary.append(div('', 'TOTAL'), div('onvio-donut-number', formatValue(container, total)));
    plot.append(summary);
    [22, 15, 8, 0].forEach((depth, layer) => {
      const svg = n('svg', {
        viewBox: '0 0 240 240',
        class: 'onvio-donut-layer',
        'data-depth': depth,
        style: 'transform:translateY(' + depth + 'px) rotateX(56deg);' + (depth ? 'filter:brightness(.72);' : ''),
        'aria-hidden': 'true'
      });
      const defs = n('defs');
      svg.append(defs);
      parts.forEach((p, i) => {
        const id = 'status-gradient-' + layer + '-' + i;
        const grad = n('linearGradient', {
          id,
          x1: 0,
          y1: 0,
          x2: 1,
          y2: 1
        });
        [0, .42, 1].forEach((stop, j) => grad.append(n('stop', {
          offset: stop,
          'stop-color': p.tone[j]
        })));
        defs.append(grad);
        const circle = n('circle', {
          cx: 120,
          cy: 120,
          r: 78,
          pathLength: 100,
          fill: 'none',
          stroke: depth ? p.tone[3] : 'url(#' + id + ')',
          'stroke-width': 43,
          'stroke-dasharray': p.length + ' ' + (100 - p.length),
          'stroke-dashoffset': -p.offset,
          transform: 'rotate(-90 120 120)'
        });
        slices.push({
          node: circle,
          index: i
        });
        circle.append(n('title', {}, p.name + ': ' + p.value + ' (' + p.p.toFixed(2) + '%)'));
        if (!depth) {
          circle.setAttribute('tabindex', '0');
          circle.setAttribute('role', 'button');
          circle.setAttribute('aria-pressed', 'false');
          circle.setAttribute('aria-label', p.name + ': ' + p.value);
          svg.removeAttribute('aria-hidden');
          const show = () => activate(i);
          circle.addEventListener('pointerenter', show);
          circle.addEventListener('pointermove', position);
          circle.addEventListener('pointerleave', reset);
          circle.addEventListener('focus', show);
          circle.addEventListener('blur', reset);
          circle.addEventListener('keydown', event => {
            if (event.key === 'Escape') {
              reset();
            }
          });
        }
        svg.append(circle);
      });
      scene.append(svg);
    });
    const legend = div('onvio-donut-legend');
    parts.forEach((p, i) => {
      const row = div('onvio-donut-item'),
        dot = div('onvio-donut-dot');
      dot.style.background = p.tone[1];
      const text = div('');
      text.append(div('onvio-donut-name', p.name), div('', p.p.toFixed(2).replace('.', ',') + '%'), div('onvio-donut-value', formatValue(container, p.value)));
      row.append(dot, text);
      rows.push(row);
      row.tabIndex = 0;
      row.setAttribute('role', 'button');
      row.setAttribute('aria-pressed', 'false');
      row.setAttribute('aria-label', p.name + ': ' + formatValue(container, p.value));
      row.addEventListener('pointerenter', () => activate(i));
      row.addEventListener('pointermove', position);
      row.addEventListener('pointerleave', reset);
      row.addEventListener('focus', () => activate(i));
      row.addEventListener('blur', reset);
      row.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
          reset();
        }
      });
      legend.append(row);
    });
    container.append(plot, legend, tooltip);
    uniqueIds(container);
  }
  function bars(container, days) {
    barObservers.get(container)?.disconnect();
    container.replaceChildren();
    if (!days.length) {
      container.append(Object.assign(document.createElement('p'), {
        className: 'chart-empty',
        textContent: 'Sem no período.'
      }));
      return;
    }
    const width = Math.max(360, container.clientWidth - 40),
      left = 133,
      right = 38,
      top = 22,
      row = 34,
      depth = 7,
      height = top + days.length * row + 18;
    const max = OnvioSyncChart.axisMax(Math.max(...days.map(d => d.records), 1));
    const svg = n('svg', {
      viewBox: `0 0 ${width} ${height}`,
      role: 'group',
      'aria-label': 'Distribuição por categoria',
      class: 'volume-svg department-bars-svg'
    });
    const defs = n('defs');
    [['blue', '#123a9f', '#2563eb', '#60a5fa'], ['teal', '#0b6e68', '#14b8a6', '#5eead4'], ['orange', '#b45309', '#f59e0b', '#fde68a']].forEach(([tone, start, middle, end]) => {
      const grad = n('linearGradient', {
        id: 'volume-' + tone,
        x1: 0,
        y1: 0,
        x2: 1,
        y2: 0
      });
      grad.append(n('stop', {
        offset: 0,
        'stop-color': start
      }), n('stop', {
        offset: .48,
        'stop-color': middle
      }), n('stop', {
        offset: 1,
        'stop-color': end
      }));
      defs.append(grad);
    });
    const shadow = n('filter', {
      id: 'volume-shadow',
      x: '-80%',
      y: '-30%',
      width: '260%',
      height: '180%'
    });
    shadow.append(n('feDropShadow', {
      dx: 5,
      dy: 7,
      stdDeviation: 4,
      'flood-color': '#172b55',
      'flood-opacity': '.3'
    }));
    defs.append(shadow);
    svg.append(defs);
    days.forEach((day, i) => {
      const y = top + i * row,
        w = Math.max(0, (width - left - right - depth - 8) * day.records / max),
        label = day.label || day.key;
      const tone = ['blue', 'teal', 'orange'][i % 3],
        dark = {
          blue: '#123a9f',
          teal: '#075b57',
          orange: '#78350f'
        }[tone],
        light = {
          blue: '#93c5fd',
          teal: '#5eead4',
          orange: '#fde68a'
        }[tone];
      const g = n('g', {
        tabindex: 0,
        role: 'button',
        'aria-pressed': false,
        'aria-label': `${label}: ${formatValue(container, day.records)}`,
        class: 'volume-bar department-bar'
      });
      g.append(n('title', {}, `${label}: ${formatValue(container, day.records)}`));
      svg.append(n('text', {
        x: left - 10,
        y: y + 14,
        'text-anchor': 'end',
        class: 'chart-label department-label'
      }, label), n('line', {
        x1: left,
        y1: y + row - 2,
        x2: width - right,
        y2: y + row - 2,
        class: 'chart-grid'
      }));
      if (w > 0) {
        g.setAttribute('filter', 'url(#volume-shadow)');
        g.append(n('rect', {
          x: left,
          y,
          width: w,
          height: 18,
          rx: 2,
          fill: `url(#volume-${tone})`
        }), n('polygon', {
          points: `${left + w},${y} ${left + w + depth},${y - depth} ${left + w + depth},${y + 18 - depth} ${left + w},${y + 18}`,
          fill: dark
        }), n('polygon', {
          points: `${left},${y} ${left + depth},${y - depth} ${left + w + depth},${y - depth} ${left + w},${y}`,
          fill: light
        }));
      }
      g.append(n('text', {
        x: left + w + depth + 7,
        y: y + 13,
        'text-anchor': 'start',
        class: 'chart-label department-value'
      }, formatValue(container, day.records)));
      g.addEventListener('pointerenter', () => {
        svg.classList.add('has-hover');
        g.classList.add('is-active');
      });
      g.addEventListener('pointerleave', () => {
        svg.classList.remove('has-hover');
        g.classList.remove('is-active');
      });
      g.addEventListener('focus', () => {
        svg.classList.add('has-hover');
        g.classList.add('is-active');
      });
      g.addEventListener('blur', () => {
        svg.classList.remove('has-hover');
        g.classList.remove('is-active');
      });
      svg.append(g);
    });
    container.append(svg);
    uniqueIds(container);
    const observer = new ResizeObserver(() => {
      if (Math.abs(Math.max(360, container.clientWidth - 40) - width) > 1) {
        bars(container, days);
      }
    });
    barObservers.set(container, observer);
    observer.observe(container);
  }
  function line(container, days) {
    lineObservers.get(container)?.disconnect();
    container.replaceChildren();
    if (!days.length) {
      container.append(Object.assign(document.createElement('p'), {
        className: 'chart-empty',
        textContent: 'Sem dados no período.'
      }));
      return;
    }
    const width = Math.max(360, container.clientWidth - 40),
      left = 100,
      right = 18,
      top = 52,
      base = 210,
      plotW = width - left - right,
      plotH = base - top;
    const max = OnvioSyncChart.axisMax(Math.max(...days.map(d => d.records), 1)),
      x = i => left + (days.length === 1 ? plotW / 2 : i * plotW / (days.length - 1)),
      y = d => base - d.records / max * plotH;
    const svg = n('svg', {
        viewBox: `0 0 ${width} 252`,
        role: 'group',
        'aria-label': 'Evolução do faturamento por período',
        class: 'volume-svg'
      }),
      defs = n('defs');
    const area = n('linearGradient', {
      id: 'records-area',
      x1: 0,
      y1: 0,
      x2: 0,
      y2: 1
    });
    area.append(n('stop', {
      offset: 0,
      'stop-color': '#38bdf8',
      'stop-opacity': '.24'
    }), n('stop', {
      offset: 1,
      'stop-color': '#38bdf8',
      'stop-opacity': '.015'
    }));
    defs.append(area);
    const glow = n('filter', {
      id: 'records-glow',
      x: '-40%',
      y: '-80%',
      width: '180%',
      height: '260%'
    });
    glow.append(n('feGaussianBlur', {
      stdDeviation: 4,
      result: 'blur'
    }));
    defs.append(glow);
    svg.append(defs);
    for (let i = 0; i <= 4; i++) {
      const gy = base - i / 4 * plotH;
      svg.append(n('line', {
        x1: left,
        y1: gy,
        x2: width - right,
        y2: gy,
        class: 'chart-grid'
      }), n('text', {
        x: left - 10,
        y: gy + 4,
        'text-anchor': 'end',
        class: 'chart-label'
      }, Math.round(max * i / 4).toLocaleString('pt-BR')));
    }
    const pts = days.map((d, i) => [x(i), y(d)]),
      path = OnvioSyncChart.smoothPath(pts),
      areaPath = `${path} L ${pts.at(-1)[0]} ${base} L ${pts[0][0]} ${base} Z`;
    svg.append(n('path', {
      d: areaPath,
      fill: 'url(#records-area)'
    }), n('path', {
      d: path,
      fill: 'none',
      stroke: '#38bdf8',
      'stroke-width': 8,
      opacity: .2,
      filter: 'url(#records-glow)'
    }), n('path', {
      d: path,
      fill: 'none',
      stroke: '#23adff',
      'stroke-width': 4,
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round'
    }));
    let maxIndex = 0,
      minIndex = 0;
    days.forEach((day, index) => {
      if (day.records > days[maxIndex].records) {
        maxIndex = index;
      }
      if (day.records < days[minIndex].records) {
        minIndex = index;
      }
    });
    days.forEach((day, i) => {
      const destaque = rotuloExtremo(i, maxIndex, minIndex);
      const sufixo = destaque ? ' · ' + destaque : '';
      const [px, py] = pts[i],
        label = day.label || new Intl.DateTimeFormat('pt-BR', {
          month: 'short',
          timeZone: 'UTC'
        }).format(new Date(day.key.slice(0, 7) + '-01T00:00:00Z')).replace('.', ''),
        g = n('g', {
          tabindex: 0,
          role: 'button',
          'aria-pressed': false,
          'aria-label': `${label}: ${formatValue(container, day.records)}${sufixo.toLowerCase()}`,
          class: 'line-point'
        });
      g.append(n('title', {}, `${label}: ${formatValue(container, day.records)}${sufixo}`), n('circle', {
        cx: px,
        cy: py,
        r: 9,
        fill: '#2385ff',
        opacity: .14,
        class: 'point-halo'
      }), n('circle', {
        cx: px,
        cy: py,
        r: 4.5,
        fill: '#fff',
        stroke: '#2185ff',
        'stroke-width': 3,
        class: 'point-dot'
      }));
      const labels = OnvioSyncChart.lineLabelLayout(py);
      if (days.length <= 12) {
        g.append(n('text', {
          x: px,
          y: labels.valueY,
          'text-anchor': i === days.length - 1 && days.length > 1 ? 'end' : 'middle',
          class: 'chart-label',
          'font-weight': '700'
        }, formatValue(container, day.records)));
      }
      if (i === maxIndex || i === minIndex) {
        g.append(n('rect', {
          x: px - 18,
          y: labels.badgeTop,
          width: 36,
          height: labels.badgeHeight,
          rx: 8,
          fill: '#235be8',
          class: 'extreme-badge'
        }), n('text', {
          x: px,
          y: labels.badgeY,
          'text-anchor': 'middle',
          fill: '#fff',
          class: 'extreme-label'
        }, i === maxIndex ? 'Máx.' : 'Mín.'));
        g.classList.add(i === maxIndex ? 'is-maximum' : 'is-minimum');
      }
      g.addEventListener('focus', () => g.classList.add('is-active'));
      g.addEventListener('blur', () => g.classList.remove('is-active'));
      svg.append(g);
      if (i === 0 || i === days.length - 1 || i % Math.ceil(days.length / 6) === 0) {
        svg.append(n('text', {
          x: px,
          y: base + 24,
          'text-anchor': 'middle',
          class: 'chart-label'
        }, label));
      }
    });
    container.append(svg);
    uniqueIds(container);
    const observer = new ResizeObserver(() => {
      if (Math.abs(Math.max(360, container.clientWidth - 40) - width) > 1) {
        line(container, days);
      }
    });
    lineObservers.set(container, observer);
    observer.observe(container);
  }
  function weekly(container, days) {
    lineObservers.get(container)?.disconnect();
    container.replaceChildren();
    if (!days.length) {
      container.append(Object.assign(document.createElement('p'), {
        className: 'chart-empty',
        textContent: 'Sem atividade registrada no período.'
      }));
      return;
    }
    const width = Math.max(360, container.clientWidth - 40),
      left = 45,
      right = 12,
      top = 24,
      base = 330,
      plotH = base - top,
      plotW = width - left - right,
      step = plotW / days.length,
      max = OnvioSyncChart.axisMax(Math.max(1, ...days.flatMap(day => [day.received, day.completed])));
    const svg = n('svg', {
        viewBox: `0 0 ${width} 372`,
        role: 'group',
        'aria-label': 'Remaps ECU e TCU por mês',
        class: 'volume-svg weekly-svg'
      }),
      defs = n('defs');
    [['weekly-blue', '#60a5fa', '#3b82f6', '#2563eb'], ['weekly-teal', '#5eead4', '#10b981', '#047857']].forEach(([id, light, middle, dark]) => {
      const grad = n('linearGradient', {
        id,
        x1: 0,
        y1: 0,
        x2: 1,
        y2: 0
      });
      grad.append(n('stop', {
        offset: 0,
        'stop-color': dark
      }), n('stop', {
        offset: .5,
        'stop-color': middle
      }), n('stop', {
        offset: 1,
        'stop-color': light
      }));
      defs.append(grad);
    });
    const shadow = n('filter', {
      id: 'weekly-shadow',
      x: '-60%',
      y: '-30%',
      width: '220%',
      height: '180%'
    });
    shadow.append(n('feDropShadow', {
      dx: 4,
      dy: 5,
      stdDeviation: 3,
      'flood-color': '#172b55',
      'flood-opacity': '.28'
    }));
    defs.append(shadow);
    svg.append(defs);
    const positive = days.flatMap(day => [day.received, day.completed]).filter(value => value > 0);
    const compressed = positive.length > 1 && Math.max(...positive) / Math.min(...positive) >= 20;
    const scale = value => compressed ? Math.log10(1 + value) / Math.log10(1 + max) : value / max;
    const ticks = compressed ? [0, ...Array.from({
      length: Math.ceil(Math.log10(max))
    }, (_, i) => 10 ** i), max].filter((value, i, values) => value <= max && values.indexOf(value) === i) : Array.from({
      length: 5
    }, (_, i) => max * i / 4);
    ticks.forEach(value => {
      const y = base - scale(value) * plotH;
      svg.append(n('line', {
        x1: left,
        y1: y,
        x2: width - right,
        y2: y,
        class: 'chart-grid'
      }), n('text', {
        x: left - 8,
        y: y + 4,
        'text-anchor': 'end',
        class: 'chart-label'
      }, Math.round(value).toLocaleString('pt-BR')));
    });
    days.forEach((day, i) => {
      const center = left + i * step + step / 2,
        bw = Math.min(24, step * .24),
        gap = 10,
        depth = 6,
        x1 = center - bw - gap / 2,
        x2 = center + gap / 2,
        h1 = scale(day.received) * plotH,
        h2 = scale(day.completed) * plotH;
      const g = n('g', {
        class: 'weekly-day',
        tabindex: 0,
        role: 'button',
        'aria-pressed': false,
        'aria-label': `${day.label}: ${day.received} ECU, ${day.completed} TCU`
      });
      g.append(n('title', {}, `${day.label}: ${day.received} ECU · ${day.completed} TCU`));
      const addBar = (x, h, series, color) => {
        if (h <= 0) {
          return;
        }
        const y = base - h,
          front = n('rect', {
            x,
            y,
            width: bw,
            height: h,
            rx: 2,
            fill: `url(#weekly-${color})`,
            class: `weekly-bar weekly-${series}`
          }),
          side = n('polygon', {
            points: `${x + bw},${y} ${x + bw + depth},${y - depth} ${x + bw + depth},${base - depth} ${x + bw},${base}`,
            fill: color === 'blue' ? '#123a9f' : '#075b57',
            class: `weekly-bar weekly-${series}`
          }),
          topFace = n('polygon', {
            points: `${x},${y} ${x + depth},${y - depth} ${x + bw + depth},${y - depth} ${x + bw},${y}`,
            fill: color === 'blue' ? '#93c5fd' : '#5eead4',
            class: `weekly-bar weekly-${series}`
          });
        [front, side, topFace].forEach(node => node.setAttribute('filter', 'url(#weekly-shadow)'));
        g.append(front, side, topFace);
      };
      addBar(x1, h1, 'received', 'blue');
      addBar(x2, h2, 'completed', 'teal');
      [[x1, h1, day.received], [x2, h2, day.completed]].forEach(([x, h, value]) => {
        if (value > 0) {
          g.append(n('text', {
            x: x + bw / 2,
            y: base - h - 12,
            'text-anchor': 'middle',
            class: 'chart-label'
          }, formatValue(container, value)));
        }
      });
      g.append(n('text', {
        x: center,
        y: base + 22,
        'text-anchor': 'middle',
        class: 'chart-label'
      }, day.label));
      svg.append(g);
    });
    const setSeriesFocus = series => {
      svg.classList.add('has-series-hover');
      svg.dataset.activeSeries = series;
    };
    const clearSeriesFocus = () => {
      svg.classList.remove('has-series-hover');
      delete svg.dataset.activeSeries;
    };
    svg.querySelectorAll('.weekly-received').forEach(node => {
      node.addEventListener('pointerenter', () => setSeriesFocus('received'));
      node.addEventListener('pointerleave', clearSeriesFocus);
      node.addEventListener('focus', () => setSeriesFocus('received'));
      node.addEventListener('blur', clearSeriesFocus);
    });
    svg.querySelectorAll('.weekly-completed').forEach(node => {
      node.addEventListener('pointerenter', () => setSeriesFocus('completed'));
      node.addEventListener('pointerleave', clearSeriesFocus);
      node.addEventListener('focus', () => setSeriesFocus('completed'));
      node.addEventListener('blur', clearSeriesFocus);
    });
    const legend = container.closest('.weekly-panel')?.querySelector('.weekly-legend');
    legendEvents.get(container)?.abort();
    const events = new AbortController();
    legendEvents.set(container, events);
    legend?.querySelectorAll('[data-series]').forEach(item => {
      const focus = () => setSeriesFocus(item.dataset.series),
        options = {
          signal: events.signal
        };
      item.addEventListener('pointerenter', focus, options);
      item.addEventListener('pointerleave', clearSeriesFocus, options);
      item.addEventListener('focus', focus, options);
      item.addEventListener('blur', clearSeriesFocus, options);
    });
    const note = document.createElement('p');
    note.className = 'weekly-scale-note';
    note.textContent = compressed ? 'Escala logarítmica (1 + quantidade) para comparar a carga inicial com volumes menores. Os números nas barras mostram as quantidades reais.' : 'Escala linear · quantidades reais. Quantidade de serviços por mês.';
    container.append(svg, note);
    uniqueIds(container);
    const observer = new ResizeObserver(() => {
      if (Math.abs(Math.max(360, container.clientWidth - 40) - width) > 1) {
        weekly(container, days);
      }
    });
    lineObservers.set(container, observer);
    observer.observe(container);
  }
  return {
    donut,
    bars,
    line,
    weekly,
    destroy(container) {
      barObservers.get(container)?.disconnect();
      lineObservers.get(container)?.disconnect();
      legendEvents.get(container)?.abort();
    }
  };
})();
function rotuloExtremo(indice, maximo, minimo) {
  if (indice === maximo) return 'Máximo';
  if (indice === minimo) return 'Mínimo';
  return '';
}
