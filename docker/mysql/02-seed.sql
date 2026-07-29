-- =============================================================================
-- SIGA-UBS — Dados iniciais para desenvolvimento
-- ATENÇÃO: Alterar as senhas no primeiro uso em ambiente de produção!
-- Senhas exclusivas de desenvolvimento:
-- admin/AdminDev#2026 | sms/SmsDev#2026 | user/UserDev#2026
-- =============================================================================

USE sigaubs;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Manutenção global
-- -----------------------------------------------------------------------------
INSERT INTO system_maintenance (id, enabled) VALUES
(1, 0);

-- -----------------------------------------------------------------------------
-- Tenants
-- -----------------------------------------------------------------------------
INSERT INTO tenants (id, slug, name, domain, status, creation_date, creation_user) VALUES
(1, 'afogados', 'Afogados da Ingazeira', 'afogados.sigaubs.com.br', 'ACTIVE', NOW(6), 'sistema'),
(2, 'caruaru',  'Caruaru',               'caruaru.sigaubs.com.br',  'ACTIVE', NOW(6), 'sistema');

-- -----------------------------------------------------------------------------
-- Perfis de acesso
-- -----------------------------------------------------------------------------
INSERT INTO system_roles (id, `role`, title, description, root, creation_date, creation_user) VALUES
(2, 'ROLE_SMS',        'Secretaria de Saúde',    'Gestão da Secretaria Municipal de Saúde',                FALSE, NOW(6), 'sistema'),
(3, 'ROLE_ATENDENTE',  'Atendente',              'Atendente da Unidade Básica de Saúde',                   FALSE, NOW(6), 'sistema'),
(4, 'ROLE_ENFERMEIRO', 'Enfermeiro',             'Enfermeiro da Unidade Básica de Saúde',                  FALSE, NOW(6), 'sistema'),
(5, 'ROLE_ACS',        'ACS',                    'Agente Comunitário de Saúde',                            FALSE, NOW(6), 'sistema'),
(6, 'ROLE_USER',       'Usuário',                'Usuário padrão da Unidade Básica de Saúde',              FALSE, NOW(6), 'sistema');

-- -----------------------------------------------------------------------------
-- Administradores globais
-- Hash BCrypt (custo 12) da senha exclusiva de desenvolvimento.
-- -----------------------------------------------------------------------------
INSERT INTO system_admins (id, username, `password`, name, email, active, creation_date, creation_user) VALUES
(1, 'admin', '$2y$12$8K8OmpqHQokX3TKK.tKt/uVIU4mAUHP5t2WyIH5Qy7xP8ZKopnWFe', 'Administrador do Sistema', 'admin@sigaubs.local', 1, NOW(6), 'sistema');

-- -----------------------------------------------------------------------------
-- Unidades Básicas de Saúde
-- -----------------------------------------------------------------------------
INSERT INTO basic_health_units (id, tenant_id, name, neighborhood, creation_date, creation_user) VALUES
(1,   1, 'UBS São Francisco',            'São Francisco',                  NOW(6), 'sistema'),
(2,   1, 'UBS São Brás',                  'São Brás',                       NOW(6), 'sistema'),
(3,   1, 'UBS Borges',                    'Borges e Brotas',                 NOW(6), 'sistema'),
(4,   1, 'UBS São Sebastião',             'São Sebastião e Costa',           NOW(6), 'sistema'),
(5,   1, 'UBS Sobreira',                  'Sobreira e São Cristóvão',        NOW(6), 'sistema'),
(6,   1, 'UBS Padre Pedro Pereira',       'Padre Pedro Pereira',             NOW(6), 'sistema'),
(7,   1, 'UBS Planalto',                  'Planalto e Manoela Valadares',    NOW(6), 'sistema'),
(8,   1, 'ESF Povoado da Varzinha',       'Varzinha e zona rural',           NOW(6), 'sistema'),
(101, 2, 'USF Agamenon Magalhães I',      'Agamenon Magalhães',              NOW(6), 'sistema'),
(102, 2, 'USF Caiucá I',                  'Caiucá',                           NOW(6), 'sistema'),
(103, 2, 'USF Centenário',                'Centenário',                      NOW(6), 'sistema'),
(104, 2, 'USF Cidade Jardim',             'Cidade Jardim',                   NOW(6), 'sistema'),
(105, 2, 'USF Rendeiras I',               'Rendeiras',                       NOW(6), 'sistema'),
(106, 2, 'USF Salgado I',                 'Salgado',                         NOW(6), 'sistema'),
(107, 2, 'USF Santa Rosa I',              'Santa Rosa',                      NOW(6), 'sistema'),
(108, 2, 'USF São João da Escócia I',     'São João da Escócia',             NOW(6), 'sistema');

-- -----------------------------------------------------------------------------
-- Usuários do sistema
-- Hashes BCrypt (custo 12) das senhas exclusivas de desenvolvimento.
-- -----------------------------------------------------------------------------
INSERT INTO system_users (id, tenant_id, username, `password`, name, email, active, id_basic_health_unit, creation_date, creation_user) VALUES
(1, 1, 'sms',  '$2y$12$qTZNzElTly6bwFdk0EWBu.pXlJkq2Sdl.mDabIbCpR8H1gyy7uAO.', 'Secretaria Municipal',        'sms@afogados.sigaubs.local',  1, NULL, NOW(6), 'sistema'),
(2, 1, 'user', '$2y$12$LFBA2FKSTWlQtmXVyYWOCuNIIYHV/nBRk.QxqXHrzUH0Ykar.hOpy', 'Usuário UBS São Francisco',   'user@afogados.sigaubs.local', 1, 1,    NOW(6), 'sistema'),
(3, 2, 'sms',  '$2y$12$qTZNzElTly6bwFdk0EWBu.pXlJkq2Sdl.mDabIbCpR8H1gyy7uAO.', 'Secretaria Municipal Caruaru','sms@caruaru.sigaubs.local',   1, NULL, NOW(6), 'sistema'),
(4, 2, 'user', '$2y$12$LFBA2FKSTWlQtmXVyYWOCuNIIYHV/nBRk.QxqXHrzUH0Ykar.hOpy', 'Usuário USF Agamenon',        'user@caruaru.sigaubs.local',  1, 101,  NOW(6), 'sistema');

-- -----------------------------------------------------------------------------
-- Associação de perfis aos usuários
-- -----------------------------------------------------------------------------
INSERT INTO system_users_roles (id_system_user, id_system_role) VALUES
(1, 2), -- sms afogados  → ROLE_SMS
(2, 6), -- user afogados → ROLE_USER
(3, 2), -- sms caruaru   → ROLE_SMS
(4, 6); -- user caruaru  → ROLE_USER

