import { listaCadastro } from './catalogos.js';
import { graficoMarkup } from './charts.js';
import { diasSemana, diaSemana } from './lib/jornada.js';
import { linkPonto } from './lib/ponto.js';
import { moeda, numero, percentual, escapar as e, rotulo, horas } from './lib/ui.js';
import { variacaoFinanceira, rotuloMes } from './lib/financeiro.js';
import { filtroOrdens, pesquisaOrdens, resultadoOrdens, paginacaoOrdens } from './ordens-lista.js';

export const paginas = {
    geral: ['Visão geral', 'Sua oficina, em perspectiva.', 'dashboard'],
    operacao: ['Ordens de serviço', 'Cada etapa da oficina, em um só lugar.', 'orders'],
    clientes: ['Clientes e veículos', 'Conheça quem confia na sua oficina.', 'people'],
    servicos: ['Serviços', 'Seu catálogo de manutenção e performance.', 'tool'],
    tecnico: ['Remap e diagnóstico', 'Performance e precisão em números.', 'pulse'],
    financeiro: ['Financeiro', 'Acompanhe receitas, custos e resultados.', 'wallet'],
    ponto: ['Funcionários e ponto', 'Uma visão clara da jornada de trabalho.', 'clock']
};
const paths = {
    dashboard: '<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/>',
    orders: '<rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 3h6v4H9zM9 12h6m-6 4h6"/>',
    people: '<circle cx="9" cy="8" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3m2-16a3 3 0 0 1 0 6m1 3a5 5 0 0 1 3 5v2"/>',
    tool: '<path d="M14 6a6 6 0 0 0-8 8l-3 3a2 2 0 0 0 4 4l3-3a6 6 0 0 0 8-8l-4 4-4-4z"/>',
    pulse: '<path d="M2 12h5l3-8 4 16 3-8h5"/>',
    wallet: '<rect x="3" y="5" width="18" height="15" rx="3"/><path d="M3 8h18m0 5h-6v4h6M6 5V3h12"/>',
    clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    arrow: '<path d="M5 17L17 5M5 5h12v12"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    connect: '<path d="M8 3v5m8-5v5M5 8h14v4a7 7 0 0 1-14 0zM12 19v3"/>',
    car: '<path d="M5 10l2-6h10l2 6M3 10h18v8H3zM5 18v3m14-3v3M6 14h2m8 0h2"/>'
};
export const icone = nome => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[nome] ?? paths.dashboard}</svg>`;
export const vazio = (texto = 'Os registros aparecerão aqui.') => `<div class="empty">${icone('orders')}<strong>Nenhum registro por enquanto</strong><p>${e(texto)}</p></div>`;
const card = (titulo, valor, nota, icon, tone = '') => `<article class="metric ${tone}"><span class="metric-icon">${icone(icon)}</span><div class="metric-content"><div class="metric-top">${e(titulo)}</div><strong>${e(valor)}</strong><small>${e(nota)}</small></div></article>`;
const iconesPainel = {
    'Evolução do faturamento': 'pulse', 'Serviços mais executados': 'tool',
    'Ordens da oficina': 'orders', 'Clientes': 'people', 'Veículos': 'car',
    'Catálogo de serviços': 'tool', 'Remaps por mês': 'pulse', 'Marcas atendidas': 'car',
    'Manutenção e diagnóstico': 'tool', 'Receita, custos e resultado': 'wallet',
    'Despesas da oficina': 'wallet', 'Funcionários': 'people', 'Jornada de trabalho': 'clock',
    'Resumo de ponto': 'clock', 'Extrato bancário · demonstração': 'wallet'
};
const panel = (titulo, subtitulo, content, extra = '', icon = iconesPainel[titulo] ?? 'orders') => `<section class="panel"><div class="panel-heading"><div class="panel-title"><span class="panel-icon">${icone(icon)}</span><div><h2>${e(titulo)}</h2>${subtitulo ? `<p>${e(subtitulo)}</p>` : ''}</div></div>${extra}</div>${content}</section>`;
const acao = (tipo, nome) => `<button class="button primary" data-form="${tipo}">${icone('plus')}${nome}</button>`;
export const badge = (status, texto = rotulo(status)) => `<span class="badge ${e(status)}"><i></i>${e(texto)}</span>`;

function tabela(colunas, linhas) {
    if (!linhas.length) return vazio();
    return `<div class="table-scroll"><table><thead><tr>${colunas.map(c => `<th>${e(c)}</th>`).join('')}</tr></thead><tbody>${linhas.map(row => `<tr>${row.map(cell => `<td>${cell}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`;
}

function grafico(evolucao, campo = 'receita') {
    return graficoMarkup('line', evolucao.map(m => ({
        key: m.mes, label: new Date(m.mes + '-02T00:00:00Z').toLocaleDateString('pt-BR', { month: 'short', timeZone: 'UTC' }), records: Number(m[campo])
    })), 'money');
}

function barras(registros, campo, nome) {
    return graficoMarkup('bars', registros.map(m => ({ label: m[nome], records: Number(m[campo]) })));
}

function distribuicao(d) {
    return graficoMarkup('donut', Object.entries(d.dashboard.ordensPorStatus).map(([nome, value]) => ({ name: rotulo(nome), value: Number(value) })));
}

function remaps(evolucao) {
    return graficoMarkup('weekly', evolucao.map(m => ({ label: new Date(m.mes + '-02T00:00:00Z').toLocaleDateString('pt-BR', { month: 'short', timeZone: 'UTC' }), received: Number(m.remapsEcu), completed: Number(m.remapsTcu) })));
}

export function estrutura(estado) {
    const pagina = paginas[estado.pagina];
    return `<aside class="sidebar"><a class="brand" href="/" aria-label="AutoGestão início"><span class="brand-symbol">A<span>↗</span></span><div>AutoGestão<small>LOCAL · OFICINA</small></div></a><div class="workspace"><span class="workspace-icon">${icone('tool')}</span><div>Sua oficina<small>Painel de gestão</small></div><span class="live-dot"></span></div><div class="nav-label">PRINCIPAL</div><nav aria-label="Principal">${Object.entries(paginas).map(([key, [label, , icon]]) => `<button data-page="${key}" class="nav-item ${key === estado.pagina ? 'active' : ''}" ${key === estado.pagina ? 'aria-current="page"' : ''}>${icone(icon)}${label}</button>`).join('')}</nav><div class="sidebar-bottom"><div class="local-note">${icone('connect')}<div>Gestão local<small>Seus dados na sua oficina</small></div></div><button class="profile" data-connect><span class="avatar">P</span><span>Proprietário<small>${estado.modo === 'real' ? 'Acesso conectado' : 'Prévia do sistema'}</small></span><span>⋯</span></button></div></aside><main><header class="topbar"><span>Oficina <span class="crumb">/</span> <strong>${pagina[0]}</strong></span><div><span class="connection ${estado.modo}"><i></i>${estado.modo === 'real' ? 'Backend conectado' : 'Modo demonstrativo'}</span><button class="button small" data-demo>Demonstração</button><button class="button small" data-connect>Dados reais${icone('connect')}</button></div></header><div class="content"><div class="page-heading"><div><span class="eyebrow">AUTOGESTÃO LOCAL</span><h1>${pagina[0]}</h1><p>${pagina[1]}</p></div><div class="view-options"><div class="chart-toggle" role="group" aria-label="Visualização dos gráficos"><button class="button small" data-chart-mode="2d" aria-pressed="${estado.graficoModo === '2d'}">2D</button><button class="button small" data-chart-mode="3d" aria-pressed="${estado.graficoModo !== '2d'}">3D</button></div><div class="period"><label for="month">Período</label><input id="month" type="month" value="${estado.mes}" aria-label="Mês dos indicadores"></div></div></div><div class="demo-note">${estado.modo === 'real' ? 'Dados reais da API · Spring Boot' : 'PRÉVIA · Dados fictícios para visualizar o layout. Inicie o backend para usar sua oficina.'}<button class="text-button" data-refresh>Atualizar ↻</button></div><div id="page-content">${estado.carregando ? '<div class="loading">Carregando dados da oficina…</div>' : estado.erro ? `<div class="error-box" role="alert"><strong>Não foi possível carregar os dados</strong><p>${e(estado.erro)}</p><button class="button" data-refresh>Tentar novamente</button></div>` : conteudo(estado)}</div><footer>AutoGestão Local <span>Gestão que acompanha o ritmo da sua oficina.</span></footer></div></main>`;
}

function ordensTabela(d, limite = d.ordens.length) {
    return tabela(['Ordem / serviço', 'Veículo', 'Cliente', 'Situação', 'Valor', 'Ações'], d.ordens.slice(0, limite).map(o => {
        const v = d.veiculos.find(v => v.id === o.veiculoId), c = d.clientes.find(c => c.id === o.clienteId);
        return [`<strong>#${e(o.numero)}</strong><small>${e(o.itens[0]?.descricao ?? 'Sem itens')}</small>`, `${e(v ? v.marca + ' ' + v.modelo : o.veiculoId)}<small>${e(v?.placa)}</small>`, e(c?.nome ?? o.clienteId), badge(o.status), `<strong>${moeda(o.total)}</strong>`, `<button class="text-button order-action" data-atendimento="${e(o.id)}" aria-label="Acompanhar ordem #${e(o.numero)}" title="Abrir atendimento da ordem #${e(o.numero)}">${icone('arrow')}<span>Acompanhar ordem →</span></button>`];
    }));
}

