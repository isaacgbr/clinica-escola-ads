package br.edu.uninassau.clinicaescola.service;

import br.edu.uninassau.clinicaescola.dto.CadastroValidado;
import br.edu.uninassau.clinicaescola.exception.CadastroInvalidoException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

// Preserva as regras do Node, inclusive limites de datas e caracteres.
public final class ValidadorCadastro {
    private static final List<String> CAMPOS = List.of(
            "nome", "email", "senha", "confirmar_senha", "nascimento", "telefone");
    private static final Pattern EMAIL = Pattern.compile("^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$");
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private final Clock relogio;

    public ValidadorCadastro(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio);
    }

    // Recebe o objeto JSON já decodificado, preservando campos e tipos sem coerção.
    public CadastroValidado validar(Object entrada) {
        if (!(entrada instanceof Map<?, ?> dados)) {
            throw new CadastroInvalidoException("Envie um objeto JSON com os dados do cadastro.");
        }
        if (dados.keySet().stream().anyMatch(campo -> !(campo instanceof String) || !CAMPOS.contains(campo))) {
            throw new CadastroInvalidoException("O cadastro contém campos não permitidos.");
        }

        String nome = removerEspacosExternos(textoOuVazio(dados.get("nome")));
        String email = removerEspacosExternos(textoOuVazio(dados.get("email"))).toLowerCase(Locale.ROOT);
        String senha = textoOuVazio(dados.get("senha"));
        String nascimento = textoOuVazio(dados.get("nascimento"));
        String telefone = textoOuVazio(dados.get("telefone")).chars()
                .filter(c -> !espacoNode(c) && c != '(' && c != ')' && c != '.' && c != '-')
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        Map<String, String> erros = new LinkedHashMap<>();

        for (String campo : CAMPOS) {
            if (!(dados.get(campo) instanceof String texto) || texto.isEmpty()) {
                erros.put(campo, "Campo obrigatório; informe um texto.");
            }
        }
        // As mensagens específicas substituem as de obrigatoriedade, como no Node.
        if (nome.isEmpty() || caracteres(nome) > 150 || nome.indexOf(0) >= 0) {
            erros.put("nome", "Informe um nome com até 150 caracteres, sem o caractere NUL.");
        }
        if (caracteres(email) > 150 || !EMAIL.matcher(email).matches()) {
            erros.put("email", "Informe um email válido com até 150 caracteres.");
        }
        if (caracteres(senha) < 8 || caracteres(senha) > 128 || bytesUtf8Node(senha) > 512) {
            erros.put("senha", "A senha deve ter entre 8 e 128 caracteres.");
        }
        if (!(dados.get("confirmar_senha") instanceof String confirmacao)
                || confirmacao.isEmpty() || !confirmacao.equals(senha)) {
            erros.put("confirmar_senha", "A confirmação deve ser igual à senha.");
        }
        LocalDate data = validarNascimento(nascimento);
        if (data == null) {
            erros.put("nascimento", "Informe uma data válida, não futura, no formato YYYY-MM-DD.");
        }
        if (!telefone.matches("[0-9]{10,11}")) {
            erros.put("telefone", "Informe um telefone com DDD de 10 ou 11 dígitos.");
        }
        if (!erros.isEmpty()) {
            throw new CadastroInvalidoException("Confira os campos informados.", erros);
        }
        return new CadastroValidado(nome, email, senha, data, telefone);
    }

    // Rejeita dias inexistentes e compara com o dia atual em São Paulo.
    private LocalDate validarNascimento(String nascimento) {
        if (!nascimento.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            return null;
        }
        try {
            LocalDate data = LocalDate.parse(nascimento);
            return data.getYear() >= 1 && !data.isAfter(LocalDate.now(relogio.withZone(SAO_PAULO)))
                    ? data : null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String textoOuVazio(Object valor) {
        return valor instanceof String texto ? texto : "";
    }

    private static int caracteres(String texto) {
        return texto.codePointCount(0, texto.length());
    }

    private static int bytesUtf8Node(String texto) {
        // Surrogates isolados ocupam três bytes (U+FFFD), como Buffer.byteLength no Node.
        return texto.codePoints().map(c -> c <= 0x7f ? 1 : c <= 0x7ff ? 2 : c <= 0xffff ? 3 : 4).sum();
    }

    private static String removerEspacosExternos(String texto) {
        int inicio = 0;
        int fim = texto.length();
        while (inicio < fim && espacoNode(texto.charAt(inicio))) inicio++;
        while (fim > inicio && espacoNode(texto.charAt(fim - 1))) fim--;
        return texto.substring(inicio, fim);
    }

    private static boolean espacoNode(int c) {
        // Conjunto de trim()/\s do JavaScript; trim()/strip() do Java não são equivalentes.
        return (c >= 0x09 && c <= 0x0d) || c == 0x20 || c == 0xa0 || c == 0x1680
                || (c >= 0x2000 && c <= 0x200a) || c == 0x2028 || c == 0x2029
                || c == 0x202f || c == 0x205f || c == 0x3000 || c == 0xfeff;
    }
}
