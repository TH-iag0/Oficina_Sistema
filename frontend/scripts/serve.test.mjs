import test from 'node:test';
import assert from 'node:assert/strict';
import { arquivoPublico, servidor } from './serve.mjs';

test('servidor só expõe interface e scripts públicos, sem arquivos internos', () => {
    assert.ok(arquivoPublico('/ponto').endsWith('index.html'));
    assert.ok(arquivoPublico('/src/assets/logo.svg').endsWith('logo.svg'));
    assert.ok(arquivoPublico('/src/app.js').endsWith('app.js'));
    for (const path of ['/pom.xml', '/package.json', '/.git/config', '/scripts/serve.mjs', '/src/lib/ui.test.mjs', '/src/..%2f..%2fpom.xml']) {
        assert.equal(arquivoPublico(path), null, path);
    }
});

test('servidor entrega HTML e módulos com MIME correto e rejeita escrita', async () => {
    const server = servidor();
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    const base = `http://127.0.0.1:${server.address().port}`;
    try {
        const pagina = await fetch(base + '/ponto');
        assert.equal(pagina.status, 200);
        assert.match(pagina.headers.get('content-type'), /text\/html/);
        assert.ok((await pagina.text()).includes('/src/app.js'));
        const estiloCarro = await fetch(base + '/src/assets/carro-background.css');
        assert.equal(estiloCarro.status, 200);
        assert.match(estiloCarro.headers.get('content-type'), /text\/css/);
        const imagemIncorporada = (await estiloCarro.text()).match(/data:image\/png;base64,([A-Za-z0-9+/=]+)/);
        assert.ok(imagemIncorporada, 'O carro deve carregar mesmo em servidores sem suporte a PNG');
        for (const nome of ['logo', 'carro']) {
            const asset = await fetch(base + `/src/assets/${nome}.svg`);
            assert.equal(asset.status, 200);
            assert.match(asset.headers.get('content-type'), /image\/svg\+xml/);
            assert.ok((await asset.text()).includes('<svg'));
        }
        const carro = await fetch(base + '/src/assets/carro-blueprint.png');
        assert.equal(carro.status, 200);
        assert.equal(carro.headers.get('content-type'), 'image/png');
        const imagemCarro = Buffer.from(await carro.arrayBuffer());
        assert.deepEqual([...imagemCarro.slice(0, 8)], [137, 80, 78, 71, 13, 10, 26, 10]);
        assert.deepEqual(Buffer.from(imagemIncorporada[1], 'base64'), imagemCarro);
        const script = await fetch(base + '/src/app.js');
        assert.match(script.headers.get('content-type'), /text\/javascript/);
        assert.equal((await fetch(base + '/.git/config')).status, 404);
        assert.equal((await fetch(base + '/', { method: 'POST' })).status, 405);
        assert.equal((await fetch(base + '/%broken')).status, 404);
    } finally {
        await new Promise(resolve => server.close(resolve));
    }
});