function geral(d) {
    const b = d.dashboard, serie = d.indicadores.evolucao;
    return `<div class="metrics">${card('Faturamento do mês', moeda(b.faturamento), percentual(b.variacaoPercentual) + ' vs. mês anterior', 'wallet', b.variacaoPercentual >= 0 ? 'positive' : 'negative')}${card('Ordens concluídas', numero(b.ordensConcluidas), 'Serviços finalizados no período', 'orders')}${card('Ticket médio', moeda(b.ticketMedio), 'Por ordem concluída', 'arrow')}${card('Lucro bruto', moeda(b.lucroBruto), b.margemPercentual == null ? 'Margem não disponível' : numero(b.margemPercentual) + '% de margem bruta', 'pulse', 'positive')}</div><div class="grid-main">${panel('Evolução do faturamento', 'Resultado mensal da oficina', grafico(serie), '<span class="legend"><i></i>Faturamento</span>')}${panel('Serviços mais executados', 'Volume de itens concluídos', barras(b.servicosMaisExecutados.slice(0, 4), 'quantidade', 'nome'))}</div><div class="overview-bottom">${panel('Distribuição das ordens', 'Situação atual das ordens da oficina', distribuicao(d), '', 'wallet')}${panel('A oficina agora', 'Até 4 ordens carregadas · veja todas na operação', ordensTabela(d, 4), '<button class="button small" data-page="operacao">Ver operação →</button>', 'orders')}</div><div class="quick-stats"><div><span class="round-icon">${icone('people')}</span><div><strong>${numero(b.clientesNoMes)} clientes</strong><small>atendidos neste mês</small></div></div><div><span class="round-icon">${icone('car')}</span><div><strong>${numero(b.clientesNaSemana)} clientes</strong><small>atendidos nesta semana</small></div></div><div><span class="round-icon green">${icone('clock')}</span><div><strong>Mais clareza no dia a dia</strong><small>Operação, financeiro e ponto juntos</small></div></div></div>`;
}

