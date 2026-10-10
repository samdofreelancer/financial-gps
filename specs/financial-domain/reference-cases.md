# Financial Domain: Reference Cases (Normative Financial Oracle)

> **Normative Financial Oracle**: the deterministic acceptance matrix that every feature MUST
> satisfy. These are pure, reproducible cases: give the documented inputs and `asOfDate`, expect
> the documented output. They are the "yes/no source of truth" for the financial engine.

Each case: target inputs, expected output, which rule it pins down, and any status. Wiring to
JUnit/Vitest reference tables is an implementation concern.

## A. Cash flow (rule §3)

| ID | Income | Expense | Mandatory Payment | Expected Net Cash Flow | Expected Available Capacity |
|---|---|---|---|---|---|
| CF-001 | 74 | 30 | 20 | 24 | 24 |
| CF-002 | 30 | 30 | 0 | 0 | 0 |
| CF-003 | 20 | 30 | 0 | −10 (negative reported, not concealed) | 0 (capacity clamps to 0) |

## B. Debt amortization (rule §4)

| ID | balance | annual rate | monthly payment | month | expected new balance | notes |
|----|----|----|----|----|----|----|
| DC-001 | 1000 | 0.12 | 50 | 1 | 940 (= 1000 − (50 − 1000×0.12/12)) | principal 40 |
| DC-002 | 1000 | 0 | 100 | 1 | 900 | zero interest, linear |
| DC-003 | 50 | 0.12 | 100 | 1 | 0 | payment > balance; no negative |
| DC-004 | 1000 | 0.12 | 8 | — | grows; ETA `UNAVAILABLE`; status `BLOCKED` | payment < interest |

## C. Goals & ETA (rule §5, §6)

| ID | target | current | capacity/mo | expected remaining | expected ETA (mo) |
|----|--------|---------|-------------|--------------------|-------------------|
| G-001 | 108 | 0 | 24 | 108 | 5 (ceil 108/24) |
| G-002 | 108 | 108 | 24 | 0 | 0, `COMPLETED` |
| G-003 | 108 | 24 | 24 | 84 | 4 |
| G-004 | 100 | 0 | 0 | 100 | no finite ETA → `BLOCKED` |
| G-005 | 100 | 120 | 24 | 0 (max, never negative) | 0, progress 100%, `COMPLETED` |

### C2. Goal completion condition (rule §5)

A goal's `completionCondition` selects the completion rule. `DEBT_FREE` is valid only for a
`DEBT_FREEDOM` goal; every other goal type is `AMOUNT_REACHED`.

| ID | goal | completion condition | 002 portfolio status | expected goal lifecycle | expected GPS route |
|---|---|---|---|---|---|
| GC-001 | `DEBT_FREEDOM`, `current 120 >= target 100` | `DEBT_FREE` | `AVAILABLE` (ACTIVE debts remain) | `ACTIVE` — **not** `COMPLETED` | not `COMPLETED` (route judged by ETA/tolerance) |
| GC-002 | `DEBT_FREEDOM` | `DEBT_FREE` | `COMPLETED` (no ACTIVE debts) | `COMPLETED` | `COMPLETED` |
| GC-003 | `DEBT_FREEDOM` | `DEBT_FREE` | `BLOCKED` (payment < interest) | `ACTIVE` — **not** `COMPLETED` | `BLOCKED` with the propagated 002 reason code |
| GC-004 | amount goal, `current 120 >= target 100` | `AMOUNT_REACHED` | `AVAILABLE` (ACTIVE debts remain) | `COMPLETED` | `COMPLETED` (amount condition is independent of debts) |
| GC-005 | `DEBT_FREEDOM` previously completed (portfolio `COMPLETED`), then a new ACTIVE debt appears | `DEBT_FREE` | `AVAILABLE` (new ACTIVE debt) | `ACTIVE` — **reactivated**, not sticky | not `COMPLETED` (route re-evaluated) |

GC-001 is the pivotal case: a `DEBT_FREEDOM` goal whose reported amounts have met the target but
whose debts are not yet cleared is **not** `COMPLETED`. GC-004 pins that amount-based goals keep
their existing behaviour. GC-005 pins that `DEBT_FREE` completion is **dynamic** (not sticky): an
archived goal would instead preserve the milestone.

