package br.edu.uninassau.clinicaescola.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import java.time.Clock;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@EnableWebSecurity
@org.springframework.context.annotation.Import(SessaoCookie.class)
// Define quais rotas são públicas e quais exigem sessão válida.
public class SegurancaConfig {
    // Evita usuário/senha de desenvolvimento automáticos. Autenticacao e realizada pelo servico de sessoes.
    @Bean UserDetailsService autenticacaoIndisponivel() {
        return identificador -> { throw new UsernameNotFoundException("Autenticação não disponível neste módulo."); };
    }

    @Bean SecurityFilterChain seguranca(HttpSecurity http, Clock relogio, br.edu.uninassau.clinicaescola.service.AuthService authService, SessaoCookie cookies) throws Exception {
        RequestMatcher cadastro = request -> "POST".equals(request.getMethod())
                && (request.getRequestURI().substring(request.getContextPath().length()).equals("/usuarios") || request.getRequestURI().substring(request.getContextPath().length()).equals("/usuarios/"));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/usuarios", "/usuarios/").permitAll()
                .requestMatchers(HttpMethod.GET, "/health").permitAll()
                .requestMatchers(HttpMethod.POST,"/auth/login","/auth/login/").permitAll()
                .requestMatchers("/auth/me","/auth/logout").authenticated()
                .anyRequest().denyAll());
        // Cadastro nao usa cookies; autenticacao usa a protecao de origem do filtro proprio.
        http.csrf(csrf -> csrf.ignoringRequestMatchers(cadastro, request -> request.getRequestURI().startsWith(request.getContextPath()+"/auth/")));
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.requestCache(cache -> cache.disable());
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());
        http.logout(logout -> logout.disable());
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, erro) -> ErroSeguranca.enviar(res, 401,
                        "NAO_AUTENTICADO", "Autenticação necessária."))
                .accessDeniedHandler((req, res, erro) -> ErroSeguranca.enviar(res, 403,
                        "ACESSO_NEGADO", "Requisição não permitida.")));
        http.addFilterBefore(new CadastroProtecaoFilter(relogio), AuthorizationFilter.class);
        http.addFilterBefore(new AuthProtecaoFilter(authService,cookies,relogio), AuthorizationFilter.class);
        return http.build();
    }
}
