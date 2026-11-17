package br.edu.avaliacao.sacm.exception;

/** Médico ou paciente já tem consulta AGENDADA/CONFIRMADA no mesmo horário (RN04). */
public class ConflitoHorarioException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    public ConflitoHorarioException(String mensagem) {
        super(mensagem);
    }

    public ConflitoHorarioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
