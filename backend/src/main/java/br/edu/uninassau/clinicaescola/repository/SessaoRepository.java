package br.edu.uninassau.clinicaescola.repository;
import br.edu.uninassau.clinicaescola.entity.Sessao;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
// Atualiza a sessão usando somente o resumo do identificador.
public interface SessaoRepository extends JpaRepository<Sessao,Long>  {
    Optional<Sessao> findByTokenHash(String hash);
    @Modifying @Query(value="UPDATE public.sessoes SET revogada_em=:agora WHERE token_hash=:hash AND revogada_em IS NULL",nativeQuery=true)
    int revogar(@Param("hash") String hash,@Param("agora") Instant agora);
    // Atualizacao atomica nao retrocede a atividade de requisicoes fora de ordem.
    @Modifying @Query(value="UPDATE public.sessoes SET ultima_atividade_em=GREATEST(ultima_atividade_em,:agora) WHERE token_hash=:hash AND revogada_em IS NULL AND expira_em>:agora AND ultima_atividade_em>:limite",nativeQuery=true)
    int renovar(@Param("hash") String hash,@Param("agora") Instant agora,@Param("limite") Instant limite);
}
