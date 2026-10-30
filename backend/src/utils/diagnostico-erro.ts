export function diagnosticoErro(error: unknown) {
  const codigo = typeof error === 'object' && error !== null && 'code' in error ? error.code : undefined;
  const codigoSeguro = typeof codigo === 'string' && /^(P\d{4}|ERR_ASSERTION|ECONNREFUSED|ETIMEDOUT|ECONNRESET)$/.test(codigo)
    ? codigo : 'NAO_CLASSIFICADO';
  const tipo = error instanceof TypeError ? 'TIPO' : error instanceof RangeError ? 'LIMITE'
    : error instanceof SyntaxError ? 'SINTAXE' : error instanceof Error ? 'ERRO' : 'VALOR_INESPERADO';
  // Somente metadados permitidos: nunca mensagem, stack completo, causa ou objeto original.
  return { tipo, codigo: codigoSeguro };
}
