# Feature 008 — Requirements Analysis & Decision Backlog

**Status**: Analysis complete, 5 blocking decisions ratified (2026-10-19). No code, plan, or tasks
produced. This file is an **analysis artifact**, not a SpecKit `plan.md`/`research.md` output.

**Document amendments applied with this analysis** (2026-10-19):

- `specs/financial-domain/reference-cases.md` — `TM-001`/`TM-002` restated as `SET_VALUE`,
  `TM-003` deprecated (extra debt payment is an allocation), `TM-004..TM-009` and
  `AL-007..AL-017` added.
- `specs/financial-domain/calculation-rules.md` — §0 gained `Debt Extra`, `Funded Extra`,
  `Effective Payment`, `Shortfall`, `Unallocated Amount`; §4 pins the payment source and drops lump
  sums; §5 gained the emergency-fund rule; §7/§8 rewritten to the waterfall and the v1 order
  (rate-based reordering removed); §9 drops the prerequisite override; §10 pins `SET_VALUE`.
- `specs/financial-domain/data-model.md` — `TimelineChange` kinds/`operation`, `CashAllocationRule`
  fields, `AllocationResult` rows, and the allocation validation rules aligned; implementation delta
  noted for `FinancialPolicy`/`Portfolio.goals`.
- `specs/financial-domain/contracts/engine-contract.md` — `AllocationCalculator.route` pinned to the
  waterfall and `DebtExtraResolver` added; implementation delta noted (D-17).
- `specs/004-financial-gps/spec.md` — §2.2, §7.3, §9.2, §9.3, §13, §16 (D-7), §17, §18 amended and a
  new §19 records the route-aware ETA supersession and its preconditions.
- `docs/development/agent-notes.md` — three findings recorded (engine contract vs implementation,
  `Portfolio.goals` typing, `FinancialPolicy` scope).

**Purpose**: before an architecture design (Archify), make explicit

1. what is already decided by the normative contracts,
2. what already exists in `backend/src/main/java` and must be reused, not rebuilt,
3. which business decisions are still missing, with options, a proposed default, and testable
   acceptance criteria.

**Normative sources consulted**

- `.specify/memory/constitution.md` (v1.2.0, principles I–XIV)
- `specs/financial-domain/calculation-rules.md`, `status-rules.md`, `reference-cases.md`,
  `data-model.md`, `contracts/engine-contract.md`
- `specs/001-financial-profile/spec.md`, `specs/002-debt-management/spec.md`,
  `specs/003-financial-goals/spec.md`, `specs/004-financial-gps/spec.md`
- `specs/005-financial-roadmap/spec.md`, `specs/006-scenario-planning/spec.md`,
  `specs/009-financial-review/spec.md`
- Implementation: `domain/`, `application/gps/`, `application/goal/`, `infrastructure/`,
  `db/migration/V1..V9`

---

## 1. Classification of every requirement

### 1.1 Already decided (normative — do not re-decide)

| # | Decision | Source |
|---|---|---|
| N-01 | `Income`, `Expense`, `Mandatory Payment`, `Net Cash Flow`, `Available Capacity`, `Allocation`, `Contribution` are fixed terms | calculation-rules §0 |
| N-02 | `Net Cash Flow = Income − Expense − Mandatory Payment`; `Available Capacity = max(NetCashFlow, 0)` | §3 |
| N-03 | Monthly simple amortization, final-payment clamp, no negative balance | §4 |
| N-04 | `remaining`, `progress`, `requiredMonthlyCapacity` (`CEILING`) | §5 |
| N-05 | Contribution capped by `remaining`; ETA = earliest month reaching `remaining` | §6 |
| N-06 | Default route order: debt → emergency fund → prioritized goals → remaining goals; the applied order MUST be disclosed | §7, §8 |
| N-07 | Freed capacity MUST be reassigned when a destination completes (route, not a static split) | §7, constitution XII |
| N-08 | Dependency is a DAG; cycle → validation `BLOCKED`, never loop | §9 |
| N-09 | `frequency` is `MONTHLY` only in v1 | §10 |
| N-10 | A timeline change is never silently extrapolated; future ≠ present only when declared | §10 |
| N-11 | Provenance labels `actual` / `user-assumed` / `calculated` on every value | §11 |
| N-12 | Status set and precedence: `COMPLETED → BLOCKED → ON_TRACK → AT_RISK → OFF_TRACK` | status-rules |
| N-13 | `DEBT_FREE` completion reads the 002 portfolio status; dynamic, not sticky | calculation-rules §5, 004 D-6 |
| N-14 | Goal ordering key `(priority, createdAt, id)`, lower = first | 003 §4.4 |
| N-15 | `plannedPayment >= minimumPayment`; surplus is "voluntary acceleration funded from Available Capacity" | 002 §4.2, SC5.3 |
| N-16 | Projections never mutate persisted actual state | constitution VI, calculation-rules §11 |
| N-17 | Engine signature `calculate(inputs, assumptions, asOf, policy)`, identity-free, no clock/AI | constitution XIV, engine-contract |
| N-18 | `AL-001..AL-006`, `TM-001..TM-003`, `status-001..014` are the acceptance oracle | reference-cases |
| N-19 | **Amended 2026-10-19**: the oracle now carries `TM-001`, `TM-002`, `TM-004..TM-009` and `AL-001..AL-017`; `TM-003` (extra debt payment as a timeline change) is a deprecated tombstone, re-homed to `AL-007..AL-010` | reference-cases §G/§H |

