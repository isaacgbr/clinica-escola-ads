package br.edu.uninassau.clinicaescola.service;

import br.edu.uninassau.clinicaescola.dto.UsuarioAutenticado;
import br.edu.uninassau.clinicaescola.dto.UsuarioPublico;
import br.edu.uninassau.clinicaescola.entity.Sessao;
import br.edu.uninassau.clinicaescola.exception.*;
import br.edu.uninassau.clinicaescola.repository.*;
import br.edu.uninassau.clinicaescola.config.NodeScryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
@Service
// Coordena credenciais e sessões sem depender das requisições HTTP.
public class AuthService {
    private final UsuarioRepository usuarios;
    private final SessaoRepository sessoes;
    private final PasswordEncoder senhas;
    private final Clock relogio;
    public AuthService(UsuarioRepository usuarios,SessaoRepository sessoes,PasswordEncoder senhas,Clock relogio) {
        this.usuarios=usuarios;
        this.sessoes=sessoes;
        this.senhas=senhas;
        this.relogio=relogio;
    }
    public record EntradaLogin(String email,String senha) {
        @Override public String toString() {
            return "EntradaLogin[protegido]";
        }
    }
    public record LoginConcluido(String token,UsuarioPublico usuario) {
        @Override public String toString() {
            return "LoginConcluido[protegido]";
        }
    }
    // Usa a mesma normalização do limitador para reconhecer variações de espaços.
    public static String normalizarEmail(String email) {
        return email.replaceAll("^[\\s\\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff]+|[\\s\\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff]+$", "").toLowerCase(Locale.ROOT);
    }
    // Rejeita campos extras e preserva os espaços da senha.
    public static EntradaLogin validar(Object entrada) {
        if (!(entrada instanceof Map<?,?> dados)) throw new CadastroInvalidoException("Informe email e senha em um objeto JSON.");
        if (dados.keySet().stream().anyMatch(c -> !Set.of("email","senha").contains(c)) || !(dados.get("email") instanceof String) || !(dados.get("senha") instanceof String))
        throw new CadastroInvalidoException("Informe somente email e senha como textos.");
        String email = normalizarEmail((String) dados.get("email"));
        String senha=(String)dados.get("senha");
        int tamanho=senha.codePointCount(0,senha.length());
        if(email.length()>150 || email.codePoints().anyMatch(c -> Character.isSpaceChar(c) || c == 0xfeff) || !email.matches("[^\\s@\\x00]+@[^\\s@\\x00]+\\.[^\\s@\\x00]+") || tamanho<8 || tamanho>128 || NodeScryptPasswordEncoder.utf8(senha).length>512)
        throw new CadastroInvalidoException("Informe email válido e senha de 8 a 128 caracteres.");
        return new EntradaLogin(email,senha);
    }
    public static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        }
        catch(NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel.");
        }
    }
    // Verifica a senha mesmo sem usuário e substitui apenas a sessão apresentada.
    @Transactional public LoginConcluido login(Object entrada,String anterior) {
        var dados=validar(entrada);
        var usuario=usuarios.buscarPorEmail(dados.email()).orElse(null);
        boolean valida=senhas.matches(dados.senha(),usuario==null ? null : usuario.senhaHash());
        if(usuario==null || !valida) throw new AutenticacaoException("CREDENCIAIS_INVALIDAS","Email ou senha inválidos.");
        var agora=relogio.instant();
        if(anterior!=null)sessoes.revogar(hash(anterior),agora);
        byte[] bytes=new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token=HexFormat.of().formatHex(bytes);
        sessoes.saveAndFlush(new Sessao(usuario,hash(token),agora));
        return new LoginConcluido(token,new UsuarioPublico(usuario.getId().toString(),usuario.getNomeCompleto()));
    }
    // Mantém a renovação e a leitura da sessão na mesma transação.
    @Transactional public UsuarioAutenticado autenticar(String token) {
        if(token!=null) {
            var agora=relogio.instant();
            if(sessoes.renovar(hash(token),agora,agora.minusSeconds(1800))==1) {
                var usuario=sessoes.findByTokenHash(hash(token)).orElseThrow().getUsuario();
                return new UsuarioAutenticado(
                        new UsuarioPublico(usuario.getId().toString(),usuario.getNomeCompleto()), usuario.getPerfil());
            }
        }
        throw new AutenticacaoException("NAO_AUTENTICADO","Autenticação necessária.");
    }
    // Revoga somente a sessão apresentada, preservando outros dispositivos.
    @Transactional public void logout(String token) {
        if(token!=null)sessoes.revogar(hash(token),relogio.instant());
    }
}
