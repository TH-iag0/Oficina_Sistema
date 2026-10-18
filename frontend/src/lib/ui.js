export const moeda = valor => valor == null ? '—' : new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL'
}).format(valor);
export const numero = valor => valor == null ? '—' : new Intl.NumberFormat('pt-BR', {
  maximumFractionDigits: 1
}).format(valor);
export function percentual(valor) {
  if (valor == null) return 'Sem comparação';
  const sinal = valor > 0 ? '+' : '';
  return `${sinal}${numero(valor)}%`;
}
export const horas = minutos => {
  return `${Math.floor(minutos / 60)}h ${String(minutos % 60).padStart(2, '0')}min`;
};
export const escapar = valor => String(valor ?? '').replace(/[&<>"']/g, char => ({
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;'
})[char]);
const nomes = {
  EM_SERVICO: 'Em serviço',
  DIAGNOSTICO: 'Diagnóstico',
  AGUARDANDO_APROVACAO: 'Aguardando aprovação',
  MANUTENCAO: 'Manutenção',
  REMAP_ECU: 'Remap ECU',
  REMAP_TCU: 'Remap TCU'
};
export const rotulo = valor => nomes[valor] ?? String(valor ?? '').toLowerCase().replaceAll('_', ' ').replace(/^./, char => char.toUpperCase());
export const itens = resposta => Array.isArray(resposta) ? resposta : resposta?.itens ?? [];
export function intervaloMes(mes) {
  if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(mes)) {
    throw new Error('Mês inválido.');
  }
  const [ano, numeroMes] = mes.split('-').map(Number);
  const fim = new Date(Date.UTC(ano, numeroMes, 1)).toISOString().slice(0, 10);
  return {
    inicio: mes + '-01',
    fim
  };
}

// Um retry de uma operação incerta deve conservar tanto o UUID como o conteúdo.
export function confirmacaoIdempotente(enviar, uuid = () => crypto.randomUUID(), mensagem = 'Repita o envio anterior antes de alterar os dados; o resultado ainda é incerto.') {
  let pendente;
  return async dados => {
    const conteudo = JSON.stringify(dados);
    if (pendente && pendente.conteudo !== conteudo) {
      throw new Error(mensagem);
    }
    pendente ??= {
      conteudo,
      dados: {
        ...dados,
        requisicaoId: uuid()
      }
    };
    try {
      const resposta = await enviar(pendente.dados);
      pendente = undefined;
      return resposta;
    } catch (error_) {
      if (error_.status >= 400 && error_.status < 500) {
        pendente = undefined;
      }
      throw error_;
    }
  };
}
export const confirmacaoPonto = (enviar, uuid) => confirmacaoIdempotente(enviar, uuid, 'Repita a marcação anterior antes de alterar os dados; o resultado ainda é incerto.');
