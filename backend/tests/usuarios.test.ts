import assert from 'node:assert/strict';
import { test } from 'node:test';
import express from 'express';
import { app } from '../src/app.js';
import { validarCadastro } from '../src/services/usuarios.service.js';
import { ApiError, errorMiddleware } from '../src/middlewares/error.middleware.js';

const valido = {
  nome: ' Maria Silva ', email: ' Maria@Example.com ', senha: 'Teste-local-123!',
  confirmar_senha: 'Teste-local-123!', nascimento: '2000-02-29', telefone: '(81) 99999-9999',
};

test('normaliza dados e preserva data e senha', () => {
  const dados = validarCadastro(valido);
  assert.equal(dados.nome, 'Maria Silva');
  assert.equal(dados.email, 'maria@example.com');
  assert.equal(dados.telefone, '81999999999');
  assert.equal(dados.data.toISOString(), '2000-02-29T00:00:00.000Z');
  assert.ok(dados.senha === valido.senha);
});

for (const campo of Object.keys(valido)) {
  for (const value of [undefined, null, 123, '', []]) {
    test(`rejeita tipo inválido ou ausência de ${campo}: ${typeof value}/${String(value)}`, () => {
      assert.throws(() => validarCadastro({ ...valido, [campo]: value }), ApiError);
    });
  }
}

const invalidos = [
  { nome: '   ' }, { nome: 'x'.repeat(151) },
  { email: 'a@b' }, { email: 'a..b@example.com' }, { email: 'a@-example.com' },
  { email: `${'a'.repeat(140)}@example.com` },
  { senha: 'curta', confirmar_senha: 'curta' },
  { senha: 'x'.repeat(129), confirmar_senha: 'x'.repeat(129) },
  { confirmar_senha: 'diferente' }, { nascimento: '2025-02-29' },
  { nascimento: '2000-02-30' }, { nascimento: '31/12/2000' },
  { nascimento: '9999-01-01' }, { nascimento: '0000-01-01' },
  { telefone: '819999999' }, { telefone: '819999999999' },
  { telefone: 'abc81999999999' }, { telefone: '+5581999999999' },
  { id: '1' }, { criado_em: '2000-01-01' }, { atualizado_em: '2000-01-01' },
  { senha_hash: 'nao-permitido' }, { outro: 'campo' },
];
invalidos.forEach((dados, index) => {
  test(`rejeita entrada inválida ${index + 1}`, () => {
    assert.throws(() => validarCadastro({ ...valido, ...dados }), ApiError);
  });
});

test('aceita limites e telefone de dez dígitos', () => {
  validarCadastro({ ...valido, nome: 'x'.repeat(150), telefone: '8133334444',
    senha: 'x'.repeat(128), confirmar_senha: 'x'.repeat(128) });
});

test('rejeita NUL isolado ou inserido no nome', () => {
  for (const nome of ['\u0000', 'Maria\u0000Silva']) {
    assert.throws(() => validarCadastro({ ...valido, nome }),
      (error: unknown) => error instanceof ApiError && error.status === 400 && !!error.campos?.nome);
  }
});

for (const [agora, hoje, amanha] of [
  ['2026-10-29T01:00:00.000Z', '2026-10-28', '2026-10-29'],
  ['2026-10-29T02:59:59.999Z', '2026-10-28', '2026-10-29'],
  ['2026-10-29T03:00:00.000Z', '2026-10-29', '2026-10-30'],
  ['2027-01-01T01:00:00.000Z', '2026-12-31', '2027-01-01'],
]) {
  test(`valida nascimento pelo dia de São Paulo em ${agora}`, t => {
    t.mock.timers.enable({ apis: ['Date'], now: new Date(agora) });
    assert.equal(validarCadastro({ ...valido, nascimento: hoje }).data.toISOString(), `${hoje}T00:00:00.000Z`);
    assert.throws(() => validarCadastro({ ...valido, nascimento: amanha }),
      (error: unknown) => error instanceof ApiError && error.status === 400 && !!error.campos?.nascimento);
  });
}

test('contrato HTTP: health, JSON inválido, corpo excessivo, tipo e erro interno', async t => {
  t.mock.timers.enable({ apis: ['Date'], now: new Date('2026-10-29T01:00:00.000Z') });
  const server = app.listen(0, '127.0.0.1');
  await new Promise<void>(resolve => server.once('listening', resolve));
  const address = server.address();
  assert.ok(address && typeof address === 'object');
  const base = `http://127.0.0.1:${address.port}`;
  try {
    assert.equal((await fetch(`${base}/health`)).status, 200);
    for (const [campo, valor] of [['nome', 'Maria\u0000Silva'], ['nascimento', '2026-10-29']]) {
      const response = await fetch(`${base}/usuarios`, { method: 'POST',
        headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...valido, [campo]: valor }) });
      assert.equal(response.status, 400);
      const body = await response.json();
      assert.equal(body.erro.codigo, 'DADOS_INVALIDOS');
      assert.equal(typeof body.erro.campos[campo], 'string');
    }
    for (const body of ['{', 'null', '[]', '{}', JSON.stringify({ ...valido, id: '1' }), JSON.stringify({ nome: 'x'.repeat(17000) })]) {
      const response = await fetch(`${base}/usuarios`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body });
      assert.equal(response.status, 400);
      assert.equal((await response.json()).erro.codigo, 'DADOS_INVALIDOS');
    }
    const response = await fetch(`${base}/usuarios`, { method: 'POST', body: 'nome=Maria' });
    assert.equal(response.status, 400);
  } finally {
    await new Promise<void>((resolve, reject) => server.close(error => error ? reject(error) : resolve()));
  }

  const failingApp = express();
  failingApp.get('/', () => { throw new Error('detalhe-interno-proibido'); });
  failingApp.use(errorMiddleware);
  const failingServer = failingApp.listen(0, '127.0.0.1');
  await new Promise<void>(resolve => failingServer.once('listening', resolve));
  try {
    const address = failingServer.address();
    assert.ok(address && typeof address === 'object');
    const response = await fetch(`http://127.0.0.1:${address.port}`);
    assert.equal(response.status, 500);
    assert.deepEqual(await response.json(), { erro: { codigo: 'ERRO_INTERNO', mensagem: 'Não foi possível concluir a operação.' } });
  } finally {
    await new Promise<void>((resolve, reject) => failingServer.close(error => error ? reject(error) : resolve()));
  }
});
