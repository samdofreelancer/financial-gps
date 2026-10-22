# Research: Financial Roadmap (005)

**Phase**: 0 (Outline & Research) — `$speckit-plan`
**Date**: 2026-10-22
**Inputs**: [spec.md](spec.md), `.specify/memory/constitution.md` (v1.2.0),
`specs/financial-domain/*`, `specs/008-financial-timing-allocation/open-decisions.md`,
`specs/004-financial-gps/spec.md`, and the implemented backend/frontend code.

Every decision below is either **normative** (fixed by the oracle — not re-decided here) or a
**new** decision for this feature, with the alternative that was rejected and why.

---

## 1. Normative inputs (not re-decided)

| # | Decision | Source |
|---|---|---|
| N-01 | The canonical money terms and their formulas (`Available Capacity = max(NetCashFlow, 0)`, `requiredMonthlyCapacity`, `remaining`, `progress`) | `calculation-rules.md` §0, §3, §5, §6 |
| N-02 | The default route order: committed debt tier → emergency-fund goal → goals by priority → remaining goals; the applied order MUST be disclosed | `calculation-rules.md` §8 |
| N-03 | Goal ordering key `(priority, createdAt, id)`, lower first | 003 §4.4 (`Goal.ORDERING`) |
| N-04 | `DEBT_FREE` completion is a dynamic 002 portfolio fact, never a sticky flag | `calculation-rules.md` §5, 003 D-6, 004 D-6 |
| N-05 | The emergency fund is a Feature 003 goal (`GoalType = EMERGENCY_FUND`); no `EMERGENCY_FUND` goal ⇒ the step is skipped, never invented | 008 ratified decision C-05 |
| N-06 | Status set and precedence `COMPLETED → BLOCKED → ON_TRACK → AT_RISK → OFF_TRACK` | `status-rules.md` |
| N-07 | Provenance labels `actual` / `user-assumed` / `calculated` on every value | `calculation-rules.md` §11 |
| N-08 | Projections never mutate persisted actual state | constitution VI |
| N-09 | Cross-owner access is `404 RESOURCE_NOT_FOUND`, never `403` | `docs/architecture/security.md`, AGENTS.md gotcha 8 |
| N-10 | The domain lane is framework-free; transactions are applied at the input-port boundary | constitution XIV, `UseCaseTransactions` |
| N-11 | Identity & Access is independent; the domain is identity-agnostic | constitution XIV |

## 2. What already exists in the code (reuse, never re-implement)

| Capability | Owner in the codebase | Use in 005 |
|---|---|---|
| Goal ordering | `domain/goal/Goal.ORDERING` = `(priority, createdAt, id)`, **nulls first**; `GoalStore.findAllByOwner` already returns that order | stage order inside the goal tiers |
| Goal facts and priority | `application/goal/port/out/GoalStore.findAllByOwner` | stage set |
| Goal remaining / progress | `domain/goal/GoalProgressCalculator` | stage progress |
| Goal required capacity, coverage, shortfall, date feasibility | `domain/goal/GoalCapacityCalculator` | stage required capacity + capacity-coverage next action (`CAPACITY_SHORTFALL` is an input token of the 004 action factory, not a blocker code; the roadmap never reports the shortfall amount) |
| Goal completion condition | `domain/goal/Goal.completionConditionFor` / `Goal.isDebtFree` | stage completion condition |
| Debt portfolio facts, projection, blocked debts | `application/debt/port/in/GetDebtSummary` (002) | debt stage |
| Available Capacity / Net Cash Flow | `domain/finance/CashFlowCalculator` via `CurrentPositionCalculator` | ETA hypothesis + position facts |
| Period ceiling for ETA | `domain/gps/GpsEtaCalculator.ceilPeriods` | stage ETA hypothesis |
| Blockers, next actions, explanations, provenance | `domain/gps/GpsBlockerFactory`, `GpsExplanationFactory`, `GpsProvenance` | stage blockers / next action / provenance |
| ETA unavailability reasons | `domain/gps/GpsEta.Reason` | stage ETA reason codes |
| Owner scoping, 404 hiding, export/delete | `platform/security`, `application/account`, `api/common` | owner isolation of the roadmap |
| Business date | `application/gps/port/out/GpsBusinessDate` (004) | `asOf` default |

**Consequence**: the roadmap needs **no new money formula**. It composes the existing
calculators and adds only ordering, sequencing and explanation.

## 3. New decisions for 005

### R-01 — Where does the roadmap live?

