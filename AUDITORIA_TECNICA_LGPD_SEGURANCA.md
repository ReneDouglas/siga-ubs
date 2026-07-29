# Auditoria Técnica de Segurança e Proteção de Dados — SIGA-UBS

**Data da revisão:** 28 de julho de 2026<br>
**Escopo:** aplicação web, regras de negócio automatizadas, autenticação e autorização, banco de dados, containers, nginx, cadeia de suprimentos, VPS, Cloudflare, backup, observabilidade e resposta a incidentes<br>
**Referencial principal:** LGPD, orientações da ANPD e OWASP ASVS 5.0<br>
**Classificação:** documento técnico; não substitui parecer jurídico, auditoria da operação ou teste de intrusão

## 1. Resultado executivo

O SIGA-UBS **não deve entrar em produção com dados reais no estado atual**.

O repositório contém controles positivos — Spring Security, CSRF, BCrypt, separação por tenant, escopo de leitura por UBS, TLS no nginx e processo Java sem root —, mas ainda existem vulnerabilidades críticas e altas que permitem:

- execução de JavaScript persistente no navegador de profissionais autenticados;
- alteração de pacientes e consultas sem autorização por UBS em todos os fluxos de escrita;
- elevação indevida de papel por adulteração de requisição;
- exposição de nome, CPF e filtros de pesquisa em logs e URLs;
- ataques automatizados contra autenticação pública sem MFA ou limitação de tentativas;
- execução concorrente ou induzida da rotina de contemplação;
- decisões automatizadas de acesso à saúde sem explicação fiel, versionamento e mecanismo técnico de revisão;
- perda de até uma semana de dados no cenário de backup informado;
- acesso direto à origem e contorno da Cloudflare, caso o firewall da VPS não esteja restrito;
- comprometimento ampliado do banco por configuração insegura disponível no projeto;
- exploração de dependências e imagens sem uma esteira contínua de verificação.

O acesso exclusivo por profissionais credenciados **não neutraliza esses riscos**. Credenciais podem ser roubadas, usuários podem exceder sua finalidade funcional e um XSS executa com os mesmos privilégios do profissional comprometido.

### 1.1 Recomendação de decisão

Adotar um **bloqueio de go-live** até que:

1. todos os achados críticos estejam corrigidos e testados;
2. os achados altos diretamente exploráveis estejam corrigidos;
3. MFA e proteção contra automação estejam ativos;
4. a origem aceite tráfego web somente da Cloudflare;
5. exista backup de banco compatível com RPO/RTO definidos e restauração testada;
6. a fila automatizada tenha critérios aprovados, explicação correta e processo de revisão;
7. seja concluído um pentest autenticado independente em ambiente de homologação;
8. exista plano técnico de incidente, responsáveis e monitoração efetiva.

## 2. Informações confirmadas pelo responsável

- O sistema ficará acessível pela Internet, sem VPN.
- Os usuários são profissionais de saúde credenciados.
- ACS, enfermeiro e usuário comum devem acessar somente dados da própria UBS.
- SMS deve acessar todo o tenant/município.
- Foi informado um ADMIN coordenador por município, tenant-scoped.
- Também foi informado um administrador global da empresa para suporte técnico.
- Pacientes podem ser crianças e adolescentes.
- A contemplação pretende funcionar sem revisão humana obrigatória; a revisão será opcional.
- A hospedagem será em uma VPS da Hostinger.
- IP e domínio serão gerenciados pela Cloudflare.
- Pode existir allowlist de IP para redes municipais.
- Há backup semanal.
- Observabilidade ainda será implementada/configurada.
- Não existem hoje encarregado, aviso de privacidade, RIPD, inventário formal, política de retenção ou plano de incidentes.
- Não há integrações, planilhas, notificações ou exportações externas informadas.
- PKCS#12 e seed presentes no repositório são mocks de desenvolvimento.
- Certificado e chave privada de produção são gerenciados pelo nginx fora do diretório do projeto.
- O arquivo local `.env.prd` analisado não representa a credencial real de produção.

### 2.1 Premissa sobre os dois tipos de administrador

Este relatório assume que existem duas identidades diferentes:

- **coordenador municipal/tenant:** limitado ao próprio tenant;
- **administrador global de suporte da empresa:** conta de plataforma, separada dos usuários municipais.

O nome `ADMIN` não deve representar simultaneamente esses dois níveis. A ambiguidade é, por si só, um risco de autorização. Recomenda-se usar papéis explicitamente diferentes, por exemplo `TENANT_COORDINATOR` e `PLATFORM_SUPPORT_ADMIN`.

## 3. Limites da auditoria

Foram examinados:

- código Java e templates JTE;
- configurações Spring;
- schema MySQL;
- nginx e Docker Compose de produção;
- Dockerfile de produção;
- testes automatizados;
- dependências npm;
- histórico e arquivos versionados para exposição evidente de segredos;
- documentação oficial atual da LGPD, ANPD, Cloudflare, Hostinger e OWASP.

Não foram inspecionados:

- a VPS implantada;
- regras reais de firewall da Hostinger ou do sistema operacional;
- painel e regras reais da Cloudflare;
- modo SSL/TLS efetivamente selecionado na Cloudflare;
- cabeçalhos e cookies de uma instância real;
- criptografia do disco e dos backups;
- credenciais reais de produção;
- configuração SSH, patching, EDR e contas do sistema operacional;
- conteúdo real do banco;
- contratos e processos organizacionais;
- restauração real de backup;
- teste DAST/pentest contra uma implantação.

Quando um controle não foi demonstrado, ele aparece como **não comprovado**, e não como inexistente.

## 4. Arquitetura e superfície de ataque

### 4.1 Fluxo esperado

```text
Profissional de saúde
        |
        | HTTPS público
        v
Cloudflare DNS/proxy/WAF?  <-- configuração real não comprovada
        |
        | HTTPS até a origem?  <-- deve ser Full (strict)
        v
VPS Hostinger
        |
        +--> firewall Hostinger/OS?  <-- não comprovado
        |
        v
nginx container :443
        |
        | HTTP na rede Docker
        v
Spring Boot :8080
        |
        | JDBC `useSSL=false`
        v
MySQL container :3306
        |
        +--> volume Docker externo
        +--> backup semanal da VPS
```

### 4.2 Superfícies principais

- login municipal;
- login global de administração;
- endpoints HTMX e formulários server-rendered;
- busca de pacientes;
- cadastro e edição de paciente;
- marcação, cancelamento e contemplação;
- job agendado de contemplação;
- actuator;
- nginx público;
- painel Cloudflare;
- painel Hostinger;
- SSH e Docker daemon da VPS;
- banco e backups;
- bibliotecas JavaScript locais e de terceiros;
- pipeline de build.

## 5. Dados sob risco

| Grupo | Dados |
|---|---|
| Identificação | nome, CPF, CNS/Cartão SUS, nascimento, gênero |
| Contato/localização | telefone, logradouro, número, complemento, referência |
| Saúde | procedimento, especialidade, prioridade, observação, status, contemplação e histórico |
| Socioeconômico | classificação de situação social |
| Vínculo assistencial | UBS e ACS responsável |
| Profissionais | nome, login, e-mail, hash de senha, papel, UBS e tenant |
| Auditoria atual | usuário de criação/alteração, datas, falhas de autenticação e logs do job |

Dados de saúde são dados pessoais sensíveis. CPF e CNS não pertencem automaticamente à lista legal de dados sensíveis, mas são identificadores de alto impacto e agravam fraude e reidentificação.

## 6. Critérios de severidade

| Severidade | Critério |
|---|---|
| Crítica | exploração pode comprometer contas privilegiadas, alterar fronteiras de acesso ou expor dados sensíveis em escala |
| Alta | exploração ou falha operacional pode causar vazamento relevante, fraude, indisponibilidade ou perda material de dados |
| Média | aumenta significativamente a superfície de ataque ou reduz capacidade de prevenção/detecção |
| Baixa | hardening, inconsistência ou dívida que deve ser corrigida, mas não representa comprometimento direto isoladamente |

## 7. Resumo consolidado dos achados

| ID | Severidade | Achado | Estado |
|---|---:|---|---|
| APP-01 | Crítica | XSS persistente em handlers JavaScript inline | Confirmado |
| APP-02 | Crítica | Falta de autorização por objeto/UBS em mutações | Confirmado |
| IAM-01 | Crítica | Elevação de papel e alteração de usuário por parâmetros adulterados | Confirmado |
| LOG-01 | Alta | Nome, CPF e filtros sensíveis em logs e URLs | Confirmado |
| IAM-02 | Alta | Aplicação pública sem MFA e proteção contra automação | Confirmado |
| IAM-03 | Alta | Sessões não revogadas após eventos de risco | Confirmado |
| IAM-04 | Alta | Reautenticação sensível aplicada somente no frontend | Confirmado |
| APP-03 | Alta | Binding direto de entidades e ausência de validação no servidor | Confirmado |
| BUS-01 | Alta | Decisão automatizada sem explicação fiel e revisão estruturada | Confirmado |
| BUS-02 | Alta | GET com efeito de escrita e concorrência na contemplação | Confirmado |
| DB-01 | Alta | Integridade tenant/UBS não garantida pelas FKs | Confirmado |
| DB-02 | Alta | Defaults e exemplo de produção permitem banco como root | Confirmado no projeto; produção informada como diferente |
| CRYPTO-01 | Alta | Proteção em trânsito interno e em repouso não comprovada | Parcial/não comprovado |
| NET-01 | Alta | Possível contorno da Cloudflare pelo IP da origem | Não comprovado |
| BAK-01 | Alta | Backup semanal implica RPO de até sete dias | Confirmado |
| OPS-01 | Alta | Observabilidade e resposta a incidente ainda não implantadas | Confirmado |
| AUD-01 | Alta | Auditoria insuficiente de leitura e ações privilegiadas | Confirmado |
| PRIV-01 | Alta | Exposição excessiva de campos e RBAC incompleto | Confirmado |
| PRIV-02 | Alta | Ausência de ciclo técnico de retenção, correção e eliminação | Confirmado |
| MIN-01 | Alta | Ausência de controles específicos para menores | Confirmado |
| SUP-01 | Alta | JavaScript externo sem pin/SRI e contato com terceiros | Confirmado |
| SUP-02 | Alta | Dependências npm vulneráveis e SCA Java inconclusiva | Confirmado |
| OBS-01 | Média | Actuator revela detalhes de saúde da infraestrutura | Confirmado |
| NET-02 | Média | IP real, logs, rate limits e timeouts do nginx incompletos | Confirmado |
| NET-03 | Média | Redirecionamento HTTP usa Host não validado | Confirmado |
| INF-01 | Média | Containers sem hardening adicional | Confirmado |
| INF-02 | Média | Imagens mutáveis e build com `curl | bash` | Confirmado |
| AUTH-01 | Média | Política de senha insuficiente | Confirmado |
| SES-01 | Média | Cookies e timeouts não são totalmente explícitos/verificados | Parcial |
| ERR-01 | Média | Mensagens brutas de exceção podem vazar dados nos logs | Confirmado |
| AVAIL-01 | Média | Configuração MySQL aceita perda recente de transações | Confirmado |
| COR-01 | Baixa | CORS permissivo latente | Confirmado, aparentemente inativo |
| CERT-01 | Baixa | Mock empacotado e caminho de certificado divergente | Confirmado |
| GOV-01 | Alta | Controles técnicos não possuem governança mínima documentada | Confirmado |

