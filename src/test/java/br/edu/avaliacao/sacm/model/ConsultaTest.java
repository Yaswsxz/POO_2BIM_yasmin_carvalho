package br.edu.avaliacao.sacm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.avaliacao.sacm.enums.Especialidade;
import br.edu.avaliacao.sacm.enums.StatusConsulta;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;

/** Testes unitários do modelo: não precisam do Spring nem do MySQL. */
class ConsultaTest {

    private Medico medico;
    private Paciente paciente;
    private LocalDateTime amanha;

    @BeforeEach
    void preparar() {
        medico = new Medico("Ana Souza", "52998224725", LocalDate.of(1985, 3, 10), null, null,
                "Londrina", "PR", "12345-PR", Especialidade.CARDIOLOGIA, new BigDecimal("300"));
        paciente = new Paciente("Bruno Lima", "11144477735", LocalDate.of(2000, 7, 1), null, null,
                "Londrina", "PR", null, null, null);
        amanha = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
    }

    @Test
    void consultaNovaComecaAgendada() { // RN06
        Consulta consulta = new Consulta(medico, paciente, amanha, "Dor no peito");
        assertEquals(StatusConsulta.AGENDADA, consulta.getStatus());
        assertTrue(consulta.isAtiva());
    }

    @Test
    void naoAceitaDataNoPassado() { // RN05
        assertThrows(RegraNegocioException.class,
                () -> new Consulta(medico, paciente, LocalDateTime.now().minusHours(1), "Retorno"));
    }

    @Test
    void naoAceitaMotivoVazio() { // RN05
        assertThrows(RegraNegocioException.class, () -> new Consulta(medico, paciente, amanha, "   "));
    }

    @Test
    void fluxoCompletoAgendadaConfirmadaFinalizada() { // RN06
        Consulta consulta = new Consulta(medico, paciente, amanha, "Check-up");
        consulta.confirmar();
        assertEquals(StatusConsulta.CONFIRMADA, consulta.getStatus());
        consulta.finalizar();
        assertEquals(StatusConsulta.FINALIZADA, consulta.getStatus());
        assertFalse(consulta.isAtiva());
    }

    @Test
    void naoFinalizaSemConfirmar() { // RN06
        Consulta consulta = new Consulta(medico, paciente, amanha, "Check-up");
        assertThrows(RegraNegocioException.class, consulta::finalizar);
    }

    @Test
    void estadoFinalNaoMuda() { // RN06
        Consulta consulta = new Consulta(medico, paciente, amanha, "Check-up");
        consulta.cancelar();
        assertFalse(consulta.isAtiva()); // RN07: cancelada não ocupa horário
        assertThrows(RegraNegocioException.class, consulta::confirmar);
        assertThrows(RegraNegocioException.class, consulta::cancelar);
        assertThrows(RegraNegocioException.class, consulta::finalizar);
    }

    @Test
    void segundosSaoIgnoradosNoHorario() { // RN04: 14:00 e 14:00:37 são o mesmo horário
        Consulta consulta = new Consulta(medico, paciente, amanha.withSecond(37), "Check-up");
        assertEquals(0, consulta.getDataHora().getSecond());
    }
}
