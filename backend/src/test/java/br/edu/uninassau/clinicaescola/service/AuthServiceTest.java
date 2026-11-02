package br.edu.uninassau.clinicaescola.service;
import br.edu.uninassau.clinicaescola.entity.Usuario;
import br.edu.uninassau.clinicaescola.repository.*;
import br.edu.uninassau.clinicaescola.exception.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
// Simula usuários e sessões para verificar as regras do serviço.
class AuthServiceTest {
    UsuarioRepository usuarios=mock(UsuarioRepository.class);
    SessaoRepository sessoes=mock(SessaoRepository.class);
    PasswordEncoder senhas=mock(PasswordEncoder.class);
    Clock clock=Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"),ZoneOffset.UTC);
    AuthService service=new AuthService(usuarios,sessoes,senhas,clock);
    private Map<String,String> entrada(){return Map.of("email"," MARIA@EXAMPLE.COM ","senha","Teste-local-123!");}
    @Test void emailInexistenteTambemVerificaSenha(){
        when(usuarios.buscarPorEmail("maria@example.com")).thenReturn(Optional.empty());
        var erro=assertThrows(AutenticacaoException.class,()->service.login(entrada(),null));
        assertEquals("CREDENCIAIS_INVALIDAS",erro.codigo());verify(senhas).matches("Teste-local-123!",null);verifyNoInteractions(sessoes);
    }
    @Test void senhaErradaNaoCriaSessao(){
        Usuario usuario=mock(Usuario.class);when(usuario.senhaHash()).thenReturn("hash-simulado");when(usuarios.buscarPorEmail(any())).thenReturn(Optional.of(usuario));
        assertThrows(AutenticacaoException.class,()->service.login(entrada(),null));verifyNoInteractions(sessoes);
    }
    @Test void loginValidoRotacionaSomenteSessaoApresentada(){
        Usuario usuario=mock(Usuario.class);when(usuario.getId()).thenReturn(7L);when(usuario.getNomeCompleto()).thenReturn("Maria");
        when(usuarios.buscarPorEmail(any())).thenReturn(Optional.of(usuario));when(senhas.matches(any(),any())).thenReturn(true);
        String anterior="a".repeat(64);var resultado=service.login(entrada(),anterior);
        assertEquals("7",resultado.usuario().id());assertTrue(resultado.token().matches("[a-f0-9]{64}"));assertFalse(resultado.token().equals(anterior));
        verify(sessoes).revogar(AuthService.hash(anterior),clock.instant());verify(sessoes).saveAndFlush(any());
    }
    @Test void sessaoExpiradaOuRevogadaNaoAutentica(){
        assertThrows(AutenticacaoException.class,()->service.autenticar(null));
        assertThrows(AutenticacaoException.class,()->service.autenticar("a".repeat(64)));
        verify(sessoes).renovar(any(),eq(clock.instant()),eq(clock.instant().minusSeconds(1800)));
    }
    @Test void validacaoNaoAceitaCamposExtrasNemAlteraSenha(){
        assertThrows(CadastroInvalidoException.class,()->AuthService.validar(Map.of("email","a@example.com","senha","Teste-local-123!","perfil","admin")));
        assertEquals("  senha  ",AuthService.validar(Map.of("email"," a@example.com ","senha","  senha  ")).senha());
    }

    @Test void preservaEspacosNodeNoEmail(){
        assertEquals("a@example.com",AuthService.normalizarEmail("\ufeff\u00a0a@example.com\u00a0"));
        assertThrows(CadastroInvalidoException.class,()->AuthService.validar(Map.of("email","a\u00a0b@example.com","senha","Teste-local-123!")));
    }
}
