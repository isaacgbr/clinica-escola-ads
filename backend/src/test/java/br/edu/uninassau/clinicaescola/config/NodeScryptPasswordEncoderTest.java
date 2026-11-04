package br.edu.uninassau.clinicaescola.config;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
// Compara Java e Node com exemplos de espaços e caracteres especiais.
class NodeScryptPasswordEncoderTest {
    record Vetor(String senha,String hash) {
        @Override public String toString() {
            return "Vetor[protegido]";
        }
    }
    @Test void verificaVetoresNodeEHashesJavaNoNode() throws Exception {
        var mapper=new JsonMapper();
        var encoder=new NodeScryptPasswordEncoder();
        try(var stream=getClass().getResourceAsStream("/scrypt-node-vectors.json")) {
            for(var vetor:mapper.readValue(stream,Vetor[].class)) {
                assertTrue(encoder.matches(vetor.senha(),vetor.hash()));
                assertFalse(encoder.matches("incorreta",vetor.hash()));
                String hash=encoder.encode(vetor.senha());
                assertTrue(encoder.matches(vetor.senha(),hash));
                var process=new ProcessBuilder("node","-e","let s='';process.stdin.on('data',c=>s+=c);process.stdin.on('end',()=>{const x=JSON.parse(s),p=x.hash.split('$'),c=require('node:crypto');process.stdout.write(c.scryptSync(x.senha,p[4],64,{N:32768,r:8,p:3,maxmem:67108864}).toString('hex')===p[5]?'ok':'falha')})").start();
                try(var output=process.getOutputStream()) {
                    output.write(NodeScryptPasswordEncoder.utf8(mapper.writeValueAsString(Map.of("senha",vetor.senha(),"hash",hash))));
                }
                assertTrue(process.waitFor(20,java.util.concurrent.TimeUnit.SECONDS));
                assertEquals("ok",new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8));
            }
        }
    }
    @Test void rejeitaFormatoInvalidoEUsaSaltAleatorio() {
        var encoder=new NodeScryptPasswordEncoder();
        assertFalse(encoder.matches("Teste-local-123!",null));
        assertFalse(encoder.matches("Teste-local-123!","invalido"));
        assertFalse(encoder.encode("Teste-local-123!").equals(encoder.encode("Teste-local-123!")));
    }
}
