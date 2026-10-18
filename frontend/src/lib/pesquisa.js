export function adiarPesquisa(executar, atraso = 300) {
  let timer;
  const agendar = (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => executar(...args), atraso);
  };
  agendar.cancelar = () => clearTimeout(timer);
  return agendar;
}
export function redesenharPesquisa(root, desenhar) {
  const input = root.ownerDocument.activeElement;
  const preservar = input?.matches('input[name=busca]') && root.contains(input);
  const form = preservar ? input.closest('form') : null;
  let seletor = null;
  if (form) {
    seletor = Object.hasOwn(form.dataset, 'orderSearch') ? '[data-order-search]' : `[data-cadastro-search="${form.dataset.cadastroSearch}"]`;
  }
  const valor = input?.value,
    inicio = input?.selectionStart,
    fim = input?.selectionEnd;
  desenhar();
  if (!seletor) {
    return;
  }
  const novo = root.querySelector(`${seletor} input[name=busca]`);
  if (!novo) {
    return;
  }
  novo.value = valor;
  novo.focus({
    preventScroll: true
  });
  if (inicio != null) {
    novo.setSelectionRange(inicio, fim);
  }
}
