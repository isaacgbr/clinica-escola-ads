import { diagnosticoErro } from '../src/utils/diagnostico-erro.js';

export function exigirBancoTeste(env: NodeJS.ProcessEnv = process.env): void {
  if (env.NODE_ENV !== 'test' || env.ALLOW_DB_TESTS !== '1') {
    throw new Error('Integração bloqueada: exige NODE_ENV=test e ALLOW_DB_TESTS=1.');
  }
  let url: URL;
  try { url = new URL(env.DATABASE_URL ?? ''); }
  catch { throw new Error('Integração bloqueada: conexão de teste inválida.'); }
  if (!['postgresql:', 'postgres:'].includes(url.protocol) ||
      !['localhost', '127.0.0.1', '[::1]'].includes(url.hostname) ||
      !/^\/[a-z0-9_]+_(test|teste)$/.test(url.pathname) || url.search || url.hash) {
    throw new Error('Integração bloqueada: use PostgreSQL local com banco terminado em _test ou _teste.');
  }
}

export function falhaIntegracao(etapa: 'cadastro' | 'autenticacao' | 'concorrencia' | 'limpeza', error: unknown): Error {
  const diagnostico = diagnosticoErro(error);
  const local = error instanceof Error
    ? error.stack?.match(/(?:usuarios|auth)\.integration\.test\.ts:\d{1,6}:\d{1,6}/)?.[0] : undefined;
  return new Error(`Falha de integração (${etapa}): ${diagnostico.tipo}/${diagnostico.codigo}; local: ${local ?? 'indisponivel'}.`);
}
