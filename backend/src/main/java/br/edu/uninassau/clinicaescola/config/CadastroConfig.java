package br.edu.uninassau.clinicaescola.config;

import br.edu.uninassau.clinicaescola.service.ValidadorCadastro;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
// Fornece o relógio e as regras de cadastro usados pela aplicação e pelos testes.
public class CadastroConfig {
    @Bean org.springframework.security.crypto.password.PasswordEncoder senhas() { return new NodeScryptPasswordEncoder(); }
    @Bean Clock relogio() { return Clock.systemUTC(); }
    @Bean ValidadorCadastro validadorCadastro(Clock relogio) { return new ValidadorCadastro(relogio); }
}
