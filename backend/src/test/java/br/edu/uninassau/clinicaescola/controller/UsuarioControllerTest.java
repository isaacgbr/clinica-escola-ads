package br.edu.uninassau.clinicaescola.controller;

import br.edu.uninassau.clinicaescola.config.*;
import br.edu.uninassau.clinicaescola.dto.UsuarioPublico;
import br.edu.uninassau.clinicaescola.exception.*;
import br.edu.uninassau.clinicaescola.service.UsuarioService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.*;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({UsuarioController.class,AuthController.class})
@Import({SegurancaConfig.class, CadastroConfig.class, TratamentoErros.class})
@ExtendWith(OutputCaptureExtension.class)
// Confere respostas e limites do cadastro sem gravar no banco.
class UsuarioControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean br.edu.uninassau.clinicaescola.service.AuthService authService;
    @MockitoBean UsuarioService service;
    @MockitoBean Clock relogio;
    private static final AtomicLong INSTANTE = new AtomicLong(1793448000L);
    private Instant agora;
    private static final String JSON = "{\"nome\":\"Maria\",\"email\":\"maria@example.com\",\"senha\":\"marcador-senha\","
            + "\"confirmar_senha\":\"marcador-senha\",\"nascimento\":\"2000-01-01\",\"telefone\":\"81999999999\"}";
    @BeforeEach void preparar() {
        agora = Instant.ofEpochSecond(INSTANTE.addAndGet(901));
        when(relogio.instant()).thenAnswer(call -> agora);
        when(service.cadastrar(any())).thenReturn(new UsuarioPublico("42", "Maria"));
    }
    @Test void criaSemCsrfOuAutenticacaoComRespostaPublica(CapturedOutput logs) throws Exception {
        var result = mvc.perform(post("/usuarios").contentType("application/json").content(JSON))
                .andExpect(status().isCreated()).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json("{\"usuario\":{\"id\":\"42\",\"nome\":\"Maria\"}}"))
                .andExpect(jsonPath("$.usuario.*", hasSize(2)))
                .andExpect(header().doesNotExist("Set-Cookie")).andReturn();
        assertFalse(result.getResponse().getContentAsString().contains("marcador-senha"));
        assertFalse(logs.getAll().contains("marcador-senha"));
    }
    @Test void preservaTiposJsonSemCoercao() throws Exception {
        mvc.perform(post("/usuarios").contentType("application/json").content("{\"nome\":123}"))
                .andExpect(status().isCreated());
        verify(service).cadastrar(argThat(value -> value instanceof Map<?, ?> dados && dados.get("nome") instanceof Number));
    }
    @Test void validaCamposERetornaErroPadronizado() throws Exception {
        when(service.cadastrar(any())).thenThrow(new CadastroInvalidoException("Confira os campos informados.",
                Map.of("nome", "Informe um nome com até 150 caracteres, sem o caractere NUL.")));
        mvc.perform(post("/usuarios").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro.codigo").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("$.erro.campos.nome").isString());
    }
    @Test void duplicadoRetorna409() throws Exception {
        when(service.cadastrar(any())).thenThrow(new EmailJaCadastradoException());
        mvc.perform(post("/usuarios").contentType("application/json").content(JSON))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.erro.codigo").value("EMAIL_JA_CADASTRADO"));
    }
    @Test void falhaInternaNaoExpoeMensagemOuHash(CapturedOutput logs) throws Exception {
        when(service.cadastrar(any())).thenThrow(new RuntimeException("marcador-senha marcador-hash jdbc:secreto"));
        var result = mvc.perform(post("/usuarios").contentType("application/json").content(JSON))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.erro.codigo").value("ERRO_INTERNO"))
                .andReturn();
        String saida = logs.getAll() + result.getResponse().getContentAsString();
        assertFalse(saida.contains("marcador-senha"));
        assertFalse(saida.contains("marcador-hash"));
        assertFalse(saida.contains("jdbc:secreto"));
    }
    @Test void rejeitaJsonMalformadoTipoErradoECorpoExcessivo() throws Exception {
        mvc.perform(post("/usuarios").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro.codigo").value("DADOS_INVALIDOS"));
        mvc.perform(post("/usuarios").contentType("text/plain").content(JSON)).andExpect(status().isBadRequest());
        mvc.perform(post("/usuarios").contentType("application/json").content("x".repeat(16385)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void limitaCincoTentativasIncluindoFalhasELiberaAposJanela() throws Exception {
        when(service.cadastrar(any())).thenThrow(new CadastroInvalidoException("Confira os campos informados."));
        for (int i = 0; i < 5; i++) mvc.perform(post("/usuarios").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/usuarios").contentType("application/json").content("{}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.erro.codigo").value("MUITAS_TENTATIVAS"));
        agora = agora.plusSeconds(900);
        mvc.perform(post("/usuarios").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verify(service, times(6)).cadastrar(any());
    }
    @Test void contaSucessosIgnoraCabecalhosDeProxyESeparaIps() throws Exception {
        for (int i = 0; i < 5; i++) mvc.perform(post("/usuarios").contentType("application/json").content(JSON))
                .andExpect(status().isCreated());
        mvc.perform(post("/usuarios").header("X-Forwarded-For", "203.0.113.2")
                .contentType("application/json").content(JSON)).andExpect(status().isTooManyRequests());
        mvc.perform(post("/usuarios").with(req -> { req.setRemoteAddr("203.0.113.3"); return req; })
                .contentType("application/json").content(JSON)).andExpect(status().isCreated());
    }
    @Test void agrupaIpv6Por56Bits() throws Exception {
        for (int i = 0; i < 5; i++) mvc.perform(post("/usuarios").with(req -> {
            req.setRemoteAddr("2001:db8:abcd:1200::1"); return req;
        }).contentType("application/json").content(JSON)).andExpect(status().isCreated());
        mvc.perform(post("/usuarios").with(req -> { req.setRemoteAddr("2001:db8:abcd:12ff::2"); return req; })
                .contentType("application/json").content(JSON)).andExpect(status().isTooManyRequests());
    }
    @Test void demaisRotasNaoSaoLiberadas() throws Exception {
        mvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/health")).andExpect(status().isOk());
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/pacientes")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
