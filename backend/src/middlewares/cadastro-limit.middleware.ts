import { rateLimit } from 'express-rate-limit';
import { ApiError } from './error.middleware.js';

export function criarLimiteCadastro() {
  return rateLimit({
    windowMs: 15 * 60 * 1000,
    limit: 5,
    standardHeaders: 'draft-8',
    legacyHeaders: false,
    identifier: 'cadastro-ip',
    // Conta sucessos e falhas; cada instancia possui armazenamento separado do Login.
    handler: (_req, _res, next) => next(new ApiError(
      429, 'MUITAS_TENTATIVAS', 'Muitas tentativas. Aguarde antes de tentar novamente.',
    )),
  });
}

export const limiteCadastro = criarLimiteCadastro();
