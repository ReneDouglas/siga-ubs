-- Fixture enxuta para testes manuais de isolamento multi-tenant.
-- Execute apos docker/mysql/01-schema.sql e docker/mysql/02-seed.sql.

USE sigaubs;

INSERT INTO patients (
    id, tenant_id, name, birth_date, gender, social_sit_rating, sus_card_number, cpf,
    phone_number, id_basic_health_unit, creation_date, creation_user
) VALUES
(1, 1, 'Paciente Afogados', '1980-01-10', 'Feminino', 3, '111111111111111', '111.111.111-11', '(87) 99999-0001', 1, NOW(6), 'sistema'),
(2, 2, 'Paciente Caruaru',  '1985-02-20', 'Masculino', 3, '111111111111111', '111.111.111-11', '(81) 99999-0002', 3, NOW(6), 'sistema');

INSERT INTO appointments (
    id, tenant_id, request_date, priority, observation, status, creation_user,
    id_medical_procedure, id_patient
) VALUES
(1, 1, '2026-01-10 09:00:00.000000', 3, 'Fila Afogados', 'Aguardando Contemplação', 'sistema', 19, 1),
(2, 2, '2026-01-10 09:00:00.000000', 3, 'Fila Caruaru',  'Aguardando Contemplação', 'sistema', 19, 2);

INSERT INTO medical_slots (
    id, tenant_id, reference_month, total_slots, current_slots,
    id_medical_procedure, id_basic_health_unit, creation_user, creation_date
) VALUES
(1, 1, '2026-01-01', 5, 5, 19, 1, 'sistema', NOW(6)),
(2, 2, '2026-01-01', 5, 5, 19, 3, 'sistema', NOW(6));
