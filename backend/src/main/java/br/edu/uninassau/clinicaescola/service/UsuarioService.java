package br.edu.uninassau.clinicaescola.service;

import br.edu.uninassau.clinicaescola.dto.UsuarioPublico;
import br.edu.uninassau.clinicaescola.entity.Usuario;
import br.edu.uninassau.clinicaescola.entity.Perfil;
import org.springframework.transaction.annotation.Isolation;
import br.edu.uninassau.clinicaescola.exception.EmailJaCadastradoException;
import br.edu.uninassau.clinicaescola.repository.UsuarioRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// Coordena validação, proteção da senha e gravação do cadastro.
public class UsuarioService {
    private final UsuarioRepository repository;
    private final ValidadorCadastro validador;
    private final PasswordEncoder senhas;

    public UsuarioService(UsuarioRepository repository, ValidadorCadastro validador, PasswordEncoder senhas) {
        this.repository = repository;
        this.validador = validador;
        this.senhas = senhas;
    }

    // Valida antes de proteger a senha e mantém a gravação na mesma transação.
    @Transactional
    public UsuarioPublico cadastrar(Object entrada) {
        return criar(entrada, Perfil.INTERNO);
    }

    // A transação serializável impede dois provisionamentos simultâneos de primeiro ADMIN.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void provisionarPrimeiroAdmin(Object entrada) {
        if (repository.existsByPerfil(Perfil.ADMIN)) {
            throw new IllegalStateException("Já existe um administrador; provisionamento recusado.");
        }
        criar(entrada, Perfil.ADMIN);
    }

    private UsuarioPublico criar(Object entrada, Perfil perfil) {
        var dados = validador.validar(entrada);
        if (repository.existePorEmail(dados.email())) throw new EmailJaCadastradoException();
        String hash = senhas.encode(dados.senha());
        var usuario = perfil == Perfil.ADMIN
                ? Usuario.primeiroAdministrador(dados.nome(), dados.email(), hash, dados.nascimento(), dados.telefone())
                : new Usuario(dados.nome(), dados.email(), hash, dados.nascimento(), dados.telefone());
        try {
            usuario = repository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException erro) {
            // A pré-consulta não substitui o índice único sob concorrência.
            if (conflitoEmail(erro)) throw new EmailJaCadastradoException();
            throw erro;
        }
        return new UsuarioPublico(usuario.getId().toString(), usuario.getNomeCompleto());
    }

    private boolean conflitoEmail(Throwable erro) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacao
                    && "23505".equals(violacao.getSQLState())
                    && "uq_usuarios_email".equals(violacao.getConstraintName())) return true;
            // O Hibernate pode omitir o nome; o driver mantém a restrição em metadados estruturados.
            if (causa instanceof PSQLException postgres && "23505".equals(postgres.getSQLState())) {
                var detalhes = postgres.getServerErrorMessage();
                if (detalhes != null && "uq_usuarios_email".equals(detalhes.getConstraint())) return true;
            }
        }
        return false;
    }
}
