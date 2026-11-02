package br.edu.uninassau.clinicaescola.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

// Padroniza os erros sem incluir detalhes internos da aplicação.
public record ErroResposta(Detalhe erro) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Detalhe(String codigo, String mensagem, Map<String, String> campos) { }
    public static ErroResposta de(String codigo, String mensagem, Map<String, String> campos) {
        return new ErroResposta(new Detalhe(codigo, mensagem, campos));
    }
}
