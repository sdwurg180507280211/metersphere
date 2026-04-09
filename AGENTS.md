# Repository Guidelines

## Project Structure & Module Organization
- `framework/`: shared Spring Boot starters, Dubbo integrations, and cross-cutting utilities used by every service module.
- `api-test/`, `performance-test/`, `test-track/`: core business services; each module is a standalone Maven project with its own `application.yml` and resources.
- `system-setting/`, `project-management/`, `workflow-service/`, `analytics-stat/`, `report-stat/`: ancillary services for settings, orchestration, reporting, and analytics pipelines.
- `workstation/` and `workstation/ui`: web front-end bundles and static assets; run Node/NPM tasks here.
- `docs/`: user & operator manuals published to the knowledge base. Keep marketing/WeChat articles under `docs/01-快速开始/`.

## Build, Test, and Development Commands
- `./mvnw clean install -DskipTests`: compile all Java modules and assemble artifacts; use before submitting large changes.
- `./mvnw -pl workstation -am test`: run unit and integration tests for a specific module and its dependencies.
- `npm install && npm run build` (inside `workstation/ui`): install front-end dependencies and produce production bundles.
- `./mvnw spring-boot:run -pl api-test`: start a single service locally with Spring profiles inherited from `framework`.

## Coding Style & Naming Conventions
- Java 17, Spring Boot 3.2.12 baseline. Use 4-space indentation, camelCase for variables/methods, and UPPER_SNAKE_CASE for constants.
- REST endpoints follow kebab-case paths (e.g., `/api/test-plan`), DTO classes end with `Request`/`Response`, and repositories use the `*Repository` suffix.
- Lombok is enabled—prefer annotations over manual getters/setters, but keep constructors explicit for entities.
- Front-end uses ESLint defaults; align file names with component purpose (`TaskList.vue`, `usePlanStore.ts`).

## Testing Guidelines
- Default framework is JUnit 5 with Spring Boot Test; use `@DataJpaTest`/`@WebMvcTest` for focused suites.
- Place unit tests under the mirroring `src/test/java` package; name classes `*Test` or `*IT` for integration tests.
- Use Testcontainers/Kafka embedded brokers when touching messaging modules; clean up resources to keep CI stable.
- Aim for ≥80% line coverage in touched packages; verify locally with `./mvnw -pl module jacoco:report` before opening a PR.

## Commit & Pull Request Guidelines
- Write commits in imperative mood (`Add plan caching`, `Fix report import`) and keep scope small. Reference issues using `#1234` when applicable.
- Pull requests must include: summary of changes, affected modules, manual/automated test evidence, and screenshots for UI updates.
- Run the full Maven build (and relevant NPM builds) before requesting review. Tag reviewers from OWNERS for shared modules.

## Security & Configuration Tips
- Secrets belong in environment variables or `application-*.yml` templates stored outside Git. Never commit actual keys.
- Follow `SECURITY.md` for disclosure/reporting; if a change touches auth, add steps for regression testing SSO, LDAP, and API tokens.
- Keep `mcp.json` and `docs` artifacts in sync with released features so downstream teams have accurate automation hooks.
