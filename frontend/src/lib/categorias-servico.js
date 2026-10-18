export const categoriasPadrao = [['MANUTENCAO', 'Manutenção'], ['DIAGNOSTICO', 'Diagnóstico'], ['REMAP_ECU', 'Remap ECU'], ['REMAP_TCU', 'Remap TCU'], ['OUTRO', 'Outro']];
export function opcoesCategorias(categorias = []) {
  return [...categoriasPadrao, ...categorias.filter(c => !c.padrao).map(c => [c.id, c.nome])];
}
export function dadosCategoriaServico(valor) {
  if (categoriasPadrao.some(([codigo]) => codigo === valor)) {
    return {
      categoria: valor
    };
  }
  if (!/^[a-f\d]{8}(?:-[a-f\d]{4}){3}-[a-f\d]{12}$/i.test(valor || '')) {
    throw new Error('Selecione uma categoria válida.');
  }
  return {
    categoria: 'OUTRO',
    categoriaId: valor
  };
}