### 1.2 Already in the codebase (reuse; never re-implement)

| Capability | Owner (existing code) | 008 usage |
|---|---|---|
| Effective-dated line inclusion | `domain/model/EffectiveDating.isEffectiveOn(active, effectiveFrom, asOf)` | timeline resolution primitive |
| Income/Expense/Mandatory/NetCF/AvailableCapacity | `domain/finance/CashFlowCalculator.calculate/combine` | per-period cash base |
| Amortization, final clamp, blockers | `domain/debt/DebtPayoffCalculator.project/schedule`, `DebtCalculationPolicy` | per-period debt projection with the funded payment |
| Portfolio status + blocker propagation | `domain/debt/DebtSummaryCalculator` | `DEBT_FREE` destination and blocker codes |
| `remaining`, `progress`, required capacity, `monthsRemaining` | `domain/goal/GoalProgressCalculator`, `GoalCapacityCalculator` | destination need |
| Goal ordering | `Goal.ORDERING` | allocation order |
| `DEBT_FREE` reconciliation | `application/goal/usecase/GoalLifecycle` | read-only completion fact |
| Status bands + tolerance | `domain/gps/GpsStatusPolicy`, `GpsStatus.Policy` | route status |
| ETA period math | `GpsEtaCalculator.ceilPeriods` | single-destination degenerate case |
| Money / Rate / precision | `domain/model/Money`, `domain/debt/Rate` | all arithmetic |
| Provenance / blockers / next actions / explanations | `domain/gps/GpsProvenance`, `GpsBlockerFactory`, `GpsExplanationFactory` | result assembly |
| Owner isolation, 404 hiding, export sections, CSRF | `platform/security`, `application/account`, `api/common` | new resources must plug in |

### 1.3 True gaps (neither normatively decided nor implemented)

| # | Gap | Why it blocks design |
|---|---|---|
| G-01 | No `TimelineChange`, `CashAllocationRule`, `GoalDependency` type, table, port, or API | whole feature |
| G-02 | No allocation/projection orchestrator; `FinancialEngine` computes cash flow only | no place to host the route loop |
| G-03 | `Portfolio.goals` is `List<Object>` — goals are untyped in the engine input | cannot allocate to typed destinations |
| G-04 | `FinancialPolicy` has only two rounding modes — no `AllocationPolicy`/`DebtPolicy`/`StatusPolicy` | policy is not expressible; the engine contract claims it exists |
| G-05 | `GpsRouteContext` hardcodes `SINGLE_DESTINATION` | route cannot be described |
| G-06 | `GpsEta.Reason` and blocker codes are closed enums with no route reasons | new machine-readable reasons need a contract change |
| G-07 | Debt amortization accepts one constant rate/payment — no dated rate or payment schedule | blocks the ratified debt-timeline decision |
| G-08 | No horizon guard for a route that never converges | risk of unbounded simulation |
| G-09 | 004's `contracts/rest-api.md` documents a single-destination ETA | must be updated before implementation |

---

## 2. Formula ownership audit — "no duplicated money formula"

The single rule 008 must obey: **008 may only partition money. It must never recompute a quantity
another feature already computes.**

