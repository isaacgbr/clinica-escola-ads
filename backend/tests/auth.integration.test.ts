import 'dotenv/config';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { test } from 'node:test';
import { app } from '../src/app.js';
import { getPrisma } from '../src/repositories/prisma.js';
import { configAuth } from '../src/config/auth.js';
import { hashToken } from '../src/services/auth.service.js';
import { criarSessao, renovarAtividade, revogarSessao } from '../src/repositories/sessoes.repository.js';
import { gerarHashSenha } from '../src/utils/senha.js';
import { exigirBancoTeste, falhaIntegracao } from './integracao-segura.js';

test('renovacao concorrente preserva a maior atividade sem rejeitar requisicoes fora de ordem', async () => {
  exigirBancoTeste();
  const prisma = getPrisma();
  const email = `teste-concorrencia-${randomUUID()}@example.com`;
  const base = Date.now() - 1000;
  const hash = hashToken(randomUUID());
  const limite = new Date(base - 30 * 60 * 1000);
  try {
    const usuario = await prisma.usuarios.create({ data: {
      nome_completo: 'Teste Concorrência', email, senha_hash: await gerarHashSenha(randomUUID()),
      data_nascimento: new Date('2000-01-01T00:00:00Z'), telefone: '81999999999',
    }, select: { id: true } });
    await criarSessao({ usuario_id: usuario.id, token_hash: hash, criado_em: new Date(base),
      ultima_atividade_em: new Date(base), expira_em: new Date(base + 60 * 60 * 1000) }, null);

    assert.ok(await renovarAtividade(hash, new Date(base + 100), limite));
    assert.ok(await renovarAtividade(hash, new Date(base + 10), limite), 'Requisição anterior deve continuar autenticada.');
    let sessao = await prisma.sessoes.findUniqueOrThrow({ where: { token_hash: hash } });
    assert.equal(sessao.ultima_atividade_em.getTime(), base + 100);

    const instantes = [100, 150, 50, 200, 180, 200];
    const resultados = await Promise.all(instantes.map(ms => renovarAtividade(hash, new Date(base + ms), limite)));
    assert.ok(resultados.every(resultado => resultado !== null), 'Todas as requisições válidas devem ser aceitas.');
    sessao = await prisma.sessoes.findUniqueOrThrow({ where: { token_hash: hash } });
    assert.equal(sessao.ultima_atividade_em.getTime(), base + 200);

    assert.equal(await renovarAtividade(hash, sessao.expira_em, limite), null, 'Expiração absoluta deve continuar ativa.');
    assert.equal(await renovarAtividade(hash, new Date(base + 31 * 60 * 1000),
      new Date(base + 60 * 1000)), null, 'Sessão inativa não deve ser reativada.');
    await revogarSessao(hash, new Date(base + 201));
    assert.equal(await renovarAtividade(hash, new Date(base + 300), limite), null, 'Logout deve impedir renovação.');
    sessao = await prisma.sessoes.findUniqueOrThrow({ where: { token_hash: hash } });
    assert.equal(sessao.ultima_atividade_em.getTime(), base + 200);
  } catch (error) {
    throw falhaIntegracao('concorrencia', error);
  } finally {
    try { await prisma.usuarios.deleteMany({ where: { email } }); }
    catch (error) { throw falhaIntegracao('limpeza', error); }
    finally { await prisma.$disconnect(); }
  }
});

