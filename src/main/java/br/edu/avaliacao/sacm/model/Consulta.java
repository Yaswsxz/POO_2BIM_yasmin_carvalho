package br.edu.avaliacao.sacm.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import br.edu.avaliacao.sacm.enums.StatusConsulta;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;

/** Consulta entre exatamente um médico e um paciente. Controla o próprio ciclo de vida (RN06). */
public class Consulta {

    /** Mesmo tamanho da coluna motivo VARCHAR(255) do banco. */
    public static final int TAMANHO_MAXIMO_MOTIVO = 255;

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Long id;
    private final Medico medico;
    private final Paciente paciente;
    private final LocalDateTime dataHora;
    private final String motivo;
    private StatusConsulta status;
    private final LocalDateTime dataAgendamento;
    private String observacao;

    /** Nova consulta: RN05 (data futura e motivo) e RN06 (começa AGENDADA). */
    public Consulta(Medico medico, Paciente paciente, LocalDateTime dataHora, String motivo) {
        if (medico == null || paciente == null) {
            throw new RegraNegocioException("A consulta precisa de um médico e de um paciente.");
        }
        if (dataHora == null) {
            throw new RegraNegocioException("Data/hora da consulta é obrigatória.");
        }
        LocalDateTime agora = LocalDateTime.now();
        if (!dataHora.isAfter(agora)) {
            throw new RegraNegocioException("A data/hora da consulta deve ser futura.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("O motivo da consulta não pode estar vazio.");
        }
        if (motivo.trim().length() > TAMANHO_MAXIMO_MOTIVO) {
            throw new RegraNegocioException("O motivo pode ter no máximo " + TAMANHO_MAXIMO_MOTIVO + " caracteres.");
        }
        this.medico = medico;
        this.paciente = paciente;
        // Segundos são descartados para que 14:00 e 14:00:37 sejam o MESMO horário (RN04).
        this.dataHora = dataHora.truncatedTo(ChronoUnit.MINUTES);
        this.motivo = motivo.trim();
        this.status = StatusConsulta.AGENDADA;
        this.dataAgendamento = agora;
    }

    /**
     * Remonta uma consulta lida do banco. Não revalida a data futura,
     * porque consultas antigas (finalizadas) também precisam ser listadas.
     */
    public static Consulta restaurar(Long id, Medico medico, Paciente paciente, LocalDateTime dataHora,
                                     String motivo, StatusConsulta status,
                                     LocalDateTime dataAgendamento, String observacao) {
        return new Consulta(id, medico, paciente, dataHora, motivo, status, dataAgendamento, observacao);
    }

    private Consulta(Long id, Medico medico, Paciente paciente, LocalDateTime dataHora, String motivo,
                     StatusConsulta status, LocalDateTime dataAgendamento, String observacao) {
        this.id = id;
        this.medico = medico;
        this.paciente = paciente;
        this.dataHora = dataHora;
        this.motivo = motivo;
        this.status = status;
        this.dataAgendamento = dataAgendamento;
        this.observacao = observacao;
    }

    // ------------------------------------------------------------------ transições (RN06)

    public void confirmar() {
        exigirStatus(StatusConsulta.AGENDADA, "confirmar");
        status = StatusConsulta.CONFIRMADA;
    }

    public void cancelar() {
        if (!status.ocupaHorario()) {
            throw new RegraNegocioException(
                    "Não é possível cancelar: a consulta #" + id + " já está " + status + " (estado final).");
        }
        status = StatusConsulta.CANCELADA;
    }

    public void finalizar() {
        exigirStatus(StatusConsulta.CONFIRMADA, "finalizar");
        status = StatusConsulta.FINALIZADA;
    }

    /** AGENDADA ou CONFIRMADA: ainda ocupa o horário. */
    public boolean isAtiva() {
        return status.ocupaHorario();
    }

    public String getResumo() {
        return String.format("Consulta #%d | %s | Dr(a). %s (%s) | Paciente: %s | Motivo: %s | %s%s",
                id, dataHora.format(FORMATO), medico.getNome(), medico.getEspecialidade().getDescricao(),
                paciente.getNome(), motivo, status,
                observacao == null ? "" : " | Obs.: " + observacao);
    }

    public void registrarObservacao(String texto) {
        this.observacao = (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    private void exigirStatus(StatusConsulta esperado, String acao) {
        if (status != esperado) {
            throw new RegraNegocioException(String.format(
                    "Não é possível %s a consulta #%d: ela está %s e precisa estar %s.",
                    acao, id, status, esperado));
        }
    }

    /** Mesmo cuidado de Pessoa: o id só é atribuído uma vez, após o INSERT. */
    public void definirId(Long idGerado) {
        if (this.id != null) {
            throw new IllegalStateException("O id desta consulta já foi definido: " + this.id);
        }
        this.id = idGerado;
    }

    // ------------------------------------------------------------------ getters

    public Long getId() { return id; }
    public Medico getMedico() { return medico; }
    public Paciente getPaciente() { return paciente; }
    public LocalDateTime getDataHora() { return dataHora; }
    public String getMotivo() { return motivo; }
    public StatusConsulta getStatus() { return status; }
    public LocalDateTime getDataAgendamento() { return dataAgendamento; }
    public String getObservacao() { return observacao; }

    @Override
    public String toString() {
        return getResumo();
    }
}
