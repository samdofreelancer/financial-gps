# Feature Specification: Financial Timeline & Allocation

**Feature Branch**: `008-financial-timing-allocation`

**Created**: 2026-08-25

**Last Updated**: 2026-10-19

**Status**: Clarified — ready for architecture design. `plan.md`, `tasks.md`, `data-model.md`, and
`contracts/` are intentionally NOT produced (this revision is requirements-only).

**Input**: User description: "Model a financial time dimension (effective-date income/expense/debt
changes instead of a flat monthly snapshot) and a cash allocation engine that decides where free
monthly cash is routed (debt, emergency fund, goals), so the GPS route and its ETA reflect real
planning rather than assuming future equals present."

**Depends on**: `001-financial-profile`, `002-debt-management`, `003-financial-goals`,
`004-financial-gps`.

**Normative context**: this specification MUST satisfy, and MUST NOT restate with a competing
formula:

- `specs/financial-domain/calculation-rules.md` — canonical terminology (§0), formulas, route order.
- `specs/financial-domain/status-rules.md` — the five statuses, precedence, `latenessTolerance`.
- `specs/financial-domain/reference-cases.md` — the deterministic oracle (`CF-*`, `TM-*`, `AL-*`).
- `specs/financial-domain/data-model.md` and `contracts/engine-contract.md` — engine input/output.
- `specs/001-financial-profile/spec.md`, `002-debt-management/spec.md`,
  `003-financial-goals/spec.md`, `004-financial-gps/spec.md`.
- `.specify/memory/constitution.md` — principles I–XIV (especially I, II, III, V, VI, VIII, XI,
  XII, XIV).

**Amends**: `004-financial-gps` §2.2/§7.3/§9.2/§9.3/§13/§16/§17/§18 plus its new §19 (allocation and
timeline are no longer non-goals; the ETA becomes route-aware) and its `contracts/rest-api.md` ETA
section. `004` keeps its endpoint and response shape; its `VR-*` cases are re-baselined as the
degenerate case "one destination, no debt extra, no competitor".

**Also amends**: `specs/financial-domain/calculation-rules.md` (§0 terms, §4 payment source, §5
emergency fund, §7/§8 waterfall and order, §9 dependencies, §10 `SET_VALUE`) and
`specs/financial-domain/reference-cases.md` (§G timeline, §H allocation).

**Analysis**: `open-decisions.md` in this folder holds the full classification (decided / already
implemented / true gap), the formula-ownership audit, the unratified backlog with options, and the
risk register. This spec is the normative subset.

## Problem this solves

The GPS treats financials as a flat monthly snapshot ("salary 74, expense 30") and implicitly
extrapolates **future = present** forever. It never answers the route question: **"where does my
Available Capacity actually go each month?"** This feature adds a **timeline** (effective-dated
changes), a **cash allocation** (the route), and **goal dependencies**, so the roadmap stops being
a flat list.

It also fixes a latent double-count: `Available Capacity` already excludes `Mandatory Payment`, but
the debt projection consumes `plannedPayment`, which may exceed the mandatory minimum. If the whole
`Available Capacity` were routed to goals, the surplus would be spent twice.

---

## Clarifications

### Session 2026-10-19

