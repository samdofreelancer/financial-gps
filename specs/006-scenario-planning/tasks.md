# Tasks: Scenario Planning

- [ ] T001 Define scenario override and comparison DTOs in `backend/src/main/java/com/financialgps/api/scenario/`
- [ ] T002 [P] Write scenario isolation tests in `backend/src/test/java/com/financialgps/domain/scenario/ScenarioIsolationTest.java`
- [ ] T003 [P] Write baseline-comparison tests in `backend/src/test/java/com/financialgps/domain/scenario/ScenarioComparisonTest.java`
- [ ] T004 [US1] Implement in-memory override application in `backend/src/main/java/com/financialgps/domain/scenario/ScenarioOverrideApplier.java`
- [ ] T005 [US1] Implement read-only scenario evaluation service in `backend/src/main/java/com/financialgps/application/scenario/EvaluateScenarioService.java`
- [ ] T006 [US1] Implement `POST /api/v1/gps/scenarios/evaluate` in `backend/src/main/java/com/financialgps/api/scenario/ScenarioController.java`
- [ ] T007 [US1] Implement scenario editor and comparison UI in `frontend/src/features/scenarios/`
- [ ] T008 [US1] Add non-mutation E2E test in `frontend/e2e/scenario-planning.spec.ts`
- [ ] T009 [US2] Render changed-assumption and ETA/status difference details in `frontend/src/features/scenarios/components/ScenarioComparison.tsx`
- [ ] T010 [US2] Add PostgreSQL no-write integration test in `backend/src/test/java/com/financialgps/integration/ScenarioIsolationIT.java`
