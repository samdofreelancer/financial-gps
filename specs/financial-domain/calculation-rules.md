# Financial Domain: Calculation Rules

> **Status**: Normative domain contract — the single source of truth for deterministic
> financial calculation. Every feature (`001`–`009`) and every acceptance test MUST satisfy
> these rules; they are NOT advisory. This is not a feature itself; it is the financial engine
> contract that the GPS, Roadmap, Scenario, Timeline, and Allocation features execute against.

## Purpose

The review found that the specs defined *what* a Financial GPS must do but not *how* the engine
calculates. This file fixes the exact arithmetic, projection, and routing rules so the engine is
deterministic and testable. It is technology-agnostic at the rule level; the plan layer chooses
the implementation (e.g., `BigDecimal`/`numeric`).

## 0. Canonical Terminology (MUST be used verbatim)

These terms are **fixed**; no feature, plan, or test MAY introduce a near-synonym field. If you
find yourself adding an "available surplus", "disposable cash", "remaining capacity", or
"extra margin", you are duplicating one of the terms below and MUST reuse it instead.

| Term | Definition | Do NOT call it |
|---|---|---|
| **Income** | Total active monthly money in (sum of recurring incomes effective on `asOf`). | revenue, inflow |
| **Expense** | Total active monthly money out (sum of recurring expenses effective on `asOf`). | spending, outflow |
| **Mandatory Payment** | A payment that is already committed before discretionary decisions: the required monthly debt payment. | committed payment, obligation |
| **Net Cash Flow** | `Income − Expense − MandatoryPayment`. May be negative; a negative value is reported, never concealed. | disposable, cash surplus |
| **Available Capacity** | `AvailableCapacity = max(NetCashFlow, 0)` — the portion of NetCashFlow available for discretionary **allocation** in a period. For v1 no borrow/drawdown is allowed, so a negative NetCashFlow yields zero allocation. See §7. | free cash, available money, leftover |
| **Allocation** | A priority-ordered rule that routes a slice of **Available Capacity** to a **destination** (debt, emergency fund, goal). Allocation is the *decision* of where money goes. | distribution, split, plan |
| **Contribution** | The money that **actually reaches** a goal or debt in a period after the allocation rule; a payment is the identical term for debts. Contribution is capped by `remaining` and must not exceed the goal target. | deposit, savings, pay-in |

The consent flow for a projection: `Income` and `Expense` are facts → `Mandatory Payment` is a
obligation → `Net Cash Flow` is fixed by the facts → `Allocation` chooses the destination →
`Contribution` is what arrives there. Use exactly these names in every DTO, entity, API field,
and explanation label.

## 1. Money, Precision, and Rounding

- Monetary amounts are decimal, never floating point, never JavaScript `number` for
  calculation.
- **Central scale**: 2 decimal places for persisted VND amounts. All division declares an
  explicit rounding mode; the defaults are `HALF_UP` for display and `CEILING` for any count that
  must not overstate that a full contribution occurred.
- A rate (interest, ratio) is a non-negative fraction (e.g., `0.12` = 12%/year), stored to 6
  decimal places.
- Rounding is applied at declared boundaries only; calculations keep full precision until then.

## 2. Determinism and the effective "as-of" Date

- Every projection is a pure function: `(inputs, assumptions, asOfDate, policy) -> GpsResult`.
- `asOfDate` is a **first-class domain concept**: the calendar date the projection is evaluated
  from. Recalculating the *same* inputs with the *same* `asOfDate` MUST yield an *identical*
  result.
- Changing only `asOfDate` MAY change an ETA, progress, or status; this is correct and MUST be
  explained (never silently cached or hidden).
- No calculation reads the system clock, network, database, randomness, or AI. The system clock
  only ever seeds `asOfDate` when the user does not choose one.

## 3. Income, Expense, and Net Cash Flow

Uses only the canonical terms from §0.

- **Income** = sum of active recurring incomes effective on `asOfDate`.
- **Expense** = sum of active recurring expenses effective on `asOfDate`.
- **Mandatory Payment** = sum of required monthly debt payments (see §4).
- **Net Cash Flow** = `Income − Expense − MandatoryPayment`. A negative value is reported, never
  concealed.
- **Available Capacity** = `max(NetCashFlow, 0)` — for v1 no borrow/drawdown is allowed, so a
  negative NetCashFlow yields zero discretionary allocation.
- **Allocation** (see §7) partitions **Available Capacity** into Contribution to debt/goals; this
  is what the GPS routes.

## 4. Debt Amortization

**MVP model**: `MONTHLY_SIMPLE_AMORTIZATION`. The engine models only one monthly simple
amortization per debt and does NOT model lender-specific daily accrual, penalties, fees,
payment-date rules, or daily compounding. The user supplies balance, rate, and payment; the
engine derives the payoff timeline.

Per debt, per monthly period, in order:

```
monthlyInterest   = round(outstandingBalance × annualRate / 12, scale = 2, HALF_UP)
principalPayment  = monthlyPaymentOrAllocation − monthlyInterest
newBalance        = openingBalance − principalPayment
```

