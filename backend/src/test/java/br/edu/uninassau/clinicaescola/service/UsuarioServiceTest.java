package br.edu.uninassau.clinicaescola.service;

import br.edu.uninassau.clinicaescola.entity.Usuario;
import br.edu.uninassau.clinicaescola.exception.*;
import br.edu.uninassau.clinicaescola.repository.UsuarioRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.SQLException;
import java.time.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Testa o cadastro e os conflitos sem usar o banco real.
class UsuarioServiceTest {
    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final PasswordEncoder senhas = mock(PasswordEncoder.class);
    private final UsuarioService service = new UsuarioService(repository,
            new ValidadorCadastro(Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneOffset.UTC)), senhas);
    static Map<String, Object> valido() {
        return Map.of("nome", " Maria Silva ", "email", " MARIA@EXAMPLE.COM ", "senha", "Teste-local-123!",
                "confirmar_senha", "Teste-local-123!", "nascimento", "2000-02-29", "telefone", "(81) 99999-9999");
    }
    @Test void normalizaGravaHashERetornaSomenteDadosPublicos() {
        when(senhas.encode("Teste-local-123!")).thenReturn("hash-seguro");
        when(repository.saveAndFlush(any())).thenAnswer(call -> {
            Usuario usuario = call.getArgument(0);
            assertEquals("maria@example.com", usuario.email());
            assertEquals("hash-seguro", usuario.senhaHash());
            assertEquals(br.edu.uninassau.clinicaescola.entity.Perfil.INTERNO, usuario.getPerfil());
            assertEquals(LocalDate.of(2000, 2, 29), usuario.dataNascimento());
            assertEquals("81999999999", usuario.telefone());
            assertFalse(usuario.toString().contains("hash-seguro"));
            ReflectionTestUtils.setField(usuario, "id", 42L);
            return usuario;
        });
        var usuario = service.cadastrar(valido());
        assertEquals("42", usuario.id());
        assertEquals("Maria Silva", usuario.nome());
        verify(repository).existePorEmail("maria@example.com");
    }
    @Test void naoAcessaPersistenciaNemHashParaDadosInvalidos() {
        assertThrows(CadastroInvalidoException.class, () -> service.cadastrar(Map.of()));
        verifyNoInteractions(repository, senhas);
    }
    @Test void duplicidadeConhecidaNaoGeraHashNemInsere() {
        when(repository.existePorEmail("maria@example.com")).thenReturn(true);
        assertThrows(EmailJaCadastradoException.class, () -> service.cadastrar(valido()));
        verifyNoInteractions(senhas);
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void traduzConflitoConcorrenteSomenteDoIndiceDeEmail() {
        when(senhas.encode(any())).thenReturn("hash");
        var sql = new SQLException("email e hash sensiveis", "23505");
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("sensivel",
                new ConstraintViolationException("sensivel", sql, "uq_usuarios_email")));
        assertThrows(EmailJaCadastradoException.class, () -> service.cadastrar(valido()));
    }
    @Test void traduzConflitoQuandoHibernateOmiteNomeDaRestricao() {
        when(senhas.encode(any())).thenReturn("hash");
        var postgres = erroPostgres("23505", "uq_usuarios_email");
        var violacao = new ConstraintViolationException("Falha simulada", postgres, null);
        assertNull(violacao.getConstraintName());
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Falha simulada", violacao));
        assertThrows(EmailJaCadastradoException.class, () -> service.cadastrar(valido()));
    }

    @Test void metadadosPostgresNaoConvertemOutrasRestricoesOuEstados() {
        when(senhas.encode(any())).thenReturn("hash");
        for (var postgres : java.util.List.of(erroPostgres("23505", "outra_restricao"),
                erroPostgres("23514", "uq_usuarios_email"), erroPostgres("23505", null),
                new PSQLException("Falha simulada", org.postgresql.util.PSQLState.UNIQUE_VIOLATION))) {
            var falha = new DataIntegrityViolationException("Falha simulada",
                    new ConstraintViolationException("Falha simulada", postgres, null));
            doThrow(falha).when(repository).saveAndFlush(any());
            assertSame(falha, assertThrows(DataIntegrityViolationException.class, () -> service.cadastrar(valido())));
        }
    }

    private static PSQLException erroPostgres(String estado, String restricao) {
        // Reproduz os campos do protocolo PostgreSQL, sem conexão com o banco.
        String campos = "SERROR\0C" + estado + "\0MFalha simulada\0"
                + (restricao == null ? "" : "n" + restricao + "\0") + "\0";
        return new PSQLException(new ServerErrorMessage(campos));
    }

    @Test void outrasRestricoesNaoSaoConfundidasComEmailDuplicado() {
        when(senhas.encode(any())).thenReturn("hash");
        var falha = new DataIntegrityViolationException("sensivel", new ConstraintViolationException("sensivel",
                new SQLException("sensivel", "23514"), "ck_usuarios_telefone"));
        when(repository.saveAndFlush(any())).thenThrow(falha);
        assertSame(falha, assertThrows(DataIntegrityViolationException.class, () -> service.cadastrar(valido())));
    }
}
