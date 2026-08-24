# Research: Financial GPS

## Decisions

### Deterministic calculation architecture

- **Decision**: Use a modular Spring Boot monolith with framework-free Java domain policies and
  immutable value objects for all financial calculation.
- **Rationale**: The same inputs must yield the same GPS result. Domain policies accept an explicit
  as-of date and configuration; they do not access the database, HTTP, system clock, randomness,
  or AI.
- **Alternatives considered**: Microservices and event sourcing are deferred because they add
  coordination and audit complexity without helping the initial single-profile product.

### Precise financial arithmetic

- **Decision**: Use Java `BigDecimal` and PostgreSQL `numeric(19,2)` for VND amounts; define a
  central scale and `RoundingMode` at domain-service boundaries. Use `numeric(9,6)` for rates and
  `numeric(7,4)` for ratios.
- **Rationale**: Decimal arithmetic preserves exact financial values; float/double and JavaScript
  `number` do not. PostgreSQL `money` is rejected because it is locale-sensitive.
- **Alternatives considered**: Integer minor units are viable only for a permanently fixed currency
  scale. `numeric` maps naturally to `BigDecimal` and allows future currency evolution.

### Data integrity and projections

- **Decision**: Persist user inputs, assumptions, UUID identity, and audit timestamps. Recompute
  GPS and roadmap projections; do not persist them as mutable source-of-truth records.
- **Rationale**: This keeps a result traceable to its input snapshot and prevents stale
  projections. Flyway owns non-null, check, unique, and foreign-key constraints; cross-record
  invariants stay in transactional application/domain validation.
- **Alternatives considered**: Database triggers for every business rule are rejected initially
  because they obscure domain logic and make explanations harder to test.

### Scenario isolation

- **Decision**: Evaluate a scenario as an in-memory override DTO applied to a read-only baseline,
  then pass it through the same calculation engine.
- **Rationale**: It guarantees a scenario cannot alter actual profile, debts, goals, or route.
- **Alternatives considered**: Temporary tables and cloned records are rejected; pooled database
  sessions and cleanup make them riskier than an in-memory projection.

### API, frontend state, and errors

- **Decision**: REST uses validated DTOs and RFC 9457 `ProblemDetail` errors. The backend returns
  full explanation DTOs. React uses controlled string-based money fields, `Intl.NumberFormat` only
  for display, TanStack Query for server state, and local state for drafts/modals.
- **Rationale**: The client never recalculates financial rules or performs risky optimistic updates
  on balances and ETAs; server responses remain the source of truth.
- **Alternatives considered**: Redux is unnecessary for the initial server-centric UI; optimistic
  financial mutations are deferred.

### Test strategy

- **Decision**: Test domain policies with fast JUnit cases and deterministic reference tables;
  test migration/repository/transaction behavior using real PostgreSQL Testcontainers; test REST
  contracts with MockMvc; test UI behavior with Vitest/React Testing Library and primary journeys
  with Playwright.
- **Rationale**: Domain correctness is isolated while financial precision, constraints, and
  scenario non-mutation are verified against the production database type.
- **Alternatives considered**: H2-only integration tests are rejected because decimal behavior and
  PostgreSQL constraints must be verified on PostgreSQL.

## Authoritative References

- [Spring unit testing](https://docs.spring.io/spring-framework/reference/testing/unit.html)
- [Spring transaction annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [Java BigDecimal](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/math/BigDecimal.html)
- [PostgreSQL numeric types](https://www.postgresql.org/docs/current/datatype-numeric.html)
- [PostgreSQL transaction isolation](https://www.postgresql.org/docs/current/transaction-iso.html)
- [React controlled inputs](https://react.dev/reference/react-dom/components/input)
- [TanStack Query invalidation](https://tanstack.com/query/latest/docs/framework/react/guides/query-invalidation)