## D. Required capacity (dated goal, rule §5)

| ID | remaining | months(asOf→target) | expected requiredMonthly |
|----|-----------|---------------------|--------------------------|
| RC-001 | 120 | 10 | 12 (CEILING: not 11.99) |
| RC-002 | 121 | 10 | 13 (CEILING 12.1) |

## E. Determinism & as-of (rule §2 of calculation rules)

| ID | action | expected |
|----|--------|----------|
| DM-001 | same inputs, same `asOfDate`, recalc | identical result (values, ETA, status) |
| DM-002 | same inputs, `asOfDate` +1 day | ETA may shift; result explains the shift |
| DM-003 | same inputs, different `asOfDate`, same contribution | progress/status reflects the difference, never a stale cache |

## F. Status (status-rules)

| ID | situation | expected status |
|----|-----------|-----------------|
| status-001 | adequate capacity, dated goal | `ON_TRACK` |
| status-002 | positive capacity, finite ETA past target within tolerance | `AT_RISK` |
| status-003 | positive capacity, ETA slips beyond tolerance | `OFF_TRACK` |
| status-004 | non-positive Net Cash Flow or payment < interest | `BLOCKED` |
| status-005 | all completion conditions met | `COMPLETED` |
| status-006 | two identical inputs, only target-date horizon differs (review #7) | both evaluated by the same watched `tolerance`, not an absolute month count |
| status-007 | tolerance 3; goal finishes exactly **2 periods late** (`lateness = 2`) | `AT_RISK` — and nothing else; `ON_TRACK` is reserved for `lateness = 0` |
| status-008 | tolerance 3; `lateness = 3` (exactly at tolerance) | `AT_RISK` (inclusive upper bound) |
| status-009 | tolerance 3; `lateness = 4` (one period beyond tolerance) | `OFF_TRACK` |
| status-010 | `lateness = 0` (ETA on or before `targetDate`) | `ON_TRACK` |
| status-011 | undated goal with a finite route | `ON_TRACK` (no `AT_RISK`/`OFF_TRACK` without a target) |
| status-012 | debt-freedom destination, 002 portfolio `BLOCKED` (payment < interest) | `BLOCKED` with the propagated 002 reason code |
| status-013 | debt-freedom destination, 002 portfolio `COMPLETED` (no ACTIVE debts remain) | `COMPLETED` |
| status-014 | debt-freedom destination whose portfolio was `COMPLETED`, then a new ACTIVE debt appears | no longer `COMPLETED`; route re-evaluated from the **current** portfolio (not sticky) |

## G. Timeline change (rule §10 / 008)

| ID | timeline input | expected |
|----|----------------|----------|
| TM-001 | salary ×1.1 effective 2027-04 | projection before 2027-04 uses old salary; from 2027-04 uses ×1.1 |
| TM-002 | rent +3 from 2027-02 | Net Cash Flow drops by 3 only for periods from 2027-02 |
| TM-003 | extra debt payment 5/mo from 2027-01 | debt ETA shortens; extra is labelled a user assumption |

## H. Allocation & dependency (rule §7, §8, §9)

| ID | allocation | expected |
|----|-----------|----------|
| AL-001 | Available Capacity 24 → debt 15, goal 9 | both receive their documented amounts |
| AL-002 | debt completes | freed debt capacity 15 now routed to next priority goal |
| AL-003 | goal A requires goal B; B incomplete | A gets no contribution, no silent start |
| AL-004 | dependency cycle A→B→A | validation error / `BLOCKED`, engine does not loop |
| AL-005 | default vs user order differ | engine states which order policy applied and why |
| AL-006 | goal A requires goal A | validation error / `BLOCKED` (self-dependency); no loop |

## Is Simulated vs Actual (rule §12)

| ID | scenario | expected |
|----|----------|----------|
| SC-001 | scenario `income ×1.1` | actual persisted income unchanged |
| SC-002 | scenario extra debt payment | actual debt balance unchanged; result labelled projection |

## How to maintain

Add a case whenever a formula, rounding mode, or status rule changes, and re-run the suite. A
record that fails a case is a constitution violation until the doc or the case is explicitly
amended.