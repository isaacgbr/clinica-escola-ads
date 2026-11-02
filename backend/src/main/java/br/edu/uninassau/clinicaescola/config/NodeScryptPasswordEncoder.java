package br.edu.uninassau.clinicaescola.config;
import org.bouncycastle.crypto.generators.SCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;
import java.util.regex.Pattern;
// Mantém as senhas compatíveis com os hashes já criados pelo Node.
public final class NodeScryptPasswordEncoder implements PasswordEncoder {
    private static final Pattern FORMATO = Pattern.compile("scrypt\\$32768\\$8\\$3\\$([0-9a-f]{32})\\$([0-9a-f]{128})");
    private final SecureRandom random = new SecureRandom();
    // Substitui surrogates isolados por U+FFFD, como o UTF-8 do Node.
    public static byte[] utf8(String texto) {
        StringBuilder seguro = new StringBuilder();
        texto.codePoints().forEach(c -> seguro.appendCodePoint(c >= 0xd800 && c <= 0xdfff ? 0xfffd : c));
        return seguro.toString().getBytes(StandardCharsets.UTF_8);
    }
    // Usa os parâmetros do Node para preservar as senhas existentes.
    private byte[] derivar(CharSequence senha, String salt) {
        return SCrypt.generate(utf8(senha.toString()), utf8(salt), 32768, 8, 3, 64);
    }
    @Override public String encode(CharSequence senha) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String hex = HexFormat.of().formatHex(salt);
        return "scrypt$32768$8$3$" + hex + "$" + HexFormat.of().formatHex(derivar(senha, hex));
    }
    // Usa o salt hexadecimal como texto, igual ao cadastro feito no Node.
    @Override public boolean matches(CharSequence senha, String hash) {
        if (senha == null) return false;
        var partes = FORMATO.matcher(hash == null ? "" : hash);
        boolean formatoValido = partes.matches();
        byte[] chave = derivar(senha, formatoValido ? partes.group(1) : "0".repeat(32));
        byte[] esperada = formatoValido ? HexFormat.of().parseHex(partes.group(2)) : new byte[64];
        boolean igual = MessageDigest.isEqual(chave, esperada);
        return formatoValido && igual;
    }
}
