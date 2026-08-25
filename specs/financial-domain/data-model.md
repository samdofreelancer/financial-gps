# Data Model: Financial Domain Engine

> Pure domain types and value objects. None of these persist or access infrastructure; they are
> the input/output types of `calculate(...)`. Names follow the **canonical terminology** in
> `calculation-rules.md` §0 — no near-synonyms.

## Value Objects

| Type | Fields / Rules |
|---|---|
| `Currency` | ISO 4217 string; initial `VND`. |
| `Money` | `BigDecimal amount` (scale 2), `Currency currency`. Non-negative for balances/amounts. All arithmetic uses an explicit `RoundingMode`. Never `float`/`double`. |
| `Rate` | `BigDecimal fraction` (0…1 for MVP; e.g. `0.12` = 12%/yr), scale 6. |
| `Ratio` | `BigDecimal` 0…1, scale 4 (e.g. progress %, debt-to-income). |
| `LocalDate` / `AsOfDate` | The `asOfDate` is a first-class value; all projections are a function of it. |
| `Month` | A `YearMonth` period token; periods are delimited by effective dates. |

## Inputs (supplied by features 001/002/003)

| Type | Fields |
|---|---|
| `FinancialInput` | `ownerId`-free; an aggregate of active income, expenses, debts, goals, present as-of. Never reads a store. |
| `Income` | `Money amount`, `effectiveFrom`, `source`, `active`. |
| `Expense` | `Money amount`, `effectiveFrom`, `category`, `expenseType`, `active`. |
| `Debt` | `Money outstandingBalance`, `Rate annualInterestRate`, `Money monthlyPayment`, `Money originalPrincipal?`, `LocalDate dueDate`, `LocalDate plannedPayoffDate?`, `status(ACTIVE/PAID/ARCHIVED)`. |
| `Goal` | `Money targetAmount`, `Money currentAmount`, `LocalDate targetDate?`, `completionCondition`, `priority`, `status`. `remaining = max(target − current, 0)`. |
| `Assumptions` | a labelled set of `FinancialAssumption`; each carries `source: USER_SUPPLIED / SYSTEM_DEFAULT` and a `userAssumed` flag. |

## Derived / projected

| Type | Fields |
|---|---|
| `NetCashFlow` | `Money income − expense − mandatoryPayment`; may be negative; reported, not concealed. |
| `AvailableCapacity` | `max(NetCashFlow, 0)`. |
| `TimelineChange` | `kind` (income/expense/debt/goal), `effectiveFrom`, `amount/rate`, `frequency` (only `MONTHLY`), `source`. |
| `CashAllocationRule` | `destination`, `amount`, `priority`, `effectiveFrom`. `source` is always `AvailableCapacity`. |
| `AllocationResult` | per period: the contribution to each debt/goal; capped by `remaining`. |
| `Contribution` | `Money` actually reaching a goal/debt in a period. |
| `GoalDependency` | `successorGoal`, `prerequisiteGoals[]`, `type`; must be a DAG (reject self-loop/cycle). |
| `Route` | the effective, priority-ordered destination sequence. |
| `GpsResult` / `FinancialResult` | the deterministic output: position, destination, distance, progressPercent, eta, status, source of each field, explanation set, provenance. |
| `ProjectionFinancialState` | a snapshot after applying timeline + allocation to the actual inputs; **isolated** — it never mutates the persisted actual state. |

## Relationships (locked)

```text
FinancialInput
  ├── incomes:          List<Income>
  ├── expenses:         List<Expense>
  ├── debts:            List<Debt>
  ├── goals:            List<Goal>
  ├── timelineChanges:  List<TimelineChange>
  └── allocationRules:  List<CashAllocationRule>

Goal
  └── prerequisites → GoalDependency[]      (successorGoal = this)

Debt
  └── paymentPolicy                          (from FinancialPolicy.debt)

FinancialPolicy
  ├── rounding:   RoundingPolicy
  ├── debt:       DebtPolicy
  ├── allocation: AllocationPolicy
  └── status:     StatusPolicy
```

All composition is by reference to immutable value objects; nothing is persisted or lazily
loaded here. A `FinancialInput` is the aggregate passed to `calculate(...)`; feature adapters
(`001/002/003`) build one per owner/request at the boundary, never inside the engine.

## Validation rules (from contract)

- Money amounts are non-negative.
- `Rate` in [0,1]; ratio in [0,?] per `calculation-rules.md` §1.
- Debt: `payment OR balance` consistent; `monthlyPayment < accrued interest` ⇒ no finite payoff.
- Goal: `target >= 0`; `remaining = max(target − current, 0)`; progress `min(..., 100%)`.
- Timeline: `frequency` is `MONTHLY` only; effectiveFrom required.
- Allocation: total per period ≤ `AvailableCapacity`; a `BLOCKED`/`UNAVAILABLE` ETA is allowed but must be labelled.
- Dependencies are acyclic (self-loop, A→A, rejected).

## State transitions

- A `Debt` moves `ACTIVE → PAID → ARCHIVED`; `PAID` requires zero outstanding balance.
- A `Goal` moves `ACTIVE → COMPLETED` when `remaining == 0`; `ARCHIVED` is a user/team action.
- A projection never changes input status: **ProjectionFinancialState** is read-only vs actual.