- Q: Which money does allocation partition, and how is `plannedPayment − minimumPayment` handled so it is not counted twice? → A: `Available Capacity` is the single pool. `DecimalExtra = plannedPayment − minimumPayment` is an allocation funded from it, never a second pool. Without an explicit rule the 002 planned extra is the default debt allocation; an explicit rule replaces it (never adds to it); the debt projection uses `MinimumPayment + FundedExtra`, which is what the route actually funded.
- Q: When the rules request more than `Available Capacity`, how is it split? → A: Priority-based waterfall, never pro-rata. `Funded = min(Requested, RemainingNeed, RemainingCapacity)`; leftover need of a completing destination cascades to the next destination in the same period; destinations past exhausted capacity receive 0 and are reported as shortfall; the plan is disclosed, not rejected.
- Q: May 008 change the ETA/status of `GET /api/v1/gps`? → A: Yes, deliberately: the GPS ETA becomes route-aware and must be simulated period by period. 004's contract is amended first. No second, parallel default ETA algorithm. "Not yet funded because a higher priority destination comes first" is not "no Available Capacity".
- Q: Should `TimelineChange` store absolute values or multipliers, and are debt lines in scope? → A: Absolute `SET_VALUE` only; the UI may accept "+10%" and stores the computed amount. In scope: income, expense, debt annual interest rate, debt minimum payment. `plannedPayment` is NOT a timeline change (it belongs to allocation). One-off lump sums and future balance edits are out.
- Q: Is the route's "emergency fund" step a Feature 003 goal or the profile's `emergencyFundAmount`? → A: A Feature 003 `GoalType = EMERGENCY_FUND` goal, using `Goal.currentAmount`; the profile balance is never added, auto-synced, or silently preferred. No such goal → the step is skipped; no months-of-expense formula is invented.

No other question was asked (SpecKit clarification cap: 5). The remaining items are listed in
§12 "Deferred decisions" with proposed defaults and in `open-decisions.md` §4.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record future income/expense/debt changes (Priority: P1)

As a user, I record that my salary will rise, my rent will change, or my loan rate will move from a
specific month, so projections reflect my actual plans instead of assuming the present repeats.

**Why this priority**: a flat "future = present" assumption is the largest source of inaccurate
ETAs; scenario planning on top of it is built on a false base.

**Independent Test**: add "salary 81.4 from 2027-04" and confirm the projected cash flow is
unchanged before 2027-04 and higher from 2027-04 onward.

**Acceptance Scenarios**:

1. **Given** a salary line of 74, **When** the user records 81.4 effective 2027-04 (entered as
   "+10%", stored as the absolute value), **Then** periods before 2027-04 use 74, periods from
   2027-04 use 81.4, and the change is labelled `user-assumed`.
2. **Given** an expense change effective 2027-02, **When** the GPS is projected, **Then** the change
   affects only periods from its effective date.
3. **Given** no recorded change for a line, **When** the projection runs, **Then** the flat value is
   used and explicitly labelled "assumed unchanged" (never presented as a forecast).
4. **Given** a debt rate change effective 2027-06, **When** the route is projected, **Then**
   interest, payoff date, and ETA change only from 2027-06, and the earlier periods keep the old
   rate.
5. **Given** a change dated before `asOf`, **When** the projection runs, **Then** it applies from
   the first projected period and keeps its provenance; no already-reported month is rewritten.

---

### User Story 2 - See and control where monthly cash goes (Cash Allocation) (Priority: P1)

As a user, I can see and adjust how my Available Capacity is routed each month (debt extra,
emergency fund, goals) so the GPS describes a real route, not just a balance.

**Why this priority**: this is the product's central route concept. Without it the GPS only reports
"you have X capacity" and never "X goes here, then there".

**Independent Test**: with Available Capacity 24 and a debt whose planned extra is 7, confirm only
17 reaches other destinations; then add an explicit debt-extra rule of 15 and confirm the default 7
is replaced, not added.

**Acceptance Scenarios**:

1. **Given** income 74, expense 30, minimum payment 20, planned payment 27, **When** the route is
   computed, **Then** `Available Capacity = 24`, the debt extra row funds `7`, other destinations
   may receive at most `17`, and the debt amortizes on 27 — never on 20 + 24.
2. **Given** an explicit debt-extra rule of 15 (replacing a planned extra of 7) and an emergency
   fund rule of 9, **Then** the debt receives 15, the fund 9, the debt payment used for projection
   is `20 + 15 = 35`, and no third stream is created.
