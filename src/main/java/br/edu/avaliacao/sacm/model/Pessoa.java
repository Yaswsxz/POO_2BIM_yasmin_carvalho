package br.edu.avaliacao.sacm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import br.edu.avaliacao.sacm.enums.StatusPessoa;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;

/**
 * Dados e comportamentos comuns a médicos e pacientes.
 * É abstrata porque ninguém é "só pessoa" na clínica: sempre é Medico ou Paciente.
 */
public abstract class Pessoa {

    /** Um CPF tem sempre 11 dígitos; usado na validação e no CHAR(11) do banco. */
    public static final int TAMANHO_CPF = 11;

    private Long id;
    private String nome;
    private final String cpf;              // não muda depois do cadastro
    private final LocalDate dataNascimento;
    private String email;
    private String telefone;
    private String cidade;
    private String estado;
    private final LocalDateTime dataCadastro;
    private StatusPessoa status;

    /** Construtor para um cadastro novo: começa ATIVO e sem id (o banco gera). */
    protected Pessoa(String nome, String cpf, LocalDate dataNascimento, String email,
                     String telefone, String cidade, String estado) {
        this(null, nome, cpf, dataNascimento, email, telefone, cidade, estado,
                LocalDateTime.now(), StatusPessoa.ATIVO);
    }

    /** Construtor usado pelos repositórios para remontar o objeto lido do banco. */
    protected Pessoa(Long id, String nome, String cpf, LocalDate dataNascimento, String email,
                     String telefone, String cidade, String estado,
                     LocalDateTime dataCadastro, StatusPessoa status) {
        this.id = id;
        this.nome = exigirTexto(nome, "Nome");
        this.cpf = normalizarCpf(cpf);
        this.dataNascimento = validarNascimento(dataNascimento);
        atualizarContato(email, telefone);
        this.cidade = textoOuNulo(cidade);
        this.estado = validarEstado(estado);
        this.dataCadastro = dataCadastro;
        this.status = status;
    }

    // ------------------------------------------------------------------ operações do UML

    public int getIdade() {
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    public final void atualizarContato(String email, String telefone) {
        String emailLimpo = textoOuNulo(email);
        if (emailLimpo != null && !emailLimpo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new RegraNegocioException("E-mail inválido: " + email);
        }
        this.email = emailLimpo;
        this.telefone = textoOuNulo(telefone);
    }

    public void ativar() {
        this.status = StatusPessoa.ATIVO;
    }

    public void desativar() {
        this.status = StatusPessoa.INATIVO;
    }

    public boolean isAtivo() {
        return status == StatusPessoa.ATIVO;
    }

    /** Cada subclasse se descreve de um jeito (polimorfismo por sobrescrita). */
    public abstract String getDescricao();

    // ------------------------------------------------------------------ identificador

    /**
     * O id só pode ser atribuído uma vez, logo depois do INSERT.
     * Por isso não existe setId "livre" (invariante pedida no enunciado).
     */
    public void definirId(Long idGerado) {
        if (this.id != null) {
            throw new IllegalStateException("O id desta pessoa já foi definido: " + this.id);
        }
        this.id = idGerado;
    }

    // ------------------------------------------------------------------ validações

    protected static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException(campo + " é obrigatório.");
        }
        return valor.trim();
    }

    protected static String textoOuNulo(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    /** Aceita "123.456.789-09" ou "12345678909" e guarda só os dígitos. */
    public static String normalizarCpf(String cpf) {
        String digitos = exigirTexto(cpf, "CPF").replaceAll("\\D", "");
        if (digitos.length() != TAMANHO_CPF) {
            throw new RegraNegocioException("CPF deve ter " + TAMANHO_CPF + " dígitos.");
        }
        return digitos;
    }

    private static LocalDate validarNascimento(LocalDate data) {
        if (data == null) {
            throw new RegraNegocioException("Data de nascimento é obrigatória.");
        }
        if (data.isAfter(LocalDate.now())) {
            throw new RegraNegocioException("Data de nascimento não pode estar no futuro.");
        }
        return data;
    }

    private static String validarEstado(String uf) {
        String limpo = textoOuNulo(uf);
        if (limpo == null) {
            return null;
        }
        if (!limpo.matches("[A-Za-z]{2}")) {
            throw new RegraNegocioException("Estado deve ser a sigla com 2 letras (ex.: PR).");
        }
        return limpo.toUpperCase();
    }

    // ------------------------------------------------------------------ getters

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public String getCidade() { return cidade; }
    public String getEstado() { return estado; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public StatusPessoa getStatus() { return status; }

    /** CPF mascarado para exibir no console sem expor o número inteiro. */
    public String getCpfMascarado() {
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }

    @Override
    public String toString() {
        return getDescricao();
    }
}