## 8. Achados detalhados e recomendações

### APP-01 — XSS persistente em dados de paciente

**Severidade:** Crítica<br>
**Estado:** Confirmado

#### Evidência

- `src/main/jte/patientManagement/patientFragments/patient_history.jte:45`
- `src/main/jte/queueManagement/queueFragments/patientHistory_tab.jte:37`
- `src/main/jte/appointmentManagement/appointment_management.jte:192`
- `src/main/jte/patientManagement/patientFragments/patient_search_autocomplete.jte:8`
- `src/main/jte/specialtyManagement/specialty_management.jte:72`

Dados como observação e nome são inseridos em `onclick` ou em HTML construído por concatenação. A substituição de `'` por `\'` não é codificação JavaScript segura. Uma barra invertida previamente fornecida pode alterar o contexto e permitir execução.

#### Impacto

- roubo de dados visíveis ao profissional;
- alteração de pacientes, consultas ou contemplações;
- execução com papel SMS/ADMIN quando a vítima for privilegiada;
- captura de token CSRF por execução no mesmo origin;
- propagação persistente por observações armazenadas.

#### Recomendação

1. Remover dados não confiáveis de todos os handlers inline.
2. Registrar eventos com `addEventListener`.
3. Transportar apenas IDs em atributos `data-*` codificados.
4. Carregar texto por endpoint ou atributo e atribuí-lo com `textContent`/`value`.
5. Para conteúdo que realmente admita HTML, usar sanitizador mantido e com allowlist explícita.
6. Após remover scripts inline, implantar CSP com nonce ou hashes, sem depender de `unsafe-inline`.
7. Adicionar testes com aspas, barras invertidas, quebras de linha, entidades HTML e payloads armazenados.

#### Critério de aceite

- nenhuma interpolação de dados pessoais em `onclick`, `onerror`, `<script>` ou `innerHTML`;
- teste automatizado demonstra que payloads são exibidos como texto;
- CSP em modo enforcement sem `unsafe-inline` para scripts.

---

### APP-02 — Falta de autorização por objeto e UBS em mutações

**Severidade:** Crítica<br>
**Estado:** Confirmado

#### Evidência

- `src/main/java/br/com/tecsus/sigaubs/controllers/PatientController.java:99`
- `src/main/java/br/com/tecsus/sigaubs/services/PatientService.java:48`
- `src/main/java/br/com/tecsus/sigaubs/services/AppointmentService.java:80`
- `src/main/java/br/com/tecsus/sigaubs/services/AppointmentService.java:46`

A leitura de paciente possui escopo por UBS, mas a atualização recebe uma entidade destacada e salva pelo ID sem recarregar o paciente dentro da UBS permitida. O cancelamento de consulta carrega por ID/tenant, sem validar UBS. A criação de consulta aceita IDs de paciente e procedimento enviados pelo navegador.

#### Impacto

Um profissional de uma UBS pode tentar:

- atualizar paciente de outra UBS do mesmo tenant;
- reassociar o paciente à sua UBS;
- cancelar consulta pertencente a outra UBS;
- criar relacionamento inconsistente usando IDs conhecidos ou enumerados.

#### Recomendação

1. Criar DTOs específicos para cada comando.
2. Recarregar paciente/consulta pelo par `id + tenant + UBS autorizada`.
3. Aplicar autorização no service antes de qualquer alteração.
4. Não aceitar `tenantId`, `basicHealthUnit`, `roles`, campos de auditoria ou estado interno diretamente do formulário.
5. Validar no servidor que paciente, UBS, consulta, procedimento, vaga e contemplação pertencem ao mesmo escopo.
6. Implementar política explícita para SMS, coordenador tenant e suporte global.
7. Criar testes negativos cross-UBS, cross-tenant e para IDs inexistentes.

#### Critério de aceite

- todas as mutações falham com 403 quando o objeto está fora do escopo;
- nenhum comando persiste associação cross-tenant/cross-UBS;
- os testes cobrem ACS, enfermeiro, usuário, SMS, coordenador e suporte global.

---

### IAM-01 — Elevação de papel e administração de usuários por adulteração

**Severidade:** Crítica<br>
**Estado:** Confirmado

#### Evidência

- `src/main/java/br/com/tecsus/sigaubs/services/SystemUserService.java:108`
- `src/main/java/br/com/tecsus/sigaubs/services/SystemUserService.java:114`
- `src/main/java/br/com/tecsus/sigaubs/services/SystemUserService.java:132`
- `src/main/java/br/com/tecsus/sigaubs/services/SystemUserService.java:173`

A interface remove ADMIN/SMS da lista visível, mas o serviço carrega qualquer `selectedRoleId`. Os endpoints de update/delete também não comprovam que o ator pode administrar o usuário alvo.

#### Impacto

- autoelevação ou criação de conta privilegiada;
- alteração de senha/papel de outro usuário do tenant;
- exclusão ou desativação indevida;
- comprometimento total dos dados do município.

#### Recomendação

1. Definir allowlist de papéis atribuíveis por cada papel ator.
2. Separar `TENANT_COORDINATOR` de `PLATFORM_SUPPORT_ADMIN`.
3. Manter contas globais fora da tabela/fluxo de contas municipais.
4. Proibir que um usuário altere o próprio papel ou remova o último coordenador.
5. Validar o alvo por tenant e hierarquia.
6. Para suporte global, usar acesso `break-glass`/just-in-time:
   - MFA resistente a phishing;
   - motivo e número de chamado;
   - seleção explícita do tenant;
   - duração limitada;
   - leitura por padrão;
   - trilha imutável;
   - alerta ao responsável do tenant.
7. Testar adulteração de todos os IDs de papel e usuário.

#### Critério de aceite

- papel enviado pelo cliente nunca é aceito sem autorização do servidor;
- um SMS/coordenador não cria conta global nem eleva usuário;
- toda sessão de suporte global possui motivo, início, fim e tenant auditados.

---

### LOG-01 — Dados pessoais em logs e query strings

**Severidade:** Alta<br>
**Estado:** Confirmado

#### Evidência

- `src/main/java/br/com/tecsus/sigaubs/services/ContemplationScheduleService.java:146`
- `src/main/java/br/com/tecsus/sigaubs/services/ContemplationScheduleService.java:202`
- `src/main/jte/patientManagement/patient_list.jte:40`
- `docker-compose-prd.yml:67`

O job registra nome, CPF e motivo em nível INFO. A busca GET envia nome, CPF, CNS, endereço, ACS e situação social na URL. O nginx usa o access log padrão, que inclui a requisição completa.

#### Impacto

- replicação de dados em logs Docker, navegador, proxy, Cloudflare e ferramentas futuras de observabilidade;
- aumento do número de operadores e locais com dados de saúde/identificação;
- vazamento por suporte, exportação de logs ou comprometimento do painel.

#### Recomendação

1. Remover nome, CPF, CNS e texto clínico de logs.
2. Usar IDs internos opacos, ator, operação, resultado e código de motivo.
3. Migrar pesquisas sensíveis para POST ou manter filtros em estado de servidor.
4. Definir formato de access log que não registre query string.
5. Sanitizar CR/LF e delimitadores para evitar log injection.
6. Não enviar corpo de requisição para APM.
7. Definir retenção, acesso e criptografia dos logs.
8. Revisar logs existentes e eliminá-los conforme política aprovada.

#### Critério de aceite

- busca por CPF/nome de teste não aparece em logs, URL ou telemetria;
- job registra somente IDs opacos;
- acesso a logs é segregado e auditado.

---

### IAM-02 — Serviço público na Internet sem MFA e anti-automação

**Severidade:** Alta<br>
**Estado:** Confirmado

#### Evidência

- `src/main/java/br/com/tecsus/sigaubs/security/SecurityEventListener.java:15`
- `src/main/java/br/com/tecsus/sigaubs/security/config/WebSecurityConfig.java:81`
- `nginx/prd.conf` não possui `limit_req` ou `limit_conn`

Falhas de autenticação são apenas registradas. Não existe MFA, limitação por conta/IP, atraso progressivo ou detecção de credential stuffing.

#### Impacto

- força bruta e credential stuffing;
- tomada de conta de profissional;
- enumeração de contas por comportamento/tempo;
- negação de serviço por bloqueio ingênuo ou excesso de requisições.

#### Recomendação

1. Implantar MFA para todos os usuários; para administradores globais, preferir WebAuthn/passkeys ou chave de segurança.
2. Usar TOTP como alternativa, com códigos de recuperação protegidos.
3. Aplicar rate limit em camadas:
   - Cloudflare;
   - nginx;
   - aplicação por conta + IP + tenant.
4. Usar atraso progressivo e bloqueio temporário, evitando bloqueio permanente explorável.
5. Alertar sobre tentativas distribuídas e credenciais reutilizadas.
6. Considerar Cloudflare Access para o hostname global de administração e, se operacionalmente viável, para toda a aplicação.
7. IP allowlist deve ser adicional e somente usada quando a rede municipal possuir IP de saída estável.

#### Critério de aceite

- MFA é exigido em login e elevação de privilégio;
- testes demonstram rate limit por conta e IP;
- alertas são gerados para força bruta distribuída;
- hostname global de administração não depende apenas de senha.

---

### IAM-03 — Sessões não revogadas após eventos de risco

**Severidade:** Alta<br>
**Estado:** Confirmado

#### Evidência

- `src/main/java/br/com/tecsus/sigaubs/services/SystemUserService.java:132`
- `src/main/java/br/com/tecsus/sigaubs/services/SystemUserService.java:173`
- `src/main/java/br/com/tecsus/sigaubs/tenancy/TenantSessionValidationFilter.java:23`

O filtro de sessão valida apenas correspondência de tenant. Usuários comuns podem manter sessão após alteração de senha, papel, desativação ou exclusão. O fluxo SMS revoga quando inativado, mas não necessariamente quando a senha muda.

#### Recomendação

