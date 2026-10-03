# Feature Specification: Debt Management

**Feature Branch**: `features/debt-management`  
**Created**: 2026-08-24  
**Last Updated**: 2026-09-27  
**Status**: Specification Complete (Implementation Ready)  
**Input**: User problem: "Who do I owe, how much do I owe, what is my mandatory monthly burden versus planned payment, and when will I become debt-free?"

---

## 1. Executive Summary & Problem Statement

Users carrying debt face four critical questions:
1. **Creditor & Balance**: Whom do I owe, and what is the current outstanding balance?
2. **Monthly Burden**: What is my non-negotiable minimum monthly payment versus what I actually plan to pay each month?
3. **Payoff Horizon**: When will each debt and my entire debt portfolio reach zero balance under my planned payments?
4. **Feasibility & Blockers**: If a payoff date cannot be calculated (e.g. payment does not cover interest or interest rate is missing), why is it blocked and what is the exact financial reason?

SpecKit 002 establishes **Debt Management** as an integral component of Financial GPS. It captures debt facts, enforces deterministic monthly amortization, calculates Debt-To-Income (DTI) ratio, computes standalone debt payoff projections, explains calculation blockers with machine-readable codes, and integrates seamlessly into the Financial Position (supplying `Mandatory Payment` to `CashFlowCalculator`).

---

## 2. User Scenarios & Acceptance Criteria

### Functional Requirements (traceability keys)

Tasks (`tasks.md §1`) trace to these requirement keys:

| Requirement Key | Description | User Stories | Acceptance Scenarios |
|---|---|---|---|
| FR-001 | Record and maintain owner-scoped debt facts with lifecycle (ACTIVE / PAID_OFF / ARCHIVED) | US1 | SC1.1 – SC1.5 |
| FR-002 | Portfolio aggregation and deterministic DTI ratio | US2 | SC2.1 – SC2.4 |
| FR-003 | Deterministic per-debt payoff projection and debt-free date | US3 | SC3.1 – SC3.3 |
| FR-004 | Explainable calculation blockers and portfolio blocker propagation | US4 | SC4.1 – SC4.5 |
| FR-005 | Financial Position / cash-flow integration, export section, and ownership isolation | US5 + §8 + §9 | SC5.1 – SC5.3 |

Success Criteria for end-to-end acceptance map to the same scenario IDs:
SC-001 = debt lifecycle (SC1.1 – SC1.5), SC-002 = summary/DTI/payoff (SC2.1 – SC3.3),
SC-003 = blockers + cash-flow integration (SC4.1 – SC5.3).

### User Story 1 - Record and Maintain Debt Facts (Priority: P1)
As an authenticated user, I want to record and maintain each of my debts with creditor, debt type, balances, interest rate, minimum payment, planned payment, and payment schedule, so that I have a single source of truth for my debt obligations.

**Why this priority**: Without accurate, owner-scoped debt facts, portfolio aggregations, DTI calculations, and Financial GPS cash flow cannot be determined.

**Independent Test**: Record debts across diverse types (credit card, mortgage, personal loan) with zero or positive interest; update balances; mark paid debts; soft-delete debts; verify portfolio totals match active records.

**Acceptance Scenarios**:
1. **SC1.1 - Record single debt**:
   - **Given** an authenticated user with no debts,
   - **When** the user records a debt with creditor "Techcombank", debt type `CREDIT_CARD`, original principal `20000000.00`, outstanding balance `15000000.00`, annual interest rate `0.180000`, minimum payment `1500000.00`, planned payment `3000000.00`, due day `15`,
   - **Then** the debt is saved with status `ACTIVE`, currency matching the user profile (default `VND`), and returned with an immutable server-assigned ID.
2. **SC1.2 - Validation: planned payment vs minimum payment**:
   - **Given** a debt creation or update request,
   - **When** `plannedPayment < minimumPayment`,
   - **Then** the system rejects the request with HTTP 400 (`VALIDATION_FAILED`), explaining that planned monthly payment cannot be less than the mandatory minimum payment.
3. **SC1.3 - Validation: positive balance requires positive minimum payment**:
   - **Given** an outstanding balance > 0,
   - **When** `minimumPayment <= 0` or `plannedPayment <= 0`,
   - **Then** the system rejects the request with HTTP 400 (`VALIDATION_FAILED`).
4. **SC1.4 - Debt Lifecycle: Zero Balance and Paid Off**:
   - **Given** an active debt,
   - **When** the user updates the outstanding balance to `0.00` (or marks it paid),
   - **Then** the status transitions to `PAID_OFF`, minimum payment and planned payment become `0.00`, and this debt is excluded from outstanding debt totals and monthly payment burdens in subsequent summaries.
5. **SC1.5 - Soft Deletion via Archive**:
   - **Given** an existing debt belonging to the authenticated user,
   - **When** the user sends `DELETE /api/v1/debts/{id}`,
   - **Then** the debt is soft-deleted by transitioning its status to `ARCHIVED`. It is never hard-deleted from the database table. An archived debt is excluded from active debt lists, portfolio summaries, payoff projections, and cash flow calculations. Subsequent GET requests for an archived debt return HTTP 404 (`RESOURCE_NOT_FOUND`).

