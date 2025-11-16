# Repository Guidelines

## Project Structure & Module Organization
The Spring Boot API lives in `src/main/java/fr/siovision/voyages`, split into `config` (security/beans), `web` (controllers + REST DTOs), `application` (services, mappers, aspects), `domain` (aggregates, events, enums), and `infrastructure` (repositories, storage adapters). Client-facing templates/static assets sit in `src/main/resources/templates` and `src/main/resources/static`, while Flyway SQL migrations execute from `src/main/resources/db/migration`. Tests should mirror the package tree inside `src/test/java/fr/siovision/voyages`, keeping fixtures or JSON payloads in `src/test/resources`. Build outputs land in `build/` and deployment jars in `build/libs`.

## Build, Test & Development Commands
- `./gradlew bootRun --args='--spring.profiles.active=dev'` – run the API locally with the dev profile and RequestAuditFilter active.
- `docker-compose up -d` – start the Postgres + MinIO stack defined in `docker-compose.yml`; drop it with `docker-compose down -v`.
- `./gradlew test` – execute the full JUnit 5 suite; review reports in `build/reports/tests`.
- `./gradlew bootJar` – create an executable jar for packaging or deployment.
- `./gradlew clean build` – CI-equivalent compile, test, and verification (fails if lint/tests break).

## Coding Style & Naming Conventions
Target Java 21 (toolchain configured in `build.gradle`) and Spring Boot 3.5. Use 4-space indentation, braces on the same line, and `final` where applicable. Annotate components by responsibility (`@RestController`, `@Service`, `@Repository`) and keep DTOs suffixed with `Request`/`Response` in the `web` layer. Prefer MapStruct interfaces for mapping and Lombok for boilerplate but keep constructors explicit on entities. When adding Flyway scripts, follow `VYYYYMMDD__description.sql`. Qodana checks run in CI; avoid suppressing inspections without justification.

## Testing Guidelines
Write slice or integration tests with Spring Boot Test + AssertJ, naming files `*Test` (unit/slice) or `*IT` (integration) so Gradle picks them up. Use `MockMvc` for controller tests, `@DataJpaTest` for repository coverage, and seed deterministic data from `src/test/resources` via `@Sql` or builders. Every change touching business rules, security, or migrations should raise coverage and leave `./gradlew test` clean.

## Commit & Pull Request Guidelines
Branches follow `feat/<issue-id>-topic` or `fix/...`. Commits adhere to Conventional Commits (`feat(auth): enforce MFA (#52)`) to keep changelogs readable. Pull requests must describe intent, link the tracked issue (`Closes #52`), list manual verification (curl, Swagger, Postman), and attach payload diffs or screenshots when API contracts change. Ensure Gradle tests and Qodana linting pass before requesting review; PRs are merged via squash into `main`.

## Security & Configuration Tips
Load credentials through `.env` or your IDE EnvFile, setting `SPRING_DATASOURCE_*`, `S3_*`, `WEBAUTHN_*`, `JWT_*`, and `SPRING_PROFILES_ACTIVE`. Keep secrets out of Git, prefer Docker secrets or CI vaults, and recycle `docker-compose up -d` only when rotating dependencies (Postgres, MinIO) or refreshing challenge secrets.
