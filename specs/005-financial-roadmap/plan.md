# Implementation Plan: Financial Roadmap

**Branch**: `005-financial-roadmap` | **Date**: 2026-10-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-financial-roadmap/spec.md`

> This plan replaces the 2026-08-24 draft, which was a 7-line note written against a React
> frontend and a pre-DDD backend. It is written against the **implemented** stack: a pure-DDD
> Spring Boot backend (feature 010) and a Vue 3 + Pinia frontend.

## Summary

Turn the owner's debts and non-archived goals into one **ordered roadmap of stages**. Each stage carries
its objective, start condition, completion condition, its capacity figure, projected completion date,
progress, blockers and next action; the roadmap states which stage is current and why the next one
is not.

The roadmap is a **derived read model**: recomputed on read from the profile (001), the debt
portfolio (002) and the goals with their priority (003), using the **roadmap stage order** of
`spec.md` §7.2. That stage order is a presentation order, deliberately separate from the per-period
allocation order of `specs/financial-domain/calculation-rules.md` §7–§8 (owned by Feature 008). The
feature adds **no table, no migration, no write
endpoint and no money formula** — it composes the existing domain calculators and adds ordering,
sequencing and explanation. The result is exposed read-only through `GET /api/v1/roadmap` and
rendered by a Vue 3 screen.

## Technical Context

**Language/Version**: Java 21 LTS; TypeScript 5.x (strict) with Vue 3 SFCs

**Primary Dependencies**: Spring Boot 3.2 (MVC, validation, data access), PostgreSQL driver,
Flyway; Vue 3 + Pinia + Vue Router, Vite 5

**Storage**: PostgreSQL 16+; Flyway migrations; `numeric` monetary columns mapped to `BigDecimal`.
**This feature adds no table and no migration** — it reads the existing `profile`, `debt` and
`goal` tables. Migration numbers `V10..V12` stay reserved for Feature 008.

**Testing**: JUnit 5 + Spring Boot test support, Testcontainers PostgreSQL, Vitest (happy-dom),
Playwright 1.63 + Allure

**Target Platform**: Modern desktop and mobile browsers; Linux-hosted Spring Boot service

**Project Type**: Web application with a Vue 3 frontend and a REST backend (monorepo, single Maven
module)

**Performance Goals**: 95% of roadmap reads complete in under 1 second for an owner with up to 50
debts and 25 active goals (verified in T046 against the running stack); two reads with the same
facts and `asOf` return identical output (SC-004, asserted by T011 and T013).

**Constraints**: Monetary arithmetic uses `BigDecimal` and `numeric`, never float/double or
JavaScript `number`; the roadmap domain is framework-free (no Spring, JPA, HTTP, clock,
randomness, AI); the roadmap never reports a funded amount, a shortfall amount or an
`allocationStatus` (allocation is Feature 008's); stage `status` is positional and
`completionReached` is separate; the debt-stage ETA is the 002 plan-based projection and is never
presented as route-funded; reading the roadmap never mutates a stored fact.

**Scale/Scope**: One owner-scoped roadmap per read; one stage per non-archived, non-`DEBT_FREEDOM`
goal plus at most one debt-elimination stage; monthly projection cadence; VND as the initial
currency. Excludes allocation, dependencies, timelines, scenarios, snapshots and debt-order
strategies.

## Constitution Check

*GATE: passed before Phase 0 research and re-checked after Phase 1 design.*

| Principle / gate | Design response | Status |
|---|---|---|
| I. Financial truth first | Every derived value carries provenance (`actual` / `user-assumed` / `calculated`); the debt stage reports `progress = null` instead of inventing a ratio; an uncomputable ETA is `null` with a reason code. | Pass |
| II. Deterministic calculation | The roadmap is a pure function of (facts, `asOf`); the ordering key is `Goal.ORDERING` = `(priority, createdAt, id)` with nulls first, preserved from `GoalStore.findAllByOwner`; rounding comes from the existing policies. | Pass |
| III. Explainable GPS | Every stage states objective, conditions, progress, blockers, next action; the route states the applied order policy and why the next stage is not current. | Pass |
| IV. Goal-driven roadmaps | Each stage defines start and completion conditions, projected completion date, progress and next action — exactly as required. | Pass |
| V. Conservative planning | Each stage ETA is labelled with both `etaBasis` and `etaAssumption`: the debt stage `PORTFOLIO_PROJECTION` / `PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA` (it assumes the planned extra is paid in full), the goal stage `SINGLE_DESTINATION_HYPOTHESIS` / `WHOLE_AVAILABLE_CAPACITY`. It never claims a guaranteed outcome, and the UI repeats the assumption. | Pass |
| VI. Scenario isolation | No scenario logic in 005; the roadmap reads actual facts only. | Pass |
| VII. Human control | The roadmap is guidance: it names the next action, never prescribes a financial decision. | Pass |
| VIII. Testable financial domain | The roadmap generator is a pure domain service with a deterministic test matrix (spec §12). | Pass |
| IX. Simple before intelligent | No AI, no learned ordering, no automatic re-prioritisation. | Pass |
| X. Privacy / data minimisation | **No new stored data**; stage priority reuses the existing `goal.priority`; the response contains only the caller's own data. | Pass |
| XI. Financial time model | The roadmap is evaluated at an explicit `asOf` from the `RoadmapBusinessDate` port (which delegates to the single production clock, `SystemBusinessDate`); effective-dated lines come from the existing 001/002 readers. | Pass |
| XII. Cash allocation and deterministic routes | 005 delivers the **roadmap stage order** (a presentation order, distinct from the allocation order) and the mandatory disclosure of the applied ordering policy (`orderPolicy.policy` + `scope = ROADMAP_STAGE_ORDER`, spec §7.2). It never reports an allocation status and never states that a later stage is unfunded. The allocation half — partitioning free cash across destinations and reassigning it deterministically — is **deferred to Feature 008** by the ratified ownership matrix (`specs/008-financial-timing-allocation/open-decisions.md` §7); 005 MUST NOT implement it, so no competing formula is created. This is recorded below as an explicit feature-level exception under the constitution's deviation rule. | Pass — stage order + disclosure delivered; allocation half covered by a documented feature-level exception (008) |
| XIII. Review, history, audit | No snapshot in 005; the roadmap is reproducible from its inputs + `asOf`. | Pass |
| XIV. DDD architecture | New bounded slice `domain/roadmap` (pure), `application/roadmap` (ports + use case), `infrastructure` adapters, `api/roadmap` controller. The domain imports nothing but `domain` and the JDK; the guards stay green. | Pass |

**Gate result**: no violations. **One documented feature-level exception**: Principle XII's
allocation half (partitioning free cash and reassigning freed cash) is delivered by Feature 008, not
005. The constitution allows "an explicit feature-level exception" for a deviation; here the feature
delivers the other half of the same principle (roadmap stage order + applied-policy disclosure), and
the ownership matrix in `specs/008-financial-timing-allocation/open-decisions.md` §7 records the split
(roadmap stage presentation → 005; allocation and route-aware ETA → 008). No other principle needs
an exception.

**Approval status**: **both items approved 2026-10-24** — the Constitution XII feature-level exception
and the T023 shared `ProblemDetailAdvice` handler (a repo-wide error-contract change). The
documentation gate is closed and implementation runs from T001.

## Cross-Feature Dependencies

| Direction | Dependency | Contract |
|---|---|---|
| 005 → 001/002/003 | Profile facts, debt portfolio projection, goal facts and priority | Read-only ports; no new table, no write |
| 005 → 004 | `GpsEtaCalculator.ceilPeriods`, `CurrentPositionCalculator`, blocker/action vocabulary | Reuse, never fork |
| 005 → 008 | Allocation and route-aware ETA | 008 owns both; 005 forwards the route-aware ETA unchanged (`ROUTE_PROJECTION` / `ROUTE_ALLOCATION`) and simulates nothing |
| 008 → 004 | The 004 spec and REST contract must be amended before 008 implementation (008 C-03) | Documented in 008; 005 is a consumer, not the owner |
| 008 → 005 | Stage-order contract | 005's stage order is presentation-only; 008's allocation order (oracle §7–§8) is unaffected |

## Design Decisions (summary — full rationale in [research.md](research.md))

| # | Decision | Effect on the implementation |
|---|---|---|
| R-02 | The roadmap orders and explains; it never partitions money | No funding, shortfall or `allocationStatus` field, including the 004 single-destination shortfall in `nextAction`/`explanations`; no schedule; no allocation code. Stage order is presentation-only and `UPCOMING` is not a funding statement |
| R-03 | Stage ETA = a labelled forwarded projection (002 plan-based for debt, 004 hypothesis for goals) | Debt: `PORTFOLIO_PROJECTION` / `PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA`; goal: `SINGLE_DESTINATION_HYPOTHESIS` / `WHOLE_AVAILABLE_CAPACITY`; reuse `GpsEtaCalculator.ceilPeriods`; both labels mandatory in the response |
| R-04 | Priority reuses `goal.priority`; no new persistence | No migration, no entity, no write endpoint, no export/delete work |
| R-05 | One debt stage + one stage per non-archived, non-`DEBT_FREEDOM` goal (ACTIVE or COMPLETED) | `DEBT_FREEDOM` goals and ARCHIVED goals produce no stage; a `DEBT_FREEDOM`-only owner is `NO_DESTINATION` |
| R-06 | The current stage = the first stage whose completion condition is not met | Pure function, no stored state, implements FR-003; `completionReached` keeps the stage's own completion separate from its position |
| R-07 | v1 gates by sequence; the dependency gate arrives with 008 | `blockedBy` field reserved |
| R-08 | A separate read-only `GET /api/v1/roadmap` | 004's `/gps` success payload and domain semantics stay untouched. One deliberate cross-cutting change: T023 adds the shared `ProblemDetailAdvice` type-mismatch handler (research R-17) so malformed query parameters on 001–004 return the already documented `400 VALIDATION_FAILED` — error body only, no success or domain change |
| R-10 | Vue 3 + Pinia, matching the implemented frontend | `views/RoadmapView.vue`, `api/roadmap.ts`, `stores/roadmapStore.ts`, `components/roadmap/` |
| R-11 | Input/output ports + use case + infrastructure adapters | Required by `ArchitectureGuardTest` (wired input port, adapter per output port) |
| R-18 | `status` is not an allocation status | Positional status plus independent `completionReached`; no `allocationStatus` in v1; the UI shows a reached badge for `completionReached = true` even when `UPCOMING` |
| R-19 | 008 owns the route-aware ETA; 005 forwards it | The domain has no allocation loop and no second ETA algorithm; the post-008 values are `ROUTE_PROJECTION` / `ROUTE_ALLOCATION` |

## Project Structure

### Documentation (this feature)

```text
specs/005-financial-roadmap/
├── spec.md              # Feature specification (implementation ready)
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── rest-api.md      # Phase 1 output
├── checklists/
│   └── requirements.md  # Specification quality checklist
└── tasks.md             # Phase 2 output ($speckit-tasks)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/financialgps/
│   ├── domain/
│   │   └── roadmap/                     # NEW — pure: Roadmap, RoadmapStage, RoadmapGenerator, value objects
│   ├── application/
│   │   └── roadmap/                     # NEW — model/, port.in/, port.out/, usecase/
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── goal/RoadmapGoalAdapter.java        # NEW
│   │   │   ├── debt/RoadmapDebtAdapter.java        # NEW
│   │   │   └── profile/RoadmapProfileAdapter.java  # NEW
│   │   ├── time/
│   │   │   ├── SystemBusinessDate.java             # EXISTING — the single production clock
│   │   │   └── RoadmapBusinessDateAdapter.java     # NEW — delegates to it (mirrors GpsBusinessDateAdapter)
│   │   └── configuration/RoadmapUseCaseConfiguration.java  # NEW — same package as the package-private UseCaseTransactions
│   ├── api/
│   │   └── roadmap/RoadmapController.java          # NEW
│   └── platform/security/                          # EXISTING — CurrentOwnerProvider
├── src/test/java/com/financialgps/
│   ├── domain/roadmap/                             # NEW — generator, stage, ordering tests
│   ├── application/roadmap/usecase/                # NEW — use case tests with fake ports
│   ├── api/roadmap/                                # NEW — controller, ownership-isolation tests
│   └── architecture/                               # EXISTING — guards must stay green
└── src/main/resources/db/migration/                # UNCHANGED — no migration for this feature

