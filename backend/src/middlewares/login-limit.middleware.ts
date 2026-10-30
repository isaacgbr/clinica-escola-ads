import { createHash } from 'node:crypto';
import type { Request } from 'express';
import { ipKeyGenerator, rateLimit } from 'express-rate-limit';
import { ApiError } from './error.middleware.js';

function chaveEmailIp(req: Request): string {
  const email = typeof req.body?.email === 'string' ? req.body.email.trim().toLowerCase().slice(0, 150) : '';
  return createHash('sha256').update(`${ipKeyGenerator(req.ip ?? '')}\n${email}`).digest('hex');
}

export function criarLimitesLogin() {
  const comum = {
    windowMs: 15 * 60 * 1000,
    standardHeaders: 'draft-8' as const,
    legacyHeaders: false,
    // Somente 401 e 429 consomem cota; sucessos e falhas de infraestrutura sao descontados.
    skipSuccessfulRequests: true,
    requestWasSuccessful: (_req: Request, res: { statusCode: number }) => ![401, 429].includes(res.statusCode),
    handler: (_req: Request, _res: unknown, next: (error?: unknown) => void) =>
      next(new ApiError(429, 'MUITAS_TENTATIVAS', 'Muitas tentativas. Aguarde antes de tentar novamente.')),
  };
  const porIp = rateLimit({ ...comum, limit: 20, identifier: 'login-ip' });
  const porEmailIp = rateLimit({ ...comum, limit: 5, identifier: 'login-email-ip', keyGenerator: chaveEmailIp });
  // Apos sucesso, limpa o par email/IP sem apagar as falhas gerais daquele IP.
  return { porIp, porEmailIp, limparEmailIp: (req: Request) => porEmailIp.resetKey(chaveEmailIp(req)) };
}

export const limitesLogin = criarLimitesLogin();
