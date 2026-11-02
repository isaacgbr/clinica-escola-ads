package br.edu.uninassau.clinicaescola.service;

import br.edu.uninassau.clinicaescola.dto.CadastroValidado;
import br.edu.uninassau.clinicaescola.exception.CadastroInvalidoException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

// Confere as regras e mensagens que devem permanecer iguais às do Node.
class ValidadorCadastroTest {
    private final ValidadorCadastro validador = validadorEm("2026-10-29T01:00:00Z");
    private static final Map<String, String> MENSAGENS = Map.of(
            "nome", "Informe um nome com até 150 caracteres, sem o caractere NUL.",
            "email", "Informe um email válido com até 150 caracteres.",
            "senha", "A senha deve ter entre 8 e 128 caracteres.",
            "confirmar_senha", "A confirmação deve ser igual à senha.",
            "nascimento", "Informe uma data válida, não futura, no formato YYYY-MM-DD.",
            "telefone", "Informe um telefone com DDD de 10 ou 11 dígitos.");

    @Test
    void normalizaCadastroSemAlterarSenha() {
        var entrada = valido();
        entrada.put("senha", "  senha com espaços  ");
        entrada.put("confirmar_senha", entrada.get("senha"));
        CadastroValidado dados = validador.validar(entrada);
        assertEquals("Maria Silva", dados.nome());
        assertEquals("maria@example.com", dados.email());
        assertEquals("81999999999", dados.telefone());
        assertEquals(LocalDate.of(2000, 2, 29), dados.nascimento());
        assertEquals(entrada.get("senha"), dados.senha());
        assertFalse(dados.toString().contains((String) entrada.get("senha")));
        assertFalse(dados.toString().contains(dados.email()));
    }

    @TestFactory
    Stream<DynamicTest> rejeitaCorposQueNaoSaoObjetos() {
        return Arrays.asList(null, true, 42, "texto", List.of(), new String[0]).stream()
                .map(entrada -> DynamicTest.dynamicTest("corpo: " + String.valueOf(entrada), () -> {
                    var erro = assertThrows(CadastroInvalidoException.class, () -> validador.validar(entrada));
                    assertEquals("Envie um objeto JSON com os dados do cadastro.", erro.getMessage());
                    assertEquals("DADOS_INVALIDOS", erro.codigo());
                    assertNull(erro.campos());
                }));
    }

    @TestFactory
    List<DynamicTest> rejeitaAusenciaETiposSemConverterParaTexto() {
        var testes = new ArrayList<DynamicTest>();
        for (String campo : valido().keySet()) {
            testes.add(DynamicTest.dynamicTest(campo + " ausente", () -> {
                var entrada = valido();
                entrada.remove(campo);
                conferirErroCampo(entrada, campo);
            }));
            for (Object valor : Arrays.asList(null, 123, true, List.of(), Map.of(), "")) {
                testes.add(DynamicTest.dynamicTest(campo + ": " + String.valueOf(valor), () ->
                        conferirErroCampo(com(campo, valor), campo)));
            }
        }
        return testes;
    }

