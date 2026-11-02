package br.edu.uninassau.clinicaescola.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*;
import java.net.InetAddress;
import java.time.*;
import java.util.*;

// Protege o cadastro antes de encaminhar os dados ao controller.
public final class CadastroProtecaoFilter extends OncePerRequestFilter {
    private static final int MAX_CORPO = 16 * 1024;
    private static final int MAX_IPS = 10000;
    private static final Duration JANELA = Duration.ofMinutes(15);
    private final Clock relogio;
    private final Map<String, Cota> cotas = new HashMap<>();
    private record Cota(Instant fim, int tentativas) { }

    public CadastroProtecaoFilter(Clock relogio) { this.relogio = relogio; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equals(request.getMethod()) || !(path.equals("/usuarios") || path.equals("/usuarios/"));
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain chain) throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        try {
            if (request.getContentType() == null || !MediaType.APPLICATION_JSON
                    .equalsTypeAndSubtype(MediaType.parseMediaType(request.getContentType()))) {
                invalido(response); return;
            }
        } catch (IllegalArgumentException erro) { invalido(response); return; }
        byte[] corpo = request.getInputStream().readNBytes(MAX_CORPO + 1);
        if (corpo.length > MAX_CORPO) { invalido(response); return; }
        long espera = consumir(chaveIp(request.getRemoteAddr()));
        if (espera > 0) {
            response.setHeader("Retry-After", Long.toString(espera));
            ErroSeguranca.enviar(response, 429, "MUITAS_TENTATIVAS",
                    "Muitas tentativas. Aguarde antes de tentar novamente."); return;
        }
        chain.doFilter(new CorpoLimitado(request, corpo), response);
    }

    private synchronized long consumir(String ip) {
        Instant agora = relogio.instant();
        cotas.entrySet().removeIf(entry -> !entry.getValue().fim().isAfter(agora));
        Cota cota = cotas.get(ip);
        // Capacidade limitada: falha fechada sem remover cotas ainda ativas.
        if (cota == null && cotas.size() >= MAX_IPS) return 1;
        if (cota == null) cota = new Cota(agora.plus(JANELA), 0);
        if (cota.tentativas() >= 5) return Math.max(1, (Duration.between(agora, cota.fim()).toMillis() + 999) / 1000);
        cotas.put(ip, new Cota(cota.fim(), cota.tentativas() + 1));
        return 0;
    }

    static String chaveIp(String ip) {
        // Usa somente o IP do transporte. Cabeçalhos de proxy não são confiados.
        if (ip != null && ip.contains(":")) {
            try {
                byte[] bytes = InetAddress.getByName(ip).getAddress();
                if (bytes.length == 16) return HexFormat.of().formatHex(bytes, 0, 7) + "/56";
                return HexFormat.of().formatHex(bytes);
            } catch (IOException erro) { return "ip-invalido"; }
        }
        return ip == null ? "ip-ausente" : ip;
    }

    private void invalido(HttpServletResponse response) throws IOException {
        ErroSeguranca.enviar(response, 400, "DADOS_INVALIDOS", "Corpo JSON inválido ou fora dos limites permitidos.");
    }

    private static final class CorpoLimitado extends HttpServletRequestWrapper {
        private final byte[] corpo;
        CorpoLimitado(HttpServletRequest request, byte[] corpo) { super(request); this.corpo = corpo; }
        @Override public ServletInputStream getInputStream() {
            ByteArrayInputStream stream = new ByteArrayInputStream(corpo);
            return new ServletInputStream() {
                @Override public int read() { return stream.read(); }
                @Override public boolean isFinished() { return stream.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { throw new IllegalStateException("Leitura síncrona."); }
            };
        }
        @Override public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
