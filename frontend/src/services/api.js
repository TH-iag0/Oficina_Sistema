// Acesso local sem login; escritas continuam protegidas por CSRF.
export function criarApi({
  baseUrl = "http://localhost:8080",
  fetchImpl = fetch
} = {}) {
  const base = baseUrl.replace(/\/$/, "");
  async function requisicao(caminho, {
    metodo = "GET",
    dados
  } = {}) {
    const headers = {
      Accept: "application/json"
    };
    if (dados !== undefined) {
      headers["Content-Type"] = "application/json";
    }
    if (metodo !== "GET") {
      const csrf = await requisicao("/api/csrf");
      headers[csrf.headerName] = csrf.token;
    }
    const resposta = await fetchImpl(base + caminho, {
      method: metodo,
      headers,
      credentials: "include",
      body: dados === undefined ? undefined : JSON.stringify(dados)
    });
    if (!resposta.ok) {
      const problema = await resposta.json().catch(() => ({
        detail: "Falha na comunicação com o backend."
      }));
      const erro = new Error(problema.detail ?? "Requisição recusada.");
      erro.status = resposta.status;
      erro.problema = problema;
      throw erro;
    }
    return resposta.status === 204 ? undefined : resposta.json();
  }
  const id = valor => encodeURIComponent(valor);
  const consulta = (rota, parametros = {}) => {
    const valores = Object.entries(parametros).filter(([, valor]) => valor !== undefined && valor !== null);
    const query = new URLSearchParams(valores).toString();
    return requisicao(rota + (query ? "?" + query : ""));
  };
  const gravar = (rota, dados, metodo = "POST") => requisicao(rota, {
    metodo,
    dados
  });
  const cadastro = rota => ({
    listar: parametros => consulta(rota, parametros),
    buscar: codigo => requisicao(rota + "/" + id(codigo)),
    cadastrar: dados => gravar(rota, dados),
    atualizar: (codigo, dados) => gravar(rota + "/" + id(codigo), dados, "PUT"),
    excluir: codigo => requisicao(rota + "/" + id(codigo) + "/exclusao", {
      metodo: "DELETE"
    }),
    desativar: codigo => requisicao(rota + "/" + id(codigo), {
      metodo: "DELETE"
    })
  });
  return {
    veiculos: {
      ...cadastro("/api/veiculos"),
      revisao: (codigo, dados) => gravar("/api/veiculos/" + id(codigo) + "/revisao", dados, "PUT")
    },
    servicos: {
      ...cadastro("/api/servicos"),
      categorias: () => requisicao("/api/servicos/categorias"),
      cadastrarCategoria: dados => gravar("/api/servicos/categorias", dados)
    },
    funcionarios: {
      excluir: codigo => requisicao('/api/funcionarios/' + id(codigo) + '/exclusao', { metodo: 'DELETE' }),
      listar: parametros => consulta("/api/funcionarios", parametros),
      cadastrar: dados => gravar("/api/funcionarios", dados),
      atualizar: (codigo, dados) => gravar("/api/funcionarios/" + id(codigo), dados, "PUT"),
      alterarPin: (codigo, dados) => gravar("/api/funcionarios/" + id(codigo) + "/pin", dados, "PUT"),
      jornadas: codigo => requisicao("/api/funcionarios/" + id(codigo) + "/jornadas"),
      criarJornada: (codigo, dados) => gravar("/api/funcionarios/" + id(codigo) + "/jornadas", dados),
      configurarSemana: (codigo, dados) => gravar("/api/funcionarios/" + id(codigo) + "/jornadas/semana", dados),
      encerrarJornada: (codigo, fim) => gravar("/api/funcionarios/jornadas/" + id(codigo) + "/encerramento?fim=" + id(fim), undefined, "PUT")
    },
    ordens: {
      listar: parametros => consulta("/api/ordens", parametros),
      buscar: codigo => requisicao("/api/ordens/" + id(codigo)),
      abrir: dados => gravar("/api/ordens", dados),
      adicionarItem: (codigo, dados) => gravar("/api/ordens/" + id(codigo) + "/itens", dados),
      status: (codigo, dados) => gravar("/api/ordens/" + id(codigo) + "/status", dados, "PUT"),
      aprovar: (codigo, dados) => gravar("/api/ordens/" + id(codigo) + "/aprovacao", dados, "PUT"),
      concluirItem: (codigo, dados) => gravar("/api/ordens/itens/" + id(codigo) + "/conclusao", dados, "PUT"),
      iniciarExecucao: dados => gravar("/api/ordens/execucoes", dados),
      encerrarExecucao: codigo => gravar("/api/ordens/execucoes/" + id(codigo) + "/encerramento", undefined, "PUT"),
      execucoes: (codigo, parametros) => consulta("/api/ordens/itens/" + id(codigo) + "/execucoes", parametros)
    },
    financeiro: {
      analises: parametros => consulta("/api/financeiro/analises", parametros),
      extrato: mes => consulta("/api/financeiro/extrato", {
        mes
      }),
      sincronizarExtrato: mes => gravar("/api/financeiro/extrato/sincronizacao?mes=" + id(mes)),
      receber: dados => gravar("/api/financeiro/pagamentos", dados),
      pagamentos: parametros => consulta("/api/financeiro/pagamentos", parametros),
      estornar: dados => gravar("/api/financeiro/estornos", dados),
      estornos: parametros => consulta("/api/financeiro/estornos", parametros),
      criarDespesa: dados => gravar("/api/financeiro/despesas", dados),
      despesas: parametros => consulta("/api/financeiro/despesas", parametros),
      pagarDespesa: codigo => gravar("/api/financeiro/despesas/" + id(codigo) + "/pagamento", undefined, "PUT")
    },
    tecnico: {
      criarRemap: dados => gravar("/api/tecnico/remaps", dados),
      remap: codigo => requisicao("/api/tecnico/remaps/" + id(codigo)),
      atualizarRemap: (codigo, dados) => gravar("/api/tecnico/remaps/" + id(codigo), dados, "PUT"),
      criarDiagnostico: dados => gravar("/api/tecnico/diagnosticos", dados),
      diagnostico: codigo => requisicao("/api/tecnico/diagnosticos/" + id(codigo)),
      atualizarDiagnostico: (codigo, dados) => gravar("/api/tecnico/diagnosticos/" + id(codigo), dados, "PUT")
    },
    indicadores: parametros => consulta("/api/dashboard/indicadores", parametros),
    atualizarCliente: (codigo, dados) => gravar("/api/clientes/" + id(codigo), dados, "PUT"),
    ajustarPonto: dados => gravar("/api/ponto/ajustes", dados),
    ajustePonto: codigo => requisicao("/api/ponto/ajustes/" + id(codigo)),
    resumoPonto: parametros => consulta("/api/ponto/resumo", parametros),
    alterarSenha: dados => gravar("/api/acesso/senha", dados, "PUT"),
    dashboard: mes => requisicao("/api/dashboard" + (mes ? `?mes=${encodeURIComponent(mes)}` : "")),
    clientes: ({
      nome = "",
      pagina = 0,
      tamanho = 20
    } = {}) => requisicao("/api/clientes?" + new URLSearchParams({
      nome,
      pagina,
      tamanho
    })),
    cliente: id => requisicao(`/api/clientes/${encodeURIComponent(id)}`),
    cadastrarCliente: dados => requisicao("/api/clientes", {
      metodo: "POST",
      dados
    }),
    excluirCliente: codigo => requisicao("/api/clientes/" + id(codigo) + "/exclusao", {
      metodo: "DELETE"
    }),
    desativarCliente: id => requisicao(`/api/clientes/${encodeURIComponent(id)}`, {
      metodo: "DELETE"
    }),
    // Preserve requisicaoId ao repetir a mesma confirmacao apos falha de rede.
    registrarPonto: dados => requisicao("/api/ponto/marcacoes", {
      metodo: "POST",
      dados
    }),
    historicoPonto: parametros => requisicao("/api/ponto/marcacoes?" + new URLSearchParams(parametros))
  };
}
