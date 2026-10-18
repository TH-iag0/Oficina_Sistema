export function abrirRegistro(contexto, config, modal) {
  const {
    estado,
    abrirModal,
    aviso,
    mensagemErro,
    carregar
  } = contexto;
  const api = estado.api,
    demonstracao = estado.modo !== 'real';
  abrirModal(config.titulo, `<form id="record-form">${config.campos}<p class="form-error" role="alert"></p><button type="submit" class="button primary wide" ${demonstracao ? 'disabled' : ''}>Salvar</button></form>
        ${demonstracao ? '<p class="modal-description">Demonstração: preencha para conhecer o formulário. O salvamento está desativado.</p>' : ''}`);
  const form = modal.querySelector('form'),
    botao = form.querySelector('[type=submit]');
  form.querySelector('input')?.focus();
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (demonstracao || estado.modo !== 'real' || estado.api !== api || botao.disabled) {
      return;
    }
    botao.disabled = true;
    form.querySelector('.form-error').textContent = '';
    try {
      await config.enviar(Object.fromEntries(new FormData(form)));
      if (estado.api !== api || estado.modo !== 'real') {
        return;
      }
      if (form.isConnected) {
        modal.close();
      }
      aviso('Registro salvo no backend.');
      await carregar();
    } catch (error_) {
      if (form.isConnected) {
        form.querySelector('.form-error').textContent = mensagemErro(error_);
      }
    } finally {
      botao.disabled = demonstracao || estado.modo !== 'real';
    }
  });
}
