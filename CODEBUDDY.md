# CODEBUDDY.md

This file provides guidance to CodeBuddy Code when working with code in this repository.

## Project Overview

MeterSphere is a one-stop open-source continuous testing platform (v2.10-lts) covering test tracking, API testing, UI testing, and performance testing. It's built as a **microservices** architecture with **micro-frontend** (qiankun).

**Tech stack:** Java 17, Spring Boot 3.2.12, Spring Cloud 2023.0.1, MyBatis 3.0.3, Vue 2.7.3, Element UI 2.15.13, qiankun 2.9.3, MySQL 8.0, Redis 7.2, Kafka, MinIO, JMeter 5.5

## Build Commands

### Install base POM and SDK first (required before building any module)

```bash
./mvnw install -N
./mvnw clean install -pl framework,framework/sdk-parent,framework/sdk-parent/domain,framework/sdk-parent/sdk,framework/sdk-parent/xpack-interface,framework/sdk-parent/jmeter
```

### Full build (skip frontend modules)

```bash
./mvnw clean package -DskipTests -DskipAntRunForJenkins -pl "!framework/sdk-parent/frontend,!api-test/frontend,!performance-test/frontend,!project-management/frontend,!report-stat/frontend,!system-setting/frontend,!test-track/frontend,!workstation/frontend"
```

### Full build (including frontends, built via frontend-maven-plugin)

```bash
./mvnw clean package -DskipTests
```

### Build a single backend module

```bash
./mvnw clean package -DskipTests -pl api-test/backend -am
```

### Run a single backend service locally

Each service is a Spring Boot application. Run the main class in your IDE or:

```bash
./mvnw spring-boot:run -pl api-test/backend
```

Requires dev infrastructure (MySQL, Redis, Kafka, MinIO) running via:

```bash
docker-compose -f docker-compose-dev.yml up -d
# or
./scripts/dev-env.sh start
```

### Frontend development

Each business module has its own Vue app under `<module>/frontend/`. The shared frontend SDK at `framework/sdk-parent/frontend` is the qiankun main app (dev port 3000).

```bash
# Install shared SDK dependencies first
cd framework/sdk-parent/frontend && npm install

# Then install and run a business module frontend
cd api-test/frontend && npm install && npm run serve
```

### Tests

There are currently no unit test sources checked into the repository. Test dependencies exist in POMs (JUnit, Mockito, spring-boot-starter-test) but no `src/test/java` directories are present. Maven tests are skipped in CI via `-DskipTests`.

## Architecture

### Microservices Topology

```
Browser → Gateway (:8000) → Eureka (:8761, service registry)
                              ├── system-setting (:8001)
                              ├── project-management (:8002)
                              ├── performance-test (:8003)
                              ├── api-test (:8004)
                              ├── test-track (:8005)
                              ├── report-stat (:8006)
                              └── workstation (:8007)

Infrastructure: MySQL (:3306), Redis (:6379), Kafka (:9092), MinIO (:9000)
```

All external requests enter through the Spring Cloud Gateway (WebFlux-based), which routes to microservices via Eureka discovery. Gateway also serves frontend static assets and aggregates Swagger/OpenAPI docs.

### Module Structure Pattern

Every business module follows this layout:

```
<module>/
├── backend/
│   ├── src/main/java/io/metersphere/
│   │   ├── controller/        # REST endpoints
│   │   ├── service/           # Business logic
│   │   ├── base/mapper/       # MyBatis data access
│   │   └── <Module>Application.java
│   ├── src/main/resources/
│   │   ├── application.properties  # Spring Boot config (NOT .yml)
│   │   └── db/migration/           # Flyway migrations
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── api/               # API client definitions
│   │   ├── business/          # Business components
│   │   ├── router/            # Vue Router
│   │   ├── store/             # Pinia state
│   │   └── i18n/              # Internationalization
│   └── vue.config.js
└── pom.xml
```

### Framework Layer (`framework/`)

- **gateway/** — Spring Cloud Gateway (WebFlux). Entry point for all requests. Routes to services, serves static assets, aggregates API docs.
- **eureka/** — Service registry. All microservices register here.
- **sdk-parent/sdk/** — Shared utilities: autoconfigure, security, logging, exception handling, file management, i18n, scheduling, etc.
- **sdk-parent/domain/** — Shared data models/entity classes used across all business modules.
- **sdk-parent/jmeter/** — JMeter 5.5 engine integration for test execution.
- **sdk-parent/frontend/** — Shared Vue components, published as `metersphere-frontend` npm package. Contains qiankun bootstrap code (main app, dev port 3000). All business module frontends reference it via `"metersphere-frontend": "file:../../framework/sdk-parent/frontend"`.
- **sdk-parent/xpack-interface/** — SPI-style interfaces for enterprise extensions. The xpack JAR in `xpack-lib/` implements these at Docker build time.

### Micro-Frontend Architecture

The shared frontend SDK is the qiankun **main application**. Each business module frontend is a qiankun **sub-application**, built as a UMD library. In production, static assets are served through the gateway at `/<service-name>/` paths.

### Key Patterns

- **Layered architecture**: Controller → Service → Mapper (MyBatis)
- **DTO pattern**: Data transfer objects in `dto/` packages, separate from domain entities
- **Flyway migrations**: Each service tracks its own schema version (e.g., `api_version`, `track_version`). Naming: `V{version}__{description}.sql`
- **Configuration**: Services use `application.properties` (not YAML). External config loaded from `/opt/metersphere/conf/metersphere.properties`
- **Version management**: All modules share version via Maven `${revision}` property (currently `2.10`)
- **XPack injection**: Enterprise features are injected at Docker build time from `xpack-lib/`

## Naming Conventions

- Java packages: `io.metersphere.<module>` (e.g., `io.metersphere.controller`, `io.metersphere.service`)
- Frontend files: `kebab-case` for filenames, `PascalCase` for Vue components, `camelCase` for JS variables
- Flyway migrations: `V{version}__{description}.sql`

## Dev Environment

Infrastructure services are managed via Docker Compose:

```bash
docker-compose -f docker-compose-dev.yml up -d
# or use the management script:
./scripts/dev-env.sh start
```

| Service | Port | Credentials |
|---------|------|-------------|
| MySQL | 3306 | metersphere / Password123@mysql (db: metersphere_test) |
| Redis | 6379 | Password123@redis |
| Kafka | 9092 | No auth |
| MinIO | 9000/9001 | minioadmin / minioadmin123 |

## Important Notes

- **SDK must be built first** — always install `framework/sdk-parent` before building any business module
- **No test sources** — the repo has no `src/test/java` directories; test dependencies exist but are unused
- **Spring Boot version must not be upgraded** — comment in root POM warns of compatibility issues
- **commons-io must not be upgraded** — compatibility issues noted in root POM
- **Chinese comments preferred** — per project conventions, add detailed Chinese technical comments