---

### User Story 2 - Portfolio Summary and Debt-To-Income (DTI) Ratio (Priority: P1)
As an authenticated user, I want to see my aggregate debt portfolio summary—including total outstanding balance, total minimum monthly payment, total planned monthly payment, and Debt-To-Income (DTI) ratio—so that I understand my financial leverage and fixed commitments.

**Why this priority**: Fixed debt payments represent mandatory commitments that directly constrain available financial capacity.

**Independent Test**: Combine multiple active debts with varying payments against a user financial profile with known income; verify portfolio totals, DTI, and remaining cash flow.

**Acceptance Scenarios**:
1. **SC2.1 - Aggregate Portfolio Totals**:
   - **Given** 2 active debts (Debt A: balance `10000000.00`, min `1000000.00`, planned `2000000.00`; Debt B: balance `20000000.00`, min `2000000.00`, planned `3000000.00`), 1 paid-off debt (balance `0.00`), and 1 archived debt,
   - **When** the user views `GET /api/v1/debts/summary`,
   - **Then** `totalOutstandingDebt` is `30000000.00`, `totalMinimumMonthlyPayment` is `3000000.00`, and `totalPlannedMonthlyPayment` is `5000000.00`.
2. **SC2.2 - Deterministic DTI Calculation**:
   - **Given** total minimum monthly payment `3000000.00` and total active monthly income from Financial Profile `30000000.00`,
   - **When** the debt summary is computed,
   - **Then** `debtToIncomeRatio` is `0.1000` (10.00%), calculated strictly as `totalMinimumMonthlyPayment / totalMonthlyIncome`.
3. **SC2.3 - DTI with Zero or Missing Income**:
   - **Given** total minimum monthly payment > 0 and monthly income `0.00` (or profile not yet created),
   - **When** the debt summary is computed,
   - **Then** `debtToIncomeRatio` is `null`, and `dtiStatus` is reported as `UNAVAILABLE` with reason code `ZERO_OR_MISSING_INCOME`.
4. **SC2.4 - DTI with Zero Debt**:
   - **Given** no active debts (total minimum monthly payment = `0.00`) and positive monthly income,
   - **When** the debt summary is computed,
   - **Then** `debtToIncomeRatio` is `0.0000` (0.00%) with `dtiStatus: AVAILABLE`.

---

### User Story 3 - Deterministic Payoff Projection & Debt-Free Date (Priority: P1)
As an authenticated user, I want to see deterministic payoff projections for each debt and for my entire portfolio, including projected debt-free dates, number of payments, and total interest payable, so that I can see the finish line of my debt freedom journey.

**Why this priority**: Core value of Financial GPS: current position + route + projected arrival date (ETA).

**Independent Test**: Simulate standard interest loans, zero-interest loans, and overpayment schedules; confirm step-by-step balance decline, final payment clamp, and exact payoff date matching mathematical oracle.

**Acceptance Scenarios**:
1. **SC3.1 - Standard Positive Interest Projection**:
   - **Given** a debt with balance `10000000.00`, annual rate `0.120000` (12%/yr), planned monthly payment `1000000.00`, evaluated as of `2026-10-01`,
   - **When** projection is computed,
   - **Then** projection status is `AVAILABLE`, payoff requires 11 payments, projected debt-free date is `2027-09-01`, total interest paid is `589848.78`, and the final payment is `589848.78` (clamped to remaining balance + accrued interest).
2. **SC3.2 - Zero-Interest Loan Projection**:
   - **Given** a debt with balance `12000000.00`, annual rate `0.000000` (0%), planned monthly payment `1000000.00`, evaluated as of `2026-10-01`,
   - **When** projection is computed,
   - **Then** projection status is `AVAILABLE`, balance declines linearly by `1000000.00` each month, payoff requires exactly 12 payments, total interest is `0.00`, and debt-free date is `2027-10-01`.
3. **SC3.3 - Portfolio Overall Debt-Free Date**:
   - **Given** multiple active debts with valid individual projections,
   - **When** portfolio projection is evaluated,
   - **Then** the portfolio `projectedDebtFreeDate` equals `max(projectedDebtFreeDate of all active debts)`, representing when the user is completely debt-free under standalone planned payments.

---

### User Story 4 - Explainable Calculation Blockers & Portfolio Blocker Propagation (Priority: P2)
As an authenticated user, when one or more of my debts cannot be projected to payoff, I want the system to clearly state that the projection is `BLOCKED`, provide a machine-readable reason code, give a clear human-readable explanation and next action, and propagate the blocker to the portfolio projection, rather than inventing an impossible or misleading payoff date.

**Why this priority**: Constitution Principle I (Financial Truth First) and Principle III (Explainable GPS) forbid fabricating dates or concealing insolvencies.

**Independent Test**: Enter debts with payment < monthly interest, payment == monthly interest, or missing interest rate; verify individual and portfolio projections return `BLOCKED` with accurate codes.

