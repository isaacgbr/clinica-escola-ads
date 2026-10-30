import { gerarHashSenha } from '../utils/senha.js';
import { ApiError } from '../middlewares/error.middleware.js';
import { buscarUsuarioPorEmail, inserirUsuario } from '../repositories/usuarios.repository.js';

const camposPermitidos = ['nome', 'email', 'senha', 'confirmar_senha', 'nascimento', 'telefone'];
const contarCaracteres = (texto: string) => Array.from(texto).length;
const textoOuVazio = (valor: unknown) => typeof valor === 'string' ? valor : '';

// Mantém a regra original de email, incluindo caracteres como % e _.
const formatoEmail = /^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$/;

function obterDadosCadastro(entrada: unknown): Record<string, unknown> {
  if (!entrada || typeof entrada !== 'object' || Array.isArray(entrada)) {
    throw new ApiError(400, 'DADOS_INVALIDOS', 'Envie um objeto JSON com os dados do cadastro.');
  }

  const dados = entrada as Record<string, unknown>;

  if (Object.keys(dados).some(campo => !camposPermitidos.includes(campo))) {
    throw new ApiError(400, 'DADOS_INVALIDOS', 'O cadastro contém campos não permitidos.');
  }

  return dados;
}

function normalizarDadosCadastro(dados: Record<string, unknown>) {
  return {
    nome: textoOuVazio(dados.nome).trim(),
    email: textoOuVazio(dados.email).trim().toLowerCase(),
    senha: textoOuVazio(dados.senha),
    nascimento: textoOuVazio(dados.nascimento),
    telefone: textoOuVazio(dados.telefone).replace(/[\s().-]/g, ''),
  };
}

function emailEhValido(email: string): boolean {
  return contarCaracteres(email) <= 150 && formatoEmail.test(email);
}

function senhaEhValida(senha: string): boolean {
  const caracteres = contarCaracteres(senha);
  const tamanhoValido = caracteres >= 8 && caracteres <= 128;
  const bytesValidos = Buffer.byteLength(senha) <= 512;

  return tamanhoValido && bytesValidos;
}

// Usa o calendário de São Paulo para impedir datas futuras.
function dataAtualEmSaoPaulo(): string {
  const partes = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date());

  const hoje = Object.fromEntries(partes.map(parte => [parte.type, parte.value]));
  return `${hoje.year}-${hoje.month}-${hoje.day}`;
}

function nascimentoEhValido(nascimento: string, data: Date): boolean {
  const formatoValido = /^\d{4}-\d{2}-\d{2}$/.test(nascimento);
  const dataExiste = Number.isFinite(data.getTime());

  if (!formatoValido || nascimento < '0001-01-01' || !dataExiste) {
    return false;
  }

  // A comparação ISO rejeita datas inexistentes ajustadas automaticamente pelo Date.
  const dataCorreta = data.toISOString().slice(0, 10) === nascimento;
  const naoEhFutura = nascimento <= dataAtualEmSaoPaulo();

  return dataCorreta && naoEhFutura;
}

export function validarCadastro(entrada: unknown) {
  const dadosRecebidos = obterDadosCadastro(entrada);
  const { nome, email, senha, nascimento, telefone } = normalizarDadosCadastro(dadosRecebidos);
  const erros: Record<string, string> = {};

  for (const campo of camposPermitidos) {
    const valor = dadosRecebidos[campo];

    if (typeof valor !== 'string' || valor.length === 0) {
      erros[campo] = 'Campo obrigatório; informe um texto.';
    }
  }

  if (!nome || contarCaracteres(nome) > 150 || nome.includes('\u0000')) {
    erros.nome = 'Informe um nome com até 150 caracteres, sem o caractere NUL.';
  }

  if (!emailEhValido(email)) {
    erros.email = 'Informe um email válido com até 150 caracteres.';
  }

  if (!senhaEhValida(senha)) {
    erros.senha = 'A senha deve ter entre 8 e 128 caracteres.';
  }

  const confirmacao = dadosRecebidos.confirmar_senha;
  if (typeof confirmacao !== 'string' || !confirmacao || confirmacao !== senha) {
    erros.confirmar_senha = 'A confirmação deve ser igual à senha.';
  }

  // DATE em UTC preserva o dia; a regra de data futura considera São Paulo.
  const data = new Date(`${nascimento}T00:00:00.000Z`);

  if (!nascimentoEhValido(nascimento, data)) {
    erros.nascimento = 'Informe uma data válida, não futura, no formato YYYY-MM-DD.';
  }

  if (!/^[0-9]{10,11}$/.test(telefone)) {
    erros.telefone = 'Informe um telefone com DDD de 10 ou 11 dígitos.';
  }

  if (Object.keys(erros).length > 0) {
    throw new ApiError(400, 'DADOS_INVALIDOS', 'Confira os campos informados.', erros);
  }

  return { nome, email, senha, data, telefone };
}

export async function criarUsuario(body: unknown) {
  const dados = validarCadastro(body);

  if (await buscarUsuarioPorEmail(dados.email)) {
    throw new ApiError(409, 'EMAIL_JA_CADASTRADO', 'Email já cadastrado.');
  }

  const senhaHash = await gerarHashSenha(dados.senha);

  const usuario = await inserirUsuario({
    nome_completo: dados.nome,
    email: dados.email,
    senha_hash: senhaHash,
    data_nascimento: dados.data,
    telefone: dados.telefone,
  });

  return { id: usuario.id.toString(), nome: usuario.nome_completo };
}