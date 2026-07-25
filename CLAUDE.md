# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SIGA-UBS is a Brazilian healthcare management system for Basic Health Units (UBS - Unidades Básicas de Saúde). It manages patients, medical appointments, specialties, medical procedures, and an automated contemplation/scheduling routine that assigns patients to available medical slots based on priority criteria.

## Build & Run Commands

**Backend (Maven + Spring Boot 4, Java 21):**
```bash
./mvnw spring-boot:run              # Run with dev profile (default)
./mvnw clean package                # Build JAR (output: sigaubs-0.0.1-SNAPSHOT-dev.jar)
./mvnw test                         # Run all tests
./mvnw test -Dtest=TestClassName    # Run a single test class
```

**Frontend (Tailwind CSS via PostCSS):**
```bash
npm run watch:postcss    # Watch mode - rebuilds CSS on changes (use during development)
npm run build:postcss    # One-time CSS build (minified)
```

Both the Spring Boot app and `npm run watch:postcss` need to run simultaneously during development.

## Database

MySQL on `localhost:3306/sigaubs`. Dev credentials are configured through `.env` and `application-dev.properties`.
The Docker schema source is `docker/mysql/01-schema.sql`; `src/main/resources/database.sql` mirrors that final DDL for reference. Development seed data lives in `docker/mysql/02-seed.sql`; `src/main/resources/inserts.sql` is a small optional fixture for manual multi-tenant isolation checks.

## Architecture

Base package: `br.com.tecsus.sigaubs`. Standard Spring MVC layered architecture with server-side rendered JTE templates:

- **`controllers/`** - Spring MVC controllers returning JTE views/fragments.
- **`services/`** - Business logic with `@Transactional` boundaries. `SystemUserService` also implements `UserDetailsService`.
- **`repositories/`** - Spring Data JPA repositories. Complex queries use a custom repository pattern: interface `FooRepositoryCustom` + implementation `repositories/Impl/FooRepositoryCustomImpl` using `EntityManager` directly.
- **`entities/`** - JPA entities with Lombok `@Getter`/`@Setter` and `@DynamicUpdate`. Uses custom `AttributeConverter` classes for enums (`SocialSituationAttrConverter`, `PriorityConverter`, `AppointmentStatusConverter`, `YearMonthDateAttributeConverter`).
- **`dtos/`** - Data transfer objects for view/query projections (Java records).
- **`enums/`** - Domain enumerations (`Priorities`, `AppointmentStatus`, `ProcedureType`, `Roles`, `SocialSituationRating`, etc.).
- **`security/`** - Spring Security 6 config with form login, BCrypt, role-based auth (`ADMIN`, `SMS`, `ATENDENTE`, `ENFERMEIRO`, `ACS`, `USER`). Max 1 concurrent session per user. URL patterns defined in `UrlPatternConfig`. Authenticated principal accessed via `@AuthenticationPrincipal SystemUserDetails`.
- **`jobs/`** - Scheduled tasks. `ContemplationScheduleV2` is the **active** contemplation routine (`ContemplationSchedule` is the old/inactive version). Cron configurable via `schedule.cron.contemplation` property (default: daily at midnight). Uses Spring Retry (max 4 attempts, 5s backoff).
- **`utils/`** - `ContemplationScheduleStatus` tracks job state globally (static fields). `DefaultValues` holds domain constants (e.g. `QUATRO_MESES = 4`).

**Frontend stack:** JTE + Tailwind CSS + HTMX/JavaScript. Templates in `src/main/jte`, grouped by feature module. Shared layout fragments are in `src/main/jte/fragments`. Controllers return full pages or partial JTE fragments for HTMX-style updates. HTML forms use PUT/DELETE via `spring.mvc.hiddenmethod.filter.enabled=true`.

## Key Domain Model

```
Specialty --(1:N)--> MedicalProcedure (types: CONSULTA, EXAME, CIRURGIA)
BasicHealthUnit --(1:N)--> Patient
BasicHealthUnit --(1:N)--> SystemUser
Patient --(1:N)--> Appointment --(N:1)--> MedicalProcedure
Appointment --(1:N)--> AppointmentStatusHistory
Appointment --(1:1)--> Contemplation --(N:1)--> MedicalSlot
MedicalSlot --(N:1)--> MedicalProcedure, BasicHealthUnit
```

## Contemplation Priority Logic

The contemplation routine (`ContemplationScheduleV2`) selects patients from the waiting queue ordered by these tiebreaker rules (in order):

1. Appointments older than 4 months (`MAIS_DE_QUATRO_MESES`, value=1)
2. Manual priority value (lower `Priorities.value` = higher priority: `URGENCIA=2`, `RETORNO=3`, `PRIORITARIO=4`, `ELETIVO=8`)
3. Patient age (older patients first, `birthDate ASC`) → `IDADE`
4. Social situation rating → `SITUACAO_SOCIAL`
5. Gender (Feminino before Masculino) → `SEXO`
6. Appointment request date (FIFO) → `DATA_DA_MARCACAO`

The `contemplatedBy` field on `Contemplation` records which rule was the deciding factor. Admin-initiated contemplations use `Priorities.ADMINISTRATIVO` (value=9).

## Important Configuration Notes

- `spring.jpa.open-in-view=false` — Entities must not be lazily loaded outside transactions. Use entity graphs or DTOs.
- Hibernate batch size is 50 for bulk operations.
- HikariCP pool: max 40 connections, `READ_COMMITTED` isolation, auto-commit disabled.
- Active Spring profile is `dev` by default (`application.properties` → `application-dev.properties`). Production uses the `prd` profile.
- Virtual threads enabled (`spring.threads.virtual.enabled=true`).
- Static resources (JS/CSS) use content-based versioning in dev profile.

## Language

The codebase, commit messages, and comments are primarily in **Portuguese (Brazilian)**. Follow this convention.
