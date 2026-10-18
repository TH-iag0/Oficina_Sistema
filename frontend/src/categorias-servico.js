import { opcoesCategorias } from './lib/categorias-servico.js';
import { escapar as e } from './lib/ui.js';
export function montarCategoriasServico(form, contexto, categorias) {
  const campo = form.querySelector('[name=novaCategoria]'),
    botao = form.querySelector('[data-adicionar-categoria]');
  const mensagem = form.querySelector('[data-categoria-error]');
  botao.disabled = contexto.estado.modo !== 'real';
  campo.addEventListener('keydown', event => {
    if (event.key === 'Enter') {
      event.preventDefault();
      botao.click();
    }
  });
  botao.addEventListener('click', async () => {
    const {
        estado,
        mensagemErro,
        aviso
      } = contexto,
      api = estado.api;
    if (estado.modo !== 'real' || botao.disabled) {
      return;
    }
    mensagem.textContent = '';
    const nome = campo.value.trim().replace(/\s+/g, ' ');
    if (!nome || nome.length > 80) {
      mensagem.textContent = 'Informe um nome de até 80 caracteres.';
      campo.focus();
      return;
    }
    botao.disabled = true;
    form.dataset.categoriaLoading = 'true';
    try {
      const categoria = await api.servicos.cadastrarCategoria({
        nome
      });
      if (!form.isConnected || estado.api !== api || estado.modo !== 'real') {
        return;
      }
      categorias.push(categoria);
      form.elements.categoria.innerHTML = opcoesCategorias(categorias).map(([valor, texto]) => {
        return `<option value="${e(valor)}">${e(texto)}</option>`;
      }).join('');
      form.elements.categoria.value = categoria.id;
      campo.value = '';
      form.querySelector('[data-nova-categoria]').open = false;
      form.elements.categoria.focus();
      aviso('Categoria criada e selecionada para este serviço.');
    } catch (error_) {
      if (form.isConnected) {
        mensagem.textContent = mensagemErro(error_);
      }
    } finally {
      botao.disabled = estado.modo !== 'real';
      delete form.dataset.categoriaLoading;
    }
  });
}
