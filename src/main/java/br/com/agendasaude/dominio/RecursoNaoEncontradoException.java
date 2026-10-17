package br.com.agendasaude.dominio;

/** Lançada quando um id informado não existe (vira HTTP 404 na camada web). */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Long id) {
        super(recurso + " " + id + " não encontrado(a).");
    }
}