3. **Given** rules requesting 15 / 12 / 10 with capacity 24, **When** the waterfall runs, **Then**
   funded amounts are 15 / 9 / 0 and the shortfall of 3 + 10 is stated per destination.
4. **Given** a destination completes with only 8 of its 15 requested, **When** the period closes,
   **Then** the 7 residue is routed to the next eligible destination in the same period and the new
   route is disclosed.
5. **Given** a user-defined order differs from the default (debt extra → emergency fund → goals by
   priority), **When** the user overrides priority, **Then** the engine applies it and states which
   policy was used and why.
6. **Given** a selected goal that is not first in the route, **When** the GPS ETA is requested,
   **Then** the ETA reflects the periods in which that goal actually receives funding, not the whole
   Available Capacity.

---

### User Story 3 - Step through a goal prerequisite (Goal Dependency) (Priority: P1)

As a user, I declare that a goal can only start once a prerequisite is complete (e.g., an emergency
fund before a house fund), so the roadmap is a real sequence and not a list.

**Why this priority**: without dependencies every goal competes at once and the roadmap misleads;
ordering definitions are where route logic loses clarity fastest.

**Independent Test**: create goal B requiring goal A; with A incomplete, B receives no contribution;
complete A and watch B start in the following period.

**Acceptance Scenarios**:

1. **Given** goal B depends on goal A and A is incomplete, **When** a projection is made, **Then** B
   receives 0, the result states the `PREREQUISITE_INCOMPLETE` blocker, and B's required capacity is
   still reported (the future load stays visible).
2. **Given** A completed in period *k*, **When** period *k + 1* is projected, **Then** B becomes
   eligible and receives the freed capacity.
3. **Given** a cycle A→B→A (or self-loop A→A), **When** the dependency is created, **Then** the
   request is rejected `400 VALIDATION_FAILED` with `DEPENDENCY_CYCLE`; **When** legacy data already
   contains one, **Then** the projection returns `BLOCKED` with `DEPENDENCY_CYCLE` and never loops.
4. **Given** goal B has priority 1 and its prerequisite A has priority 5, **When** the route is
   computed, **Then** dependency acts as an eligibility gate (A is funded, B is not) and the applied
   override is disclosed.

### Edge Cases

- A change effective before `asOfDate` applies from the first projected period; it is never applied
  retroactively to already-reported months without a labelled audit entry.
- Requested allocation above `Available Capacity` is truncated by the waterfall and reported as
  shortfall — the engine never allocates imaginary money and never rejects the plan for this.
- `Available Capacity = 0` means every optional allocation is `0.00`; no negative or over-budget
  funding is produced, and the shortfall is explained.
- Negative Net Cash Flow means zero discretionary allocation; the result is `BLOCKED` with the
  existing `NO_AVAILABLE_CAPACITY` reason and no fabricated route.
- A route-funded debt payment below its accrued interest is a route blocker with its own code, and
  is never presented as the stored 002 plan. (Deferred detail: D-04.)
- When every destination is complete or none is eligible, the whole capacity is reported as
  `unallocatedAmount`, labelled; it is never routed to overflow.
- A destination that never receives funding because earlier destinations never complete yields
  `UNAVAILABLE` with a stated reason (horizon or never-funded) — never a fabricated date and never
  confused with "no Available Capacity".
- A timeline change plus a scenario stays isolated: a scenario's timeline is a projection only
  (`006-scenario-planning`).
- A `DEBT_FREE` goal never receives a cash contribution; its completion remains the 002 portfolio
  fact, dynamic and not sticky.
- A completed or archived goal is not an allocation destination; archived is terminal.
- Two changes for the same line with the same `effectiveFrom` are rejected, not resolved by order.
- A route that cannot converge within the period guard returns `UNAVAILABLE` with
  `PROJECTION_HORIZON_EXCEEDS_MAXIMUM`.

---

## Requirements *(mandatory)*

### Money basis and debt repayment

