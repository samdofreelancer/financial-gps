---

description: "Dependency-ordered implementation tasks for Financial GPS"
---

# Tasks: Financial GPS

**Input**: Design documents from `/specs/004-financial-gps/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [REST contract](contracts/rest-api.md), and
[quickstart.md](quickstart.md)

**Tests**: Required by the project constitution and the feature's measurable success criteria.

**Organization**: Tasks are grouped by user story so each increment can be tested independently.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish the planned Java/Spring Boot and React workspace.

- [ ] T001 Create Maven Spring Boot 3.x project configuration in `backend/pom.xml`
- [ ] T002 Create React TypeScript strict-mode project configuration in `frontend/package.json`
- [ ] T003 [P] Configure backend quality tools in `backend/pom.xml` and `.editorconfig`
- [ ] T004 [P] Configure frontend linting, formatting, and test tooling in `frontend/eslint.config.js`
- [ ] T005 [P] Create local PostgreSQL development environment in `compose.yaml`
- [ ] T006 [P] Create frontend browser test configuration in `frontend/playwright.config.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create the data, precision, validation, and deterministic calculation foundation that
all GPS stories require.

**⚠️ CRITICAL**: Complete this phase before beginning user-story work.

- [ ] T007 Configure Spring datasource, Flyway, and Testcontainers properties in `backend/src/main/resources/application.yml`
- [ ] T008 Create initial PostgreSQL schema for profiles, incomes, expenses, debts, goals, and assumptions in `backend/src/main/resources/db/migration/V001__financial_inputs.sql`
- [ ] T009 [P] Implement `Money`, `Rate`, currency, scale, and rounding value objects in `backend/src/main/java/com/financialgps/domain/finance/`
- [ ] T010 [P] Implement profile, income, expense, debt, goal, and assumption domain entities in `backend/src/main/java/com/financialgps/domain/`
- [ ] T011 Implement JPA persistence mappings and repository adapters in `backend/src/main/java/com/financialgps/infrastructure/persistence/`
- [ ] T012 [P] Implement RFC 9457 error response and field-validation advice in `backend/src/main/java/com/financialgps/api/error/GlobalExceptionHandler.java`
- [ ] T013 [P] Implement typed API client, decimal-string money types, and QueryClient setup in `frontend/src/api/`
- [ ] T014 Implement profile/debt/goal input DTO validation and REST controllers in `backend/src/main/java/com/financialgps/api/input/`
- [ ] T015 Create controlled `MoneyInput` and localized `MoneyDisplay` components in `frontend/src/components/money/`
- [ ] T016 Write PostgreSQL migration and constraint integration tests in `backend/src/test/java/com/financialgps/integration/FinancialInputPersistenceIT.java`
- [ ] T017 Write money precision and rounding unit tests in `backend/src/test/java/com/financialgps/domain/finance/MoneyTest.java`

**Checkpoint**: Profile, debt, goal, and assumptions can be persisted with exact decimal values;
the backend emits validated errors; the frontend can send/receive decimal strings.

---

## Phase 3: User Story 1 - Understand Current Route (Priority: P1) 🎯 MVP

**Goal**: A user can select a goal and receive a deterministic, explainable baseline GPS result.

**Independent Test**: Seed a profile, debt, and goal; call `GET /api/v1/gps` twice with the same
as-of date; verify identical result and display current position, distance, progress, ETA/status,
blockers, and next action.

