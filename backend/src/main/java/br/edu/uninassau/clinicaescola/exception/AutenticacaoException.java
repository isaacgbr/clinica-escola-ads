package br.edu.uninassau.clinicaescola.exception;
// Representa uma falha esperada de credenciais ou de sessão.
public final class AutenticacaoException extends RuntimeException  {
    private final String codigo;
    public AutenticacaoException(String codigo, String mensagem)  {
        super(mensagem);
        this.codigo = codigo;
    }
    public String codigo()  {
        return codigo;
    }
}
