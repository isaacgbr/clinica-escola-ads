package br.edu.uninassau.clinicaescola.controller;

import br.edu.uninassau.clinicaescola.config.*;
import br.edu.uninassau.clinicaescola.entity.Usuario;
import br.edu.uninassau.clinicaescola.exception.TratamentoErros;
import br.edu.uninassau.clinicaescola.repository.UsuarioRepository;
import br.edu.uninassau.clinicaescola.service.UsuarioService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@Import({UsuarioService.class, SegurancaConfig.class, CadastroConfig.class, TratamentoErros.class})
// Testa o cadastro completo simulando apenas a senha e a gravação.
class CadastroFluxoHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean br.edu.uninassau.clinicaescola.service.AuthService authService;
    @MockitoBean UsuarioRepository repository;
    @MockitoBean PasswordEncoder senhas;
    @MockitoBean Clock relogio;
    private static final AtomicLong TEMPO = new AtomicLong(1793448000L);
    private static final String JSON = """
            {"nome":" Maria ","email":" MARIA@EXAMPLE.COM ","senha":"Teste-local-123!",
             "confirmar_senha":"Teste-local-123!","nascimento":"2000-02-29","telefone":"(81) 99999-9999"}
            """;
    @BeforeEach void preparar() {
        var clock = Clock.fixed(Instant.ofEpochSecond(TEMPO.addAndGet(901)), ZoneOffset.UTC);
        when(relogio.instant()).thenReturn(clock.instant());
        when(relogio.withZone(any())).thenAnswer(call -> clock.withZone(call.getArgument(0)));
        when(senhas.encode(any())).thenReturn("marcador-hash-nao-publicar");
        when(repository.saveAndFlush(any())).thenAnswer(call -> {
            Usuario usuario = call.getArgument(0); ReflectionTestUtils.setField(usuario, "id", 7L); return usuario;
        });
    }
    @Test void percorreControllerServiceERepositoryComDtoPublico() throws Exception {
        mvc.perform(post("/usuarios").contentType("application/json").content(JSON))
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"usuario\":{\"id\":\"7\",\"nome\":\"Maria\"}}"))
                .andExpect(jsonPath("$.usuario.senha").doesNotExist())
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());
        verify(repository).existePorEmail("maria@example.com");
        verify(senhas).encode("Teste-local-123!");
    }
    @Test void validacaoRealRejeitaTiposCamposExtrasNullEArray() throws Exception {
        for (String body : new String[]{"{\"nome\":123}", "{\"perfil\":\"admin\"}", "null", "[]", "{}"}) {
            mvc.perform(post("/usuarios").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro.codigo").value("DADOS_INVALIDOS"));
        }
        verifyNoInteractions(repository, senhas);
    }
    @Test void duplicidadeRealRetorna409AntesDoHash() throws Exception {
        when(repository.existePorEmail("maria@example.com")).thenReturn(true);
        mvc.perform(post("/usuarios").contentType("application/json").content(JSON))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.erro.codigo").value("EMAIL_JA_CADASTRADO"));
        verifyNoInteractions(senhas);
    }
}
