-- =============================================================================
-- SIGA-UBS — DDL final limpo (derivado das migrações v1.0 a v1.6)
-- Tabelas em ordem topológica (respeitando FKs)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS sigaubs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sigaubs;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE tenants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    slug VARCHAR(80) NOT NULL,
    name VARCHAR(255) NOT NULL,
    domain VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    disabled_date DATETIME(6),
    disabled_user VARCHAR(255),
    disabled_reason VARCHAR(255),
    maintenance_date DATETIME(6),
    maintenance_user VARCHAR(255),
    maintenance_message VARCHAR(500),
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    CONSTRAINT uk_tenants_slug UNIQUE (slug),
    CONSTRAINT uk_tenants_domain UNIQUE (domain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_maintenance (
    id BIGINT PRIMARY KEY,
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    message VARCHAR(500),
    start_date DATETIME(6),
    end_date DATETIME(6),
    update_date DATETIME(6),
    update_user VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    `role` VARCHAR(100) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(255) NOT NULL,
    root BOOLEAN NOT NULL,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    CONSTRAINT uk_system_roles_role UNIQUE (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_admins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(100) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_system_admins_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE basic_health_units (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    neighborhood VARCHAR(200) NOT NULL,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    CONSTRAINT uk_bhu_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_bhu_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(100) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    id_basic_health_unit BIGINT,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_su_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_su_bhu_tenant FOREIGN KEY (tenant_id, id_basic_health_unit)
        REFERENCES basic_health_units(tenant_id, id),
    CONSTRAINT uk_su_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_su_tenant_username UNIQUE (tenant_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_users_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_system_user BIGINT NOT NULL,
    id_system_role BIGINT NOT NULL,
    CONSTRAINT fk_sur_user FOREIGN KEY (id_system_user) REFERENCES system_users(id),
    CONSTRAINT fk_sur_role FOREIGN KEY (id_system_role) REFERENCES system_roles(id),
    CONSTRAINT uk_sur_user_role UNIQUE (id_system_user, id_system_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE specialties (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255),
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE basic_health_units_specialties (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    id_basic_health_unit BIGINT NOT NULL,
    id_specialties BIGINT NOT NULL,
    CONSTRAINT fk_bhus_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_bhus_bhu_tenant FOREIGN KEY (tenant_id, id_basic_health_unit)
        REFERENCES basic_health_units(tenant_id, id),
    CONSTRAINT fk_bhus_spec FOREIGN KEY (id_specialties) REFERENCES specialties(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medical_procedures (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    description VARCHAR(255),
    `type` VARCHAR(255) NOT NULL,
    id_specialty BIGINT NOT NULL,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    CONSTRAINT fk_mp_specialty FOREIGN KEY (id_specialty) REFERENCES specialties(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE patients (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    gender VARCHAR(100) NOT NULL,
    social_sit_rating INT NOT NULL,
    sus_card_number CHAR(15) NOT NULL,
    cpf CHAR(11) NOT NULL,
    phone_number VARCHAR(11) NOT NULL,
    address_street VARCHAR(255),
    address_number VARCHAR(50),
    address_complement VARCHAR(255),
    address_ref VARCHAR(255),
    acs_name VARCHAR(150),
    id_basic_health_unit BIGINT NOT NULL,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    CONSTRAINT fk_pat_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_pat_bhu_tenant FOREIGN KEY (tenant_id, id_basic_health_unit)
        REFERENCES basic_health_units(tenant_id, id),
    CONSTRAINT uk_pat_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_pat_tenant_sus UNIQUE (tenant_id, sus_card_number),
    CONSTRAINT uk_pat_tenant_cpf UNIQUE (tenant_id, cpf),
    CONSTRAINT chk_pat_sus_digits CHECK (sus_card_number REGEXP '^[0-9]{15}$'),
    CONSTRAINT chk_pat_cpf_digits CHECK (cpf REGEXP '^[0-9]{11}$'),
    CONSTRAINT chk_pat_phone_digits CHECK (phone_number REGEXP '^[0-9]{10,11}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE patients ADD FULLTEXT INDEX idx_fulltext_patient (name, sus_card_number, cpf);

-- medical_slots: renomeada de available_appointments → available_medical_slots → medical_slots (v1.4)
CREATE TABLE medical_slots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    reference_month DATE NOT NULL,
    total_slots INT NOT NULL,
    current_slots INT NOT NULL DEFAULT 0,
    id_medical_procedure BIGINT NOT NULL,
    id_basic_health_unit BIGINT NOT NULL,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_ms_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_ms_procedure FOREIGN KEY (id_medical_procedure) REFERENCES medical_procedures(id),
    CONSTRAINT fk_ms_bhu_tenant FOREIGN KEY (tenant_id, id_basic_health_unit)
        REFERENCES basic_health_units(tenant_id, id),
    CONSTRAINT uk_ms_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT chk_ms_total_positive CHECK (total_slots > 0),
    CONSTRAINT chk_ms_balance CHECK (current_slots >= 0 AND current_slots <= total_slots)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- contemplations: id_appointment foi REMOVIDO na v1.6 (appointments agora aponta para contemplations)
CREATE TABLE contemplations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    contemplation_date DATETIME(6) NOT NULL,
    contemplated_by INT NOT NULL,
    id_available_medical_slot BIGINT NOT NULL,
    observation VARCHAR(2000),
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    CONSTRAINT fk_cont_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_cont_slot_tenant FOREIGN KEY (tenant_id, id_available_medical_slot)
        REFERENCES medical_slots(tenant_id, id),
    CONSTRAINT uk_cont_tenant_id UNIQUE (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- appointments: status (varchar 50) substituiu canceled na v1.5; id_contemplation adicionado na v1.6
CREATE TABLE appointments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    request_date DATETIME(6) NOT NULL,
    priority INT NOT NULL,
    observation TEXT,
    status VARCHAR(50),
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    id_medical_procedure BIGINT NOT NULL,
    id_patient BIGINT NOT NULL,
    id_contemplation BIGINT,
    CONSTRAINT fk_appt_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_appt_procedure FOREIGN KEY (id_medical_procedure) REFERENCES medical_procedures(id),
    CONSTRAINT fk_appt_patient_tenant FOREIGN KEY (tenant_id, id_patient)
        REFERENCES patients(tenant_id, id),
    CONSTRAINT fk_appt_contemplation_tenant FOREIGN KEY (tenant_id, id_contemplation)
        REFERENCES contemplations(tenant_id, id),
    CONSTRAINT uk_appt_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_appt_tenant_contemplation UNIQUE (tenant_id, id_contemplation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- appointment_status_history: adicionada na v1.6.0
CREATE TABLE appointment_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    status VARCHAR(100) NOT NULL,
    id_appointment BIGINT NOT NULL,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ash_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_ash_appt_tenant FOREIGN KEY (tenant_id, id_appointment)
        REFERENCES appointments(tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE patient_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    id_appointment BIGINT NOT NULL,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    CONSTRAINT fk_ph_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_ph_appt_tenant FOREIGN KEY (tenant_id, id_appointment)
        REFERENCES appointments(tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contemplation_job_executions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    execution_key VARCHAR(100) NOT NULL,
    window_start DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6),
    lock_token CHAR(36) NOT NULL,
    lease_until DATETIME(6) NOT NULL,
    CONSTRAINT fk_cje_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uk_cje_tenant_execution UNIQUE (tenant_id, execution_key),
    CONSTRAINT chk_cje_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    INDEX idx_cje_tenant_started (tenant_id, started_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Spring Session JDBC faz parte do DDL inicial; o auto-initializer permanece desabilitado.
CREATE TABLE SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(200),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID),
    CONSTRAINT SPRING_SESSION_IX1 UNIQUE (SESSION_ID)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;

CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES LONGBLOB NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;

DELIMITER $$

CREATE TRIGGER trg_contemplations_validate_insert
BEFORE INSERT ON contemplations
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM medical_slots slot
        WHERE slot.id = NEW.id_available_medical_slot
          AND slot.tenant_id = NEW.tenant_id
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Contemplação e vaga devem pertencer ao mesmo tenant';
    END IF;
END$$

CREATE TRIGGER trg_contemplations_validate_update
BEFORE UPDATE ON contemplations
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM medical_slots slot
        WHERE slot.id = NEW.id_available_medical_slot
          AND slot.tenant_id = NEW.tenant_id
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Contemplação e vaga devem pertencer ao mesmo tenant';
    END IF;
END$$

CREATE TRIGGER trg_appointments_validate_insert
BEFORE INSERT ON appointments
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM patients patient
        WHERE patient.id = NEW.id_patient
          AND patient.tenant_id = NEW.tenant_id
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Consulta e paciente devem pertencer ao mesmo tenant';
    END IF;

    IF NEW.id_contemplation IS NOT NULL AND NOT EXISTS (
        SELECT 1
        FROM contemplations contemplation
        JOIN medical_slots slot ON slot.id = contemplation.id_available_medical_slot
            AND slot.tenant_id = contemplation.tenant_id
        JOIN patients patient ON patient.id = NEW.id_patient
            AND patient.tenant_id = NEW.tenant_id
        WHERE contemplation.id = NEW.id_contemplation
          AND contemplation.tenant_id = NEW.tenant_id
          AND slot.id_medical_procedure = NEW.id_medical_procedure
          AND slot.id_basic_health_unit = patient.id_basic_health_unit
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Procedimento ou UBS da contemplação diverge da consulta';
    END IF;
END$$

CREATE TRIGGER trg_appointments_validate_update
BEFORE UPDATE ON appointments
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM patients patient
        WHERE patient.id = NEW.id_patient
          AND patient.tenant_id = NEW.tenant_id
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Consulta e paciente devem pertencer ao mesmo tenant';
    END IF;

    IF NEW.id_contemplation IS NOT NULL AND NOT EXISTS (
        SELECT 1
        FROM contemplations contemplation
        JOIN medical_slots slot ON slot.id = contemplation.id_available_medical_slot
            AND slot.tenant_id = contemplation.tenant_id
        JOIN patients patient ON patient.id = NEW.id_patient
            AND patient.tenant_id = NEW.tenant_id
        WHERE contemplation.id = NEW.id_contemplation
          AND contemplation.tenant_id = NEW.tenant_id
          AND slot.id_medical_procedure = NEW.id_medical_procedure
          AND slot.id_basic_health_unit = patient.id_basic_health_unit
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Procedimento ou UBS da contemplação diverge da consulta';
    END IF;
END$$

DELIMITER ;

-- =============================================================================
-- Índices de performance — colunas de alta frequência em queries e filtros
-- =============================================================================
CREATE INDEX idx_appt_status     ON appointments(status);
CREATE INDEX idx_appt_tenant_status ON appointments(tenant_id, status);
CREATE INDEX idx_appt_tenant_request ON appointments(tenant_id, request_date);
CREATE INDEX idx_appt_tenant_status_request ON appointments(tenant_id, status, request_date);
CREATE INDEX idx_appt_patient    ON appointments(tenant_id, id_patient);
CREATE INDEX idx_appt_tenant_status_cont_proc_request ON appointments(tenant_id, status, id_contemplation, id_medical_procedure, request_date);
CREATE INDEX idx_appt_tenant_contemplation_status ON appointments(tenant_id, id_contemplation, status);
CREATE INDEX idx_pat_tenant_ubs_name ON patients(tenant_id, id_basic_health_unit, name);
CREATE INDEX idx_ms_tenant_month_ubs_proc ON medical_slots(tenant_id, reference_month, id_basic_health_unit, id_medical_procedure);
CREATE INDEX idx_ms_tenant_ubs_proc_month ON medical_slots(tenant_id, id_basic_health_unit, id_medical_procedure, reference_month);
CREATE INDEX idx_cont_date       ON contemplations(tenant_id, contemplation_date);
CREATE INDEX idx_cont_tenant_slot_date ON contemplations(tenant_id, id_available_medical_slot, contemplation_date);
CREATE INDEX idx_bhu_tenant_name ON basic_health_units(tenant_id, name);
CREATE INDEX idx_su_tenant_bhu ON system_users(tenant_id, id_basic_health_unit);
CREATE INDEX idx_specialty_title ON specialties(title);
