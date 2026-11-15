package br.com.sis_dispo_docente.exception;

/** Violação de regra de negócio. Vira HTTP 409. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