-- -----------------------------------------------------------------------------
-- Especialidades médicas
-- -----------------------------------------------------------------------------
INSERT INTO specialties (id, title, description, active, creation_date, creation_user) VALUES
(1,  'Cardiologia',        'Diagnostica e trata doenças do coração e do sistema circulatório.',                  1, NOW(6), 'sistema'),
(2,  'Mastologia',         'Cuida da saúde das mamas, incluindo a prevenção e tratamento do câncer de mama.',   1, NOW(6), 'sistema'),
(3,  'Endocrinologia',     'Trata distúrbios hormonais e doenças das glândulas endócrinas.',                    1, NOW(6), 'sistema'),
(4,  'Otorrinolaringologia','Diagnostica e trata doenças do ouvido, nariz e garganta.',                         1, NOW(6), 'sistema'),
(5,  'Urologia',           'Foca no trato urinário de homens e mulheres e no sistema reprodutor masculino.',    1, NOW(6), 'sistema'),
(6,  'Oftalmologia',       'Cuida da saúde dos olhos e trata problemas visuais.',                               1, NOW(6), 'sistema'),
(7,  'Ortopedia',          'Trata doenças e lesões do sistema musculoesquelético.',                             1, NOW(6), 'sistema'),
(8,  'Cirurgia Geral',     'Realiza intervenções para tratar doenças, lesões e deformidades.',                  1, NOW(6), 'sistema'),
(9,  'Neurologia',         'Trata distúrbios do sistema nervoso central e periférico.',                         1, NOW(6), 'sistema'),
(10, 'Obstetrícia',        'Cuida da gestação, parto e saúde da mãe e do bebê.',                               1, NOW(6), 'sistema'),
(12, 'Reumatologia',       'Trata doenças reumáticas e autoimunes que afetam articulações, músculos e ossos.', 1, NOW(6), 'sistema'),
(13, 'Ginecologia',        'Cuida da saúde do sistema reprodutor feminino.',                                    1, NOW(6), 'sistema'),
(14, 'Dermatologia',       'Diagnostica e trata doenças da pele, cabelos e unhas.',                             1, NOW(6), 'sistema'),
(15, 'Gastroenterologia',  'Trata doenças do sistema digestivo.',                                              1, NOW(6), 'sistema'),
(16, 'Angiologia',         'Cuida das doenças dos vasos sanguíneos e linfáticos.',                             1, NOW(6), 'sistema'),
(17, 'Nutrição',           'Estuda os alimentos e sua influência na saúde, elaborando planos alimentares.',    1, NOW(6), 'sistema'),
(18, 'Pediatria',          'Cuida da saúde de bebês, crianças e adolescentes.',                                1, NOW(6), 'sistema'),
(19, 'Psiquiatria',        'Diagnostica e trata transtornos mentais e emocionais.',                            1, NOW(6), 'sistema'),
(20, 'Radiologia',         'Utiliza técnicas de imagem para diagnosticar e tratar doenças.',                   1, NOW(6), 'sistema'),
(23, 'Fonoaudiologia',     'Trata distúrbios da comunicação, fala, linguagem e audição.',                      1, NOW(6), 'sistema'),
(25, 'Fisioterapia',       'Utiliza exercícios terapêuticos para tratar e prevenir doenças e lesões físicas.', 1, NOW(6), 'sistema'),
(27, 'Nefrologia',         'Diagnostica e trata doenças dos rins e do sistema urinário.',                      1, NOW(6), 'sistema');

-- -----------------------------------------------------------------------------
-- Procedimentos médicos
-- -----------------------------------------------------------------------------
INSERT INTO medical_procedures (id, description, `type`, id_specialty, creation_date, creation_user) VALUES
-- Cirurgias
(1,  'Histerectomia',           'CIRURGIA', 13, NOW(6), 'sistema'),
(2,  'Cirurgia HREC',           'CIRURGIA',  8, NOW(6), 'sistema'),
(3,  'Cirurgia de Hérnia',      'CIRURGIA',  8, NOW(6), 'sistema'),
(4,  'Cirurgia de Hérnia Inguinal',  'CIRURGIA', 8, NOW(6), 'sistema'),
(5,  'Cirurgia de Hérnia Epigástrica','CIRURGIA', 8, NOW(6), 'sistema'),
-- Exames
(6,  'Mamografia',              'EXAME',  2, NOW(6), 'sistema'),
(7,  'Densitometria',           'EXAME', 12, NOW(6), 'sistema'),
(8,  'Tomografia',              'EXAME', 20, NOW(6), 'sistema'),
(9,  'Ecocardiograma',          'EXAME',  1, NOW(6), 'sistema'),
(10, 'Eletrocardiograma',       'EXAME',  1, NOW(6), 'sistema'),
(11, 'Teste Ergonométrico',     'EXAME',  1, NOW(6), 'sistema'),
(12, 'Raio X',                  'EXAME', 20, NOW(6), 'sistema'),
(13, 'Colonoscopia',            'EXAME', 15, NOW(6), 'sistema'),
(14, 'Endoscopia',              'EXAME', 15, NOW(6), 'sistema'),
(15, 'Colposcopia',             'EXAME', 13, NOW(6), 'sistema'),
(16, 'Ultrassonografia',        'EXAME', 20, NOW(6), 'sistema'),
(17, 'Ressonância Magnética',   'EXAME', 20, NOW(6), 'sistema'),
(18, 'Histeroscopia',           'EXAME', 13, NOW(6), 'sistema'),
-- Consultas (uma por especialidade)
(19, '-', 'CONSULTA',  1, NOW(6), 'sistema'),
(20, '-', 'CONSULTA',  2, NOW(6), 'sistema'),
(21, '-', 'CONSULTA',  3, NOW(6), 'sistema'),
(22, '-', 'CONSULTA',  4, NOW(6), 'sistema'),
(23, '-', 'CONSULTA',  5, NOW(6), 'sistema'),
(24, '-', 'CONSULTA',  6, NOW(6), 'sistema'),
(25, '-', 'CONSULTA',  7, NOW(6), 'sistema'),
(27, '-', 'CONSULTA',  9, NOW(6), 'sistema'),
(28, '-', 'CONSULTA', 10, NOW(6), 'sistema'),
(29, '-', 'CONSULTA', 12, NOW(6), 'sistema'),
(30, '-', 'CONSULTA', 13, NOW(6), 'sistema'),
(31, '-', 'CONSULTA', 14, NOW(6), 'sistema'),
(32, '-', 'CONSULTA', 15, NOW(6), 'sistema'),
(33, '-', 'CONSULTA', 16, NOW(6), 'sistema'),
(34, '-', 'CONSULTA', 17, NOW(6), 'sistema'),
(35, '-', 'CONSULTA', 18, NOW(6), 'sistema'),
(36, '-', 'CONSULTA', 19, NOW(6), 'sistema'),
(37, '-', 'CONSULTA', 20, NOW(6), 'sistema'),
(38, '-', 'CONSULTA', 23, NOW(6), 'sistema'),
(39, '-', 'CONSULTA', 25, NOW(6), 'sistema'),
(40, '-', 'CONSULTA', 27, NOW(6), 'sistema');

-- -----------------------------------------------------------------------------
-- Especialidades disponíveis por UBS
-- Todas as UBSs podem registrar encaminhamentos para as especialidades ativas.
-- -----------------------------------------------------------------------------
INSERT INTO basic_health_units_specialties
    (tenant_id, id_basic_health_unit, id_specialties)
SELECT
    bhu.tenant_id,
    bhu.id,
    specialty.id
FROM basic_health_units bhu
CROSS JOIN specialties specialty
WHERE specialty.active = TRUE;

-- =============================================================================
-- Massa de desenvolvimento
-- =============================================================================
-- Todos os nomes, CPFs, CNS, telefones, números e combinações de endereço abaixo
-- são sintéticos. Os bairros, logradouros, CEPs e áreas de cobertura foram
-- baseados em referências públicas territoriais:
--   Afogados:
--     https://www.ubsbrasil.org/cidade/afogados-da-ingazeira-pe
--     https://maispajeu.com.br/wp-content/uploads/2025/08/CEPS-AFOGADOS-DA-INGAZEIRA.pdf
--     https://afogadosdaingazeira.pe.gov.br/ler_noticia.php?id=158
--   Caruaru:
--     https://saudecaruaru.pe.gov.br/site/index.php/2018/08/03/unidades-de-saude/
--     https://www.ruacep.com.br/pe/caruaru/bairros/
--
-- A carga gera 180 pacientes distintos por tenant. Em cada tenant:
--   * pacientes 1 a 60: sem consulta ou histórico;
--   * pacientes 61 a 100: em fila, aguardando contemplação;
--   * pacientes 101 a 180: com desfechos e histórico variados.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Funções auxiliares temporárias para identificadores sintéticos válidos
-- -----------------------------------------------------------------------------
DROP FUNCTION IF EXISTS mock_seed_cpf;
DROP FUNCTION IF EXISTS mock_seed_cns;

