# Implementation Plan: Financial Domain Engine

**Branch**: `financial-domain` | **Date**: 2026-08-25 | **Spec**: [spec](spec.md) · [calculation-rules](calculation-rules.md) · [status-rules](status-rules.md) · [reference-cases](reference-cases.md)

**Input**: Normative domain contract (`specs/financial-domain/*`); `$speckit-plan` command output.

**Note**: This is the shared engine that `004/005/006/008/009` depend on. It is not a numbered
feature that ships UI/API; it is the deterministic calculation core consumed by those features.

## Summary

Build a framework-free, deterministic Financial Domain Engine as a pure Java module
(`com.financialgps.domain`). It implements the exact rules in `calculation-rules.md`, evaluates
the five GPS statuses per `status-rules.md`, and is verified against every row of
`reference-cases.md`. It exposes one pure entry point `calculate(inputs, assumptions, asOfDate,
policy) -> FinancialResult`, with small calculators (cash flow, debt, goal, allocation,
timeline, projection, dependency, status). No DB/HTTP/clock/randomness/AI. TDD, reference cases
as the normative oracle.

## Technical Context

**Language/Version**: Java 21 LTS

**Primary Dependencies**: None beyond the JDK. The engine uses `java.math.BigDecimal` for all
monetary math; no Spring/web/persistence/AI libraries inside the engine module.

**Storage**: N/A — the engine is pure in-process. Later features persist inputs and call this
engine; this module does not own a store.

**Testing**: JUnit 5, table-driven; each row of `reference-cases.md` is an automated domain test.

**Target Platform**: JVM 21; consumed by the Spring Boot backend of later features.

**Project Type**: Pure Java domain/engine module (library-style, not a web app).

**Performance Goals**: Deterministic; a GPS read for ≤50 debts and ≤25 goals completes in <1s
(engine-only less); reference table runs in <5s.

**Constraints**: No database, HTTP, system clock, randomness, or AI within the module; all math
via `BigDecimal` + explicit rounding; `MONTHLY` frequency only; canonical terminology only.

**Scale/Scope**: Single-engine v1; VND currency; monthly periods. Excludes UI, API, storage,
auth, AI, inflation, market returns, transaction feeds, multi-currency.

## Constitution Check

*GATE: Passed before Phase 0 research and re-checked after Phase 1 design.*

| Gate | Design response | Status |
|------|-----------------|--------|
| II. Deterministic calculation | Pure entry point with explicit `asOfDate` + `FinancialPolicy`; no clock/DB/HTTP/random/AI. `DM-*` verify identical results. | Pass |
| III. Explainable GPS | Every result carries position, destination, eta (or unavailable+reason), status, blockers, nextActions, and explanation/provenance (actual/assumed/calculated). | Pass |
| V. Conservative planning | Assumptions labelled `USER_SUPPLIED`/`SYSTEM_DEFAULT`; `CEILING` for required capacity; negative Net Cash Flow reported, not concealed. | Pass |
| VI. Scenario isolation | ProjectionFinancialState is read-only; never mutates actual inputs. | Pass |
| VII. Human control | Guidance only; no unexplained score; `StatusEvaluator` exposes its rule expression. | Pass |
| VIII. Testable domain | Reference cases become table-driven domain tests; TDD red→green per case group. | Pass |
| IX. Simple before intelligent | Framework-free; no AI in the engine; AI only at feature adapters (extract/explain). | Pass |
| X. Privacy & minimization | Engine computes from supplied inputs only; retains nothing; no external sharing. | Pass |
| I. Financial truth | Formulas exactly match the normative contract; no new business rules introduced. | Pass |
## Project Structure

### Documentation (this feature)

```text
specs/financial-domain/
├── calculation-rules.md     # normative contract (single source of truth)
├── status-rules.md          # status conditions + policy
├── reference-cases.md       # normative financial oracle
├── plan.md                  # this file ($speckit-plan output)
├── research.md              # Phase 0 output
├── data-model.md            # Phase 1 output
├── quickstart.md            # Phase 1 validation guide
└── contracts/
    └── engine-contract.md   # pure Java engine API (not REST)
```

### Source Code (repository root)

