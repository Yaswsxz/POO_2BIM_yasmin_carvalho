package br.edu.avaliacao.sacm.exception;

/**
 * Erro de regra de negócio: a mensagem é escrita para o usuário ler (RN09).
 * É unchecked para não poluir as assinaturas, mas é sempre tratada no console.
 */
public class RegraNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }

    /** Usado quando a regra foi detectada pelo banco (ex.: UNIQUE): guarda a causa técnica. */
    public RegraNegocioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
