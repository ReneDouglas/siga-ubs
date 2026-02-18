# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SIGA-UBS is a Brazilian healthcare management system for Basic Health Units (UBS - Unidades Básicas de Saúde). It manages patients, medical appointments, specialties, medical procedures, and an automated contemplation/scheduling routine that assigns patients to available medical slots based on priority criteria.

## Build & Run Commands

**Backend (Maven + Spring Boot 3.3.4, Java 17):**
```bash
./mvnw spring-boot:run              # Run with dev profile (default)
./mvnw clean package                # Build JAR (output: SCCUBS-0.0.1-SNAPSHOT-dev.jar)
./mvnw test                         # Run all tests
./mvnw test -Dtest=TestClassName    # Run a single test class
```

**Frontend (Tailwind CSS):**
```bash
npm run watch:postcss    # Watch mode - rebuilds CSS on changes (use during development)
npm run build:postcss    # One-time CSS build (minified)
```

Both the Spring Boot app and `npm run watch:postcss` need to run simultaneously during development.

## Database

MySQL on `localhost:3306/sigaubs`. Dev credentials: `root/root` (overridable via `PASSWORD` env var). Schema DDL in `src/main/resources/database.sql`, seed data in `src/main/resources/inserts.sql`.

## Architecture

Standard Spring MVC layered architecture with server-side rendered Thymeleaf templates:

- **`controllers/`** - Spring MVC controllers returning Thymeleaf views. Some use `@SessionScope`.
- **`services/`** - Business logic with `@Transactional` boundaries. `SystemUserService` also implements `UserDetailsService`.
- **`repositories/`** - Spring Data JPA repositories, some with custom implementations for complex queries.
- **`entities/`** - JPA entities with Lombok annotations. Uses custom `AttributeConverter` classes for enums (`SocialSituationAttrConverter`, `PriorityConverter`, `AppointmentStatusConverter`, `YearMonthDateAttributeConverter`).
- **`dtos/`** - Data transfer objects for view/query projections.
- **`enums/`** - Domain enumerations (`Priorities`, `AppointmentStatus`, `ProcedureType`, `Roles`, etc.).
- **`security/`** - Spring Security 6 config with form login, BCrypt, role-based auth (`ADMIN`, `SMS`, `ATENDENTE`, `ENFERMEIRO`, `ACS`, `USER`). Max 1 concurrent session per user.
- **`jobs/`** - Scheduled tasks. `ContemplationScheduleV2` is the active contemplation routine (cron: daily at midnight) with Spring Retry (max 4 attempts).

**Frontend stack:** Thymeleaf + Tailwind CSS + Alpine.js + ApexCharts. Templates in `src/main/resources/templates/` with reusable fragments in `templates/fragments/`.

## Key Domain Model

```
Specialty --(1:N)--> MedicalProcedure (types: CONSULTA, EXAME, CIRURGIA)
BasicHealthUnit --(1:N)--> Patient
BasicHealthUnit --(1:N)--> SystemUser
Patient --(1:1)--> Appointment --(1:1)--> MedicalProcedure
Appointment --(1:1)--> Contemplation --(N:1)--> MedicalSlot
MedicalSlot --(1:1)--> MedicalProcedure, BasicHealthUnit
```

The contemplation routine processes appointments by UBS and procedure, selecting patients by priority and assigning them to available medical slots.

## Important Configuration Notes

- `spring.jpa.open-in-view=false` - Entities must not be lazily loaded outside transactions. Use entity graphs or DTOs.
- Hibernate batch size is 50 for bulk operations.
- HikariCP pool: max 40 connections, `READ_COMMITTED` isolation, auto-commit disabled.
- Active Spring profile is `dev` by default (`application.properties` → `application-dev.properties`).
- Virtual threads enabled (`spring.threads.virtual.enabled=true`).

## Language

The codebase, commit messages, and comments are primarily in **Portuguese (Brazilian)**. Follow this convention.