function operacao(d, estado) {
    const statuses = [['RECEBIDO', 'Aguardando'], ['DIAGNOSTICO', 'Em diagnóstico'], ['EM_SERVICO', 'Em serviço'], ['FINALIZADO', 'Finalizadas']];
    const resultado = resultadoOrdens(estado), erro = filtroOrdens(estado).erro;
    const registros = erro ? `<p class="form-error orders-message" role="alert">${e(erro)}</p>` : resultado.itens.length
        ? ordensTabela({ ...d, ordens: resultado.itens }) : '<p class="empty">Nenhuma ordem encontrada. Altere a pesquisa ou crie uma nova ordem.</p>';
    return `<div class="metrics">${statuses.map(([key, label]) => card(label, numero(d.dashboard.ordensPorStatus[key] ?? 0), 'Situação atual das ordens', 'orders')).join('')}</div>${panel('Ordens da oficina', 'Crie um atendimento ou use Acompanhar ordem para atualizar a situação do veículo.', pesquisaOrdens(estado) + registros + paginacaoOrdens(estado, resultado), acao('ordem', 'Abrir ordem'))}`;
}

function clientes(d, estado) {
    const i = d.indicadores;
    return `<div class="metrics">${card('Clientes atendidos', numero(d.dashboard.clientesNoMes), 'No período selecionado', 'people')}${card('Novos cadastros', numero(i.novosClientesCadastrados), 'Clientes cadastrados no mês', 'plus')}${card('Gasto médio', moeda(i.gastoMedioCliente), 'Por cliente no mês', 'wallet')}${card('Intervalo entre visitas', numero(i.diasMediosEntreVisitas) + ' dias', 'Média das visitas registradas', 'clock')}</div>${panel('Clientes', 'Pesquise e edite os cadastros da oficina', listaCadastro('cliente', estado), acao('cliente', 'Novo cliente'))}${panel('Veículos', 'Pesquise por placa, marca ou modelo', listaCadastro('veiculo', estado), acao('veiculo', 'Novo veículo'))}`;
}

