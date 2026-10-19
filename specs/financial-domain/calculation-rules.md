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

"Remaining capacity" is banned as a *name for `Available Capacity`*. It is legitimate only as the
waterfall's local running remainder (§7), which starts at `AvailableCapacity_p` and is decremented by
each `Funded` amount — never a second pool, never a stored field.

| Term | Definition | Do NOT call it |
|---|---|---|
| **Income** | Total active monthly money in (sum of recurring incomes effective on `asOf`). | revenue, inflow |
| **Expense** | Total active monthly money out (sum of recurring expenses effective on `asOf`). | spending, outflow |
| **Mandatory Payment** | A payment that is already committed before discretionary decisions: the required monthly debt payment. | committed payment, obligation |
| **Net Cash Flow** | `Income − Expense − MandatoryPayment`. May be negative; a negative value is reported, never concealed. | disposable, cash surplus |
| **Available Capacity** | `AvailableCapacity = max(NetCashFlow, 0)` — the portion of NetCashFlow available for discretionary **allocation** in a period. For v1 no borrow/drawdown is allowed, so a negative NetCashFlow yields zero allocation. See §7. | free cash, available money, leftover |
| **Allocation** | A priority-ordered rule that routes a slice of **Available Capacity** to a **destination** (debt, emergency fund, goal). Allocation is the *decision* of where money goes. | distribution, split, plan |
| **Contribution** | The money that **actually reaches** a goal or debt in a period after the allocation rule; a payment is the identical term for debts. Contribution is capped by `remaining` and must not exceed the goal target. | deposit, savings, pay-in |
| **Debt Extra** | `DecimalExtra = max(plannedPayment − minimumPayment, 0)` for one ACTIVE debt: the discretionary payment the user plans on top of the mandatory minimum. It is **requested**, not spent — it is funded from `Available Capacity`. | surplus payment, overpayment, extra cash |
| **Funded Extra** | The slice of a debt's **Debt Extra** that the allocation actually funded in a period. `FundedExtra <= DecimalExtra` and `<=` remaining capacity. | actual extra, real payment |
| **Effective Payment** | `minimumPayment + FundedExtra` — the payment a debt projection MUST amortize on. A projection MUST NOT amortize on `plannedPayment` when the extra was not funded. | total payment, planned payment |
| **Shortfall** | `Requested − Funded` for one destination in one period. It is disclosed, never funded by imaginary money, and never a reason to reject the plan. | gap, deficit, underfunding |
| **Unallocated Amount** | `AvailableCapacity − Σ Funded` for a period: capacity no destination could absorb. Reported and labelled; never routed to a complete destination or turned into a savings movement. | leftover cash, spare money |

Requested versus funded, in one line: `Available Capacity` is the pool; `Debt Extra` is a
**request** against it; `Funded Extra` is what the waterfall actually granted; `Effective Payment` is
what the debt projection consumes; `Shortfall` is what was refused. No step in this chain creates
money, and no amount is ever counted in two of them.

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
- **Payment source**: the monthly payment used above is the debt's **Effective Payment**
  (`minimumPayment + FundedExtra`), not the stored `plannedPayment`. `plannedPayment` only *requests*
  a `Debt Extra`; if the allocation could not fund it, the extra is not spent and MUST NOT appear in
  the projection (§7, §8; 008 FR-005). The 002 standalone summary may use `plannedPayment` for the
  user's own plan, but any route-driven projection MUST use `Effective Payment`.

### Supported debt actions (per period or dated)

- **Regular payment** — configured monthly payment (`minimumPayment`).
- **Extra payment** — a `Debt Extra`, requested by `plannedPayment` or by an explicit allocation rule,
  funded from `Available Capacity` (§7). It is never a second payment stream and never a stored debt
  fact.
- **Interest rate change** — applied from an effective date, absolute value (`SET_VALUE`, §10).
- **Minimum payment change** — applied from an effective date, absolute value (`SET_VALUE`, §10).
- **Missed/partial payment** — interest still accrues; ETA extends; flagged as an assumption.

Out of v1: one-off lump sums, future balance edits, and lender-specific schedules (fees, penalties,
payment-date rules).

## 5. Goals

- **Remaining amount** = `max(targetAmount − currentAmount, 0)`. When `current >= target`
  remaining is `0` (never negative; a goal is not "negative-progress").
- **Progress %** = 100 when `remaining == 0` (i.e., `current >= target`), else
  `currentAmount / targetAmount` (floored at 2 decimals for display so progress never overstates
  completion).
- **Required monthly capacity (dated goal)** = `remaining / numberOfMonths(asOfDate, targetDate)`,
  rounded with `CEILING` so the requirement is never understated.
- **Completion condition**: a goal carries exactly one `completionCondition`:
  - `AMOUNT_REACHED` — the amount-based condition: the goal completes when `remaining <= 0`.
  - `DEBT_FREE` — the debt-freedom condition: the goal completes when the Feature 002 debt
    **portfolio projection status is `COMPLETED`** (no ACTIVE debts remain). This is an explicit
    boolean condition evaluated from Feature 002's portfolio status; it MUST NOT be re-derived here
    (no duplicate debt arithmetic). The check uses the **current** portfolio status and is **not
    sticky** (a new ACTIVE debt makes the destination non-`COMPLETED` again). `DEBT_FREE` is valid
    only for a `GoalType = DEBT_FREEDOM` goal; every other goal type MUST use `AMOUNT_REACHED`
    (`data-model.md`).
- For a `DEBT_FREE` goal, `targetAmount`/`currentAmount` are **optional advisory context** and MUST
  NOT define `remaining`, progress, required capacity, or completion.
