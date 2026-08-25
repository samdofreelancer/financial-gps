# Research: Financial Domain Engine

> Phase 0 output for the Financial Domain Engine plan. Every decision is bounded by the
> normative contract (`calculation-rules.md`, `status-rules.md`, `reference-cases.md`). No new
> business rules and no UI/API/database/auth/AI are introduced here; this is the persistent
> calculation core that later features embed.

## Decisions

### Module shape and language

- **Decision**: A framework-free, pure Java 21 module `com.financialgps.domain` with no Spring,
  web, persistence, or AI dependencies in this layer. It lives at the repo plan location
  `backend/src/main/java/com/financialgps/domain/` matching the `004-financial-gps` structure,
  with tests in `backend/src/test/java/com/financialgps/domain/`.
- **Rationale**: determinism and testability (constitution Principle II, VIII) are easiest when
  the engine is a pure function library with no infrastructure. It is consumed later by
  `004/005/006/008/009` layers that provide adapters, never the reverse.
- **Alternatives considered**: embedding rules in Spring services (couples rules to the app),
  a separate multi-module Maven artifact (premature for a modular monolith), and treating the
  oracle markdown as runtime config (kept as documentation; the constants are expressed in code
  policy objects).

## Money and precision

- **Decision**: `Money` wraps `BigDecimal` with an ISO 4217 currency string, persisted value
  scale 2 for VND, explicit `RoundingMode`. All arithmetic lives on the value object or in
  calculators; no raw `BigDecimal` leaks into higher layers.
- **Rationale**: exact decimal arithmetic; enforce rounding at closed boundaries per
  `calculation-rules.md` §1.
- **Alternatives**: long integer minor units (fixed-currency assumption only) and
  `double` (non-deterministic) both rejected.

## Policy model

- **Decision**: an immutable `FinancialPolicy` aggregates `RoundingPolicy`,
  `DebtPolicy`, `AllocationPolicy`, `StatusPolicy`; every calculator takes a policy so rounding,
  rates, debt mode, and status tolerance are explicit parameters, not magic numbers.
- **Rationale**: `status-rules.md` requires every threshold to be a documented, changeable policy
  rather than a hardcoded value; the same function must be reusable under different policies.
- **Alternatives**: baking thresholds into each calculator (rejected: not testable against the
  oracle, not policy-configurable).

## Determinism boundary

- **Decision**: `calculate(...)` takes all inputs + assumptions + `asOfDate` + policy and returns
  a value; it never reads clocks, DB, HTTP, randomness, or AI. The system clock only ever seeds
  `asOfDate` at a boundary owned by a feature adapter, never inside the engine.
- **Rationale**: constitution Principle II; `DM-001/002/003` reference cases depend on it.
- **Alternatives**: injecting a clock into the engine (outside the pure contract) and caching
  projections (rejected — recompute).

## Timeline and allocation

- **Decision**: the projection engine slices time into monthly periods from `asOfDate` onward,
  applies effective-dated timeline changes at their boundaries, routes Available Capacity through
  the ordered allocation rules, and detects satisfaction/completion when a destination's
  condition is met. Goal dependencies are validated as a DAG (cycle detection incl. self-loop).
- **Rationale**: `008-*` spec and `calculation-rules.md` §6–§10; `reference-cases` `AL-004`,
  `AL-006` need cycle/self-dependency rejection.
- **Alternatives**: weekly/daily cadence (rejected — `frequency` is locked to `MONTHLY`),
  greedy recompute without reuse (acceptable but tests keep it deterministic).

## Status evaluation

- **Decision**: a `StatusEvaluator` implements `status-rules.md` — order of checks
  `COMPLETED → BLOCKED → ON_TRACK → AT_RISK → OFF_TRACK` against a projected result, using a
  changeable `StatusPolicy` for `latenessTolerance`.
- **Rationale**: `status-rules.md`; replacement of the arbitrary 3-month rule with a
  configurable tolerance.
- **Alternatives**: scoring/health index rejected (constitution §III, §VII).

## TDD flow

- **Decision**: each reference case (`reference-cases.md`) is the seed of an automated unit test.
  Development order is `reference case → red test → minimal domain code → pass → refactor`.
  A traceability table maps `FR → calculator → reference case → task`.
- **Rationale**: `reference-cases` is the "normative financial oracle"; constitution §VIII demands
  independent domain tests.
- **Alternatives**: generate services then tests (rejected — risks inventing surplus fields).

## No integration

- **Decision**: no REST, no JPA/repository, no auth, no AI, no DB migration. This module only
  defines pure domain types and calculators. Foreign-feature (001/002/003) inputs are supplied as
  inputs to `calculate`, not persisted here.
- **Rationale**: keeps the engine dependency-free per the plan input.
- **Alternatives**: including persistence (rejected — that is adapter work in later features).