function servicos(d, estado) {
    return panel('Catálogo de serviços', 'Pesquise por nome e atualize preços e custos', listaCadastro('servico', estado), acao('servico', 'Novo serviço'));
}

function tecnico(d) {
    const i = d.indicadores;
    return `<div class="metrics">${card('Aguardando diagnóstico', numero(i.aguardandoDiagnostico), 'Ordens na fila', 'car')}${card('Conclusão média', i.segundosMediosConclusao == null ? '—' : horas(Math.round(i.segundosMediosConclusao / 60)), 'Tempo médio até a conclusão', 'clock')}${card('Retrabalhos', numero(i.retrabalhos), 'Itens refeitos no período', 'tool')}${card('Diagnósticos convertidos', `${numero(i.diagnosticosConvertidos)} / ${numero(i.diagnosticos)}`, 'Conversão em reparos aprovados', 'pulse')}</div><div class="grid-two">${panel('Remaps por mês', 'Quantidade de serviços ECU e TCU', remaps(i.evolucao))}${panel('Marcas atendidas', 'Clientes e veículos da oficina', barras(i.marcas, 'quantidade', 'nome'))}</div>${panel('Manutenção e diagnóstico', 'Serviços mais executados', barras(d.dashboard.servicosMaisExecutados, 'quantidade', 'nome'))}`;
}