- revogar todas as sessões após senha, papel, UBS, estado ativo ou exclusão;
- renovar ID de sessão após autenticação e mudança de privilégio;
- manter índice de sessões por identificador estável de usuário;
- se houver mais de uma instância, usar armazenamento de sessão compartilhado;
- permitir ao usuário e administrador visualizar e encerrar sessões.

#### Critério de aceite

- sessão anterior recebe 401/redirect de login imediatamente após evento de risco;
- revogação funciona entre instâncias e reinicializações planejadas.

---

### IAM-04 — Reautenticação de contemplação apenas no frontend

**Severidade:** Alta<br>
**Estado:** Confirmado

#### Evidência

- `src/main/jte/queueManagement/queue_management_v2.jte:247`
- `src/main/java/br/com/tecsus/sigaubs/controllers/QueueController.java:337`

A interface chama um endpoint de validação de senha antes de contemplar, mas o POST de contemplação não exige prova de reautenticação recente.

#### Recomendação

- registrar no servidor um instante de autenticação forte recente;
- exigir step-up/MFA para contemplação manual, alteração de papel e suporte global;
- vincular autorização ao usuário, sessão, ação, objeto e curto prazo;
- não aceitar booleano ou token reutilizável controlado pelo navegador.

#### Critério de aceite

- POST direto sem step-up recente é negado;
- autorização expira e não pode ser reutilizada para outro paciente/ação.

---

### APP-03 — Binding de entidades e validação insuficiente

**Severidade:** Alta<br>
**Estado:** Confirmado

#### Evidência

- controllers usam `@ModelAttribute Patient`, `Appointment` e `SystemUser`;
- não foram encontrados `@Valid`, `BindingResult` ou Jakarta Bean Validation;
- `src/main/java/br/com/tecsus/sigaubs/controllers/PatientController.java:58`
- `src/main/java/br/com/tecsus/sigaubs/controllers/AppointmentController.java:108`
- `src/main/java/br/com/tecsus/sigaubs/controllers/SessionController.java:128`

#### Impacto

- mass assignment;
- valores fora dos limites;
- datas futuras;
- CPF/CNS/telefone inválidos;
- campos grandes para consumo de recursos;
- estados e relações inconsistentes;
- paginação abusiva.

#### Recomendação

1. Criar DTOs por caso de uso.
2. Usar Bean Validation no servidor.
3. Validar formato e semântica:
   - CPF;
   - CNS;
   - e-mail;
   - telefone;
   - data de nascimento;
   - enumerações;
   - comprimentos;
   - quantidade de vagas;
   - paginação com máximo.
4. Normalizar antes de comparar/persistir.
5. Retornar erro seguro e específico por campo.
6. Manter constraints equivalentes no banco.

#### Critério de aceite

- campos não previstos são ignorados/rejeitados;
- entradas inválidas não chegam ao repositório;
- fuzzing básico não produz 500 ou consumo desproporcional.

---

### BUS-01 — Decisão automatizada sem explicação fiel

**Severidade:** Alta<br>
**Estado:** Confirmado

#### Evidência

- `src/main/java/br/com/tecsus/sigaubs/repositories/Impl/AppointmentRepositoryCustomImpl.java:137`
- `src/main/java/br/com/tecsus/sigaubs/services/ContemplationScheduleService.java:208`

A fila usa, em ordem, tempo superior a quatro meses, prioridade, idade, situação social e data de solicitação. O método de explicação pode registrar `SEXO`, apesar de sexo não participar do `ORDER BY`.

#### Impacto

- motivo registrado incorretamente;
- incapacidade de explicar a decisão;
- risco de discriminação ou regra sem fundamento clínico;
- contestação sem evidência reproduzível;
- impacto ampliado para crianças e grupos vulneráveis.

#### Recomendação

1. Suspender a execução automática em produção até aprovação dos critérios pelo controlador e responsáveis assistenciais.
2. Elaborar RIPD antes do tratamento em escala.
3. Versionar formalmente o algoritmo e os critérios.
4. Registrar para cada decisão:
   - versão da regra;
   - dados de entrada relevantes;
   - posição/ranking;
   - critérios aplicados;
   - desempate;
   - horário;
   - vagas disponíveis;
   - resultado.
5. Corrigir a divergência entre ordenação e motivo.
6. Disponibilizar explicação clara e processo de solicitação de revisão.
7. A revisão não precisa ocorrer em toda decisão, mas deve existir quando solicitada e para exceções.
8. Criar override manual com motivo obrigatório e auditoria.
9. Executar testes de:
   - determinismo;
   - regressão;
   - empate;
   - viés;
   - qualidade de dados;
   - impacto em menores;
   - ausência de critérios não aprovados.

#### Critério de aceite

- uma decisão pode ser reproduzida exatamente;
- o motivo exibido corresponde ao algoritmo executado;
- há canal e workflow de revisão;
- mudanças de regra exigem aprovação e nova versão.

---

### BUS-02 — GET com escrita e concorrência no job

**Severidade:** Alta<br>
**Estado:** Confirmado

#### Evidência

- `src/main/java/br/com/tecsus/sigaubs/controllers/ScheduleController.java:20`
- `src/main/java/br/com/tecsus/sigaubs/services/MedicalSlotService.java:98`
- não há `@Version`, lock pessimista ou lock distribuído;
- o status em memória não bloqueia uma segunda execução.

#### Impacto

- execução por navegação cross-site de administrador autenticado;
- dupla contemplação;
- saldo incorreto de vagas;
- execuções duplicadas após escalabilidade horizontal;
- reprocessamento parcial após retry.

#### Recomendação

1. Remover o endpoint de teste do profile de produção.
2. Se necessário, usar POST com CSRF e papel específico.
3. Adotar lock distribuído/banco para uma única execução por tenant/período.
4. Usar decremento atômico:
   `UPDATE ... SET current_slots = current_slots - 1 WHERE id = ? AND current_slots > 0`.
5. Verificar quantidade de linhas alteradas.
6. Usar `@Version` ou lock pessimista onde aplicável.
7. Registrar execução/idempotency key.
8. Tornar retry idempotente.
9. Impedir mais de uma contemplação válida por consulta.

#### Critério de aceite

- teste concorrente não gera saldo negativo, duplicidade ou perda;
- GET nunca altera estado;
- múltiplas instâncias executam apenas um job.

---

### DB-01 — FKs não garantem isolamento relacional

**Severidade:** Alta<br>
**Estado:** Confirmado

#### Evidência

- `docker/mysql/01-schema.sql:133`
- `docker/mysql/01-schema.sql:162`
- `docker/mysql/01-schema.sql:180`
- `docker/mysql/01-schema.sql:196`

As tabelas possuem `tenant_id`, mas FKs como consulta → paciente e paciente → UBS referenciam somente o ID.

#### Recomendação

- adicionar chave/índice único `(tenant_id, id)` nas tabelas tenant-scoped;
- usar FKs compostas:
  - paciente → UBS;
  - consulta → paciente;
  - contemplação → vaga;
  - histórico → consulta;
  - usuário → UBS;
- adicionar `CHECK` para saldo entre zero e total;
- verificar procedimento e UBS da vaga contra a consulta;
- manter validação no serviço mesmo com constraints;
- executar migração com auditoria prévia de inconsistências.

#### Critério de aceite

- inserts/updates cross-tenant são rejeitados pelo MySQL;
- consulta de integridade retorna zero divergências.

---

### DB-02 — Configuração permite root no banco

**Severidade:** Alta como configuração; produção real informada como diferente<br>
**Estado:** Confirmado no repositório

#### Evidência

- `src/main/resources/application-prd.properties:11`
- `src/main/resources/application-prd.properties:12`
- `.env.prd.example:16`
- `.env.prd.example:21`

O profile possui fallback `root/root`, e o exemplo recomenda usar root. Mesmo que a produção real não utilize esse arquivo, o artefato aceita inicialização insegura fora do compose.

#### Recomendação

1. Remover qualquer fallback de produção.
2. Falhar o startup quando usuário/senha não forem fornecidos.
3. Criar usuário de aplicação restrito ao schema e operações necessárias.
4. Separar senha root.
5. Desabilitar login root remoto.
6. Usar secret file/secret manager; restringir arquivos locais a `0600`.
7. Rotacionar credenciais e auditar acesso administrativo.

#### Critério de aceite

- aplicação não inicia sem segredo explícito;
- aplicação não consegue criar usuários, conceder privilégios ou acessar outro schema;
- root não é usado pelo processo Spring.

---

### CRYPTO-01 — Criptografia interna e em repouso não comprovada

**Severidade:** Alta<br>
**Estado:** Parcial

#### Evidência

- TLS público no nginx está configurado;
- `docker-compose-prd.yml:63` usa JDBC com `useSSL=false`;
- dados são armazenados em colunas em claro;
- criptografia do volume/backup não foi comprovada.

#### Recomendação

1. Cloudflare → origem em **Full (strict)**.
2. Criptografar disco/volume e backups.
3. Manter chaves fora do banco e dos backups.
4. Avaliar criptografia de campo/tokenização para CPF, CNS e observações:
   - preservar busca por hash/token quando necessário;
   - separar chave e índice;
   - prever rotação.
5. Se app e banco permanecerem no mesmo host/rede Docker, documentar o risco aceito do HTTP/JDBC interno.
6. Se banco for separado, exigir TLS autenticado.
7. Confirmar que dumps e arquivos temporários também são criptografados.

#### Critério de aceite

- evidência de criptografia de volume e backup;
- teste de restauração mantém proteção;
- tráfego Cloudflare-origem é autenticado e criptografado;
- chaves não aparecem em imagem, repositório, variável exibida ou backup.

---

### NET-01 — Possível bypass da Cloudflare

**Severidade:** Alta<br>
**Estado:** Não comprovado

Gerenciar DNS pela Cloudflare não garante que o tráfego passe pelo proxy. Se o IP da VPS aceitar 443 de qualquer origem, um atacante pode contornar WAF, rate limit e regras Cloudflare.

#### Recomendação

1. Ativar proxy Cloudflare para todos os hostnames da aplicação.
2. No firewall Hostinger e no firewall do SO:
   - permitir 443 somente dos ranges oficiais Cloudflare;
   - permitir SSH somente de rede administrativa/Access;
   - bloquear 3306, 8080 e Docker API externamente.
3. Usar Full (strict).
4. Habilitar Authenticated Origin Pulls quando aplicável.
5. Alternativamente, avaliar Cloudflare Tunnel para não expor portas web na origem.
6. Evitar vazamento do IP da origem em DNS histórico, outros serviços e respostas.
7. Configurar servidor default que rejeite IP/Host desconhecido.
8. Garantir que páginas autenticadas enviem `Cache-Control: no-store` e nunca sejam incluídas em regra “Cache Everything”.

