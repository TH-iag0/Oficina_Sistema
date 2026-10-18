import { escapar as e } from './ui.js';
import { opcoesCategorias, dadosCategoriaServico } from './categorias-servico.js';
import { valorMonetario } from './validacao.js';
export const colecoes = {
  cliente: 'clientes',
  veiculo: 'veiculos',
  servico: 'servicos'
};
export const nomes = {
  cliente: 'cliente',
  veiculo: 'veículo',
  servico: 'serviço'
};
const campo = (nome, label, valor = '', tipo = 'text', extra = '') => {
  return `<label>${label}<input name="${nome}" type="${tipo}" value="${e(valor)}" ${extra}></label>`;
};
const select = (nome, label, opcoes, atual) => {
  const _opcoesMarkup = opcoes.map(([valor, texto]) => {
    return `<option value="${e(valor)}" ${valor === atual ? 'selected' : ''}>${e(texto)}</option>`;
  }).join('');
  return `<label>${label}<select name="${nome}" required>${_opcoesMarkup}</select></label>`;
};
export function camposCadastro(tipo, item = {}, categorias = []) {
  if (tipo === 'cliente') {
    return select('tipo', 'Tipo de cliente', [['PF', 'Pessoa física'], ['PJ', 'Pessoa jurídica']], item.tipo || 'PF') + campo('nome', 'Nome', item.nome, 'text', 'required maxlength="160" autocomplete="name"') + campo('documento', 'CPF / CNPJ (opcional)', item.documento, 'text', 'maxlength="20"') + campo('telefone', 'Telefone / WhatsApp (opcional)', item.telefone, 'tel', 'maxlength="30"');
  }
  if (tipo === 'veiculo') {
    return (item.id ? `<p class="modal-description">Placa: <strong>${e(item.placa)}</strong>. Este formulário mantém o cliente e a placa originais.</p>` : '<div data-seletor-cliente></div>' + campo('placa', 'Placa', item.placa, 'text', 'required maxlength="8" autocapitalize="characters"')) + campo('marca', 'Marca', item.marca, 'text', 'required maxlength="80"') + campo('modelo', 'Modelo', item.modelo, 'text', 'required maxlength="120"') + campo('quilometragem', 'Quilometragem (km)', item.quilometragem ?? 0, 'number', `required min="${item.quilometragem ?? 0}" step="1"`);
  }
  if (tipo === 'servico') {
    return campo('nome', 'Nome do serviço', item.nome, 'text', 'required maxlength="120"') + select('categoria', 'Categoria', opcoesCategorias(categorias), item.categoriaId || item.categoria || 'MANUTENCAO') + '<details class="optional-fields" data-nova-categoria><summary>+ Adicionar categoria</summary><label>Nome da nova categoria<input name="novaCategoria" maxlength="80" placeholder="Ex.: Higienização automotiva"></label><button type="button" class="button small" data-adicionar-categoria>Adicionar categoria</button><p class="form-error" data-categoria-error role="alert"></p></details>' + campo('preco', 'Preço sugerido (R$)', item.preco ?? '', 'number', 'required min="0" step="0.01"') + campo('custo', 'Custo estimado (R$)', item.custo ?? '', 'number', 'required min="0" step="0.01"');
  }
  throw new Error('Cadastro inválido.');
}
export function dadosCadastro(tipo, valores, edicao = false) {
  if (tipo === 'cliente') {
    return {
      tipo: valores.tipo,
      nome: valores.nome.trim(),
      documento: valores.documento?.trim() || null,
      telefone: valores.telefone?.trim() || null
    };
  }
  if (tipo === 'veiculo') {
    const quilometragem = Number(valores.quilometragem);
    if (valores.quilometragem == null || String(valores.quilometragem).trim() === '' || !Number.isSafeInteger(quilometragem) || quilometragem < 0) {
      throw new Error('Informe uma quilometragem inteira válida.');
    }
    const dados = {
      marca: valores.marca.trim(),
      modelo: valores.modelo.trim(),
      quilometragem
    };
    return edicao ? dados : {
      ...dados,
      clienteId: valores.clienteId,
      placa: valores.placa.trim().toUpperCase()
    };
  }
  if (tipo === 'servico') {
    const preco = valorMonetario(valores.preco, 'Preço'),
      custo = valorMonetario(valores.custo, 'Custo');
    return {
      nome: valores.nome.trim(),
      ...dadosCategoriaServico(valores.categoria),
      preco,
      custo
    };
  }
  throw new Error('Cadastro inválido.');
}
export function guardarCadastro(dados, tipo, registro) {
  const chave = colecoes[tipo];
  dados[chave] = [...(dados[chave] || []).filter(item => item.id !== registro.id), registro];
}
export function pesquisarCadastro(api, tipo, parametros) {
  return tipo === 'cliente' ? api.clientes({
    ...parametros,
    nome: parametros.busca || ''
  }) : api[colecoes[tipo]].listar(parametros);
}
export function paginaDemo(dados, tipo, {
  busca = '',
  pagina = 0,
  tamanho = 20,
  clienteId,
  ativo
} = {}) {
  const termo = busca.toLocaleLowerCase('pt-BR').trim();
  const lista = dados[colecoes[tipo]].filter(item => (!clienteId || item.clienteId === clienteId) && (ativo === undefined || item.ativo === ativo) && [item.nome, item.placa, item.marca, item.modelo].some(v => v?.toLocaleLowerCase('pt-BR').includes(termo)));
  return {
    itens: lista.slice(pagina * tamanho, (pagina + 1) * tamanho),
    pagina,
    tamanho,
    total: lista.length,
    totalPaginas: Math.ceil(lista.length / tamanho)
  };
}
