package br.com.agendasaude.web;

import br.com.agendasaude.dominio.RecursoNaoEncontradoException;
import br.com.agendasaude.dominio.RegraDeNegocioException;
import br.com.agendasaude.dominio.Violacao;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Traduz exceções em respostas HTTP no formato padrão Problem Details (RFC 9457), sempre com um
 * campo "codigo" estável que o front-end pode usar.
 *
 * <ul>
 *   <li>409 Conflict — o pedido é válido, mas conflita com o estado atual (horário ocupado).</li>
 *   <li>422 Unprocessable Content — o pedido é entendido, mas viola uma regra (fora do expediente).</li>
 *   <li>404 Not Found — id inexistente. 400 Bad Request — corpo inválido.</li>
 * </ul>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Set<Violacao> CONFLITOS =
            EnumSet.of(Violacao.MEDICO_OCUPADO, Violacao.PACIENTE_OCUPADO, Violacao.CONSULTA_JA_CANCELADA);
    private static final HttpStatus UNPROCESSABLE = HttpStatus.valueOf(422);

    @ExceptionHandler(RegraDeNegocioException.class)
    ResponseEntity<ProblemDetail> regraDeNegocio(RegraDeNegocioException e) {
        HttpStatus status = CONFLITOS.contains(e.getViolacao()) ? HttpStatus.CONFLICT : UNPROCESSABLE;
        return problema(status, "Regra de negócio violada", e.getMessage(), e.getViolacao().name());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ProblemDetail> naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage(), "NAO_ENCONTRADO");
    }

    /** Índice único do banco: corrida entre duas requisições no mesmo horário, CRM ou e-mail repetido. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> conflitoNoBanco(DataIntegrityViolationException e) {
        return problema(HttpStatus.CONFLICT, "Conflito de dados",
                "O registro conflita com outro já existente (horário recém-ocupado, CRM ou e-mail repetido).",
                "CONFLITO_DE_DADOS");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> requisicaoInvalida(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(erro -> campos.put(erro.getField(), erro.getDefaultMessage()));
        ResponseEntity<ProblemDetail> resposta =
                problema(HttpStatus.BAD_REQUEST, "Requisição inválida", "Corrija os campos indicados.", "REQUISICAO_INVALIDA");
        resposta.getBody().setProperty("campos", campos);
        return resposta;
    }

    private static ResponseEntity<ProblemDetail> problema(HttpStatus status, String titulo, String detalhe, String codigo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setProperty("codigo", codigo);
        return ResponseEntity.status(status).body(problema);
    }
}