DELIMITER $$

CREATE FUNCTION mock_seed_cpf(p_seed INT)
RETURNS CHAR(11)
DETERMINISTIC
BEGIN
    DECLARE raw_base CHAR(9);
    DECLARE raw_with_check CHAR(10);
    DECLARE digit_index INT DEFAULT 1;
    DECLARE weighted_sum INT DEFAULT 0;
    DECLARE first_check INT;
    DECLARE second_check INT;

    SET raw_base = LPAD(100000000 + MOD(p_seed * 7919, 899999999), 9, '0');

    WHILE digit_index <= 9 DO
        SET weighted_sum = weighted_sum
            + CAST(SUBSTRING(raw_base, digit_index, 1) AS UNSIGNED) * (11 - digit_index);
        SET digit_index = digit_index + 1;
    END WHILE;

    SET first_check = 11 - MOD(weighted_sum, 11);
    IF first_check >= 10 THEN
        SET first_check = 0;
    END IF;

    SET raw_with_check = CONCAT(raw_base, first_check);
    SET digit_index = 1;
    SET weighted_sum = 0;

    WHILE digit_index <= 10 DO
        SET weighted_sum = weighted_sum
            + CAST(SUBSTRING(raw_with_check, digit_index, 1) AS UNSIGNED) * (12 - digit_index);
        SET digit_index = digit_index + 1;
    END WHILE;

    SET second_check = 11 - MOD(weighted_sum, 11);
    IF second_check >= 10 THEN
        SET second_check = 0;
    END IF;

    RETURN CONCAT(raw_base, first_check, second_check);
END$$

CREATE FUNCTION mock_seed_cns(p_seed INT)
RETURNS VARCHAR(15)
DETERMINISTIC
BEGIN
    DECLARE raw_base CHAR(13);
    DECLARE digit_index INT DEFAULT 1;
    DECLARE weighted_sum INT DEFAULT 0;
    DECLARE suffix_candidate INT DEFAULT 0;
    DECLARE generated_cns VARCHAR(15) DEFAULT NULL;

    SET raw_base = CONCAT(
        '7',
        LPAD(100000000000 + MOD(CAST(p_seed AS UNSIGNED) * 104729, 899999999999), 12, '0')
    );

    WHILE digit_index <= 13 DO
        SET weighted_sum = weighted_sum
            + CAST(SUBSTRING(raw_base, digit_index, 1) AS UNSIGNED) * (16 - digit_index);
        SET digit_index = digit_index + 1;
    END WHILE;

    WHILE suffix_candidate <= 99 AND generated_cns IS NULL DO
        IF MOD(
            weighted_sum
            + FLOOR(suffix_candidate / 10) * 2
            + MOD(suffix_candidate, 10),
            11
        ) = 0 THEN
            SET generated_cns = CONCAT(raw_base, LPAD(suffix_candidate, 2, '0'));
        END IF;
        SET suffix_candidate = suffix_candidate + 1;
    END WHILE;

    RETURN generated_cns;
END$$

DELIMITER ;

-- -----------------------------------------------------------------------------
-- Tabelas temporárias usadas somente durante a inicialização
-- -----------------------------------------------------------------------------
CREATE TEMPORARY TABLE mock_numbers (
    n INT PRIMARY KEY
);

INSERT INTO mock_numbers (n)
SELECT ones.n + tens.n * 10 + hundreds.n * 100 + 1
FROM (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) ones
CROSS JOIN (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) tens
CROSS JOIN (
    SELECT 0 AS n UNION ALL SELECT 1
) hundreds
WHERE ones.n + tens.n * 10 + hundreds.n * 100 + 1 <= 180;

CREATE TEMPORARY TABLE mock_given_names (
    tenant_id BIGINT NOT NULL,
    name_id INT NOT NULL,
    given_name VARCHAR(100) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    PRIMARY KEY (tenant_id, name_id)
);

INSERT INTO mock_given_names (tenant_id, name_id, given_name, gender) VALUES
-- Afogados da Ingazeira
(1,  1, 'Ana Clara',          'Feminino'),
(1,  2, 'João Miguel',        'Masculino'),
(1,  3, 'Maria Eduarda',      'Feminino'),
(1,  4, 'José Lucas',         'Masculino'),
(1,  5, 'Francisca Vitória',  'Feminino'),
(1,  6, 'Antônio Gabriel',    'Masculino'),
(1,  7, 'Maria Cecília',      'Feminino'),
(1,  8, 'Pedro Henrique',     'Masculino'),
(1,  9, 'Rita de Cássia',     'Feminino'),
(1, 10, 'Carlos André',       'Masculino'),
(1, 11, 'Luzia Helena',       'Feminino'),
(1, 12, 'Francisco José',     'Masculino'),
(1, 13, 'Mariana Alves',      'Feminino'),
(1, 14, 'Paulo Roberto',      'Masculino'),
(1, 15, 'Tereza Cristina',    'Feminino'),
(1, 16, 'Luiz Fernando',      'Masculino'),
(1, 17, 'Beatriz Ramos',      'Feminino'),
(1, 18, 'Raimundo Nonato',    'Masculino'),
(1, 19, 'Isabel Cristina',    'Feminino'),
(1, 20, 'Marcos Vinícius',    'Masculino'),
(1, 21, 'Joana Darc',         'Feminino'),
(1, 22, 'Sebastião Antônio',  'Masculino'),
(1, 23, 'Laura Gabriela',     'Feminino'),
(1, 24, 'Mateus Felipe',      'Masculino'),
(1, 25, 'Sandra Regina',      'Feminino'),
(1, 26, 'Cícero Romão',       'Masculino'),
(1, 27, 'Mônica Patrícia',    'Feminino'),
(1, 28, 'Edson Pereira',      'Masculino'),
(1, 29, 'Aparecida de Fátima','Feminino'),
(1, 30, 'Manoel Joaquim',     'Masculino'),
(1, 31, 'Eliane Márcia',      'Feminino'),
(1, 32, 'Geraldo Magela',     'Masculino'),
(1, 33, 'Socorro Maria',      'Feminino'),
(1, 34, 'Adriano César',      'Masculino'),
(1, 35, 'Nayara Emilly',      'Feminino'),
(1, 36, 'Damião Alexandre',   'Masculino'),
(1, 37, 'Valéria Lúcia',      'Feminino'),
(1, 38, 'Renato Augusto',     'Masculino'),
(1, 39, 'Emanuela Sofia',     'Feminino'),
(1, 40, 'Joaquim Batista',    'Masculino'),
-- Caruaru
(2,  1, 'Alice Beatriz',      'Feminino'),
(2,  2, 'Bento Augusto',      'Masculino'),
(2,  3, 'Júlia Helena',       'Feminino'),
(2,  4, 'Enzo Rafael',        'Masculino'),
(2,  5, 'Lorena Valentina',   'Feminino'),
(2,  6, 'Arthur Davi',        'Masculino'),
(2,  7, 'Camila Fernanda',    'Feminino'),
(2,  8, 'Miguel Ângelo',      'Masculino'),
(2,  9, 'Débora Raquel',      'Feminino'),
(2, 10, 'Samuel Matheus',     'Masculino'),
(2, 11, 'Clarice Lis',        'Feminino'),
(2, 12, 'Heitor Guilherme',   'Masculino'),
(2, 13, 'Márcia Valéria',     'Feminino'),
(2, 14, 'Bruno Leonardo',     'Masculino'),
(2, 15, 'Sônia Regina',       'Feminino'),
(2, 16, 'Cláudio Henrique',   'Masculino'),
(2, 17, 'Yasmin Eloá',        'Feminino'),
(2, 18, 'Danilo Murilo',      'Masculino'),
(2, 19, 'Cristiane Moura',    'Feminino'),
(2, 20, 'Rodrigo César',      'Masculino'),
(2, 21, 'Neide Aparecida',    'Feminino'),
(2, 22, 'Márcio Alexandre',   'Masculino'),
(2, 23, 'Rebeca Ester',       'Feminino'),
(2, 24, 'Vitor Hugo',         'Masculino'),
(2, 25, 'Patrícia Aline',     'Feminino'),
(2, 26, 'Wellington José',    'Masculino'),
(2, 27, 'Mirela Vitória',     'Feminino'),
(2, 28, 'Diego Ramon',        'Masculino'),
(2, 29, 'Graça Emília',       'Feminino'),
(2, 30, 'Flávio Eduardo',     'Masculino'),
(2, 31, 'Renata Mirele',      'Feminino'),
(2, 32, 'Sérgio Ricardo',     'Masculino'),
(2, 33, 'Karla Simone',       'Feminino'),
(2, 34, 'Leandro William',    'Masculino'),
(2, 35, 'Talita Roberta',     'Feminino'),
(2, 36, 'Alex Sandro',        'Masculino'),
(2, 37, 'Priscila Mayara',    'Feminino'),
(2, 38, 'Fábio Júnior',       'Masculino'),
(2, 39, 'Gabriela Manuela',   'Feminino'),
(2, 40, 'Otávio Lorenzo',     'Masculino');

