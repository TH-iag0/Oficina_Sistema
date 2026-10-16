export function origemBackend(valor) {
    const url = new URL(valor);
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash || url.pathname !== '/') {
        throw new Error('Informe a origem do backend, como http://localhost:8080, sem usuário, senha ou caminhos.');
    }
    return url.origin;
}

export const linkPonto = backend => '/ponto?api=' + encodeURIComponent(origemBackend(backend));
export const backendPonto = busca => origemBackend(new URLSearchParams(busca).get('api') || 'http://localhost:8080');
