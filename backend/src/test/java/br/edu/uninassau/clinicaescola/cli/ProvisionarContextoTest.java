package br.edu.uninassau.clinicaescola.cli;

import br.edu.uninassau.clinicaescola.config.SessaoCookie;
import br.edu.uninassau.clinicaescola.controller.AuthController;
import br.edu.uninassau.clinicaescola.repository.SessaoRepository;
import br.edu.uninassau.clinicaescola.repository.UsuarioRepository;
import br.edu.uninassau.clinicaescola.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.SecurityFilterChain;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Inicia o mesmo contexto do comando, substituindo apenas o acesso ao banco.
class ProvisionarContextoTest {
    @TestConfiguration(proxyBeanMethods = false)
    static class PersistenciaSimulada {
        @Bean UsuarioRepository usuarios() { return mock(UsuarioRepository.class); }
        @Bean SessaoRepository sessoes() { return mock(SessaoRepository.class); }
    }

    @Test void iniciaSemServidorSemProvisionarENemAcessarBanco() {
        // Desabilita os componentes que poderiam criar conexão ou executar SQL no teste.
        try (var contexto = ProvisionarPrimeiroAdmin.prepararContexto()
                .sources(PersistenciaSimulada.class)
                .properties("spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
                        "AUTH_ORIGIN=http://localhost:8080", "NODE_ENV=test")
                .run()) {
            assertTrue(contexto.isActive());
            assertInstanceOf(AnnotationConfigApplicationContext.class, contexto);
            assertTrue(contexto.getBeansOfType(SecurityFilterChain.class).isEmpty());
            assertTrue(contexto.getBeansOfType(javax.sql.DataSource.class).isEmpty());
            assertNotNull(contexto.getBean(SessaoCookie.class));
            assertNotNull(contexto.getBean(AuthController.class));
            assertNotNull(contexto.getBean(UsuarioService.class));
            verifyNoInteractions(contexto.getBean(UsuarioRepository.class), contexto.getBean(SessaoRepository.class));
        }
    }
}
