import { valorMonetario } from './validacao.js';

export const proximasEtapas = {
    RASCUNHO: ['RECEBIDO', 'Receber veículo'], RECEBIDO: ['DIAGNOSTICO', 'Iniciar diagnóstico'],
    DIAGNOSTICO: ['AGUARDANDO_APROVACAO', 'Solicitar aprovação'], APROVADO: ['EM_SERVICO', 'Iniciar serviço'],
    AGUARDANDO_PECA: ['EM_SERVICO', 'Retomar serviço'], EM_SERVICO: ['FINALIZADO', 'Finalizar ordem'],
    FINALIZADO: ['ENTREGUE', 'Registrar entrega']
};

export function acoesSituacao(ordem) {
    const proxima = proximasEtapas[ordem.status], acoes = [];
    if (proxima) acoes.push({ acao: 'status', destino: proxima[0], titulo: proxima[1],
        bloqueada: ordem.status === 'EM_SERVICO' && !podeFinalizar(ordem) });
    if (ordem.status === 'AGUARDANDO_APROVACAO') acoes.push({ acao: 'aprovar', destino: 'APROVADO', titulo: 'Registrar aprovação', bloqueada: !ordem.itens.length });
    if (['APROVADO', 'EM_SERVICO'].includes(ordem.status)) acoes.push({ acao: 'status', destino: 'AGUARDANDO_PECA', titulo: 'Aguardar peça', bloqueada: false });
    return acoes;
}

export function dadosConclusao(valores) {
    const relato = valores.relato?.trim();
    if (!relato || relato.length > 4000) throw new Error('Descreva o trabalho realizado, com até 4.000 caracteres.');
    const custoReal = valorMonetario(valores.custoReal, 'Custo real');
    return { relato, custoReal, motivoRetrabalho: valores.motivoRetrabalho?.trim() || null };
}

export function podeFinalizar(ordem) {
    return ordem.status === 'EM_SERVICO' && ordem.itens.length > 0 && ordem.itens.every(item => item.concluidoEm);
}
