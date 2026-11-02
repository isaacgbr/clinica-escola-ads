package br.edu.uninassau.clinicaescola.exception;

// Distingue o conflito de email das demais falhas ao salvar.
public final class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException() { super("Email já cadastrado."); }
}
