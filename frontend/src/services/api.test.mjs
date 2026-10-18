import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const fonte = await readFile(new URL("./api.js", import.meta.url), "utf8");
const { criarApi } = await import("data:text/javascript;base64," + Buffer.from(fonte).toString("base64"));

test('jornadas e descrição do trabalho usam endpoints protegidos com CSRF', async () => {
    const chamadas = [];
    const api = criarApi({ usuario: 'proprietario', senha: 'teste', fetchImpl: async (url, opcoes) => {
        chamadas.push({ url, opcoes });
        return Response.json(url.endsWith('/api/csrf') ? { headerName: 'X-CSRF-TOKEN', token: 'csrf' } : {});
    } });
    await api.funcionarios.criarJornada('f/1', { dia: 'MONDAY', entrada: '08:00', saida: '17:00', intervaloMinutos: 60, toleranciaMinutos: 5, vigenciaInicio: '2026-10-01' });
    await api.funcionarios.encerrarJornada('j/1', '2026-10-31');
    await api.ordens.concluirItem('item', { custoReal: 80, relato: 'Troca de óleo e filtro.' });
    assert.ok(chamadas[1].url.endsWith('/api/funcionarios/f%2F1/jornadas'));
    assert.equal(chamadas[1].opcoes.method, 'POST');
    assert.ok(chamadas[3].url.endsWith('/api/funcionarios/jornadas/j%2F1/encerramento?fim=2026-10-31'));
    assert.equal(chamadas[3].opcoes.method, 'PUT');
    assert.equal(JSON.parse(chamadas[5].opcoes.body).relato, 'Troca de óleo e filtro.');
    assert.equal(chamadas[5].opcoes.headers['X-CSRF-TOKEN'], 'csrf');
});

test("cadastro local usa CSRF sem enviar login ou senha", async () => {
    const chamadas = [];
    const api = criarApi({ usuario: "admin", senha: "senha-de-teste", fetchImpl: async (url, opcoes) => {
        chamadas.push({ url, opcoes });
        return Response.json(url.endsWith("/api/csrf") ? { headerName: "X-CSRF-TOKEN", token: "token-teste" } : { id: "cliente" });
    }});
    assert.equal((await api.cadastrarCliente({ tipo: "PF", nome: "João" })).id, "cliente");
    assert.equal(chamadas.length, 2);
    assert.equal(chamadas[1].opcoes.headers["X-CSRF-TOKEN"], "token-teste");
    assert.equal(chamadas[1].opcoes.credentials, "include");
    assert.equal(chamadas[1].opcoes.headers.Authorization, undefined);
});

test("ponto publico usa CSRF e preserva requisicaoId sem enviar autorização administrativa", async () => {
    const chamadas = [];
    const api = criarApi({ usuario: "admin", senha: "senha-de-teste", fetchImpl: async (url, opcoes) => {
        chamadas.push({ url, opcoes });
        return Response.json(url.endsWith('/api/csrf')
            ? { headerName: 'X-CSRF-TOKEN', token: 'token-ponto' } : { id: "marcacao" });
    }});
    const dados = { matricula: "OF-001", pin: "123456", tipo: "ENTRADA", requisicaoId: "pedido-original" };
    await api.registrarPonto(dados);
    await api.registrarPonto(dados);
    assert.equal(chamadas.length, 4);
    for (const chamada of chamadas) {
        assert.equal(chamada.opcoes.credentials, "include");
        assert.equal(chamada.opcoes.headers.Authorization, undefined);
    }
    for (const chamada of [chamadas[1], chamadas[3]]) {
        assert.equal(chamada.opcoes.headers['X-CSRF-TOKEN'], 'token-ponto');
        assert.deepEqual(JSON.parse(chamada.opcoes.body), dados);
    }
});

test('ponto não envia marcação quando a obtenção do CSRF falha', async () => {
    const urls = [];
    const api = criarApi({ fetchImpl: async url => {
        urls.push(url);
        return Response.json({ detail: 'CSRF indisponível.' }, { status: 503 });
    }});
    await assert.rejects(api.registrarPonto({ matricula: '001', pin: '123456' }), { message: 'CSRF indisponível.' });
    assert.deepEqual(urls, ['http://localhost:8080/api/csrf']);
});

