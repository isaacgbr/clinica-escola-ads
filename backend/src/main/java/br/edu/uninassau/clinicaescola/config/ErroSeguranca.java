package br.edu.uninassau.clinicaescola.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Mantém as respostas dos filtros no mesmo formato de erro da API.
final class ErroSeguranca {
    private ErroSeguranca() { }
    static void enviar(HttpServletResponse response, int status, String codigo, String mensagem) throws IOException {
        // Argumentos são constantes internas; nunca interpolar conteúdo de requisições aqui.
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"erro\":{\"codigo\":\"" + codigo
                + "\",\"mensagem\":\"" + mensagem + "\"}}");
    }
}
