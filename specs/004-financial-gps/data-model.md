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
| status | `ACTIVE`, `PAID`, or `ARCHIVED`; zero outstanding balance requires `PAID` |

### Goal

| Field | Rules |
|-------|-------|
| profileId, name | Required |
| targetAmount, currentAmount | Required non-negative money for amount-based goals |
| targetDate | Optional; required capacity is calculated only when present |
| completionCondition | Required; initial amount-based condition is `currentAmount >= targetAmount` |
| priority, status | Required; status is `ACTIVE`, `COMPLETED`, or `ARCHIVED` |

### FinancialAssumption

| Field | Rules |
|-------|-------|
| profileId, name, value | Required; value is typed money/rate/date/text as applicable |
| appliesFrom, appliesTo | Optional validity period |
| source | Required: `USER_SUPPLIED` or `SYSTEM_DEFAULT` |

## Derived, Non-Authoritative Entities

### CurrentPosition

Monthly income, monthly expenses, available cash flow, debt total, monthly debt payment,
debt-to-income ratio, savings, and emergency fund. Each field links to input values or a
documented calculation rule.

### FinancialGpsResult

| Field | Description |
|-------|-------------|
| asOf, destination | Calculation date and selected goal or stage |
| inputSnapshot | Actual values and assumptions used by calculation |
| distance, progress | Remaining amount/condition and percentage where measurable |
| capacityComparison | Required versus projected monthly capacity |
| eta | Projected date or explicit unavailable reason |
| status | `ON_TRACK`, `AT_RISK`, `OFF_TRACK`, `BLOCKED`, or `COMPLETED` |
| blockers | Verifiable conditions slowing/preventing progress |
| nextActions | Measurable, non-prescriptive actions linked to blockers/rules |
| explanation | Rule evaluations that produced the fields above |

### Status Transitions

- `COMPLETED`: destination completion condition is satisfied.
- `BLOCKED`: required progress is impossible under current inputs, such as non-positive available
  cash flow or a debt balance that cannot decline under its payment.
- `ON_TRACK`: a dated goal's projected capacity meets or exceeds required capacity.
- `AT_RISK`: positive projected capacity is below required capacity but a finite ETA remains.
- `OFF_TRACK`: positive projected capacity exists but the target date is missed by more than three
  monthly contribution periods.

Every result includes the evaluated condition, inputs, and shortfall for its status.

Scenario overrides and comparison results belong to `006-scenario-planning`. That feature MUST use
this same deterministic GPS result contract and apply its changes in memory without modifying the
persisted input entities defined here.