    @Test
    void preservaPrecedenciaMensagensEOrdemDosCampos() {
        for (String campoExtra : List.of("id", "senha_hash", "criado_em", "atualizado_em", "outro")) {
            var erro = assertThrows(CadastroInvalidoException.class,
                    () -> validador.validar(Map.of(campoExtra, "1")));
            assertEquals("O cadastro contém campos não permitidos.", erro.getMessage());
            assertNull(erro.campos());
        }
        var erro = assertThrows(CadastroInvalidoException.class, () -> validador.validar(Map.of()));
        assertEquals("Confira os campos informados.", erro.getMessage());
        assertEquals(MENSAGENS, erro.campos());
        assertEquals(List.of("nome", "email", "senha", "confirmar_senha", "nascimento", "telefone"),
                new ArrayList<>(erro.campos().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> erro.campos().clear());
        var entrada = com("senha", null);
        var senhaInvalida = assertThrows(CadastroInvalidoException.class, () -> validador.validar(entrada));
        assertEquals(Map.of("senha", MENSAGENS.get("senha"),
                "confirmar_senha", MENSAGENS.get("confirmar_senha")), senhaInvalida.campos());
        var soNome = assertThrows(CadastroInvalidoException.class, () -> validador.validar(com("nome", null)));
        assertEquals(Map.of("nome", MENSAGENS.get("nome")), soNome.campos());
    }

    @Test
    void respeitaLimitesUnicodeDoNomeSemExigirSobrenome() {
        for (String nome : List.of("A", "123", "😀".repeat(150), "x".repeat(150))) {
            assertEquals(nome, validador.validar(com("nome", nome)).nome());
        }
        for (String nome : List.of("   ", "x".repeat(151), "😀".repeat(151),
                String.valueOf((char) 0), "Maria" + (char) 0 + "Silva")) {
            conferirErroCampo(com("nome", nome), "nome");
        }
    }

    @Test
    void preservaRegexDoEmailELimiteDe150Caracteres() {
        String emailLimite = "a".repeat(138) + "@example.com";
        assertEquals(150, emailLimite.length());
        for (String email : List.of(emailLimite, "a_b@example.com", "a%b@example.com", "o'neil@example.com",
                "a+b@example.com", "a@b.co", "k@example.com")) {
            assertEquals(email, validador.validar(com("email", email)).email());
        }
        assertEquals("k@example.com", validador.validar(com("email", "K@example.com")).email());
        for (String email : List.of("a" + emailLimite, "a@b", "a..b@example.com", "a@-example.com",
                "a@b-.com", "joão@example.com", ".a@example.com", "a.@example.com", "a b@example.com")) {
            conferirErroCampo(com("email", email), "email");
        }
    }

    @Test
    void preservaEspacosDaSenhaEContaPontosDeCodigo() {
        for (String senha : List.of("x".repeat(8), "x".repeat(128), " ".repeat(8),
                "😀".repeat(128), "á".repeat(128), String.valueOf((char) 0xd800).repeat(128))) {
            var entrada = com("senha", senha);
            entrada.put("confirmar_senha", senha);
            assertEquals(senha, validador.validar(entrada).senha());
        }
        for (String senha : List.of("x".repeat(7), "x".repeat(129), "😀".repeat(129))) {
            var entrada = com("senha", senha);
            entrada.put("confirmar_senha", senha);
            conferirErroCampo(entrada, "senha");
        }
        conferirErroCampo(com("confirmar_senha", "Teste-local-123! "), "confirmar_senha");
        conferirErroCampo(com("confirmar_senha", "teste-local-123!"), "confirmar_senha");
    }

    @Test
    void normalizaSomenteOsEspacosEPontuacoesAceitosNoNode() {
        int[] espacos = {9, 10, 11, 12, 13, 32, 0xa0, 0x1680, 0x2000, 0x2001, 0x2002, 0x2003,
                0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200a, 0x2028, 0x2029,
                0x202f, 0x205f, 0x3000, 0xfeff};
        for (int codigo : espacos) {
            String espaco = Character.toString(codigo);
            var entrada = com("nome", espaco + "Maria" + espaco);
            entrada.put("email", espaco + "MARIA@EXAMPLE.COM" + espaco);
            entrada.put("telefone", "81" + espaco + "99999-9999");
            var dados = validador.validar(entrada);
            assertEquals("Maria", dados.nome(), "Espaço " + codigo);
            assertEquals("maria@example.com", dados.email());
            assertEquals("81999999999", dados.telefone());
        }
        for (int codigo : new int[]{0x85, 0x180e, 0x200b, 0x1c}) {
            String caractere = Character.toString(codigo);
            assertEquals(caractere + "Maria" + caractere,
                    validador.validar(com("nome", caractere + "Maria" + caractere)).nome());
            conferirErroCampo(com("telefone", "81" + caractere + "999999999"), "telefone");
        }
        assertEquals("8133334444", validador.validar(com("telefone", " (81) 3333.4444 ")).telefone());
        for (String telefone : List.of("819999999", "819999999999", "+5581999999999", "81/99999/9999",
                "abc81999999999", "٨١٩٩٩٩٩٩٩٩٩")) {
            conferirErroCampo(com("telefone", telefone), "telefone");
        }
    }

    @TestFactory
    Stream<DynamicTest> validaDatasReaisComFormatoExato() {
        return Stream.of("0000-01-01", "1900-02-29", "2025-02-29", "2000-02-30", "2000-04-31",
                "2000-13-01", "2000-00-01", "2000-01-00", "31/12/2000", "2000-2-01",
                " 2000-02-29 ", "2000-02-29T00:00:00Z", "+2000-02-29", "10000-01-01", "9999-01-01")
                .map(data -> DynamicTest.dynamicTest(data, () -> conferirErroCampo(com("nascimento", data), "nascimento")));
    }

    @Test
    void aceitaAnoUmEBissextosSemIdadeMinima() {
        for (String data : List.of("0001-01-01", "2000-02-29", "2024-02-29", "2026-10-28")) {
            assertEquals(LocalDate.parse(data), validador.validar(com("nascimento", data)).nascimento());
        }
    }

    @Test
    void usaCalendarioDeSaoPauloMesmoQuandoUtcJaMudouDeDiaOuAno() {
        String[][] cenarios = {
                {"2026-10-29T01:00:00Z", "2026-10-28", "2026-10-29"},
                {"2026-10-29T02:59:59.999Z", "2026-10-28", "2026-10-29"},
                {"2026-10-29T03:00:00Z", "2026-10-29", "2026-10-30"},
                {"2027-01-01T01:00:00Z", "2026-12-31", "2027-01-01"}};
        for (String[] cenario : cenarios) {
            var validadorData = validadorEm(cenario[0]);
            assertEquals(LocalDate.parse(cenario[1]), validadorData.validar(com("nascimento", cenario[1])).nascimento());
            var erro = assertThrows(CadastroInvalidoException.class,
                    () -> validadorData.validar(com("nascimento", cenario[2])));
            assertEquals(Map.of("nascimento", MENSAGENS.get("nascimento")), erro.campos());
        }
    }

    @Test
    void naoModificaEntradaERejeitaChavesQueNaoRepresentamCamposJson() {
        var entrada = valido();
        var copia = new LinkedHashMap<>(entrada);
        validador.validar(entrada);
        assertEquals(copia, entrada);
        for (Object chave : Arrays.asList(null, 42)) {
            var dados = new LinkedHashMap<Object, Object>(entrada);
            dados.put(chave, "valor");
            var erro = assertThrows(CadastroInvalidoException.class, () -> validador.validar(dados));
            assertEquals("O cadastro contém campos não permitidos.", erro.getMessage());
            assertNull(erro.campos());
        }
    }

    private void conferirErroCampo(Map<String, Object> entrada, String campo) {
        var erro = assertThrows(CadastroInvalidoException.class, () -> validador.validar(entrada));
        assertEquals("DADOS_INVALIDOS", erro.codigo());
        assertEquals("Confira os campos informados.", erro.getMessage());
        var esperados = new LinkedHashMap<>(Map.of(campo, MENSAGENS.get(campo)));
        if (campo.equals("senha") && !Objects.equals(entrada.get("senha"), entrada.get("confirmar_senha"))) {
            esperados.put("confirmar_senha", MENSAGENS.get("confirmar_senha"));
        }
        assertEquals(esperados, erro.campos());
    }

    private static ValidadorCadastro validadorEm(String instante) {
        return new ValidadorCadastro(Clock.fixed(Instant.parse(instante), ZoneOffset.UTC));
    }

    private static Map<String, Object> com(String campo, Object valor) {
        var entrada = valido();
        entrada.put(campo, valor);
        return entrada;
    }

    private static Map<String, Object> valido() {
        var dados = new LinkedHashMap<String, Object>();
        dados.put("nome", " Maria Silva ");
        dados.put("email", " Maria@Example.com ");
        dados.put("senha", "Teste-local-123!");
        dados.put("confirmar_senha", "Teste-local-123!");
        dados.put("nascimento", "2000-02-29");
        dados.put("telefone", "(81) 99999-9999");
        return dados;
    }
}