| Quantity | Computed once in | 008 must |
|---|---|---|
| `Income`, `Expense` | `CashFlowCalculator` | pass in the period's effective lines |
| `Mandatory Payment` | `CashFlowCalculator` (Σ ACTIVE `minimumPayment`) | subtract once; never model it as a destination |
| `Net Cash Flow`, `Available Capacity` | `CashFlowCalculator.combine` | consume the result |
| Monthly interest, principal split, payoff date, final clamp | `DebtPayoffCalculator` | pass `minimumPayment + fundedExtra` and the period's rate; never re-derive |
| Portfolio debt-freedom status | `DebtSummaryCalculator` | read for `DEBT_FREE` goals |
| `remaining`, `progress` | `GoalProgressCalculator` | consume |
| `requiredMonthlyCapacity`, `monthsRemaining` | `GoalCapacityCalculator` | consume for reporting |
| Status bands, lateness | `GpsStatusPolicy` | feed a route-aware `etaPeriods` |
| `ceil` period counts | `GpsEtaCalculator.ceilPeriods` | reuse (degenerate single-destination case) |

**008 introduces exactly four new computations**: per-period partition, dependency eligibility,
period slicing by effective dates, and reallocation events. Nothing else.

### 2.1 The double-count defect found in the current spec

`Available Capacity` already excludes `minimumPayment`, but the debt projection consumes
`plannedPayment` (which includes the surplus). Therefore routing the whole `Available Capacity` to
goals pays the surplus twice.

Worked example (ratified): income 74, expense 30, minimum 20, planned 27 →
`Available Capacity = 24`; `Debt Extra = 27 − 20 = 7`; only `24 − 7 = 17` may reach other
destinations; the debt amortizes on 27, not 20 + 24.

---

## 3. Ratified decisions (2026-10-19)

### C-01 — One money source; the debt extra is an allocation, not a pool

- `Extra_i = max(plannedPayment_i − minimumPayment_i, 0)` is funded **from `Available Capacity`**.
- Without an explicit debt-extra rule, the 002 planned extra is the default debt allocation.
- With an explicit rule for debt *i*, that rule **replaces** (never adds to) the default extra.
- `EffectivePayment_i = MinimumPayment_i + FundedExtra_i`; `FundedExtra` is capped by capacity
  and by the debt's remaining balance + accrued interest.
- A projection must never amortize on a payment the route did not fund.

**AC**: `AL-007..AL-010` (§5 of the spec).

### C-02 — Priority-based waterfall, no pro-rata

- `Funded_i = min(Requested_i, RemainingNeed_i, RemainingCapacity_i)`, capacity decremented after
  each destination.
- A destination that needs less than requested passes the residue to the next destination **in the
  same period**.
- Rules beyond exhausted capacity receive `0` and are reported as shortfall; the plan is not
  rejected.
- Each row exposes `requestedAmount`, `fundedAmount`, `shortfallAmount`, `priority`, `reason`.
- Shortfall means "the plan is underfunded", not new debt and not lost money.

**AC**: `AL-011..AL-015`.

### C-03 — 008 owns the GPS ETA; 004 is amended, not forked

- `GET /api/v1/gps` keeps its endpoint and response shape, but ETA/status become **route-aware**.
- ETA must be simulated over periods, never inferred from the current period's allocation.
- "Not yet funded because a higher priority destination comes first" **≠** "no Available Capacity".
- `UNAVAILABLE` only when no finite route exists (blocker, cycle, horizon, or a destination that
  never receives funding while earlier destinations never complete).
- No parallel second default ETA algorithm; 004's `VR-*` cases are re-baselined as the degenerate
  case "one destination, no debt extra, no competing destination".
- 004 spec and `contracts/rest-api.md` MUST be amended before implementation.

### C-04 — Timeline changes store absolute values; debt lines are in scope

- `operation = SET_VALUE` only in v1. The UI may accept "+10%" and computes the absolute amount
  before saving; the domain never interprets multipliers.
- In scope: income amount, expense amount, **debt annual interest rate**, **debt minimum payment**.
- Out of scope: `plannedPayment` as a timeline change (it belongs to allocation), future balance
  edits, one-off lump sums, and `MULTIPLY`.
- Changing `plannedPayment` must not create a second extra-payment stream.

**AC**: `TM-001..TM-008`.

### C-05 — The emergency fund is a Feature 003 goal

- `GoalType = EMERGENCY_FUND` is the only emergency-fund destination.
- `Goal.currentAmount` is the accumulated amount; the profile's `emergencyFundAmount` is never
  added, never auto-synced, and never silently preferred.
- No `EMERGENCY_FUND` goal ⇒ the step is skipped (no invented target, no months-of-expense formula).
- Completion cascades to the next destination.

---

## 4. Remaining decisions (proposed defaults, not yet ratified)

