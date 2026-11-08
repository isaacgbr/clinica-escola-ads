package br.edu.uninassau.clinicaescola.controller;
import br.edu.uninassau.clinicaescola.config.*;
import br.edu.uninassau.clinicaescola.dto.*;
import br.edu.uninassau.clinicaescola.service.AuthService;
import br.edu.uninassau.clinicaescola.exception.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;
import java.time.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(AuthController.class)
@Import( {
    SegurancaConfig.class,CadastroConfig.class,TratamentoErros.class
}
)
// Confere cookies, origem e limites sem consultar o PostgreSQL.
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthService service;
    @MockitoBean Clock relogio;
    private Instant agora;
    private static long tempo=1793448000L;
    @BeforeEach void preparar() {
        agora=Instant.ofEpochSecond(tempo+=901);
        when(relogio.instant()).thenAnswer(c->agora);
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login() {
        return post("/auth/login").header("Origin","http://localhost:8080").header("X-Requested-With","XMLHttpRequest").contentType("application/json").content("{\"email\":\"a@example.com\",\"senha\":\"Teste-local-123!\"}");
    }
    @Test void loginEmiteCookieHttpOnlySemTokenNoJson()throws Exception {
        when(service.login(any(),isNull())).thenReturn(new AuthService.LoginConcluido("a".repeat(64),new UsuarioPublico("1","Maria")));
        mvc.perform(login()).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
        .andExpect(cookie().httpOnly("clinica_sessao_local",true)).andExpect(cookie().path("clinica_sessao_local","/"))
        .andExpect(content().json("{\"usuario\":{\"id\":\"1\",\"nome\":\"Maria\"}}"));
    }
    @Test void consultaSessaoELogout()throws Exception {
        when(service.autenticar(any())).thenReturn(new UsuarioAutenticado(new UsuarioPublico("1","Maria"),br.edu.uninassau.clinicaescola.entity.Perfil.INTERNO));
        var cookie=new Cookie("clinica_sessao_local","a".repeat(64));
        mvc.perform(get("/auth/me").cookie(cookie)).andExpect(status().isOk());
        mvc.perform(post("/auth/logout").cookie(cookie).header("Origin","http://localhost:8080").header("X-Requested-With","XMLHttpRequest").contentType("application/json").content("{}"))
        .andExpect(status().isNoContent()).andExpect(cookie().maxAge("clinica_sessao_local",0));
        verify(service).logout("a".repeat(64));
    }
    @Test void bloqueiaOrigemAusenteETentativasExcessivas()throws Exception {
        mvc.perform(post("/auth/login").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        when(service.login(any(),any())).thenThrow(new AutenticacaoException("CREDENCIAIS_INVALIDAS","Email ou senha inválidos."));
        for(int i=0;i<5;i++)mvc.perform(login()).andExpect(status().isUnauthorized());
        mvc.perform(login()).andExpect(status().isTooManyRequests());
        agora=agora.plusSeconds(900);
        mvc.perform(login()).andExpect(status().isUnauthorized());
    }
    @Test void sessaoRevogadaRetorna401ELimpaCookie()throws Exception {
        when(service.autenticar(any())).thenThrow(new AutenticacaoException("NAO_AUTENTICADO","Autenticação necessária."));
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized()).andExpect(cookie().maxAge("clinica_sessao_local",0));
    }
    @Test void healthContinuaPublico()throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(content().json("{\"status\":\"ok\"}"));
    }
    @Test void limiteIpNaoDependeDoEmailECsrfRejeitaOrigemFalsa() throws Exception {
        mvc.perform(post("/auth/login").header("Origin","https://origem-invalida.example").header("X-Requested-With","XMLHttpRequest").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        when(service.login(any(),any())).thenThrow(new AutenticacaoException("CREDENCIAIS_INVALIDAS","Email ou senha inválidos."));
        for(int i=0;i<20;i++)mvc.perform(login().content("{\"email\":\"teste"+i+"@example.com\",\"senha\":\"Teste-local-123!\"}")).andExpect(status().isUnauthorized());
        mvc.perform(login().content("{\"email\":\"novo@example.com\",\"senha\":\"Teste-local-123!\"}")).andExpect(status().isTooManyRequests());
    }
    @Test void sucessoLimpaParENaoConsomeCota() throws Exception {
        when(service.login(any(),any())).thenThrow(new AutenticacaoException("CREDENCIAIS_INVALIDAS","Email ou senha inválidos."));
        for(int i=0;i<4;i++)mvc.perform(login()).andExpect(status().isUnauthorized());
        doReturn(new AuthService.LoginConcluido("a".repeat(64),new UsuarioPublico("1","Maria"))).when(service).login(any(),any());
        mvc.perform(login()).andExpect(status().isOk());
        when(service.login(any(),any())).thenThrow(new AutenticacaoException("CREDENCIAIS_INVALIDAS","Email ou senha inválidos."));
        for(int i=0;i<5;i++)mvc.perform(login()).andExpect(status().isUnauthorized());
        mvc.perform(login()).andExpect(status().isTooManyRequests());
    }
    @Test void configuracaoProducaoExigeHttpsECookieSeguro() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->new SessaoCookie("http://localhost:8080","production"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->new SessaoCookie("https:invalida","production"));
        var response=new org.springframework.mock.web.MockHttpServletResponse();
        new SessaoCookie("https://clinica.example","production").escrever(response,"a".repeat(64));
        org.junit.jupiter.api.Assertions.assertTrue(response.getHeader("Set-Cookie").contains("__Host-clinica_sessao="));
        org.junit.jupiter.api.Assertions.assertTrue(response.getHeader("Set-Cookie").contains("Secure"));
    }
}
