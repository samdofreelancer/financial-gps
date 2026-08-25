# Financial Domain: Reference Cases (Financial Oracle)

> **Advisory oracle**: the deterministic acceptance matrix that every feature MUST satisfy.
> These are pure, reproducible cases: give the documented inputs and `asOfDate`, expect the
> documented output. They are the "yes/no single source of truth" for the financial engine.

Each case: target inputs, expected output, which rule it pins down, and any status. Wiring to
JUnit/Vitest reference tables is an implementation concern.

## A. Cash flow (rule §3)

| ID | income | expense | req. debt | expected available | expected free |
|---|---|---|---|---|---|
| CF-001 | 74 | 30 | 20 | 24 | 24 |
| CF-002 | 30 | 30 | 0 | 0 | 0 |
| CF-003 | 20 | 30 | 0 | −10 (negative reported, not concealed) | −10 |

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

## D. Required capacity (dated goal, rule §5)

| ID | remaining | months(asOf→target) | expected requiredMonthly |
|----|-----------|---------------------|--------------------------|
| RC-001 | 120 | 10 | 12 (CEILING: not 11.99) |
| RC-002 | 121 | 10 | 13 (CEILING 12.1) |

## E. Determinism & as-of (rule §10 of calculation rules)

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
| status-004 | non-positive cash flow or payment < interest | `BLOCKED` |
| status-005 | all completion conditions met | `COMPLETED` |
| status-006 | two identical inputs, only target-date horizon differs (review #7) | both evaluated by the same watched `tolerance`, not an absolute month count |

## G. Timeline change (rule §11 / 008)

| ID | timeline input | expected |
|----|----------------|----------|
| TM-001 | salary ×1.1 effective 2027-04 | projection before 2027-04 uses old salary; from 2027-04 uses ×1.1 |
| TM-002 | rent +3 from 2027-02 | available cash flow drops by 3 only for periods from 2027-02 |
| TM-003 | extra debt payment 5/mo from 2027-01 | debt ETA shortens; extra is labelled a user assumption |

## H. Allocation & dependency (rule §7, §8, §9)

| ID | allocation | expected |
|----|-----------|----------|
| AL-001 | free cash 24 → debt 15, goal 9 | both receive their documented amounts |
| AL-002 | debt completes | freed debt cash 15 now routed to next priority goal |
| AL-003 | goal A requires goal B; B incomplete | A gets no contribution, no silent start |
| AL-004 | dependency cycle A→B→A | validation error / `BLOCKED`, engine does not loop |
| AL-005 | default vs user order differ | engine states which order policy applied and why |

## Is Simulated vs Actual (rule §12)

| ID | scenario | expected |
|----|----------|----------|
| SC-001 | scenario `income ×1.1` | actual persisted income unchanged |
| SC-002 | scenario extra debt payment | actual debt balance unchanged; result labelled projection |

## How to maintain

Add a case whenever a formula, rounding mode, or status rule changes, and re-run the suite. A
record that fails a case is a constitution violation until the doc or the case is explicitly
amended.