test("propaga Problem Details sem esconder status de validacao", async () => {
    const api = criarApi({ fetchImpl: async () => Response.json({ detail: "Marcação duplicada." }, { status: 409 }) });
    await assert.rejects(api.dashboard(), erro => erro.status === 409 && erro.message === "Marcação duplicada.");
});

test("desativacao aceita resposta 204 sem tentar decodificar JSON", async () => {
    const api = criarApi({ fetchImpl: async url => url.endsWith("/api/csrf")
        ? Response.json({ headerName: "X-CSRF-TOKEN", token: "teste" }) : new Response(null, { status: 204 }) });
    assert.equal(await api.desativarCliente("id"), undefined);
});

test("atualizacoes dos modulos usam PUT, CSRF e codificam identificadores", async () => {
    const chamadas = [];
    const api = criarApi({ fetchImpl: async (url, opcoes) => {
        chamadas.push({ url, opcoes });
        return url.endsWith("/api/csrf")
            ? Response.json({ headerName: "X-CSRF-TOKEN", token: "teste" }) : Response.json({});
    }});
    await api.veiculos.atualizar("id/externo", { marca: "Fiat" });
    await api.ordens.aprovar("ordem", { consentimento: true });
    await api.financeiro.pagarDespesa("despesa");
    await api.tecnico.atualizarDiagnostico("diag", { causa: "Falha" });
    await api.funcionarios.alterarPin("funcionario", { pin: "987654" });
    for (const chamada of chamadas.filter(c => !c.url.endsWith("/api/csrf"))) {
        assert.equal(chamada.opcoes.method, "PUT");
        assert.equal(chamada.opcoes.headers["X-CSRF-TOKEN"], "teste");
    }
    assert.equal(chamadas[1].url, "http://localhost:8080/api/veiculos/id%2Fexterno");
    assert.equal(chamadas[5].opcoes.body, undefined);
});

test("consultas preservam filtros e omitem parametros nao definidos", async () => {
    const urls = [];
    const api = criarApi({ fetchImpl: async url => { urls.push(url); return Response.json({}); } });
    await api.indicadores({ mes: "2026-10", meses: 6, ignorado: undefined });
    await api.resumoPonto({ funcionarioId: "a/b", inicio: "2026-10-01", fim: "2026-10-31" });
    await api.financeiro.pagamentos({ ordemId: "ordem", pagina: 1 });
    assert.equal(urls[0], "http://localhost:8080/api/dashboard/indicadores?mes=2026-10&meses=6");
    assert.ok(urls[1].includes("funcionarioId=a%2Fb"));
    assert.ok(urls[2].endsWith("ordemId=ordem&pagina=1"));
});

test("pagamentos e ajustes preservam a chave de idempotencia enviada pela tela", async () => {
    const corpos = [];
    const api = criarApi({ fetchImpl: async (url, opcoes) => {
        if (url.endsWith("/api/csrf")) return Response.json({ headerName: "X-CSRF-TOKEN", token: "teste" });
        corpos.push(JSON.parse(opcoes.body)); return Response.json({});
    }});
    const pagamento = { ordemServicoId: "os", requisicaoId: "pedido", valor: 200, forma: "PIX" };
    await api.financeiro.receber(pagamento);
    await api.financeiro.receber(pagamento);
    await api.ajustarPonto({ funcionarioId: "f", requisicaoId: "ajuste", motivo: "Esquecimento" });
    assert.deepEqual(corpos[0], corpos[1]);
    assert.equal(corpos[2].requisicaoId, "ajuste");
});


test('aplicação inicia conectada sem formulário de login', async () => {
    const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
    assert.match(app, /else\s*\{\s*await conectar\(\);\s*\}/);
    assert.ok(!app.includes('connection-form'));
    assert.ok(!app.includes('autocomplete="current-password"'));
});
