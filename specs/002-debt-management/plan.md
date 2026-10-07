# Implementation Plan: Debt Management (002-debt-management)

**Branch**: `features/debt-management`  
**Date**: 2026-09-27  
**Spec**: [spec.md](spec.md)  
**Status**: Implementation Ready  

---

## 1. Architectural Baseline & Evidence

### 1.1 Existing Architecture Alignment
- **Backend**: Spring Boot 3.2.2 on Java 21, single Maven module modular monolith.
  - Strict layered architecture per Constitution §XIV:
    `Interface (API)` → `Application` → `Domain (Pure)`.
    `Infrastructure` supports `Application` and `Domain`.
  - Database: PostgreSQL 16 via Flyway migrations. Current lineage: `V1__auth.sql`, `V2__profile.sql`. Next migration for Feature 002 is **`V3__debt.sql`**.
  - Authentication & Session: Spring Session JDBC (`SESSION` cookie), CSRF double-submit protection (`XSRF-TOKEN` / `X-XSRF-TOKEN`), `CurrentOwnerProvider` resolving authenticated `OwnerId`.
- **Frontend**: Vue 3 SPA with Vite, TypeScript, Pinia store, vue-router, Axios.
  - Money presentation: `MoneyInput.vue` (vi-VN locale formatting, dot grouping, comma decimals, plain decimal string wire model).
  - Navigation: `SidebarNav.vue` with routes `/dashboard`, `/profile`, and upcoming `/debts`.

### 1.2 Identified Architectural Conflicts & Resolutions
1. **Frontend Stack Drift**:
   - *Conflict in old 002 artifacts*: Early draft referenced React/TanStack/`features/debts/`.
   - *Resolution*: The actual codebase is Vue 3 + Pinia + Axios. All frontend design and tasks are re-homed to `frontend/src/views/DebtsView.vue`, `frontend/src/components/debts/`, and `frontend/src/stores/debtStore.ts`.
2. **Flyway Migration Sequence**:
   - *Conflict in old 002 tasks*: Referenced `V002__debts.sql`.
   - *Resolution*: `V1__auth.sql` and `V2__profile.sql` are already merged and active in production. The new migration MUST be **`V3__debt.sql`**.
3. **Cash Flow Integration**:
   - *Conflict*: Feature 001 hardcoded `Mandatory Payment = 0.00` with the note `debt logic belongs to 002`.
   - *Resolution*: Feature 002 enriches `FinancialInput` with `List<Debt> debts`. `CashFlowCalculator.calculate(FinancialInput, LocalDate, FinancialPolicy)` sums active debts' `minimumPayment` into `Mandatory Payment`. No second cash flow engine or parallel formula is introduced.
4. **Archive & Deletion Semantics**:
   - *Resolution*: `DELETE /api/v1/debts/{id}` performs an unambiguous soft-delete by transitioning `status = 'ARCHIVED'`. Rows are preserved in PostgreSQL for audit and data integrity, but filtered out of all normal queries. Hard delete occurs only upon `Account` deletion via `ON DELETE CASCADE`.
5. **Portfolio Blocker Propagation**:
   - *Resolution*: If any active debt projection is `BLOCKED`, the portfolio projection status is deterministically `BLOCKED` with `projectedDebtFreeDate = null` and reason `PORTFOLIO_CONTAINS_BLOCKED_DEBTS`.

---

## 2. Boundaries & Contracts

### 2.1 Domain Boundary (`com.financialgps.domain.debt.*`)
Purity invariant: Zero framework/infrastructure imports (no Spring, Jakarta, JPA, Jackson, or Clock).
- **`Debt`**: Aggregate entity / record:
  - `creditor`, `debtType` (`CREDIT_CARD`, `MORTGAGE`, `AUTO_LOAN`, `STUDENT_LOAN`, `PERSONAL_LOAN`, `OTHER`), `originalPrincipal`, `outstandingBalance`, `annualInterestRate` (optional Rate), `minimumPayment`, `plannedPayment`, `dueDay`, `status` (`ACTIVE`, `PAID_OFF`, `ARCHIVED`).
- **`Rate`**: Value object wrapping `BigDecimal` (scale 6, e.g. `0.180000`).
- **`DebtCalculationPolicy`**: Value object configuring:
  - `paymentFrequency` (`MONTHLY`), `maxSimulationMonths` (360), `monetaryScale` (2), `roundingMode` (`HALF_UP`).
- **`DebtPayoffCalculator`**: Pure domain function (canonical name; `tasks.md` T002 and
  `DebtPayoffCalculatorTest` bind to this name):
  `(Debt, asOfDate, policy) -> DebtProjectionResult`
  - Simulates month-by-month simple amortization.
  - Clamps final payment to exact remaining balance + interest.
  - Evaluates blockers: `PAYMENT_DOES_NOT_COVER_INTEREST`, `PAYMENT_COVERS_ONLY_INTEREST`, `INTEREST_RATE_MISSING`, `PAYOFF_HORIZON_EXCEEDS_MAXIMUM`.