#### Critério de aceite

- acesso direto ao IP da VPS falha;
- somente Cloudflare alcança 443;
- Cloudflare Full (strict) está ativo;
- respostas autenticadas não são armazenadas em cache compartilhado.

---

### BAK-01 — Backup semanal e ausência de DR testado

**Severidade:** Alta<br>
**Estado:** Confirmado

Backup semanal representa perda potencial de até sete dias de novos pacientes, consultas, vagas e contemplações. Backup do VPS também não comprova consistência transacional do MySQL.

#### Recomendação

1. Definir formalmente RPO e RTO com os municípios.
2. Implementar backup de banco:
   - diário no mínimo;
   - incremental/binlog para RPO menor;
   - consistente com MySQL;
   - criptografado;
   - separado da VPS e, preferencialmente, do mesmo provedor/conta;
   - imutável ou offline;
   - com retenções diária, semanal e mensal.
3. Manter o backup semanal da VPS como camada adicional, não como única.
4. Testar restauração completa e point-in-time periodicamente.
5. Monitorar sucesso, tamanho, duração e idade do último backup.
6. Documentar quem pode restaurar e como os segredos são recuperados.
7. Realizar exercício de desastre sem sobrescrever a única cópia válida.

#### Critério de aceite

- RPO/RTO aprovados;
- restauração em ambiente isolado concluída e documentada;
- backup corrompido/ausente gera alerta;
- comprometimento da VPS não permite apagar todas as cópias.

---

### OPS-01 — Observabilidade e resposta a incidentes não implantadas

**Severidade:** Alta<br>
**Estado:** Confirmado

#### Recomendação

Monitorar e alertar, sem enviar PII:

- falhas/sucessos incomuns de autenticação;
- elevação de papel;
- uso do administrador global;
- pesquisas e leituras em volume anormal;
- contemplações e overrides;
- WAF/rate limits;
- 4xx/5xx e latência;
- CPU, memória, disco e inode;
- reinício/crash de containers;
- conexões MySQL e erro de backup;
- alteração de configuração;
- certificado próximo do vencimento;
- acesso SSH e comandos privilegiados.

Criar runbook de incidente contendo:

1. detecção e triagem;
2. preservação de evidência;
3. contenção;
4. revogação de sessões e rotação de segredos;
5. avaliação de titulares/dados afetados;
6. comunicação ao controlador;
7. suporte à comunicação ANPD/titulares no prazo aplicável;
8. recuperação e lições aprendidas.

#### Critério de aceite

- alertas críticos possuem responsável e SLA;
- simulado de incidente é executado;
- equipe consegue identificar tenant, período, dados e contas afetadas sem consultar conteúdo clínico nos logs.

---

### AUD-01 — Auditoria insuficiente

**Severidade:** Alta<br>
**Estado:** Confirmado

Existem usuários/datas de criação e alteração e histórico de status, mas não há trilha de:

- leitura de paciente/histórico;
- pesquisa por identificador;
- mudança de papel;
- alteração de escopo UBS;
- sessão global de suporte;
- decisão automatizada reproduzível;
- antes/depois de mudanças relevantes;
- exportação, caso seja criada no futuro.

#### Recomendação

- registrar ator estável, tenant, UBS, ação, objeto opaco, resultado, origem confiável, horário e motivo;
- não registrar CPF/CNS/nome/observação;
- proteger contra alteração e exclusão;
- centralizar com acesso segregado;
- alertar por comportamento anômalo;
- definir retenção própria para auditoria.

#### Critério de aceite

- é possível responder quem acessou/alterou um prontuário, quando, de qual escopo e por quê;
- o administrador global não consegue apagar sua própria trilha.

---

### PRIV-01 — Minimização e RBAC incompletos

**Severidade:** Alta<br>
**Estado:** Confirmado

#### Evidência

- `src/main/jte/patientManagement/patientFragments/patient_datatable.jte:35`
- `src/main/jte/patientManagement/patientFragments/patient_info.jte:27`
- pacientes e histórico são acessíveis a qualquer usuário autenticado, com escopo UBS para parte das consultas;
- o papel `ATENDENTE` existe no projeto, mas não foi definido na matriz fornecida.

#### Recomendação

1. Criar matriz campo × ação × papel × escopo.
2. Separar:
   - listar;
   - visualizar;
   - cadastrar;
   - editar;
   - acessar histórico;
   - contemplar;
   - administrar usuários.
3. Mascarar CPF, CNS e telefone em listagens.
4. Revelar campo integral somente mediante ação e necessidade.
5. Definir o papel `ATENDENTE` ou removê-lo.
6. Aplicar autorização no servidor, nunca apenas no template.
7. Para suporte, usar dados sintéticos/pseudonimizados sempre que possível.
8. Proibir cópia de dados reais para desenvolvimento.

#### Critério de aceite

- teste de matriz cobre cada endpoint e papel;
- campos desnecessários não são enviados no HTML;
- suporte global usa acesso excepcional auditado.

---

### PRIV-02 — Ciclo de vida, correção e retenção ausentes

**Severidade:** Alta<br>
**Estado:** Confirmado

#### Evidência

- não foi encontrado fluxo de retenção, bloqueio ou anonimização;
- CPF e CNS estão com `updatable = false` em `Patient.java:37`;
- não há exportação segura para atendimento de acesso do titular;
- desativar tenant não encerra o ciclo dos dados.

#### Recomendação

1. Definir prazo e base de guarda por categoria.
2. Implementar estados:
   - ativo;
   - arquivado;
   - bloqueado/legal hold;
   - anonimizado;
   - eliminado.
3. Não excluir prontuários automaticamente sem validar obrigações sanitárias.
4. Criar correção controlada de CPF/CNS:
   - dupla verificação;
   - motivo;
   - antes/depois;
   - propagação;
   - prevenção de duplicidade.
5. Criar exportação segura e autenticada para atendimento de solicitações.
6. Aplicar retenção também a logs, backups, caches e ambientes de suporte.

#### Critério de aceite

- política aprovada é executável pelo sistema;
- correção não exige SQL manual;
- eliminação/anonimização é verificável, inclusive após expiração de backups.

---

### MIN-01 — Tratamento de crianças e adolescentes

**Severidade:** Alta<br>
**Estado:** Confirmado

O cadastro permite qualquer data de nascimento, mas não existe tratamento técnico específico para menores.

#### Recomendação

- identificar menor por data, sem depender de campo manual redundante;
- modelar responsável legal quando necessário;
- registrar fundamento/finalidade aplicável;
- aplicar melhor interesse e minimização;
- adaptar transparência e canal de exercício de direitos;
- restringir exibição e suporte;
- incluir menores no RIPD e testes de risco;
- garantir que o algoritmo da fila não cause efeito incompatível com a política assistencial.

#### Critério de aceite

- fluxos de menor/responsável são definidos e testados;
- acesso e decisão automatizada possuem salvaguardas documentadas.

---

### SUP-01 — Recursos externos e risco de cadeia de suprimentos no navegador

**Severidade:** Alta<br>
**Estado:** Confirmado

#### Evidência

- `src/main/jte/fragments/head.jte:16` carrega Alpine por jsDelivr sem SRI;
- `src/main/jte/home.jte:153` carrega ApexCharts sem versão fixa;
- `src/main/resources/static/css/styles.css:1` carrega Google Fonts;
- navegador envia metadados/IP a terceiros;
- JavaScript de terceiro executa no mesmo origin lógico da página e pode ler o DOM.

#### Recomendação

1. Preferir self-host de Alpine, ApexCharts, fontes e ícones.
2. Fixar versão e hash.
3. Se CDN permanecer, usar SRI e `crossorigin`.
4. Inventariar bibliotecas locais vendorizadas no gerenciador de dependências.
5. Aplicar CSP restritiva.
6. Revisar contratos/transferência de dados dos fornecedores que permanecerem.

#### Critério de aceite

- nenhum recurso sem versão/hash;
- indisponibilidade ou alteração do CDN não injeta código novo;
- CSP bloqueia origem não autorizada.

---

### SUP-02 — Dependências vulneráveis e ausência de SCA contínua

**Severidade:** Alta<br>
**Estado:** Confirmado

`npm audit` em 28/07/2026 encontrou:

| Pacote | Versão observada | Severidade |
|---|---:|---:|
| `postcss` | 8.5.3 | Alta |
| `brace-expansion` | 2.0.1 | Alta |
| `glob` | 10.4.1 | Alta |
| `minimatch` | 9.0.4 | Alta |
| `picomatch` | 2.3.1 / 4.0.2 | Alta |
| `yaml` | 2.4.3 | Moderada |

São majoritariamente ferramentas de build, o que reduz exploração direta no runtime Java, mas não elimina comprometimento da cadeia de build.

A varredura Java pelo OWASP Dependency-Check não terminou porque a sincronização inicial da NVD, sem API key, exigia centenas de milhares de registros. Isso não é evidência de ausência de CVEs.

#### Recomendação

- atualizar dependências npm e lockfile;
- executar testes/build após atualização;
- configurar Dependency-Check com NVD API key ou ferramenta SCA equivalente no CI;
- falhar pipeline por CVE crítica/alta explorável;
- gerar SBOM CycloneDX para JAR e imagem;
- monitorar dependências vendorizadas;
- usar Dependabot/Renovate com revisão;
- definir SLA de correção por severidade.

#### Critério de aceite

- SCA Java e npm executam em todo build;
- não há vulnerabilidade crítica conhecida no artefato;
- altas possuem correção ou aceite técnico formal com prazo;
- SBOM acompanha cada release.

---

### OBS-01 — Actuator público com detalhes

**Severidade:** Média<br>
**Estado:** Confirmado

#### Evidência

- `src/main/resources/application-prd.properties:33`
- `src/main/resources/application-prd.properties:34`
- `/actuator/health` é público e `show-details=always`.

#### Recomendação

- health público deve retornar somente estado mínimo;
- usar `show-details=never` ou `when-authorized`;
- expor métricas em porta/rede de gestão separada;
- proteger Prometheus por firewall, mTLS ou identidade de serviço;
- não usar papel de usuário da aplicação como único controle operacional.

#### Critério de aceite

- requisição pública não revela DB, disco ou componentes;
- endpoint de métricas não é acessível pela Internet.

---

### NET-02 — Configuração nginx incompleta

**Severidade:** Média<br>
**Estado:** Confirmado

#### Evidência

- a aplicação espera `X-Forwarded-For`, mas nginx não o envia;
- somente `X-Real-IP` é definido;
- não há CSP ou `Permissions-Policy`;
- não há rate limit, limite de conexões ou timeouts explícitos;
- `client_max_body_size` é 20 MB apesar de não haver upload informado.