test('autenticacao persistente: cadastro, credenciais, cookies, CSRF, expiracao, rotacao e logout', async () => {
  exigirBancoTeste();
  const prisma = getPrisma();
  const email = `teste-auth-${randomUUID()}@example.com`;
  const senha = 'Teste-local-123!';
  const server = app.listen(0, '127.0.0.1');
  await new Promise<void>(resolve => server.once('listening', resolve));
  const address = server.address();
  assert.ok(address && typeof address === 'object');
  const base = `http://127.0.0.1:${address.port}`;
  const headers = { Origin: configAuth().origem, 'X-Requested-With': 'XMLHttpRequest', 'Content-Type': 'application/json' };
  const post = (path: string, body: unknown, cookie?: string) => fetch(base + path, {
    method: 'POST', headers: { ...headers, ...(cookie && { Cookie: cookie }) }, body: JSON.stringify(body),
  });
  const me = (cookie?: string) => fetch(base + '/auth/me', { headers: cookie ? { Cookie: cookie } : {} });
  const cookieOf = (response: Response) => {
    const value = response.headers.get('set-cookie');
    assert.ok(value);
    assert.ok(value.includes('HttpOnly') && value.includes('SameSite=Lax') && value.includes('Path=/'));
    assert.ok(value.includes('Secure') === configAuth().cookie.secure);
    return value.split(';')[0];
  };
  const tokenOf = (cookie: string) => cookie.slice(cookie.indexOf('=') + 1);
  try {
    const cadastro = await post('/usuarios', { nome: 'Teste Auth', email, senha, confirmar_senha: senha,
      nascimento: '2000-02-29', telefone: '(81) 99999-9999' });
    assert.equal(cadastro.status, 201);
    assert.equal((await fetch(base + '/health')).status, 200);
    assert.equal((await me()).status, 401);

    const incorreto = await post('/auth/login', { email, senha: 'Outra-senha-123!' });
    const inexistente = await post('/auth/login', { email: `nao-${email}`, senha });
    assert.equal(incorreto.status, 401);
    assert.equal(inexistente.status, 401);
    assert.deepEqual(await incorreto.json(), await inexistente.json());
    assert.ok(!incorreto.headers.has('set-cookie'));
    assert.equal(await prisma.sessoes.count({ where: { usuarios: { email } } }), 0);

    const login = await post('/auth/login', { email: email.toUpperCase(), senha });
    assert.equal(login.status, 200);
    assert.equal(login.headers.get('cache-control'), 'no-store');
    const body = await login.json();
    assert.deepEqual(Object.keys(body), ['usuario']);
    assert.deepEqual(Object.keys(body.usuario).sort(), ['id', 'nome']);
    assert.equal(typeof body.usuario.id, 'string');
    const cookie = cookieOf(login);
    const hash = hashToken(tokenOf(cookie));
    const sessao = await prisma.sessoes.findUniqueOrThrow({ where: { token_hash: hash } });
    assert.ok(sessao.token_hash !== tokenOf(cookie));
    assert.ok(sessao.expira_em.getTime() - sessao.criado_em.getTime() === 8 * 60 * 60 * 1000);
    assert.equal((await me(cookie)).status, 200);
    await prisma.$disconnect();
    assert.equal((await me(cookie)).status, 200);

    const csrf = await fetch(base + '/auth/logout', { method: 'POST', headers: { Cookie: cookie, 'Content-Type': 'application/json' }, body: '{}' });
    assert.equal(csrf.status, 403);
    assert.equal((await me(cookie)).status, 200);
    const rotated = await post('/auth/login', { email, senha }, cookie);
    assert.equal(rotated.status, 200);
    const novoCookie = cookieOf(rotated);
    assert.ok(novoCookie !== cookie);
    assert.equal((await me(cookie)).status, 401);
    const anterior = await prisma.sessoes.findUniqueOrThrow({ where: { token_hash: hash } });
    assert.ok(anterior.revogada_em !== null);
    const response = await post('/auth/logout', {}, novoCookie);
    assert.equal(response.status, 204);
    assert.ok(response.headers.get('set-cookie')?.includes('Expires=Thu, 01 Jan 1970'));
    assert.equal((await me(novoCookie)).status, 401);

    for (const tipo of ['absoluta', 'inatividade']) {
      const entrada = await post('/auth/login', { email, senha });
      assert.equal(entrada.status, 200);
      const cookieExpirado = cookieOf(entrada);
      const agora = Date.now();
      await prisma.sessoes.update({ where: { token_hash: hashToken(tokenOf(cookieExpirado)) }, data: {
        criado_em: new Date(agora - 9 * 60 * 60 * 1000),
        ultima_atividade_em: new Date(agora - 31 * 60 * 1000),
        expira_em: new Date(agora + (tipo === 'absoluta' ? -1000 : 60 * 60 * 1000)),
      } });
      assert.equal((await me(cookieExpirado)).status, 401);
    }

    // A consulta de email nao pode interpretar caracteres de padrao SQL.
    const wildcard = email.replace('teste-auth', 'teste%');
    assert.equal((await post('/auth/login', { email: wildcard, senha })).status, 401);
    for (let i = 0; i < 7; i++) {
      const response = await post('/auth/login', { email, senha });
      assert.equal(response.status, 200);
      assert.equal((await post('/auth/logout', {}, cookieOf(response))).status, 204);
    }
    for (let i = 0; i < 5; i++) assert.equal((await post('/auth/login', { email, senha: 'Outra-senha-123!' })).status, 401);
    const blocked = await post('/auth/login', { email: email.toUpperCase(), senha: 'Outra-senha-123!' });
    assert.equal(blocked.status, 429);
    assert.ok(blocked.headers.has('retry-after'));
  } catch (error) {
    throw falhaIntegracao('autenticacao', error);
  } finally {
    try {
      // A FK remove somente as sessoes do usuario exclusivo deste teste.
      await prisma.usuarios.deleteMany({ where: { email } });
    } catch (error) {
      throw falhaIntegracao('limpeza', error);
    } finally {
      await prisma.$disconnect();
      await new Promise<void>(resolve => server.close(() => resolve()));
    }
  }
});
