-- =====================================================================
-- SACM - Sistema de Agendamento de Consultas Médicas
-- Script de criação do banco (MySQL 8.x; também roda no MariaDB do XAMPP)
-- Pode ser executado mais de uma vez: não apaga dados existentes (RN08).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS sacm_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE sacm_db;

-- ---------------------------------------------------------------------
-- PESSOA: dados comuns de médicos e pacientes (classe abstrata Pessoa).
-- O CPF é UNIQUE aqui, então vale para médicos E pacientes juntos (RN01).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pessoa (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    tipo             VARCHAR(10)  NOT NULL,
    nome             VARCHAR(120) NOT NULL,
    cpf              CHAR(11)     NOT NULL,
    data_nascimento  DATE         NOT NULL,
    email            VARCHAR(120) NULL,
    telefone         VARCHAR(20)  NULL,
    cidade           VARCHAR(80)  NULL,
    estado           CHAR(2)      NULL,
    data_cadastro    DATETIME     NOT NULL,
    status           VARCHAR(10)  NOT NULL DEFAULT 'ATIVO',
    CONSTRAINT pk_pessoa PRIMARY KEY (id),
    CONSTRAINT uk_pessoa_cpf UNIQUE (cpf),
    CONSTRAINT ck_pessoa_tipo CHECK (tipo IN ('MEDICO', 'PACIENTE')),
    CONSTRAINT ck_pessoa_status CHECK (status IN ('ATIVO', 'INATIVO'))
);

-- ---------------------------------------------------------------------
-- MEDICO: herda de pessoa (mesmo id = chave primária e estrangeira).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS medico (
    id               BIGINT        NOT NULL,
    crm              VARCHAR(20)   NOT NULL,
    especialidade    VARCHAR(20)   NOT NULL,
    disponibilidade  VARCHAR(15)   NOT NULL DEFAULT 'DISPONIVEL',
    valor_consulta   DECIMAL(10,2) NOT NULL,
    CONSTRAINT pk_medico PRIMARY KEY (id),
    CONSTRAINT uk_medico_crm UNIQUE (crm),
    CONSTRAINT fk_medico_pessoa FOREIGN KEY (id) REFERENCES pessoa (id),
    CONSTRAINT ck_medico_especialidade CHECK (especialidade IN
        ('CLINICO_GERAL', 'CARDIOLOGIA', 'DERMATOLOGIA', 'PEDIATRIA', 'ORTOPEDIA')),
    CONSTRAINT ck_medico_disponibilidade CHECK (disponibilidade IN ('DISPONIVEL', 'INDISPONIVEL')),
    CONSTRAINT ck_medico_valor CHECK (valor_consulta > 0)
);

-- ---------------------------------------------------------------------
-- PACIENTE: herda de pessoa.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS paciente (
    id                   BIGINT       NOT NULL,
    numero_prontuario    VARCHAR(20)  NOT NULL,
    convenio             VARCHAR(60)  NULL,
    contato_emergencia   VARCHAR(120) NULL,
    telefone_emergencia  VARCHAR(20)  NULL,
    CONSTRAINT pk_paciente PRIMARY KEY (id),
    CONSTRAINT uk_paciente_prontuario UNIQUE (numero_prontuario),
    CONSTRAINT fk_paciente_pessoa FOREIGN KEY (id) REFERENCES pessoa (id)
);

-- ---------------------------------------------------------------------
-- CONSULTA: associa exatamente um médico e um paciente.
--
-- RN04 também é garantida no banco: a coluna gerada horario_ativo só tem
-- valor quando a consulta está AGENDADA ou CONFIRMADA. Consultas
-- CANCELADAS/FINALIZADAS ficam com NULL, e o UNIQUE ignora NULL —
-- por isso o horário cancelado volta a ficar livre (RN07).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS consulta (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    medico_id         BIGINT       NOT NULL,
    paciente_id       BIGINT       NOT NULL,
    data_hora         DATETIME     NOT NULL,
    motivo            VARCHAR(255) NOT NULL,
    status            VARCHAR(12)  NOT NULL DEFAULT 'AGENDADA',
    data_agendamento  DATETIME     NOT NULL,
    observacao        VARCHAR(255) NULL,
    horario_ativo     DATETIME GENERATED ALWAYS AS
        (CASE WHEN status IN ('AGENDADA', 'CONFIRMADA') THEN data_hora ELSE NULL END) STORED,
    CONSTRAINT pk_consulta PRIMARY KEY (id),
    CONSTRAINT fk_consulta_medico FOREIGN KEY (medico_id) REFERENCES medico (id),
    CONSTRAINT fk_consulta_paciente FOREIGN KEY (paciente_id) REFERENCES paciente (id),
    CONSTRAINT ck_consulta_status CHECK (status IN ('AGENDADA', 'CONFIRMADA', 'CANCELADA', 'FINALIZADA')),
    CONSTRAINT uk_consulta_medico_horario UNIQUE (medico_id, horario_ativo),
    CONSTRAINT uk_consulta_paciente_horario UNIQUE (paciente_id, horario_ativo)
);
