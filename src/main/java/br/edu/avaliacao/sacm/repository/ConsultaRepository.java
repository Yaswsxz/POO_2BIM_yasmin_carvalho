package br.edu.avaliacao.sacm.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import br.edu.avaliacao.sacm.enums.StatusConsulta;
import br.edu.avaliacao.sacm.model.Consulta;
import br.edu.avaliacao.sacm.model.Medico;
import br.edu.avaliacao.sacm.model.Paciente;

/** Acesso ao banco para consultas. */
@Repository
public class ConsultaRepository {

    private static final String SELECT_BASE = """
            SELECT id, medico_id, paciente_id, data_hora, motivo, status, data_agendamento, observacao
            FROM consulta
            """;

    /** Só estes status ocupam horário (RN04); CANCELADA e FINALIZADA não bloqueiam (RN07). */
    private static final String FILTRO_STATUS_ATIVO = " AND status IN ('AGENDADA', 'CONFIRMADA')";

    private final JdbcTemplate jdbcTemplate;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;

    /**
     * Cada linha traz só os ids de médico e paciente; os objetos completos são
     * buscados nos outros repositórios. É simples de entender, mas faz mais
     * consultas ao banco (limitação aceitável para o volume de uma clínica).
     */
    private final RowMapper<Consulta> consultaMapper;

    public ConsultaRepository(JdbcTemplate jdbcTemplate, MedicoRepository medicoRepository,
                              PacienteRepository pacienteRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.consultaMapper = (rs, numeroLinha) -> {
            Medico medico = this.medicoRepository.buscarPorId(rs.getLong("medico_id"))
                    .orElseThrow(() -> new IllegalStateException("Consulta aponta para médico inexistente."));
            Paciente paciente = this.pacienteRepository.buscarPorId(rs.getLong("paciente_id"))
                    .orElseThrow(() -> new IllegalStateException("Consulta aponta para paciente inexistente."));
            return Consulta.restaurar(
                    rs.getLong("id"),
                    medico,
                    paciente,
                    rs.getTimestamp("data_hora").toLocalDateTime(),
                    rs.getString("motivo"),
                    StatusConsulta.valueOf(rs.getString("status")),
                    rs.getTimestamp("data_agendamento").toLocalDateTime(),
                    rs.getString("observacao"));
        };
    }

    public Consulta salvar(Consulta consulta) {
        KeyHolder chave = new GeneratedKeyHolder();
        jdbcTemplate.update(conexao -> {
            PreparedStatement ps = conexao.prepareStatement("""
                    INSERT INTO consulta (medico_id, paciente_id, data_hora, motivo, status,
                                          data_agendamento, observacao)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, consulta.getMedico().getId());
            ps.setLong(2, consulta.getPaciente().getId());
            ps.setTimestamp(3, Timestamp.valueOf(consulta.getDataHora()));
            ps.setString(4, consulta.getMotivo());
            ps.setString(5, consulta.getStatus().name());
            ps.setTimestamp(6, Timestamp.valueOf(consulta.getDataAgendamento()));
            ps.setString(7, consulta.getObservacao());
            return ps;
        }, chave);
        consulta.definirId(chave.getKey().longValue());
        return consulta;
    }

    /** RN07: confirmação, cancelamento e finalização são gravados no banco. */
    public void atualizarStatus(Consulta consulta) {
        jdbcTemplate.update("UPDATE consulta SET status = ?, observacao = ? WHERE id = ?",
                consulta.getStatus().name(), consulta.getObservacao(), consulta.getId());
    }

    public Optional<Consulta> buscarPorId(Long id) {
        List<Consulta> resultado = jdbcTemplate.query(SELECT_BASE + " WHERE id = ?", consultaMapper, id);
        return resultado.stream().findFirst();
    }

    public List<Consulta> listarTodas() {
        return jdbcTemplate.query(SELECT_BASE + " ORDER BY data_hora, id", consultaMapper);
    }

    public List<Consulta> listarPorMedico(Long medicoId) {
        return jdbcTemplate.query(SELECT_BASE + " WHERE medico_id = ? ORDER BY data_hora, id",
                consultaMapper, medicoId);
    }

    public List<Consulta> listarPorPaciente(Long pacienteId) {
        return jdbcTemplate.query(SELECT_BASE + " WHERE paciente_id = ? ORDER BY data_hora, id",
                consultaMapper, pacienteId);
    }

    public boolean existeConsultaAtivaDoMedico(Long medicoId, LocalDateTime dataHora) {
        return contar("SELECT COUNT(*) FROM consulta WHERE medico_id = ? AND data_hora = ?"
                + FILTRO_STATUS_ATIVO, medicoId, dataHora);
    }

    public boolean existeConsultaAtivaDoPaciente(Long pacienteId, LocalDateTime dataHora) {
        return contar("SELECT COUNT(*) FROM consulta WHERE paciente_id = ? AND data_hora = ?"
                + FILTRO_STATUS_ATIVO, pacienteId, dataHora);
    }

    private boolean contar(String sql, Long id, LocalDateTime dataHora) {
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, id, Timestamp.valueOf(dataHora));
        return total != null && total > 0;
    }
}
