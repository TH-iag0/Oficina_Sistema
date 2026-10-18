export function valorMonetario(valor, campo = 'Valor') {
  let texto = '';
  if (typeof valor === 'string') {
    texto = valor.trim();
  } else if (typeof valor === 'number') {
    texto = String(valor);
  }
    const numero = Number(texto);
    if (!/^\d+(?:\.\d{1,2})?$/.test(texto) || !Number.isFinite(numero) || numero > 9999999999999.99) {
        throw new Error(`${campo}: informe um valor não negativo, com até duas casas decimais.`);
    }
    return numero;
}

export function dadosItemServico(valores) {
    const quantidade = Number(valores.quantidade);
    if (!valores.servicoId || !Number.isSafeInteger(quantidade) || quantidade < 1 || quantidade > 2147483647) {
        throw new Error('Selecione um serviço e informe uma quantidade inteira válida.');
    }
    return { servicoId: valores.servicoId, quantidade,
        preco: valorMonetario(valores.preco, 'Preço'), custo: valorMonetario(valores.custo, 'Custo') };
}
