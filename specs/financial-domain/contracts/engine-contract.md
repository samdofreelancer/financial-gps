# Engine Contract: Financial Domain Core

> The pure Java interface of the Financial Domain Engine. This is **not** a REST/network
> contract; it is the in-process API that feature layers (`004/005/006/008/009`) and adapters
> call. The engine has no dependency on DB, HTTP, clock, randomness, AI, authentication, user
> identity, sessions, or security context. All amounts are `Money`/decimal strings; nothing here
> persists.

## Single entry point

This is an **internal, in-process** API — not REST. The public signature is fixed:

```java
FinancialResult calculate(
    FinancialInput   input,          // active income, expenses, debts, goals (from 001/002/003)
    Assumptions      assumptions,    // labels user-supplied vs system-default
    LocalDate        asOfDate,       // first-class; determinism depends on it
    FinancialPolicy  policy           // rounding, debt, allocation, status
);
```

- Orchestration order is **locked** in `plan.md` §"FinancialEngine orchestration"
  (validate → timeline → cash flow → debt → dependency → allocation → goal/ETA → status →
  assemble). The implementation agent must not reorder it.
- Dependencies (cycle/self-loop) are validated inside step 5; not at the engine boundary only.
- **Identity-free by design**: associating a result with an authenticated owner is an
  application-layer task (`007-authentication`, an independent platform capability); the
  signature contains no user/session/token parameter.

## Supporting calculators (internal API, reusable & unit-tested)

| Method | Purpose |
|---|---|
| `CashFlowCalculator.calculate(input, policy)` | `NetCashFlow` and `AvailableCapacity` (`income − expense − mandatoryPayment; available = max(...,0)`). |
| `DebtCalculator.payoff(debt, allocation, policy)` | per-period amortization; final-period clamp; `payment < interest` → no finite ETA. |
| `GoalCalculator.progress(goal)` | `remaining = max(target−current,0)`, progress %, required capacity (amount goals); a `DEBT_FREE` goal reports these not applicable. |
| `GoalCompletion.satisfied(goal, portfolioStatus)` | Evaluates `completionCondition`: `AMOUNT_REACHED` → `remaining = 0`; `DEBT_FREE` → Feature 002 portfolio `COMPLETED`. Reads 002's status; never recomputes debt. The `DEBT_FREE` result follows the **current** status and is **not sticky** (a new ACTIVE debt makes it false again). |
| `AllocationCalculator.route(available, rules, deps)` | priority **waterfall** over ordered debt/goal rules — `funded = min(requested, remainingNeed, remainingCapacity)`, residue cascades in-period; reallocates on completion. Never pro-rata. Returns rows with `requestedAmount`/`fundedAmount`/`shortfallAmount` plus `unallocatedAmount` (`AL-007..AL-017`). |
| `DebtExtraResolver.funded(debt, fundedExtra)` | the single place that turns a debt's `Debt Extra` into `EffectivePayment = minimumPayment + FundedExtra`; the amortizer never reads `plannedPayment`. |
| `TimelineEngine.slice(inputs, asOf, changes)` | divides from `asOfDate` into monthly periods, applies effective changes. |
| `ProjectionEngine.project(...)` | drives periods → calculators → `FinancialResult`. |
| `DependencyResolver.validate(deps)` | DAG check; rejects cycle incl. self-loop (`AL-004`, `AL-006`). |
| `StatusEvaluator.evaluate(result, statusPolicy)` | `COMPLETED→BLOCKED→ON_TRACK→AT_RISK→OFF_TRACK` with tolerance. |

> **Implementation delta (verified 2026-10-19)**: this table is the **target** internal API. The
> implemented `com.financialgps.domain.engine.FinancialEngine` returns only
> `(asOf, position, provenance, explanations)` and has no allocation, route, or ETA stage; the real
> GPS orchestration lives in `application/gps/usecase/CalculateFinancialGpsUseCase`. There is no
> `AllocationCalculator`, `TimelineEngine`, or `DependencyResolver` class today. Whether to build them
> or promote `FinancialEngine` is 008 decision **D-17**, settled in the plan — not here.

## Determinism & policy

- `FinancialPolicy` is immutable and passed, never read from globals. `calculate` with same
  `inputs + assumptions + asOfDate + policy` returns an identical `FinancialResult` (see
  `DM-001/002/003`).
- The engine never introduces randomness, clock reads, or AI. An AI-derived change must be
  presented as a `SYSTEM_DEFAULT`/user assumption before it can enter `calculate`, never computed
  inside it.

## Return value

- `FinancialResult` carries: current position, destination, distance, progressPercent, eta
  (or `UNAVAILABLE` + reason), status, blockers, nextActions, and an `explanation` + `provenance`
  set labelled actual/assumed/calculated (constitution §§IV–VII). No hidden numeric score as the
  primary result.

## Failure semantics

- Domain violations (e.g., negative amount, cyclic dependency) throw typed domain exceptions or
  return a `ValidationResult` with a stable code; they never silently proceed. This is the layer
  later features map to their error responses (`ProblemDetail`), but the engine itself has no HTTP
  concept.