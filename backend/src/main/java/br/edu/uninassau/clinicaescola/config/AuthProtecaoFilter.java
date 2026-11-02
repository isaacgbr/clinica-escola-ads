package br.edu.uninassau.clinicaescola.config;
import br.edu.uninassau.clinicaescola.service.AuthService;
import br.edu.uninassau.clinicaescola.exception.AutenticacaoException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.io.*;
import java.time.*;
import java.util.*;
// Confere a origem das chamadas, limita tentativas e identifica a sessão atual.
public final class AuthProtecaoFilter extends OncePerRequestFilter {
    private final AuthService service;
    private final SessaoCookie cookies;
    private final Clock relogio;
    private final Map<String,Cota> ips=new HashMap<>(), pares=new HashMap<>();
    private static final class Cota {
        Instant fim;
        int usadas;
        Cota(Instant fim) {
            this.fim=fim;
        }
    }
    private record Reserva(String chave,Cota ip,Cota par) {
    }
    public AuthProtecaoFilter(AuthService service,SessaoCookie cookies,Clock relogio) {
        this.service=service;
        this.cookies=cookies;
        this.relogio=relogio;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest req) {
        return !req.getRequestURI().substring(req.getContextPath().length()).startsWith("/auth/");
    }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws IOException,ServletException {
        res.setHeader("Cache-Control","no-store");
        String path=req.getRequestURI().substring(req.getContextPath().length());
        boolean login=path.equals("/auth/login")||path.equals("/auth/login/");
        Reserva reserva=null;
        try {
            if("POST".equals(req.getMethod())) {
                if(!cookies.origem().equals(req.getHeader("Origin"))||!"XMLHttpRequest".equals(req.getHeader("X-Requested-With"))||"cross-site".equals(req.getHeader("Sec-Fetch-Site"))) {
                    ErroSeguranca.enviar(res,403,"ORIGEM_NAO_PERMITIDA","Requisição não permitida.");
                    return;
                }
                if(req.getContentType()==null||!MediaType.APPLICATION_JSON.equalsTypeAndSubtype(MediaType.parseMediaType(req.getContentType()))) {
                    invalido(res);
                    return;
                }
                byte[] corpo=req.getInputStream().readNBytes(16385);
                if(corpo.length>16384) {
                    invalido(res);
                    return;
                }
                Object entrada;
                try {
                    entrada=new tools.jackson.databind.json.JsonMapper().readValue(corpo,Object.class);
                }
                catch(RuntimeException e) {
                    invalido(res);
                    return;
                }
                req=new Corpo(req,corpo);
                if(login) {
                    String email=entrada instanceof Map<?,?> dados && dados.get("email") instanceof String texto?AuthService.normalizarEmail(texto):"";
                    reserva=reservar(CadastroProtecaoFilter.chaveIp(req.getRemoteAddr()),email);
                    if(reserva==null) {
                        res.setHeader("Retry-After","900");
                        ErroSeguranca.enviar(res,429,"MUITAS_TENTATIVAS","Muitas tentativas. Aguarde antes de tentar novamente.");
                        return;
                    }
                }
            }
            if(path.equals("/auth/me")||path.equals("/auth/logout")) {
                String token=cookies.ler(req);
                if(token==null)throw new AutenticacaoException("NAO_AUTENTICADO","Autenticação necessária.");
                var usuario=service.autenticar(token);
                req.setAttribute("usuarioAutenticado",usuario);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(usuario,null,List.of()));
            }
            chain.doFilter(req,res);
        }
        catch(AutenticacaoException e) {
            cookies.limpar(res);
            ErroSeguranca.enviar(res,401,e.codigo(),e.getMessage());
        }
        catch(IllegalArgumentException e) {
            invalido(res);
        }
        catch(RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(AuthProtecaoFilter.class).error("Falha na autenticacao: {}", e.getClass().getSimpleName());
            ErroSeguranca.enviar(res,500,"ERRO_INTERNO","Não foi possível concluir a operação.");
        }
        finally {
            if(reserva!=null)concluir(reserva,res.getStatus());
            SecurityContextHolder.clearContext();
        }
    }
    // Conta chamadas simultâneas; sucessos não consomem a cota de falhas.
    private synchronized Reserva reservar(String ip,String email) {
        Instant agora=relogio.instant();
        ips.entrySet().removeIf(e->!e.getValue().fim.isAfter(agora));
        pares.entrySet().removeIf(e->!e.getValue().fim.isAfter(agora));
        String chave=AuthService.hash(ip+"\n"+email);
        if((ips.size()>=10000&&!ips.containsKey(ip))||(pares.size()>=10000&&!pares.containsKey(chave)))return null;
        Cota a=ips.computeIfAbsent(ip,k->new Cota(agora.plusSeconds(900)));
        if(a.usadas>=20)return null;
        a.usadas++;
        Cota b=pares.computeIfAbsent(chave,k->new Cota(agora.plusSeconds(900)));
        if(b.usadas>=5)return null;
        b.usadas++;
        return new Reserva(chave,a,b);
    }
    // Desconta sucessos sem apagar as falhas gerais daquele IP.
    private synchronized void concluir(Reserva reserva,int status) {
        if(status==200) {
            pares.remove(reserva.chave(),reserva.par());
            reserva.ip().usadas=Math.max(0,reserva.ip().usadas-1);
        }
        else if(status!=401&&status!=429) {
            reserva.ip().usadas=Math.max(0,reserva.ip().usadas-1);
            reserva.par().usadas=Math.max(0,reserva.par().usadas-1);
        }
    }
    private void invalido(HttpServletResponse res)throws IOException {
        ErroSeguranca.enviar(res,400,"DADOS_INVALIDOS","Corpo JSON inválido ou fora dos limites permitidos.");
    }
    private static final class Corpo extends HttpServletRequestWrapper {
        private final byte[] bytes;
        Corpo(HttpServletRequest req,byte[] bytes) {
            super(req);
            this.bytes=bytes;
        }
        @Override public ServletInputStream getInputStream() {
            var input=new ByteArrayInputStream(bytes);
            return new ServletInputStream() {
                public int read() {
                    return input.read();
                }
                public boolean isFinished() {
                    return input.available()==0;
                }
                public boolean isReady() {
                    return true;
                }
                public void setReadListener(ReadListener listener) {
                    throw new IllegalStateException("Leitura sincrona.");
                }
            }
            ;
        }
    }
}