- [ ] T018 [P] [US1] Write deterministic current-position reference tests in `backend/src/test/java/com/financialgps/domain/gps/CurrentPositionCalculatorTest.java`
- [ ] T019 [P] [US1] Write debt payoff and ETA reference tests in `backend/src/test/java/com/financialgps/domain/gps/DebtPayoffCalculatorTest.java`
- [ ] T020 [P] [US1] Write goal distance/progress/capacity reference tests in `backend/src/test/java/com/financialgps/domain/gps/GoalProjectionCalculatorTest.java`
- [ ] T021 [US1] Implement `CurrentPositionCalculator` in `backend/src/main/java/com/financialgps/domain/gps/CurrentPositionCalculator.java`
- [ ] T022 [US1] Implement `DebtPayoffCalculator` in `backend/src/main/java/com/financialgps/domain/gps/DebtPayoffCalculator.java`
- [ ] T023 [US1] Implement `GoalProjectionCalculator` in `backend/src/main/java/com/financialgps/domain/gps/GoalProjectionCalculator.java`
- [ ] T024 [US1] Implement immutable GPS result, input snapshot, blocker, ETA, and next-action values in `backend/src/main/java/com/financialgps/domain/gps/`
- [ ] T025 [US1] Implement read-only `CalculateFinancialGpsService` with explicit `asOf` date in `backend/src/main/java/com/financialgps/application/gps/CalculateFinancialGpsService.java`
- [ ] T026 [US1] Implement `GET /api/v1/gps` response mapping per contract in `backend/src/main/java/com/financialgps/api/gps/FinancialGpsController.java`
- [ ] T027 [P] [US1] Write GPS REST contract and deterministic recalculation tests in `backend/src/test/java/com/financialgps/api/gps/FinancialGpsControllerIT.java`
- [ ] T028 [P] [US1] Implement GPS query hook and result DTO types in `frontend/src/features/gps/api/useFinancialGps.ts`
- [ ] T029 [US1] Implement current-position and destination summary UI in `frontend/src/features/gps/components/GpsSummary.tsx`
- [ ] T030 [US1] Implement distance, progress, ETA, blockers, and next-action UI in `frontend/src/features/gps/components/GpsRouteDetails.tsx`
- [ ] T031 [US1] Write frontend GPS result rendering tests in `frontend/src/features/gps/components/GpsSummary.test.tsx`
- [ ] T032 [US1] Add seeded end-to-end GPS baseline journey in `frontend/e2e/financial-gps-baseline.spec.ts`

**Checkpoint**: The MVP produces a reproducible baseline GPS for one goal and makes all result
fields visible without a client-side financial calculation.

---

## Phase 4: User Story 2 - Understand Status and Advice (Priority: P2)

**Goal**: A user can understand the named route status, capacity comparison, and measurable action
needed to improve the route.

**Independent Test**: Run otherwise identical profiles with sufficient capacity, insufficient
positive capacity, non-positive capacity, and a completed goal; verify each expected status and its
explanation.

- [ ] T033 [P] [US2] Write status transition reference tests for `ON_TRACK`, `AT_RISK`, `OFF_TRACK`, `BLOCKED`, and `COMPLETED` in `backend/src/test/java/com/financialgps/domain/gps/GpsStatusPolicyTest.java`
- [ ] T034 [US2] Implement documented status and threshold policy in `backend/src/main/java/com/financialgps/domain/gps/GpsStatusPolicy.java`
- [ ] T035 [US2] Implement rule-evaluation explanations and capacity shortfall generation in `backend/src/main/java/com/financialgps/domain/gps/GpsExplanationFactory.java`
- [ ] T036 [US2] Extend `CalculateFinancialGpsService` with status, capacity comparison, and explanation composition in `backend/src/main/java/com/financialgps/application/gps/CalculateFinancialGpsService.java`
- [ ] T037 [P] [US2] Write status/explanation REST tests in `backend/src/test/java/com/financialgps/api/gps/FinancialGpsStatusIT.java`
- [ ] T038 [US2] Implement status badge and capacity comparison components in `frontend/src/features/gps/components/GpsStatus.tsx`
- [ ] T039 [US2] Implement explanation, blocker, and action panels in `frontend/src/features/gps/components/GpsExplanation.tsx`
- [ ] T040 [P] [US2] Write status and explanation UI behavior tests in `frontend/src/features/gps/components/GpsStatus.test.tsx`
- [ ] T041 [US2] Extend end-to-end coverage for all five GPS statuses in `frontend/e2e/financial-gps-status.spec.ts`

