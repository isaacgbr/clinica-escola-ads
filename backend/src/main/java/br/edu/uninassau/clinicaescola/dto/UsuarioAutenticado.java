package br.edu.uninassau.clinicaescola.dto;

import br.edu.uninassau.clinicaescola.entity.Perfil;

// Dados internos da sessão; o perfil não é recebido do cliente nem enviado na resposta HTTP.
public record UsuarioAutenticado(UsuarioPublico usuario, Perfil perfil) { }
