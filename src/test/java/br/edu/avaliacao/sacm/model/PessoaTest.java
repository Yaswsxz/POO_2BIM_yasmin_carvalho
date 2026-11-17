package br.edu.avaliacao.sacm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.edu.avaliacao.sacm.enums.DisponibilidadeMedico;
import br.edu.avaliacao.sacm.enums.Especialidade;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;

class PessoaTest {

    private Medico novoMedico() {
        return new Medico("Ana Souza", "529.982.247-25", LocalDate.of(1985, 3, 10), "ana@clinica.com",
                "43999990000", "Londrina", "pr", "12345-pr", Especialidade.CARDIOLOGIA, new BigDecimal("300"));
    }

    @Test
    void cpfEhGuardadoSoComDigitos() {
        assertEquals("52998224725", novoMedico().getCpf());
    }

    @Test
    void cpfComTamanhoErradoEhRecusado() {
        assertThrows(RegraNegocioException.class, () -> Pessoa.normalizarCpf("123"));
    }

    @Test
    void medicoComecaAtivoEDisponivel() { // RN02/RN03
        Medico medico = novoMedico();
        assertTrue(medico.isAtivo());
        assertTrue(medico.podeAtender());
    }

    @Test
    void medicoIndisponivelOuInativoNaoAtende() { // RN02/RN03
        Medico medico = novoMedico();
        medico.alterarDisponibilidade(DisponibilidadeMedico.INDISPONIVEL);
        assertFalse(medico.podeAtender());
        medico.alterarDisponibilidade(DisponibilidadeMedico.DISPONIVEL);
        medico.desativar();
        assertFalse(medico.podeAtender());
    }

    @Test
    void crmEhObrigatorio() { // RN01
        assertThrows(RegraNegocioException.class, () -> new Medico("Ana", "52998224725",
                LocalDate.of(1985, 3, 10), null, null, null, null, " ", Especialidade.PEDIATRIA, BigDecimal.TEN));
    }

    @Test
    void idSoPodeSerDefinidoUmaVez() {
        Medico medico = novoMedico();
        medico.definirId(1L);
        assertThrows(IllegalStateException.class, () -> medico.definirId(2L));
    }

    @Test
    void polimorfismoGetDescricao() {
        Pessoa pessoa = new Paciente("Bruno Lima", "11144477735", LocalDate.of(2000, 7, 1),
                null, null, null, null, null, null, null);
        assertTrue(pessoa.getDescricao().startsWith("[Paciente"));
        Pessoa outra = novoMedico();
        assertTrue(outra.getDescricao().startsWith("[Médico"));
    }
}
