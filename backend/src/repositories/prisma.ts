import { PrismaPg } from '@prisma/adapter-pg';
import { PrismaClient } from '../generated/prisma/client.js';

let prisma: PrismaClient | undefined;

// Inicializacao adiada: o health check nao depende de credenciais do banco.
export function getPrisma(): PrismaClient {
  if (prisma) return prisma;

  const connectionString = process.env.DATABASE_URL;

  if (!connectionString) {
    throw new Error('DATABASE_URL deve ser configurada para acessar o PostgreSQL existente.');
  }

  prisma = new PrismaClient({ adapter: new PrismaPg({ connectionString }) });
  return prisma;
}
