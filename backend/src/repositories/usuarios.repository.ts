import { Prisma } from '../generated/prisma/client.js';
import { ApiError } from '../middlewares/error.middleware.js';
import { getPrisma } from './prisma.js';

export function buscarUsuarioPorEmail(email: string) {
  return getPrisma().usuarios.findFirst({
    where: { email: { equals: email, mode: 'insensitive' } },
    select: { id: true },
  });
}

export async function inserirUsuario(dados: {
  nome_completo: string;
  email: string;
  senha_hash: string;
  data_nascimento: Date;
  telefone: string;
}) {
  try {
    return await getPrisma().usuarios.create({ data: dados, select: { id: true, nome_completo: true } });
  } catch (error) {
    // O índice por LOWER(email) protege também contra cadastros concorrentes.
    if (error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2002') {
      throw new ApiError(409, 'EMAIL_JA_CADASTRADO', 'Email já cadastrado.');
    }
    throw error;
  }
}
