# Implementation Plan: Financial GPS

**Branch**: `004-financial-gps` | **Date**: 2026-08-24 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-financial-gps/spec.md`

**Note**: This template is filled in by the `$speckit-plan` command; its definition describes the execution workflow.

## Summary

Build an explainable Financial GPS that turns financial profile, debt, and goal data into a
deterministic current position, distance, progress, ETA, status, blockers, and next action. A
framework-free Java calculation core is called by Spring Boot services and exposed through REST;
React renders returned explanations without reimplementing financial rules. PostgreSQL persists
user-entered facts and assumptions; projections are recomputed from an explicit as-of date.

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Java 21 LTS; TypeScript 5.x with strict mode

**Primary Dependencies**: Spring Boot (MVC, validation, data access), React 19, TanStack Query v5

**Storage**: PostgreSQL 16+; Flyway migrations; `numeric` monetary columns mapped to `BigDecimal`

**Testing**: JUnit 5, Spring Boot test support, Testcontainers PostgreSQL, Vitest, React Testing
Library, Playwright

**Target Platform**: Modern desktop and mobile browsers; Linux-hosted Spring Boot service

**Project Type**: Web application with React frontend and REST backend

**Performance Goals**: 95% of GPS reads complete in under 1 second for a profile with up to 50
debts and 25 active goals; all 100 reference calculation cases are deterministic.

**Constraints**: Monetary arithmetic uses `BigDecimal` and `numeric`, never float/double or
JavaScript `number`; calculations have no database, HTTP, clock, randomness, or AI dependency;
scenario evaluation makes no durable change to real financial data.

**Scale/Scope**: First release supports a single owner profile per authenticated account, one
selected destination per GPS view, monthly projections, and VND as the initial currency. It excludes
transaction feeds, market returns, inflation forecasts, collaboration, and investment advice.

## Constitution Check

*GATE: Passed before Phase 0 research and re-checked after Phase 1 design.*

| Gate | Design response | Status |
|------|-----------------|--------|
| Financial truth and conservative projections | Persist supplied facts and explicit assumptions; label actual, assumed, and calculated values in every GPS response. | Pass |
| Determinism and testability | Keep calculation policies as pure Java domain services; use explicit as-of date and `BigDecimal` rounding. | Pass |
| Explainability | Return input snapshot, status-rule evaluation, capacity comparison, blockers, and next actions with each GPS result. | Pass |
| Goal-driven roadmap | GPS accepts a selected goal or stage destination with explicit conditions. | Pass |
| Scenario isolation | Apply scenario overrides in memory to read-only baseline data. | Pass |
| Human control, privacy, simplicity | Use guidance only, minimize data, and begin with a modular monolith. | Pass |

## Project Structure

### Documentation (this feature)

```text
specs/004-financial-gps/
├── plan.md              # This file ($speckit-plan command output)
├── research.md          # Phase 0 output ($speckit-plan command)
├── data-model.md        # Phase 1 output ($speckit-plan command)
├── quickstart.md        # Phase 1 output ($speckit-plan command)
├── contracts/           # Phase 1 output ($speckit-plan command)
└── tasks.md             # Phase 2 output ($speckit-tasks command - NOT created by $speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
backend/
├── src/main/java/com/financialgps/
│   ├── domain/                 # Pure money, profile, debt, goal, GPS policies and values
│   ├── application/            # Transactional commands, queries, and ports
│   ├── infrastructure/          # JPA repositories, Flyway, adapters
│   └── api/                    # REST DTOs, validation, ProblemDetail advice
├── src/main/resources/db/migration/
└── src/test/java/com/financialgps/
    ├── domain/
    ├── api/
    └── integration/

frontend/
├── src/
│   ├── features/               # Profile, debt, goal, GPS, roadmap, scenario screens
│   ├── components/             # MoneyInput, MoneyDisplay, status/explanation components
│   ├── api/                    # Typed REST client and query hooks
│   └── test/
└── e2e/
```

**Structure Decision**: Separate React and Spring Boot applications with a framework-free domain
module. JPA and REST remain adapters; a multi-service or event-sourced design is premature.

## Complexity Tracking

No constitution violations or complexity exceptions are required.