#### Recomendação

1. Depois de restringir a origem à Cloudflare, configurar IP real usando ranges oficiais e `CF-Connecting-IP`.
2. Enviar `X-Forwarded-For` corretamente.
3. Não confiar nesses headers enquanto acesso direto à origem for possível.
4. Adicionar:
   - CSP;
   - `Permissions-Policy`;
   - timeouts de leitura/escrita/conexão;
   - limites de requisição/conexão;
   - limite de corpo compatível com o maior formulário real.
5. Redigir access logs.
6. Testar headers com ferramenta automatizada.

#### Critério de aceite

- aplicação recebe IP real não falsificável;
- requisições lentas/excessivas são limitadas;
- headers de segurança estão presentes em todas as respostas, inclusive erros.

---

### NET-03 — Redirect baseado em Host não validado

**Severidade:** Média<br>
**Estado:** Confirmado

#### Evidência

- `nginx/prd.conf:23`
- `nginx/prd.conf:26`

O primeiro servidor HTTP pode atuar como default e redireciona usando `$host`. Um Host não esperado pode ser refletido no `Location`.

#### Recomendação

- criar `default_server` separado para 80/443 que rejeite hosts desconhecidos;
- redirecionar somente nomes explicitamente autorizados;
- validar host também na Cloudflare e aplicação;
- testar Host header poisoning.

#### Critério de aceite

- Host arbitrário recebe rejeição e nunca redirecionamento controlado.

---

### INF-01 — Hardening de containers

**Severidade:** Média<br>
**Estado:** Confirmado

O container Spring executa como usuário `sigaubs`, o que é positivo. Não há, porém:

- `read_only`;
- `tmpfs` controlado;
- `cap_drop`;
- `no-new-privileges`;
- limites de PID/CPU para todos os serviços;
- perfis seccomp/AppArmor documentados;
- redes separadas entre gateway e banco.

#### Recomendação

- `security_opt: no-new-privileges:true`;
- `cap_drop: [ALL]` e adicionar somente capacidades necessárias;
- filesystem read-only com volumes/tmpfs explícitos;
- limites de memória, CPU e PIDs;
- rede `frontend` para nginx/app e `backend` para app/MySQL;
- impedir nginx de alcançar MySQL;
- nunca montar Docker socket;
- executar Docker rootless quando compatível.

#### Critério de aceite

- aplicação funciona com capacidades mínimas e filesystem read-only;
- gateway não conecta ao banco;
- escape de processo não obtém privilégio adicional.

---

### INF-02 — Imagens mutáveis e build não reprodutível

**Severidade:** Média<br>
**Estado:** Confirmado

#### Evidência

- `nginx:1.27-alpine`, `mysql:8.0` e imagens Temurin não usam digest;
- `Dockerfile.prd:7` executa script NodeSource via `curl | bash`;
- não há assinatura, provenance ou scan de imagem.

#### Recomendação

- fixar versões e digests;
- usar imagem builder oficial com Node já presente ou validar assinatura/checksum;
- gerar SBOM e provenance;
- scanear imagem em CI e periodicamente após publicação;
- assinar imagem e validar antes do deploy;
- rebuild periódico para patches;
- registrar digest efetivamente implantado.

#### Critério de aceite

- mesma revisão gera dependências identificáveis;
- deploy usa digest aprovado;
- imagem crítica vulnerável é bloqueada.

---

### AUTH-01 — Política de senha insuficiente

**Severidade:** Média<br>
**Estado:** Confirmado

#### Evidência

- `SystemUserService` valida essencialmente igualdade;
- `AdminUserManagementService.java:172`;
- `AdminSmsUserService.java:249`;
- não há comprimento mínimo ou verificação contra senhas comprometidas.

BCrypt via `DelegatingPasswordEncoder` é um controle positivo, mas o work factor não é explicitamente calibrado.

#### Recomendação

- com MFA, mínimo de oito caracteres; preferir passphrases maiores;
- sem MFA, mínimo de quinze caracteres até a migração;
- permitir comprimento amplo respeitando o limite seguro do encoder;
- bloquear senhas comuns/comprometidas;
- não impor troca periódica sem evento de risco;
- calibrar e registrar custo do hash;
- migrar hash no próximo login quando parâmetros mudarem;
- considerar Argon2id para novas credenciais se suportado operacionalmente.

#### Critério de aceite

- senha comum/comprometida é rejeitada;
- hash é calibrado e versionado;
- alteração revoga sessões.

---

### SES-01 — Cookies e timeouts

**Severidade:** Média<br>
**Estado:** Parcial

`SameSite=Lax` está explícito. `HttpOnly` e `Secure` dependem de defaults e reconhecimento correto do proxy. Não há timeout absoluto explícito.

#### Recomendação

- fixar `Secure`, `HttpOnly`, `SameSite`;
- não definir `Domain` mais amplo que o necessário;
- usar nome de cookie endurecido quando compatível;
- definir timeout ocioso e absoluto conforme risco;
- exigir reautenticação após timeout e eventos de risco;
- impedir cache de páginas autenticadas;
- validar `Set-Cookie` na implantação real.

#### Critério de aceite

- teste automatizado confirma atributos;
- sessão expira no servidor por ociosidade e tempo absoluto.

---

### ERR-01 — Exceções brutas nos logs

**Severidade:** Média<br>
**Estado:** Confirmado

Controllers registram `e.getMessage()` para erros de banco e integridade. A mensagem pode incluir SQL, nomes de colunas e valores.

#### Recomendação

- registrar código de erro, tipo da exceção e correlation ID;
- não registrar SQL/valor bruto em produção;
- sanitizar mensagens;
- manter detalhes apenas em ambiente restrito, sem PII;
- garantir respostas genéricas ao usuário.

#### Critério de aceite

- erro de constraint com CPF de teste não grava o CPF nem SQL completo.

---

### AVAIL-01 — Durabilidade reduzida no MySQL

**Severidade:** Média<br>
**Estado:** Confirmado

#### Evidência

- `docker/mysql/my.cnf:10` define `innodb_flush_log_at_trx_commit = 2`.

Pode haver perda de transações recentes em falha de energia ou sistema operacional.

#### Recomendação

- definir RPO transacional;
- para fila de saúde, preferir valor `1` se o requisito for perda próxima de zero;
- medir desempenho antes de aceitar risco;
- combinar com binlog, backup e UPS/infra;
- documentar qualquer exceção.

#### Critério de aceite

- configuração corresponde ao RPO aprovado e foi testada em recuperação.

---

### COR-01 — CORS permissivo latente

**Severidade:** Baixa atualmente; pode tornar-se alta se ativado<br>
**Estado:** Confirmado

#### Evidência

- `src/main/java/br/com/tecsus/sigaubs/security/config/WebSecurityConfig.java:190`

Existe configuração baseada em defaults permissivos com credenciais, mas as chains aparentam não habilitar CORS.

#### Recomendação

- remover a configuração se não houver chamadas cross-origin;
- se necessária, listar origens exatas por ambiente;
- nunca usar wildcard com credenciais;
- testar preflight e respostas.

#### Critério de aceite

- origem não autorizada não recebe CORS;
- configuração inativa não permanece como armadilha futura.

---

### CERT-01 — Mock empacotado e documentação divergente

**Severidade:** Baixa<br>
**Estado:** Confirmado

O PKCS#12 é mock, mas aparece dentro do JAR. O compose monta `./certs`, enquanto foi informado que produção usa pasta paralela.

#### Recomendação

- excluir mock do artefato de produção;
- manter fixture somente em resources/test ou profile dev;
- documentar caminho real sem versionar segredo;
- montar certificado/key como read-only;
- restringir permissões da chave;
- monitorar validade/renovação;
- alinhar compose versionado ao deploy real.

#### Critério de aceite

- JAR de produção não contém PKCS#12;
- configuração documentada corresponde ao servidor.

---

### GOV-01 — Governança técnica mínima ausente

**Severidade:** Alta<br>
**Estado:** Confirmado

Não existem atualmente:

- RIPD;
- inventário de tratamento;
- política de segurança;
- política de acesso;
- processo de onboarding/offboarding;
- retenção;
- plano de incidentes;
- responsáveis técnicos e de privacidade;
- SLA de vulnerabilidade;
- processo de revisão da regra automatizada;
- requisitos de segurança para fornecedores.

#### Recomendação

Criar, no mínimo:

1. inventário de dados e fluxos;
2. matriz RBAC;
3. threat model;
4. RIPD;
5. política de backup/DR;
6. política de logging/auditoria;
7. plano de incidente;
8. gestão de vulnerabilidades;
9. processo de acesso global de suporte;
10. checklist de release e mudança;
11. treinamento e termo de confidencialidade;
12. registro de suboperadores e localização dos dados.

#### Critério de aceite

- controles possuem proprietário, periodicidade, evidência e revisão;
- exceções possuem prazo e aprovação.

## 9. Configuração recomendada para Cloudflare e Hostinger

As configurações reais dos provedores não foram fornecidas. Portanto, **todos os itens desta seção estão com status “não verificado”** até que o responsável apresente evidência e execute os testes propostos. A mera marcação de uma opção no painel não substitui o teste externo.

O responsável deve copiar as tabelas para o registro de implantação e preencher, para cada item:

- `Conforme`, `Não conforme`, `Não aplicável` ou `Risco aceito`;
- evidência datada, sem segredos nem dados pessoais;
- responsável pela validação;
- data da próxima revisão;
- chamado de correção e prazo, quando não conforme.

Capturas devem ocultar IPs administrativos, tokens, chaves, cookies, nomes de pacientes e demais dados pessoais. Exportações de configuração são preferíveis a capturas, desde que sejam sanitizadas e armazenadas em repositório de evidências com acesso restrito.

### 9.1 Matriz de responsabilidade mínima

| Camada | Responsável primário | Responsabilidade que não deve ser presumida como coberta pelo fornecedor |
|---|---|---|
| Cloudflare | administrador da conta Cloudflare | proxy, WAF, rate limit, Access, TLS de borda e origem, cache, DNSSEC, logs, alertas e proteção da conta |
| Hostinger | titular/administrador da conta | proteção da conta, firewall no painel, backups/snapshots contratados, disponibilidade e recursos da VPS |
| Sistema operacional da VPS | equipe de infraestrutura da Tecsus | patching, firewall local, SSH, usuários, `sudo`, serviços, malware, integridade e logs |
| nginx/Docker/MySQL/aplicação | Tecsus | configuração segura, atualização, segredos, autorização, logs, banco, containers, certificados da origem e recuperação |
| Município/SMS | controlador ou responsável definido contratualmente | credenciamento, desligamento, revisão de acesso, IPs institucionais e uso adequado |

