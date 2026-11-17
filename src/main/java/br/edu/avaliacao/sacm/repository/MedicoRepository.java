package br.edu.avaliacao.sacm.repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.edu.avaliacao.sacm.enums.DisponibilidadeMedico;
import br.edu.avaliacao.sacm.enums.Especialidade;
import br.edu.avaliacao.sacm.enums.StatusPessoa;
import br.edu.avaliacao.sacm.model.Medico;
import br.edu.avaliacao.sacm.model.Pessoa;

/** Acesso ao banco para médicos (tabelas pessoa + medico). */
@Repository
public class MedicoRepository {

    private static final String SELECT_BASE = """
            SELECT p.id, p.nome, p.cpf, p.data_nascimento, p.email, p.telefone, p.cidade, p.estado,
                   p.data_cadastro, p.status,
                   m.crm, m.especialidade, m.disponibilidade, m.valor_consulta
            FROM medico m
            JOIN pessoa p ON p.id = m.id
            """;

    private final JdbcTemplate jdbcTemplate;

    /** RowMapper: transforma cada linha do ResultSet em um objeto Medico. */
    private final RowMapper<Medico> medicoMapper = (rs, numeroLinha) -> new Medico(
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
            rs.getString("crm"),
            Especialidade.valueOf(rs.getString("especialidade")),
            DisponibilidadeMedico.valueOf(rs.getString("disponibilidade")),
            rs.getBigDecimal("valor_consulta"));

    public MedicoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Grava em pessoa e depois em medico. @Transactional garante que, se o segundo
     * INSERT falhar (ex.: CRM repetido), o primeiro é desfeito (rollback).
     */
    @Transactional
    public Medico salvar(Medico medico) {
        KeyHolder chave = new GeneratedKeyHolder();
        jdbcTemplate.update(conexao -> {
            PreparedStatement ps = conexao.prepareStatement("""
                    INSERT INTO pessoa (tipo, nome, cpf, data_nascimento, email, telefone,
                                        cidade, estado, data_cadastro, status)
                    VALUES ('MEDICO', ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            preencherPessoa(ps, medico);
            return ps;
        }, chave);
        medico.definirId(chave.getKey().longValue());

        jdbcTemplate.update("""
                INSERT INTO medico (id, crm, especialidade, disponibilidade, valor_consulta)
                VALUES (?, ?, ?, ?, ?)
                """,
                medico.getId(), medico.getCrm(), medico.getEspecialidade().name(),
                medico.getDisponibilidade().name(), medico.getValorConsulta());
        return medico;
    }

    public Optional<Medico> buscarPorId(Long id) {
        List<Medico> resultado = jdbcTemplate.query(SELECT_BASE + " WHERE p.id = ?", medicoMapper, id);
        return resultado.stream().findFirst();
    }

    public List<Medico> listarTodos() {
        return jdbcTemplate.query(SELECT_BASE + " ORDER BY p.nome", medicoMapper);
    }

    public boolean existeCrm(String crm) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM medico WHERE crm = ?", Integer.class, crm);
        return total != null && total > 0;
    }

    /** RN01: o CPF é único na tabela pessoa, ou seja, entre médicos e pacientes. */
    public boolean existeCpf(String cpf) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pessoa WHERE cpf = ?", Integer.class, cpf);
        return total != null && total > 0;
    }

    public void atualizarDisponibilidade(Medico medico) {
        jdbcTemplate.update("UPDATE medico SET disponibilidade = ? WHERE id = ?",
                medico.getDisponibilidade().name(), medico.getId());
    }

    public void atualizarStatus(Medico medico) {
        jdbcTemplate.update("UPDATE pessoa SET status = ? WHERE id = ?",
                medico.getStatus().name(), medico.getId());
    }

    /** Parâmetros comuns do INSERT em pessoa (reaproveitado pelo PacienteRepository). */
    static void preencherPessoa(PreparedStatement ps, Pessoa pessoa)
            throws SQLException {
        ps.setString(1, pessoa.getNome());
        ps.setString(2, pessoa.getCpf());
        ps.setDate(3, Date.valueOf(pessoa.getDataNascimento()));
        ps.setString(4, pessoa.getEmail());
        ps.setString(5, pessoa.getTelefone());
        ps.setString(6, pessoa.getCidade());
        ps.setString(7, pessoa.getEstado());
        ps.setTimestamp(8, Timestamp.valueOf(pessoa.getDataCadastro()));
        ps.setString(9, pessoa.getStatus().name());
    }
}
