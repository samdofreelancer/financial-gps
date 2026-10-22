# Tasks: Financial Roadmap

**Input**: Design documents from `specs/005-financial-roadmap/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/rest-api.md`, `quickstart.md`

**Tests**: Required. The constitution's Definition of Done demands acceptance coverage, and
`spec.md` §12 defines a 14-case deterministic verification matrix.

**Organization**: Tasks are grouped by user story so each story is independently implementable and
testable. US1 delivers the ordered route (stages, order, current stage, the endpoint and the
timeline). US2 completes every stage with its transition conditions, blockers, next action and the
explanation of why the next stage is not current yet.

**Note**: This file replaces the 2026-08-24 draft, whose tasks pointed at a React frontend
(`.tsx`), a `frontend/e2e/` folder, a `frontend/src/features/` tree, a non-existent
`backend/src/test/.../integration/` package and a migration `V005__roadmap_priorities.sql` that
duplicated the existing `goal.priority`. Every path below matches the implemented repository.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: can run in parallel (different files, no dependency on incomplete tasks)
- **[Story]**: the user story the task belongs to (US1, US2)
- Setup / Foundational / Polish phases carry no story label

---

## Requirement Coverage

Every functional requirement and success criterion of `spec.md` maps to at least one task, and
every case of the verification matrix (spec §12) is covered.

| Requirement | Implemented by | Verified by |
|---|---|---|
| FR-001 ordered roadmap from the owner's facts | T015, T019, T020, T023 | T011, T013 |
| FR-002 every stage shows its conditions, capacity, ETA, progress, blockers, next action | T016, T017, T034, T035, T036, T037 | T011, T032, T033 |
| FR-003 current stage identified; a later stage is never completed early | T015, T034 | T011, T032 |
| FR-004 recomputed from the facts on every read | T018, T023 | T013 |
| FR-005 the order and the transitions are explained | T034, T026, T037 | T033, T038 |
| FR-006 no money partitioning, no funded amount | T015, T021, T034 | T011, T044 |
| FR-007 owner-scoped; no cross-owner leak | T014, T019, T020, T021, T023 | T014 |
| FR-008 the ETA carries its basis and is never fabricated | T017, T035 | T032, T033 |
| FR-009 same facts + same `asOf` ⇒ identical roadmap | T011, T013 | T011, T013 |
| FR-010 no new user fact; a read mutates nothing | T018, T023 | T044, T046 |
| SC-001 the current stage and its next action are visible without navigation | T027, T031, T036 | T031, T039 |
| SC-002 a transition happens only after the condition holds | T015, T034 | T011, T032, T039 |
| SC-003 90% of users can explain the next goal — post-launch metric | — | outside the automated suite (spec SC-003) |
| SC-004 two reads return identical output | T011, T013 | T011, T013, T046 |
| SC-005 the suite stays green; no duplicated formula | T010, T021 | T010, T044 |

| Verification case (spec §12) | Task |
|---|---|
| RM-001, RM-002, RM-003, RM-004, RM-005, RM-006 | T011 |
| RM-007, RM-008, RM-009 | T032 |
| RM-010, RM-011, RM-012, RM-013 | T011 |
| RM-014 | T014 |

---

## Phase 1: Setup (shared scaffolding)

**Purpose**: create the package skeleton of the new bounded slice and align the oracle with the
implemented v1 tier order.

- [ ] T001 Create `backend/src/main/java/com/financialgps/domain/roadmap/package-info.java` documenting the pure bounded slice, its oracle references (`calculation-rules.md` §8) and the "no framework, no clock, no money formula" rule
- [ ] T002 [P] Create the application slice folders with package docs: `backend/src/main/java/com/financialgps/application/roadmap/{model,port/in,port/out,usecase}/package-info.java`
- [ ] T003 [P] Register the roadmap route in `frontend/src/router/index.ts` — a **static** import of `views/RoadmapView.vue` (the router has no lazy loading) and `meta: { requiresAuth: true }`; add the navigation link to the journey group in `frontend/src/components/SidebarNav.vue` (`router-link` to `{ name: 'roadmap' }`, active state) and a `case 'roadmap'` entry in that file's contextual CTA map; create the placeholder `frontend/src/views/RoadmapView.vue` so the frontend build stays green until T027 replaces it
- [ ] T004 [P] Amend `specs/financial-domain/calculation-rules.md` §8 to state the collapsed v1 goal tiers explicitly (debt tier → emergency-fund goal → remaining goals by `(priority, createdAt, id)`, nulls first), matching spec XR-1, and correct the stale "debt strategy feature (005)" phrase so it names the roadmap's deferred debt-order strategy (spec XR-6) — documentation only, no formula change

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: the value objects, ports and records that both user stories build on.

