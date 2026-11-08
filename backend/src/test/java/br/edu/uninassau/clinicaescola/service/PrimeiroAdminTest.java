package br.edu.uninassau.clinicaescola.service;

import br.edu.uninassau.clinicaescola.entity.Perfil;
import br.edu.uninassau.clinicaescola.entity.Usuario;
import br.edu.uninassau.clinicaescola.exception.*;
import br.edu.uninassau.clinicaescola.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

// Nenhum destes testes abre conexão ou provisiona contas reais.
class PrimeiroAdminTest {
    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final PasswordEncoder senhas = mock(PasswordEncoder.class);
    private final UsuarioService service = new UsuarioService(repository,
            new ValidadorCadastro(Clock.fixed(Instant.parse("2026-11-07T12:00:00Z"), ZoneOffset.UTC)), senhas);

    @Test void provisionaAdminComMesmaValidacaoEHashDoCadastro() {
        when(senhas.encode("Teste-local-123!")).thenReturn("hash-simulado");
        when(repository.saveAndFlush(any())).thenAnswer(call -> {
            Usuario usuario = call.getArgument(0);
            assertEquals(Perfil.ADMIN, usuario.getPerfil());
            assertEquals("maria@example.com", usuario.email());
            assertEquals("hash-simulado", usuario.senhaHash());
            ReflectionTestUtils.setField(usuario, "id", 1L);
            return usuario;
        });
        service.provisionarPrimeiroAdmin(UsuarioServiceTest.valido());
        verify(repository).existsByPerfil(Perfil.ADMIN);
        verify(senhas).encode("Teste-local-123!");
        verify(repository).saveAndFlush(any());
    }
    @Test void adminExistenteBloqueiaProvisionamento() {
        when(repository.existsByPerfil(Perfil.ADMIN)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.provisionarPrimeiroAdmin(UsuarioServiceTest.valido()));
        verifyNoInteractions(senhas);
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void emailExistenteNaoEPromovidoNemSobrescrito() {
        when(repository.existePorEmail("maria@example.com")).thenReturn(true);
        assertThrows(EmailJaCadastradoException.class, () -> service.provisionarPrimeiroAdmin(UsuarioServiceTest.valido()));
        verifyNoInteractions(senhas);
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void dadosInvalidosOuPerfilNoJsonNaoGeramHash() {
        var dados = new HashMap<>(UsuarioServiceTest.valido());
        dados.put("perfil", "ADMIN");
        assertThrows(CadastroInvalidoException.class, () -> service.provisionarPrimeiroAdmin(dados));
        assertThrows(CadastroInvalidoException.class, () -> service.cadastrar(dados));
        dados.remove("perfil");
        dados.put("confirmar_senha", "outra-senha");
        assertThrows(CadastroInvalidoException.class, () -> service.provisionarPrimeiroAdmin(dados));
        verifyNoInteractions(senhas);
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void comandoRecusaArgumentosExtrasAntesDeAbrirContexto() {
        assertThrows(IllegalArgumentException.class, () -> br.edu.uninassau.clinicaescola.ClinicaEscolaApplication.main(
                new String[]{"--provisionar-primeiro-admin", "--qualquer-opcao"}));
    }
}
