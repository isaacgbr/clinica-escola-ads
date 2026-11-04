package br.edu.uninassau.clinicaescola.config;
import jakarta.servlet.http.*;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.time.Duration;
import java.util.*;
// Cuida do cookie sem enviar o identificador no corpo da resposta.
public class SessaoCookie {
    private final String origem;
    private final boolean secure;
    private final String nome;
    public SessaoCookie(@Value("${AUTH_ORIGIN:http://localhost:8080}") String origem,@Value("${NODE_ENV:development}") String ambiente) {
        URI uri;
        try {
            uri=URI.create(origem);
        }
        catch(IllegalArgumentException e) {
            throw new IllegalStateException("AUTH_ORIGIN invalida.");
        }
        secure="https".equals(uri.getScheme());
        boolean local=Set.of("localhost","127.0.0.1","[::1]").contains(uri.getHost()==null?"":uri.getHost());
        if(uri.getHost()==null || uri.isOpaque() || uri.getUserInfo()!=null || uri.getRawQuery()!=null || uri.getFragment()!=null || !(uri.getPath()==null || uri.getPath().isEmpty()) || (!secure && (!local || !"http".equals(uri.getScheme()) || "production".equals(ambiente))))throw new IllegalStateException("AUTH_ORIGIN exige origem exata e HTTPS em producao.");
        this.origem=origem;
        nome=secure?"__Host-clinica_sessao":"clinica_sessao_local";
    }
    public String origem() {
        return origem;
    }
    // Recusa cookies repetidos ou fora do formato esperado.
    public String ler(HttpServletRequest request) {
        var valores=Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0])).filter(c->nome.equals(c.getName())).toList();
        return valores.size()==1 && valores.getFirst().getValue().matches("[a-f0-9]{64}") ? valores.getFirst().getValue():null;
    }
    public void escrever(HttpServletResponse response,String token) {
        cookie(response,token,Duration.ofHours(8));
    }
    public void limpar(HttpServletResponse response) {
        cookie(response,"",Duration.ZERO);
    }
    private void cookie(HttpServletResponse response,String valor,Duration duracao) {
        response.addHeader("Set-Cookie",ResponseCookie.from(nome,valor).httpOnly(true).secure(secure).sameSite("Lax").path("/").maxAge(duracao).build().toString());
    }
}