**Acceptance Scenarios**:
1. **SC4.1 - Insufficient Payment (`payment < interest`)**:
   - **Given** a debt with balance `10000000.00`, annual rate `0.120000` (monthly interest = `100000.00`), and planned payment `80000.00`,
   - **When** projection is computed,
   - **Then** debt projection status is `BLOCKED`, `reasonCode` is `PAYMENT_DOES_NOT_COVER_INTEREST`, explanation states "Monthly planned payment (80,000 VND) is less than monthly accrued interest (100,000 VND). Balance will increase over time.", and portfolio projection status becomes `BLOCKED`.
2. **SC4.2 - Perpetual Interest Payment (`payment == interest`)**:
   - **Given** a debt with balance `10000000.00`, annual rate `0.120000` (monthly interest = `100000.00`), and planned payment `100000.00`,
   - **When** projection is computed,
   - **Then** debt projection status is `BLOCKED`, `reasonCode` is `PAYMENT_COVERS_ONLY_INTEREST`, explanation states "Planned payment only covers monthly interest; principal balance will never decrease.", and portfolio projection status becomes `BLOCKED`.
3. **SC4.3 - Missing Interest Rate**:
   - **Given** a debt entered without a known annual interest rate (`annualInterestRate = null`),
   - **When** projection is computed,
   - **Then** the system does NOT silently assume 0%, debt projection status is `BLOCKED`, `reasonCode` is `INTEREST_RATE_MISSING`, and the explanation prompts the user to enter the contractual interest rate.
4. **SC4.4 - Computational Projection Safety Limit (360 Months)**:
   - **Given** an amortizing debt where payoff would mathematically require > 360 monthly periods (30 years) under the current nominal payment,
   - **When** projection is computed,
   - **Then** debt projection status is `BLOCKED` with `reasonCode: PAYOFF_HORIZON_EXCEEDS_MAXIMUM`. This is explicitly a computational projection safety limit to guard the projection simulation engine, NOT a business rule invalidating long-term loans.
5. **SC4.5 - Portfolio Blocker Propagation**:
   - **Given** a portfolio of active debts where at least one debt has projection status `BLOCKED`,
   - **When** portfolio projection is evaluated,
   - **Then** the portfolio projection status MUST be `BLOCKED`, `projectedDebtFreeDate` MUST be `null`, `totalMonthsRemaining` MUST be `null`, `totalInterestRemaining` MUST be `null`, `reasonCode` is `PORTFOLIO_CONTAINS_BLOCKED_DEBTS`, and the response enumerates the specific blocked debts and their respective reason codes.

---

### User Story 5 - Financial Position & Cash Flow Integration (Priority: P1)
As an authenticated user, I want my active debt payments to automatically update my Financial Position, so that my mandatory debt burden reduces my available cash flow without requiring manual synchronization.

**Why this priority**: Seamless integration between Feature 001 (Financial Profile) and Feature 002 (Debt Management) preserves financial truth across Financial GPS.

**Independent Test**: Record active debts, inspect `CashFlowResult` and `FinancialResult`; confirm `Mandatory Payment` equals `totalMinimumMonthlyPayment`, `Net Cash Flow = Income - Expense - Mandatory Payment`, and `Available Capacity = max(Net Cash Flow, 0)`.

**Acceptance Scenarios**:
1. **SC5.1 - Mandatory Payment Feeds Financial Position**:
   - **Given** active income `74000000.00`, fixed+variable expenses `30000000.00`, and active debts with total minimum payment `20000000.00`,
   - **When** Financial Position is computed,
   - **Then** `Mandatory Payment` is `20000000.00`, `Net Cash Flow` is `24000000.00` (`74M - 30M - 20M`), and `Available Capacity` is `24000000.00`.
2. **SC5.2 - Negative Net Cash Flow Reported Honestly**:
   - **Given** active income `30000000.00`, expenses `20000000.00`, and total minimum debt payment `15000000.00`,
   - **When** Financial Position is computed,
   - **Then** `Net Cash Flow` is `-5000000.00` (honestly reported as negative), and `Available Capacity` is clamped to `0.00`.
3. **SC5.3 - Planned Payment Surplus as Optional Capacity Allocation**:
   - **Given** planned debt payment exceeds minimum debt payment (`totalPlannedMonthlyPayment > totalMinimumMonthlyPayment`),
   - **Then** only `totalMinimumMonthlyPayment` is treated as non-negotiable `Mandatory Payment` in baseline cash flow; the surplus (`plannedPayment - minimumPayment`) is categorized as an active planned allocation in the user's debt summary and roadmap route.

---

## 3. Explicit Non-Goals (Scope Boundaries)

To maintain high quality, strict focus, and zero feature creep, Feature 002 explicitly excludes:
- **Debt payoff strategy optimization**: No automatic comparison or switching between Debt Avalanche (highest interest rate first) and Debt Snowball (lowest balance first). (Planned for Feature 005 - Roadmap).
- **Refinancing / consolidation calculations**: No modeling of replacement loans, balance transfers, or origination fees.
- **Bank / Open Banking integration**: No automatic balance syncing via banking APIs or web scrapers.
- **Transaction-by-transaction payment ledger**: No manual ledger recording daily transactions; debt balance is updated by user input or period reconciliation.
- **What-if scenario mutations**: No scenario simulation that modifies real debt records (Scenario Planning belongs to Feature 006).
- **AI-generated financial advice**: No generative AI prescribing debt settlement or non-deterministic credit counseling (Constitution Principle II & VII).

