package br.edu.uninassau.clinicaescola.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "usuarios", schema = "public")
// Mapeia a tabela existente, sem criar ou alterar sua estrutura.
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;
    @Column(nullable = false, length = 150)
    private String email;
    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;
    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;
    @Column(nullable = false, length = 11)
    private String telefone;
    // Defaults e trigger pertencem ao PostgreSQL existente.
    @Column(name = "criado_em", insertable = false, updatable = false, nullable = false)
    private Instant criadoEm;
    @Column(name = "atualizado_em", insertable = false, updatable = false, nullable = false)
    private Instant atualizadoEm;

    protected Usuario() { }

    public Usuario(String nomeCompleto, String email, String senhaHash, LocalDate dataNascimento, String telefone) {
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.senhaHash = senhaHash;
        this.dataNascimento = dataNascimento;
        this.telefone = telefone;
    }

    public Long getId() { return id; }
    public String getNomeCompleto() { return nomeCompleto; }
    public String email() { return email; }
    public String senhaHash() { return senhaHash; }
    public LocalDate dataNascimento() { return dataNascimento; }
    public String telefone() { return telefone; }
    @Override public String toString() { return "Usuario[conteúdo protegido]"; }
}
