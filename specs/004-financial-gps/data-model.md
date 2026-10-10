# Data Model: Financial GPS

## Common Value Rules

- `Money`: non-negative `BigDecimal` amount, ISO 4217 currency, central scale of 2 decimal places
  for persisted VND values; all division declares rounding mode.
- `Rate`: non-negative decimal fraction no greater than 1 unless a documented product rule permits
  a higher value.
- `asOf`: explicit calendar date used to make every projection reproducible.
- All persisted entities use UUID identity and `createdAt`/`updatedAt` timestamps.

## Persisted Input Entities

### FinancialProfile

| Field | Rules |
|-------|-------|
| id, ownerId | UUID; one active profile per owner in the initial release |
| currency | Required ISO 4217 code; initial UI supports VND |
| emergencyFundAmount, savingsAmount | Required non-negative money |
| dependentsCount | Required non-negative integer |

### Income and Expense

| Field | Rules |
|-------|-------|
| profileId | Required parent profile |
| amount | Required non-negative monthly money |
| source/category | Required descriptive label |
| expenseType | `FIXED` or `VARIABLE` for expenses |
| active | Inactive items are excluded from current calculation but retained as history |

### Debt

| Field | Rules |
|-------|-------|
| profileId, creditor | Required |
| originalPrincipal | Optional non-negative money |
| outstandingBalance | Required non-negative money |
| annualInterestRate | Required rate or explicitly labelled user assumption |
| monthlyPayment | Required non-negative money |
| dueDate, plannedPayoffDate | Optional dates |
| status | `ACTIVE`, `PAID_OFF`, or `ARCHIVED`; zero outstanding balance requires `PAID_OFF` |

### Goal

| Field | Rules |
|-------|-------|
| profileId, name | Required |
| targetAmount, currentAmount | Required non-negative money for amount-based goals; optional advisory context for a `DEBT_FREEDOM` goal |
| targetDate | Optional; required capacity is calculated only when present |
| completionCondition | Required; `AMOUNT_REACHED` (`currentAmount >= targetAmount`) for every goal type except `DEBT_FREEDOM`. A `DEBT_FREEDOM` goal uses `DEBT_FREE`: it is `COMPLETED` iff the Feature 002 portfolio is `COMPLETED` (decision D-6, `spec.md` §16), independent of amounts |
| priority, status | Required; status is `ACTIVE`, `COMPLETED`, or `ARCHIVED` (owned by Feature 003; Feature 004 never writes it) |

### FinancialAssumption

| Field | Rules |
|-------|-------|
| profileId, name, value | Required; value is typed money/rate/date/text as applicable |
| appliesFrom, appliesTo | Optional validity period |
| source | Required: `USER_SUPPLIED` or `SYSTEM_DEFAULT` |

## Derived, Non-Authoritative Entities

### CurrentPosition

Monthly Income, monthly Expense, Available Capacity, total outstanding debt, Mandatory Payment,
debt-to-income ratio, savings, and emergency fund. Each field links to input values or a
documented calculation rule.

### FinancialGpsResult

| Field | Description |
|-------|-------------|
| asOf, destination | Calculation date and the selected goal (amount-based, or the debt portfolio for a debt-freedom goal; roadmap stages are deferred, `spec.md` §2.2) |
| currentPosition | Income, Expense, Mandatory Payment, Net Cash Flow, Available Capacity, savings, emergency fund, dependents, debt total, DTI — each with provenance and an explicit availability state (`UNAVAILABLE` when its input is absent, never a fabricated `0`) |
| inputSnapshot | Actual values and assumptions used by calculation |
| distance, progress | Remaining amount/condition and percentage where measurable (debt-freedom progress is `null`, reason `PROGRESS_NOT_MEASURABLE`) |
| capacityComparison | Required versus projected monthly capacity for a dated amount-based goal; `null` (not applicable) for undated and for every debt-freedom goal (`spec.md` §7.2) |
| eta | Projected date + period count, or explicit unavailable reason |
| status | `ON_TRACK`, `AT_RISK`, `OFF_TRACK`, `BLOCKED`, or `COMPLETED` |
| blockers | Verifiable conditions slowing/preventing progress |
| nextActions | Measurable, non-prescriptive actions linked to blockers/rules |
| explanations | Rule evaluations that produced the fields above |
| missingInputs | Required inputs that were absent and affected the result (`spec.md` §11.3) |
| provenance | `actual` / `assumed` / `calculated` (with source) for each value |

### Status Transitions

Definitions follow `status-rules.md` (one deterministic lateness band set). With
`lateness = max(etaPeriods − monthsRemaining, 0)` for a dated destination:

- `COMPLETED`: the destination's route has arrived (amount `remaining = 0`; debt-freedom 002
  portfolio `COMPLETED`). This is the **GPS route status**; for a debt-freedom goal Feature 003
  derives the goal lifecycle from the same 002 portfolio status (decision D-6, `spec.md` §16), and
  Feature 004 never writes that status.
- `BLOCKED`: required progress is impossible under current inputs, such as non-positive available
  cash flow, a debt balance that cannot decline under its payment, or a missing required input.
- `ON_TRACK`: a finite route with `lateness = 0` — a dated goal's ETA is at or before the target
  date (equivalently projected capacity meets or exceeds required capacity); an undated goal with a
  finite route is also `ON_TRACK`. Being late within tolerance is `AT_RISK`, never `ON_TRACK`.
- `AT_RISK`: a finite route with `1 <= lateness <= latenessTolerance` (target missed within the
  configurable tolerance; `status-rules.md`, default 3 contribution periods).
- `OFF_TRACK`: a finite route with `lateness > latenessTolerance` (target missed beyond tolerance).
  The tolerance is a documented policy value, not a hardcoded month count (`status-rules.md`).

Every result includes the evaluated condition, inputs, and shortfall for its status.

Scenario overrides and comparison results belong to `006-scenario-planning`. That feature MUST use
this same deterministic GPS result contract and apply its changes in memory without modifying the
persisted input entities defined here.