frontend/
├── src/
│   ├── views/RoadmapView.vue                       # NEW
│   ├── api/roadmap.ts                              # NEW — typed client
│   ├── stores/roadmapStore.ts                      # NEW
│   ├── components/roadmap/                         # NEW — RoadmapTimeline.vue, RoadmapStageDetails.vue
│   ├── router/index.ts                             # UPDATED — /roadmap route (static import, requiresAuth)
│   └── components/BlockerPanel.vue, ExplanationPanel.vue, MoneyDisplay.vue   # EXISTING — reused
└── tests/
    ├── api/roadmap.test.ts                         # NEW
    └── components/roadmap/                         # NEW — Vitest

e2e/
├── specs/financial-roadmap.spec.ts                 # NEW
└── pages/RoadmapPage.ts                            # NEW — page object
```

**Structure Decision**: keep the single Maven module and mirror the 004 GPS slice
(`domain/gps` → `application/gps` → `infrastructure` → `api/gps`). The roadmap gets its own
bounded slice instead of extending `FinancialGpsResult`, because the road map is a *route* (zero or
many stages) while the GPS result is a *single destination* deep dive. The frontend mirrors the
existing `views/` + `components/` + `stores/` + `api/` layout instead of the `features/` tree the
2026-08-24 draft assumed.

## Implementation Order

1. **Domain** — value objects, `RoadmapGenerator`, ordering, current-stage rule with
   `completionReached`, and the v1 ETA labels (`PORTFOLIO_PROJECTION` / `SINGLE_DESTINATION_HYPOTHESIS`
   with their assumptions). Pure and testable first (TDD).
2. **Application** — `RoadmapModels`, ports, `GetRoadmapUseCase`, transaction demarcation.
3. **Infrastructure** — the three readers, the business date, the Spring wiring.
4. **API** — `RoadmapController` (owner-scoped, `asOf` optional).
5. **Frontend** — typed client, store, view, components (reached badge, ETA assumption), router entry.
6. **Acceptance** — Vitest, Playwright journey, quickstart validation, oracle documentation
   (T004: `calculation-rules.md` §8.1 distinguishes the roadmap stage order from the allocation
   order; the waterfall itself is unchanged).

## Complexity Tracking

No constitution violations and no complexity exceptions are required beyond the single
feature-level exception for Principle XII documented under the Constitution Check. The feature adds
no table, no migration, no write endpoint, no new money formula and no new dependency.

One deliberate cross-cutting touch: the shared `ProblemDetailAdvice` gains a
`MethodArgumentTypeMismatchException` handler (T023) so invalid query binding returns the documented
`400 VALIDATION_FAILED` problem instead of Spring's default body. It changes error bodies only —
never a successful response, a domain rule or a persisted fact. See research R-17.
