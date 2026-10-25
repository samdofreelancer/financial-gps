# Architecture Overview

## Purpose

Financial GPS is a personal finance platform centered on a domain-first financial engine. The solution helps users manage a financial profile, debt obligations, goals, scenario planning, and roadmap planning while preserving a clear separation between business rules and technical implementation details.

## System boundary

The application is organized around three primary layers:

1. Frontend SPA
   - user-facing experience for profile, debt, goals, roadmap, auth, and account management
   - communicates with backend through HTTP APIs
   - handles client-side state, routing, and form validation

2. Backend API
   - exposes authenticated REST endpoints
   - enforces ownership, security, and session policies
   - orchestrates use cases and business logic

3. Data and infrastructure layer
   - PostgreSQL persistence for accounts, sessions, and app data
   - Docker-based local orchestration
   - Kubernetes manifests for production-style deployment

## Core principles

- Domain-first design: business rules stay inside the financial domain layer
- Security by default: session-based auth, CSRF protection, ownership enforcement
- DDD boundary enforcement: infrastructure and transport concerns are outside the domain model
- Deterministic financial calculations: export and state transitions are predictable and testable
- Testability: unit tests, integration tests, and end-to-end flows validate behavior

## Runtime topology

```text
Browser / User
    |
    v
Frontend (Vite + Vue 3 / Pinia / TypeScript)
    |
    | REST API
    v
Backend (Spring Boot 3.2 / Java 21)
    |
    | JDBC / JPA
    v
PostgreSQL 16
```

## Key domain capabilities

- financial profile management
- debt management and repayment modeling
- goal planning and target tracking
- financial GPS planning and projections
- scenario planning and comparison
- auth and account ownership
- secure export and account deletion

## Architectural constraints

- domain packages must not depend on framework, database, security, or HTTP concerns
- application use cases must not import infrastructure or transport adapters
- ownership checks must prevent cross-user access
- session and auth behavior must be consistent and secure
- financial rules are validated through domain tests and boundary guard tests

## Backend package map

Each feature is one vertical slice with the same lanes; dependencies point inward only.

| Lane | Package | Holds |
|---|---|---|
| domain | `domain/{profile,debt,goal,gps,roadmap}` | pure rules, value objects, calculators — no Spring, JPA, auth, JDBC or clock |
| application | `application/{profile,debt,goal,gps,roadmap}/{model,port/in,port/out,usecase}` | use cases, input/output ports, read models |
| infrastructure | `infrastructure/{persistence,time,configuration}` | adapters (JPA stores), `*UseCaseConfiguration` wiring, `UseCaseTransactions` (transaction boundary) |
| api | `api/{auth,account,profile,debt,goal,gps,roadmap,common}` | controllers, DTOs, `ProblemDetailAdvice` |
| platform | `platform/security` | session, CSRF, ownership plumbing |

Feature 005 adds one slice per lane: `domain/roadmap` (`RoadmapGenerator`, `RoadmapStageProjector`,
`RoadmapEtaHypothesis`, `RoadmapStageDetailFactory`), `application/roadmap` (`GetRoadmap` plus the
four output ports), `infrastructure/persistence/{goal,debt,profile}/Roadmap*Adapter` with
`RoadmapBusinessDateAdapter` (time) and `RoadmapUseCaseConfiguration` (read-only transaction), and
`api/roadmap/RoadmapController`. The frontend mirror is `frontend/src/api/roadmap.ts`,
`stores/roadmapStore.ts`, `components/roadmap/` and `views/RoadmapView.vue`.

## Related documents

- [system-context.md](system-context.md)
- [../runbooks/README.md](../runbooks/README.md)
- [../troubleshooting/README.md](../troubleshooting/README.md)
