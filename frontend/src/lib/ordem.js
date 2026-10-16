import { escapar as e } from './ui.js';
import { dadosItemServico } from './validacao.js';

export function camposAbertura(rascunho, demonstracao = false) {
    return `<p class="modal-description">Cadastre um atendimento. Escolha cliente e veículo ou use os botões para cadastrar novos.</p>
        <form id="ordem-form"><fieldset><legend>1. Cliente e veículo</legend>
        <div data-os-cliente></div><button class="button small" type="button" data-os-novo="cliente">+ Novo cliente</button>
        <div data-os-veiculo></div><button class="button small" type="button" data-os-novo="veiculo" ${rascunho.clienteId ? '' : 'disabled'}>+ Novo veículo deste cliente</button></fieldset>
        <fieldset><legend>2. Atendimento</legend>
        <label>Situação inicial<select name="situacaoInicial">${[['RASCUNHO', 'Rascunho · atendimento ainda não recebido'], ['RECEBIDO', 'Recebido · veículo na oficina'], ['DIAGNOSTICO', 'Em diagnóstico']].map(([valor, texto]) => `<option value="${valor}" ${rascunho.situacaoInicial === valor ? 'selected' : ''}>${texto}</option>`).join('')}</select></label>
        <label>Previsão de entrega (opcional)<input type="datetime-local" name="previsaoEntrega" value="${e(rascunho.previsaoEntrega)}"></label>
        <p class="modal-description">O servidor registra o horário do recebimento. Serviço e finalização serão atualizados no acompanhamento da ordem.</p></fieldset>
        <label class="initial-service-check"><input type="checkbox" name="incluirServico" ${rascunho.incluirServico ? 'checked' : ''}>Adicionar o primeiro serviço agora</label>
        <fieldset data-os-primeiro-servico ${rascunho.incluirServico ? '' : 'hidden disabled'}><legend>3. Primeiro serviço</legend><div data-os-servico></div>
        <label>Quantidade<input name="quantidade" type="number" min="1" step="1" required value="${e(rascunho.quantidade ?? 1)}"></label>
        <div class="form-row"><label>Preço por unidade (R$)<input name="preco" type="number" min="0" step="0.01" required value="${e(rascunho.preco)}"></label><label>Custo por unidade (R$)<input name="custo" type="number" min="0" step="0.01" required value="${e(rascunho.custo)}"></label></div></fieldset>
        <p class="form-error" role="alert"></p><button type="submit" class="button primary wide" ${demonstracao ? 'disabled' : ''}>Criar ordem e acompanhar</button></form>
        ${demonstracao ? '<p class="modal-description">Demonstração: preencha para conhecer o fluxo. O salvamento está desativado.</p>' : ''}`;
}

export function dadosAbertura(rascunho) {
    if (!rascunho.clienteId || !rascunho.veiculoId) throw new Error('Escolha o cliente e o veículo para criar a ordem.');
    if (!['RASCUNHO', 'RECEBIDO', 'DIAGNOSTICO'].includes(rascunho.situacaoInicial)) throw new Error('Situação inicial inválida.');
    const dados = { clienteId: rascunho.clienteId, veiculoId: rascunho.veiculoId, situacaoInicial: rascunho.situacaoInicial };
    if (rascunho.previsaoEntrega) {
        const data = new Date(rascunho.previsaoEntrega);
        if (Number.isNaN(data.getTime())) throw new Error('Previsão de entrega inválida.');
        dados.previsaoEntrega = data.toISOString();
    }
    if (rascunho.incluirServico) {
        dados.servicoInicial = dadosItemServico(rascunho);
    }
    return dados;
}