```text
backend/src/main/java/com/financialgps/domain/
├── money/                   # Money, Currency, Rate, Ratio; RoundingPolicy
├── model/                   # FinancialInput, Income, Expense, Debt, Goal, Assumption,
│                            # TimelineChange, CashAllocationRule, GoalDependency,
│                            # NetCashFlow, AvailableCapacity, Contribution, Route
├── finance/                 # calculators: CashFlowCalculator, DebtCalculator,
│                            # GoalCalculator, AllocationCalculator
├── timeline/                # TimelineEngine (monthly periods + effective changes)
├── projection/              # ProjectionEngine, FinancialResult, provenance/explanation
├── policy/                  # FinancialPolicy, DebtPolicy, AllocationPolicy, StatusPolicy
├── status/                  # StatusEvaluator (COMPLETED..OFF_TRACK)
└── dependency/              # DependencyResolver (DAG / cycle + self-loop)

backend/src/test/java/com/financialgps/domain/
├── ReferenceCaseRunnerTest.java   # table-driven: every row of reference-cases.md
├── CashFlowCalculatorTest.java
├── DebtCalculatorTest.java
├── GoalCalculatorTest.java
├── TimelineEngineTest.java
├── AllocationCalculatorTest.java
├── DependencyResolverTest.java
├── ProjectionEngineTest.java
└── StatusEvaluatorTest.java
```

**Structure Decision**: The engine is a pure domain package inside the planned Maven backend,
kept free of web/persistence dependencies. Later `004–009` features build adapters around it and
never embed contradictory formulas. This matches the `004` plan's "framework-free Java domain
core" and keeps TDD fast.
## Traceability (FR → Design → Reference → Tests → Tasks)

| Requirement | Calculator / Type | Reference cases | Test |
|---|---|---|---|
| `calculation-rules` §3 (Net Cash Flow, Available Capacity) | `CashFlowCalculator` | `CF-001..003` | `CashFlowCalculatorTest` |
| `calculation-rules` §4 (amortization, final clamp, blocker) | `DebtCalculator` | `DC-001..004` | `DebtCalculatorTest` |
| `calculation-rules` §5 (remaining, progress, required capacity) | `GoalCalculator` | `G-001..005`, `RC-001..002` | `GoalCalculatorTest` |
| `calculation-rules` §2 (determinism + as-of) | `ProjectionEngine` | `DM-001..003` | `ProjectionEngineTest` |
| `status-rules` (5 statuses, tolerance policy) | `StatusEvaluator` + `StatusPolicy` | `status-001..006` | `StatusEvaluatorTest` |
| `calculation-rules` §10 (timeline, MONTHLY) | `TimelineEngine` | `TM-001..003` | `TimelineEngineTest` |
| `calculation-rules` §6–§8 (allocation, route, reallocation) | `AllocationCalculator` | `AL-001..006` | `AllocationCalculatorTest` |
| `calculation-rules` §9 (dependency DAG, cycle) | `DependencyResolver` | `AL-003, AL-004, AL-006` | `DependencyResolverTest` |
| `calculation-rules` §11/§12 (projection isolation, AI boundary) | `ProjectionEngine` (read-only state) | `SC-001..002` | `ProjectionEngineTest` |
| Full engine | `FinancialEngine.calculate(...)` | All of the above | `ReferenceCaseRunnerTest` |

Every requirement above maps to a reference case and a test. **If any requirement cannot be
traced, the plan is not complete** (this is the review gate requested before implementing).

## TDD Order (reference case → red test → minimal code → green → refactor)

The user-provided task ordering is adopted verbatim as the implementation sequence; each task
below is a TDD increment that starts from the failing reference case it must satisfy:

```text
T001  Project structure + build skeleton        T002  Money / value objects (+MoneyTest)
T003  FinancialPolicy (rounding, debt, alloc, status)
T004  FinancialState / model types
T005  CashFlowCalculator        → CF-001..003
T006  DebtCalculator            → DC-001..004
T007  GoalCalculator            → G-001..005, RC-001..002
T008  TimelineEngine            → TM-001..003
T009  AllocationCalculator      → AL-001..006
T010  DependencyResolver        → AL-003, AL-004, AL-006
T011  ProjectionEngine          → DM-*, SC-*
T012  StatusEvaluator           → status-001..006
T013  ReferenceCaseRunnerTest   → table-driven whole suite
T014  Integration/purity test   → classpath has no DB/HTTP/clock/AI deps
```

**Important**: never generate all classes before tests, then "hope". Each increment: write the
reference case as a failing test first, implement the smallest domain code to pass, then refactor.

## Complexity Tracking

No constitution violations or complexity exceptions are required. The engine stays a single,
pure, framework-free module; adapters (REST/persistence/auth) are explicitly deferred to later
features (`004/007`, …), honoring Principle IX.
| IV. Goal-driven roadmap | Engine exposes required capacity, dependency gating, and completion reallocation for route/roadmap. | Pass |