---

## 4. Domain Model & Semantics

### 4.1 Debt Aggregate & Attributes

An entity representing a legal or contractual monetary debt obligation owed by an authenticated `OwnerId`.

| Attribute | Type | Nullable | Validation / Domain Rules | Description |
|---|---|---|---|---|
| `id` | `UUID` | No | Immutable; server-generated | Unique identifier of the debt |
| `ownerId` | `OwnerId` (UUID) | No | Scoped to authenticated user session | Data boundary owner; never client-supplied |
| `creditor` | `String` | No | Non-blank; max 120 chars | Name of lender, bank, or individual (e.g. "Techcombank", "Vietcombank") |
| `debtType` | `DebtType` (Enum) | No | `CREDIT_CARD`, `MORTGAGE`, `AUTO_LOAN`, `STUDENT_LOAN`, `PERSONAL_LOAN`, `OTHER` | Classification of the debt obligation |
| `originalPrincipal` | `Money` | Yes | If present: `>= 0`, same currency as profile | Initial principal borrowed at loan origination |
| `outstandingBalance` | `Money` | No | `>= 0`; scale 2 | Current remaining balance. If `0.00`, status MUST be `PAID_OFF` |
| `annualInterestRate` | `Rate` | Yes | If present: `0 <= rate <= 1.000000` (scale 6). Null = missing assumption | Contractual annual nominal interest rate (e.g. `0.120000` = 12.00%/yr) |
| `minimumPayment` | `Money` | No | If balance > 0: `> 0`; else `= 0.00`. Scale 2 | Non-negotiable mandatory monthly payment due to creditor |
| `plannedPayment` | `Money` | No | Must satisfy: `plannedPayment >= minimumPayment`. Scale 2 | Amount the user actually plans to pay each month |
| `paymentFrequency` | `PaymentFrequency` (Enum) | No | Constrained to `MONTHLY` for MVP | Payment frequency; aligned with financial-domain engine contract |
| `dueDay` | `Integer` | Yes | `1 <= dueDay <= 31` | Day of the month when payment is due |
| `status` | `DebtStatus` (Enum) | No | `ACTIVE`, `PAID_OFF`, `ARCHIVED` | Lifecycle state |
| `createdAt` | `Instant` | No | Immutable timestamp | Timestamp of creation |
| `updatedAt` | `Instant` | No | Updated on mutation | Timestamp of last modification |

### 4.2 Distinguishing Minimum Payment vs Planned Payment

This distinction is mandatory and non-negotiable:
- **`minimumPayment` (Mandatory Monthly Obligation)**:
  - Contractual requirement imposed by the creditor (e.g. 5% of credit card balance, or fixed mortgage installment).
  - Used as the **numerator in Debt-To-Income (DTI)** ratio.
  - Used as the **`Mandatory Payment` in Financial Position and `CashFlowCalculator`** (`NetCashFlow = Income - Expense - MandatoryPayment`).
  - Represents the legal baseline: failing to pay this triggers penalties and credit impairment.
- **`plannedPayment` (Actual Intended Amortization Payment)**:
  - User's deliberate monthly commitment toward paying off this specific debt.
  - Must satisfy invariant: `plannedPayment >= minimumPayment`.
  - Used as the **actual periodic payment `P` in Payoff Projection & Amortization Calculations**.
  - Any surplus (`plannedPayment - minimumPayment`) represents voluntary acceleration funded from `Available Capacity`.

### 4.3 Debt Lifecycle States & Soft Deletion

```text
[ Created: balance > 0 ]  ───►  ACTIVE
                                  │
    ┌─────────────────────────────┼─────────────────────────────┐
    ▼                             ▼                             ▼
[ Balance updated to 0.00 ]  [ User explicit Pay-Off ]   [ User DELETE /debts/{id} ]
    │                             │                             │
    └──────────────► PAID_OFF ◄───┘                             ▼
                        │                                   ARCHIVED
                        ▼                               (soft-deleted,
              (retained in history,                      excluded from
               excluded from totals)                      all queries)
```

1. **`ACTIVE`**:
   - Debt has `outstandingBalance > 0` and is being serviced.
   - Contributes to `totalOutstandingDebt`, `totalMinimumMonthlyPayment`, `totalPlannedMonthlyPayment`, DTI, and Financial Position `Mandatory Payment`.
2. **`PAID_OFF`**:
   - `outstandingBalance == 0.00`.
   - `minimumPayment` and `plannedPayment` are set to `0.00`.
   - Does NOT contribute to outstanding balance, mandatory payments, or DTI.
   - Displayed under "Paid-off debts" history in UI for celebration and audit trail.
3. **`ARCHIVED` (Soft-Deleted)**:
   - When a user deletes a debt via `DELETE /api/v1/debts/{id}`, the system performs a **soft-delete** by setting `status = ARCHIVED` and updating `updatedAt`.
   - The row is **never hard-deleted** from PostgreSQL during normal user deletion.
   - Excluded from active debt lists, portfolio summaries, payoff projections, and cash flow calculations.
   - Subsequent `GET /api/v1/debts/{id}`, `PUT /api/v1/debts/{id}`, or `DELETE /api/v1/debts/{id}` for an archived debt return HTTP 404 (`RESOURCE_NOT_FOUND`).
   - Hard deletion occurs exclusively via cascading delete when the parent `Account` is permanently deleted (`ON DELETE CASCADE`).

