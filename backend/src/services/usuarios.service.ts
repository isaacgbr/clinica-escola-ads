import { gerarHashSenha } from '../utils/senha.js';
import { ApiError } from '../middlewares/error.middleware.js';
import { buscarUsuarioPorEmail, inserirUsuario } from '../repositories/usuarios.repository.js';

const camposPermitidos = ['nome', 'email', 'senha', 'confirmar_senha', 'nascimento', 'telefone'];
const tamanho = (value: string) => Array.from(value).length;

export function validarCadastro(body: unknown) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    throw new ApiError(400, 'DADOS_INVALIDOS', 'Envie um objeto JSON com os dados do cadastro.');
  }

  const dados = body as Record<string, unknown>;
  const erros: Record<string, string> = {};
  if (Object.keys(dados).some(campo => !camposPermitidos.includes(campo))) {
    throw new ApiError(400, 'DADOS_INVALIDOS', 'O cadastro contém campos não permitidos.');
  }
  for (const campo of camposPermitidos) {
    if (typeof dados[campo] !== 'string' || !(dados[campo] as string).length) {
      erros[campo] = 'Campo obrigatório; informe um texto.';
    }
  }

  const nome = typeof dados.nome === 'string' ? dados.nome.trim() : '';
  const email = typeof dados.email === 'string' ? dados.email.trim().toLowerCase() : '';
  const senha = typeof dados.senha === 'string' ? dados.senha : '';
  const nascimento = typeof dados.nascimento === 'string' ? dados.nascimento : '';
  const telefoneOriginal = typeof dados.telefone === 'string' ? dados.telefone : '';
  const telefone = telefoneOriginal.replace(/[\s().-]/g, '');

  if (!nome || tamanho(nome) > 150 || nome.includes('\u0000')) {
    erros.nome = 'Informe um nome com até 150 caracteres, sem o caractere NUL.';
  }
  const emailValido = /^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$/;
  if (tamanho(email) > 150 || !emailValido.test(email)) erros.email = 'Informe um email válido com até 150 caracteres.';
  if (tamanho(senha) < 8 || tamanho(senha) > 128 || Buffer.byteLength(senha) > 512) {
    erros.senha = 'A senha deve ter entre 8 e 128 caracteres.';
  }
  if (typeof dados.confirmar_senha !== 'string' || !dados.confirmar_senha || dados.confirmar_senha !== senha) {
    erros.confirmar_senha = 'A confirmação deve ser igual à senha.';
  }

  // DATE e tratado em UTC para preservar o dia; o limite de futuro segue o calendario de Sao Paulo.
  const data = new Date(`${nascimento}T00:00:00.000Z`);
  const partesHoje = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date());
  const hoje = Object.fromEntries(partesHoje.map(parte => [parte.type, parte.value]));
  const dataAtual = `${hoje.year}-${hoje.month}-${hoje.day}`;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(nascimento) || nascimento < '0001-01-01' ||
      !Number.isFinite(data.getTime()) || data.toISOString().slice(0, 10) !== nascimento ||
      nascimento > dataAtual) {
    erros.nascimento = 'Informe uma data válida, não futura, no formato YYYY-MM-DD.';
  }
  if (!/^[0-9]{10,11}$/.test(telefone)) erros.telefone = 'Informe um telefone com DDD de 10 ou 11 dígitos.';

  if (Object.keys(erros).length) {
    throw new ApiError(400, 'DADOS_INVALIDOS', 'Confira os campos informados.', erros);
  }
  return { nome, email, senha, data, telefone };
}

export async function criarUsuario(body: unknown) {
  const dados = validarCadastro(body);
  if (await buscarUsuarioPorEmail(dados.email)) {
    throw new ApiError(409, 'EMAIL_JA_CADASTRADO', 'Email já cadastrado.');
  }
  const usuario = await inserirUsuario({
    nome_completo: dados.nome,
    email: dados.email,
    senha_hash: await gerarHashSenha(dados.senha),
    data_nascimento: dados.data,
    telefone: dados.telefone,
  });
  return { id: usuario.id.toString(), nome: usuario.nome_completo };
}
