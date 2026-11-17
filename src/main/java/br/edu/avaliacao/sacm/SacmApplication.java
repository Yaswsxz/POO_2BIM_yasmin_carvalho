package br.edu.avaliacao.sacm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada. O Spring cria o DataSource, o JdbcTemplate, os repositórios,
 * o serviço e o console; o menu começa no SistemaConsole (CommandLineRunner).
 */
@SpringBootApplication
public class SacmApplication {

    public static void main(String[] args) {
        System.out.println("Iniciando SACM - Sistema de Agendamento de Consultas Médicas...");
        SpringApplication.run(SacmApplication.class, args);
    }
}
