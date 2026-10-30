import { Prisma } from '../generated/prisma/client.js';
import { ApiError } from '../middlewares/error.middleware.js';
import { getPrisma } from './prisma.js';

export async function buscarCredenciaisPorEmail(email: string) {
  // Igualdade literal usa o indice LOWER(email), sem interpretar % e _ como curingas.
  const usuarios = await getPrisma().$queryRaw<Array<{ id: bigint; nome_completo: string; senha_hash: string }>>`
    SELECT id, nome_completo, senha_hash FROM public.usuarios WHERE LOWER(email) = ${email} LIMIT 1
  `;
  return usuarios[0] ?? null;
}

export async function buscarUsuarioPorEmail(email: string) {
  // Igualdade literal preserva % e _ e utiliza o indice unico em LOWER(email).
  const usuarios = await getPrisma().$queryRaw<Array<{ id: bigint }>>`
    SELECT id FROM public.usuarios WHERE LOWER(email) = LOWER(${email}) LIMIT 1
  `;
  return usuarios[0] ?? null;
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
