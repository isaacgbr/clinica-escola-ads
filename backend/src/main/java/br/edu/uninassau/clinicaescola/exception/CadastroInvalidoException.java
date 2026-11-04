package br.edu.uninassau.clinicaescola.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Preserva as mensagens e os campos inválidos para a resposta de cadastro.
public final class CadastroInvalidoException extends RuntimeException {
    private final Map<String, String> campos;

    public CadastroInvalidoException(String mensagem) {
        this(mensagem, null);
    }

    public CadastroInvalidoException(String mensagem, Map<String, String> campos) {
        super(mensagem);
        this.campos = campos == null ? null
                : Collections.unmodifiableMap(new LinkedHashMap<>(campos));
    }

    public String codigo() {
        return "DADOS_INVALIDOS";
    }

    public Map<String, String> campos() {
        return campos;
    }
}