---

## 5. Mathematical Specification: Amortization & Projection

Following normative contract `specs/financial-domain/calculation-rules.md` §4 (`MONTHLY_SIMPLE_AMORTIZATION`).

### 5.1 Calculation Policy (Explicit Policy Object)

All amortization and payoff projections execute against an explicit `DebtCalculationPolicy`:
- **`paymentFrequency`**: Locked to `MONTHLY`.
- **`maxSimulationMonths`**: Default `360` (30 years). Serves strictly as a computational safety guardrail to terminate the simulation and prevent runaway CPU cycles, NOT a business rule invalidating long-term loans.
- **`monetaryScale`**: `2` decimal places.
- **`monetaryRounding`**: `RoundingMode.HALF_UP` applied at declared period boundaries.
- **`rateScale`**: `6` decimal places.
- **`asOfDate`**: The reference anchor date for projection (e.g. `2026-10-01`).

### 5.2 Deterministic Step-by-Step Amortization Algorithm

Let:
- $B_0$: Opening outstanding balance (`outstandingBalance`)
- $r$: Annual nominal interest rate (`annualInterestRate`)
- $P$: Planned monthly payment (`plannedPayment`)
- $m$: Period index ($m = 1, 2, \dots$)

For each month $m$:
1. **Periodic Monthly Interest**:
   $$I_m = \text{round}\left(B_{m-1} \times \frac{r}{12}, \text{scale} = 2, \text{HALF\_UP}\right)$$
   *(If $r = 0$ or $r$ is null, $I_m = 0.00$)*
2. **Principal Reduction Before Clamp**:
   $$P_{\text{principal}, m} = P - I_m$$
3. **Closing Balance Before Clamp**:
   $$B_m^* = B_{m-1} - P_{\text{principal}, m} = B_{m-1} + I_m - P$$
4. **Final Payment Clamp & Balance Termination**:
   - If $B_m^* \le 0$:
     - Payoff occurs in period $m$.
     - Number of payments: $N = m$.
     - Final payment: $P_{\text{final}} = B_{m-1} + I_m$ (the user pays only the remaining balance plus final interest).
     - Total interest paid: $\sum_{k=1}^m I_k$.
     - Closing balance: $B_m = 0.00$.
     - Calculation terminates.
   - If $B_m^* > 0$:
     - Closing balance: $B_m = B_m^*$.
     - Advance to month $m + 1$.

### 5.3 Blocker Semantics & Non-Finite Projections

Before simulation loop execution, the engine evaluates blocker conditions:

| Condition | Projection Status | `reasonCode` | Explanation |
|---|---|---|---|
| `annualInterestRate == null` | `BLOCKED` | `INTEREST_RATE_MISSING` | "Annual interest rate is missing. Cannot calculate payoff without interest rate." |
| `plannedPayment < round(B_0 * r / 12, 2, HALF_UP)` | `BLOCKED` | `PAYMENT_DOES_NOT_COVER_INTEREST` | "Monthly planned payment does not cover monthly accrued interest. Balance will grow." |
| `plannedPayment == round(B_0 * r / 12, 2, HALF_UP)` | `BLOCKED` | `PAYMENT_COVERS_ONLY_INTEREST` | "Monthly planned payment only covers interest. Balance will never decrease." |
| `m > maxSimulationMonths (360)` | `BLOCKED` | `PAYOFF_HORIZON_EXCEEDS_MAXIMUM` | "Payoff horizon exceeds computational safety limit (360 months / 30 years)." |
| `outstandingBalance == 0` | `COMPLETED` | `DEBT_ALREADY_PAID` | "Debt is already fully paid." |

When individual debt status is `BLOCKED`:
- `payoffDate`: `null`
- `numberOfPayments`: `null`
- `totalInterest`: `null`
- `finalPayment`: `null`
- Response contains `reasonCode` and `explanation`.

### 5.4 Portfolio Payoff Date & Blocker Propagation

Given projection evaluated as of $D_{\text{asOf}}$ (e.g. `2026-10-01`) and finite number of payments $N$:
$$\text{PayoffDate} = D_{\text{asOf}} \text{ plus } N \text{ months}$$
*(Example: evaluated as of 2026-10-01 with $N = 1$ payment $\rightarrow$ payoff on 2026-11-01; with $N = 23$ payments $\rightarrow$ payoff on 2028-09-01)*.

**Portfolio Payoff Blocker Rule**:
- If **ALL** active debts have projection status `AVAILABLE` or `COMPLETED`:
  $$\text{PortfolioPayoffDate} = \max_{d \in \text{ActiveDebts}}(\text{PayoffDate}_d)$$
  $$\text{TotalMonthsRemaining} = \max_{d \in \text{ActiveDebts}}(\text{numberOfPayments}_d)$$
  $$\text{TotalInterestRemaining} = \sum_{d \in \text{ActiveDebts}} \text{totalInterest}_d$$
  Portfolio status is `AVAILABLE` (or `COMPLETED` if no active debt remains).