- **Zero interest** (`rate = 0`): principal = full payment; balance declines linearly.
- **Final period**: the balance is cleared exactly; the final payment never exceeds the actual
  balance.
- **Payment > outstanding balance**: paid off that period; no negative balance is produced and no
  over-credit is granted.
- **Minimum-payment blocker**: if the monthly payment is less than the accrued monthly interest,
  the balance grows every period and **no finite payoff date exists**. The GPS reports `BLOCKED`,
  ETA `UNAVAILABLE`, reason `PAYMENT_DOES_NOT_COVER_INTEREST`, and a next action to raise the
  payment or renegotiate.

### Supported debt actions (per period or dated)

- **Regular payment** — configured monthly payment.
- **Extra payment / lump-sum** — additional principal on a date (validates against the balance).
- **Interest rate change** — applied from an effective date.
- **Missed/partial payment** — interest still accrues; ETA extends; flagged as an assumption.

## 5. Goals

- **Remaining amount** = `max(targetAmount − currentAmount, 0)`. When `current >= target`
  remaining is `0` (never negative; a goal is not "negative-progress").
- **Progress %** = 100 when `remaining == 0` (i.e., `current >= target`), else
  `currentAmount / targetAmount` (floored at 2 decimals for display so progress never overstates
  completion).
- **Required monthly capacity (dated goal)** = `remaining / numberOfMonths(asOfDate, targetDate)`,
  rounded with `CEILING` so the requirement is never understated.
- **Completion condition**: amount-based goal completes when `remaining <= 0`. Non-amount goals
  carry an explicit boolean condition.

## 6. Contribution toward a goal (the route step)

Given a monthly allocation `c` toward a goal with `remaining`:

- Each period contributes `min(c, remaining)`; contribution stops when remaining is exhausted.
- **ETA** = the earliest month such that accumulated contributions `>= remaining`.

## 7. Cash Allocation (the route)

Allocation decides **where Available Capacity goes**. It is an ordered list of rules:

| Priority | Rule |
|---:|---|
| 1 | Debt-vs-goal | High-rate debt is paid before low-priority savings unless overridden (see §8). |
| 2 | Goal order | Among active goals, follow user priority, then dependency order. |
| 3 | Cap | A goal stops once complete; freed capacity flows to the next rule. |

A rule carries: `(sourceCash, destination, amount, priority, effectiveFrom)`. When a destination
completes, the routing recomputes and freed capacity is reassigned — this is the **GPS route**.

## 8. Order of the route

Default order:

```
interest-bearing debt → emergency fund → prioritized goals → remaining goals
```

This is a **default policy**, not a hidden truth. It may be overridden by a goal dependency, a
user-defined priority, or a financial constraint (e.g., a rate threshold). The engine MUST state
which order policy was applied, and why, in every result that uses it.

## 9. Goal Dependency

- `(successorGoal, prerequisiteGoals, type)`. A dependent goal does not start contributing while a
  prerequisite is incomplete unless an explicit override is recorded.
- Completing a prerequisite frees capacity and reconciles the successor.
- The dependency graph is a DAG; the engine MUST detect a cycle and return a validation `BLOCKED`
  status instead of looping.

## 10. Timeline / Planned Changes (time model)

Financial inputs are not flat; they change on effective dates.

- A **timeline change** is `(kind, effectiveFrom, amount/rate, frequency, source)` on an income,
  expense, or debt line (e.g., "salary ×1.1 from 2027-04").
- **Frequency is locked to `MONTHLY` for the MVP**: the engine projects on a monthly cadence, so
  timeline changes and contributions are monthly. No `YEARLY`, `WEEKLY`, or `ONCE` scheduling is
  supported in v1 (a non-monthly cadence would force a calendar model the MVP does not need).
  `frequency` therefore has a single allowed value `MONTHLY`; keep the field for forward
  compatibility but constrain it now.
- The engine partitions time into **periods delimited by these effective dates** and projects
  within each period. Before `effectiveFrom`, the prior value applies.
- Any change is stored as a **user assumption or actual input with its effectiveFrom**; it is
  never silently extrapolated forward (future ≠ present is allowed only when declared).

## 11. Actual vs Projection Snapshot

- **ActualFinancialState**: the persisted user-supplied facts as of a date.
- **ProjectionFinancialState**: the derived state from applying timeline/allocation to the
  actual state, with isolation. Scenario runs use the projection state and never mutate the
  actual state.
- Every result carries an input/provenance snapshot labelling each value: actual,
  user-assumed, or calculated.

## 12. Determinism, AI boundary

- The engine recomputes instead of storing stale results; `reference-cases.md` verifies
  identical inputs + identical `asOfDate` → identical result.
- AI may extract intent (e.g., "salary +10% in 2027-04" → a structured timeline change) and
  may explain results, but MUST NEVER compute money or ETA. A structured value derived by AI is
  labelled a "user assumption" until the user confirms it.

## Definition of Done

Any change to a formula, rounding mode, or status rule MUST update this file, `status-rules.md`,
`reference-cases.md`, and the affected feature specs together (per the constitution's
Development Workflow section).