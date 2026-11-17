package br.edu.avaliacao.sacm.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.edu.avaliacao.sacm.enums.StatusPessoa;
import br.edu.avaliacao.sacm.model.Paciente;

/** Acesso ao banco para pacientes (tabelas pessoa + paciente). */
@Repository
public class PacienteRepository {

    private static final String SELECT_BASE = """
            SELECT p.id, p.nome, p.cpf, p.data_nascimento, p.email, p.telefone, p.cidade, p.estado,
                   p.data_cadastro, p.status,
                   pa.numero_prontuario, pa.convenio, pa.contato_emergencia, pa.telefone_emergencia
            FROM paciente pa
            JOIN pessoa p ON p.id = pa.id
            """;

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Paciente> pacienteMapper = (rs, numeroLinha) -> new Paciente(
            rs.getLong("id"),
            rs.getString("nome"),
            rs.getString("cpf"),
            rs.getDate("data_nascimento").toLocalDate(),
            rs.getString("email"),
            rs.getString("telefone"),
            rs.getString("cidade"),
            rs.getString("estado"),
            rs.getTimestamp("data_cadastro").toLocalDateTime(),
            StatusPessoa.valueOf(rs.getString("status")),
            rs.getString("numero_prontuario"),
            rs.getString("convenio"),
            rs.getString("contato_emergencia"),
            rs.getString("telefone_emergencia"));

    public PacienteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Grava em pessoa, gera o prontuário a partir do id e grava em paciente (tudo ou nada). */
    @Transactional
    public Paciente salvar(Paciente paciente) {
        KeyHolder chave = new GeneratedKeyHolder();
        jdbcTemplate.update(conexao -> {
            PreparedStatement ps = conexao.prepareStatement("""
                    INSERT INTO pessoa (tipo, nome, cpf, data_nascimento, email, telefone,
                                        cidade, estado, data_cadastro, status)
                    VALUES ('PACIENTE', ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            MedicoRepository.preencherPessoa(ps, paciente);
            return ps;
        }, chave);
        paciente.definirId(chave.getKey().longValue());
        paciente.gerarNumeroProntuario();

        jdbcTemplate.update("""
                INSERT INTO paciente (id, numero_prontuario, convenio, contato_emergencia, telefone_emergencia)
                VALUES (?, ?, ?, ?, ?)
                """,
                paciente.getId(), paciente.getNumeroProntuario(), paciente.getConvenio(),
                paciente.getContatoEmergencia(), paciente.getTelefoneEmergencia());
        return paciente;
    }

    public Optional<Paciente> buscarPorId(Long id) {
        List<Paciente> resultado = jdbcTemplate.query(SELECT_BASE + " WHERE p.id = ?", pacienteMapper, id);
        return resultado.stream().findFirst();
    }

    public List<Paciente> listarTodos() {
        return jdbcTemplate.query(SELECT_BASE + " ORDER BY p.nome", pacienteMapper);
    }

    public boolean existeCpf(String cpf) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pessoa WHERE cpf = ?", Integer.class, cpf);
        return total != null && total > 0;
    }

    public void atualizarStatus(Paciente paciente) {
        jdbcTemplate.update("UPDATE pessoa SET status = ? WHERE id = ?",
                paciente.getStatus().name(), paciente.getId());
    }
}