- **FR-001**: The system MUST treat `Available Capacity` as the single pool for discretionary
  allocation. `Mandatory Payment` is already removed by `Net Cash Flow` and MUST NOT be modelled as
  an allocation destination.
- **FR-002**: The system MUST treat each ACTIVE debt's `DecimalExtra = max(plannedPayment −
  minimumPayment, 0)` as an allocation funded from `Available Capacity`, never as a second source
  of money.
- **FR-003**: When no explicit allocation rule targets a debt, the system MUST use that debt's
  planned extra as the default debt allocation.
- **FR-004**: When an explicit allocation rule targets a debt, the system MUST replace that debt's
  default planned extra (never add to it), for the periods in which the rule is in force.
- **FR-005**: The debt projection used by the route MUST amortize on
  `EffectivePayment = minimumPayment + FundedExtra`, with `FundedExtra` capped by remaining
  capacity and by the debt's remaining balance plus accrued interest. A projection MUST NOT
  amortize on a payment the route did not fund, and MUST NOT use `plannedPayment` and the funded
  extra at the same time.
- **FR-006**: The system MUST NOT write `plannedPayment`, `outstandingBalance`, or any other stored
  debt fact while producing a route (projection isolation).

### Timeline

- **FR-007**: The system MUST let a user attach a change to an income line, an expense line, a debt
  annual interest rate, or a debt minimum payment, with an `effectiveFrom` date, and MUST project
  only from that date onward.
- **FR-008**: A timeline change MUST store an absolute value (`SET_VALUE`). Multipliers, one-off
  lump sums, future balance edits, and `plannedPayment` changes are out of scope for v1.
- **FR-009**: The effective value for a period MUST be the change with the latest `effectiveFrom`
  on or before the period start; two changes for one line with the same `effectiveFrom` MUST be
  rejected as a conflict.
- **FR-010**: `frequency` MUST be `MONTHLY` only; partial months are not modelled.
- **FR-011**: A line with no change MUST keep its flat value and be labelled "assumed unchanged".
- **FR-012**: The system MUST compute `Income`, `Expense`, `Mandatory Payment`, `Net Cash Flow`, and
  `Available Capacity` per period using the existing `CashFlowCalculator`; it MUST NOT introduce a
  second cash-flow formula.

### Allocation order and execution

- **FR-013**: The system MUST keep an ordered allocation: tier 1 committed debt extra, tier 2
  emergency-fund goal, tier 3 goals by priority, tier 4 undated goals. Rate-based reordering of
  debts is out of scope (Feature 005).
- **FR-014**: Within a tier, order MUST be deterministic and stable: `(priority, createdAt, id)`,
  falling back to `id` when `createdAt` is absent. Query order MUST NOT affect the result.
- **FR-015**: Allocation MUST be a priority waterfall:
  `Funded = min(Requested, RemainingNeed, RemainingCapacity)`, with capacity decremented after each
  destination. Pro-rata splitting is prohibited in v1.
- **FR-016**: A destination that needs less than it requested MUST pass the residue to the next
  eligible destination in the same period.
- **FR-017**: Destinations beyond exhausted capacity MUST receive `0` and be reported as shortfall;
  the plan MUST NOT be rejected for being underfunded.
- **FR-018**: Each allocation row MUST expose `requestedAmount`, `fundedAmount`,
  `shortfallAmount`, `priority`, and a machine-readable `reason`.
- **FR-019**: The sum of `fundedAmount` across a period MUST never exceed that period's
  `Available Capacity`.
- **FR-020**: Capacity that no destination can absorb MUST be reported as `unallocatedAmount`; it
  MUST NOT be routed to a completed destination or turned into a savings movement.
- **FR-021**: The emergency fund MUST be a Feature 003 goal (`GoalType = EMERGENCY_FUND`) using
  `Goal.currentAmount`. The profile's `emergencyFundAmount` MUST NOT be added, auto-synced, or
  silently preferred. With no such goal the step is skipped and no target is invented.

### Goal dependencies

- **FR-022**: The system MUST record goal dependencies `(successorGoal, prerequisiteGoals, type)`
  with `type = FINISH_TO_START` only in v1, and MUST validate them as a DAG.
- **FR-023**: A dependency MUST act as an eligibility gate evaluated at the start of each period.
  `priority` orders only eligible destinations; an ineligible destination receives `0` and MUST
  still report its required capacity.
- **FR-024**: A cycle, including a self-loop, MUST be rejected on write with
  `400 VALIDATION_FAILED` / `DEPENDENCY_CYCLE`, and MUST be reported as `BLOCKED` with
  `DEPENDENCY_CYCLE` if it is nevertheless encountered during projection. The engine MUST NOT loop.
- **FR-025**: A dependent goal MUST NOT start in the period in which its prerequisite completes; it
  becomes eligible from the following period.
- **FR-026**: v1 has no override for an incomplete prerequisite; the only ways to unblock a
  dependent goal are to complete or archive the prerequisite.

### Route, ETA, and status

- **FR-027**: The route MUST be the per-period, priority-ordered funding of destinations plus the
  reallocation events produced when a destination completes. It MUST be part of the GPS result.
- **FR-028**: The GPS ETA for a destination MUST be derived by simulating the route period by
  period until that destination completes. It MUST NOT be derived from the current period's
  allocation alone, and MUST NOT assume the whole `Available Capacity` belongs to it.
- **FR-029**: A destination that is merely not yet funded (a higher priority destination comes
  first) MUST NOT be reported as having no Available Capacity. `UNAVAILABLE` is reserved for a
  genuinely absent finite route: a blocker, a cycle, an exceeded horizon, or a destination that
  never receives funding.
- **FR-030**: Status MUST use the existing five-status precedence and lateness bands with the
  route-aware period count; no new status and no numeric health score may be introduced.
- **FR-031**: Every route result MUST state the applied order policy and why: the ordered
  destination list, the policy code, the applied overrides (user priority, dependency gate), and
  the `latenessTolerance` used. (Constitution XII, SC-005.)

### Provenance, determinism, data, and access

- **FR-032**: Every timeline change, allocation rule, dependency, and allocation row MUST carry the
  `actual` / `user-assumed` / `calculated` label; a user-created change is `user-assumed` with
  source `USER_SUPPLIED`.
- **FR-033**: Identical inputs, assumptions, `asOfDate`, and policy MUST produce an identical route,
  ETA, and status. No clock, randomness, network, or AI inside the calculation.
- **FR-034**: Money MUST remain `BigDecimal` scale 2 (decimal strings on the wire), rates scale 6,
  ratios scale 4. Allocation amounts MUST be rounded once, at the declared boundary, and the sum of
  funded amounts MUST equal the allocated total exactly.
- **FR-035**: The route MUST be bounded by a documented period guard (proposed: 360 monthly periods,
  mirroring the 002 safety limit) and MUST terminate deterministically when the guard is reached.
- **FR-036**: Timeline changes, allocation rules, and dependencies MUST be owner-scoped, MUST NOT
  accept an owner id from the client, MUST return `404 RESOURCE_NOT_FOUND` for cross-owner access,
  MUST require CSRF on mutations, and MUST participate in the owner export/delete lifecycle.
- **FR-037**: This feature MUST NOT add roadmap stages (005), scenario isolation (006), snapshots
  or a change ledger (009), avalanche/snowball strategy (005), AI forecasting, inflation, or market
  returns.

### Key Entities *(include if feature involves data)*

- **TimelineChange** — `(lineType: INCOME | EXPENSE | DEBT_RATE | DEBT_MINIMUM_PAYMENT, lineId,
  effectiveFrom, operation: SET_VALUE, amount|rate, currency, source, createdAt)`. Owner-scoped;
  `frequency` fixed to `MONTHLY`. One in force per line per period.
- **CashAllocationRule** — `(destinationType: DEBT | GOAL, destinationId, amount, priority,
  effectiveFrom, source)`. `sourceCash` is always `Available Capacity`. Fixed money amount only in
  v1. Replaces, never augments, a debt's default planned extra.
- **GoalDependency** — `(successorGoalId, prerequisiteGoalIds[], type: FINISH_TO_START)`. Must be a
  DAG; self-loops and cycles rejected.
- **AllocationPlan / Route** — `(asOf, policyDisclosure, period rows, unallocatedAmount,
  reallocationEvents[], per-destination ETA, horizon)`. Read-only, recomputed per request, never
  persisted.
- **AllocationRow** — `(destinationType, destinationId, label, tier, priority,
  requestedAmount, fundedAmount, shortfallAmount, reason, provenance)`.
- **ReallocationEvent** — `(period, fromDestination, toDestination, amount, trigger)` where
  `trigger ∈ {DESTINATION_COMPLETED, RESIDUE_CASCADED, ELIGIBILITY_STARTED}`.

---

## Allocation algorithm (normative, deterministic)

This is behaviour, not implementation guidance. It exists so the engine, the oracle, and the tests
cannot drift apart.

```text
period p = 0, 1, 2, ...   (anchored at asOfDate, MONTHLY)