- If **ANY** active debt has projection status `BLOCKED`:
  - Portfolio projection status MUST BE `BLOCKED`.
  - `projectedDebtFreeDate` MUST BE `null`.
  - `totalMonthsRemaining` MUST BE `null`.
  - `totalInterestRemaining` MUST BE `null`.
  - `reasonCode` = `PORTFOLIO_CONTAINS_BLOCKED_DEBTS`.
  - The response enumerates all blocked debts along with their specific individual reason codes.

---

## 6. Debt-To-Income (DTI) Specification

### 6.1 Formula
$$\text{DTI} = \frac{\text{Total Minimum Monthly Debt Payment}}{\text{Total Active Monthly Income}}$$

### 6.2 Numerator and Denominator Binding
- **Numerator**: Sum of `minimumPayment` across all `ACTIVE` debts owned by the user.
  - *Why minimum payment*: DTI is a regulatory and risk metric reflecting non-negotiable contractual debt burden. Planned acceleration payments are discretionary.
- **Denominator**: Total active monthly income from `FinancialProfile` (effective as of `asOfDate`), retrieved from the `GetProfile` / `CashFlowCalculator` boundary.

### 6.3 Division, Rounding, and Edge Cases
- Stored as domain `Ratio` (`BigDecimal`, scale 4, `RoundingMode.HALF_UP`). Example: `0.3333` (33.33%).
- **Edge Cases**:
  - `Total Active Monthly Income == 0.00` (or no income recorded):
    - `debtToIncomeRatio`: `null`
    - `dtiStatus`: `UNAVAILABLE`
    - `dtiReasonCode`: `ZERO_OR_MISSING_INCOME`
  - `Total Minimum Monthly Debt Payment == 0.00` and Income > 0:
    - `debtToIncomeRatio`: `0.0000`
    - `dtiStatus`: `AVAILABLE`

---

## 7. Precision, Rounding, and Currency

To ensure deterministic results compliant with Constitution Principle II:
- **Monetary amounts**: `BigDecimal`, scale 2, `RoundingMode.HALF_UP` for display and period balances.
- **Interest rates**: `BigDecimal`, scale 6 (`0.120000` = 12.00%).
- **Ratios (DTI)**: `BigDecimal`, scale 4 (`0.1500` = 15.00%).
- **Wire format**: Canonical plain decimal strings (e.g. `"15000000.00"`). Floats and JavaScript numbers are prohibited on the wire and in calculation.
- **Single currency MVP**: All debts inherit or match the user profile currency (default `VND`). Cross-currency conversion is out of scope for MVP.

---

## 8. Debt Summary & Financial Position Integration

### 8.1 Debt Summary Contract (`GET /api/v1/debts/summary`)

```json
{
  "totalOutstandingDebt": "30000000.00",
  "totalMinimumMonthlyPayment": "3000000.00",
  "totalPlannedMonthlyPayment": "5000000.00",
  "currency": "VND",
  "debtToIncome": {
    "status": "AVAILABLE",
    "ratio": "0.1000",
    "reasonCode": null,
    "explanation": "Total minimum monthly debt (3,000,000.00 VND) divided by monthly income (30,000,000.00 VND)"
  },
  "portfolioProjection": {
    "status": "AVAILABLE",
    "projectedDebtFreeDate": "2027-09-01",
    "totalMonthsRemaining": 11,
    "totalInterestRemaining": "589848.78",
    "reasonCode": null,
    "explanation": "All active debts will be paid off by 2027-09-01 under planned payments."
  },
  "blockedDebtCount": 0,
  "asOf": "2026-10-01"
}
```

### 8.2 Integration into Financial Position (`specs/001-financial-profile`)

In Feature 001, `FinancialEngine.java` and `CashFlowCalculator.java` calculate current position:
- `Income`: sum of active incomes effective on asOf.
- `Expense`: sum of active expenses effective on asOf.
- `Mandatory Payment`: **Sum of `minimumPayment` of active debts effective on asOf**. (In Feature 001, this was hardcoded to `0.00` pending 002).
- `Net Cash Flow`: `Income - Expense - Mandatory Payment`.
- `Available Capacity`: `max(Net Cash Flow, 0.00)`.

**Contract Enforcement**:
Feature 002 populates `FinancialInput.debts()` with domain `Debt` models. `CashFlowCalculator` computes `Mandatory Payment` directly from active debts. No duplicate cash flow calculator is created.

---

## 9. Security & Account Ownership Boundary

1. **Owner Scoping**:
   - Every debt row in the database has `owner_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE`.
   - All repository queries filter by authenticated `OwnerId`:
     - `findAllByOwnerId(ownerId)`
     - `findByIdAndOwnerId(id, ownerId)`
      - archiveByIdAndOwner(id, ownerId): soft-delete to ARCHIVED
2. **Session / Principal Resolution**:
   - Resolved strictly on the server via `CurrentOwnerProvider.requireCurrentOwner()`.
   - The API DTOs NEVER accept an `ownerId` or `accountId` in the request body or path.