- **Decision**: a new bounded-context slice `domain/roadmap` + `application/roadmap` +
  `api/roadmap`, mirroring the 004 GPS slice, reusing 003/002 output ports through new adapter
  ports (`RoadmapGoalReader`, `RoadmapDebtReader`, `RoadmapProfileReader`) exactly as 004 did with
  `GpsGoalReader` / `GpsDebtReader`.
- **Rationale**: constitution XIV requires a bounded context per feature area; reusing the 004
  adapters would couple 005 to the GPS destination shape. Note that 005 is a **slice inside
  bounded context C (Financial GPS)** — the constitution's context table lists 004, 005, 006, 008
  and 009 together — so reusing 004's domain calculators (`CurrentPositionCalculator`,
  `GpsEtaCalculator`) is intra-context reuse, not a context violation. 005 still MUST NOT change
  004's success payload or domain semantics. One deliberate cross-cutting exception — the shared
  `ProblemDetailAdvice` type-mismatch handler (R-17/T023) — changes only the error body for invalid
  query binding on 001–004, aligning it with the already documented `400 VALIDATION_FAILED` contract.
- **Rejected**: extending `FinancialGpsResult` with a `stages[]` field — it would change the 004
  wire contract and couple two destinations' lifecycles.

### R-02 — Does the roadmap partition money?

- **Decision**: **No.** The roadmap reports order, conditions, capacity requirements, progress and
  a labelled ETA hypothesis. It reports no funded amount, no funded-amount shortfall per stage and
  no schedule. The `CAPACITY_SHORTFALL` next-action token it may reuse from 004 is a **capacity
  comparison** (`requiredMonthlyCapacity` vs Available Capacity), not an allocation shortfall — the
  roadmap never states how much money a stage receives or how much it is short.
- **Rationale**: `GpsRouteContext` already states *"multi-destination allocation not applied (owned
  by Feature 008)"*, and 008's ownership matrix gives 005 only "Roadmap stage presentation".
  Implementing a second partitioner would create two competing formulas — forbidden by constitution
  "Financial Data & Projection Rules" and by 008 analysis R-07.
- **Rejected**: computing a "suggested monthly contribution per stage" — it is the allocation rule
  of §7 of the oracle, owned by 008.

### R-03 — How is a stage's ETA computed without an allocation?

- **Decision**: the **single-destination hypothesis** of 004 D-2: *if the whole Available Capacity
  were directed at this stage*, using `ceilPeriods(remaining, availableCapacity)`. Labelled
  `etaBasis = SINGLE_DESTINATION_HYPOTHESIS` with the capacity amount used.
  `GpsEtaCalculator.ceilPeriods` divides by the capacity, so the caller MUST guard
  `capacity > 0` and report `UNAVAILABLE` (`NO_AVAILABLE_CAPACITY`) instead of dividing.
- **Rationale**: deterministic, already implemented (`GpsEtaCalculator`), and honest when labelled.
  The label is mandatory, because the hypothesis ignores competition between stages.
- **Rejected**: `requiredMonthlyCapacity`-based ETA only — unavailable for undated goals (003
  reports "no required capacity without a target date"), which would leave most goals without an
  ETA. The hypothesis is kept *alongside* the required capacity, never instead of it.
- **Rejected**: a multi-period route simulation — that is 008's allocation loop.

### R-04 — Where do priorities live?

- **Decision**: reuse `goal.priority` (003) and `Goal.ORDERING`. **No new table, no migration, no
  write endpoint.** Priority is edited with the existing `PUT /api/v1/goals/{id}`.
- **Rationale**: constitution X (data minimisation) — a second priority source would be a duplicated
  fact with a conflict resolution problem. 008 reserves migrations `V10..V12`; 005 needs none, so
  the numbering stays free.
- **Rejected**: the 2026-08-24 task "Create roadmap priority migration `V005__roadmap_priorities.sql`"
  — it would duplicate `goal.priority`, use a stale migration number, and add an owner-scoped
  table with export/delete obligations for no gain.

### R-05 — What is a stage?