**⚠️ CRITICAL**: no user-story work may start before this phase is complete.

- [ ] T005 [P] Create the domain value objects and enums in `backend/src/main/java/com/financialgps/domain/roadmap/`: `Roadmap`, `RoadmapStage`, `StageKind`, `StageStatus`, `RouteStatus`, `EtaBasis`, `OrderPolicy`, `Blocker`, `ProvenanceEntry`
- [ ] T006 [P] Create the domain input snapshots in `backend/src/main/java/com/financialgps/domain/roadmap/RoadmapInput.java` (`RoadmapInput`, `GoalSnapshot`, `DebtPortfolioFacts`, `ProfileSnapshot`) — facts only, no entity types
- [ ] T007 [P] Create the application records in `backend/src/main/java/com/financialgps/application/roadmap/model/RoadmapModels.java` (`RoadmapView`, `RoadmapStageView`, `OrderPolicyView`, `BlockerView`, `ProvenanceView`) with decimal-string amounts, the route-level `currency`, and ISO dates
- [ ] T008 [P] Create the input port `backend/src/main/java/com/financialgps/application/roadmap/port/in/GetRoadmap.java` (`roadmap(OwnerId, LocalDate)` and `roadmap(OwnerId)`)
- [ ] T009 [P] Create the output ports in `backend/src/main/java/com/financialgps/application/roadmap/port/out/`: `RoadmapGoalReader`, `RoadmapDebtReader`, `RoadmapProfileReader`, `RoadmapBusinessDate` — mirroring the 004 ports `GpsGoalReader` / `GpsDebtReader` / `GpsProfileReader` / `GpsBusinessDate`
- [ ] T010 Verify the existing architecture guards already cover the roadmap slice — no guard edit is expected: `ArchitectureGuardTest.simpleNamesOf` walks every `application/**/port/{in,out}` (so `GetRoadmap` is checked for wiring + transaction demarcation and every roadmap output port for an adapter), and `platform/security/DomainBoundaryGuardTest` walks every `domain/**` (no Spring/JDBC/Clock/Random). If either fails, fix the roadmap code, not the guard

**Checkpoint**: the read model, the ports and the guards are in place — both stories can start.

---

## Phase 3: User Story 1 - See the route as stages (Priority: P1) 🎯 MVP

**Goal**: generate an ordered roadmap of stages from the owner's debts and goals, identify the
current stage, expose it read-only and render it as a timeline.

**Independent Test**: create a debt and two goals, call `GET /api/v1/roadmap`, and verify the
stage order, the current stage and the rendered timeline (RM-001, RM-002, RM-003, RM-004, RM-005,
RM-013).

### Tests for User Story 1 (write first, they must fail before implementation) ⚠️