function financeiro(d, estado) {
    const serie = d.indicadores.evolucao, atual = serie.at(-1), anterior = serie.at(-2);
    const indicadores = [['Faturamento', 'receita', 'Ordens concluídas no mês', 'wallet'],
        ['Custos diretos', 'custosDiretos', 'Custos dos serviços', 'tool'],
        ['Despesas operacionais', 'despesasOperacionais', 'Competência do período', 'orders'],
        ['Resultado operacional', 'resultadoOperacional', 'Lucro bruto menos despesas', 'pulse']];
    const metricas = indicadores.map(([titulo, campo, nota, icon]) => {
        const variacao = variacaoFinanceira(atual?.[campo], anterior?.[campo]);
        const melhor = variacao == null ? '' : (['custosDiretos', 'despesasOperacionais'].includes(campo) ? variacao <= 0 : variacao >= 0) ? 'positive' : 'negative';
        return card(titulo, moeda(atual?.[campo]), `${percentual(variacao)} vs. mês anterior · ${nota}`, icon, melhor);
    }).join('');
    return `<div class="finance-page">${painelBanco(d.banco, estado)}<div class="metrics finance-metrics">${metricas}</div>
        ${graficosFinanceiros(d, estado)}<div class="finance-tables">
        ${panel('Receita, custos e resultado', 'Faturamento é diferente de recebimento', tabela(['Mês', 'Faturamento', 'Custos diretos', 'Lucro bruto', 'Recebimentos'], serie.map(m => [e(m.mes), moeda(m.receita), moeda(m.custosDiretos), `<span class="${Number(m.lucroBruto) < 0 ? 'red-text' : 'green-text'}">${moeda(m.lucroBruto)}</span>`, moeda(m.recebimentos)])))}
        ${panel('Despesas da oficina', 'Até 100 registros · o gráfico considera todos os lançamentos do mês', tabela(['Descrição', 'Categoria', 'Vencimento', 'Valor', 'Situação'], d.despesas.map(x => [`<strong>${e(x.descricao)}</strong>`, e(x.categoria), e(x.vencimento), moeda(x.valor), badge(x.pagaEm ? 'FINALIZADO' : 'AGUARDANDO_APROVACAO', x.pagaEm ? 'Paga' : 'Em aberto')])), acao('despesa', 'Nova despesa'))}</div></div>`;
}

function periodoFinanceiro(estado, label) {
    return `<select class="finance-period" data-finance-months aria-label="${e(label)}">${[3, 6, 12].map(m => `<option value="${m}" ${(estado.financeiroMeses ?? 6) === m ? 'selected' : ''}>Últimos ${m} meses</option>`).join('')}</select>`;
}

function graficosFinanceiros(d, estado) {
    const analise = d.analiseFinanceira;
    return `<div class="finance-charts">
        ${panel('Fluxo de caixa mensal', 'Recebimentos, despesas pagas e estornos registrados.', graficoMarkup('cashflow', analise?.fluxoCaixa ?? [], 'money') + '<p class="finance-help">Movimento líquido do mês; não é saldo bancário nem lucro.</p>', periodoFinanceiro(estado, 'Período do fluxo de caixa'), 'wallet')}
        ${panel('Receitas x custos', 'Faturamento e custos diretos por competência.', graficoMarkup('revenue', d.indicadores.evolucao, 'money'), periodoFinanceiro(estado, 'Período de receitas e custos'), 'pulse')}
        ${panel('Despesas por categoria', 'Todas as despesas da competência selecionada.', graficoMarkup('donut', (analise?.despesasPorCategoria ?? []).map(c => ({ name: c.nome, value: Number(c.valor) })), 'money'), `<span class="finance-period-note">${e(rotuloMes(estado.mes))}</span>`, 'wallet')}
        </div>`;
}