CREATE TEMPORARY TABLE mock_surnames (
    tenant_id BIGINT NOT NULL,
    surname_id INT NOT NULL,
    surname VARCHAR(80) NOT NULL,
    PRIMARY KEY (tenant_id, surname_id)
);

INSERT INTO mock_surnames (tenant_id, surname_id, surname) VALUES
-- Afogados da Ingazeira
(1,  1, 'da Silva'),       (1,  2, 'dos Santos'),    (1,  3, 'de Oliveira'),
(1,  4, 'de Souza'),       (1,  5, 'Pereira'),       (1,  6, 'Ferreira'),
(1,  7, 'da Costa'),       (1,  8, 'de Almeida'),    (1,  9, 'Lima'),
(1, 10, 'Gomes'),          (1, 11, 'Rodrigues'),     (1, 12, 'de Carvalho'),
(1, 13, 'do Nascimento'),  (1, 14, 'Araújo'),        (1, 15, 'Barbosa'),
(1, 16, 'Ribeiro'),        (1, 17, 'Alves'),         (1, 18, 'Bezerra'),
(1, 19, 'Marques'),        (1, 20, 'Lopes'),         (1, 21, 'Dantas'),
(1, 22, 'Siqueira'),       (1, 23, 'Veras'),         (1, 24, 'de Morais'),
-- Caruaru
(2,  1, 'de Melo'),        (2,  2, 'Cavalcanti'),    (2,  3, 'Tabosa'),
(2,  4, 'Lira'),           (2,  5, 'Monteiro'),      (2,  6, 'Torres'),
(2,  7, 'Albuquerque'),    (2,  8, 'Barros'),        (2,  9, 'Nunes'),
(2, 10, 'Rocha'),          (2, 11, 'Farias'),        (2, 12, 'Correia'),
(2, 13, 'Andrade'),        (2, 14, 'Tavares'),       (2, 15, 'Leite'),
(2, 16, 'Matos'),          (2, 17, 'Vieira'),        (2, 18, 'de Freitas'),
(2, 19, 'Teixeira'),       (2, 20, 'Batista'),       (2, 21, 'Florêncio'),
(2, 22, 'Ramalho'),        (2, 23, 'Cordeiro'),      (2, 24, 'Galdino');

-- O MySQL não permite reabrir a mesma tabela temporária duas vezes no mesmo
-- SELECT; a cópia abaixo representa a segunda posição de sobrenome.
CREATE TEMPORARY TABLE mock_second_surnames LIKE mock_surnames;

INSERT INTO mock_second_surnames (tenant_id, surname_id, surname)
SELECT tenant_id, surname_id, surname
FROM mock_surnames;

CREATE TEMPORARY TABLE mock_streets (
    tenant_id BIGINT NOT NULL,
    ubs_id BIGINT NOT NULL,
    street_id INT NOT NULL,
    street VARCHAR(255) NOT NULL,
    neighborhood VARCHAR(100) NOT NULL,
    cep VARCHAR(9) NOT NULL,
    PRIMARY KEY (tenant_id, ubs_id, street_id)
);

INSERT INTO mock_streets
    (tenant_id, ubs_id, street_id, street, neighborhood, cep)
