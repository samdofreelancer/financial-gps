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
├── dependency/              # DependencyResolver (DAG / cycle + self-loop)
└── engine/                  # FinancialEngine.calculate(...) orchestration (locked order)

backend/src/test/java/com/financialgps/domain/
├── MoneyTest.java                        # T002 value objects + RoundingPolicy
├── ValueObjectTest.java               # T002 Rate/Ratio boundaries
├── ModelCompilerTest.java            # T004 canonical-terms / structural model guard
├── ReferenceCaseRunnerTest.java      # T014 table-driven: every reference row
├── CashFlowCalculatorTest.java       # T005
├── DebtCalculatorTest.java           # T006
├── GoalCalculatorTest.java           # T007
├── TimelineEngineTest.java           # T008
├── DependencyResolverTest.java       # T009
├── AllocationCalculatorTest.java     # T010
├── ProjectionEngineTest.java         # T011
├── StatusEvaluatorTest.java          # T012
├── FinancialEngineTest.java          # T013 end-to-end orchestration + DM-*
└── PurityTest.java                   # T015 architectural dependency isolation
                                      #     (no DB/HTTP/clock/AI in engine classpath)
```

**Test responsibility split (locked)**: `ModelCompilerTest` owns §0 canonical terminology and
model structure (`T004`). `PurityTest` owns architectural dependency isolation only (`T015`) —
it never asserts canonical field names or business values.

**Structure Decision**: The engine is a pure domain package inside the planned Maven backend,
kept free of web/persistence dependencies. Later `004–009` features build adapters around it and
never embed contradictory formulas. This matches the `004` plan's "framework-free Java domain
core" and keeps TDD fast.
## Traceability (normative rule → design → case/N/A → test → task)

**Principle**: Every normative requirement MUST map to:
a normative rule → a design element → a reference case **OR an explicit `N/A`** → an automated
test → a task. If a requirement cannot be traced the plan is incomplete — this is the review gate.
`N/A` is reserved for structural invariants (e.g., money precision) that are proven by unit
tests rather than the case table; it must be stated, never implied.

### Rule → Design → Test → Task

| Normative requirement | Design element | Reference case / `N/A` | Automated test | Task |
|---|---|---|---|---|
| §1 Money precision, scale, rounding (`BigDecimal`, never float) | `Money`, `Currency`, `RoundingPolicy` | `N/A` (structural; proven by `MoneyTest`) | `MoneyTest` | `T002` |
| §1 Rate (0…1, scale 6) & Ratio | `Rate`, `Ratio` | `N/A` (boundaries) | `ValueObjectTest` | `T002` |
| §0 Canonical terminology (fix field names) | all model types | `N/A` (compile/structural) | `ModelCompilerTest` | `T004` |
| §2 Determinism + `asOfDate` first-class | `ProjectionEngine`, `AsOfDate` | `DM-001..003` | `ProjectionEngineTest` | `T011` |
| §3 Net Cash Flow & Available Capacity | `CashFlowCalculator` | `CF-001..003` | `CashFlowCalculatorTest` | `T005` |
| §3 negative NCF reported, not concealed | `CashFlowCalculator` (+`NetCashFlow` VO) | `CF-003` | `CashFlowCalculatorTest` | `T005` |
| §4 amortization, final clamp, payment>balance, blocker | `DebtCalculator` | `DC-001..004` | `DebtCalculatorTest` | `T006` |
| §4 zero interest / extras / rate change (supported actions) | `DebtCalculator` | `DC-002`, `TM-003` | `DebtCalculatorTest`, `TimelineEngineTest` | `T006`, `T008` |
| §5 remaining=`max(...)`, progress, required capacity | `GoalCalculator` | `G-001..005`, `RC-001..002` | `GoalCalculatorTest` | `T007` |
| §6 contribution clamp + ETA | `GoalCalculator` (ETA), `AllocationCalculator` | `G-001, G-004`, `AL-001` | `GoalCalculatorTest`, `AllocationCalculatorTest` | `T007`, `T010` |
| §7 ordered allocation + cap | `AllocationCalculator` | `AL-001`, `CF-003` | `AllocationCalculatorTest` | `T010` |
| §7 completion reallocation | `AllocationCalculator` | `AL-002` | `AllocationCalculatorTest` | `T010` |
| §8 route order (default vs user/rule) | `AllocationCalculator` + `AllocationPolicy` | `AL-005` | `AllocationCalculatorTest` | `T010` |
| §9 dependency gating (DAG, cycle, self-loop) | `DependencyResolver` | `AL-003, AL-004, AL-006` | `DependencyResolverTest` | `T009` |
| §10 timeline + frequency `MONTHLY` | `TimelineEngine` | `TM-001..003` | `TimelineEngineTest` | `T008` |
| §11 actual vs projection isolation | `ProjectionEngine` (read-only state) | `SC-001..002` | `ProjectionEngineTest` | `T011` |
| §12 AI boundary (engine never computes money) | `ProjectionEngine` (no AI dep) | `N/A` (architectural) + `SC-*` | `PurityTest` | `T015` |
| `status-rules` five statuses + tolerance policy | `StatusEvaluator` + `StatusPolicy` | `status-001..006` | `StatusEvaluatorTest` | `T012` |
| Engine orchestration order (§ below) | `FinancialEngine` | `DM-*` (end-to-end determinism) | `FinancialEngineTest` | `T013` |
| Whole oracle green | `ReferenceCaseRunner` | all cases | `ReferenceCaseRunnerTest` | `T014` |

### Success criteria mapping

The measurable success criteria are proven by the same cases:

| Success criterion | Reference case(s) | Test / Task |
|---|---|---|
| Timeline change applies exactly at effective date | `TM-001..003` | `TimelineEngineTest` (`T008`) |
| Completion reallocation deterministic | `AL-002` | `AllocationCalculatorTest` (`T010`) |
| Dependency holds/cycle + self-loop rejected | `AL-003, AL-004, AL-006` | `DependencyResolverTest` (`T009`) |
| Status is a reproducible function (tolerance policy) | `status-001..006` | `StatusEvaluatorTest` (`T012`) |
| Determinism: same inputs + same `asOfDate` → identical result | `DM-001..003` | `ProjectionEngineTest`, `ReferenceCaseRunnerTest` (`T011`, `T014`) |
| No DB/HTTP/clock/random/AI dependency | `N/A` (architecture) | `PurityTest` (`T015`) |

## FinancialEngine orchestration (locked)

`FinancialEngine.calculate(...)` assembles the calculators in this exact order, so the
implementation agent does not choose the flow:

```text
FinancialEngine.calculate(inputs, assumptions, asOfDate, policy)
  ├── 1. validate inputs                      (domain-validation, typed exception on violation)
  ├── 2. resolve timeline                    (TimelineEngine.slice → monthly periods)
  ├── 3. calculate cash flow per period      (CashFlowCalculator → NetCashFlow, AvailableCapacity)
  ├── 4. calculate debt state per period     (DebtCalculator → payoff, blockers)
  ├── 5. resolve dependencies                (DependencyResolver → DAG/cycle/self-loop gate)
  ├── 6. calculate allocation per period     (AllocationCalculator → route, reallocation, contribution)
  ├── 7. calculate goal progress / ETA       (GoalCalculator)
  ├── 8. evaluate status                     (StatusEvaluator → COMPLETED..OFF_TRACK)
  └── 9. assemble FinancialResult            (with provenance + explanations)
