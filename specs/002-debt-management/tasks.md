# Implementation Tasks: Debt Management (002-debt-management)

**Branch**: `features/debt-management`  
**Date**: 2026-09-27  
**Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)  
**Status**: Implementation Ready  

---

## 1. Traceability Matrix

| Task ID | Component / Area | Primary Requirements | User Stories | Target Test Artifacts |
|---|---|---|---|---|
| **T001** | Domain Model & Invariants | FR-001, FR-005, Invariants 1-4 | US1 | `DebtDomainValidationTest.java` |
| **T002** | Domain Payoff Calculator | FR-003, FR-004, Rules §4 | US1, US2 | `DebtPayoffCalculatorTest.java` |
| **T003** | Domain Summary & DTI Calculator | FR-002, Rules §0, §1 | US1, US2 | `DebtSummaryCalculatorTest.java` |
| **T004** | Cash Flow / Financial Position | FR-005, Constitution §XI | US3 | `CashFlowCalculatorDebtIntegrationTest.java` |
| **T005** | Database Migration & Schema | FR-001, Invariant 1, Ownership | US1 | `V3__debt.sql`, `DebtSchemaTest.java` |
| **T006** | Persistence Repositories & Store | FR-001, Ownership Boundary | US1 | `DebtRepositoryTest.java`, `JpaDebtStore.java` |
| **T007** | Account Export Section | FR-005, 007 Contract | US1 | `DebtExportSectionTest.java`, `DebtExportSection.java` |
| **T008** | Application Ports & Use Cases | FR-001, FR-002, FR-003, FR-004 | US1, US2 | `DebtUseCasesTest.java` |
| **T009** | REST API Controller & DTO Validation | FR-001, FR-004 | US1, US2 | `DebtControllerTest.java` |
| **T010** | Security & Ownership Isolation | Constitution §XIV, 007 Security | US1 | `DebtOwnershipIsolationTest.java` |
| **T011** | Full API Journey & Integration | FR-001 - FR-005 | US1, US2, US3 | `DebtApiJourneyTest.java` |
| **T012** | Frontend API Client & Pinia Store | FR-001 - FR-004, UX Boundary | US1, US2 | `debts.ts`, `debtStore.ts`, `debtStore.test.ts` |
| **T013** | Frontend Form & Money Input | FR-001, UX Boundary | US1 | `DebtForm.vue`, `DebtForm.test.ts` |
| **T014** | Frontend Summary & Blocker UI | FR-002, FR-004 | US1, US2 | `DebtSummaryCard.vue`, `DebtBlockerAlert.vue` |
| **T015** | Frontend Views & Navigation | FR-001 - FR-005 | US1, US2 | `DebtsView.vue`, `SidebarNav.vue`, router |
| **T016** | End-to-End User Journey Test | SC-001, SC-002, SC-003 | All | `e2e/debt-journey.spec.ts` |

---

## 2. Dependency Graph & TDD Execution Phases

```text
Phase 1: Pure Domain TDD
  [T001: Domain Model] ──► [T002: Payoff Calculator] ──► [T003: Summary & DTI] ──► [T004: Cash Flow Integration]
                                                                                          │
Phase 2: Persistence & Export                                                              ▼
  [T005: Flyway V3 Schema] ──► [T006: Repository & Store] ──► [T007: Export Adapter]
                                          │
Phase 3: Application Services             ▼
  [T008: Debt Use Cases & Ports (Record, Update, Summary, Delete)]
               │
Phase 4: API & Security Integration
  [T009: Controller & Validation] ──► [T010: Ownership Isolation] ──► [T011: API Journey Test]
                                                                               │
Phase 5: Frontend Vue 3 + Pinia                                                ▼
  [T012: Store & Client] ──► [T013: Form Component] ──► [T014: Summary & Blocker] ──► [T015: Page & Routing]
                                                                                               │
Phase 6: End-to-End Verification                                                              ▼
  [T016: Playwright / Vitest E2E Full Journey]
```

---

## 3. Detailed Implementation Tasks

### Phase 1: Pure Domain Layer (TDD First)

