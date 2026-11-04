package br.edu.uninassau.clinicaescola.controller;
import br.edu.uninassau.clinicaescola.config.SessaoCookie;
import br.edu.uninassau.clinicaescola.dto.*;
import br.edu.uninassau.clinicaescola.service.AuthService;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
@RestController
// Separa as respostas HTTP das regras de login e sessão.
public class AuthController {
    private final AuthService service;
    private final SessaoCookie cookies;
    public AuthController(AuthService service,SessaoCookie cookies) {
        this.service=service;
        this.cookies=cookies;
    }
    // O identificador e enviado somente no cookie, nunca no JSON.
    @PostMapping(value= {
        "/auth/login","/auth/login/"
    }
    ,consumes="application/json")
    public UsuarioResposta login(@RequestBody(required=false) Object entrada,HttpServletRequest request,HttpServletResponse response) {
        var resultado=service.login(entrada,cookies.ler(request));
        cookies.escrever(response,resultado.token());
        return new UsuarioResposta(resultado.usuario());
    }
    @GetMapping("/auth/me") public UsuarioResposta me(HttpServletRequest request) {
        return new UsuarioResposta((UsuarioPublico)request.getAttribute("usuarioAutenticado"));
    }
    @PostMapping("/auth/logout") public ResponseEntity<Void> logout(HttpServletRequest request,HttpServletResponse response) {
        service.logout(cookies.ler(request));
        cookies.limpar(response);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/health") public java.util.Map<String,String> health() {
        return java.util.Map.of("status","ok");
    }
}