- **Decision**: one `DEBT_ELIMINATION` stage when the 002 portfolio is not COMPLETED, plus one
  `GOAL` stage per non-archived goal that is not `DEBT_FREEDOM` (ACTIVE or COMPLETED). `DEBT_FREEDOM`
  goals do not create their own stage (they mirror the debt stage's completion condition), so a
  `DEBT_FREEDOM`-only owner with no debt gets `NO_DESTINATION`. ARCHIVED goals never create a stage.
- **Rationale**: matches the oracle order (debt tier first), keeps one stage = one objective, and
  avoids two stages describing the same 002 fact.
- **Rejected**: one stage per goal *including* `DEBT_FREEDOM` — it would duplicate the debt stage
  and confuse "which stage is current".

### R-06 — How is the current stage determined?

- **Decision**: the **first stage in route order whose completion condition is not met**. Earlier
  stages are `COMPLETED`, later stages are `UPCOMING` with `blockedBy` = the first incomplete
  earlier stage. All complete ⇒ `routeStatus = COMPLETED`, no current stage. No stages ⇒
  `NO_DESTINATION`.
- **Rationale**: implements FR-003 with a total, deterministic rule that needs no extra state and
  no dependency graph.
- **Rejected**: "first stage with the lowest progress" — ambiguous and not tied to a completion
  condition. **Rejected**: marking a stage complete when its goal says COMPLETED regardless of
  order — that is exactly the FR-003 violation.

### R-07 — Which gating mechanism in v1?

- **Decision**: the **sequence gate** ("all earlier stages are complete"). The goal-dependency gate
  arrives with Feature 008 and reuses the `blockedBy` field.
- **Rationale**: 008 owns `GoalDependency` (type, table, port, cycle detection — 008 gap G-01).
  Inventing a dependency model in 005 would duplicate it.
- **Rejected**: implementing a minimal dependency table in 005 — duplicate ownership, and 008's
  cycle rules would have to be re-implemented.

### R-08 — Does the roadmap need its own endpoint?

- **Decision**: yes — `GET /api/v1/roadmap` (owner-scoped, optional `asOf`), separate from
  `GET /api/v1/gps?goalId=…`.
- **Rationale**: the GPS view is a single-destination deep dive; the roadmap is a route overview
  with a different cardinality (zero or many stages). One endpoint keeps each payload honest and
  leaves 004's successful response contract untouched (008 R-09: additive only); the shared
  error-mapping handler is the documented exception (R-17).
- **Rejected**: overloading `/api/v1/gps` with an optional "roadmap mode" — two response shapes
  behind one path, and a breaking risk for the existing client.

### R-09 — Does 005 ship a UI?

- **Decision**: yes — a Vue 3 view (`RoadmapView.vue`), a typed API client, a Pinia store, Vitest
  unit tests and a Playwright E2E journey.
- **Rationale**: the constitution's Definition of Done requires an acceptance journey, and the
  feature's value ("see the route as stages") is only delivered through the screen. The 001-004
  features were all delivered as full vertical slices.
- **Rejected**: backend-only delivery — it would leave US1/US2 unverified at the user level.

### R-10 — Frontend structure

- **Decision**: follow the **existing Vue 3 + Pinia** structure — `frontend/src/views/RoadmapView.vue`,
  `frontend/src/api/roadmap.ts`, `frontend/src/stores/roadmapStore.ts`, route entry in
  `frontend/src/router/index.ts`, components in `frontend/src/components/roadmap/`, unit tests in
  `frontend/tests/**` (Vitest includes `tests/**`), E2E in `e2e/specs/financial-roadmap.spec.ts`.
- **Rationale**: the 2026-08-24 plan/tasks specified **React** and `.tsx` files; the repository is
  Vue 3. `frontend/src/features/` does not exist — the repository uses `views/` + `components/`.
- **Rejected**: creating a `features/` directory for one screen — inconsistent with the 001-004
  screens and with `frontend/tests` mirroring.

### R-11 — Application-layer shape

- **Decision**: input port `GetRoadmap` (`roadmap(OwnerId, LocalDate)` + the business-date overload
  `roadmap(OwnerId)`), application model `RoadmapModels`, output
  ports `RoadmapGoalReader` / `RoadmapDebtReader` / `RoadmapProfileReader` / `RoadmapBusinessDate`,
  use case `GetRoadmapUseCase` in `application/roadmap/usecase`, infrastructure adapters in
  `infrastructure/persistence/{goal,debt,profile}` + `infrastructure/time`, wiring in
  `infrastructure/configuration/RoadmapUseCaseConfiguration`, transaction demarcation through the
  existing `UseCaseTransactions` decorator.
- **Rationale**: the 2026-08-24 tasks jumped straight from the domain to the controller and would
  have failed `ArchitectureGuardTest` (which requires every input port to be wired and
  transaction-demarcated and every output port to have an infrastructure adapter).
- **Rejected**: calling the 004 use case from the roadmap controller — it would couple 005 to one
  destination at a time and force N+1 calls per stage.

### R-12 — Which persistence adapter reads the goals?

- **Decision**: a new `RoadmapGoalReader` adapter over the existing `GoalStore` (003) rather than
  over `GoalRepository` directly.
- **Rationale**: the adapter pattern keeps the JPA repository an implementation detail
  (`infrastructure.persistence`), and the domain stays identity-agnostic — the owner id is passed
  in, never read from the security context inside the domain.
- **Rejected**: reading `GoalRepository` from the domain — violates the dependency direction
  enforced by `ArchitectureGuardTest`.

### R-13 — How is a missing Financial Profile represented?

- **Decision**: the roadmap is still returned, with `MISSING_FINANCIAL_PROFILE` reported in the
  blockers/explanations and no capacity value anywhere.
- **Rationale**: 004 D-4 already established that behaviour; the roadmap must not invent a
  capacity or crash on a fresh account.
- **Rejected**: `404` — the profile is not the resource being requested; the owner exists.

### R-14 — What about archived goals and `DEBT_FREEDOM` goals?

- **Decision**: ARCHIVED goals produce no stage; a `DEBT_FREEDOM` goal produces no stage of its own
  (its condition is the debt stage's completion condition). With no active debt and no other goal,
  a `DEBT_FREEDOM`-only portfolio is `NO_DESTINATION` with `stages = []` (RM-005).
- **Rationale**: `GoalStatus.ARCHIVED` is a terminal tombstone (003); `DEBT_FREE` completion is a
  002 portfolio fact (003 D-6).
- **Rejected**: rendering a `DEBT_FREEDOM` goal as a stage — it would duplicate the debt stage and
  make "which stage is current" ambiguous.

### R-15 — What is the debt stage's `progress`?

- **Decision**: `null` with an explanation; the stage reports the remaining outstanding debt and
  the projected debt-free date instead.
- **Rationale**: a debt portfolio has no progress ratio, and 005 must not invent a denominator
  (constitution I: no fabricated financial values).
- **Rejected**: `1 − outstanding / originalPrincipal` — the original principal of a portfolio is
  not a stored fact (002 stores per-debt balances), so the ratio would be fabricated.

### R-16 — Does the roadmap need a `Research`-level open-questions file?

- **Decision**: no. All five decisions are closed as explicit defaults in spec §4 and can be
  overridden by an amendment. 008 keeps `open-decisions.md` because its decisions change the money
  model; 005 changes no formula.
- **Rejected**: copying 008's backlog format — it would add a second place to keep in sync for
  zero benefit.

### R-17 — Does 005 change shared API error handling?

- **Decision**: yes, one narrow cross-cutting change: T023 adds a
  `MethodArgumentTypeMismatchException` handler to the shared
  `api/common/ProblemDetailAdvice`, so a malformed `asOf` (or any query parameter that fails type
  conversion) returns the documented `400 VALIDATION_FAILED` problem with `violations` instead of
  Spring's default error body.
- **Rationale**: `GET /roadmap?asOf=abc` must follow the same RFC 7807 contract as every other
  endpoint (`docs/architecture/api-contract.md` §Errors); T013 asserts it. The handler is shared, so
  it also closes the same gap on 001–004 — deliberate, because it makes those endpoints match the
  contract that already documents `VALIDATION_FAILED`, and it changes only the error body for invalid
  binding: never a successful response, a domain rule or a persisted fact. `violations` keeps the
  existing `string[]` shape (`"<parameter>: <message>"`, e.g. `asOf: must be a valid ISO date`), so
  the frontend's `violations?: string[]` type is unchanged. There is no API test for `/gps`, so T023
  also reruns `e2e/specs/gps.spec.ts`.
- **Rejected**: leaving Spring's default body — it violates the documented error contract and would
  make the contract's `400 VALIDATION_FAILED` row untestable. **Rejected**: a roadmap-local
  `@RestControllerAdvice` — two error shapes for the same class of failure.

## 4. Open questions carried into implementation

| # | Question | Handling |
|---|---|---|
| OQ-1 | Will 008 change the stage ETA basis? | Yes, by design. The roadmap must keep the `etaBasis` field so the wire contract survives the change (additive, 008 R-09). |
| OQ-2 | Should 005 ever own the debt-order strategy (avalanche/snowball)? | Deferred by `calculation-rules.md` §8; 008 D-05 option B points at 005. A future 005 amendment may add it — not in v1. |
| OQ-3 | Does the oracle need a §8 amendment for the collapsed goal tiers? | Yes — a documentation task in `tasks.md` (no formula change). |
| OQ-4 | Does the roadmap need a write endpoint (reordering stages)? | No in v1. Priority is a goal fact; stage order follows it (R-04). |
