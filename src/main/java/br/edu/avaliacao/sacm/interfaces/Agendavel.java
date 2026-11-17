package br.edu.avaliacao.sacm.interfaces;

import java.time.LocalDateTime;

import br.edu.avaliacao.sacm.model.Consulta;

/** Contrato de quem sabe agendar e cancelar consultas. Implementado por ConsultaService. */
public interface Agendavel {

    Consulta agendarConsulta(Long medicoId, Long pacienteId, LocalDateTime dataHora, String motivo);

    void cancelarConsulta(Long consultaId);
}
