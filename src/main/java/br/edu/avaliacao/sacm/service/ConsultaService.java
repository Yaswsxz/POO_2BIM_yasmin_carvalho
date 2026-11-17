package br.edu.avaliacao.sacm.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import br.edu.avaliacao.sacm.enums.DisponibilidadeMedico;
import br.edu.avaliacao.sacm.exception.ConflitoHorarioException;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;
import br.edu.avaliacao.sacm.interfaces.Agendavel;
import br.edu.avaliacao.sacm.model.Consulta;
import br.edu.avaliacao.sacm.model.Medico;
import br.edu.avaliacao.sacm.model.Paciente;
import br.edu.avaliacao.sacm.repository.ConsultaRepository;
import br.edu.avaliacao.sacm.repository.MedicoRepository;
import br.edu.avaliacao.sacm.repository.PacienteRepository;

/**
 * Regras de negócio da clínica. O console só conversa com esta classe;
 * esta classe é quem decide o que pode e chama os repositórios.
 */
@Service
public class ConsultaService implements Agendavel {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final ConsultaRepository consultaRepository;

    /** Injeção pelo construtor: o Spring entrega os três repositórios prontos. */
    public ConsultaService(MedicoRepository medicoRepository, PacienteRepository pacienteRepository,
                           ConsultaRepository consultaRepository) {
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.consultaRepository = consultaRepository;
    }

    // =================================================================== cadastros (RN01)

    public Medico cadastrarMedico(Medico medico) {
        if (medicoRepository.existeCpf(medico.getCpf())) {
            throw new RegraNegocioException("Já existe uma pessoa cadastrada com o CPF " + medico.getCpf() + ".");
        }
        if (medicoRepository.existeCrm(medico.getCrm())) {
            throw new RegraNegocioException("Já existe um médico com o CRM " + medico.getCrm() + ".");
        }
        try {
            return medicoRepository.salvar(medico);
        } catch (DuplicateKeyException e) {
            // Segunda barreira: o UNIQUE do banco pegou um duplicado que passou pela checagem acima.
            throw new RegraNegocioException("CPF ou CRM já cadastrado.", e);
        }
    }

    public Paciente cadastrarPaciente(Paciente paciente) {
        if (pacienteRepository.existeCpf(paciente.getCpf())) {
            throw new RegraNegocioException("Já existe uma pessoa cadastrada com o CPF " + paciente.getCpf() + ".");
        }
        try {
            return pacienteRepository.salvar(paciente);
        } catch (DuplicateKeyException e) {
            throw new RegraNegocioException("CPF já cadastrado.", e);
        }
    }

    public List<Medico> listarMedicos() {
        return medicoRepository.listarTodos();
    }

    public List<Paciente> listarPacientes() {
        return pacienteRepository.listarTodos();
    }

    public Medico alterarDisponibilidadeMedico(Long medicoId, DisponibilidadeMedico disponibilidade) {
        Medico medico = buscarMedico(medicoId);
        medico.alterarDisponibilidade(disponibilidade);
        medicoRepository.atualizarDisponibilidade(medico);
        return medico;
    }

    public Medico alterarStatusMedico(Long medicoId, boolean ativo) {
        Medico medico = buscarMedico(medicoId);
        if (ativo) {
            medico.ativar();
        } else {
            medico.desativar();
        }
        medicoRepository.atualizarStatus(medico);
        return medico;
    }

    public Paciente alterarStatusPaciente(Long pacienteId, boolean ativo) {
        Paciente paciente = buscarPaciente(pacienteId);
        if (ativo) {
            paciente.ativar();
        } else {
            paciente.desativar();
        }
        pacienteRepository.atualizarStatus(paciente);
        return paciente;
    }

    // =================================================================== agendamento

