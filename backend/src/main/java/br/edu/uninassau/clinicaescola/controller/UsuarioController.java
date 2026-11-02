package br.edu.uninassau.clinicaescola.controller;

import br.edu.uninassau.clinicaescola.dto.UsuarioResposta;
import br.edu.uninassau.clinicaescola.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
// Delega o cadastro ao serviço e retorna apenas dados públicos.
public class UsuarioController {
    private final UsuarioService service;
    public UsuarioController(UsuarioService service) { this.service = service; }

    @PostMapping(value = {"/usuarios", "/usuarios/"}, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResposta cadastrar(@RequestBody(required = false) Object entrada) {
        // Object preserva tipos JSON: números/booleanos não são convertidos para String.
        return new UsuarioResposta(service.cadastrar(entrada));
    }
}