A documentação da Hostinger classifica a VPS como **self-managed**: o provedor não deve ser tratado como responsável por atualizar e proteger o software instalado pelo cliente.

### 9.2 Checklist verificável — Cloudflare

| ID | Controle obrigatório ou recomendado | Configuração/critério de aceite | Evidência a coletar | Validação independente |
|---|---|---|---|---|
| CF-01 | Proxy DNS | Todos os hostnames web de produção estão `Proxied`/nuvem laranja; não há `A`, `AAAA` ou `CNAME` web em modo DNS-only apontando à origem | export dos registros DNS e captura sanitizada | `dig` deve retornar IPs da Cloudflare, não o IP da VPS |
| CF-02 | Não exposição da origem | IP da origem não aparece em registros DNS atuais, históricos operacionais controláveis, subdomínios esquecidos, MX ou serviços hospedados na mesma VPS | inventário DNS/subdomínios e revisão de histórico | pesquisa externa autorizada de DNS; varredura de todos os registros |
| CF-03 | Bloqueio de bypass | A origem não aceita HTTP/HTTPS da Internet, exceto ranges oficiais Cloudflare; preferencialmente usar também AOP ou Tunnel | regras do firewall Hostinger e do SO | acesso direto autorizado ao IP da origem deve falhar, inclusive com `Host`/SNI correto |
| CF-04 | TLS visitante–Cloudflare | somente TLS 1.2/1.3; certificado válido; redirecionamento HTTP→HTTPS; HSTS ativado somente após teste de todos os subdomínios | export de SSL/TLS e certificado | teste TLS externo e `curl -I`; não aceitar HTTP sem redirecionamento |
| CF-05 | TLS Cloudflare–origem | modo **Full (strict)**; certificado da origem válido, não expirado e correspondente ao hostname | painel SSL/TLS e dados públicos do certificado, sem chave | verificar handshake na origem a partir de caminho autorizado; monitorar expiração |
| CF-06 | Autenticação da origem | Authenticated Origin Pulls com certificado próprio quando viável, ou Cloudflare Tunnel; allowlist de IP isoladamente não é tratada como prova criptográfica | configuração AOP/Tunnel e nginx sanitizada | requisição sem certificado AOP ou fora do Tunnel deve falhar |
| CF-07 | WAF gerenciado | Cloudflare Managed Ruleset e regras OWASP disponíveis no plano, ajustadas ao tráfego real; implantação inicial em log/simulação ou desafio antes de bloquear | lista de rulesets, versões, overrides, justificativas e eventos de falso positivo | casos de teste OWASP em homologação; confirmar bloqueio sem quebrar fluxo legítimo |
| CF-08 | Regras customizadas | bloquear métodos inesperados, paths técnicos não públicos, hostnames inválidos e padrões de exploração confirmados; não basear autorização de negócio no WAF | export das regras e ordem de precedência | suíte negativa em homologação e revisão da ordem das regras |
| CF-09 | Rate limiting | regras distintas para login, validação/step-up de senha, recuperação de conta, pesquisas, operações de escrita e endpoints técnicos | regras, limiares e justificativa baseada em baseline | teste controlado confirma `429`/desafio e alerta; aplicação/nginx também limitam porque rate limit de borda pode falhar aberto |
| CF-10 | Proteção do administrador global | hostname/rota exclusiva para administração da plataforma protegida por Cloudflare Access, MFA e política de grupo explícita; a autorização Spring continua obrigatória | aplicação Access, políticas `Allow`/`Deny`, IdP e grupo | usuário comum e usuário fora do grupo não alcançam sequer a tela; token Access é validado |
| CF-11 | MFA e identidade da conta | MFA resistente a phishing, preferencialmente passkey/chave FIDO2; contas nominativas; pelo menos dois administradores de contingência controlados | lista de membros, funções e métodos MFA, sem dados secretos | revisão trimestral e simulação de recuperação segura da conta |
| CF-12 | Menor privilégio | papéis Cloudflare mínimos por pessoa; sem conta compartilhada; produção separada de teste quando possível | matriz membros × papéis × zonas | revisão por segundo administrador e remoção de acessos órfãos |
| CF-13 | Tokens de API | tokens por automação, escopo mínimo de conta/zona/permissão, expiração/rotação; não usar Global API Key | inventário com proprietário, finalidade, escopo e validade, nunca o valor do token | tentativa com token de teste confirma que ações fora do escopo são negadas |
| CF-14 | Cache de conteúdo sensível | rotas autenticadas, respostas com sessão, erros e dados de pacientes não são armazenados; origem envia `Cache-Control: no-store, private` quando aplicável; nenhuma regra `Cache Everything` alcança o sistema | regras de cache e cabeçalhos por rota | respostas autenticadas não podem retornar `CF-Cache-Status: HIT`; testar dois usuários/tenants distintos |
| CF-15 | Cookies e transformação | nenhuma Worker/Transform Rule remove `Secure`, `HttpOnly`, `SameSite`, `Cache-Control`, CSP ou cabeçalhos de segurança | inventário de Workers, Rules e Snippets | comparar cabeçalhos na origem homologada e na borda |
| CF-16 | IP real do cliente | nginx só confia em `CF-Connecting-IP`/proxy headers quando a conexão vem de ranges oficiais da Cloudflare; ranges IPv4 e IPv6 são atualizados | `set_real_ip_from`, `real_ip_header` e processo de atualização | envio direto/spoof de `X-Forwarded-For` não altera IP de auditoria; teste via borda registra IP correto |
| CF-17 | DNSSEC | DNSSEC ativado e DS corretamente publicado no registrador | status DNSSEC e registro DS | `dig +dnssec`/validador externo sem erro |
| CF-18 | DDoS/bots | proteções automáticas mantidas; desafio/bot control somente onde compatível com profissionais e acessibilidade; não depender de CAPTCHA como único controle | configurações de DDoS/bot e exceções | teste de fluxo com navegadores/dispositivos institucionais e monitoramento de bloqueios |
| CF-19 | Logs e privacidade | Security Events, Access e auditoria de conta com acesso restrito, retenção definida e export seguro; minimizar query strings/corpos e dados pessoais | política de logging, destino, retenção, ACL e amostra sanitizada | busca por CPF/CNS/nome/token em amostra; acesso ao log é auditado |
| CF-20 | Alertas | alertas para alteração de DNS, WAF, Access, membros, tokens, certificados, indisponibilidade e picos de bloqueio/login | catálogo de alertas, canais e escala | teste periódico entrega alerta ao plantonista e gera evidência |
| CF-21 | Mudanças e auditoria | Audit Logs revisados; mudanças críticas passam por dupla revisão e procedimento de rollback | export de audit log, change record e rollback | simulação de mudança controlada e restauração |
| CF-22 | Separação de ambientes | homologação e produção não compartilham tokens, Access apps, certificados, credenciais ou origem; ambiente de teste não contém dados reais | inventário por ambiente | teste cruzado confirma que credencial de homologação não atua em produção |

#### 9.2.1 Procedimento de validação externa — Cloudflare

Executar de uma estação autorizada, substituir os placeholders e guardar apenas resultados sanitizados:

```bash
# O resultado deve ser endereço da Cloudflare, nunca o IP da VPS.
dig +short <HOSTNAME_PRODUCAO> A
dig +short <HOSTNAME_PRODUCAO> AAAA

# Deve redirecionar para HTTPS; cabeçalhos não devem permitir cache autenticado.
curl --head http://<HOSTNAME_PRODUCAO>/login
curl --head https://<HOSTNAME_PRODUCAO>/login

# Executar somente com autorização e IP de origem conhecido.
# Deve falhar por timeout/rejeição ou por autenticação de origem.
curl --resolve <HOSTNAME_PRODUCAO>:443:<IP_ORIGEM> \
  https://<HOSTNAME_PRODUCAO>/login

# Validar DNSSEC.
dig +dnssec <HOSTNAME_PRODUCAO>
```

Para cache, o teste deve autenticar duas contas de tenants diferentes em clientes isolados e comprovar que nenhuma resposta privada é servida ao outro cliente. Não registrar cookies ou corpo contendo dados pessoais. `CF-Cache-Status: DYNAMIC` ou `BYPASS` ajuda na evidência; a prova principal é a política correta na origem e a ausência de `HIT` em conteúdo privado.

O teste de bypass precisa cobrir IPv4 e IPv6. Restringir apenas IPv4 deixa a origem exposta quando a VPS possui endereço IPv6. Se for adotado Cloudflare Tunnel, as portas web de entrada devem permanecer fechadas, o token do Tunnel deve ser tratado como segredo, `cloudflared` deve ser atualizado e executado como serviço não privilegiado quando suportado.

### 9.3 Checklist verificável — Hostinger, VPS e sistema operacional