function ponto(d, estado) {
    const options = d.funcionarios.map(f => `<option value="${e(f.id)}" ${f.id === estado.funcionarioId ? 'selected' : ''}>${e(f.nome)}</option>`).join('');
    const selecionado = estado.funcionarioId ?? d.funcionarios[0]?.id;
    const jornadas = [...(estado.jornadas ?? d.jornadas ?? [])].filter(j => j.funcionarioId === selecionado)
        .sort((a, b) => diasSemana.findIndex(([dia]) => dia === a.dia) - diasSemana.findIndex(([dia]) => dia === b.dia) || b.vigenciaInicio.localeCompare(a.vigenciaInicio));
    const horarios = tabela(['Dia', 'Entrada', 'Saída', 'Intervalo', 'Tolerância', 'Vigência', ''], jornadas.map(j => [
        e(diaSemana(j.dia)), e(j.entrada.slice(0, 5)), e(j.saida.slice(0, 5)), `${j.intervaloMinutos} min`, `${j.toleranciaMinutos} min`,
        `${e(j.vigenciaInicio)}<small>${j.vigenciaFim ? 'Até ' + e(j.vigenciaFim) : 'Sem data final'}</small>`,
        j.vigenciaFim ? 'Encerrada' : `<button class="text-button" data-encerrar-jornada="${e(j.id)}">Encerrar vigência</button>`
    ]));
    const configurar = `<button class="button primary" data-jornada="${e(selecionado)}" ${selecionado ? '' : 'disabled'}>${icone('clock')}Configurar horários</button>`;
    return `<div class="point-callout"><div><span class="eyebrow">PONTO PELO QR CODE</span><h2>Escaneou. Confirmou. Registrou.</h2><p>O QR Code da loja leva à página exclusiva de entrada, almoço, retorno e saída.</p></div><a class="button primary" href="${e(linkPonto(estado.baseUrl || 'http://localhost:8080'))}" target="_blank" rel="noopener">Abrir página do funcionário ${icone('arrow')}</a></div>
        ${panel('Funcionários', 'O proprietário configura os horários; o funcionário registra o ponto.', tabela(['Nome', 'Matrícula', 'Situação', ''], d.funcionarios.map(f => [
            `<strong>${e(f.nome)}</strong>`, e(f.matricula), badge(f.ativo ? 'APROVADO' : 'CANCELADO', f.ativo ? 'Ativo' : 'Inativo'),
            `<button class="text-button" data-jornada="${e(f.id)}">Configurar horários</button>`
        ])), acao('funcionario', 'Novo funcionário'))}
        ${panel('Jornada de trabalho', 'Configure a entrada e a saída esperadas para cada dia da semana.', `<label class="inline-label">Funcionário<select id="employee">${options || '<option>Sem funcionários</option>'}</select></label>${horarios}<p class="schedule-help">Para mudar horários, encerre a vigência anterior e crie outra a partir do dia seguinte. Os horários anteriores ficam no histórico.</p>`, configurar)}
        ${resumoPonto(estado.resumo)}`;
}

function resumoPonto(resumo) {
    const conteudo = resumo ? `<div class="metrics point-metrics">
        ${card('Horas trabalhadas', horas(resumo.minutosTrabalhados), 'Calculadas pelas marcações', 'clock')}
        ${card('Horas em serviços', horas(resumo.minutosEmServicos), 'Execuções registradas', 'tool')}
        ${card('Horas extras', horas(resumo.minutosExtras), 'Conforme jornada prevista', 'plus')}
        ${card('Atrasos', horas(resumo.minutosAtraso), 'Conforme entrada e tolerância', 'clock')}</div>
        ${tabela(['Data', 'Trabalhado', 'Previsto', 'Atraso', 'Situação'], resumo.dias.map(x => [e(x.data), horas(x.minutosTrabalhados), horas(x.minutosPrevistos), horas(x.minutosAtraso), badge(x.incompleto ? 'AGUARDANDO_APROVACAO' : 'FINALIZADO', x.incompleto ? 'Incompleto' : x.minutosTrabalhados ? 'Completo' : 'Sem horas registradas')]))}`
        : vazio('Selecione um funcionário para consultar o ponto.');
    return panel('Resumo de ponto', 'Previsto vem da jornada configurada. Trabalhado vem das marcações confirmadas pelo servidor.', conteudo);
}
function conteudo(estado) {
    const views = { geral, operacao, clientes, servicos, tecnico, financeiro, ponto };
    return views[estado.pagina](estado.dados, estado);
}

