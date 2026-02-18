-- =============================================================================
-- SIGA-UBS — DDL final limpo (derivado das migrações v1.0 a v1.6)
-- Tabelas em ordem topológica (respeitando FKs)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS sigaubs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sigaubs;

CREATE TABLE system_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    `role` VARCHAR(100) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(255) NOT NULL,
    root BOOLEAN NOT NULL,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE basic_health_units (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    neighborhood VARCHAR(200) NOT NULL,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(100) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    id_basic_health_unit BIGINT,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    CONSTRAINT fk_su_bhu FOREIGN KEY (id_basic_health_unit) REFERENCES basic_health_units(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_users_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_system_user BIGINT NOT NULL,
    id_system_role BIGINT NOT NULL,
    CONSTRAINT fk_sur_user FOREIGN KEY (id_system_user) REFERENCES system_users(id),
    CONSTRAINT fk_sur_role FOREIGN KEY (id_system_role) REFERENCES system_roles(id)
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
    id_basic_health_unit BIGINT NOT NULL,
    id_specialties BIGINT NOT NULL,
    CONSTRAINT fk_bhus_bhu FOREIGN KEY (id_basic_health_unit) REFERENCES basic_health_units(id),
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
    name VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    gender VARCHAR(100) NOT NULL,
    social_sit_rating INT NOT NULL,
    sus_card_number VARCHAR(20) UNIQUE,
    cpf VARCHAR(14) UNIQUE,
    phone_number VARCHAR(20),
    address_street VARCHAR(255),
    address_number VARCHAR(50),
    address_complement VARCHAR(255),
    address_ref VARCHAR(255),
    acs_name VARCHAR(150),
    id_basic_health_unit BIGINT,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    CONSTRAINT fk_pat_bhu FOREIGN KEY (id_basic_health_unit) REFERENCES basic_health_units(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE patients ADD FULLTEXT INDEX idx_fulltext_patient (name, sus_card_number, cpf);

-- medical_slots: renomeada de available_appointments → available_medical_slots → medical_slots (v1.4)
CREATE TABLE medical_slots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reference_month DATE NOT NULL,
    total_slots INT NOT NULL,
    current_slots INT DEFAULT 0,
    id_medical_procedure BIGINT NOT NULL,
    id_basic_health_unit BIGINT NOT NULL,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    CONSTRAINT fk_ms_procedure FOREIGN KEY (id_medical_procedure) REFERENCES medical_procedures(id),
    CONSTRAINT available_medical_slots_FK FOREIGN KEY (id_basic_health_unit) REFERENCES basic_health_units(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- contemplations: id_appointment foi REMOVIDO na v1.6 (appointments agora aponta para contemplations)
CREATE TABLE contemplations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    contemplation_date DATETIME(6) NOT NULL,
    contemplated_by INT NOT NULL,
    id_available_medical_slot BIGINT NOT NULL,
    observation VARCHAR(255),
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    CONSTRAINT fk_cont_slot FOREIGN KEY (id_available_medical_slot) REFERENCES medical_slots(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- appointments: status (varchar 50) substituiu canceled na v1.5; id_contemplation adicionado na v1.6
CREATE TABLE appointments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_date DATETIME(6) NOT NULL,
    priority INT NOT NULL,
    observation TEXT,
    status VARCHAR(50),
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    id_medical_procedure BIGINT NOT NULL,
    id_patient BIGINT NOT NULL,
    id_contemplation BIGINT,
    CONSTRAINT fk_appt_procedure FOREIGN KEY (id_medical_procedure) REFERENCES medical_procedures(id),
    CONSTRAINT fk_appt_patient FOREIGN KEY (id_patient) REFERENCES patients(id),
    CONSTRAINT appointments_FK FOREIGN KEY (id_contemplation) REFERENCES contemplations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- appointment_status_history: adicionada na v1.6.0
CREATE TABLE appointment_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(100) NOT NULL,
    id_appointment BIGINT NOT NULL,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ash_appt FOREIGN KEY (id_appointment) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE patient_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    id_appointment BIGINT NOT NULL,
    creation_date DATETIME(6) NOT NULL,
    creation_user VARCHAR(255) NOT NULL,
    update_date DATETIME(6),
    update_user VARCHAR(255),
    CONSTRAINT fk_ph_appt FOREIGN KEY (id_appointment) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