VALUES
-- Afogados da Ingazeira — São Francisco
(1, 1, 1, 'Rua Sete de Setembro',                         'São Francisco',        '56812-302'),
(1, 1, 2, 'Rua Abílio Barbosa Albuquerque',               'São Francisco',        '56812-329'),
(1, 1, 3, 'Rua Cândido Pedro da Silva',                   'São Francisco',        '56812-359'),
(1, 1, 4, 'Rua da Linha',                                 'São Francisco',        '56812-281'),
(1, 1, 5, 'Rua Epitácio da Silva Ramos',                  'São Francisco',        '56812-278'),
-- Afogados da Ingazeira — São Brás
(1, 2, 1, 'Rua Valdecir Xavier de Menezes',                'São Brás',             '56808-288'),
(1, 2, 2, 'Rua Nelson João de Siqueira',                  'São Brás',             '56808-345'),
(1, 2, 3, 'Rua Olavo Bilac',                              'São Brás',             '56808-312'),
(1, 2, 4, 'Rua Padre Antônio de Paula Santos',             'São Brás',             '56808-294'),
(1, 2, 5, 'Rua Poeta João Paraibano',                     'São Brás',             '56808-369'),
-- Afogados da Ingazeira — cobertura Borges e Brotas
(1, 3, 1, 'Rua Projetada 19',                             'Borges',                '56804-060'),
(1, 3, 2, 'Rua Projetada 20',                             'Borges',                '56804-053'),
(1, 3, 3, 'Avenida Severino Pedro de Carvalho',            'Brotas',                '56804-292'),
(1, 3, 4, 'Rua São Francisco',                            'Brotas',                '56804-316'),
(1, 3, 5, 'Rua Aristheu Veras Cruz Campos',               'Brotas',                '56804-229'),
-- Afogados da Ingazeira — cobertura São Sebastião e Costa
(1, 4, 1, 'Rua Isídio Leite',                             'São Sebastião',         '56800-000'),
(1, 4, 2, 'Avenida José Valdemar de Almeida',              'Costa',                 '56800-705'),
(1, 4, 3, 'Rua Jasmelina Maria da Conceição',             'Costa',                 '56800-681'),
(1, 4, 4, 'Rua João Braz Almeida Lima',                   'Costa',                 '56800-663'),
(1, 4, 5, 'Rua José Barbosa de Oliveira',                 'Costa',                 '56800-693'),
-- Afogados da Ingazeira — cobertura Sobreira e São Cristóvão
(1, 5, 1, 'Avenida José Barbosa da Silva',                 'Sobreira',              '56808-634'),
(1, 5, 2, 'Rua Andrelino Mendes da Silva',                'Sobreira',              '56808-700'),
(1, 5, 3, 'Rua Berta Celi Lemos Liberal',                 'Sobreira',              '56808-637'),
(1, 5, 4, 'Rua Virgília Gomes de Almeida',                'São Cristóvão',         '56812-024'),
(1, 5, 5, 'Travessa José Alves de Oliveira',              'São Cristóvão',         '56812-021'),
-- Afogados da Ingazeira — Padre Pedro Pereira
(1, 6, 1, 'Rua Regina Pereira da Silva',                  'Padre Pedro Pereira',   '56812-573'),
(1, 6, 2, 'Rua Santa Catarina',                           'Padre Pedro Pereira',   '56812-884'),
(1, 6, 3, 'Rua Santa Rosa',                               'Padre Pedro Pereira',   '56812-899'),
(1, 6, 4, 'Rua São João',                                 'Padre Pedro Pereira',   '56812-651'),
(1, 6, 5, 'Rua São Pedro',                                'Padre Pedro Pereira',   '56812-648'),
-- Afogados da Ingazeira — Planalto / Manoela Valadares
(1, 7, 1, 'Rua Edson Barbosa de Araújo',                  'Planalto',              '56804-635'),
(1, 7, 2, 'Rua Ernesto Marinho de Lima',                  'Planalto',              '56804-647'),
(1, 7, 3, 'Rua João Alves Guimarães',                     'Planalto',              '56804-638'),
(1, 7, 4, 'Rua João Domingos Sobrinho',                   'Planalto',              '56804-644'),
(1, 7, 5, 'Rua Margarida Martins',                        'Planalto',              '56804-632'),
-- Afogados da Ingazeira — polo rural da Varzinha
(1, 8, 1, 'Povoado da Varzinha',                          'Zona Rural',            '56815-899'),
(1, 8, 2, 'Sítio Curral Velho',                           'Zona Rural',            '56815-899'),
(1, 8, 3, 'Sítio Pau Ferro',                              'Zona Rural',            '56815-899'),
(1, 8, 4, 'Sítio Poço do Veado',                          'Zona Rural',            '56815-899'),
(1, 8, 5, 'Sítio Riacho da Onça',                         'Zona Rural',            '56815-899'),
-- Caruaru — Agamenon Magalhães
(2, 101, 1, 'Rua Marieta Cruz',                            'Agamenon Magalhães',    '55031-070'),
(2, 101, 2, 'Rua Arquimedes de Oliveira',                 'Agamenon Magalhães',    '55032-420'),
(2, 101, 3, 'Rua Pedro Paulo da Silva Filho',             'Agamenon Magalhães',    '55032-550'),
(2, 101, 4, 'Rua Severino de Lima Sá',                    'Agamenon Magalhães',    '55034-150'),
(2, 101, 5, 'Rua Capitão PM Israel Rodrigues de Figueiredo','Agamenon Magalhães',  '55034-700'),
-- Caruaru — Caiucá
(2, 102, 1, 'Rua São Joaquim do Monte',                   'Caiucá',                '55034-480'),
(2, 102, 2, 'Rua Plácido de Castro',                      'Caiucá',                '55034-215'),
(2, 102, 3, 'Rua Demócrito de Souza',                     'Caiucá',                '55034-230'),
(2, 102, 4, 'Rua Caracas',                                'Caiucá',                '55034-450'),
(2, 102, 5, 'Rua Neil Armstrong',                         'Caiucá',                '55034-530'),
-- Caruaru — Centenário
(2, 103, 1, 'Rua da União',                               'Centenário',            '55008-250'),
(2, 103, 2, 'Rua Sebastião Alves Correia',                'Centenário',            '55008-385'),
(2, 103, 3, 'Rua São Caetano',                            'Centenário',            '55008-400'),
(2, 103, 4, 'Travessa Professora Maria Emília',           'Centenário',            '55008-431'),
(2, 103, 5, 'Rua do Cruzeiro',                            'Centenário',            '55008-455'),
-- Caruaru — Cidade Jardim
(2, 104, 1, 'Rua José Vieira de Lima',                    'Cidade Jardim',         '55021-250'),
(2, 104, 2, 'Rua Everaldo Cordeiro de Souza',             'Cidade Jardim',         '55021-255'),
(2, 104, 3, 'Avenida Coronel Jurandyr Galindo',            'Cidade Jardim',         '55021-260'),
(2, 104, 4, 'Avenida Gonçalo Nunes de Oliveira',           'Cidade Jardim',         '55021-265'),
(2, 104, 5, 'Rua Manoel Abílio da Silva',                 'Cidade Jardim',         '55021-275'),
-- Caruaru — Rendeiras
(2, 105, 1, 'Rua Ricardo Gervásio',                        'Rendeiras',              '55022-000'),
(2, 105, 2, 'Rua Jaime Bezerra da Silva',                 'Rendeiras',              '55022-010'),
(2, 105, 3, 'Rua José Gomes Irmão',                       'Rendeiras',              '55022-040'),
(2, 105, 4, 'Rua João Wanderley Neto',                    'Rendeiras',              '55022-070'),
(2, 105, 5, 'Rua Major João Coelho',                      'Rendeiras',              '55022-220'),
-- Caruaru — Salgado
(2, 106, 1, 'Rua Admilson José',                           'Salgado',                '55018-720'),
(2, 106, 2, 'Rua Adolfo Bezerra Cavalcante',              'Salgado',                '55020-355'),
(2, 106, 3, 'Rua Alexandrino Alencar',                    'Salgado',                '55020-215'),
(2, 106, 4, 'Rua Antônio Alves da Silva Gomes',            'Salgado',                '55016-035'),
(2, 106, 5, 'Rua Antônio Carlos de Oliveira',              'Salgado',                '55018-540'),
-- Caruaru — Santa Rosa
(2, 107, 1, 'Rua Alcides Arquedas',                        'Santa Rosa',             '55026-250'),
(2, 107, 2, 'Rua Alexandrina Fernandes Gomes',            'Santa Rosa',             '55026-275'),
(2, 107, 3, 'Rua Alfredo Pinto',                          'Santa Rosa',             '55028-130'),
(2, 107, 4, 'Rua Altemar Dutra',                          'Santa Rosa',             '55026-095'),
(2, 107, 5, 'Avenida Bento Ribeiro',                       'Santa Rosa',             '55028-300'),
-- Caruaru — São João da Escócia
(2, 108, 1, 'Rua Adelmo Fontoura',                         'São João da Escócia',    '55019-330'),
(2, 108, 2, 'Rua Almir Afonso',                           'São João da Escócia',    '55019-090'),
(2, 108, 3, 'Rua Antônio Laurentino',                     'São João da Escócia',    '55019-065'),
(2, 108, 4, 'Rua Bibiano Alves Lagos',                    'São João da Escócia',    '55019-053'),
(2, 108, 5, 'Rua Capitão Nilo Ferreira da Costa',         'São João da Escócia',    '55019-365');

