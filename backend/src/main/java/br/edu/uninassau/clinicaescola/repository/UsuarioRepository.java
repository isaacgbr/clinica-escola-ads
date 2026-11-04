package br.edu.uninassau.clinicaescola.repository;

import br.edu.uninassau.clinicaescola.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Consulta usuários sem tratar caracteres do email como padrões de busca.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @Query(value="SELECT * FROM public.usuarios WHERE LOWER(email)=LOWER(:email) LIMIT 1",nativeQuery=true)
    java.util.Optional<Usuario> buscarPorEmail(@Param("email") String email);

    // Igualdade literal utiliza o índice existente, sem curingas de LIKE/ILIKE.
    @Query(value = "SELECT EXISTS (SELECT 1 FROM public.usuarios WHERE LOWER(email) = LOWER(:email))", nativeQuery = true)
    boolean existePorEmail(@Param("email") String email);
}