    /**
     * Ordem das checagens: dados da consulta (RN05) -> existência e status (RN02)
     * -> disponibilidade (RN03) -> conflito de horário (RN04) -> grava (RN06: AGENDADA).
     */
    @Override
    public Consulta agendarConsulta(Long medicoId, Long pacienteId, LocalDateTime dataHora, String motivo) {
        if (dataHora == null) {
            throw new RegraNegocioException("Data/hora da consulta é obrigatória.");
        }
        LocalDateTime horario = dataHora.truncatedTo(ChronoUnit.MINUTES);
        if (!horario.isAfter(LocalDateTime.now())) {
            throw new RegraNegocioException("A data/hora da consulta deve ser futura.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("O motivo da consulta não pode estar vazio.");
        }

        Medico medico = buscarMedico(medicoId);
        if (!medico.isAtivo()) {
            throw new RegraNegocioException("O médico " + medico.getNome() + " está INATIVO e não pode receber consultas.");
        }
        if (!medico.isDisponivel()) {
            throw new RegraNegocioException("O médico " + medico.getNome() + " está INDISPONIVEL para novos agendamentos.");
        }

        Paciente paciente = buscarPaciente(pacienteId);
        if (!paciente.podeAgendar()) {
            throw new RegraNegocioException("O paciente " + paciente.getNome() + " está INATIVO e não pode agendar.");
        }

        verificarConflitoHorario(medicoId, pacienteId, horario);

        Consulta consulta = new Consulta(medico, paciente, horario, motivo);
        try {
            return consultaRepository.salvar(consulta);
        } catch (DuplicateKeyException e) {
            // Duas pessoas agendaram o mesmo horário quase ao mesmo tempo: o UNIQUE do banco barrou.
            throw new ConflitoHorarioException(
                    "Horário " + horario.format(FORMATO) + " acabou de ser ocupado. Escolha outro.", e);
        }
    }

    /** RN04: lança ConflitoHorarioException se médico OU paciente já estiver ocupado. */
    public void verificarConflitoHorario(Long medicoId, Long pacienteId, LocalDateTime dataHora) {
        LocalDateTime horario = dataHora.truncatedTo(ChronoUnit.MINUTES);
        if (consultaRepository.existeConsultaAtivaDoMedico(medicoId, horario)) {
            throw new ConflitoHorarioException(
                    "Conflito: o médico #" + medicoId + " já tem consulta em " + horario.format(FORMATO) + ".");
        }
        if (consultaRepository.existeConsultaAtivaDoPaciente(pacienteId, horario)) {
            throw new ConflitoHorarioException(
                    "Conflito: o paciente #" + pacienteId + " já tem consulta em " + horario.format(FORMATO) + ".");
        }
    }

    // =================================================================== mudanças de status (RN06/RN07)

    @Override
    public void cancelarConsulta(Long consultaId) {
        Consulta consulta = buscarConsulta(consultaId);
        consulta.cancelar();                         // a própria Consulta valida a transição
        consultaRepository.atualizarStatus(consulta); // e o banco registra (RN07)
    }

    public void confirmarConsulta(Long consultaId) {
        Consulta consulta = buscarConsulta(consultaId);
        consulta.confirmar();
        consultaRepository.atualizarStatus(consulta);
    }

    public void finalizarConsulta(Long consultaId) {
        Consulta consulta = buscarConsulta(consultaId);
        consulta.finalizar();
        consultaRepository.atualizarStatus(consulta);
    }

    // =================================================================== listagens

    public List<Consulta> listarConsultas() {
        return consultaRepository.listarTodas();
    }

    public List<Consulta> listarConsultasPorMedico(Long medicoId) {
        buscarMedico(medicoId);
        return consultaRepository.listarPorMedico(medicoId);
    }

    public List<Consulta> listarConsultasPorPaciente(Long pacienteId) {
        buscarPaciente(pacienteId);
        return consultaRepository.listarPorPaciente(pacienteId);
    }

    // =================================================================== apoio

    private Medico buscarMedico(Long id) {
        return medicoRepository.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Médico #" + id + " não encontrado."));
    }

    private Paciente buscarPaciente(Long id) {
        return pacienteRepository.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Paciente #" + id + " não encontrado."));
    }

    private Consulta buscarConsulta(Long id) {
        return consultaRepository.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Consulta #" + id + " não encontrada."));
    }
}