- **Emergency fund**: the destination known as "the emergency fund" is a Feature 003 goal with
  `GoalType = EMERGENCY_FUND` and `completionCondition = AMOUNT_REACHED`, using its own
  `currentAmount` and `targetAmount`. The profile's `emergencyFundAmount` is a separate reported
  fact: it MUST NOT be added to, auto-synced with, or silently preferred over the goal. With no such
  goal, the emergency-fund step of the route is skipped and no target is invented (no
  "N months of expenses" formula exists in v1).

## 6. Contribution toward a goal (the route step)

Given a monthly allocation `c` toward a goal with `remaining`:

- Each period contributes `min(c, remaining)`; contribution stops when remaining is exhausted.
- **ETA** = the earliest month such that accumulated contributions `>= remaining`.

## 7. Cash Allocation (the route)

Allocation decides **where Available Capacity goes**. It is an ordered list of rules. `Available
Capacity` is the **single pool**: no rule has its own money, and a debt's `Debt Extra` is a request
against that same pool, never a second budget.

| Priority | Rule |
|---:|---|
| 1 | **Committed debt extra** — fund each debt's `Debt Extra` (`plannedPayment − minimumPayment`), unless an explicit rule replaces it (see §8). |
| 2 | **Emergency fund** — a Feature 003 goal with `GoalType = EMERGENCY_FUND`; no such goal means this step is skipped. |
| 3 | **Goals by priority** — active, eligible goals in the deterministic order of §8. |
| 4 | **Cap** — a destination stops once its need is met; freed capacity flows to the next rule **in the same period**. |

A rule carries: `(sourceCash, destination, amount, priority, effectiveFrom)`. `sourceCash` is always
`Available Capacity`. When a destination completes, the routing recomputes and freed capacity is
reassigned — this is the **GPS route**. Capacity that no destination can absorb is the
**Unallocated Amount** (§0): reported, never routed to a completed destination, never turned into an
unspoken savings movement.

**Waterfall, not pro-rata.** For each destination in order:

```
Requested = min(ruleAmount, RemainingNeed)
Funded    = min(Requested, RemainingCapacity)
Shortfall = Requested − Funded
RemainingCapacity -= Funded
```

The residue of a destination that needed less than it requested goes to the next eligible
destination in the same period. Destinations that arrive after capacity is exhausted fund `0` and
report a shortfall; an underfunded plan is disclosed, never rejected. Pro-rata splitting is
prohibited in v1.

## 8. Order of the route

Default order:

```
committed debt extra → emergency-fund goal → goals by priority → remaining goals
```

Within a tier the order is deterministic and stable: `(priority, createdAt, id)`, falling back to
`id` when `createdAt` is absent. Persistence or query order MUST NOT change the result. A goal
dependency is an **eligibility gate**, not a reordering: an ineligible goal funds `0` and still
reports its required capacity.

This is a **default policy**, not a hidden truth. It may be overridden by a goal dependency or a
user-defined priority; each override is disclosed. The engine MUST state which order policy was
applied, and why, in every result that uses it.

**Not in v1**: reordering by interest rate ("high-rate debt first"). A debt-vs-goal rate threshold
needs a documented policy value, a disclosure code, and its own oracle case; it belongs to the debt
strategy feature (005), not to the default route. Until then, no rule may pay a debt before a
higher-priority destination because of its rate alone.

## 9. Goal Dependency

- `(successorGoal, prerequisiteGoals, type)`, with `type = FINISH_TO_START` only in v1.
- A dependent goal does not start contributing while a prerequisite is incomplete. **v1 has no
  override**: the only ways to unblock a successor are to complete or archive the prerequisite.
- The gate is evaluated at the **start** of each period, so a successor becomes eligible in the
  period **after** its prerequisite completes — never in the completing period itself.
- Completing a prerequisite frees capacity and reconciles the successor.
- An ineligible destination receives `0` and still reports its required capacity (the future load
  stays visible); it is not silently dropped from the route.
- The dependency graph is a DAG; the engine MUST detect a cycle (including a self-loop) and reject it
  on write (`400 VALIDATION_FAILED` / `DEPENDENCY_CYCLE`), or return a validation `BLOCKED` status if
  one is encountered during projection — instead of looping.

## 10. Timeline / Planned Changes (time model)

Financial inputs are not flat; they change on effective dates.

- A **timeline change** is `(kind, effectiveFrom, amount/rate, frequency, source)` on an income,
  expense, or debt line. It stores an **absolute** value (`SET_VALUE`); a relative entry such as
  "salary +10% from 2027-04" is a user-interface convenience that MUST be converted to the absolute
  amount (`81.4`) before it is stored. Multipliers, one-off lump sums, and future balance edits are
  not modelled in v1.
- In-scope kinds: income amount, expense amount, debt annual interest rate, debt minimum payment.
  `plannedPayment` is **not** a timeline change — it is a request against Available Capacity
  (§7), so it belongs to allocation, not to the time model.
- **Frequency is locked to `MONTHLY` for the MVP**: the engine projects on a monthly cadence, so
  timeline changes and contributions are monthly. No `YEARLY`, `WEEKLY`, or `ONCE` scheduling is
  supported in v1 (a non-monthly cadence would force a calendar model the MVP does not need).
  `frequency` therefore has a single allowed value `MONTHLY`; keep the field for forward
  compatibility but constrain it now.
- The effective value for a period is the change with the **latest `effectiveFrom` on or before the
  period start**. Before `effectiveFrom`, the prior value applies. Two changes on the same line with
  the same `effectiveFrom` are a **conflict**: reject them, never resolve them by insertion order.
- Any change is stored as a **user assumption or actual input with its effectiveFrom**; it is
  never silently extrapolated forward (future ≠ present is allowed only when declared).
- A line with no change keeps its flat value in every period and is labelled **"assumed unchanged"**
  — a flat value is an explicit assumption, never presented as a forecast.

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