-- -----------------------------------------------------------------------------
-- Pacientes — 180 por tenant, distribuídos igualmente entre oito territórios
-- -----------------------------------------------------------------------------
INSERT INTO patients (
    id,
    tenant_id,
    name,
    birth_date,
    gender,
    social_sit_rating,
    sus_card_number,
    cpf,
    phone_number,
    address_street,
    address_number,
    address_complement,
    address_ref,
    acs_name,
    id_basic_health_unit,
    creation_date,
    creation_user,
    update_date,
    update_user
)
SELECT
    CASE WHEN tenant.tenant_id = 1 THEN number.n ELSE 1000 + number.n END AS id,
    tenant.tenant_id,
    CONCAT(given.given_name, ' ', first_surname.surname, ' ', second_surname.surname) AS name,
    DATE_ADD(
        '1938-01-01',
        INTERVAL MOD(number.n * 173 + tenant.tenant_id * 997, 31000) DAY
    ) AS birth_date,
    given.gender,
    CASE
        WHEN MOD(number.n * 3 + tenant.tenant_id, 20) IN (0, 1, 2, 3) THEN 1
        WHEN MOD(number.n * 3 + tenant.tenant_id, 20) IN (4, 5, 6, 7) THEN 2
        WHEN MOD(number.n * 3 + tenant.tenant_id, 20) IN (8, 9, 10, 11, 12) THEN 3
        WHEN MOD(number.n * 3 + tenant.tenant_id, 20) IN (13, 14, 15) THEN 4
        WHEN MOD(number.n * 3 + tenant.tenant_id, 20) IN (16, 17) THEN 5
        WHEN MOD(number.n * 3 + tenant.tenant_id, 20) = 18 THEN 6
        ELSE 7
    END AS social_sit_rating,
    mock_seed_cns(tenant.tenant_id * 1000 + number.n) AS sus_card_number,
    mock_seed_cpf(tenant.tenant_id * 1000 + number.n) AS cpf,
    CONCAT(
        CASE WHEN tenant.tenant_id = 1 THEN '87' ELSE '81' END,
        '9',
        LPAD(10000000 + MOD((tenant.tenant_id * 1000 + number.n) * 3571, 89999999), 8, '0')
    ) AS phone_number,
    street.street,
    CASE
        WHEN MOD(number.n, 23) = 0 THEN 'S/N'
        ELSE CAST(10 + MOD(number.n * 37 + tenant.tenant_id * 19, 990) AS CHAR)
    END AS address_number,
    CONCAT(
        CASE MOD(number.n, 6)
            WHEN 0 THEN 'Casa'
            WHEN 1 THEN 'Casa térrea'
            WHEN 2 THEN 'Fundos'
            WHEN 3 THEN CONCAT('Apto. ', 10 + MOD(number.n, 40))
            WHEN 4 THEN CONCAT('Bloco ', CHAR(65 + MOD(number.n, 6)))
            ELSE 'Próximo à esquina'
        END,
        ' — Bairro/Localidade: ', street.neighborhood,
        ' — CEP: ', street.cep
    ) AS address_complement,
    CASE MOD(number.n, 6)
        WHEN 0 THEN 'Próximo à unidade de saúde'
        WHEN 1 THEN 'Próximo à escola municipal'
        WHEN 2 THEN 'Após a praça principal'
        WHEN 3 THEN 'Ao lado da associação de moradores'
        WHEN 4 THEN 'Próximo ao ponto final de ônibus'
        ELSE 'Em frente ao mercado do bairro'
    END AS address_ref,
    CASE
        WHEN tenant.tenant_id = 1 THEN
            CASE street.ubs_id
                WHEN 1 THEN 'Aline Bezerra dos Santos'
                WHEN 2 THEN 'Joana Dantas de Lima'
                WHEN 3 THEN 'Marcos Veras Pereira'
                WHEN 4 THEN 'Luciana Siqueira Gomes'
                WHEN 5 THEN 'Renata Alves de Souza'
                WHEN 6 THEN 'Célia Maria Nascimento'
                WHEN 7 THEN 'Paulo Henrique Ferreira'
                ELSE 'Rosa de Cássia Oliveira'
            END
        ELSE
            CASE street.ubs_id
                WHEN 101 THEN 'Marta Lira Cavalcanti'
                WHEN 102 THEN 'André Monteiro de Melo'
                WHEN 103 THEN 'Simone Barros Tabosa'
                WHEN 104 THEN 'Daniela Torres Nunes'
                WHEN 105 THEN 'Ricardo Florêncio Matos'
                WHEN 106 THEN 'Cláudia Tavares Rocha'
                WHEN 107 THEN 'Ester Albuquerque Farias'
                ELSE 'Wesley Cordeiro Galdino'
            END
    END AS acs_name,
    street.ubs_id,
    DATE_SUB(NOW(6), INTERVAL (30 + MOD(number.n * 13 + tenant.tenant_id * 41, 900)) DAY),
    'seed-dev',
    CASE
        WHEN MOD(number.n, 5) = 0
            THEN DATE_SUB(NOW(6), INTERVAL MOD(number.n * 7, 29) DAY)
        ELSE NULL
    END,
    CASE WHEN MOD(number.n, 5) = 0 THEN 'seed-dev' ELSE NULL END
FROM mock_numbers number
CROSS JOIN (
    SELECT 1 AS tenant_id
    UNION ALL
    SELECT 2 AS tenant_id
) tenant
JOIN mock_given_names given
    ON given.tenant_id = tenant.tenant_id
    AND given.name_id = 1 + MOD(number.n - 1, 40)
JOIN mock_surnames first_surname
    ON first_surname.tenant_id = tenant.tenant_id
    AND first_surname.surname_id = 1 + MOD(number.n * 7, 24)
JOIN mock_second_surnames second_surname
    ON second_surname.tenant_id = tenant.tenant_id
    AND second_surname.surname_id = 1 + MOD(
        MOD(number.n * 7, 24) + 5 + MOD(number.n, 17),
        24
    )
JOIN mock_streets street
    ON street.tenant_id = tenant.tenant_id
    AND street.ubs_id = CASE
        WHEN tenant.tenant_id = 1 THEN 1 + MOD(number.n - 1, 8)
        ELSE 101 + MOD(number.n - 1, 8)
    END
    AND street.street_id = 1 + MOD(FLOOR((number.n - 1) / 8), 5);

-- -----------------------------------------------------------------------------
-- Vagas médicas históricas e atuais
-- São oito competências por UBS: mês atual e os sete meses anteriores.
-- -----------------------------------------------------------------------------
CREATE TEMPORARY TABLE mock_month_offsets (
    month_offset INT PRIMARY KEY
);

INSERT INTO mock_month_offsets (month_offset) VALUES
(0), (1), (2), (3), (4), (5), (6), (7);

CREATE TEMPORARY TABLE mock_slot_procedures (
    procedure_bucket INT PRIMARY KEY,
    medical_procedure_id BIGINT NOT NULL
);

INSERT INTO mock_slot_procedures (procedure_bucket, medical_procedure_id) VALUES
(0, 19), -- Consulta em Cardiologia
(1, 21), -- Consulta em Endocrinologia
(2, 24), -- Consulta em Oftalmologia
(3, 25), -- Consulta em Ortopedia
(4, 35), -- Consulta em Pediatria
(5,  6), -- Mamografia
(6, 12), -- Raio X
(7,  3); -- Cirurgia de Hérnia

