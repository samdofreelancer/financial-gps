# Implementation Tasks: Financial Goals (003)

**Branch**: `003-financial-goals`
**Date**: 2026-08-24 (updated 2026-10-07)
**Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)
**Status**: Implemented 2026-10-08

---

## 1. Traceability Matrix

| Task ID | Component / Area | Primary Requirements | User Stories | Target Test Artifacts |
|---|---|---|---|---|
| **T001** | Domain model & invariants | FR-001, FR-002, Invariants | US1 | `GoalDomainValidationTest.java` |
| **T002** | Progress calculator | FR-002, Rules §5 | US1 | `GoalProgressCalculatorTest.java` |
| **T003** | Capacity calculator | FR-003, §4.3 | US2 | `GoalCapacityCalculatorTest.java` |
| **T004** | Database migration V7 | FR-001, Invariants | US1 | `V7__goal.sql`, `GoalSchemaTest.java` |
| **T005** | Persistence & export | FR-001, FR-005 | US1 | `GoalRepositoryTest.java`, `GoalExportSectionTest.java` |
| **T006** | Application use cases | FR-001–FR-004 | US1, US2 | `GoalUseCasesTest.java` |
| **T007** | REST controller & DTO validation | FR-001–FR-005 | US1, US2 | `GoalControllerTest.java` |
| **T008** | Security & ownership isolation | FR-005, Constitution §XIV | US1, US2 | `GoalOwnershipIsolationTest.java` |
| **T009** | Full API journey | FR-001–FR-005 | US1, US2 | `GoalApiJourneyTest.java` |
| **T010** | Frontend client & store | FR-001–FR-004 | US1, US2 | `goals.ts`, `goalStore.ts`, `goalStore.test.ts` |
| **T011** | Frontend form & list UI | FR-001, FR-002 | US1 | `GoalForm.vue`, `GoalList.vue`, tests |
| **T012** | Frontend progress & capacity UI | FR-002, FR-003 | US2 | `GoalProgressCard.vue`, `GoalCapacity.vue`, tests |
| **T013** | View, routing, navigation | FR-001–FR-005 | US1, US2 | `GoalsView.vue`, router, `GoalsView.test.ts` |
| **T014** | E2E goal journey | SC-001..SC-003 | All | `e2e/financial-goals.spec.ts` |

## 2. Dependency / Execution Phases

```text
Phase 1: Pure Domain TDD
  [T001 Model] ──► [T002 Progress] ──► [T003 Capacity]
Phase 2: Persistence
  [T004 V7 schema] ──► [T005 Repository/Store/Export]
Phase 3: Application
  [T006 Use cases]
Phase 4: API & Security
  [T007 Controller] ──► [T008 Isolation] ──► [T009 API journey]
Phase 5: Frontend (Vue 3 + Pinia)
  [T010 Client/Store] ──► [T011 Form/List] ──► [T012 Progress/Capacity] ──► [T013 View/Nav]
Phase 6: E2E
  [T014 Playwright journey]
```

## 3. Detailed Tasks

### Phase 1: Pure Domain Layer (TDD first)

- [x] **T001: Goal Domain Model, Value Objects, Invariants**
  - **What**: `Goal.java`, `GoalStatus.java` (`ACTIVE`, `COMPLETED`, `ARCHIVED`),
    `GoalType.java`, under `com.financialgps.domain.goal`. No framework imports.
  - **Trace**: spec §4.1, §4.5, §8; plan §3.
  - **TDD (RED → GREEN)** — `GoalDomainValidationTest`:
    - Reject `targetAmount < 0`; reject `currentAmount < 0`.
    - Reject `priority < 1`; default `priority = 1` allowed.
    - `AMOUNT_REACHED` completion transitions `ACTIVE` → `COMPLETED`; archive terminal.
    - Invariant 4: `status == COMPLETED` iff the completion condition holds
      (`currentAmount >= targetAmount` for `AMOUNT_REACHED`); the condition is re-evaluated
      on every create/update mutation.
    - Ordering key `(priority, createdAt, id)` is deterministic and total.

- [x] **T002: GoalProgressCalculator**
  - **What**: pure `(Goal, asOfDate) -> GoalProgress` in
    `com.financialgps.domain.goal.GoalProgressCalculator` + `GoalCalculationPolicy`.
  - **TDD (RED → GREEN)** — `GoalProgressCalculatorTest`, cases:
    - `REF-G01` standard progress: 30000000/120000000 → remaining 90000000.00, progress 0.2500.
    - `REF-G02` exact completion: remaining 0.00, progress 1.0000, status COMPLETED.
    - `REF-G03` over-target clamp: 150000000 vs 120000000 → remaining 0.00, progress 1.0000.
    - `REF-G04` zero target: 0/0 → remaining 0.00, progress 1.0000, COMPLETED at creation.
    - `REF-G05` zero current: full remaining, progress 0.0000.
    - `REF-G06`/`REF-G07` negative target/current rejected (validation).
    - `REF-G08` completion re-evaluation on EVERY goal update: any update (create or PUT)
      resulting in `currentAmount >= targetAmount` transitions the goal to `COMPLETED`.
    - Dated vs undated goal both evaluate progress identically (date affects capacity, not progress).
    - Determinism: same inputs + same `asOfDate` → identical derived values.

