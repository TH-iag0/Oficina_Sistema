import { analiseDemonstrativa } from './financeiro.js';

export function demonstracao(mes, quantidadeMeses = 6) {
    const clientes = [
        { id: 'c1', nome: 'Rafael Almeida', tipo: 'PF', telefone: '(11) 98842-1030', ativo: true },
        { id: 'c2', nome: 'Mariana Costa', tipo: 'PF', telefone: '(11) 99721-4480', ativo: true },
        { id: 'c3', nome: 'Lucas Ferreira', tipo: 'PF', telefone: '(11) 98163-2290', ativo: true },
        { id: 'c4', nome: 'Beatriz Santos', tipo: 'PF', telefone: '(11) 99205-6710', ativo: true }
    ];
    const veiculos = [
        { id: 'v1', clienteId: 'c1', marca: 'Volkswagen', modelo: 'Golf GTI', placa: 'FEL2A21', quilometragem: 62450, ativo: true },
        { id: 'v2', clienteId: 'c2', marca: 'BMW', modelo: '320i', placa: 'GHT4B32', quilometragem: 41800, ativo: true },
        { id: 'v3', clienteId: 'c3', marca: 'Audi', modelo: 'A3 Sedan', placa: 'ABC1D23', quilometragem: 85200, ativo: true },
        { id: 'v4', clienteId: 'c4', marca: 'Chevrolet', modelo: 'Onix', placa: 'JKL5E67', quilometragem: 30200, ativo: true }
    ];
    const servicos = [
        { id: 's1', nome: 'Remap ECU · Stage 1', categoria: 'REMAP_ECU', preco: 2400, custo: 700, ativo: true },
        { id: 's2', nome: 'Revisão preventiva', categoria: 'MANUTENCAO', preco: 850, custo: 380, ativo: true },
        { id: 's3', nome: 'Diagnóstico eletrônico', categoria: 'DIAGNOSTICO', preco: 450, custo: 120, ativo: true },
        { id: 's4', nome: 'Remap de transmissão', categoria: 'REMAP_TCU', preco: 1800, custo: 520, ativo: true }
    ];
    const ordens = ['EM_SERVICO', 'DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'FINALIZADO'].map((status, i) => ({
        id: 'o' + i, numero: 1048 + i, clienteId: clientes[i].id, veiculoId: veiculos[i].id,
        status, total: servicos[i].preco, custo: servicos[i].custo, itens: [{ id: 'i' + i, servicoId: servicos[i].id, descricao: servicos[i].nome, categoria: servicos[i].categoria, quantidade: 1, preco: servicos[i].preco, custo: servicos[i].custo, concluidoEm: status === 'FINALIZADO' ? mes + '-05T15:00:00Z' : null, relato: status === 'FINALIZADO' ? 'Calibração da transmissão aplicada e testada em rodagem.' : null }]
    }));
    const [ano, m] = mes.split('-').map(Number);
    const valores = [28700, 33800, 31400, 41800, 39600, 48750];
    const evolucao = Array.from({ length: quantidadeMeses }, (_, i) => valores[(i + 6 - quantidadeMeses % 6) % 6]).map((receita, i) => ({
        mes: new Date(Date.UTC(ano, m - quantidadeMeses + i, 1)).toISOString().slice(0, 7), receita,
        custosDiretos: Math.round(receita * .561), despesasOperacionais: 7200,
        lucroBruto: Math.round(receita * .439), resultadoOperacional: Math.round(receita * .439) - 7200,
        recebimentos: receita - 1600, estornos: 0, clientesAtendidos: 18 + i * 3,
        clientesNovos: 7 + i, clientesRecorrentes: 11 + i * 2, remapsEcu: 12 + i * 2, remapsTcu: 4 + i
    }));
    const ranking = servicos.map((s, i) => ({ ...s, servicoId: s.id, quantidade: [22, 28, 19, 9][i], receita: s.preco * [22, 28, 19, 9][i], custoDireto: s.custo * [22, 28, 19, 9][i] })).sort((a, b) => b.quantidade - a.quantidade);
    const despesas = [
        { id: 'd1', descricao: 'Aluguel da oficina', categoria: 'Estrutura', valor: 4200, vencimento: mes + '-10', pagaEm: mes + '-10T12:00:00Z' },
        { id: 'd2', descricao: 'Energia elétrica', categoria: 'Estrutura', valor: 640, vencimento: mes + '-20', pagaEm: null },
        { id: 'd3', descricao: 'Internet', categoria: 'Operação', valor: 220, vencimento: mes + '-15', pagaEm: mes + '-12T12:00:00Z' },
        { id: 'd4', descricao: 'Sistema de gestão', categoria: 'Ferramentas', valor: 180, vencimento: mes + '-12', pagaEm: mes + '-12T12:00:00Z' },
        { id: 'd5', descricao: 'Contabilidade', categoria: 'Impostos e administração', valor: 500, vencimento: mes + '-28', pagaEm: null },
        { id: 'd6', descricao: 'Materiais de consumo', categoria: 'Operação', valor: 1460, vencimento: mes + '-08', pagaEm: mes + '-08T12:00:00Z' }
    ].map(d => ({ ...d, competencia: mes + '-01' }));
    return {
        dashboard: { faturamento: 48750, variacaoPercentual: 23.1, custosDiretos: evolucao.at(-1).custosDiretos, lucroBruto: evolucao.at(-1).lucroBruto,
            margemPercentual: 43.9, ordensConcluidas: 32, ticketMedio: 1523.44, clientesNoMes: 33,
            clientesNaSemana: 9, ordensPorStatus: { RECEBIDO: 4, DIAGNOSTICO: 3, EM_SERVICO: 5, FINALIZADO: 8 }, servicosMaisExecutados: ranking },
        indicadores: { evolucao, aguardandoDiagnostico: 4, segundosMediosConclusao: 16200, retrabalhos: 2,
            diagnosticos: 19, diagnosticosConvertidos: 14, novosClientesCadastrados: 12, gastoMedioCliente: 1477.27,
            diasMediosEntreVisitas: 95, marcas: [{ nome: 'Volkswagen', quantidade: 14 }, { nome: 'BMW', quantidade: 9 }, { nome: 'Audi', quantidade: 6 }, { nome: 'Outras', quantidade: 4 }],
            modelos: [], causas: [], servicosRentaveis: ranking, revisoesPendentes: [veiculos[2]], atendimentosSemanais: [] },
        clientes, veiculos, servicos, ordens,
        funcionarios: [{ id: 'f1', nome: 'Felipe Martins', matricula: '001', ativo: true }],
        jornadas: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'].map((dia, i) => ({ id: 'j' + i, funcionarioId: 'f1', dia, entrada: '08:00:00', saida: '17:00:00', intervaloMinutos: 60, toleranciaMinutos: 5, vigenciaInicio: mes + '-01', vigenciaFim: null })),
        resumo: { minutosTrabalhados: 2160, minutosEmServicos: 1800, minutosExtras: 30, minutosAtraso: 10,
            dias: [480, 480, 450, 510, 240].map((minutosTrabalhados, i) => ({ data: `${mes}-0${i + 1}`, minutosTrabalhados, minutosPrevistos: 480, minutosExtras: i === 3 ? 30 : 0, minutosAtraso: i === 2 ? 10 : 0, incompleto: i === 4 })) },
        despesas, analiseFinanceira: analiseDemonstrativa(evolucao, despesas, mes)
    };
}
