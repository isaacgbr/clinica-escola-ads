package br.edu.uninassau.clinicaescola.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="sessoes", schema="public")
// Guarda o usuário e as datas que controlam a validade da sessão.
public class Sessao  {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="usuario_id") private Usuario usuario;
    @Column(name="token_hash", nullable=false, length=64) private String tokenHash;
    @Column(name="criado_em", nullable=false) private Instant criadoEm;
    @Column(name="ultima_atividade_em", nullable=false) private Instant ultimaAtividadeEm;
    @Column(name="expira_em", nullable=false) private Instant expiraEm;
    @Column(name="revogada_em") private Instant revogadaEm;
    protected Sessao()  {
    }
    public Sessao(Usuario usuario, String hash, Instant agora)  {
        this.usuario=usuario;
        tokenHash=hash;
        criadoEm=agora;
        ultimaAtividadeEm=agora;
        expiraEm=agora.plusSeconds(28800);
    }
    public Usuario getUsuario()  {
        return usuario;
    }
    @Override public String toString()  {
        return "Sessao[conteudo protegido]";
    }
}
