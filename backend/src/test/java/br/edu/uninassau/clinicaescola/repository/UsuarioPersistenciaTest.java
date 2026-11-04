package br.edu.uninassau.clinicaescola.repository;

import br.edu.uninassau.clinicaescola.BancoTeste;
import br.edu.uninassau.clinicaescola.dto.UsuarioPublico;
import br.edu.uninassau.clinicaescola.exception.EmailJaCadastradoException;
import br.edu.uninassau.clinicaescola.service.UsuarioService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ALLOW_DB_TESTS", matches = "1")
// Confere a gravação real do cadastro no banco de testes autorizado.
class UsuarioPersistenciaTest {
    @DynamicPropertySource static void banco(DynamicPropertyRegistry properties) { BancoTeste.configurar(properties); }
    @Autowired UsuarioService service;
    @Autowired UsuarioRepository repository;
    @Autowired PasswordEncoder senhas;
    @Autowired JdbcTemplate jdbc;
    private final List<Long> criados = new CopyOnWriteArrayList<>();
    private Map<String, Object> dados(String email) {
        return Map.of("nome", "Teste Cadastro", "email", email, "senha", "Teste-local-123!", "confirmar_senha", "Teste-local-123!",
                "nascimento", "2000-02-29", "telefone", "(81) 99999-9999");
    }
    @AfterEach void limparSomenteRegistrosCriadosPeloTeste() { repository.deleteAllById(criados); }
    @Test void persisteNormalizacaoDefaultsHashEIndiceLiteral() {
        String email = "teste-" + UUID.randomUUID() + "_%@example.com";
        var publico = service.cadastrar(dados(email.toUpperCase(Locale.ROOT)));
        long id = Long.parseLong(publico.id()); criados.add(id);
        var usuario = repository.findById(id).orElseThrow();
        assertEquals(email, usuario.email());
        assertEquals("81999999999", usuario.telefone());
        assertTrue(senhas.matches("Teste-local-123!", usuario.senhaHash()));
        assertFalse(usuario.senhaHash().contains("Teste-local-123!"));
        assertTrue(repository.existePorEmail(email.toUpperCase(Locale.ROOT)));
        assertFalse(repository.existePorEmail(email.replace("_%", "XX")));
        var datas = jdbc.queryForMap("SELECT criado_em, atualizado_em, data_nascimento FROM public.usuarios WHERE id = ?", id);
        assertNotNull(datas.get("criado_em")); assertNotNull(datas.get("atualizado_em"));
        assertEquals(java.time.LocalDate.of(2000, 2, 29), ((java.sql.Date) datas.get("data_nascimento")).toLocalDate());
        assertThrows(EmailJaCadastradoException.class, () -> service.cadastrar(dados(email)));
    }
    @Test void concorrenciaProduzUmUsuarioEUmConflito() throws Exception {
        String email = "teste-race-" + UUID.randomUUID() + "@example.com";
        var inicio = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> cadastro = () -> {
                inicio.await(10, TimeUnit.SECONDS);
                try {
                    UsuarioPublico usuario = service.cadastrar(dados(email));
                    criados.add(Long.parseLong(usuario.id())); return true;
                } catch (EmailJaCadastradoException esperado) { return false; }
            };
            var a = executor.submit(cadastro); var b = executor.submit(cadastro);
            assertNotEquals(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
            assertEquals(1, criados.size());
        }
    }
}
