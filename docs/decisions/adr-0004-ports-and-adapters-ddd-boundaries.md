# ADR 0004: Ports-and-adapters DDD boundaries in the backend

- Status: Accepted
- Date: 2026-09-24

## Context

The backend grew feature by feature (`001`–`009`) as a single Maven module. Use cases
accumulated framework coupling: application services imported JPA entities and Spring
Data repositories directly, with `@Service`/`@Transactional` annotations inside the
would-be domain logic. That made the financial rules untestable without Spring, and any
new feature risked spreading persistence details further inward. See
`specs/010-ddd-refactor/plan.md`.

## Decision

Restructure `backend/` into package-level ports-and-adapters boundaries with dependencies
pointing inward only:

```text
REST / security adapter  ->  application use case  ->  domain model / ports
                                       ^
                                       |  infrastructure adapters implement the ports
```

1. `domain/` holds the pure financial model and engine — no framework, no identity
   (guarded by `DomainBoundaryGuardTest`).
2. `application/{account,profile,debt,goal}/{port.in,port.out,usecase,model}` is
   Java/Spring-free: one interface per use case in `port.in`, persistence/hashing/clock
   ports in `port.out`, immutable records across the boundary.
3. `infrastructure/` owns JPA entities, Spring Data repositories, BCrypt, and the
   `BusinessDate` clock adapter; transactions apply at the input-port boundary via
   `infrastructure.configuration.UseCaseTransactions`.
4. `api/` controllers do DTO binding, actor resolution, and response mapping only —
   no financial rules, no clock.
5. `platform/security/` holds the filter chain and authenticated-principal adapter.
6. `ArchitectureGuardTest` enforces all of the above for `src/main/java`.

## Rationale

- The financial engine becomes unit-testable as plain Java (no Spring context).
- New features (`005` roadmap, `006` scenarios) add ports + use cases without touching
  REST or schema conventions.
- Guards make the boundary self-enforcing: violations fail the build, not review.

## Consequences

- Positive: clear home for every new line of backend code; `specs/010` marked Implemented.
- Negative: more interfaces/boilerplate per feature (one in-port per use case).
- Follow-up constraint: no Maven multi-module split (deliberate non-goal); package
  discipline plus guard tests carry the weight instead.

## Alternatives considered

- **Maven multi-module split.** Rejected: heavier build/CI for a single deployable;
  package boundaries give the same dependency direction with less tooling.
- **Leave as-is (anemic services + JPA in use cases).** Rejected: each feature
  increased the cost of isolating the engine, exactly when scenario planning needs it most.
- **Full CQRS/event sourcing.** Rejected: no audit/event requirement justifies the
  operational complexity on a single Postgres.

## Related artifacts

- `specs/010-ddd-refactor/plan.md`
- `docs/architecture/c4-container.md` (Level 3)
- `ArchitectureGuardTest`, `DomainBoundaryGuardTest`