- [ ] T011 [P] [US1] Write the ordering and current-stage tests in `backend/src/test/java/com/financialgps/domain/roadmap/RoadmapGeneratorTest.java` (RM-001, RM-002, RM-003, RM-004, RM-005, RM-006, RM-010, RM-011, RM-012, RM-013: tier order, sequence gate, `blockedBy`, `NO_DESTINATION` — including the `DEBT_FREEDOM`-only variant, `COMPLETED`, archived and `DEBT_FREEDOM` goals excluded, the `Goal.ORDERING` nulls-first semantics on `createdAt`/`id`, and determinism — the same facts and `asOf` yield an identical roadmap)
- [ ] T012 [P] [US1] Write the use-case test with fake output ports in `backend/src/test/java/com/financialgps/application/roadmap/usecase/GetRoadmapUseCaseTest.java` (assembly only — no formulas, no clock)
- [ ] T013 [P] [US1] Write the API contract test in `backend/src/test/java/com/financialgps/api/roadmap/RoadmapControllerTest.java` (status 200, JSON shape, optional `asOf` echoed, malformed `asOf` → `400 VALIDATION_FAILED` with a `violations` entry `asOf: ...`, `NO_DESTINATION` payload, and two identical reads returning a byte-identical body — SC-004)
- [ ] T014 [P] [US1] Write the ownership-isolation test in `backend/src/test/java/com/financialgps/api/roadmap/RoadmapOwnershipIsolationTest.java` (RM-014: two owners seeded; each roadmap contains only the caller's goals, the reused `GET /goals/{id}` answers `404 RESOURCE_NOT_FOUND` for a foreign goal, and unauthenticated → `401 AUTH_REQUIRED`)

### Implementation for User Story 1

- [ ] T015 [US1] Implement the stage set, route order and current-stage rule in `backend/src/main/java/com/financialgps/domain/roadmap/RoadmapGenerator.java` (debt tier → emergency-fund tier → the remaining goals; preserve the order `GoalStore.findAllByOwner` already returns — `Goal.ORDERING`, nulls first — and MUST NOT define a second ordering key)
- [ ] T016 [US1] Implement the per-stage fact projection (progress, required capacity, remaining) in `backend/src/main/java/com/financialgps/domain/roadmap/RoadmapStageProjector.java` — consume the 003 values (`GoalProgressCalculator`, `GoalCapacityCalculator`), recompute nothing
- [ ] T017 [US1] Implement the ETA hypothesis in `backend/src/main/java/com/financialgps/domain/roadmap/RoadmapEtaHypothesis.java` using `domain/gps/GpsEtaCalculator.ceilPeriods` — guard `capacity > 0` **before** the call (it divides by the capacity), and if `ceilPeriods` returns its `Integer.MAX_VALUE` saturation (or `asOf.plusMonths` would overflow) report `UNAVAILABLE` with `PAYOFF_HORIZON_EXCEEDS_MAXIMUM` instead of a fabricated date
- [ ] T018 [US1] Implement the orchestration in `backend/src/main/java/com/financialgps/application/roadmap/usecase/GetRoadmapUseCase.java` (read facts → build snapshots → call the generator → map to views; no formulas)
- [ ] T019 [US1] Implement `backend/src/main/java/com/financialgps/infrastructure/persistence/goal/RoadmapGoalAdapter.java` over the existing `GoalStore` (non-archived goals of the owner — ACTIVE and COMPLETED; ARCHIVED never becomes a stage; `DEBT_FREEDOM` goals are mapped as facts and ignored by the generator, so they produce no goal stage and no `GoalLifecycle.reconcile` call is needed)
- [ ] T020 [US1] Implement `backend/src/main/java/com/financialgps/infrastructure/persistence/debt/RoadmapDebtAdapter.java` over `GetDebtSummary.summary(owner, asOf)` (portfolio projection status `AVAILABLE` / `BLOCKED` / `COMPLETED`, planned monthly payment, projected debt-free date, blocked debts)
- [ ] T021 [US1] Implement `backend/src/main/java/com/financialgps/infrastructure/persistence/profile/RoadmapProfileAdapter.java` over the 001 stores (`ProfileStore`, `IncomeStore`, `ExpenseStore`) — Available Capacity comes from the canonical `CurrentPositionCalculator` and the currency from `ProfileAssembler.currencyOf(profile)`; the roadmap MUST NOT introduce a second cash-flow formula or a second default currency
- [ ] T022 [US1] Implement `backend/src/main/java/com/financialgps/infrastructure/time/RoadmapBusinessDateAdapter.java` (delegates to the existing `SystemBusinessDate`, mirroring `GpsBusinessDateAdapter`) and wire the use case in `backend/src/main/java/com/financialgps/infrastructure/configuration/RoadmapUseCaseConfiguration.java` — the configuration MUST live in that same package, because `UseCaseTransactions` is package-private, and the input port is demarcated through `transactions.readOnly(...)`
- [ ] T023 [US1] Implement `backend/src/main/java/com/financialgps/api/roadmap/RoadmapController.java` — `GET /api/v1/roadmap`, optional `asOf`, owner from `CurrentOwnerProvider`, no business rule in the controller — and add a `MethodArgumentTypeMismatchException` handler to the shared `backend/src/main/java/com/financialgps/api/common/ProblemDetailAdvice.java` so a malformed query parameter returns `400 VALIDATION_FAILED` with a `violations` entry in the existing `"<parameter>: <message>"` string shape (e.g. `asOf: must be a valid ISO date`). This shared handler is the documented repo-wide error-contract alignment (research R-17), not a 004 behaviour change: rerun `e2e/specs/gps.spec.ts` afterwards, since 004 has no API test for `/gps`
- [ ] T024 [US1] Implement the typed client in `frontend/src/api/roadmap.ts` (decimal strings preserved, `null` preserved, route-level `currency` typed and exposed to the view)
- [ ] T025 [US1] Implement the store in `frontend/src/stores/roadmapStore.ts` (load, loading and error state, `asOf` pass-through)
- [ ] T026 [US1] Implement `frontend/src/components/roadmap/RoadmapTimeline.vue` (ordered stages, the current stage highlighted, the applied order policy shown)
- [ ] T027 [US1] Implement `frontend/src/views/RoadmapView.vue` (timeline, `NO_DESTINATION` empty state, `COMPLETED` state)
- [ ] T028 [US1] Add the Vitest client test `frontend/tests/api/roadmap.test.ts` (decimal strings, `null` handling, error mapping)
- [ ] T029 [US1] Add the Vitest component test `frontend/tests/components/roadmap/RoadmapTimeline.test.ts` (order rendering, current-stage highlight, empty state)
- [ ] T030 [US1] Add the page object `e2e/pages/RoadmapPage.ts` (open the roadmap, read the ordered stages, read the current stage)
- [ ] T031 [US1] Add the E2E journey `e2e/specs/financial-roadmap.spec.ts` (register → seed profile/debt/goals → open `/roadmap` → see the ordered stages and the current stage)

**Checkpoint**: US1 is fully functional and testable on its own — the roadmap is readable through
the API and visible on the screen.

---

## Phase 4: User Story 2 - Understand transition conditions (Priority: P2)

**Goal**: every stage states what must happen to complete it, what blocks the next one, and what
the user should do next.

**Independent Test**: open a stage before and after its condition is satisfied and verify the
completion explanation, the start condition, the blockers and the next action (RM-006, RM-007,
RM-008, RM-009).

### Tests for User Story 2 (write first, they must fail before implementation) ⚠️

- [ ] T032 [P] [US2] Write the stage-detail tests in `backend/src/test/java/com/financialgps/domain/roadmap/RoadmapStageDetailsTest.java` (RM-007, RM-008, RM-009: start condition, completion condition, `blockedBy`, blockers, next action, `etaReasonCode` when the ETA is unavailable, the undated-goal explanation, `NO_AVAILABLE_CAPACITY`, `MISSING_FINANCIAL_PROFILE`)
- [ ] T033 [P] [US2] Write the API test for the detail fields in `backend/src/test/java/com/financialgps/api/roadmap/RoadmapStageDetailsApiTest.java` (detail fields present in the response, `null` ETA always carries a reason code, explanations mention the blocking earlier stage)

### Implementation for User Story 2

- [ ] T034 [US2] Implement the stage-detail projection in `backend/src/main/java/com/financialgps/domain/roadmap/RoadmapStageDetailFactory.java` (start/completion conditions, blockers from the 004 `GpsBlocker` vocabulary — no fabricated code — next action, `blockedBy`, route explanations)
- [ ] T035 [US2] Expose the detail fields through `backend/src/main/java/com/financialgps/application/roadmap/model/RoadmapModels.java` and `backend/src/main/java/com/financialgps/application/roadmap/usecase/GetRoadmapUseCase.java`
- [ ] T036 [US2] Implement `frontend/src/components/roadmap/RoadmapStageDetails.vue` (conditions, required capacity, ETA basis, blockers, next action) reusing `frontend/src/components/BlockerPanel.vue` and `frontend/src/components/ExplanationPanel.vue`
- [ ] T037 [US2] Extend `frontend/src/views/RoadmapView.vue` to open a stage and show its details, and to state why the next stage is not current
- [ ] T038 [US2] Add the Vitest component test `frontend/tests/components/roadmap/RoadmapStageDetails.test.ts` (conditions, blockers, next action, ETA-unavailable rendering)
- [ ] T039 [US2] Extend the E2E journey `e2e/specs/financial-roadmap.spec.ts` with the transition scenario (open the stage detail, read the blocking explanation)

**Checkpoint**: US1 and US2 both work independently — the route is visible and every stage is
explainable.

---

## Phase 5: Polish & cross-cutting concerns

- [ ] T040 [P] Document the endpoint in `docs/architecture/api-contract.md` (`GET /api/v1/roadmap`, response shape, error semantics)
- [ ] T041 [P] Document the stage model and order rules in `docs/architecture/business-domain.md` (stage set, tiers, current-stage rule, ETA basis)
- [ ] T042 [P] Update the package map in `docs/architecture/overview.md` with the `domain/roadmap`, `application/roadmap`, `api/roadmap` slice
- [ ] T043 [P] Record the reusable findings in `docs/development/agent-notes.md` (a checked `tasks.md` does not mean implemented; the oracle §8 tier collapse; the 2026-08-24 draft's React paths)
- [ ] T044 Run the full backend suite `cd backend && ./mvnw -B test` and the frontend suite `cd frontend && npm run test:unit` — no regression against 001-004
- [ ] T045 Run the end-to-end suite `cd e2e && npx playwright test --workers=1` (serial for backend stability)
- [ ] T046 Validate the scenarios in `specs/005-financial-roadmap/quickstart.md` against the running stack, time one roadmap read against the plan's performance goal (95% under 1 second for 50 debts / 25 goals) and record the result
- [ ] T047 Mark the completed checkboxes in `specs/005-financial-roadmap/tasks.md` and set `specs/005-financial-roadmap/spec.md` status to `Implemented` with the delivery note

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies — can start immediately.
- **Foundational (Phase 2)**: depends on Phase 1 — **blocks all user stories**.
- **User Stories (Phase 3, 4)**: depend on Phase 2. US1 is the MVP; US2 depends on the roadmap
  response produced by US1 but is implemented and verified on its own stage-detail tests.
- **Polish (Phase 5)**: depends on US1 and US2.

### User Story Dependencies

- **US1 (P1)**: after Phase 2 — no dependency on other stories.
- **US2 (P2)**: after Phase 2 — consumes the US1 response shape but is independently testable.

### Within Each User Story

- Tests first, and they must fail before the implementation.
- Domain → application → infrastructure → API → frontend.
- Story complete before moving to the next priority.

### Parallel Opportunities

- Phase 1: T002, T003, T004 are independent of T001.
- Phase 2: T005–T009 are independent of each other; T010 depends on the packages existing.
- Phase 3: T011–T014 (tests) run in parallel; T019–T021 (adapters) run in parallel after T015.
- Phase 4: T032 and T033 run in parallel; T036 and T038 can run in parallel once T035 lands.

---

## Parallel Example: User Story 1

```bash
# Write the failing tests first (independent files):
Task: "Write the ordering and current-stage tests in backend/src/test/java/com/financialgps/domain/roadmap/RoadmapGeneratorTest.java"
Task: "Write the API contract test in backend/src/test/java/com/financialgps/api/roadmap/RoadmapControllerTest.java"
Task: "Write the ownership-isolation test in backend/src/test/java/com/financialgps/api/roadmap/RoadmapOwnershipIsolationTest.java"

# Then the infrastructure adapters (independent files):
Task: "Implement backend/src/main/java/com/financialgps/infrastructure/persistence/goal/RoadmapGoalAdapter.java"
Task: "Implement backend/src/main/java/com/financialgps/infrastructure/persistence/debt/RoadmapDebtAdapter.java"
Task: "Implement backend/src/main/java/com/financialgps/infrastructure/persistence/profile/RoadmapProfileAdapter.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phase 1 — setup and oracle alignment.
2. Phase 2 — foundational read model and ports.
3. Phase 3 — US1: the ordered route, the endpoint and the timeline.
4. **STOP and VALIDATE**: run the quickstart scenarios RM-001…RM-005 and the E2E journey.
5. Demo the roadmap screen.

### Incremental Delivery

1. Setup + Foundational → the read model exists.
2. US1 → the owner sees the route and the current stage (MVP).
3. US2 → the owner understands every transition.
4. Polish → the oracle, the architecture docs and the agent notes agree with the code.

### Parallel Team Strategy

With two developers:

- Developer A: backend domain + application + API (T015–T023).
- Developer B: frontend client, store and view (T024–T029) against the contract in
  `contracts/rest-api.md` — no dependency on the backend code, only on the agreed JSON shape.
- Then US2 on the same split, and the E2E journey once both sides are green.

---

## Notes

- [P] tasks = different files, no dependency.
- [Story] label maps a task to its user story for traceability.
- Every user story is independently completable and testable.
- Verify the tests fail before implementing.
- Commit after each task or logical group.
- Avoid: vague tasks, same-file conflicts, and cross-story dependencies that break independence.
- **No task creates a migration or a table**: this feature persists nothing (spec FR-010, research
  R-04). If a future amendment adds persistence, migration numbers `V10..V12` are reserved by
  Feature 008.
- **A checked box means implemented, not planned** — T043 records the trap that made this explicit.
