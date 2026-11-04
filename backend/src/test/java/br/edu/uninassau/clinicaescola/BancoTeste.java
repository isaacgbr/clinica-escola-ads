package br.edu.uninassau.clinicaescola;

import org.springframework.test.context.DynamicPropertyRegistry;
import java.net.URI;

// Bloqueia conexões quando o ambiente de teste não foi autorizado.
public final class BancoTeste {
    private BancoTeste() { }
    // Confere autorização e destino antes de permitir acesso ao banco.
    public static void configurar(DynamicPropertyRegistry properties) {
        String url = System.getenv("CADASTRO_TEST_DATABASE_URL");
        try {
            if (!"1".equals(System.getenv("ALLOW_DB_TESTS")) || url == null || !url.startsWith("jdbc:postgresql://"))
                throw new IllegalArgumentException();
            URI uri = URI.create(url.substring(5));
            if (uri.getHost() == null || !java.util.List.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost())
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || uri.getPath() == null || !uri.getPath().matches("/[a-z0-9_]+_(test|teste)"))
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException erro) {
            throw new IllegalStateException("Integração exige autorização e JDBC PostgreSQL local com banco terminado em _test ou _teste, sem parâmetros ou credenciais na URL.");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        properties.add("spring.sql.init.mode", () -> "never");
    }
}
