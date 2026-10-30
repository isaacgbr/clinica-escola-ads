import { configAuth } from '../config/auth.js';

export function lerTokenSessao(cookieHeader: string | undefined): string | null {
  const nome = configAuth().nomeCookie;
  const cookies = (cookieHeader ?? '').split(';').map(value => value.trim());
  const valores = cookies.filter(cookie => cookie.slice(0, cookie.indexOf('=')) === nome)
    .map(cookie => cookie.slice(cookie.indexOf('=') + 1));
  return valores.length === 1 && /^[a-f0-9]{64}$/.test(valores[0]) ? valores[0] : null;
}
