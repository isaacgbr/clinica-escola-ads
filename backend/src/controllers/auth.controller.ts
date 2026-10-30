import type { Request, Response } from 'express';
import { configAuth, DURACAO_SESSAO_MS } from '../config/auth.js';
import { login, logout } from '../services/auth.service.js';
import { lerTokenSessao } from '../utils/sessao-cookie.js';
import { limitesLogin } from '../middlewares/login-limit.middleware.js';

export async function entrar(req: Request, res: Response): Promise<void> {
  const resultado = await login(req.body, lerTokenSessao(req.headers.cookie));
  await limitesLogin.limparEmailIp(req);
  const config = configAuth();
  res.cookie(config.nomeCookie, resultado.token, { ...config.cookie, maxAge: DURACAO_SESSAO_MS });
  res.status(200).json({ usuario: resultado.usuario });
}

export function me(_req: Request, res: Response): void {
  res.status(200).json({ usuario: res.locals.usuario });
}

export async function sair(req: Request, res: Response): Promise<void> {
  const token = lerTokenSessao(req.headers.cookie);
  if (token) await logout(token);
  const config = configAuth();
  res.clearCookie(config.nomeCookie, config.cookie);
  res.sendStatus(204);
}