- [ ] **T001: Implement Pure Domain Debt Model and Invariants**
  - **What**: Create `Debt.java`, `DebtType.java`, `DebtStatus.java`, and `Rate.java` under `com.financialgps.domain.debt`.
  - **Why**: Establish core domain entity with immutable value objects and business invariants.
  - **Trace**: spec.md §4.1, §4.2, §12 (Invariants 1-4).
  - **TDD (RED → GREEN)**:
    - *Test*: Write `DebtDomainValidationTest.java`.
    - *Cases*: Reject negative balance; reject `minimumPayment <= 0` when balance > 0; reject `plannedPayment < minimumPayment`; enforce `PAID_OFF` when balance == 0.

- [ ] **T002: Implement Pure Domain Debt Payoff Calculator (Mathematical Oracle)**
  - **What**: Create `DebtPayoffCalculator.java` under `com.financialgps.domain.debt`.
  - **Why**: Implements monthly simple amortization, final payment clamp, and blocker detection without external dependencies.
  - **Trace**: spec.md §5, §11 (Table 11.1 Reference Cases REF-D01 to REF-D09), FR-003, FR-004.
  - **TDD (RED → GREEN)**:
    - *Test*: Write `DebtPayoffCalculatorTest.java`.
    - *Cases*:
      - Positive interest standard payoff (`REF-D01`: 1000 @ 12%, 50/mo → 24 payments, 127.08 interest).
      - Zero interest payoff (`REF-D02`: 1000 @ 0%, 100/mo → 10 payments).
      - Final payment clamp (`REF-D03`: 1000 @ 0%, 300/mo → 4th payment clamps to 100).
      - Overpayment clamp (`REF-D04`: 50 @ 12%, 100/mo → 1 payment, 50.50 final payment).
      - Blocker `PAYMENT_DOES_NOT_COVER_INTEREST` (`REF-D05`: 1000 @ 12%, 8/mo).
      - Blocker `PAYMENT_COVERS_ONLY_INTEREST` (`REF-D06`: 1000 @ 12%, 10/mo).
      - Blocker `INTEREST_RATE_MISSING` (`REF-D07`: 1000 @ null rate).
      - Zero balance already paid (`REF-D08`).
      - Max simulation horizon cutoff (360 periods).

- [ ] **T003: Implement Pure Domain Debt Summary & DTI Calculator**
  - **What**: Create `DebtSummaryCalculator.java` under `com.financialgps.domain.debt`.
  - **Why**: Aggregates portfolio totals, calculates DTI ratio, and derives portfolio debt-free date.
  - **Trace**: spec.md §6, §11 (Table 11.2 Reference Cases REF-P01 to REF-P04), FR-002.
  - **TDD (RED → GREEN)**:
    - *Test*: Write `DebtSummaryCalculatorTest.java`.
    - *Cases*:
      - Multi-debt portfolio aggregation (`REF-P01`: total balance 3000, min 250, plan 300, DTI 2.50%).
      - Portfolio with blocked debt (`REF-P02`: portfolio status becomes `BLOCKED`).
      - Empty/paid portfolio (`REF-P03`: total 0, DTI 0.00%, status `COMPLETED`).
      - Missing/zero income DTI handling (`REF-P04`: DTI ratio null, status `UNAVAILABLE`, reason `ZERO_OR_MISSING_INCOME`).

- [ ] **T004: Wire Debt Mandatory Payment into Domain CashFlowCalculator**
  - **What**: Update `FinancialInput.java` to strongly type `List<Debt> debts` and update `CashFlowCalculator.java` to compute `Mandatory Payment = sum(activeDebts.minimumPayment)`.
  - **Why**: Fulfill 001 contract where Mandatory Payment was deferred to 002.
  - **Trace**: spec.md §8.2, Constitution §XI, FR-005.
  - **TDD (RED → GREEN)**:
    - *Test*: Write `CashFlowCalculatorDebtIntegrationTest.java` (or extend `CashFlowCalculatorTest.java`).
    - *Cases*: Income 74M, Expense 30M, Debt Min 20M → Net Cash Flow = 24M, Available Capacity = 24M; negative cash flow honestly reported.

---

### Phase 2: Persistence & Infrastructure Layer

