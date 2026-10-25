# Project Handbook

## 1. Project summary

Financial GPS is a personal financial planning platform built to help individuals understand, plan, and improve their financial future. The platform blends personal financial profile tracking, debt management, goal setting, financial forecasting, and scenario planning in one system.

The repository also demonstrates a modern, domain-first backend architecture backed by Java 21 and Spring Boot 3.2, with secure authentication and authorization patterns.

## 2. Product vision

The core product promise is simple:

- help users understand their current financial reality
- help them define meaningful future goals
- project outcomes based on present decisions
- recommend and track a realistic path forward

## 3. Technical stack

### Backend

- Java 21
- Spring Boot 3.2
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Session JDBC
- Actuator
- Testcontainers

### Frontend

- Vite
- TypeScript
- web SPA framework used by the project
- browser-based auth and UI flows

### Infrastructure and tooling

- Docker Compose
- PostgreSQL container
- Playwright E2E runner
- Kubernetes manifests for production-style deployment
- CI workflow definitions in .github/workflows

## 4. Architecture summary

The system follows a layered and domain-first model:

- domain layer: pure business rules and financial logic
- application layer: use cases and ports
- infrastructure layer: JPA, persistence, security adapters, system wiring
- API layer: HTTP adapters and response mapping
- frontend: user interface and interaction layer

This protects the domain from framework and infrastructure pollution.

## 5. Current implementation status

The backend has implemented the authentication and ownership model with strong security characteristics:

- email/password account registration
- BCrypt hashing
- session-based auth with server-side storage
- CSRF token pattern
- ownership enforcement for user-scoped resources
- export and deletion flows with deterministic behavior
- RFC 7807 problem responses

The project is still evolving through additional financial planning domains.

## 6. Key principles

- domain rules before infrastructure concerns
- clear ownership boundaries
- secure auth by default
- explicit operational documentation
- deterministic behavior for financial outcomes and exports
- test-driven confidence for behavior changes

## 7. Project workflow

A healthy workflow for this project should be:

1. understand the domain feature under development
2. review the relevant spec and architecture docs
3. keep code aligned with domain boundaries
4. validate with targeted tests
5. update documentation if setup, flows, or decisions change

## 8. Operational notes

- DB_PASSWORD must be set before running Compose or Spring Boot locally
- local Docker Compose is the primary local development entry point
- Playwright E2E runs require the stack to be running
- service ports should be tracked and documented to avoid conflicts

## 9. Related docs

- [README.md](../README.md)
- [architecture/overview.md](architecture/overview.md)
- [architecture/business-domain.md](architecture/business-domain.md)
- [architecture/feature-map.md](architecture/feature-map.md)
- [development/local-setup.md](development/local-setup.md)
- [operations/README.md](operations/README.md)
- [operations/ports-and-services.md](operations/ports-and-services.md)
- [runbooks/local-development.md](runbooks/local-development.md)
- [runbooks/docker-compose.md](runbooks/docker-compose.md)
- [troubleshooting/README.md](troubleshooting/README.md)

## 10. Ownership of knowledge

This project should treat documentation as a durable asset, not as a byproduct of conversation. Code changes, setup changes, and architectural decisions should be reflected in this documentation set.
