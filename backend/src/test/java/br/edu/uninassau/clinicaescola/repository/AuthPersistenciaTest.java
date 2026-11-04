package br.edu.uninassau.clinicaescola.repository;
import br.edu.uninassau.clinicaescola.BancoTeste;
import br.edu.uninassau.clinicaescola.service.*;
import br.edu.uninassau.clinicaescola.exception.AutenticacaoException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@EnabledIfEnvironmentVariable(named="ALLOW_DB_TESTS",matches="1")
// Verifica sessões no banco autorizado e remove somente os dados do teste.
class AuthPersistenciaTest {
    @DynamicPropertySource static void banco(DynamicPropertyRegistry properties){BancoTeste.configurar(properties);}
    @Autowired UsuarioService cadastro;@Autowired AuthService auth;@Autowired UsuarioRepository usuarios;@Autowired JdbcTemplate jdbc;
    @Test void persisteSessaoRotacionaExpiraERevoga(){
        Long id=null;
        try {
            String email="teste-java-"+UUID.randomUUID()+"@example.com";
            var usuario=cadastro.cadastrar(Map.of("nome","Teste Java","email",email,"senha","Teste-local-123!","confirmar_senha","Teste-local-123!","nascimento","2000-01-01","telefone","81999999999"));
            id=Long.valueOf(usuario.id());var entrada=Map.of("email",email.toUpperCase(Locale.ROOT),"senha","Teste-local-123!");
            var login=auth.login(entrada,null);assertEquals(usuario,auth.autenticar(login.token()));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM public.sessoes WHERE token_hash=?",Integer.class,AuthService.hash(login.token())));
            var nova=auth.login(entrada,login.token());assertThrows(AutenticacaoException.class,()->auth.autenticar(login.token()));
            assertEquals(usuario,auth.autenticar(nova.token()));auth.logout(nova.token());assertThrows(AutenticacaoException.class,()->auth.autenticar(nova.token()));
            var expirada=auth.login(entrada,null);
            jdbc.update("UPDATE public.sessoes SET criado_em=now()-interval '9 hours',ultima_atividade_em=now()-interval '2 hours',expira_em=now()-interval '1 hour' WHERE token_hash=?",AuthService.hash(expirada.token()));
            assertThrows(AutenticacaoException.class,()->auth.autenticar(expirada.token()));
        } finally {if(id!=null)usuarios.deleteById(id);}
    }
}