1. resolve effective lines for p          -> TimelineChange resolver (§FR-007..FR-011)
2. NetCashFlow_p  = Income_p − Expense_p − MandatoryPayment_p        [CashFlowCalculator.combine]
   AvailableCapacity_p = max(NetCashFlow_p, 0)
3. eligible = destinations whose prerequisites were COMPLETED at the START of p
4. ordered  = eligible sorted by (tier, priority, createdAt, id)
5. capacity = AvailableCapacity_p
   for each d in ordered:
       requested_d = min(ruleAmount_d, remainingNeed_d)
                     where remainingNeed_debt = openingBalance + monthlyInterest_p (clamped)
                           remainingNeed_goal = remaining
       funded_d    = min(requested_d, capacity)
       capacity   -= funded_d
   unallocated_p = capacity
6. apply: goal.contributed += funded ; debt.EffectivePayment = minimumPayment + funded
7. amortize each debt on EffectivePayment and the period's rate   [DebtPayoffCalculator]
8. evaluate completions at END of p; record ReallocationEvent
9. stop when every destination is COMPLETE, or the period guard is reached
```

Consequences that MUST hold and be tested:

- The residue of a completing destination is reused **in the same period** (FR-016).
- A dependent becomes eligible **the period after** its prerequisite completes (FR-025).
- `Σ funded_d ≤ AvailableCapacity_p` in every period (FR-019).
- The debt projection consumes the same `funded_d` the allocation produced (FR-005).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Timeline cases `TM-001`, `TM-002`, `TM-004..TM-009` pass: projections differ exactly at
  the effective date, never before it, and a line with no change stays flat. (`TM-003` is a tombstone:
  an extra debt payment is an allocation, not a timeline change.)
- **SC-002**: The debt-extra cases `AL-007..AL-010` pass: `Available Capacity` is partitioned once,
  the planned extra is replaced (not added), shortfalls are recorded, and the debt projection and
  the allocation share one `FundedExtra`.
- **SC-003**: The waterfall cases `AL-011..AL-015` pass: requests 15/12/10 with capacity 24 yield
  15/9/0, residue cascades in-period, capacity 0 yields all zeros, `Σ funded ≤ capacity`, and
  repeated runs are byte-identical.
- **SC-003a**: `AL-017` passes: when nothing is eligible or everything is complete, the whole capacity
  is reported as `unallocatedAmount` and never routed.
- **SC-004**: `AL-002` (freed capacity reassigned) and `AL-003`/`AL-016` (dependency holds, no silent
  start, required capacity still shown) pass deterministically.
- **SC-005**: `AL-004` and `AL-006` pass: a cycle and a self-loop are rejected or blocked without the
  engine looping.
- **SC-006**: 100% of route results state the applied order policy, the ordered destinations, and
  the applied overrides.
- **SC-007**: For 100 representative input sets, recalculating with the same inputs, assumptions,
  `asOfDate`, and policy yields an identical route, ETA, and status.
- **SC-008**: No route ever reports a funded amount above the period's `Available Capacity`, and no
  ETA is fabricated for an unfunded destination.

---

## Assumptions

- The user chooses an `asOf` date; the system clock only seeds it once at the boundary. Results stay
  deterministic for the same `asOf`.
- This feature introduces the time and allocation model. Rich historical backfill ("what would have
  happened last year") is out of scope.
- Timeline and allocation feed a deterministic projection; they do not introduce AI forecasting.
  Inflation, market returns, and income growth beyond recorded changes remain out of scope.
- The monthly cadence is `MONTHLY`; partial months are not modelled.
- The default route order is a documented conservative default, not a hidden truth; users may
  reorder goals, and the applied order is always disclosed.
- 008 owns the route and the route-aware ETA; 004 owns the selected-destination presentation and the
  status bands; 005/006/009 consume the route without redefining it.

## Out of scope (v1)

- Debt payoff strategy optimization (avalanche / snowball / refinancing) — Feature 005.
- Rate-threshold debt-vs-goal reordering — needs its own documented policy value and oracle case.
- Pro-rata allocation, percentage-based rules, and per-destination minimum contributions.
- Multipliers/relative timeline changes, one-off lump sums, future balance edits, and
  `plannedPayment` as a timeline change.
- Auto-deriving an emergency-fund target from N months of expenses, and auto-syncing the profile's
  `emergencyFundAmount` with a goal's `currentAmount`.
- Overriding an incomplete prerequisite.
- Any AI-computed money, ETA, or route.

## Deferred decisions

Full rationale, options, and acceptance criteria for each item live in `open-decisions.md` §4.
These do not block understanding the requirement; they block freezing the domain model.

| ID | Question | Proposed default |
|---|---|---|
| D-04 | Status of a route-funded debt payment below accrued interest | `BLOCKED` with a distinct `ROUTE_PAYMENT_BELOW_INTEREST` code |
| D-05 | Per-debt order when capacity cannot fund every debt extra | explicit rules carry a user priority; implicit extras over capacity fail closed with `PLANNED_PAYMENTS_EXCEED_CAPACITY` |
| D-06 | Route payload granularity and horizon | current-period rows + reallocation events + per-destination ETA; guard 360 periods |
| D-07 | Does a dependency override priority | eligibility gate, not reordering |
| D-08 | Cycle handling timing | reject on write, block on read |
| D-09 | Dependency edge type | `FINISH_TO_START` only |
| D-10 | Prerequisite override | removed from v1 |
| D-11 | Unallocated capacity | reported and labelled, never routed |
| D-12 | Undated goals ordering | priority order, same as dated |
| D-13 | `DEBT_FREE` goals as cash destinations | excluded; condition only |
| D-14 | Allocation rule end date | no end date; a later rule supersedes |
| D-15 | Conflicting timeline changes | latest wins; same-date conflict rejected |
| D-16 | Rule amount model | fixed money only |
| D-17 | Engine hosting (extend existing orchestration vs promote `FinancialEngine`) | extend existing orchestration; resolve the `engine-contract.md` divergence in the plan |
| D-18 | API and persistence surface | three owner-scoped resources + `allocation` section in the GPS result + three migrations + export sections |
| D-19 | May 008 write `plannedPayment` | no; projection-only |
| D-20 | Provenance of timeline changes | `user-assumed` / `USER_SUPPLIED`; reuse `FinancialAssumption` |