**Checkpoint**: Every status is rule-based and shows its determining inputs, rule, and shortfall or
completion condition.

---

## Phase 5: User Story 3 - Review a Conservative Projection (Priority: P3)

**Goal**: A user can distinguish actual inputs, documented assumptions, and calculated projections.

**Independent Test**: Add a dated-goal assumption, retrieve GPS, and confirm the rendered result
labels the assumption and explains its ETA/status impact separately from actual data.

- [ ] T042 [P] [US3] Write assumption provenance and unavailable-ETA tests in `backend/src/test/java/com/financialgps/domain/gps/GpsAssumptionTest.java`
- [ ] T043 [US3] Implement assumption provenance and unavailable-ETA reason values in `backend/src/main/java/com/financialgps/domain/gps/ProjectionProvenance.java`
- [ ] T044 [US3] Extend GPS result mapping with actual/assumed/calculated sections in `backend/src/main/java/com/financialgps/api/gps/FinancialGpsResponseMapper.java`
- [ ] T045 [P] [US3] Write GPS provenance REST contract tests in `backend/src/test/java/com/financialgps/api/gps/FinancialGpsProvenanceIT.java`
- [ ] T046 [US3] Implement actual, assumption, and calculation breakdown components in `frontend/src/features/gps/components/GpsProvenance.tsx`
- [ ] T047 [P] [US3] Write provenance and unavailable-ETA UI tests in `frontend/src/features/gps/components/GpsProvenance.test.tsx`
- [ ] T048 [US3] Extend end-to-end coverage for labelled assumptions and unavailable ETA in `frontend/e2e/financial-gps-provenance.spec.ts`

**Checkpoint**: No projection can be mistaken for a fact; unavailable dates always name their
blocking input or condition.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verify the whole feature against its contract, constitution, and quickstart.

- [ ] T049 [P] Add API documentation and example problem responses in `specs/004-financial-gps/contracts/rest-api.md`
- [ ] T050 [P] Add accessibility labels and keyboard coverage to GPS components in `frontend/src/features/gps/components/`
- [ ] T051 Add query invalidation after input mutations in `frontend/src/api/queryInvalidation.ts`
- [ ] T052 Add performance-focused GPS read integration test in `backend/src/test/java/com/financialgps/integration/FinancialGpsPerformanceIT.java`
- [ ] T053 Run and record the full validation journey from `specs/004-financial-gps/quickstart.md`
- [ ] T054 Run a constitution compliance review and append findings to `specs/004-financial-gps/plan.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- Phase 1 has no dependencies.
- Phase 2 depends on Phase 1 and blocks all stories.
- US1 depends on Phase 2 and is the MVP.
- US2 depends on US1 because it extends the base GPS result with named status and explanations.
- US3 depends on US1 because it extends the same result with provenance.
- Phase 6 depends on completed desired stories.

### Parallel Opportunities

- T003–T006 can run in parallel after T001/T002 establish their projects.
- T009, T010, T012, and T013 can run in parallel after the project foundations exist.
- T018–T020 can be authored in parallel; T027 and T028 can proceed once the service response is
  stable.
- T033 and T037 can run in parallel with T038 once the status contract is agreed.
- T042, T045, and T046 can run in parallel after the provenance shape is defined.

## Parallel Example: User Story 1

```text
T018 CurrentPositionCalculator reference tests
T019 DebtPayoffCalculator reference tests
T020 GoalProjectionCalculator reference tests

After the response shape is stable:
T027 GPS REST contract tests
T028 Frontend GPS query hook
```

## Implementation Strategy

### MVP First

1. Complete Phases 1 and 2.
2. Complete US1 through T032.
3. Run its independent test and stop for product review.

### Incremental Delivery

1. Add US2 for explainable status and actions.
2. Add US3 for conservative-projection provenance.
3. Complete cross-cutting validation.

Scenario Planning, Financial Roadmap, and richer profile/debt/goal experiences remain separate
features under `005` and `006` specifications; they must consume this GPS core rather than expand
this task list.
