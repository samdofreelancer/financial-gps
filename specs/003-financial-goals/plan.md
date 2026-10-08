# Implementation Plan: Financial Goals (003-financial-goals)

**Branch**: `003-financial-goals`
**Date**: 2026-08-24 (updated 2026-10-07)
**Spec**: [spec.md](spec.md)
**Status**: Implementation Ready

---

## 1. Scope & Boundaries

Feature 003 owns **Destination + Progress + Capacity** only:

- Goal CRUD with lifecycle `ACTIVE` / `COMPLETED` / `ARCHIVED` (soft delete).
- Deterministic derived values: `remaining`, `progress`, `requiredMonthlyCapacity`,
  `monthsRemaining`, `capacityCoverage`, `dateFeasibility` (all pure functions of inputs +
  `asOfDate`; normative rules in spec §4 and financial-domain §5).
- `priority` capture for deterministic ordering (consumed later by 005-Roadmap; no allocation
  executed here).
- Owner isolation and REST contracts identical in shape to `002-debt-management`.

Explicitly OUT of scope (no partial implementations):

- Allocation of `Available Capacity` to goals, Contribution routing, GPS status evaluation
  (`ON_TRACK`/`AT_RISK`/`OFF_TRACK`/`BLOCKED`), Roadmap staging, Scenario mutation, payoff
  optimization. 003 only READS `Available Capacity` from the Financial Position boundary.

---

## 2. Architectural Baseline

Aligned with 002:

- Backend: Spring Boot 3.2.2 / Java 21, layered `Interface → Application → Domain (pure)`,
  Infrastructure supporting; PostgreSQL 16, Flyway. Existing lineage
  `V1__auth.sql`, `V2__profile.sql`, `V3__debt.sql`, `V4`, `V5`, `V6` — next migration is
  **`V7__goal.sql`**.
- Auth: Spring Session JDBC, CSRF double-submit, `CurrentOwnerProvider`.
- Frontend: Vue 3 + Vite + TS + Pinia + vue-router + Axios; `MoneyInput.vue` presentation
  boundary (vi-VN; plain decimal string wire model).

## 3. Domain Boundary (`com.financialgps.domain.goal.*`)

Purity invariant: zero Spring/Jakarta/JPA/Jackson/Clock imports.

- **`Goal`**: value object with `name`, `goalType`, `targetAmount` (`Money`), `currentAmount`
  (`Money`), `targetDate?`, `priority` (>= 1), `completionCondition` (`AMOUNT_REACHED`),
  `status` (`ACTIVE`, `COMPLETED`, `ARCHIVED`).
- **`GoalStatus`** enum: `ACTIVE`, `COMPLETED`, `ARCHIVED`.
- **`GoalType`** enum: `DEBT_FREEDOM`, `EMERGENCY_FUND`, `SAVINGS`, `HOUSING`, `EDUCATION`,
  `RETIREMENT`, `OTHER`.
- **`GoalCalculationPolicy`**: `monetaryScale` (2), `ratioScale` (4), `rounding` (`HALF_UP`),
  `capacityRounding` (`CEILING`), `paymentFrequency` locked to `MONTHLY`.
- **`GoalProgressCalculator`**: pure function
  `(Goal, asOfDate) -> GoalProgress` exposing `remaining`, `progress`, `status`-transition
  evaluation. Test oracle: spec Table 9.1 (REF-G01…REF-G07).
- **`GoalCapacityCalculator`**: pure function
  `(Goal, availableCapacity: Money, asOfDate, policy) -> GoalCapacity` exposing
  `monthsRemaining`, `requiredMonthlyCapacity`, `capacityCoverage`, `monthlyShortfall`,
  `dateFeasibility`, `explanation`. Test oracle: spec Tables 9.2 (REF-C01…REF-C06) and 9.3
  (REF-A01…REF-A06).
- **No** duplicate cash-flow logic: Available Capacity always comes from
  `CashFlowCalculator`/`FinancialResult` (001/002).

`currentAmount` contract: user-supplied actual progress for THIS goal; NEVER derived from
`001` profile `savings`/`emergencyFund`. Documented in spec §4.1; enforced by the absence of any
profile-to-goal money copy in the application layer.

## 4. Application Boundary (`com.financialgps.application.goal.*`)

- Inbound ports: `CreateGoal`, `UpdateGoal`, `DeleteGoal` (soft-delete → `ARCHIVED`),
  `GetGoals` (excludes archived), `GetGoal`, `GetGoalCapacity`.
- Outbound ports: `GoalStore` (`save`, `findByIdAndOwner`, `findAllByOwner`, `archiveByIdAndOwner`),
  `PositionReader` (reads Financial Position / `AvailableCapacity` / `asOfDate` context),
  `BusinessDate`.
- Use cases map owner, seed business date, invoke the pure calculators, and assemble view models
  with provenance labels (`actual` vs `calculated`).

