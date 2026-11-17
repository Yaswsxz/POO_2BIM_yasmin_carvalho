# POO_2BIM_yasmin_carvalho — SACM

**SACM – Sistema de Agendamento de Consultas Médicas.** Aplicação de console em Java 21 + Spring Boot (Spring JDBC / `JdbcTemplate`) com persistência em MySQL. Cadastra médicos e pacientes, agenda consultas, impede conflitos de horário e controla o ciclo de vida da consulta (agendada → confirmada → finalizada, ou cancelada).

Avaliação prática do 2º bimestre — Programação Orientada a Objetos (Prof. Cesar Malvezi), Engenharia de Software, UniCesumar.
Autora: Yasmin Fernanda de Carvalho.

---

## 1. Pré-requisitos

| Ferramenta | Versão | Conferir com |
|---|---|---|
| JDK | 21 | `java -version` |
| MySQL Server | 8.x (o MariaDB do XAMPP também funciona) | `mysql --version` |
| Maven | opcional — o projeto traz o Maven Wrapper (`mvnw`) | `.\mvnw.cmd -v` |
| VS Code | Extension Pack for Java + Spring Boot Extension Pack | — |

Na primeira compilação o Maven baixa as dependências, então é preciso internet.

## 2. Criar o banco

No MySQL Workbench, phpMyAdmin (aba SQL) ou terminal, como **root**:

```bash
mysql -u root -p < database/schema.sql
```

Depois, crie o usuário da aplicação (abra `database/usuario.sql`, **troque a senha** e execute):

```bash
mysql -u root -p < database/usuario.sql
```

O `schema.sql` usa `CREATE ... IF NOT EXISTS`: rodar de novo **não apaga** os dados.

## 3. Configurar usuário e senha (variáveis de ambiente)

A senha **não** fica no código nem no Git. O `application.properties` lê `DB_USER` e `DB_PASSWORD`.

**Windows – PowerShell** (vale para o terminal aberto):
```powershell
$env:DB_USER="sacm_app"
$env:DB_PASSWORD="sua_senha"
```

**Windows – CMD:**
```cmd
set DB_USER=sacm_app
set DB_PASSWORD=sua_senha
```

**Linux/macOS:**
```bash
export DB_USER=sacm_app
export DB_PASSWORD=sua_senha
```

> No VS Code, rode a aplicação pelo **mesmo terminal** onde as variáveis foram definidas, ou adicione-as em `launch.json` → `"env"` (sem fazer commit desse arquivo).

## 4. Compilar, testar e executar

```powershell
.\mvnw.cmd clean test          # compila e roda os testes unitários (não precisa do MySQL)
.\mvnw.cmd spring-boot:run     # inicia o menu no terminal
```
No Linux/macOS use `./mvnw` no lugar de `.\mvnw.cmd`.

Ou gere o .jar e execute:
```powershell
.\mvnw.cmd clean package
java -jar target\sacm-0.0.1-SNAPSHOT.jar
```

## 5. Menu

```
 1 - Cadastrar médico
 2 - Cadastrar paciente
 3 - Listar médicos
 4 - Listar pacientes
 5 - Agendar consulta
 6 - Listar consultas
 7 - Alterar status de consulta (confirmar/cancelar/finalizar)
 8 - Alterar disponibilidade de médico
 9 - Ativar/desativar médico ou paciente
 0 - Sair
```

Datas: `dd/mm/aaaa`. Data e hora da consulta: `dd/mm/aaaa hh:mm` (ex.: `25/11/2026 14:30`).

## 6. Roteiro de demonstração (critérios de aceite)

1. **Médico ativo/disponível:** opção 1 → cadastre a Dra. Ana (CPF `529.982.247-25`, CRM `12345-PR`, Cardiologia, 300,00). Opção 3 mostra `ATIVO | DISPONIVEL`.
2. **Paciente ativo:** opção 2 → Bruno (CPF `111.444.777-35`). Cadastre também Carla (CPF `390.533.447-05`).
3. **Agendamento válido:** opção 5 → médico 1, paciente 2, uma data futura às 14:00.
4. **Tentativa de conflito:** opção 5 → médico 1, paciente 3, **mesma data/hora** → `[CONFLITO DE HORÁRIO]`.
5. **Cancelamento:** opção 7 → consulta 1 → Cancelar.
6. **Novo agendamento no horário liberado:** repita o passo 4 → agora funciona.
7. **Persistência (RN08):** saia (0), rode de novo e liste (opção 6); depois rode `database/evidencias.sql` no MySQL e tire print.

Extras: opção 8 deixa o médico `INDISPONIVEL` e opção 9 desativa um paciente — os dois bloqueiam novos agendamentos.

## 7. Estrutura

```
POO_2BIM_yasmin_carvalho/
├── database/
│   ├── schema.sql        # tabelas pessoa, medico, paciente, consulta
│   ├── usuario.sql       # usuário com permissão só no sacm_db
│   └── evidencias.sql    # SELECTs para os prints do relatório
├── src/main/java/br/edu/avaliacao/sacm/
│   ├── SacmApplication.java
│   ├── model/        Pessoa (abstrata), Medico, Paciente, Consulta
│   ├── enums/        StatusPessoa, DisponibilidadeMedico, Especialidade, StatusConsulta
│   ├── interfaces/   Agendavel
│   ├── repository/   MedicoRepository, PacienteRepository, ConsultaRepository (JdbcTemplate)
│   ├── service/      ConsultaService (regras RN01–RN07, implementa Agendavel)
│   ├── exception/    RegraNegocioException, ConflitoHorarioException
│   └── console/      SistemaConsole (menu, CommandLineRunner)
├── src/main/resources/application.properties
├── src/test/java/...  ConsultaTest, PessoaTest (JUnit, sem banco)
├── pom.xml
└── mvnw / mvnw.cmd
```

## 8. Problemas comuns

| Mensagem | Causa provável |
|---|---|
| `Could not resolve placeholder 'DB_USER'` | Variáveis de ambiente não definidas no terminal que rodou a aplicação. |
| `Access denied for user` | Usuário/senha errados, ou `usuario.sql` não executado. |
| `Communications link failure` | MySQL parado (no XAMPP, clique em *Start* no MySQL). |
| `Unknown database 'sacm_db'` | `schema.sql` não executado. |

Logs técnicos completos ficam em `logs/sacm.log`.

## 9. Uso de IA

Declarado no relatório técnico (seção 9).
