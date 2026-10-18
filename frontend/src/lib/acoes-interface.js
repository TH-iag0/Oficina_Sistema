export async function executarAcao(botao, acoes) {
    for (const [nome, executar] of Object.entries(acoes)) {
        if (Object.hasOwn(botao.dataset, nome)) {
            await executar(botao.dataset[nome], botao);
            return true;
        }
    }
    return false;
}
