package br.edu.avaliacao.sacm.console;

import java.io.Console;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import br.edu.avaliacao.sacm.enums.DisponibilidadeMedico;
import br.edu.avaliacao.sacm.enums.Especialidade;
import br.edu.avaliacao.sacm.exception.ConflitoHorarioException;
import br.edu.avaliacao.sacm.exception.RegraNegocioException;
import br.edu.avaliacao.sacm.model.Consulta;
import br.edu.avaliacao.sacm.model.Medico;
import br.edu.avaliacao.sacm.model.Paciente;
import br.edu.avaliacao.sacm.service.ConsultaService;

/**
 * Interface de texto do sistema. Só lê dados, chama o ConsultaService e mostra o resultado.
 * Nenhuma regra de negócio fica aqui.
 */
@Component
public class SistemaConsole implements CommandLineRunner {

    /** Formato único de data/hora digitado e exibido (ResolverStyle.STRICT recusa 31/02). */
    public static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);

    public static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static final Logger log = LoggerFactory.getLogger(SistemaConsole.class);

    private final ConsultaService consultaService;
    private final Scanner scanner;
    private boolean executando = true;

    public SistemaConsole(ConsultaService consultaService) {
        this.consultaService = consultaService;
        // Usa a codificação do terminal (no Windows, evita acentos trocados ao digitar).
        Console terminal = System.console();
        Charset charset = terminal != null ? terminal.charset() : Charset.defaultCharset();
        this.scanner = new Scanner(System.in, charset);
    }

    /** Chamado pelo Spring Boot depois que o contexto (banco, serviço etc.) está pronto. */
    @Override
    public void run(String... args) {
        System.out.println("SACM iniciado. Conectado ao banco sacm_db.");
        exibirMenu();
    }

    // =================================================================== menu

    public void exibirMenu() {
        while (executando) {
            System.out.println();
            System.out.println("================ SACM - Consultas Médicas ================");
            System.out.println(" 1 - Cadastrar médico");
            System.out.println(" 2 - Cadastrar paciente");
            System.out.println(" 3 - Listar médicos");
            System.out.println(" 4 - Listar pacientes");
            System.out.println(" 5 - Agendar consulta");
            System.out.println(" 6 - Listar consultas");
            System.out.println(" 7 - Alterar status de consulta (confirmar/cancelar/finalizar)");
            System.out.println(" 8 - Alterar disponibilidade de médico");
            System.out.println(" 9 - Ativar/desativar médico ou paciente");
            System.out.println(" 0 - Sair");
            System.out.println("==========================================================");

            try {
                int opcao = lerInteiro("Opção: ");
                executarOpcao(opcao);
            } catch (ConflitoHorarioException e) {
                System.out.println("[CONFLITO DE HORÁRIO] " + e.getMessage());
            } catch (RegraNegocioException e) {
                System.out.println("[REGRA DE NEGÓCIO] " + e.getMessage());
            } catch (DataAccessException e) {
                // RN09: mensagem clara para o usuário + causa técnica preservada para diagnóstico.
                Throwable causa = e.getMostSpecificCause();
                System.out.println("[FALHA TÉCNICA] Não foi possível acessar o banco de dados. Nada foi gravado.");
                System.out.println("  Causa: " + causa.getClass().getSimpleName() + " - " + causa.getMessage());
                log.error("Falha de acesso a dados (detalhes completos)", e);
            } catch (NoSuchElementException e) {
                // A entrada (teclado/pipe) foi fechada: encerra sem travar.
                encerrar();
            }
        }
    }

    private void executarOpcao(int opcao) {
        switch (opcao) {
            case 1 -> cadastrarMedico();
            case 2 -> cadastrarPaciente();
            case 3 -> listarMedicos();
            case 4 -> listarPacientes();
            case 5 -> agendarConsulta();
            case 6 -> listarConsultas();
            case 7 -> alterarStatusConsulta();
            case 8 -> alterarDisponibilidadeMedico();
            case 9 -> alterarStatusPessoa();
            case 0 -> encerrar();
            default -> System.out.println("Opção inválida. Digite um número de 0 a 9.");
        }
    }

    // =================================================================== operações do UML

    public void cadastrarMedico() {
        System.out.println("\n--- Cadastro de médico ---");
        String nome = lerTexto("Nome: ", true);
        String cpf = lerTexto("CPF (só números ou 000.000.000-00): ", true);
        LocalDate nascimento = lerData("Data de nascimento (dd/mm/aaaa): ");
        String email = lerTexto("E-mail (opcional): ", false);
        String telefone = lerTexto("Telefone (opcional): ", false);
        String cidade = lerTexto("Cidade (opcional): ", false);
        String estado = lerTexto("UF (opcional, ex.: PR): ", false);
        String crm = lerTexto("CRM (ex.: 12345-PR): ", true);
        Especialidade especialidade = lerOpcaoEnum("Especialidade", Especialidade.values());
        BigDecimal valor = lerValor("Valor da consulta (ex.: 250,00): ");

        Medico medico = new Medico(nome, cpf, nascimento, email, telefone, cidade, estado,
                crm, especialidade, valor);
        consultaService.cadastrarMedico(medico);
        System.out.println("[OK] Médico cadastrado: " + medico.getDescricao());
    }

    public void cadastrarPaciente() {
        System.out.println("\n--- Cadastro de paciente ---");
        String nome = lerTexto("Nome: ", true);
        String cpf = lerTexto("CPF (só números ou 000.000.000-00): ", true);
        LocalDate nascimento = lerData("Data de nascimento (dd/mm/aaaa): ");
        String email = lerTexto("E-mail (opcional): ", false);
        String telefone = lerTexto("Telefone (opcional): ", false);
        String cidade = lerTexto("Cidade (opcional): ", false);
        String estado = lerTexto("UF (opcional, ex.: PR): ", false);
        String convenio = lerTexto("Convênio (vazio = particular): ", false);
        String contato = lerTexto("Contato de emergência (opcional): ", false);
        String telContato = lerTexto("Telefone do contato de emergência (opcional): ", false);

        Paciente paciente = new Paciente(nome, cpf, nascimento, email, telefone, cidade, estado,
                convenio, contato, telContato);
        consultaService.cadastrarPaciente(paciente);
        System.out.println("[OK] Paciente cadastrado: " + paciente.getDescricao());
    }

    public void agendarConsulta() {
        System.out.println("\n--- Agendar consulta ---");
        Long medicoId = lerLong("ID do médico: ");
        Long pacienteId = lerLong("ID do paciente: ");
        LocalDateTime dataHora = lerDataHora("Data e hora (dd/mm/aaaa hh:mm): ");
        String motivo = lerTexto("Motivo: ", true);

        Consulta consulta = consultaService.agendarConsulta(medicoId, pacienteId, dataHora, motivo);
        System.out.println("[OK] " + consulta.getResumo());
    }

    public void listarConsultas() {
        System.out.println("\n--- Listar consultas ---");
        System.out.println(" 1 - Todas   2 - Por médico   3 - Por paciente");
        int filtro = lerInteiro("Filtro: ");
        List<Consulta> consultas = switch (filtro) {
            case 2 -> consultaService.listarConsultasPorMedico(lerLong("ID do médico: "));
            case 3 -> consultaService.listarConsultasPorPaciente(lerLong("ID do paciente: "));
            default -> consultaService.listarConsultas();
        };
        imprimirLista(consultas, "Nenhuma consulta encontrada.");
    }

    public void alterarStatusConsulta() {
        System.out.println("\n--- Alterar status da consulta ---");
        Long id = lerLong("ID da consulta: ");
        System.out.println(" 1 - Confirmar   2 - Cancelar   3 - Finalizar");
        int acao = lerInteiro("Ação: ");
        switch (acao) {
            case 1 -> consultaService.confirmarConsulta(id);
            case 2 -> consultaService.cancelarConsulta(id);
            case 3 -> consultaService.finalizarConsulta(id);
            default -> {
                System.out.println("Ação inválida.");
                return;
            }
        }
        System.out.println("[OK] Status da consulta #" + id + " atualizado e gravado no banco.");
    }

    public void encerrar() {
        executando = false;
        System.out.println("Encerrando o SACM. Os dados continuam salvos no MySQL. Até logo!");
    }

    // =================================================================== operações extras

    private void listarMedicos() {
        System.out.println("\n--- Médicos ---");
        imprimirLista(consultaService.listarMedicos(), "Nenhum médico cadastrado.");
    }

    private void listarPacientes() {
        System.out.println("\n--- Pacientes ---");
        imprimirLista(consultaService.listarPacientes(), "Nenhum paciente cadastrado.");
    }

    private void alterarDisponibilidadeMedico() {
        System.out.println("\n--- Disponibilidade do médico ---");
        Long id = lerLong("ID do médico: ");
        DisponibilidadeMedico nova = lerOpcaoEnum("Nova disponibilidade", DisponibilidadeMedico.values());
        Medico medico = consultaService.alterarDisponibilidadeMedico(id, nova);
        System.out.println("[OK] " + medico.getDescricao());
    }

    private void alterarStatusPessoa() {
        System.out.println("\n--- Ativar/desativar ---");
        System.out.println(" 1 - Médico   2 - Paciente");
        int tipo = lerInteiro("Tipo: ");
        Long id = lerLong("ID: ");
        System.out.println(" 1 - Ativar   2 - Desativar");
        boolean ativar = lerInteiro("Ação: ") == 1;
        String descricao = (tipo == 1)
                ? consultaService.alterarStatusMedico(id, ativar).getDescricao()
                : consultaService.alterarStatusPaciente(id, ativar).getDescricao();
        System.out.println("[OK] " + descricao);
    }

    // =================================================================== leitura do teclado

    private void imprimirLista(List<?> itens, String mensagemVazia) {
        if (itens.isEmpty()) {
            System.out.println(mensagemVazia);
        } else {
            itens.forEach(System.out::println); // toString() chama getDescricao()/getResumo()
        }
    }

    private String lerTexto(String rotulo, boolean obrigatorio) {
        while (true) {
            System.out.print(rotulo);
            String valor = scanner.nextLine().trim();
            if (!valor.isEmpty() || !obrigatorio) {
                return valor;
            }
            System.out.println("  Campo obrigatório.");
        }
    }

    private int lerInteiro(String rotulo) {
        while (true) {
            String valor = lerTexto(rotulo, true);
            try {
                return Integer.parseInt(valor);
            } catch (NumberFormatException e) {
                System.out.println("  Digite um número inteiro.");
            }
        }
    }

    private Long lerLong(String rotulo) {
        while (true) {
            String valor = lerTexto(rotulo, true);
            try {
                return Long.valueOf(valor);
            } catch (NumberFormatException e) {
                System.out.println("  Digite um número (ID).");
            }
        }
    }

    private BigDecimal lerValor(String rotulo) {
        while (true) {
            String valor = lerTexto(rotulo, true);
            if (valor.contains(",")) {               // formato brasileiro: 1.250,00
                valor = valor.replace(".", "").replace(",", ".");
            }
            try {
                return new BigDecimal(valor);
            } catch (NumberFormatException e) {
                System.out.println("  Valor inválido. Exemplo: 250,00");
            }
        }
    }

    private LocalDate lerData(String rotulo) {
        while (true) {
            try {
                return LocalDate.parse(lerTexto(rotulo, true), FORMATO_DATA);
            } catch (DateTimeParseException e) {
                System.out.println("  Data inválida. Use dd/mm/aaaa.");
            }
        }
    }

    private LocalDateTime lerDataHora(String rotulo) {
        while (true) {
            try {
                return LocalDateTime.parse(lerTexto(rotulo, true), FORMATO_DATA_HORA);
            } catch (DateTimeParseException e) {
                System.out.println("  Data/hora inválida. Use dd/mm/aaaa hh:mm (ex.: 25/11/2026 14:30).");
            }
        }
    }

    /** Mostra as constantes do enum numeradas e devolve a escolhida. */
    private <E extends Enum<E>> E lerOpcaoEnum(String rotulo, E[] opcoes) {
        for (int i = 0; i < opcoes.length; i++) {
            System.out.printf("  %d - %s%n", i + 1, opcoes[i].name());
        }
        while (true) {
            int escolha = lerInteiro(rotulo + ": ");
            if (escolha >= 1 && escolha <= opcoes.length) {
                return opcoes[escolha - 1];
            }
            System.out.println("  Escolha entre 1 e " + opcoes.length + ".");
        }
    }
}