3. **Cross-Owner Isolation**:
   - If User A attempts to access or mutate User B's debt ID:
     - The server returns HTTP 404 (`RESOURCE_NOT_FOUND`) with message "Debt not found".
     - HTTP 403 is avoided to prevent resource enumeration.
4. **Data Export & Zero-Orphan Cascade**:
   - Debt persistence implements `OwnerDataSection` registering section name `"debts"` into `ExportOwnerDataUseCase`.
   - Deleting an `Account` cascades automatically via PostgreSQL foreign key constraint to hard-delete all associated debts.

---

## 10. REST API Specification

All endpoints are protected by session authentication and require CSRF token for mutating verbs (`POST`, `PUT`, `DELETE`).

### 10.1 Endpoints

| Method | Endpoint | Description | Success Status | Errors |
|---|---|---|---|---|
| `GET` | `/api/v1/debts` | List all active and paid debts for the current owner (excludes ARCHIVED) | `200 OK` | `401 AUTH_REQUIRED` |
| `POST` | `/api/v1/debts` | Create a new debt | `201 CREATED` | `400 VALIDATION_FAILED`, `401 AUTH_REQUIRED` |
| `GET` | `/api/v1/debts/{id}` | Get single debt details including individual payoff projection | `200 OK` | `401 AUTH_REQUIRED`, `404 RESOURCE_NOT_FOUND` |
| `PUT` | `/api/v1/debts/{id}` | Update debt details | `200 OK` | `400 VALIDATION_FAILED`, `401 AUTH_REQUIRED`, `404 RESOURCE_NOT_FOUND` |
| `DELETE` | `/api/v1/debts/{id}` | Soft-delete / archive a debt (sets status to ARCHIVED) | `204 NO_CONTENT` | `401 AUTH_REQUIRED`, `404 RESOURCE_NOT_FOUND` |
| `GET` | `/api/v1/debts/summary` | Get portfolio summary, DTI, and portfolio payoff projection | `200 OK` | `401 AUTH_REQUIRED` |

### 10.2 Create / Update Debt Request Body (`DebtRequest`)

```json
{
  "creditor": "Techcombank",
  "debtType": "CREDIT_CARD",
  "originalPrincipal": "20000000.00",
  "outstandingBalance": "15000000.00",
  "annualInterestRate": "0.180000",
  "minimumPayment": "1500000.00",
  "plannedPayment": "3000000.00",
  "dueDay": 15
}
```

### 10.3 Single Debt View (`DebtView`)

> `currency` is **derived** from the owner's Financial Profile (single-currency MVP,
> default `VND`); it is not a persisted column on the `debt` table (`plan.md §2.3`).
> Cross-currency conversion is out of scope.

```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "creditor": "Techcombank",
  "debtType": "CREDIT_CARD",
  "originalPrincipal": "20000000.00",
  "outstandingBalance": "15000000.00",
  "annualInterestRate": "0.180000",
  "minimumPayment": "1500000.00",
  "plannedPayment": "3000000.00",
  "dueDay": 15,
  "status": "ACTIVE",
  "currency": "VND",
  "projection": {
    "status": "AVAILABLE",
    "projectedPayoffDate": "2027-04-01",
    "numberOfPayments": 6,
    "totalInterest": "712996.18",
    "finalPayment": "712996.18",
    "reasonCode": null,
    "explanation": null
  }
}
```

> Illustrative example only (not part of the §11 oracle): 6 monthly payments of
> `3000000.00` contribute `18000000.00` total, of which `15000000.00` retires principal
> and `712996.18` is interest; the remainder of the 6th scheduled payment is unneeded,
> so `finalPayment (712996.18) = 702459.29 + 10536.89` (remaining balance plus final-month interest).
> Exact oracle vectors live in §11.1 (`REF-D01` – `REF-D09`).

---

## 11. Deterministic Reference Matrix (TDD Test Oracle)

This deterministic test matrix MUST be implemented in domain calculator tests (`DebtPayoffCalculatorTest`, `DebtSummaryCalculatorTest`) before application services or controllers are developed.

### Table 11.1: Single Debt Payoff Scenarios (Evaluated as of `2026-10-01`)

| Case ID | Balance ($B_0$) | Annual Rate ($r$) | Planned Payment ($P$) | Expected Status | Reason Code | Expected Payments ($N$) | Expected Payoff Date | Expected Final Payment | Expected Total Interest | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| **REF-D01** | `1000.00` | `0.120000` | `50.00` | `AVAILABLE` | `null` | 23 | `2028-09-01` | `21.36` | `121.36` | Standard positive interest amortization |
| **REF-D02** | `1000.00` | `0.000000` | `100.00` | `AVAILABLE` | `null` | 10 | `2027-08-01` | `100.00` | `0.00` | Zero interest, exact divisor |
| **REF-D03** | `1000.00` | `0.000000` | `300.00` | `AVAILABLE` | `null` | 4 | `2027-02-01` | `100.00` | `0.00` | Zero interest, final payment clamp (`100.00`) |
| **REF-D04** | `50.00` | `0.120000` | `100.00` | `AVAILABLE` | `null` | 1 | `2026-11-01` | `50.50` | `0.50` | $P > B_0 + I_1$; final payment = `50.50` |
| **REF-D05** | `1000.00` | `0.120000` | `8.00` | `BLOCKED` | `PAYMENT_DOES_NOT_COVER_INTEREST` | `null` | `null` | `null` | `null` | $I_1 = 10.00 > P (8.00)$ |
| **REF-D06** | `1000.00` | `0.120000` | `10.00` | `BLOCKED` | `PAYMENT_COVERS_ONLY_INTEREST` | `null` | `null` | `null` | `null` | $I_1 = 10.00 == P (10.00)$ |
| **REF-D07** | `1000.00` | `null` | `50.00` | `BLOCKED` | `INTEREST_RATE_MISSING` | `null` | `null` | `null` | `null` | Missing rate never silently assumes 0% |
| **REF-D08** | `0.00` | `0.120000` | `0.00` | `COMPLETED` | `DEBT_ALREADY_PAID` | 0 | `2026-10-01` | `0.00` | `0.00` | Zero balance is already paid off |
| **REF-D09** | `10000000.00` | `0.180000` | `150000.00` | `BLOCKED` | `PAYMENT_COVERS_ONLY_INTEREST` | `null` | `null` | `null` | `null` | $10\text{M} \times 0.18 / 12 = 150000$ |

