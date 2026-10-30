import { createHash, randomBytes } from 'node:crypto';
import { DURACAO_SESSAO_MS, INATIVIDADE_SESSAO_MS } from '../config/auth.js';
import { ApiError } from '../middlewares/error.middleware.js';
import { buscarCredenciaisPorEmail } from '../repositories/usuarios.repository.js';
import { criarSessao, renovarAtividade, revogarSessao } from '../repositories/sessoes.repository.js';
import { verificarSenha } from '../utils/senha.js';

export function validarLogin(body: unknown): { email: string; senha: string } {
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    throw new ApiError(400, 'DADOS_INVALIDOS', 'Informe email e senha em um objeto JSON.');
  }
  const dados = body as Record<string, unknown>;
  if (Object.keys(dados).some(campo => !['email', 'senha'].includes(campo)) ||
      typeof dados.email !== 'string' || typeof dados.senha !== 'string') {
    throw new ApiError(400, 'DADOS_INVALIDOS', 'Informe somente email e senha como textos.');
  }
  const email = dados.email.trim().toLowerCase();
  if (email.length > 150 || !/^[^\s@\u0000]+@[^\s@\u0000]+\.[^\s@\u0000]+$/.test(email) ||
      Array.from(dados.senha).length < 8 || Array.from(dados.senha).length > 128 || Buffer.byteLength(dados.senha) > 512) {
    throw new ApiError(400, 'DADOS_INVALIDOS', 'Informe email válido e senha de 8 a 128 caracteres.');
  }
  return { email, senha: dados.senha };
}

export const hashToken = (token: string) => createHash('sha256').update(token).digest('hex');

export async function login(body: unknown, tokenAnterior: string | null) {
  const dados = validarLogin(body);
  const usuario = await buscarCredenciaisPorEmail(dados.email);
  const senhaValida = await verificarSenha(dados.senha, usuario?.senha_hash ?? null);
  if (!usuario || !senhaValida) {
    throw new ApiError(401, 'CREDENCIAIS_INVALIDAS', 'Email ou senha inválidos.');
  }

  const token = randomBytes(32).toString('hex');
  const agora = new Date();
  // Substitui apenas a sessao apresentada no cookie; preserva as de outros dispositivos.
  await criarSessao({
    usuario_id: usuario.id, token_hash: hashToken(token), criado_em: agora, ultima_atividade_em: agora,
    expira_em: new Date(agora.getTime() + DURACAO_SESSAO_MS),
  }, tokenAnterior ? hashToken(tokenAnterior) : null);
  return { token, usuario: { id: usuario.id.toString(), nome: usuario.nome_completo } };
}

export async function autenticarSessao(token: string | null) {
  if (token) {
    const agora = new Date();
    const sessao = await renovarAtividade(hashToken(token), agora, new Date(agora.getTime() - INATIVIDADE_SESSAO_MS));
    if (sessao) return { id: sessao.usuarios.id.toString(), nome: sessao.usuarios.nome_completo };
  }
  throw new ApiError(401, 'NAO_AUTENTICADO', 'Autenticação necessária.');
}

export async function logout(token: string): Promise<void> {
  await revogarSessao(hashToken(token), new Date());
}
