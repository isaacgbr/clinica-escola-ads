package br.edu.uninassau.clinicaescola;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ALLOW_DB_TESTS", matches = "1")
// Verifica a inicialização completa somente com banco de teste autorizado.
class ClinicaEscolaApplicationTests {

    @DynamicPropertySource
    static void bancoAutorizado(DynamicPropertyRegistry properties) { BancoTeste.configurar(properties); }

    @Test
    void contextLoads() {
    }

}