- **`DebtSummaryCalculator`**: Pure domain function:
  `(List<Debt>, Income, asOfDate, policy) -> DebtSummaryResult`
  - Computes `totalOutstandingDebt`, `totalMinimumMonthlyPayment`, `totalPlannedMonthlyPayment`.
  - Computes `debtToIncomeRatio` = `totalMinimumMonthlyPayment / totalActiveIncome`.
  - If any active debt is blocked, portfolio projection status is `BLOCKED` with `null` debt-free date; otherwise `max(individual debt-free dates)`.

### 2.2 Application Boundary (`com.financialgps.application.debt.*`)
Orchestrates use cases, enforces transactional boundaries, maps `OwnerId`, seeds business dates, calls pure domain services:
- **Inbound Ports (`port.in`)**:
  - `RecordDebt`: create new debt.
  - `UpdateDebt`: update existing debt.
  - `DeleteDebt`: soft-delete debt to `ARCHIVED`.
  - `GetDebts`: list active and paid debts for owner (excludes `ARCHIVED`).
  - `GetDebtSummary`: assemble portfolio summary, DTI, and projections.
- **Outbound Ports (`port.out`)**:
  - `DebtStore`: persistence interface (`save`, `findByIdAndOwner`, `findAllActiveAndPaidByOwner`, `archiveByIdAndOwner`).
  - `ProfileReader`: reads active monthly income for DTI denominator from `ProfileStore` / `IncomeStore`.
  - `BusinessDate`: provides authoritative `today()` date.

