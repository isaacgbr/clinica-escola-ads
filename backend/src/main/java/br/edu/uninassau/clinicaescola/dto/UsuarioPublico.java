package br.edu.uninassau.clinicaescola.dto;

// Limita os dados enviados ao cliente, sem expor a entidade ou sua senha.
public record UsuarioPublico(String id, String nome) { }