| ID | Controle obrigatório ou recomendado | Configuração/critério de aceite | Evidência a coletar | Validação independente |
|---|---|---|---|---|
| VPS-01 | Conta Hostinger | MFA/passkey ativada, contas nominativas e recuperação protegida; credenciais não compartilhadas | membros/colaboradores, papéis e MFA, sanitizados | revisão trimestral e teste documentado de recuperação |
| VPS-02 | Menor privilégio no painel | colaboradores recebem apenas acessos necessários; desligamento remove acesso, sessões e chaves | matriz de acesso e último recertification review | amostragem de desligamento e revisão de contas órfãs |
| VPS-03 | Firewall Hostinger | política default-deny; 443 apenas ranges Cloudflare se não houver Tunnel; 80 apenas se necessário e também via Cloudflare; SSH apenas origem administrativa aprovada | export/captura das regras IPv4 e IPv6 | varredura externa autorizada confirma somente portas esperadas |
| VPS-04 | Firewall do SO | `nftables`/`ufw` replica a política mínima e persiste após reboot; mudanças são versionadas/revisadas | ruleset sanitizado e teste pós-reboot | scan externo e conexão negativa às portas bloqueadas |
| VPS-05 | Portas proibidas | MySQL `3306`, Spring `8080`, Docker `2375/2376`, métricas, Actuator, Portainer e painéis não são públicos | listeners e mapeamentos Docker | scan TCP externo em IPv4/IPv6 e verificação de `ss` |
| VPS-06 | SSH | somente chave/passkey aprovada; `PasswordAuthentication no`; `PermitRootLogin no`; usuários nominativos; timeout; algoritmo moderno; origem restrita | saída sanitizada de `sshd -T`, usuários e chaves autorizadas | login por senha e como root falha; chave revogada falha |
| VPS-07 | Administração remota | preferir IP administrativo fixo ou acesso identity-aware via Tunnel/Access; break-glass documentado, monitorado e com validade | fluxo, grupos, IPs e conta de emergência | exercício de acesso e de revogação |
| VPS-08 | `sudo` e contas locais | usuário de deploy sem privilégio permanente; `sudo` mínimo e auditado; contas de serviço sem shell; `umask` adequada | `/etc/sudoers.d` sanitizado, lista de contas e logs | teste de comandos fora do escopo e revisão de eventos |
| VPS-09 | Patching | distribuição suportada; atualizações de segurança automáticas ou SLA formal; inventário e reinício planejado; kernel/runtime atualizados | versão do SO, política, data do último patch e pendências | scanner autenticado e verificação periódica de pacotes |
| VPS-10 | Hardening do SO | serviços/pacotes não usados removidos; NTP ativo; core dumps restritos; limites e sysctl revisados segundo baseline aplicável | baseline CIS/ANSSI adotado, exceções e relatório | auditoria automatizada e amostragem manual |
| VPS-11 | Detecção de abuso/malware | scanner Hostinger é consultado quando disponível, mas não substitui EDR/FIM/logs; alertar por binários, cron, chaves e processos inesperados | política, agentes ativos e último scan | arquivo de teste seguro quando suportado e simulação de alerta |
| VPS-12 | Docker daemon | socket não exposto por TCP nem montado no app/nginx; grupo `docker` restrito; daemon e Compose atualizados | configuração do daemon, grupos e listeners | `ss` confirma ausência de 2375/2376; revisão de mounts |
| VPS-13 | Containers | usuário não root, `read_only`, `no-new-privileges`, capabilities removidas, limites CPU/memória/PIDs, healthchecks e filesystem temporário mínimo, após teste de compatibilidade | Compose efetivo sanitizado e `docker inspect` sem a seção `Env` | teste funcional e tentativa controlada de escrita/elevação em homologação |
| VPS-14 | Imagens | digests fixos ou processo reprodutível; imagens mínimas, assinadas/verificadas quando possível e escaneadas; rebuild periódico | SBOM, digest, scan e origem da imagem | pipeline falha em vulnerabilidade acima do limite aprovado |
| VPS-15 | Segmentação Docker | somente nginx publica porta; app e MySQL em redes internas; banco não se conecta desnecessariamente à Internet | redes e portas efetivas | teste de conectividade entre containers e do exterior |
| VPS-16 | Segredos e arquivos | segredos fora da imagem/Git; permissões mínimas; chave da origem legível só por nginx; rotação e inventário; valores não aparecem em tickets/logs | nomes/localização lógica, ACL e datas de rotação, nunca os valores | teste com usuário/app confirma acesso negado ao que não necessita |
| VPS-17 | Banco MySQL | bind/rede interna; conta da aplicação sem `root`, sem `GRANT OPTION`, `FILE`, `SUPER` ou administração; senhas distintas; grants por schema; conexões e erros monitorados | `SHOW GRANTS` sanitizado, usuários e topologia | login da aplicação não cria usuário, não acessa outro schema e não lê arquivos |
| VPS-18 | Criptografia em repouso | verificar por escrito se volume/snapshot do provedor é criptografado e quem gerencia chaves; se não houver garantia, adotar compensação para banco/backups e registrar risco residual | contrato/documentação do plano, arquitetura de chaves | restauração controlada comprova criptografia e acesso apenas com credencial/chave autorizada |
| VPS-19 | Certificado/chave nginx | chave fora do projeto, permissão `0400`/`0440`, proprietário/grupo mínimo, renovação automática e alerta; certificado válido para Full (strict) | caminho lógico, ACL, emissor e validade, sem chave | teste de renovação e monitor de expiração; app não lê a chave |
| VPS-20 | Backup de banco | backup lógico/físico consistente no mínimo diário ou conforme RPO aprovado; binlog/PITR quando RPO exigir; job monitora falhas e volume | política, logs de job, checksum e catálogo | restauração periódica em ambiente isolado e validação de consistência |
| VPS-21 | Independência do backup | snapshot semanal da Hostinger não é a única cópia; manter cópia criptografada fora da VPS e, preferencialmente, fora da conta/provedor, com imutabilidade | destinos, retenção, criptografia e separação de contas | simular perda da VPS/conta e restaurar sem depender dela |
| VPS-22 | Retenção e descarte | retenção de backups alinhada à política LGPD; expiração automática; descarte seguro de exportações temporárias | tabela de retenção e execução de expurgo | amostragem confirma ausência após expiração |
| VPS-23 | RPO/RTO e continuidade | RPO/RTO aprovados; single VPS reconhecida como ponto único de falha; runbook reproduz VPS, DNS, secrets e banco | BIA, runbook, IaC/config backup e contatos | exercício anual ou após mudança relevante mede tempos reais |
| VPS-24 | Monitoramento de recursos | CPU, RAM, disco, inodes, I/O, rede, reinícios, processos, containers e MySQL com baseline e alertas | dashboards, regras e teste de canal | indução segura em homologação gera e encerra incidente |
| VPS-25 | Monitoramento de segurança | alertas de SSH, `sudo`, firewall, alteração de arquivos, novos usuários/chaves, Docker, falhas de login e anomalias de banco | casos de detecção, retenção e responsáveis | simulações mapeadas aos casos geram alerta com contexto suficiente |
| VPS-26 | Logs | centralização fora da VPS ou cópia protegida; sincronização de tempo; acesso mínimo; integridade; sem CPF/CNS/nome/token/senha | arquitetura, ACL, retenção e amostras sanitizadas | busca automatizada por padrões sensíveis e teste de indisponibilidade do coletor |
| VPS-27 | Resposta a incidente | contatos Tecsus/município/Hostinger/Cloudflare, preservação de evidências, isolamento, rotação, restauração e avaliação de notificação à ANPD/titulares | plano aprovado e árvore de acionamento | tabletop semestral com cenário de exfiltração de dados de saúde |
| VPS-28 | Gestão de vulnerabilidade | scans autenticados do SO, imagens, Maven/npm e DAST; SLA por severidade; exceções aprovadas e temporárias | relatórios, tickets, SLA e aceite | amostra de CVE crítica demonstra correção dentro do prazo |
| VPS-29 | Egress | saída da VPS limitada/monitorada conforme dependências reais; bloquear SMTP, proxies e destinos desnecessários quando operacionalmente seguro | regras e inventário de dependências | app funciona com policy aplicada e tentativa não autorizada é bloqueada |
| VPS-30 | Inventário e mudança | inventário de VPS, IPs, domínios, volumes, containers, versões, administradores e subprocessadores; mudanças críticas revisadas e reversíveis | CMDB/inventário e change records | confronto trimestral com o ambiente real |

#### 9.3.1 Procedimento de validação local — VPS

Executar com usuário autorizado. Os comandos são exemplos de coleta e devem ser adaptados à distribuição. Não copiar valores de ambiente de containers, arquivos de segredo, chaves privadas ou hashes de senha para o relatório.

```bash
# Listeners locais: investigar tudo que estiver em 0.0.0.0 ou [::].
sudo ss -lntup

# Usar apenas o gerenciador realmente instalado.
sudo nft list ruleset
sudo ufw status verbose

# Conferir a configuração efetiva do SSH sem exibir chaves.
sudo sshd -T | grep -E \
  '^(permitrootlogin|passwordauthentication|pubkeyauthentication|maxauthtries|clientaliveinterval|clientalivecountmax) '

# Portas publicadas e redes; não imprime variáveis de ambiente.
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}'
docker network ls

# Identidade, capabilities, modo somente leitura e mounts.
# Sanitizar caminhos sensíveis antes de anexar como evidência.
docker inspect --format \
  '{{.Name}} user={{.Config.User}} readonly={{.HostConfig.ReadonlyRootfs}} privileged={{.HostConfig.Privileged}} caps_add={{json .HostConfig.CapAdd}} caps_drop={{json .HostConfig.CapDrop}} mounts={{json .Mounts}}' \
  <CONTAINER>
```

Executar dentro do cliente MySQL com conta administrativa autorizada, sem colocar senha na linha de comando ou histórico:

```sql
SHOW GRANTS FOR '<USUARIO_APLICACAO>'@'%';
```

De uma estação externa controlada, executar varredura **somente após autorização formal** e contra os IPs explicitamente incluídos no escopo:

```bash
# Exemplos de portas que não devem estar públicas.
nmap -Pn -sT -p 22,80,443,3306,8080,2375,2376 <IP_ORIGEM>
nmap -6 -Pn -sT -p 22,80,443,3306,8080,2375,2376 <IPV6_ORIGEM>
```

Resultado esperado sem Tunnel:

- `443` aceita conexões apenas quando a origem é um IP oficial Cloudflare e o nginx exige os controles definidos;
- `22` aceita apenas IP administrativo/fluxo aprovado;
- `3306`, `8080`, `2375`, `2376`, métricas e painéis estão fechados;
- em varredura comum da Internet, até `443` da origem deve estar filtrada/rejeitada.

Resultado esperado com Tunnel:

- portas web de entrada fechadas para toda a Internet;
- apenas as saídas necessárias ao Tunnel e dependências aprovadas;
- SSH também via fluxo identity-aware ou allowlist administrativa restrita.

#### 9.3.2 Backup e restauração — critérios adicionais

O backup semanal informado produz, no pior caso, perda de quase sete dias de registros clínico-administrativos. Esse RPO precisa ser formalmente aprovado ou reduzido. Além disso:

1. snapshot de VPS não prova consistência transacional do MySQL;
2. backup armazenado somente no mesmo provedor/conta não cobre perda de conta, erro do provedor ou ataque ao painel;
3. backup sem teste de restauração é apenas uma expectativa;
4. a cópia deve ser criptografada antes ou durante o envio, com chave separada e processo de recuperação testado;
5. a restauração deve validar schema, quantidade de registros, integridade referencial, capacidade de login e isolamento entre tenants;
6. o exercício não deve reutilizar dados reais em ambiente menos protegido; usar dados anonimizados ou ambiente isolado com controles equivalentes;
7. falha, atraso ou tamanho anômalo do backup deve gerar alerta.

A documentação da Hostinger informa backups automáticos semanais e restauração pelo painel para o serviço aplicável. Isso é útil, mas não substitui backup de banco consistente e independente nem o RPO/RTO definido pelo negócio.

### 9.4 Allowlist municipal

Allowlist é apropriada quando cada município possui IP de saída fixo e redundante. Deve haver:

- inventário de IPs;
- processo de alteração/emergência;
- mais de um IP quando houver redundância;
- teste de IPv4 e IPv6;
- MFA mesmo dentro da allowlist.

Para equipes móveis ou IP dinâmico, usar identidade, MFA e postura do dispositivo; não criar ranges amplos que anulem o controle.