- [x] **T003: GoalCapacityCalculator**
  - **What**: pure `(Goal, availableCapacity, asOfDate, policy) -> GoalCapacity` in
    `com.financialgps.domain.goal.GoalCapacityCalculator`.
  - **TDD (RED → GREEN)** — `GoalCapacityCalculatorTest`, cases:
    - `REF-C01` exact divisor: remaining 120000000, 12 months → 10000000.00.
    - `REF-C02` CEILING: remaining 121000000, 12 months → 10083333.34.
    - `REF-C03` undated goal → required/months null, coverage NOT_APPLICABLE.
    - `REF-C04` target == asOfDate, remaining > 0 → months 0, required = full remaining,
      feasibility EXPIRED_TARGET_DATE.
    - `REF-C05` target in the past → same expired semantics.
    - `REF-C06` completed goal → required 0.00, coverage NOT_APPLICABLE.
    - `monthsRemaining` boundary: largest m with `asOfDate.plusMonths(m) <= targetDate`.
    - `REF-A01` surplus capacity → MEETS_REQUIRED.
    - `REF-A02` equality (available == required) → MEETS_REQUIRED (boundary, not shortfall).
    - `REF-A03` positive but insufficient → SHORTFALL with exact monthly shortfall.
    - `REF-A04` availableCapacity 0.00 (zero/negative Net Cash Flow clamp) → SHORTFALL with
      full required amount.
    - `REF-A05` undated → NOT_APPLICABLE; `REF-A06` completed → NOT_APPLICABLE.
    - Determinism: same inputs + same asOfDate + same policy → identical result.

### Phase 2: Persistence & Infrastructure

- [x] **T004: Flyway `V7__goal.sql` + Schema Test**
  - Columns/constraints/indexes per plan §5; `GoalSchemaTest` via Testcontainers asserting
    CHECK constraints (non-negative amounts, priority >= 1, status enum) and FK cascade.

- [x] **T005: Goal Entity, Repository, Store, Export Section**
  - `GoalEntity`, `GoalRepository`, `JpaGoalStore` under
    `com.financialgps.infrastructure.persistence.goal`.
  - `GoalRepositoryTest`: owner filtering on every query; archive transition to `ARCHIVED`;
    archived excluded from default lists; cross-owner fetch impossible.
  - `GoalExportSectionTest`: `"goals"` section present in export output.
  - `GoalOwnershipCascadeTest`: account deletion hard-deletes goal rows.

### Phase 3: Application Layer

- [x] **T006: Goal Use Cases & Ports**
  - `CreateGoalUseCase`, `UpdateGoalUseCase`, `DeleteGoalUseCase`, `GetGoalsUseCase`,
    `GetGoalUseCase`, `GetGoalCapacityUseCase` under
    `com.financialgps.application.goal.usecase`.
  - `GoalUseCasesTest`: lifecycle transitions; server-assigned UUID; 404 on cross-owner/archived;
    capacity use case reads `AvailableCapacity` from `PositionReader` and never recomputes it;
    view models label `remaining`/`progress` as `calculated` and amounts as `actual`;
    `UpdateGoalUseCase` re-evaluates completion on every update so
    `currentAmount >= targetAmount` always yields `COMPLETED`.

### Phase 4: API & Security

- [x] **T007: REST Controller & DTO Validation**
  - `GoalController`, `GoalDtos` under `com.financialgps.api.goal` with all spec §6 endpoints.
  - `GoalControllerTest` (`@WebMvcTest`): POST 201; POST 400 on negative amounts / malformed
    decimals; GET detail returns remaining/progress; GET capacity returns coverage; DELETE 204
    archive; GET after archive 404.

- [x] **T008: Ownership Isolation Tests**
  - `GoalOwnershipIsolationTest`: User B gets 404 on GET/PUT/DELETE of User A's goal; B's list
    excludes A's goals.

- [x] **T009: End-to-End API Journey**
  - `GoalApiJourneyTest`: register/login → profile with income → create goal → verify
    remaining/progress → capacity equality (MEETS_REQUIRED) and shortfall cases → update
    currentAmount to target → COMPLETED → archive → 404.

### Phase 5: Frontend (Vue 3 + Pinia)

- [x] **T010: API Client & Store**
  - `frontend/src/api/goals.ts`, `frontend/src/stores/goalStore.ts`.
  - `goalStore.test.ts`: fetch/add/update/delete/fetchCapacity with mocked Axios.

- [x] **T011: GoalForm & GoalList**
  - `frontend/src/components/goals/GoalForm.vue`, `GoalList.vue` using `MoneyInput.vue`.
  - Tests: client validation blocks negative amounts; list renders status badges in priority
    order.

- [x] **T012: GoalProgressCard & GoalCapacity**
  - `GoalProgressCard.vue`, `GoalCapacity.vue`.
  - Tests: renders remaining/progress with calculated-vs-actual labels; coverage badge
    MEETS_REQUIRED/SHORTFALL/NOT_APPLICABLE; EXPIRED_TARGET_DATE explanation visible.

- [x] **T013: GoalsView, Router, Navigation**
  - `frontend/src/views/GoalsView.vue`; route `/goals`; sidebar entry.
  - `GoalsView.test.ts` integration of the above components.

### Phase 6: End-to-End

- [x] **T014: Playwright Goal Journey**
  - `e2e/financial-goals.spec.ts`: register → set profile income → create goal with target date →
    assert remaining/progress on screen → assert capacity view (equality then shortfall after
    profile expense increase) → archive → direct GET returns 404.
