package br.edu.avaliacao.sacm.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import br.edu.avaliacao.sacm.enums.DisponibilidadeMedico;
import br.edu.avaliacao.sacm.enums.Especialidade;
import br.edu.avaliacao.sacm.enums.StatusPessoa;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;

/** Médico da clínica. Herda nome, CPF, contato e status de Pessoa. */
public class Medico extends Pessoa {

    private final String crm;
    private Especialidade especialidade;
    private DisponibilidadeMedico disponibilidade;
    private BigDecimal valorConsulta;

    /** Cadastro novo: médico começa ATIVO e DISPONIVEL. */
    public Medico(String nome, String cpf, LocalDate dataNascimento, String email, String telefone,
                  String cidade, String estado, String crm, Especialidade especialidade,
                  BigDecimal valorConsulta) {
        super(nome, cpf, dataNascimento, email, telefone, cidade, estado);
        this.crm = validarCrm(crm);
        this.especialidade = validarEspecialidade(especialidade);
        this.valorConsulta = validarValor(valorConsulta);
        this.disponibilidade = DisponibilidadeMedico.DISPONIVEL;
    }

    /** Usado pelo MedicoRepository para remontar o médico lido do banco. */
    public Medico(Long id, String nome, String cpf, LocalDate dataNascimento, String email,
                  String telefone, String cidade, String estado, LocalDateTime dataCadastro,
                  StatusPessoa status, String crm, Especialidade especialidade,
                  DisponibilidadeMedico disponibilidade, BigDecimal valorConsulta) {
        super(id, nome, cpf, dataNascimento, email, telefone, cidade, estado, dataCadastro, status);
        this.crm = validarCrm(crm);
        this.especialidade = validarEspecialidade(especialidade);
        this.disponibilidade = disponibilidade;
        this.valorConsulta = validarValor(valorConsulta);
    }

    // ------------------------------------------------------------------ operações do UML

    public void alterarDisponibilidade(DisponibilidadeMedico valor) {
        if (valor == null) {
            throw new RegraNegocioException("Informe a disponibilidade do médico.");
        }
        this.disponibilidade = valor;
    }

    public boolean isDisponivel() {
        return disponibilidade == DisponibilidadeMedico.DISPONIVEL;
    }

    /** RN02 + RN03: só atende quem está ATIVO e DISPONIVEL. */
    public boolean podeAtender() {
        return isAtivo() && isDisponivel();
    }

    @Override
    public String getDescricao() {
        return String.format(Locale.of("pt", "BR"),
                "[Médico #%d] Dr(a). %s | CRM %s | %s | R$ %,.2f | %s | %s",
                getId(), getNome(), crm, especialidade.getDescricao(),
                valorConsulta, getStatus(), disponibilidade);
    }

    // ------------------------------------------------------------------ validações

    private static String validarCrm(String crm) {
        // RN01: CRM obrigatório (a unicidade é conferida no serviço e no UNIQUE do banco).
        return exigirTexto(crm, "CRM").toUpperCase();
    }

    private static Especialidade validarEspecialidade(Especialidade especialidade) {
        if (especialidade == null) {
            throw new RegraNegocioException("Especialidade é obrigatória.");
        }
        return especialidade;
    }

    private static BigDecimal validarValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new RegraNegocioException("Valor da consulta deve ser maior que zero.");
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ getters

    public String getCrm() { return crm; }
    public Especialidade getEspecialidade() { return especialidade; }
    public DisponibilidadeMedico getDisponibilidade() { return disponibilidade; }
    public BigDecimal getValorConsulta() { return valorConsulta; }
}
