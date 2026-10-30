export const DURACAO_SESSAO_MS = 8 * 60 * 60 * 1000;
export const INATIVIDADE_SESSAO_MS = 30 * 60 * 1000;

export function configAuth() {
  const producao = process.env.NODE_ENV === 'production';
  const origem = process.env.AUTH_ORIGIN || (producao ? '' : `http://localhost:${process.env.PORT || 3000}`);
  let url: URL;
  try { url = new URL(origem); } catch { throw new Error('Configure AUTH_ORIGIN com uma origem HTTP valida.'); }
  const local = ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname);
  if (url.origin !== origem || url.username || url.password ||
      (url.protocol !== 'https:' && (producao || url.protocol !== 'http:' || !local))) {
    throw new Error('AUTH_ORIGIN exige HTTPS, exceto no desenvolvimento em loopback.');
  }
  const secure = url.protocol === 'https:';
  return {
    origem,
    nomeCookie: secure ? '__Host-clinica_sessao' : 'clinica_sessao_local',
    cookie: { httpOnly: true, secure, sameSite: 'lax' as const, path: '/' },
  };
}
