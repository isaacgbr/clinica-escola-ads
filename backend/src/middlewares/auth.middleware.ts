import type { RequestHandler } from 'express';
import { configAuth } from '../config/auth.js';
import { autenticarSessao } from '../services/auth.service.js';
import { lerTokenSessao } from '../utils/sessao-cookie.js';
import { ApiError } from './error.middleware.js';

export const protegerCsrf: RequestHandler = (req, _res, next) => {
  if (req.get('Origin') !== configAuth().origem || req.get('X-Requested-With') !== 'XMLHttpRequest' ||
      req.get('Sec-Fetch-Site') === 'cross-site') {
    throw new ApiError(403, 'ORIGEM_NAO_PERMITIDA', 'Requisição não permitida.');
  }
  if (!req.is('application/json')) {
    throw new ApiError(400, 'DADOS_INVALIDOS', 'Envie os dados como application/json.');
  }
  next();
};

export const autenticar: RequestHandler = async (req, res, next) => {
  const token = lerTokenSessao(req.headers.cookie);
  try {
    res.locals.usuario = await autenticarSessao(token);
    next();
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      const config = configAuth();
      res.clearCookie(config.nomeCookie, config.cookie);
    }
    next(error);
  }
};
