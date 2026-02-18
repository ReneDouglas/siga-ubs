-- =============================================================================
-- SIGA-UBS — Dados iniciais para desenvolvimento
-- ATENÇÃO: Alterar as senhas no primeiro uso em ambiente de produção!
-- Senhas padrão: admin/admin123 | sms/sms123 | user/user123
-- =============================================================================

USE sigaubs;

-- -----------------------------------------------------------------------------
-- Perfis de acesso
-- -----------------------------------------------------------------------------
INSERT INTO system_roles (id, `role`, title, description, root, creation_date, creation_user) VALUES
(1, 'ROLE_ADMIN',      'Administrador',          'Administrador do Sistema',                               TRUE,  NOW(6), 'sistema'),
(2, 'ROLE_SMS',        'Secretaria de Saúde',    'Gestão da Secretaria Municipal de Saúde',                FALSE, NOW(6), 'sistema'),
(3, 'ROLE_ATENDENTE',  'Atendente',              'Atendente da Unidade Básica de Saúde',                   FALSE, NOW(6), 'sistema'),
(4, 'ROLE_ENFERMEIRO', 'Enfermeiro',             'Enfermeiro da Unidade Básica de Saúde',                  FALSE, NOW(6), 'sistema'),
(5, 'ROLE_ACS',        'ACS',                    'Agente Comunitário de Saúde',                            FALSE, NOW(6), 'sistema'),
(6, 'ROLE_USER',       'Usuário',                'Usuário padrão da Unidade Básica de Saúde',              FALSE, NOW(6), 'sistema');

-- -----------------------------------------------------------------------------
-- Unidades Básicas de Saúde
-- -----------------------------------------------------------------------------
INSERT INTO basic_health_units (id, name, neighborhood, creation_date, creation_user) VALUES
(1, 'UBS Central',   'Centro',        NOW(6), 'sistema'),
(2, 'UBS Leste',     'Bairro Leste',  NOW(6), 'sistema');

-- -----------------------------------------------------------------------------
-- Usuários do sistema
-- Hashes BCrypt (custo 10) das senhas padrão:
--   admin123 → $2b$10$yHZY3T5CccLi.gG8FhUrtekFVlb7Xuk2yc5Mf6Fj62kre7abaFFWa
--   sms123   → $2b$10$yVOU/qiqTg2C5TUi3oW3mOjkuBBUuTKgjfS3ZgshRTuH4i2aCXbHS
--   user123  → $2b$10$bsKGvbAi45.Ga33soqVuaeh4qTW6uBRdLjOt5ET46wUyFph7lcEh6
-- -----------------------------------------------------------------------------
INSERT INTO system_users (id, username, `password`, name, email, active, id_basic_health_unit, creation_date, creation_user) VALUES
(1, 'admin', '$2b$10$yHZY3T5CccLi.gG8FhUrtekFVlb7Xuk2yc5Mf6Fj62kre7abaFFWa', 'Administrador do Sistema', 'admin@sigaubs.local',   1, NULL, NOW(6), 'sistema'),
(2, 'sms',   '$2b$10$yVOU/qiqTg2C5TUi3oW3mOjkuBBUuTKgjfS3ZgshRTuH4i2aCXbHS', 'Secretaria Municipal',     'sms@sigaubs.local',     1, NULL, NOW(6), 'sistema'),
(3, 'user',  '$2b$10$bsKGvbAi45.Ga33soqVuaeh4qTW6uBRdLjOt5ET46wUyFph7lcEh6', 'Usuário UBS Central',      'usuario@sigaubs.local', 1, 1,    NOW(6), 'sistema');

-- -----------------------------------------------------------------------------
-- Associação de perfis aos usuários
-- -----------------------------------------------------------------------------
INSERT INTO system_users_roles (id_system_user, id_system_role) VALUES
(1, 1), -- admin  → ROLE_ADMIN
(2, 2), -- sms    → ROLE_SMS
(3, 6); -- user   → ROLE_USER

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
