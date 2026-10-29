import type { Request, Response } from 'express';
import { criarUsuario } from '../services/usuarios.service.js';
import { ApiError } from '../middlewares/error.middleware.js';

export async function cadastrarUsuario(request: Request, response: Response): Promise<void> {
  if (!request.is('application/json')) {
    throw new ApiError(400, 'DADOS_INVALIDOS', 'Envie os dados como application/json.');
  }

  const usuario = await criarUsuario(request.body);
  response.status(201).json({ usuario });
}
