package br.edu.avaliacao.sacm.enums;

/**
 * Ciclo de vida da consulta (RN06):
 * AGENDADA -> CONFIRMADA ou CANCELADA; CONFIRMADA -> FINALIZADA ou CANCELADA.
 * CANCELADA e FINALIZADA são estados finais.
 */
public enum StatusConsulta {
    AGENDADA,
    CONFIRMADA,
    CANCELADA,
    FINALIZADA;

    /** Consultas nesses estados ocupam o horário do médico e do paciente (RN04/RN07). */
    public boolean ocupaHorario() {
        return this == AGENDADA || this == CONFIRMADA;
    }

    public boolean isFinal() {
        return this == CANCELADA || this == FINALIZADA;
    }
}
