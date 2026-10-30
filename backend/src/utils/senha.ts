import { randomBytes, scrypt, timingSafeEqual } from 'node:crypto';

const parametros = { N: 32768, r: 8, p: 3, maxmem: 64 * 1024 * 1024 };

function derivar(senha: string, salt: string): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    scrypt(senha, salt, 64, parametros, (error, key) => {
      if (error) reject(error);
      else resolve(key);
    });
  });
}

export async function gerarHashSenha(senha: string): Promise<string> {
  const salt = randomBytes(16).toString('hex');
  const key = await derivar(senha, salt);
  return `scrypt$32768$8$3$${salt}$${key.toString('hex')}`;
}

export async function verificarSenha(senha: string, hash: string | null): Promise<boolean> {
  const partes = /^scrypt\$32768\$8\$3\$([a-f0-9]{32})\$([a-f0-9]{128})$/.exec(hash ?? '');
  // Usuarios inexistentes ou hashes invalidos tambem passam pela derivacao.
  const key = await derivar(senha, partes?.[1] ?? '0'.repeat(32));
  const expected = Buffer.from(partes?.[2] ?? '0'.repeat(128), 'hex');
  const igual = timingSafeEqual(key, expected);
  return partes !== null && igual;
}