### 2.3 Persistence Boundary (`com.financialgps.infrastructure.persistence.debt.*`)
- Table `debt` in PostgreSQL via `V3__debt.sql` (single-currency MVP: no `currency`
  column — `DebtView.currency` is derived from the owner's Financial Profile, default `VND`):
  - Columns: `id` (UUID PK), `owner_id` (UUID FK account CASCADE), `creditor` (TEXT NOT NULL), `debt_type` (VARCHAR(32) NOT NULL), `original_principal` (NUMERIC(19,2)), `outstanding_balance` (NUMERIC(19,2) NOT NULL), `annual_interest_rate` (NUMERIC(9,6)), `minimum_payment` (NUMERIC(19,2) NOT NULL), `planned_payment` (NUMERIC(19,2) NOT NULL), `due_day` (INT), `status` (VARCHAR(16) NOT NULL), `created_at` (TIMESTAMPTZ), `updated_at` (TIMESTAMPTZ).
  - Constraints:
    - `CHECK (outstanding_balance >= 0)`
    - `CHECK (minimum_payment >= 0)`
    - `CHECK (planned_payment >= minimum_payment)`
    - `CHECK (due_day BETWEEN 1 AND 31)`
    - `CHECK (status IN ('ACTIVE', 'PAID_OFF', 'ARCHIVED'))`
  - Indexes: `ix_debt_owner (owner_id)`, `ix_debt_owner_status (owner_id, status)`.
- Soft delete implementation: `archiveByIdAndOwner` issues `UPDATE debt SET status = 'ARCHIVED', updated_at = now() WHERE id = :id AND owner_id = :ownerId AND status != 'ARCHIVED'`.
- Implements `DebtExportSection`: registers section `"debts"` into `ExportOwnerDataUseCase`.
- Automatically registered in `OwnershipQueries` for zero-orphan cascade delete tests.

### 2.4 API Boundary (`com.financialgps.api.debt.*`)
- Endpoints:
  - `GET /api/v1/debts`
  - `POST /api/v1/debts`
  - `GET /api/v1/debts/{id}`
  - `PUT /api/v1/debts/{id}`
  - `DELETE /api/v1/debts/{id}` (soft-deletes to ARCHIVED; returns 204)
  - `GET /api/v1/debts/summary`
- Request validation: Bean validation (`@Valid`, `@NotBlank`, `@Pattern(regexp = "^\\d+(\\.\\d{1,2})?$")`, `@Digits`).
- Errors handled through `ProblemDetailAdvice`:
  - `400 VALIDATION_FAILED`
  - `401 AUTH_REQUIRED`
  - `404 RESOURCE_NOT_FOUND` (used for missing ID, cross-owner ID, and archived debt).

### 2.5 Security & Ownership Boundary
- All endpoints protected by Spring Security session cookie.
- CSRF token validation required on `POST`, `PUT`, `DELETE`.
- `CurrentOwnerProvider` extracts `OwnerId` from session context. No request DTO accepts `ownerId`.
- Cross-owner isolation verified by authorization matrix and isolation sweep tests.

---

## 3. Financial Position Integration Architecture

```text
Database (PostgreSQL)
  ├── profile, income, expense (001)
  └── debt (002, status in ('ACTIVE', 'PAID_OFF'))
        │
Application Service (GetProfileUseCase / DebtSummaryUseCase)
  ├── Loads IncomeRows, ExpenseRows (filtered by OwnerId)
  └── Loads DebtRows (filtered by OwnerId, status = 'ACTIVE')
        │
Maps to pure Domain Aggregate: FinancialInput
  ├── incomes: List<Income>
  ├── expenses: List<Expense>
  └── debts: List<Debt> (new in 002)
        │
FinancialEngine / CashFlowCalculator
  ├── Income = sum(incomes)
  ├── Expense = sum(expenses)
  ├── Mandatory Payment = sum(debts.minimumPayment)  <-- Wired in 002
  ├── Net Cash Flow = Income - Expense - Mandatory Payment
  └── Available Capacity = max(Net Cash Flow, 0.00)
        │
FinancialResult & DebtSummaryResult
  └── Provenance: Mandatory Payment labelled as calculated from active debts
```

---

## 4. Frontend Architecture (Vue 3 + Pinia)

- **Store**: `frontend/src/stores/debtStore.ts`
  - State: `debts: DebtView[]`, `summary: DebtSummaryView | null`, `loading: boolean`, `error: string | null`.
  - Actions: `fetchDebts()`, `fetchSummary()`, `addDebt(payload)`, `updateDebt(id, payload)`, `deleteDebt(id)`.
- **Components**:
  - `DebtList.vue`: Displays list of active and paid-off debts with status badges.
  - `DebtForm.vue`: Modal / inline form using `MoneyInput.vue` for balance, minimum payment, planned payment. Client validation: `plannedPayment >= minimumPayment`.
  - `DebtSummaryCard.vue`: Highlights total debt, total minimum payment, total planned payment, and DTI badge.
  - `DebtBlockerAlert.vue`: Displays machine-readable blocker messages when individual or portfolio projection status is `BLOCKED`.
  - `PayoffTimelineCard.vue`: Shows projected debt-free date, remaining payments, and total interest.
- **Views**:
  - `DebtsView.vue`: Top-level page combining summary card, blocker alerts, action buttons, and debt list.
  - Router: Add route `{ path: '/debts', name: 'debts', component: DebtsView, meta: { requiresAuth: true } }` in `frontend/src/router/index.ts`.
  - Navigation: Add "Quản lý nợ" (Debt Management) menu item in `SidebarNav.vue`.

---

## 5. Testing & Verification Strategy

Following the TDD and test pyramid order:
1. **Domain Unit Tests (Purity & Math Oracle)**:
   - `DebtPayoffCalculatorTest`: Pure unit tests validating all cases in Table 11.1 (`REF-D01` through `REF-D09`).
   - `DebtSummaryCalculatorTest`: Portfolio totals, DTI formula, portfolio blocker propagation, and edge cases in Table 11.2 (`REF-P01` through `REF-P04`).
   - `DebtDomainValidationTest`: Domain invariants (non-negativity, planned >= min payment, positive payment for positive balance).
2. **Persistence & Migration Tests**:
   - `DebtSchemaTest`: Testcontainers migration test validating table structure, foreign keys, and CHECK constraints.
   - `DebtRepositoryTest`: Tests `findAllActiveAndPaidByOwner`, `findByIdAndOwnerId`, and soft-delete transition to `ARCHIVED`.
   - `DebtOwnershipCascadeTest`: Validates automatic row deletion on account deletion via `OwnershipQueries`.
3. **Application Use Case Tests**:
   - `RecordDebtUseCaseTest`, `UpdateDebtUseCaseTest`, `DeleteDebtUseCaseTest` (verifies soft delete), `GetDebtSummaryUseCaseTest`.
4. **API & Security Tests (MockMvc)**:
   - `DebtValidationTest`: Verifies HTTP 400 on malformed input or `plannedPayment < minimumPayment`.
   - `DebtOwnershipIsolationTest`: Verifies User B receives HTTP 404 when attempting to GET, PUT, or DELETE User A's debt.
   - `DebtApiJourneyTest`: Full lifecycle via HTTP endpoints (create, list, update, summary, soft delete).
5. **Financial Position Integration Tests**:
   - `FinancialEngineDebtIntegrationTest`: Verifies `Mandatory Payment` updates `Net Cash Flow` and `Available Capacity`.
6. **Frontend Unit & Component Tests (Vitest)**:
   - `debtStore.test.ts`, `DebtForm.test.ts`, `DebtSummaryCard.test.ts`, `DebtBlockerAlert.test.ts`.
7. **End-to-End Test (Playwright / Vitest E2E)**:
   - `e2e/debt-journey.spec.ts`: Login → Navigate to Debts → Add Debt → Verify Summary & DTI → View Payoff ETA → Test Blocker Warning & Portfolio Blocker → Soft-Delete Debt.