## 5. Persistence Boundary (`com.financialgps.infrastructure.persistence.goal.*`)

Table `goal` via `V7__goal.sql`:

- Columns: `id` UUID PK, `owner_id` UUID FK `account(id) ON DELETE CASCADE`, `name` TEXT NOT NULL,
  `goal_type` VARCHAR(32) NOT NULL, `target_amount` NUMERIC(19,2) NOT NULL,
  `current_amount` NUMERIC(19,2) NOT NULL, `target_date` DATE NULL, `priority` INT NOT NULL
  DEFAULT 1, `completion_condition` VARCHAR(32) NOT NULL DEFAULT 'AMOUNT_REACHED', `status`
  VARCHAR(16) NOT NULL, `created_at` TIMESTAMPTZ, `updated_at` TIMESTAMPTZ.
- Constraints: `CHECK (target_amount >= 0)`, `CHECK (current_amount >= 0)`,
  `CHECK (priority >= 1)`, `CHECK (status IN ('ACTIVE','COMPLETED','ARCHIVED'))`.
- Indexes: `ix_goal_owner (owner_id)`, `ix_goal_owner_status (owner_id, status)`.
- Soft delete: `UPDATE goal SET status='ARCHIVED', updated_at=now() WHERE id=:id AND
  owner_id=:ownerId AND status <> 'ARCHIVED'`.
- `GoalExportSection` registers section `"goals"` into `ExportOwnerDataUseCase`.

## 6. API Boundary (`com.financialgps.api.goal.*`)

- Endpoints: `GET/POST /api/v1/goals`, `GET/PUT/DELETE /api/v1/goals/{id}`,
  `GET /api/v1/goals/{id}/capacity` (see spec §6). No `/summary` aggregate is introduced; a
  portfolio view is the adapter's composition of per-goal views ordered by `(priority, createdAt,
  id)`.
- Validation: Bean Validation + canonical decimal pattern; `@Valid` on requests.
- Errors: `ProblemDetailAdvice` → `400 VALIDATION_FAILED`, `401 AUTH_REQUIRED`,
  `404 RESOURCE_NOT_FOUND` (missing, cross-owner, archived).

## 7. Frontend Architecture (Vue 3 + Pinia)

- `frontend/src/api/goals.ts` client; `frontend/src/stores/goalStore.ts`
  (`goals`, `loading`, `error`, actions `fetchGoals`, `addGoal`, `updateGoal`, `deleteGoal`,
  `fetchCapacity`).
- Components: `GoalForm.vue` (uses `MoneyInput.vue`; client-side non-negativity check),
  `GoalList.vue` (status badges, priority order), `GoalProgressCard.vue` (remaining/progress with
  calculated-vs-actual labels), `GoalCapacity.vue` (required capacity, coverage badge
  `MEETS_REQUIRED`/`SHORTFALL`/`NOT_APPLICABLE`, shortfall, expiration explanation).
- View: `frontend/src/views/GoalsView.vue`; router entry `/goals`; nav entry in
  `SidebarNav.vue`.
- Client never computes money: it renders server-derived `remaining`/`progress`/capacity.

## 8. Testing & Verification Strategy (TDD pyramid)

1. **Domain unit tests** (pure; normative oracle):
   - `GoalProgressCalculatorTest`: REF-G01…REF-G07 (progress, remaining, over-target clamp,
     zero target, negative amounts, completion transition, undated vs dated status).
   - `GoalCapacityCalculatorTest`: REF-C01…REF-C06 (dated, exact divisor, CEILING, undated null,
     target today, past target, completed zero capacity) and REF-A01…REF-A06 (equality boundary,
     shortfall, zero/negative capacity clamp, undated, completed).
   - `GoalDomainValidationTest`: non-negativity, `priority >= 1`, lifecycle transitions,
     canonical ordering key `(priority, createdAt, id)`.
2. **Persistence**: `GoalSchemaTest` (Testcontainers: constraints, FK cascade),
   `GoalRepositoryTest` (owner filters, archive transition), `GoalOwnershipCascadeTest`.
3. **Application**: `GoalUseCasesTest` (create/update/archive/list/capacity with stubbed
   `PositionReader`).
4. **API**: `GoalControllerTest` (201/200/204/400/401/404 matrix),
   `GoalOwnershipIsolationTest` (User B → 404 on GET/PUT/DELETE of User A's goal),
   `GoalApiJourneyTest` (create → view → update progress → capacity comparison → complete →
   archive → 404).
5. **Frontend**: `goalStore.test.ts`, `GoalForm.test.ts`, `GoalCapacity.test.ts`,
   `GoalsView.test.ts` (Vitest).
6. **E2E**: `e2e/financial-goals.spec.ts` (register → profile → create goal → verify
   remaining/progress → capacity view with equality and shortfall → archive → 404).