- [ ] **T005: Create Flyway Migration V3__debt.sql & Schema Test**
  - **What**: Create `backend/src/main/resources/db/migration/V3__debt.sql` and `DebtSchemaTest.java`.
  - **Why**: Provide authoritative database schema with owner foreign keys, cascade delete, and constraints.
  - **Trace**: plan.md §2.3, spec.md §9.
  - **TDD (RED → GREEN)**:
    - *Test*: `DebtSchemaTest.java` via Testcontainers PostgreSQL.
    - *Assertions*: Verify table creation, columns, indexes, foreign keys to `account(id) ON DELETE CASCADE`, and CHECK constraints.

- [ ] **T006: Implement Debt JPA Entity, Repository, and Store Adapter**
  - **What**: Create `DebtEntity.java`, `DebtRepository.java`, and `JpaDebtStore.java` under `com.financialgps.infrastructure.persistence.debt`.
  - **Why**: Provide owner-filtered CRUD persistence operations conforming to `DebtStore` outbound port.
  - **Trace**: plan.md §2.3, spec.md §9.
  - **TDD (RED → GREEN)**:
    - *Test*: Write `DebtRepositoryTest.java`.
    - *Assertions*: Owner isolation on queries (`findByOwnerId`, `findByIdAndOwnerId`), update mutation, and soft/hard delete.

- [ ] **T007: Implement Debt Export Section Adapter**
  - **What**: Create `DebtExportSection.java` implementing `OwnerDataSection` under `com.financialgps.infrastructure.persistence.debt`.
  - **Why**: Satisfy 007 data export requirement by registering `"debts"` into `ExportOwnerDataUseCase`.
  - **Trace**: spec.md §9.4, plan.md §2.3.
  - **TDD (RED → GREEN)**:
    - *Test*: Write `DebtExportSectionTest.java` and verify `ExportEndpointTest` outputs debt array.

---

### Phase 3: Application Layer

- [ ] **T008: Implement Debt Application Ports and Use Cases**
  - **What**: Create use cases (`RecordDebtUseCase`, `UpdateDebtUseCase`, `DeleteDebtUseCase`, `GetDebtsUseCase`, `GetDebtSummaryUseCase`) under `com.financialgps.application.debt.usecase`.
  - **Why**: Coordinate persistence, business date resolution, domain calculation, and view model assembly.
  - **Trace**: plan.md §2.2, spec.md §2 (US1, US2, US3, US4).
  - **TDD (RED → GREEN)**:
    - *Test*: Write `DebtUseCasesTest.java` using mock/in-memory store ports.
    - *Assertions*: Successful creation with server-assigned UUID, update logic, summary calculation binding active income for DTI, and 404 on cross-owner or non-existent debt.

---

### Phase 4: API & Security Layer

- [ ] **T009: Implement Debt REST API Controller and Validation DTOs**
  - **What**: Create `DebtController.java` and `DebtDtos.java` under `com.financialgps.api.debt`.
  - **Why**: Expose `/api/v1/debts` and `/api/v1/debts/summary` endpoints with Bean Validation and ProblemDetail error handling.
  - **Trace**: spec.md §10, plan.md §2.4.
  - **TDD (RED → GREEN)**:
    - *Test*: Write `DebtControllerTest.java` (`@WebMvcTest`).
    - *Cases*:
      - POST 201 with valid body.
      - POST 400 on negative amounts, non-decimal strings, or `plannedPayment < minimumPayment`.
      - GET /summary 200 with totals, DTI, and projections.

- [ ] **T010: Security and Ownership Isolation Tests**
  - **What**: Write `DebtOwnershipIsolationTest.java` under `backend/src/test/java/com/financialgps/api/debt/`.
  - **Why**: Guarantee User B cannot read, update, or delete User A's debts and receives 404.
  - **Trace**: spec.md §9, plan.md §2.5.
  - **TDD (RED → GREEN)**:
    - *Test*: Authenticate User A and create Debt A; authenticate User B and assert `GET /api/v1/debts/{debtA}` yields 404, `PUT` yields 404, and `DELETE` yields 404. Verify User B's summary excludes Debt A.

