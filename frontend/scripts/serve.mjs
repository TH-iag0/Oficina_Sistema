import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { dirname, extname, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const types = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml', '.png': 'image/png' };

export function arquivoPublico(url) {
    const path = decodeURIComponent(new URL(url, 'http://localhost').pathname);
    if (path === '/' || path === '/ponto') return resolve(root, 'index.html');
    const file = resolve(root, '.' + path);
    const source = resolve(root, 'src') + sep;
    if (!file.startsWith(source) || !types[extname(file)] || /(?:test\.|\.test\.)/.test(file)) return null;
    return file;
}

export function servidor() {
    return createServer(async (request, response) => {
        response.setHeader('X-Content-Type-Options', 'nosniff');
        response.setHeader('Cache-Control', 'no-store');
        if (request.method !== 'GET' && request.method !== 'HEAD') {
            response.writeHead(405, { Allow: 'GET, HEAD' }).end();
            return;
        }
        try {
            const file = arquivoPublico(request.url);
            if (!file) { response.writeHead(404).end(); return; }
            const content = await readFile(file);
            const type = types[extname(file)];
            response.writeHead(200, { 'Content-Type': type + (type.startsWith('text/') ? '; charset=utf-8' : '') });
            response.end(request.method === 'HEAD' ? undefined : content);
        } catch {
            response.writeHead(404).end();
        }
    });
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    const server = servidor();
    server.on('error', error => { console.error(error.message); process.exitCode = 1; });
    server.listen(5173, '127.0.0.1', () => console.log('AutoGestão: http://localhost:5173 · Ponto: http://localhost:5173/ponto'));
}
