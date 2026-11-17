package br.edu.avaliacao.sacm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.edu.avaliacao.sacm.enums.StatusPessoa;

/** Paciente da clínica. Herda nome, CPF, contato e status de Pessoa. */
public class Paciente extends Pessoa {

    /** Prefixo do número de prontuário gerado a partir do id (ex.: PRT-000007). */
    public static final String PREFIXO_PRONTUARIO = "PRT-";

    private String numeroProntuario;
    private String convenio;
    private String contatoEmergencia;
    private String telefoneEmergencia;

    /** Cadastro novo: o prontuário é gerado depois que o banco devolve o id. */
    public Paciente(String nome, String cpf, LocalDate dataNascimento, String email, String telefone,
                    String cidade, String estado, String convenio,
                    String contatoEmergencia, String telefoneEmergencia) {
        super(nome, cpf, dataNascimento, email, telefone, cidade, estado);
        atualizarConvenio(convenio);
        atualizarContatoEmergencia(contatoEmergencia, telefoneEmergencia);
    }

    /** Usado pelo PacienteRepository para remontar o paciente lido do banco. */
    public Paciente(Long id, String nome, String cpf, LocalDate dataNascimento, String email,
                    String telefone, String cidade, String estado, LocalDateTime dataCadastro,
                    StatusPessoa status, String numeroProntuario, String convenio,
                    String contatoEmergencia, String telefoneEmergencia) {
        super(id, nome, cpf, dataNascimento, email, telefone, cidade, estado, dataCadastro, status);
        this.numeroProntuario = numeroProntuario;
        atualizarConvenio(convenio);
        atualizarContatoEmergencia(contatoEmergencia, telefoneEmergencia);
    }

    // ------------------------------------------------------------------ operações do UML

    /** Convênio vazio significa atendimento particular. */
    public final void atualizarConvenio(String convenio) {
        this.convenio = textoOuNulo(convenio);
    }

    public final void atualizarContatoEmergencia(String nome, String telefone) {
        this.contatoEmergencia = textoOuNulo(nome);
        this.telefoneEmergencia = textoOuNulo(telefone);
    }

    /** RN02: só agenda quem está ATIVO. */
    public boolean podeAgendar() {
        return isAtivo();
    }

    @Override
    public String getDescricao() {
        return String.format("[Paciente #%d] %s | Prontuário %s | %d anos | Convênio: %s | %s",
                getId(), getNome(), numeroProntuario, getIdade(),
                convenio == null ? "Particular" : convenio, getStatus());
    }

    // ------------------------------------------------------------------ prontuário

    /** Gera o prontuário uma única vez, a partir do id definido pelo banco. */
    public void gerarNumeroProntuario() {
        if (getId() == null) {
            throw new IllegalStateException("O paciente precisa ter id antes de gerar o prontuário.");
        }
        if (numeroProntuario == null) {
            numeroProntuario = PREFIXO_PRONTUARIO + String.format("%06d", getId());
        }
    }

    // ------------------------------------------------------------------ getters

    public String getNumeroProntuario() { return numeroProntuario; }
    public String getConvenio() { return convenio; }
    public String getContatoEmergencia() { return contatoEmergencia; }
    public String getTelefoneEmergencia() { return telefoneEmergencia; }
}