Allowlist é defesa adicional, não substitui autenticação, MFA, autorização por UBS/tenant, revogação nem logs. Uma allowlist por município também não pode, por si só, decidir o tenant: o tenant deve continuar derivado do hostname/contexto validado e da conta autenticada.

### 9.5 Modelo de termo de validação da infraestrutura

Antes do go-live, o responsável técnico deve preencher e assinar um registro com:

| Campo | Preenchimento obrigatório |
|---|---|
| ambiente/hostname | hostname e identificador da VPS, sem credenciais |
| data e responsáveis | executor e revisor independente |
| versão da configuração | commit, export ou change request correspondente |
| itens verificados | IDs `CF-*` e `VPS-*` com status |
| evidências | links para cofre/repositório de acesso restrito |
| testes externos | origem IPv4/IPv6, portas, TLS, cache, WAF, rate limit e Access |
| testes internos | firewall, SSH, Docker, MySQL, logs, backup e restore |
| desvios | risco, probabilidade, impacto, compensação, proprietário e prazo |
| aprovação | segurança, responsável pelo sistema e representante do controlador |

O aceite não deve usar apenas “Cloudflare ativada”, “firewall ativo” ou “backup habilitado”. Deve informar qual política foi testada, contra qual ativo, em qual data e com qual resultado.

## 10. Plano priorizado de remediação

### P0 — antes de qualquer dado real

- [ ] APP-01: eliminar XSS.
- [ ] APP-02: autorização por objeto/UBS em todas as mutações.
- [ ] IAM-01: impedir elevação de papel e separar administradores.
- [ ] LOG-01: remover PII de logs e URLs.
- [ ] IAM-02: MFA e anti-automação.
- [ ] IAM-03/IAM-04: revogação e step-up server-side.
- [ ] APP-03: DTOs e validação.
- [ ] BUS-01: aprovar/versionar/expor critérios e revisão.
- [ ] BUS-02: remover GET e garantir concorrência/idempotência.
- [ ] DB-01/DB-02: integridade e usuário mínimo.
- [ ] NET-01: bloquear acesso direto à origem.
- [ ] BAK-01: backup de banco e restore testado.
- [ ] OPS-01: monitoração mínima e plano de incidente.

### P1 — antes do go-live formal

- [ ] criptografia de volume/backups e decisão sobre campos;
- [ ] auditoria de leitura e suporte global;
- [ ] matriz RBAC e mascaramento;
- [ ] controles para menores;
- [ ] self-host/SRI/CSP;
- [ ] SCA/SBOM e atualização das dependências;
- [ ] actuator privado;
- [ ] hardening nginx/containers/VPS;
- [ ] política de sessão e senha;
- [ ] pentest autenticado e correção dos achados.

### P2 — maturidade contínua

- [ ] DR periódico;
- [ ] DAST/SAST contínuo;
- [ ] assinatura/provenance de imagens;
- [ ] testes de abuso e fraude;
- [ ] revisão periódica do algoritmo;
- [ ] simulado de incidente;
- [ ] revisão de acessos e contas inativas;
- [ ] métricas de SLA de vulnerabilidade;
- [ ] exercício de portabilidade/correção/retensão.

## 11. Testes de segurança mínimos a adicionar

### Autorização

- usuário UBS A não lê/altera/cancela dados da UBS B;
- tenant A não referencia entidade do tenant B;
- SMS não cria papel global;
- coordenador não acessa outro tenant;
- suporte global exige step-up, motivo e tenant;
- conta desativada perde a sessão imediatamente.

### Entrada e saída

- payloads XSS armazenados;
- caracteres de controle em logs;
- CPF/CNS inválidos;
- campos acima do limite;
- datas inválidas/futuras;
- page size excessivo;
- parâmetros de estado manipulados.

### Sessão/autenticação

- credential stuffing/rate limit;
- MFA e recuperação;
- session fixation;
- timeout ocioso/absoluto;
- revogação cross-instance;
- CSRF em todos os verbos de escrita;
- GET sem efeito colateral.

### Concorrência

- duas execuções do job;
- duas contemplações para a mesma vaga;
- retry após falha parcial;
- duas requisições administrativas simultâneas;
- saldo nunca abaixo de zero/acima do total.

### Infra

- acesso direto ao IP;
- Host header arbitrário;
- 3306/8080 externos;
- actuator público;
- cookies e headers;
- cache Cloudflare;
- restore de backup;
- scan de imagem/dependência.

## 12. Evidências positivas

- CSRF ativo nas chains.
- HTTP Basic desativado.
- BCrypt/DelegatingPasswordEncoder.
- logout invalida sessão e cookie.
- máximo de três sessões concorrentes.
- frame options `DENY`.
- TLS 1.2/1.3 no nginx.
- HSTS, `nosniff` e `no-referrer`.
- banco não publicado no compose.
- aplicação não publicada diretamente.
- processo Java sem root.
- certificados montados read-only no compose.
- isolamento Hibernate por tenant.
- limpeza de TenantContext.
- teste de isolamento JPA.
- escopo de leitura por UBS para usuários comuns.
- queries parametrizadas; nenhuma SQL injection evidente foi encontrada.
- `.env` ignorado pelo Git e sem histórico encontrado.
- certificado/seed confirmados como mock.
- 177 testes passaram sem falha.
- cobertura JaCoCo aproximada de 82%.

Esses controles devem ser preservados durante a correção.

## 13. Critério técnico final de aprovação

O sistema poderá ser reavaliado para produção quando houver evidência de:

- zero achado crítico aberto;
- nenhum achado alto diretamente explorável;
- pentest autenticado sem crítico/alto pendente;
- ASVS 5.0 nível compatível com aplicação de dados sensíveis;
- MFA e controle de suporte global;
- testes cross-UBS/cross-tenant;
- algoritmo reproduzível e revisável;
- origem Cloudflare protegida;
- banco com privilégio mínimo;
- backup restaurável dentro de RPO/RTO;
- observabilidade sem PII;
- plano de incidente testado;
- SCA/SBOM por release;
- matriz de acesso aprovada.

## 14. Referências

### LGPD e ANPD

- [Lei nº 13.709/2018 — texto compilado](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm)
- [Serpro — o que muda com a LGPD](https://www.serpro.gov.br/lgpd/menu/a-lgpd/o-que-muda-com-a-lgpd)
- [MDS — princípios e conceitos da LGPD](https://www.gov.br/mds/pt-br/acesso-a-informacao/governanca/integridade/campanhas/lgpd)
- [ANPD — Comunicação de Incidente de Segurança](https://www.gov.br/anpd/pt-br/canais_atendimento/agente-de-tratamento/comunicado-de-incidente-de-seguranca-cis)
- [ANPD — Relatório de Impacto à Proteção de Dados](https://www.gov.br/anpd/pt-br/canais_atendimento/agente-de-tratamento/relatorio-de-impacto-a-protecao-de-dados-pessoais-ripd)
- [ANPD — Guia de Segurança da Informação](https://www.gov.br/anpd/pt-br/centrais-de-conteudo/materiais-educativos-e-publicacoes/guia-orientativo-sobre-seguranca-da-informacao-para-agentes-de-tratamento-de-pequeno-porte)

### Segurança de aplicação

- [OWASP ASVS 5.0](https://owasp.org/www-project-application-security-verification-standard/)
- [OWASP — Cross Site Scripting Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)
- [OWASP — Authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [OWASP — Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [OWASP — Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)
- [OWASP — Logging](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
- [OWASP — Third Party JavaScript](https://cheatsheetseries.owasp.org/cheatsheets/Third_Party_Javascript_Management_Cheat_Sheet.html)
- [OWASP — Docker Security](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html)

### Cloudflare e Hostinger

- [Cloudflare — Full (strict)](https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/full-strict/)
- [Cloudflare — Authenticated Origin Pulls](https://developers.cloudflare.com/ssl/origin-configuration/authenticated-origin-pull/)
- [Cloudflare — IP addresses e proteção da origem](https://developers.cloudflare.com/fundamentals/concepts/cloudflare-ip-addresses/)
- [Cloudflare — proteção do servidor de origem](https://developers.cloudflare.com/fundamentals/security/protect-your-origin-server/)
- [Cloudflare Tunnel](https://developers.cloudflare.com/tunnel/)
- [Cloudflare WAF](https://developers.cloudflare.com/waf/)
- [Cloudflare — regras gerenciadas](https://developers.cloudflare.com/waf/managed-rules/)
- [Cloudflare — rate limiting](https://developers.cloudflare.com/waf/rate-limiting-rules/)
- [Cloudflare — cache e `Cache-Control`](https://developers.cloudflare.com/cache/concepts/cache-control/)
- [Cloudflare Access — aplicações self-hosted](https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/)
- [Cloudflare Access — MFA](https://developers.cloudflare.com/cloudflare-one/access-controls/policies/mfa-requirements/)
- [Cloudflare — Audit Logs](https://developers.cloudflare.com/fundamentals/account/account-security/audit-logs/)
- [Cloudflare — permissões de tokens de API](https://developers.cloudflare.com/fundamentals/api/reference/permissions/)
- [Hostinger — VPS self-managed](https://support.hostinger.com/en/articles/8852150-what-is-a-self-managed-vps)
- [Hostinger — firewall gerenciado da VPS](https://support.hostinger.com/en/articles/8172641-how-to-use-a-managed-vps-firewall)
- [Hostinger — backup e restauração de VPS](https://support.hostinger.com/en/articles/1583232-how-to-back-up-or-restore-a-vps)
- [Hostinger — proteção da VPS contra abuso](https://support.hostinger.com/en/articles/8224050-how-to-secure-your-vps-from-abusive-activity)
- [Hostinger — scanner de malware da VPS](https://support.hostinger.com/en/articles/8450363-vps-malware-scanner)
- [Hostinger — monitoramento de recursos da VPS](https://support.hostinger.com/en/articles/4725768-how-to-check-vps-resources-usage)

## 15. Registro das verificações executadas

| Verificação | Resultado |
|---|---|
| `./mvnw verify` | 177 testes; 0 falhas; 0 erros |
| JaCoCo | aproximadamente 82% de instruções/linhas cobertas |
| `npm audit` | 5 altas, 1 moderada, 0 críticas |
| busca por segredos versionados | nenhum segredo real evidente; fixtures confirmadas como mock |
| histórico Git de `.env`/`.env.prd` | não encontrado |
| Dependency-Check Java | inconclusivo por sincronização NVD inicial sem API key |
| DAST/pentest | não executado; requer ambiente implantado de homologação |

---

**Parecer técnico atual:** risco **alto/crítico**; implantação com dados reais **não aprovada** até remediação e nova verificação.