### Table 11.2: Portfolio Aggregation & DTI Scenarios (Evaluated as of `2026-10-01`)

| Case ID | Debt Portfolio Inputs | Profile Monthly Income | Expected Total Debt | Expected Total Min Payment | Expected Total Planned | Expected DTI Ratio | Expected DTI Status | Expected Portfolio Payoff Date | Expected Portfolio Status | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| **REF-P01** | Debt 1: `1000` @ 12%, min 50, plan 100 ($N=11$, payoff `2027-09-01`)<br>Debt 2: `2000` @ 0%, min 200, plan 200 ($N=10$, payoff `2027-08-01`) | `10000.00` | `3000.00` | `250.00` | `300.00` | `0.0250` (2.50%) | `AVAILABLE` | `2027-09-01` | `AVAILABLE` | Both debts amortize; max date is `2027-09-01` |
| **REF-P02** | Debt 1: `5000` @ 12%, min 40, plan 40 ($I_1=50 > P \rightarrow \text{Blocked}$)<br>Debt 2: `1000` @ 0%, min 100, plan 100 | `5000.00` | `6000.00` | `140.00` | `140.00` | `0.0280` (2.80%) | `AVAILABLE` | `null` | `BLOCKED` | Debt 1 is blocked $\rightarrow$ portfolio payoff is blocked |
| **REF-P03** | No active debts (all paid or empty) | `20000.00` | `0.00` | `0.00` | `0.00` | `0.0000` (0.00%) | `AVAILABLE` | `2026-10-01` | `COMPLETED` | Clean state |
| **REF-P04** | Debt 1: `1000` @ 0%, min 100, plan 100 | `0.00` (or null profile) | `1000.00` | `100.00` | `100.00` | `null` | `UNAVAILABLE` | `2027-08-01` | `AVAILABLE` | DTI unavailable (`ZERO_OR_MISSING_INCOME`), payoff unaffected |

---

## 12. Domain Invariants

Every implementation of Feature 002 MUST satisfy these invariants:
1. **Non-negativity**: Monetary amounts (`originalPrincipal`, `outstandingBalance`, `minimumPayment`, `plannedPayment`) MUST NOT be negative ($x \ge 0$).
2. **Planned Payment Lower Bound**: For any active debt, `plannedPayment >= minimumPayment`.
3. **Positive Payment for Positive Balance**: If `outstandingBalance > 0`, `minimumPayment` and `plannedPayment` MUST be $> 0$.
4. **Paid Off Consistency**: A debt with `outstandingBalance == 0.00` MUST have status `PAID_OFF`, `minimumPayment == 0.00`, and `plannedPayment == 0.00`.
5. **Soft Deletion Only**: User deletion sets status to `ARCHIVED`; active queries never return archived records.
6. **No Negative Balance**: Amortization simulation MUST never produce a negative remaining balance ($B_m \ge 0$).
7. **No Phantom Payoff Date**: Payoff date is finite IF AND ONLY IF balance eventually reaches zero. If $P \le I$, projection status MUST be `BLOCKED`.
8. **Portfolio Blocker Propagation**: If any active debt projection is `BLOCKED`, the portfolio payoff projection MUST be `BLOCKED` with `projectedDebtFreeDate = null`.
9. **Owner Isolation**: An authenticated user CANNOT read, mutate, or delete debts belonging to another user.
10. **Financial Position Conservation**: `Mandatory Payment` in cash flow derivation MUST strictly equal the sum of active debts' `minimumPayment`.

---

## 13. Quality Gates & Acceptance Verification

1. **Deterministic Calculation**: All cases in Section 11 pass 100% identically across repeated test runs.
2. **Architecture Purity**: Domain model and calculators (`com.financialgps.domain.debt.*`) contain ZERO dependencies on Spring, Jakarta, JPA, or HTTP.
3. **Test Matrix**: Full coverage across Domain, Application, Repository, API WebMvc, and End-to-End browser scenarios.
4. **No Feature Bleed**: Avalanche/Snowball strategy optimization and banking integration are strictly kept out of scope.