Each item is written so it can be answered with one of the options and then pinned as an oracle
case. Items marked **BLOCKING** must be closed before Archify freezes the domain model.

### D-04 (BLOCKING) — What status does an under-funded debt route have?

If the route funds less than the debt's accrued interest, the debt never amortizes **under this
plan** — while the stored 002 plan (with the real `plannedPayment`) is still feasible.

| Option | Behaviour |
|---|---|
| A **(proposed)** | Route `BLOCKED` with a *distinct* code `ROUTE_PAYMENT_BELOW_INTEREST`, explanation naming the funded amount vs interest, and provenance labelled route-projection. The 002 stored projection is unaffected. |
| B | Report a shortfall only; keep the 002 status. Risk: projects a payoff the route cannot fund (violates I/III). |
| C | Treat it as a plan-validation error (400) and refuse the route. |

**Proposed default**: A. **AC**: cap 5, debt interest 10, funded extra 0 → status `BLOCKED`,
reason `ROUTE_PAYMENT_BELOW_INTEREST`, 002 stored projection still `AVAILABLE`.

### D-05 (BLOCKING) — Per-debt order when capacity cannot fund every debt extra

The ratified rule forbids silently choosing avalanche, snowball, or an even split.

| Option | Behaviour |
|---|---|
| A **(proposed)** | Explicit debt rules carry a user-set `priority` (required). Implicit extras (from `plannedPayment`) have no user order, so if `Σ implicit extras > Available Capacity` the route is `BLOCKED` with `PLANNED_PAYMENTS_EXCEED_CAPACITY` and a next action (reduce a planned payment or set an explicit priority). |
| B | Deterministic machine order (rate desc, then balance desc, then id) — this *is* avalanche and belongs to 005. |
| C | Even split across debts — introduces rounding residue and hides the choice. |

**Proposed default**: A (fail closed; no invented strategy). **AC**: two debts, extras 20 and 10,
capacity 24 → `BLOCKED`, reason `PLANNED_PAYMENTS_EXCEED_CAPACITY`, no debt funded.

### D-06 (BLOCKING) — Route payload granularity and horizon

| Option | Behaviour |
|---|---|
| A **(proposed)** | Current-period rows + `reallocationEvents` + per-destination ETA + `horizon{maxPeriods, reached}`; no full month-by-month schedule in the default payload. |
| B | Full schedule every request — unbounded payload (360 rows × destinations). |
| C | Current period only, no per-destination ETA — cannot answer "when does goal B start?". |

