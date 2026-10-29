import type { ErrorRequestHandler } from 'express';

export class ApiError extends Error {
  constructor(
    public readonly status: 400 | 409,
    public readonly codigo: string,
    message: string,
    public readonly campos?: Record<string, string>,
  ) {
    super(message);
  }
}

export const errorMiddleware: ErrorRequestHandler = (error: unknown, _req, res, next) => {
  if (res.headersSent) {
    next(error);
    return;
  }

  if (error instanceof ApiError) {
    res.status(error.status).json({
      erro: { codigo: error.codigo, mensagem: error.message, ...(error.campos && { campos: error.campos }) },
    });
    return;
  }

  const type = typeof error === 'object' && error !== null && 'type' in error ? error.type : undefined;
  if (type === 'entity.parse.failed' || type === 'entity.too.large' || type === 'charset.unsupported' || type === 'encoding.unsupported') {
    res.status(400).json({ erro: { codigo: 'DADOS_INVALIDOS', mensagem: 'Corpo JSON inválido ou fora dos limites permitidos.' } });
    return;
  }

  res.status(500).json({ erro: { codigo: 'ERRO_INTERNO', mensagem: 'Não foi possível concluir a operação.' } });
};