- [ ] **T011: Full Debt API Journey Test**
  - **What**: Create `DebtApiJourneyTest.java` under `backend/src/test/java/com/financialgps/api/debt/`.
  - **Why**: Validate the complete end-to-end API workflow against real database and session context.
  - **Trace**: spec.md §2, SC-001, SC-002.
  - **TDD (RED → GREEN)**:
    - *Flow*: Register & Login → Set profile income → Create 2 debts → Read summary (verify DTI and payoff date) → Update debt balance to 0 (verify transition to PAID_OFF) → Delete debt → Verify clean cash flow.

---

### Phase 5: Frontend Vue 3 + Pinia Implementation

- [ ] **T012: Implement Debt API Client, Types, and Pinia Store**
  - **What**: Create `frontend/src/api/debts.ts` and `frontend/src/stores/debtStore.ts`.
  - **Why**: Handle HTTP communication with CSRF tokens and manage debt state.
  - **Trace**: plan.md §4.
  - **TDD (RED → GREEN)**:
    - *Test*: Write `frontend/tests/stores/debtStore.test.ts` (mocking Axios).
    - *Cases*: Fetch debts, add debt, update debt, delete debt, refresh summary.

- [ ] **T013: Implement DebtForm Component with MoneyInput Integration**
  - **What**: Create `frontend/src/components/debts/DebtForm.vue`.
  - **Why**: Provide user input form for creditor, debt type, balance, interest rate, minimum payment, planned payment, and due day.
  - **Trace**: spec.md §10.2, plan.md §4.
  - **TDD (RED → GREEN)**:
    - *Test*: Write `frontend/tests/components/debts/DebtForm.test.ts`.
    - *Cases*: Client validation blocks submit when `plannedPayment < minimumPayment` or negative amounts; formatted display with `MoneyInput.vue`.

- [ ] **T014: Implement DebtSummaryCard & DebtBlockerAlert Components**
  - **What**: Create `DebtSummaryCard.vue` and `DebtBlockerAlert.vue` under `frontend/src/components/debts/`.
  - **Why**: Present portfolio totals, DTI badge, projected debt-free date, and explainable blocker callouts.
  - **Trace**: spec.md §2 (US2, US4), §8.1.
  - **TDD (RED → GREEN)**:
    - *Test*: Write `frontend/tests/components/debts/DebtSummaryCard.test.ts` and `DebtBlockerAlert.test.ts`.
    - *Cases*: Renders totals correctly; renders DTI percentage; displays alert box with reason explanation when status is `BLOCKED`.

- [ ] **T015: Implement DebtsView and Navigation Wiring**
  - **What**: Create `frontend/src/views/DebtsView.vue`, register route in `frontend/src/router/index.ts`, and add nav link in `frontend/src/components/SidebarNav.vue`.
  - **Why**: Deliver dedicated Debt Management user interface.
  - **Trace**: plan.md §4.
  - **TDD (RED → GREEN)**:
    - *Test*: Write `frontend/tests/views/DebtsView.test.ts` checking layout and component integration.

---

### Phase 6: End-to-End Acceptance Verification

- [ ] **T016: Implement End-to-End Debt User Journey Test**
  - **What**: Create `e2e/debt-journey.spec.ts` (or Vitest E2E equivalent).
  - **Why**: Validate complete user experience from browser to backend to database.
  - **Trace**: spec.md §2, SC-001, SC-002, SC-003.
  - **Test Scenario**:
    1. Register new user and set up financial profile (Income: 30,000,000 VND).
    2. Navigate to "Quản lý nợ" (`/debts`).
    3. Add Debt 1: Credit Card (Balance: 15,000,000 VND, Rate: 18%, Min: 1,500,000 VND, Planned: 3,000,000 VND).
    4. Assert summary updates: DTI = 5.00%, projected debt-free date visible.
    5. Add Debt 2: Insolvent Debt (Balance: 10,000,000 VND, Rate: 12%, Planned: 50,000 VND).
    6. Assert blocker alert appears with `PAYMENT_DOES_NOT_COVER_INTEREST`.
    7. Edit Debt 2 planned payment to 1,000,000 VND; assert blocker clears.
    8. Check Dashboard (`/dashboard`) and confirm `Mandatory Payment` reflects debt commitments.
