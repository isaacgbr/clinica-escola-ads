import 'dotenv/config';
import assert from 'node:assert/strict';
import { randomUUID, scryptSync } from 'node:crypto';
import { test } from 'node:test';
import { app } from '../src/app.js';
import { getPrisma } from '../src/repositories/prisma.js';
import { inserirUsuario } from '../src/repositories/usuarios.repository.js';
import { ApiError } from '../src/middlewares/error.middleware.js';
import { exigirBancoTeste, falhaIntegracao } from './integracao-segura.js';
import { ipKeyGenerator } from 'express-rate-limit';
import { limiteCadastro } from '../src/middlewares/cadastro-limit.middleware.js';

// Este teste grava somente registros identificados por UUID e os remove ao final.
test('cadastro HTTP, persistência, hash, duplicidade e concorrência no PostgreSQL', async () => {
  exigirBancoTeste();
  const prisma = getPrisma();
  const prefix = `teste-${randomUUID()}`;
  const emails = [`${prefix}@example.com`, `${prefix}-race@example.com`, `${prefix}-legacy@example.com`];
  const server = app.listen(0, '127.0.0.1');
  await new Promise<void>(resolve => server.once('listening', resolve));
  const address = server.address();
  assert.ok(address && typeof address === 'object');
  const base = `http://127.0.0.1:${address.port}`;
  const dados = { nome: ' Teste Integração ', email: emails[0], senha: 'Teste-local-123!',
    confirmar_senha: 'Teste-local-123!', nascimento: '2000-02-29', telefone: '(81) 99999-9999' };
  const post = (email: string) => fetch(`${base}/usuarios`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...dados, email }),
  });
  try {
    const response = await post(emails[0]);
    assert.equal(response.status, 201);
    const body = await response.json();
    assert.deepEqual(Object.keys(body), ['usuario']);
    assert.deepEqual(Object.keys(body.usuario).sort(), ['id', 'nome']);
    assert.equal(typeof body.usuario.id, 'string');
    const stored = await prisma.usuarios.findUnique({ where: { id: BigInt(body.usuario.id) } });
    assert.ok(stored);
    assert.equal(stored.nome_completo, 'Teste Integração');
    assert.equal(stored.email, emails[0]);
    assert.equal(stored.telefone, '81999999999');
    assert.equal(stored.data_nascimento.toISOString(), '2000-02-29T00:00:00.000Z');
    assert.ok(stored.senha_hash !== dados.senha && stored.senha_hash.length <= 255);
    const [algorithm, n, r, p, salt, key] = stored.senha_hash.split('$');
    assert.equal(algorithm, 'scrypt');
    assert.equal(salt.length, 32);
    const derived = scryptSync(dados.senha, salt, 64, { N: Number(n), r: Number(r), p: Number(p), maxmem: 64 * 1024 * 1024 });
    assert.ok(derived.toString('hex') === key);
    assert.equal((await post(emails[0].toUpperCase())).status, 409);
    assert.equal((await post(emails[0])).status, 409);
    const rows = await Promise.all([post(emails[1]), post(emails[1].toUpperCase())]);
    assert.deepEqual(rows.map(row => row.status).sort(), [201, 409]);
    const second = await prisma.usuarios.findFirstOrThrow({ where: { email: emails[1] } });
    assert.ok(second.senha_hash.split('$')[4] !== salt);
    await assert.rejects(inserirUsuario({ nome_completo: 'Teste', email: emails[0].toUpperCase(),
      senha_hash: stored.senha_hash, data_nascimento: stored.data_nascimento, telefone: stored.telefone }),
    (error: unknown) => error instanceof ApiError && error.status === 409);
    await prisma.usuarios.create({ data: { nome_completo: 'Teste legado', email: emails[2].toUpperCase(),
      senha_hash: stored.senha_hash, data_nascimento: stored.data_nascimento, telefone: stored.telefone } });
    // Isola a verificacao de duplicidade da cota ja consumida pelos cenarios anteriores.
    await limiteCadastro.resetKey(ipKeyGenerator('127.0.0.1'));
    assert.equal((await post(emails[2])).status, 409);
  } catch (error) {
    throw falhaIntegracao('cadastro', error);
  } finally {
    try {
      await prisma.usuarios.deleteMany({ where: { email: { in: emails.flatMap(email => [email, email.toUpperCase()]) } } });
    } catch (error) {
      throw falhaIntegracao('limpeza', error);
    } finally {
      await prisma.$disconnect();
      await new Promise<void>((resolve, reject) => server.close(error => error ? reject(error) : resolve()));
    }
  }
});
