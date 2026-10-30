import assert from 'node:assert/strict';
import { test } from 'node:test';
import express from 'express';
import { app } from '../src/app.js';
import { configAuth } from '../src/config/auth.js';
import { gerarHashSenha, verificarSenha } from '../src/utils/senha.js';
import { lerTokenSessao } from '../src/utils/sessao-cookie.js';
import { validarLogin } from '../src/services/auth.service.js';
import { criarLimitesLogin } from '../src/middlewares/login-limit.middleware.js';
import { errorMiddleware } from '../src/middlewares/error.middleware.js';

test('scrypt: compatibilidade, salt aleatorio e verificacao segura de entradas invalidas', async () => {
  const senha = ' Teste-local-123! ';
  const hash = await gerarHashSenha(senha);
  assert.ok(await verificarSenha(senha, hash));
  assert.ok(!(await verificarSenha(senha.trim(), hash)));
  assert.ok(hash !== await gerarHashSenha(senha));
  for (const value of [null, 'invalido', hash.replace('32768', '999999999'), hash.slice(0, -2)]) {
    assert.ok(!(await verificarSenha(senha, value)));
  }
});

test('validacao do login rejeita corpos, tipos, campos extras e limites', () => {
  const valido = { email: ' Maria@Example.com ', senha: 'Teste-local-123!' };
  assert.equal(validarLogin(valido).email, 'maria@example.com');
  for (const value of [null, [], {}, { ...valido, id: '1' }, { ...valido, email: 42 },
    { ...valido, email: 'a@b' }, { ...valido, email: 'a'.repeat(151) + '@example.com' },
    { ...valido, senha: '' }, { ...valido, senha: 'x'.repeat(129) }, { ...valido, email: 'a\u0000@example.com' }]) {
    assert.throws(() => validarLogin(value));
  }
});

test('configuracao de origem e cookies falha de forma fechada em producao', () => {
  const anterior = { origin: process.env.AUTH_ORIGIN, node: process.env.NODE_ENV };
  try {
    process.env.NODE_ENV = 'production';
    delete process.env.AUTH_ORIGIN;
    assert.throws(configAuth);
    for (const origem of ['http://localhost:3000', 'https://example.com/', 'https://example.com/path', 'https://user:pass@example.com']) {
      process.env.AUTH_ORIGIN = origem;
      assert.throws(configAuth);
    }
    process.env.AUTH_ORIGIN = 'https://example.com';
    const config = configAuth();
    assert.equal(config.nomeCookie, '__Host-clinica_sessao');
    assert.deepEqual(config.cookie, { httpOnly: true, secure: true, sameSite: 'lax', path: '/' });
  } finally {
    if (anterior.origin === undefined) delete process.env.AUTH_ORIGIN; else process.env.AUTH_ORIGIN = anterior.origin;
    if (anterior.node === undefined) delete process.env.NODE_ENV; else process.env.NODE_ENV = anterior.node;
  }
});

test('cookie: aceita somente um identificador valido e ignora nomes parecidos', () => {
  const nome = configAuth().nomeCookie;
  const token = 'a'.repeat(64);
  assert.ok(lerTokenSessao(`${nome}=${token}`) === token);
  for (const value of [undefined, `${nome}=abc`, `${nome}=${token}; ${nome}=${token}`, `${nome}_outro=${token}`, `${nome}=%61`, `${nome}="${token}"`]) {
    assert.equal(lerTokenSessao(value), null);
  }
});

test('HTTP: CSRF, JSON, sessao ausente e cache de autenticacao', async () => {
  const server = app.listen(0, '127.0.0.1');
  await new Promise<void>(resolve => server.once('listening', resolve));
  const address = server.address();
  assert.ok(address && typeof address === 'object');
  const base = `http://127.0.0.1:${address.port}`;
  const headers = { Origin: configAuth().origem, 'X-Requested-With': 'XMLHttpRequest', 'Content-Type': 'application/json' };
  try {
    for (const path of ['/auth/login', '/auth/logout']) {
      const changesList: Record<string, string>[] = [{ Origin: 'https://evil.example' }, { Origin: 'null' }, { Origin: '' },
        { 'X-Requested-With': '' }, { 'Sec-Fetch-Site': 'cross-site' }];
      for (const changes of changesList) {
        const response = await fetch(base + path, { method: 'POST', headers: { ...headers, ...changes }, body: '{}' });
        assert.equal(response.status, 403);
        assert.equal((await response.json()).erro.codigo, 'ORIGEM_NAO_PERMITIDA');
      }
    }
    const invalid = await fetch(base + '/auth/login', { method: 'POST', headers, body: '{}' });
    assert.equal(invalid.status, 400);
    const wrongType = await fetch(base + '/auth/login', { method: 'POST', headers: { ...headers, 'Content-Type': 'text/plain' }, body: '{}' });
    assert.equal(wrongType.status, 400);
    const response = await fetch(base + '/auth/me');
    assert.equal(response.status, 401);
    assert.equal(response.headers.get('cache-control'), 'no-store');
    assert.ok(response.headers.has('set-cookie'));
  } finally { await new Promise<void>(resolve => server.close(() => resolve())); }
});

test('limitadores: 5/email-IP, 20/IP, normalizacao, retry e sucesso sem consumir cota', async () => {
  const limites = criarLimitesLogin();
  const harness = express();
  harness.use(express.json());
  harness.post('/', limites.porIp, limites.porEmailIp, async (req, res) => {
    if (req.body.sucesso) { await limites.limparEmailIp(req); res.sendStatus(200); }
    else res.sendStatus(401);
  });
  harness.use(errorMiddleware);
  const server = harness.listen(0, '127.0.0.1');
  await new Promise<void>(resolve => server.once('listening', resolve));
  const address = server.address();
  assert.ok(address && typeof address === 'object');
  const base = `http://127.0.0.1:${address.port}`;
  const post = (email: string, sucesso = false) => fetch(base, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, sucesso }),
  });
  try {
    for (let i = 0; i < 6; i++) assert.equal((await post('sucesso@example.com', true)).status, 200);
    for (let i = 0; i < 4; i++) assert.equal((await post('reset@example.com')).status, 401);
    assert.equal((await post('reset@example.com', true)).status, 200);
    for (let i = 0; i < 5; i++) assert.equal((await post(' RESET@example.com ')).status, 401);
    const blocked = await post('reset@example.com');
    assert.equal(blocked.status, 429);
    assert.ok(blocked.headers.has('retry-after'));
    assert.equal((await blocked.json()).erro.codigo, 'MUITAS_TENTATIVAS');
    // Foram consumidas 10 tentativas malsucedidas no IP, incluindo o bloqueio acima.
    for (let i = 0; i < 10; i++) assert.equal((await post(`outro-${i}@example.com`)).status, 401);
    const ipBlocked = await post('novo@example.com');
    assert.equal(ipBlocked.status, 429);
  } finally { await new Promise<void>(resolve => server.close(() => resolve())); }
});
