package br.edu.uninassau.clinicaescola.dto;

import java.time.LocalDate;

// Transporta os dados validados; a senha continua restrita ao uso interno.
public record CadastroValidado(String nome, String email, String senha,
                               LocalDate nascimento, String telefone) {
    @Override
    public String toString() {
        return "CadastroValidado[conteúdo protegido]";
    }
}