```

Steps 2–8 are each a separate small step using `asOfDate` + `policy`; no step reads clock/DB/HTTP
or AI.

## TDD Order (reference case → red test → minimal code → green → refactor)

The user-provided task ordering is adopted verbatim; each task is a TDD increment that starts
from the failing reference case(s) it must satisfy. **T009 DependencyResolver runs before T010
AllocationCalculator because allocation depends on dependency resolution.**

```text
T001  Project structure + Maven/module skeleton
T002  Money + Rate + Ratio (+ RoundingPolicy)                 → MoneyTest, ValueObjectTest
T003  FinancialPolicy (rounding, debt, allocation, status)
T004  Core models  (each sub-task has its own test:)
│       T004a  FinancialInput + Income + Expense
│       T004b  Debt + Goal                       (Money/Rate/Ratio live ONLY in T002)
│       T004c  TimelineChange + CashAllocationRule + GoalDependency
T005  CashFlowCalculator        → CF-001..003
T006  DebtCalculator            → DC-001..004
T007  GoalCalculator            → G-001..005, RC-001..002
T008  TimelineEngine            → TM-001..003
T009  DependencyResolver        → AL-003, AL-004, AL-006
T010  AllocationCalculator      → AL-001..006
T011  ProjectionEngine          → DM-*, SC-*
T012  StatusEvaluator           → status-001..006
T013  FinancialEngine.calculate orchestration               → FinancialEngineTest (end-to-end DM-*)
T014  ReferenceCaseRunner       → table-driven whole suite   → ReferenceCaseRunnerTest
T015  Purity/architecture test  → engine classpath has no DB/HTTP/clock/AI deps
```

**Important**: never generate all classes before tests, then "hope". Each increment: write the
reference case as a failing test first, implement the smallest domain code to pass, then refactor.

## Complexity Tracking

No constitution violations or complexity exceptions are required. The engine stays a single,
pure, framework-free module; adapters (REST/persistence/auth) are explicitly deferred to later
features (`004/007`, …), honoring Principle IX.
| IV. Goal-driven roadmap | Engine exposes required capacity, dependency gating, and completion reallocation for route/roadmap. | Pass |