# Repository Guidelines

## Project Structure & Module Organization

SIGA-UBS is a Java 21 Spring Boot application with server-rendered JTE views and Tailwind CSS assets. Java code lives under `src/main/java/br/com/tecsus/sigaubs`, organized by responsibility: `controllers`, `services`, `repositories`, `entities`, `dtos`, `enums`, `security`, `jobs`, and `utils`. JTE templates are in `src/main/jte`, grouped by feature modules such as `patientManagement`, `queueManagement`, and `medicalSlotManagement`; shared layout fragments are in `src/main/jte/fragments`. Static CSS, JS, and images are in `src/main/resources/static`. Application profiles, SQL schema, and seed data are in `src/main/resources`.

## Build, Test, and Development Commands

- `./mvnw spring-boot:run` starts the Spring Boot app locally.
- `./mvnw clean package` builds the application JAR.
- `./mvnw test` runs the Maven test suite.
- `npm run watch:postcss` rebuilds Tailwind CSS during development.
- `npm run build:postcss` creates the minified CSS output in `target/classes/static/css/style.build.tailwind.css`.

During UI work, run the Spring Boot app and `npm run watch:postcss` together so template and CSS changes are visible.

## Coding Style & Naming Conventions

Follow the existing Spring MVC layered style: controllers handle web flow, services hold business logic and transactions, repositories isolate persistence, and entities model JPA state. Use Java package names in lowercase and class names by role, for example `PatientController`, `AppointmentService`, and `MedicalSlotRepository`. Keep feature templates under their matching JTE module and name reusable partials with clear suffixes such as `_datatable.jte`, `_form.jte`, or `_info.jte`. Code, comments, and commits are primarily in Brazilian Portuguese; keep that convention unless integrating with external APIs.

## Testing Guidelines

Place Java tests under `src/test/java` using JUnit through `spring-boot-starter-test`; mirror the main package structure and name classes `*Test` or `*Tests`. There is no explicit coverage threshold configured. Prefer focused service and repository tests for business rules, especially queue ordering, appointment status changes, and contemplation scheduling. Use `./mvnw test -Dtest=ClassName` for a single class.

## Commit & Pull Request Guidelines

Recent history uses concise Portuguese conventional-style messages such as `fix: corrigir ...` and `refactor: renomear ...`. Keep commits scoped and use prefixes like `fix:`, `feat:`, `refactor:`, or `chore:`. Pull requests should describe the change, list test commands run, link related issues when available, and include screenshots or short recordings for visible JTE/Tailwind UI changes.

## Security & Configuration Tips

Do not commit real credentials or certificates beyond existing development fixtures. Prefer profile-specific properties in `src/main/resources/application-*.properties`, and keep local database secrets in environment variables when possible.
