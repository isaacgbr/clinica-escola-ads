import { getPrisma } from './prisma.js';

export function criarSessao(dados: {
  usuario_id: bigint; token_hash: string; criado_em: Date; ultima_atividade_em: Date; expira_em: Date;
}, hashAnterior: string | null) {
  return getPrisma().$transaction(async prisma => {
    if (hashAnterior) {
      await prisma.sessoes.updateMany({
        where: { token_hash: hashAnterior, revogada_em: null }, data: { revogada_em: dados.criado_em },
      });
    }
    await prisma.sessoes.create({ data: dados, select: { id: true } });
  });
}

export function renovarAtividade(hash: string, agora: Date, limiteInatividade: Date) {
  return getPrisma().$transaction(async prisma => {
    // GREATEST preserva a atividade mais recente, inclusive sob bloqueio concorrente da linha.
    const alteradas = await prisma.$executeRaw`
      UPDATE public.sessoes
      SET ultima_atividade_em = GREATEST(ultima_atividade_em, ${agora}::timestamptz)
      WHERE token_hash = ${hash}
        AND revogada_em IS NULL
        AND expira_em > ${agora}::timestamptz
        AND ultima_atividade_em > ${limiteInatividade}::timestamptz
    `;
    if (alteradas !== 1) return null;
    return prisma.sessoes.findUnique({
      where: { token_hash: hash },
      select: { id: true, usuarios: { select: { id: true, nome_completo: true } } },
    });
  });
}

export async function revogarSessao(hash: string, agora: Date): Promise<void> {
  await getPrisma().sessoes.updateMany({
    where: { token_hash: hash, revogada_em: null }, data: { revogada_em: agora },
  });
}
