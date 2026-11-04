package br.edu.uninassau.clinicaescola.exception;

import br.edu.uninassau.clinicaescola.dto.ErroResposta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
// Traduz falhas conhecidas para HTTP e oculta detalhes de erros inesperados.
public class TratamentoErros {
    private static final Logger LOG = LoggerFactory.getLogger(TratamentoErros.class);
    @ExceptionHandler(CadastroInvalidoException.class)
    ResponseEntity<ErroResposta> cadastro(CadastroInvalidoException erro) {
        return ResponseEntity.badRequest().body(ErroResposta.de(erro.codigo(), erro.getMessage(), erro.campos()));
    }
    @ExceptionHandler(AutenticacaoException.class)
    ResponseEntity<ErroResposta> autenticacao(AutenticacaoException erro) { return ResponseEntity.status(401).body(ErroResposta.de(erro.codigo(),erro.getMessage(),null)); }
    @ExceptionHandler(EmailJaCadastradoException.class)
    ResponseEntity<ErroResposta> duplicado() {
        return ResponseEntity.status(409).body(ErroResposta.de("EMAIL_JA_CADASTRADO", "Email já cadastrado.", null));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    ResponseEntity<ErroResposta> jsonInvalido() {
        return ResponseEntity.badRequest().body(ErroResposta.de("DADOS_INVALIDOS",
                "Corpo JSON inválido ou fora dos limites permitidos.", null));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResposta> inesperado(Exception erro) {
        // Nunca registrar mensagem, stack, parâmetros SQL, causa ou objeto recebido.
        LOG.error("Falha no cadastro: {}", erro.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(ErroResposta.de("ERRO_INTERNO",
                "Não foi possível concluir a operação.", null));
    }
}
