export const coresFinanceiras = { entradas: '#1682ff', saidas: '#ff5477', saldo: '#24d5f5', receita: '#1682ff', custosDiretos: '#a340ef' };

export function seriesFinanceiras(tipo, dados) {
    const campos = tipo === 'cashflow' ? [['entradas', 'Entradas'], ['saidas', 'Saídas'], ['saldo', 'Movimento líquido']]
        : [['receita', 'Faturamento'], ['custosDiretos', 'Custos diretos']];
    return campos.map(([campo, nome]) => ({ nome, campo, cor: coresFinanceiras[campo],
        valores: dados.map(item => Number.isFinite(Number(item[campo])) ? Number(item[campo]) : 0) }));
}

export function escalaFinanceira(valores) {
    const minimo = Math.min(0, ...valores), maximo = Math.max(0, ...valores);
    const bruto = (maximo - minimo || 5) / 4;
    const potencia = 10 ** Math.floor(Math.log10(bruto));
    const passo = [1, 2, 2.5, 5, 10].find(v => v * potencia >= bruto) * potencia;
    return { minimo: Math.floor(minimo / passo) * passo,
        maximo: maximo === minimo ? passo * 4 : Math.ceil(maximo / passo) * passo, passo };
}

export function rotuloMes(mes) {
    return new Date(mes + '-02T12:00:00Z').toLocaleDateString('pt-BR', { month: 'short', year: '2-digit', timeZone: 'UTC' }).replace('.', '').replace(' de ', '/');
}

export function variacaoFinanceira(atual, anterior) {
    if (atual == null || anterior == null || Number(anterior) === 0) return null;
    return (Number(atual) - Number(anterior)) / Math.abs(Number(anterior)) * 100;
}

export function analiseDemonstrativa(evolucao, despesas, mes) {
    const categorias = new Map();
    despesas.filter(d => d.competencia?.startsWith(mes)).forEach(d => {
        categorias.set(d.categoria, (categorias.get(d.categoria) || 0) + Number(d.valor));
    });
    const pagas = despesas.filter(d => d.pagaEm?.startsWith(mes)).reduce((total, d) => total + Number(d.valor), 0);
    return { fluxoCaixa: evolucao.map(m => {
        const despesasPagas = m.mes === mes ? pagas : Number(m.despesasOperacionais);
        const saidas = despesasPagas + Number(m.estornos);
        return { mes: m.mes, entradas: Number(m.recebimentos), despesasPagas, estornos: Number(m.estornos), saidas, saldo: Number(m.recebimentos) - saidas };
    }), despesasPorCategoria: [...categorias].map(([nome, valor]) => ({ nome, valor })).sort((a, b) => b.valor - a.valor) };
}