INSERT INTO medical_slots (
    id,
    tenant_id,
    reference_month,
    total_slots,
    current_slots,
    id_medical_procedure,
    id_basic_health_unit,
    creation_date,
    creation_user,
    update_date,
    update_user
)
SELECT
    CASE
        WHEN bhu.tenant_id = 1
            THEN 1 + (bhu.id - 1) * 8 + month_ref.month_offset
        ELSE 1001 + (bhu.id - 101) * 8 + month_ref.month_offset
    END AS id,
    bhu.tenant_id,
    DATE_SUB(
        CAST(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01') AS DATE),
        INTERVAL month_ref.month_offset MONTH
    ) AS reference_month,
    2 + MOD(
        (CASE WHEN bhu.tenant_id = 1 THEN bhu.id ELSE bhu.id - 100 END) * 3
        + month_ref.month_offset,
        4
    ) AS total_slots,
    2 + MOD(
        (CASE WHEN bhu.tenant_id = 1 THEN bhu.id ELSE bhu.id - 100 END) * 3
        + month_ref.month_offset,
        4
    ) AS current_slots,
    slot_procedure.medical_procedure_id,
    bhu.id,
    DATE_SUB(
        DATE_SUB(
            CAST(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01') AS DATE),
            INTERVAL month_ref.month_offset MONTH
        ),
        INTERVAL (20 + MOD(bhu.id + month_ref.month_offset * 7, 35)) DAY
    ) AS creation_date,
    'Regulação Municipal',
    NULL,
    NULL
FROM basic_health_units bhu
CROSS JOIN mock_month_offsets month_ref
JOIN mock_slot_procedures slot_procedure
    ON slot_procedure.procedure_bucket = MOD(
        (CASE WHEN bhu.tenant_id = 1 THEN bhu.id - 1 ELSE bhu.id - 101 END)
        + month_ref.month_offset,
        8
    );

-- -----------------------------------------------------------------------------
-- Plano determinístico de consultas e históricos
-- -----------------------------------------------------------------------------
CREATE TEMPORARY TABLE mock_appointment_plan (
    appointment_id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    patient_number INT NOT NULL,
    month_offset INT NOT NULL,
    medical_slot_id BIGINT NOT NULL,
    medical_procedure_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority INT NOT NULL,
    reference_month DATE NOT NULL,
    request_date DATETIME(6),
    contemplation_date DATETIME(6),
    update_date DATETIME(6)
);

INSERT INTO mock_appointment_plan (
    appointment_id,
    tenant_id,
    patient_id,
    patient_number,
    month_offset,
    medical_slot_id,
    medical_procedure_id,
    status,
    priority,
    reference_month
)
SELECT
    CASE
        WHEN planned.tenant_id = 1 THEN planned.patient_number - 60
        ELSE 1000 + planned.patient_number - 60
    END AS appointment_id,
    planned.tenant_id,
    CASE
        WHEN planned.tenant_id = 1 THEN planned.patient_number
        ELSE 1000 + planned.patient_number
    END AS patient_id,
    planned.patient_number,
    planned.month_offset,
    slot.id,
    slot.id_medical_procedure,
    CASE
        WHEN planned.patient_number BETWEEN 61 AND 100
            THEN 'Aguardando Contemplação'
        WHEN planned.patient_number BETWEEN 101 AND 125
            THEN 'Atendimento Concluído'
        WHEN planned.patient_number BETWEEN 126 AND 140
            THEN 'Presença Confirmada'
        WHEN planned.patient_number BETWEEN 141 AND 150
            THEN 'Paciente Contemplado'
        WHEN planned.patient_number BETWEEN 151 AND 160
            THEN 'Contemplação Cancelada'
        WHEN planned.patient_number BETWEEN 161 AND 170
            THEN 'Desistência do Paciente'
        ELSE 'Atendimento Concluído'
    END AS status,
    CASE MOD(planned.patient_number + planned.tenant_id, 10)
        WHEN 0 THEN 2 -- Urgência
        WHEN 1 THEN 3 -- Retorno
        WHEN 2 THEN 4 -- Prioritário
        WHEN 3 THEN 4
        ELSE 8        -- Eletivo
    END AS priority,
    slot.reference_month
FROM (
    SELECT
        tenant.tenant_id,
        number.n AS patient_number,
        CASE
            WHEN number.n BETWEEN 61 AND 100 THEN 0
            WHEN number.n BETWEEN 101 AND 125 THEN 2 + MOD(number.n, 6)
            WHEN number.n BETWEEN 126 AND 140 THEN MOD(number.n, 2)
            WHEN number.n BETWEEN 141 AND 150 THEN 0
            WHEN number.n BETWEEN 151 AND 160 THEN MOD(number.n, 2)
            WHEN number.n BETWEEN 161 AND 170 THEN 1 + MOD(number.n, 7)
            ELSE 2 + MOD(number.n, 6)
        END AS month_offset
    FROM mock_numbers number
    CROSS JOIN (
        SELECT 1 AS tenant_id
        UNION ALL
        SELECT 2 AS tenant_id
    ) tenant
    WHERE number.n >= 61
) planned
JOIN medical_slots slot
    ON slot.id = CASE
        WHEN planned.tenant_id = 1
            THEN 1 + MOD(planned.patient_number - 1, 8) * 8 + planned.month_offset
        ELSE 1001 + MOD(planned.patient_number - 1, 8) * 8 + planned.month_offset
    END;

UPDATE mock_appointment_plan
SET contemplation_date = CASE
    WHEN month_offset = 0
        THEN DATE_SUB(NOW(6), INTERVAL (1 + MOD(patient_number, 12)) DAY)
    ELSE DATE_ADD(reference_month, INTERVAL (8 + MOD(patient_number, 18)) DAY)
END;

UPDATE mock_appointment_plan
SET request_date = CASE
    WHEN status = 'Aguardando Contemplação'
        THEN DATE_SUB(NOW(6), INTERVAL (5 + MOD(patient_number * 11 + tenant_id * 17, 260)) DAY)
    ELSE DATE_SUB(
        contemplation_date,
        INTERVAL (15 + MOD(patient_number * 7 + tenant_id * 13, 150)) DAY
    )
END;

UPDATE mock_appointment_plan
SET update_date = CASE
    WHEN status = 'Aguardando Contemplação' THEN NULL
    WHEN status = 'Atendimento Concluído'
        THEN DATE_ADD(contemplation_date, INTERVAL (3 + MOD(patient_number, 8)) DAY)
    WHEN status = 'Presença Confirmada'
        THEN LEAST(
            DATE_SUB(NOW(6), INTERVAL 1 HOUR),
            DATE_ADD(contemplation_date, INTERVAL 1 DAY)
        )
    WHEN status = 'Paciente Contemplado' THEN contemplation_date
    WHEN status = 'Contemplação Cancelada'
        THEN LEAST(
            DATE_SUB(NOW(6), INTERVAL 1 HOUR),
            DATE_ADD(contemplation_date, INTERVAL (1 + MOD(patient_number, 3)) DAY)
        )
    ELSE DATE_ADD(request_date, INTERVAL (2 + MOD(patient_number, 12)) DAY)
END;

-- -----------------------------------------------------------------------------
-- Consultas/agendamentos
-- -----------------------------------------------------------------------------
INSERT INTO appointments (
    id,
    tenant_id,
    request_date,
    priority,
    observation,
    status,
    creation_user,
    update_date,
    update_user,
    id_medical_procedure,
    id_patient,
    id_contemplation
)
SELECT
    plan.appointment_id,
    plan.tenant_id,
    plan.request_date,
    plan.priority,
    CASE
        WHEN plan.status = 'Aguardando Contemplação' THEN
            CASE MOD(plan.patient_number, 5)
                WHEN 0 THEN 'Encaminhamento para avaliação especializada; paciente disponível no turno da manhã.'
                WHEN 1 THEN 'Solicitação inserida pela equipe da UBS após avaliação clínica.'
                WHEN 2 THEN 'Paciente relata sintomas persistentes e aguarda contato da regulação.'
                WHEN 3 THEN 'Retorno solicitado pela equipe assistencial; documentos conferidos.'
                ELSE 'Encaminhamento eletivo registrado durante consulta na atenção básica.'
            END
        WHEN plan.status = 'Atendimento Concluído'
            THEN 'Atendimento realizado; orientação de retorno pela UBS em caso de necessidade.'
        WHEN plan.status = 'Presença Confirmada'
            THEN 'Paciente confirmou presença e recebeu as orientações de preparo.'
        WHEN plan.status = 'Paciente Contemplado'
            THEN 'Paciente comunicado; aguardando confirmação da presença.'
        WHEN plan.status = 'Contemplação Cancelada'
            THEN 'Vaga devolvida à regulação após impossibilidade de comparecimento.'
        ELSE 'Paciente solicitou retirada da fila após reavaliação na UBS.'
    END AS observation,
    plan.status,
    CASE
        WHEN MOD(plan.patient_number, 4) = 0 THEN 'Atendente da UBS'
        WHEN MOD(plan.patient_number, 4) = 1 THEN 'Enfermagem da UBS'
        WHEN MOD(plan.patient_number, 4) = 2 THEN 'Regulação Municipal'
        ELSE 'Agente Comunitário de Saúde'
    END AS creation_user,
    plan.update_date,
    CASE WHEN plan.update_date IS NULL THEN NULL ELSE 'Regulação Municipal' END,
    plan.medical_procedure_id,
    plan.patient_id,
    NULL
FROM mock_appointment_plan plan;

-- -----------------------------------------------------------------------------
-- Contemplações: automáticas e administrativas, atuais e históricas
-- -----------------------------------------------------------------------------
INSERT INTO contemplations (
    id,
    tenant_id,
    contemplation_date,
    contemplated_by,
    id_available_medical_slot,
    observation,
    creation_date,
    creation_user,
    update_date,
    update_user
)
SELECT
    plan.appointment_id,
    plan.tenant_id,
    plan.contemplation_date,
    CASE MOD(plan.patient_number + plan.tenant_id, 9)
        WHEN 0 THEN 1  -- Mais de quatro meses
        WHEN 1 THEN 2  -- Urgência
        WHEN 2 THEN 3  -- Retorno
        WHEN 3 THEN 4  -- Prioritário
        WHEN 4 THEN 5  -- Idade
        WHEN 5 THEN 6  -- Situação social
        WHEN 6 THEN 7  -- Gênero
        WHEN 7 THEN 9  -- Administrativo
        ELSE 10        -- Data da marcação
    END AS contemplated_by,
    plan.medical_slot_id,
    CASE
        WHEN plan.status = 'Contemplação Cancelada'
            THEN 'Cancelado pela regulação: paciente informou indisponibilidade na data ofertada.'
        WHEN MOD(plan.patient_number, 4) = 0
            THEN 'Contemplação automática conforme critérios de prioridade da fila.'
        WHEN MOD(plan.patient_number, 4) = 1
            THEN 'Paciente avisado por ligação telefônica pela equipe da UBS.'
        WHEN MOD(plan.patient_number, 4) = 2
            THEN 'Paciente avisado por mensagem e visita do agente comunitário.'
        ELSE 'Contemplação registrada pela Central de Regulação Municipal.'
    END,
    plan.contemplation_date,
    CASE WHEN MOD(plan.patient_number, 4) = 3 THEN 'Regulação Municipal' ELSE 'ROTINA' END,
    CASE
        WHEN plan.status IN ('Presença Confirmada', 'Contemplação Cancelada', 'Atendimento Concluído')
            THEN plan.update_date
        ELSE NULL
    END,
    CASE
        WHEN plan.status IN ('Presença Confirmada', 'Contemplação Cancelada', 'Atendimento Concluído')
            THEN 'Regulação Municipal'
        ELSE NULL
    END
FROM mock_appointment_plan plan
WHERE plan.status IN (
    'Paciente Contemplado',
    'Presença Confirmada',
    'Contemplação Cancelada',
    'Atendimento Concluído'
);

UPDATE appointments appointment
JOIN contemplations contemplation
    ON contemplation.id = appointment.id
    AND contemplation.tenant_id = appointment.tenant_id
SET appointment.id_contemplation = contemplation.id;

-- -----------------------------------------------------------------------------
-- Histórico de transições de status
-- -----------------------------------------------------------------------------
INSERT INTO appointment_status_history (
    tenant_id,
    status,
    id_appointment,
    creation_date,
    creation_user
)
SELECT
    plan.tenant_id,
    'Aguardando Contemplação',
    plan.appointment_id,
    plan.request_date,
    'seed-dev'
FROM mock_appointment_plan plan;

INSERT INTO appointment_status_history (
    tenant_id,
    status,
    id_appointment,
    creation_date,
    creation_user
)
SELECT
    plan.tenant_id,
    'Paciente Contemplado',
    plan.appointment_id,
    plan.contemplation_date,
    'ROTINA'
FROM mock_appointment_plan plan
WHERE plan.status IN (
    'Paciente Contemplado',
    'Presença Confirmada',
    'Contemplação Cancelada',
    'Atendimento Concluído'
);

INSERT INTO appointment_status_history (
    tenant_id,
    status,
    id_appointment,
    creation_date,
    creation_user
)
SELECT
    plan.tenant_id,
    'Presença Confirmada',
    plan.appointment_id,
    CASE
        WHEN plan.status = 'Atendimento Concluído'
            THEN DATE_ADD(plan.contemplation_date, INTERVAL 1 DAY)
        ELSE plan.update_date
    END,
    'Regulação Municipal'
FROM mock_appointment_plan plan
WHERE plan.status IN ('Presença Confirmada', 'Atendimento Concluído');

INSERT INTO appointment_status_history (
    tenant_id,
    status,
    id_appointment,
    creation_date,
    creation_user
)
SELECT
    plan.tenant_id,
    plan.status,
    plan.appointment_id,
    plan.update_date,
    'Regulação Municipal'
FROM mock_appointment_plan plan
WHERE plan.status IN (
    'Atendimento Concluído',
    'Contemplação Cancelada',
    'Desistência do Paciente'
);

-- A tabela legada patient_history continua populada para testes de compatibilidade.
INSERT INTO patient_history (
    tenant_id,
    id_appointment,
    creation_date,
    creation_user,
    update_date,
    update_user
)
SELECT
    plan.tenant_id,
    plan.appointment_id,
    COALESCE(plan.update_date, plan.request_date),
    'seed-dev',
    NULL,
    NULL
FROM mock_appointment_plan plan
WHERE plan.status <> 'Aguardando Contemplação';

-- -----------------------------------------------------------------------------
-- Saldo das vagas após o consumo histórico
-- Contemplações canceladas devolvem a vaga, reproduzindo o comportamento do app.
-- -----------------------------------------------------------------------------
CREATE TEMPORARY TABLE mock_slot_consumption (
    medical_slot_id BIGINT PRIMARY KEY,
    consumed_slots INT NOT NULL
);

INSERT INTO mock_slot_consumption (medical_slot_id, consumed_slots)
SELECT
    contemplation.id_available_medical_slot,
    COUNT(*) AS consumed_slots
FROM contemplations contemplation
JOIN appointments appointment
    ON appointment.id_contemplation = contemplation.id
    AND appointment.tenant_id = contemplation.tenant_id
WHERE appointment.status <> 'Contemplação Cancelada'
GROUP BY contemplation.id_available_medical_slot;

UPDATE medical_slots slot
JOIN mock_slot_consumption consumption
    ON consumption.medical_slot_id = slot.id
SET slot.total_slots = GREATEST(slot.total_slots, consumption.consumed_slots);

UPDATE medical_slots slot
LEFT JOIN mock_slot_consumption consumption
    ON consumption.medical_slot_id = slot.id
SET
    slot.current_slots = slot.total_slots - COALESCE(consumption.consumed_slots, 0),
    slot.update_date = CASE
        WHEN consumption.consumed_slots IS NULL THEN NULL
        ELSE NOW(6)
    END,
    slot.update_user = CASE
        WHEN consumption.consumed_slots IS NULL THEN NULL
        ELSE 'ROTINA'
    END;

-- Garante exemplos de lotes totalmente consumidos para filtros e dashboard.
UPDATE medical_slots slot
JOIN mock_slot_consumption consumption
    ON consumption.medical_slot_id = slot.id
SET
    slot.total_slots = consumption.consumed_slots,
    slot.current_slots = 0
WHERE consumption.consumed_slots > 0
  AND MOD(slot.id, 6) = 0;

-- -----------------------------------------------------------------------------
-- Limpeza dos auxiliares (somente dados persistentes permanecem no schema)
-- -----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS mock_slot_consumption;
DROP TEMPORARY TABLE IF EXISTS mock_appointment_plan;
DROP TEMPORARY TABLE IF EXISTS mock_slot_procedures;
DROP TEMPORARY TABLE IF EXISTS mock_month_offsets;
DROP TEMPORARY TABLE IF EXISTS mock_streets;
DROP TEMPORARY TABLE IF EXISTS mock_second_surnames;
DROP TEMPORARY TABLE IF EXISTS mock_surnames;
DROP TEMPORARY TABLE IF EXISTS mock_given_names;
DROP TEMPORARY TABLE IF EXISTS mock_numbers;

DROP FUNCTION IF EXISTS mock_seed_cns;
DROP FUNCTION IF EXISTS mock_seed_cpf;