**Proposed default**: A, `maxPeriods = 360` (mirrors 002's safety limit). Exceeding it ⇒
`UNAVAILABLE` reason `PROJECTION_HORIZON_EXCEEDS_MAXIMUM`. **AC**: a destination that never
completes because no earlier destination ever completes ⇒ `UNAVAILABLE` with that reason, and the
call returns within the guard (no loop).

### D-07 — Does a dependency override priority?

| Option | Behaviour |
|---|---|
| A **(proposed)** | Dependency is a hard **eligibility gate**; `priority` only orders *eligible* destinations. An ineligible destination is shown with its required capacity but receives 0. |
| B | Dependency rewrites the priority order. |

**Proposed default**: A (matches calculation-rules §8 "may be overridden by a goal dependency" and
keeps 003's priority untouched). **AC**: B (priority 1) depends on A (priority 5) → A funded first,
B funded 0, `policyDisclosure.appliedOverrides` contains `DEPENDENCY_GATE`.

### D-08 — Cycle handling: reject at write, block at read?

**Proposed default**: both. Create/update rejects with `400 VALIDATION_FAILED` code
`DEPENDENCY_CYCLE` (self-loop `A→A` identical, `AL-006`); projection defensively reports
`BLOCKED` with `DEPENDENCY_CYCLE` for imported/legacy rows. **AC**: `AL-004`, `AL-006`.

### D-09 — Dependency edge type

**Proposed default**: single value `FINISH_TO_START`; keep the field for forward compatibility
(mirrors the `frequency = MONTHLY` treatment in calculation-rules §10). Any other value → 400.

### D-10 — "Explicit override" of an incomplete prerequisite (FR-005 today)

**Proposed default**: **remove from v1**. No override entity, no override flag; the only way to
start a dependent goal is to complete or archive the prerequisite. Rationale: constitution IX
(simple before intelligent); an override without an audited record is an unexplained route.

### D-11 — Unallocated capacity

**Proposed default**: report `unallocatedAmount` + a labelled explanation when every destination is
complete or none is eligible. Never route overflow into a completed destination, never create a
savings movement. **Contract note**: this adds a result field, not a new synonym of
`Available Capacity`; it must be declared in calculation-rules §0/§7 as a result quantity.
**AC**: cap 24, all destinations complete → `unallocatedAmount = 24.00`, `funded` sum = 0.

### D-12 — Ordering of undated goals

**Proposed default**: `priority` alone orders goals, whether dated or undated (a target date affects
status, never order). **AC**: P1 undated and P2 dated → P1 funded first.

### D-13 — Do `DEBT_FREE` goals participate as cash destinations?

**Proposed default**: no. They have no `remaining`; completion is a 002 portfolio fact. They appear
in the route as a condition/blocker, never as a receiving destination. **AC**: a `DEBT_FREE` goal
never receives `fundedAmount > 0`; its route status still derives from 002.

### D-14 — Allocation rule end date / reversion

**Proposed default**: no `effectiveTo` in v1; a later rule with a later `effectiveFrom` replaces the
earlier one for the same destination. **AC**: two rules for one goal (10 from Jan, 15 from Apr) →
12 in March, 15 in April; only one row is ever "in force".

### D-15 — Conflicting timeline changes

**Proposed default**: the latest `effectiveFrom ≤ periodStart` wins; two changes for the same line
with the *same* `effectiveFrom` are rejected `400 TIMELINE_CHANGE_CONFLICT`. A change dated before
`asOf` applies from the first projected period and is labelled with its source. **AC**:
`TM-004`, plus a 400 case.

### D-16 — Allocation rule amount model

**Proposed default**: fixed money amount only (`amount`); percentage-of-capacity rules rejected in
v1. **AC**: a rule declaring `amountType = PERCENTAGE` → 400 `ALLOCATION_AMOUNT_TYPE_UNSUPPORTED`.

### D-17 — Where the route lives (architecture, but it changes the requirement shape)

| Option | Behaviour |
|---|---|
| A **(proposed)** | Extend the existing application-layer GPS orchestration with a domain `AllocationPlanner`/`ProjectionEngine`; keep `FinancialEngine` as the cash-flow entry point (or promote it) — decision for Archify, guided by engine-contract's locked step order. |
| B | Make `FinancialEngine.calculate` the single entry point for 004 and 008 (contract-conformant, but today it returns only `CashFlowResult`; 004 already bypasses it). |

**Proposed default**: A with the engine-contract orchestration order preserved; the divergence
between `engine-contract.md` and the 004 implementation must be resolved explicitly in the plan.

### D-18 — New API surface and persistence

**Proposed default**: three owner-scoped resources — `/api/v1/timeline-changes`,
`/api/v1/allocation-rules`, `/api/v1/goal-dependencies`; `GET /api/v1/gps` gains an
`allocation` section and `routeContext.policy`; three new tables (`V10..V12`) with
`ON DELETE CASCADE`; three export sections for 007; every endpoint in the unauthenticated and
authorization sweeps; CSRF on mutations; cross-owner → 404.

### D-19 — Does 008 write `plannedPayment`?

**Proposed default**: no. Preferences expressed through allocation rules never mutate 002's stored
debt facts (constitution VI). The route is projection-only; the user edits debts in Feature 002.

### D-20 — Provenance of a timeline change

**Proposed default**: a user-created change is `user-assumed` with source `USER_SUPPLIED`; a change
derived from the system (e.g. a future AI-parsed intent, out of scope) would be
`SYSTEM_DEFAULT`; past-dated input facts remain `actual`. Reuse `FinancialAssumption` +
`GpsProvenance`; no new provenance vocabulary.

---

## 5. Defects and contradictions found

| # | Defect | Action |
|---|---|---|
| X-01 | 008 `spec.md` ends with a duplicated dangling fragment ("cycle as an invalid dependency…") | removed |
| X-02 | SC-001/SC-002 cite `TIM-001`/`TIM-002`; the oracle uses `TM-*` | corrected to `TM-*` |
| X-03 | US2 AC1 ("debt extra 15") ignores that the mandatory minimum is already subtracted and that `plannedPayment` may already carry extra | re-expressed via the funded-extra rule; see `AL-008` |
| X-04 | Edge case says "caps at Net Cash Flow (see `max(NetCashFlow,0)`)" — mixes two quantities | corrected to `Available Capacity` |
| X-05 | 008 says debt changes are in FR-001 but the `Debt` aggregate has one rate/payment | resolved by C-04 scope + G-07 implementation gap |
| X-06 | 005's default order ("debt elimination, emergency fund, then goals") differs from calculation-rules §8 ("interest-bearing debt…") | reconciled in §8 amendment; 005 remains a Draft |
| X-07 | calculation-rules §7 says "High-rate debt" with no threshold | amended: all committed debt extras are tier 1 in v1; rate-based reordering deferred to 005 |
| X-08 | `FR-005` allows an "explicit override" that has no entity or field | removed from v1 (D-10) |
| X-09 | 004 §2.2 declares allocation/timeline non-goals | amended to "superseded by 008" (C-03) |
| X-10 | `reference-cases.md` TM-003 makes an extra debt payment a timeline change | moved to allocation (`AL-008`); TM-003 deprecated |

---

## 6. Risks to close before Archify

| # | Risk | Impact | Mitigation |
|---|---|---|---|
| R-01 | ETA changes from closed-form `ceil(remaining/capacity)` to a multi-period simulation | breaks 004's oracle if not re-baselined | re-baseline `VR-*` as the one-destination/no-extra case; add allocation cases |
| R-02 | Double-counting the debt surplus | every route ETA and payoff date is wrong | C-01 + `AL-007..AL-010` as first tests |
| R-03 | `Portfolio.goals` is `List<Object>`; `FinancialPolicy` has no allocation/status policy; `FinancialResult` is a 4-field 001 record | the documented engine contract cannot host 008 | decide D-17; type the portfolio; introduce `AllocationPolicy` without breaking 22 `FinancialPolicy` call sites |
| R-04 | Debt amortization cannot express a dated rate/payment | C-04 cannot be implemented | extend the single `DebtPayoffCalculator` loop with a per-period rate/payment resolver; keep REF-D01..D09 green (single-segment = identity) |
| R-05 | Unbounded route (cycle or never-converging destinations) | hang / SC-004 failure | D-08 + D-06 horizon guard, both deterministic |
| R-06 | Route result could be huge and non-reproducible in byte terms | 004 SC-002 determinism | fixed ordering `(tier, priority, createdAt, id)`, stable decimation (D-06), no maps in the wire type |
| R-07 | Cross-feature ownership overlaps: 005 claims route ordering + avalanche/snowball; 006 claims "monthly contributions"; 002 SC5.3 claims the surplus belongs to the roadmap route | duplicated rules | ratify the ownership matrix in §7 below before design |
| R-08 | New persistence adds owner-scoping, export, cascade, and CSRF surface | security regression | reuse `OwnerDataSection`, `CurrentOwnerProvider`, unauthenticated/authorization sweeps |
| R-09 | New reason codes/fields are a breaking API change | client churn | additive only; keep existing reason codes and field names; declare `routeContext.policy` values |
| R-10 | `asOf` shifts re-interpret a future change as a past change | ETA drift that looks like a bug | `TM-*` cases must pin behaviour on both sides of `effectiveFrom` |
| R-11 | Money in the frontend: rule editor and route table | JS `number` for money | reuse `MoneyInput` + decimal strings; Vitest on the route table |
| R-12 | `createdAt` is null in the GPS path (004 passes null deliberately) | tie-break instability | state that ordering falls back to `id` when `createdAt` is null (already true in `Goal.ORDERING`) |

---

## 7. Proposed ownership matrix (needs ratification with 005/006)

| Concept | Owner | Consumers |
|---|---|---|
| Income / Expense / Mandatory / Net CF / Available Capacity | 001 + 002 | 003, 004, 005, 006, 008, 009 |
| Debt facts, stored plan, stored payoff projection | 002 | 004, 005, 008 (read), 009 |
| Debt payoff strategy (avalanche/snowball) | 005 (deferred) | — |
| Goal facts, progress, required capacity, priority | 003 | 004, 005, 008 |
| Timeline changes, allocation rules, dependencies, route plan | **008** | 004 (ETA), 005 (stages), 006 (scenario override), 009 (snapshot) |
| Selected-destination GPS view + status | 004 | 009 |
| Roadmap stage presentation | 005 | 009 |
| Scenario isolation | 006 | 009 |
| Snapshots + change ledger | 009 | — |

---

## 8. What was NOT done (guard rails respected)

- No `plan.md`, `research.md`, `data-model.md`, `quickstart.md`, `contracts/`, or `tasks.md` for 008.
- No backend/frontend/database change.
- No amendment to a *formula*: only the terminology/order clauses required by the ratified
  decisions, plus the oracle cases the constitution's Definition of Done requires.