export function telaPonto() {
    return `<main class="mobile-page"><div class="brand"><span class="brand-symbol">A<span>↗</span></span><div>AutoGestão<small>PONTO DA OFICINA</small></div></div>
        <section class="point-card"><span class="round-icon">${icone('clock')}</span><span class="eyebrow">SUA JORNADA COMEÇA AQUI</span><h1>Registrar ponto</h1>
        <p>Informe sua matrícula e PIN.<br>O horário será confirmado pelo servidor.</p>
        <form id="point-form"><label>Matrícula<input name="matricula" autocomplete="username" required maxlength="32" placeholder="Ex.: 001"></label>
        <label>PIN individual<input name="pin" type="password" inputmode="numeric" pattern="[0-9]{4,12}" minlength="4" maxlength="12" required autocomplete="off" placeholder="••••"></label>
        <fieldset><legend>O que deseja registrar?</legend><div class="point-options">${['ENTRADA', 'ALMOCO', 'RETORNO', 'SAIDA'].map((tipo, i) => `<label><input type="radio" name="tipo" value="${tipo}" ${i === 0 ? 'checked' : ''}><span>${icone('clock')}${{ ENTRADA: 'Entrada', ALMOCO: 'Almoço', RETORNO: 'Retorno', SAIDA: 'Saída' }[tipo]}</span></label>`).join('')}</div></fieldset>
        <label class="confirm-check"><input type="checkbox" required>Confirmo esta marcação de ponto.</label>
        <button class="button primary wide" type="submit">Confirmar marcação ${icone('arrow')}</button><p id="point-result" role="status" aria-live="polite"></p></form></section>
        <p class="mobile-foot">Seus horários são configurados pelo responsável da oficina.<br>O computador da oficina precisa estar ligado e conectado.<br>O QR Code é um link de acesso; não comprova presença física.</p></main>`;
}


function painelBanco(banco, estado) {
    if (estado?.modo === 'demo') {
        return panel('Extrato bancário · demonstração', 'Valores fictícios para visualizar o fluxo de caixa.',
            '<div class="metrics">' + card('Entradas', moeda(6200), 'Exemplo', 'wallet')
            + card('Saídas', moeda(2100), 'Exemplo', 'wallet')
            + card('Movimento líquido', moeda(4100), 'Exemplo; não representa lucro', 'pulse') + '</div>');
    }
    if (!banco?.configurado) return panel('Conectar conta InfinitePay',
        'Importação pelo Meu Pluggy', '<p>Configure a conexão no backend com o Client ID, Client Secret e Item ID do Meu Pluggy. Nenhuma senha bancária é solicitada nesta tela.</p>');
    const data = banco.ultimaConsulta ? new Date(banco.ultimaConsulta).toLocaleString('pt-BR') : 'Aguardando primeira consulta';
    return panel('Movimentações bancárias', 'Última importação local: ' + data,
        (banco.aviso ? '<p role="alert">' + e(banco.aviso) + '</p>' : '')
        + (banco.proximaTentativa ? '<p>Próxima consulta permitida: ' + e(new Date(banco.proximaTentativa).toLocaleString('pt-BR')) + '</p>' : '')
        + '<p>Dados disponibilizados pelo Meu Pluggy a cada 24 horas. Entradas e saídas incluem transferências; não são faturamento e lucro. Últimos 100 registros do período; totais consideram todos.</p>'
        + '<div class="metrics">' + card('Entradas', moeda(banco.entradas), 'Extrato', 'wallet')
        + card('Saídas', moeda(banco.saidas), 'Extrato', 'wallet')
        + card('Movimento líquido', moeda(banco.saldoDoPeriodo), 'Entradas menos saídas; não é saldo da conta', 'pulse') + '</div>'
        + tabela(['Data', 'Descrição', 'Valor'], banco.movimentos.map(m => [
            e(new Date(m.instante).toLocaleString('pt-BR')), e(m.descricao), moeda(m.valor)])),
        '<button class="button" data-bank-sync>Consultar atualização</button>');
}
