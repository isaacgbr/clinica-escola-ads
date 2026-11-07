package br.edu.uninassau.clinicaescola.cli;

import br.edu.uninassau.clinicaescola.ClinicaEscolaApplication;
import br.edu.uninassau.clinicaescola.config.SessaoCookie;
import br.edu.uninassau.clinicaescola.service.UsuarioService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import java.io.Console;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

// Executado somente por comando explícito, sem abrir servidor HTTP.
public final class ProvisionarPrimeiroAdmin {
    private ProvisionarPrimeiroAdmin() { }

    // Compartilha com o teste a configuração usada pelo comando, sem iniciar o contexto aqui.
    static SpringApplicationBuilder prepararContexto() {
        return new SpringApplicationBuilder(ClinicaEscolaApplication.class, SessaoCookie.class)
                .web(WebApplicationType.NONE).logStartupInfo(false);
    }

    public static void executar(String[] args) {
        Console console = System.console();
        if (args.length != 1 || console == null) {
            throw new IllegalArgumentException("Use apenas --provisionar-primeiro-admin em um terminal interativo.");
        }
        if (!"PROVISIONAR ADMIN".equals(console.readLine("Confirme digitando PROVISIONAR ADMIN: "))) {
            throw new IllegalStateException("Provisionamento cancelado.");
        }
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("nome", console.readLine("Nome: "));
        dados.put("email", console.readLine("Email: "));
        dados.put("nascimento", console.readLine("Nascimento (YYYY-MM-DD): "));
        dados.put("telefone", console.readLine("Telefone com DDD: "));
        char[] senha = console.readPassword("Senha: ");
        char[] confirmacao = console.readPassword("Confirme a senha: ");
        try {
            if (senha == null || confirmacao == null) throw new IllegalArgumentException();
            dados.put("senha", new String(senha));
            dados.put("confirmar_senha", new String(confirmacao));
            // Não recebe argumentos de conexão nem executa scripts ou alterações de schema.
            try (var contexto = prepararContexto().run()) {
                contexto.getBean(UsuarioService.class).provisionarPrimeiroAdmin(dados);
            }
            console.printf("Primeiro administrador criado. Nenhuma credencial foi exibida.%n");
        } catch (Exception erro) {
            // Não propaga mensagens do driver ou dados recebidos para o terminal.
            throw new IllegalStateException("Provisionamento não concluído. Confira dados, conexão, SQL aprovado e existência de ADMIN. Nenhum usuário existente foi sobrescrito.");
        } finally {
            dados.clear();
            if (senha != null) Arrays.fill(senha, '\0');
            if (confirmacao != null) Arrays.fill(confirmacao, '\0');
        }
    }
}
