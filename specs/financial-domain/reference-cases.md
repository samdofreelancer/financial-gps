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

A timeline change is an **absolute** `SET_VALUE` on an effective date (008 C-04). A multiplier is
an input convenience only: the user may type "+10%", the stored change is the computed absolute
value. Two changes for the same line and the same `effectiveFrom` are a conflict, never resolved by
insertion order. `plannedPayment` is **not** a timeline change (it belongs to allocation, §H).

| ID | timeline input | expected |
|----|----------------|----------|
| TM-001 | salary `SET_VALUE 81.4` effective 2027-04 (entered as "+10%" of 74) | income = 74 for every period before 2027-04; income = 81.4 for every period from 2027-04; change labelled `user-assumed` |
| TM-002 | rent `SET_VALUE 33` effective 2027-02 (from 30) | Net Cash Flow is 3 higher before 2027-02 and 3 lower from 2027-02; no period is retroactively rewritten |
| TM-003 | ~~extra debt payment 5/mo from 2027-01~~ | **DEPRECATED** — an extra debt payment is not a timeline change. It is a `CashAllocationRule` funded from Available Capacity; see `AL-007..AL-010`. Retained as a tombstone so the ID is not reused with a different meaning. |
| TM-004 | debt annual rate `SET_VALUE 0.15` effective 2027-06 (from 0.12), balance 1000, payment 50 | periods before 2027-06 accrue `1000×0.12/12 = 10`; from 2027-06 they accrue on the then-open balance at `0.15/12`; payoff period count changes only because of periods on/after 2027-06 |
| TM-005 | debt `minimumPayment SET_VALUE 25` effective 2027-03 (from 20) | `Mandatory Payment` is 20 before 2027-03 and 25 from 2027-03; Net Cash Flow and Available Capacity absorb the 5 exactly once and only from that period |
| TM-006 | any change with `effectiveFrom` earlier than `asOfDate` | applies from the first projected period; provenance retained; no already-reported month is rewritten |
| TM-007 | two changes on one line: `SET_VALUE 80` from 2027-04 and `SET_VALUE 85` from 2027-07 | 2027-04..2027-06 use 80; from 2027-07 use 85 (latest `effectiveFrom` on or before the period start wins) |
| TM-008 | two changes on one line with the **same** `effectiveFrom` | rejected on write as a conflict (`400 VALIDATION_FAILED`); never resolved by insertion order |
| TM-009 | a line with no change | keeps its flat value in every period and is labelled "assumed unchanged" |

## H. Allocation & dependency (rule §7, §8, §9)

`Available Capacity` is the **single pool** (008 C-01). A debt's `DecimalExtra =
max(plannedPayment − minimumPayment, 0)` is an allocation funded *from* that pool, never a second
pool, and never re-added to Income. Allocation is a **priority waterfall** (008 C-02), never
pro-rata: `Funded = min(Requested, RemainingNeed, RemainingCapacity)`. Money a destination cannot
absorb cascades to the next eligible destination **in the same period**.

| ID | allocation | expected |
|----|-----------|----------|
| AL-001 | Available Capacity 24 → debt 15, goal 9 | both receive their documented amounts |
| AL-002 | debt completes | freed debt capacity 15 now routed to next priority goal |
| AL-003 | goal A requires goal B; B incomplete | A gets no contribution, no silent start |
| AL-004 | dependency cycle A→B→A | validation error / `BLOCKED`, engine does not loop |
| AL-005 | default vs user order differ | engine states which order policy applied and why |
| AL-006 | goal A requires goal A | validation error / `BLOCKED` (self-dependency); no loop |
| AL-007 | Income 74, Expense 30, debt `minimumPayment 20`, `plannedPayment 27` | `Net Cash Flow = 24`, `Available Capacity = 24`; the debt-extra row funds **7**; other destinations share at most **17**; the debt amortizes on **27** (`20 + 7`), never on `20 + 24`; whatever the other destinations do not request is reported as `unallocatedAmount`, not spent |
| AL-008 | same position; explicit debt rule `15` replaces the planned extra `7`; emergency-fund rule `9` | the debt funds **15** (not 22, and not 7 + 15); the emergency fund receives **9**; `Σ funded = 24`; the debt projection payment is `20 + 15 = 35`; no third money stream appears |
| AL-009 | capacity 24; debt A requested extra 20, debt B requested extra 10 (Σ requested 30 > 24) | funded amounts sum to **24** and shortfall **6** is reported per destination; a debt whose extra is only partly funded amortizes on `minimumPayment + FundedExtra` only. The split between A and B is decided by the explicit per-debt priority (008 D-05); no debt is projected on an amount the route did not fund |
| AL-010 | capacity 24; debt `minimumPayment 20`, rate `0`, balance 1000, explicit extra `15` funded in full | debt projection payment is **35**, so opening 1000 → closing **965** that period; with the same position but the extra unfunded, the payment is **20** and the closing balance is **980** — the two calculations consume the *same* `FundedExtra` |
| AL-011 | capacity 24; goal requests 15 / 12 / 10 in that priority order | funded **15 / 9 / 0**; shortfall **0 / 3 / 10**; no pro-rata (never 11.25 / 9 / 3.75) |
| AL-012 | capacity 24; first destination requests 15 but needs only 8; second destination requests 20 | first funds **8** (not 15); the unused **7** returns to the pool, so the second funds **16** (not `24 − 15 = 9`) with shortfall **4**; `Σ funded = 24`, `unallocated = 0`. If the second requested only 10, it funds **10** and `unallocated = 6`. A reallocation event discloses the cascade |
| AL-013 | `Available Capacity = 0` (Net Cash Flow ≤ 0) | every allocation row funds **0.00**; each shortfall equals its request; `unallocated = 0`; no negative funding and no fabricated route |
| AL-014 | any period, including cascades and completions | `Σ fundedAmount ≤ AvailableCapacity_p` always, and `unallocated_p = AvailableCapacity_p − Σ funded_p` exactly; no rounding residue is lost or invented |
| AL-015 | same inputs, assumptions, `asOfDate`, policy — recompute | byte-identical route: same destination order, same funded amounts, same ETA, same events. Query/persistence order never changes the result |
| AL-016 | goal B requires goal A; A incomplete at the start of the period | B funds **0.00** with reason `PREREQUISITE_INCOMPLETE`; B's **required capacity is still reported**; B does not start in the period A completes, only the following one (FR-025) |
| AL-017 | no destination is eligible or every destination is complete | `Σ funded = 0`; the whole capacity is reported as `unallocatedAmount` and labelled; it is never routed to a completed/archived destination and never turned into a savings movement |

## Is Simulated vs Actual (rule §12)

| ID | scenario | expected |
|----|----------|----------|
| SC-001 | scenario `income ×1.1` | actual persisted income unchanged |
| SC-002 | scenario extra debt payment | actual debt balance unchanged; result labelled projection |

## How to maintain

Add a case whenever a formula, rounding mode, or status rule changes, and re-run the suite. A
record that fails a case is a constitution violation until the doc or the case is explicitly
amended.