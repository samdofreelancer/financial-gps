# Feature Specification: Financial GPS

**Feature Branch**: `004-financial-gps`

**Created**: 2026-08-24

**Last Updated**: 2026-10-10

**Status**: Implementation Ready — the former blocking decision L-1 (debt-freedom completion) is resolved as D-6 in §16, and every readiness gate passes (§18).

**Input**: User description: "Turn a user's financial state, goals, and route into an explainable
GPS that answers where they are, where they are going, how far away it is, whether they are on
track, and what to do next."

**Normative context**: This specification MUST satisfy, and MUST NOT restate with a competing
formula:

- `specs/financial-domain/calculation-rules.md` — canonical terminology (§0) and formulas.
- `specs/financial-domain/status-rules.md` — the five GPS statuses, precedence, and the
  `latenessTolerance` policy.
- `specs/financial-domain/reference-cases.md` — the deterministic acceptance oracle.
- `specs/financial-domain/data-model.md` and `contracts/engine-contract.md` — engine input/output
  types.
- `specs/001-financial-profile/spec.md` — Financial Position, Income, Expense, Available Capacity.
- `specs/002-debt-management/spec.md` — debt facts, `Mandatory Payment`, payoff and portfolio
  projections, blocker reason codes.
- `specs/003-financial-goals/spec.md` — goal facts, `remaining`, `progress`, required capacity,
  `monthsRemaining`, `dateFeasibility`.
- `.specify/memory/constitution.md` — Principles I–XIV (especially I, III, V, VIII, XI, XIV).

Where this spec and a normative contract disagree on a financial value, the normative contract
wins and this spec MUST be corrected. This feature reuses the existing Feature 001–003 application
and domain contracts; it introduces no duplicate profile, debt, or goal entity, table, CRUD API, or
calculator.

---

## 1. Overview & Problem Statement

Financial GPS is the product's central promise: it answers, for one explicitly selected
destination, **where the user is, where they are going, how far away it is, whether they are on
track, and what to do next** — with every number traceable to an input value or a documented rule.

Financial GPS is a **read-only projection**. It computes nothing that Feature 001, 002, or 003
already computes, and it persists no derived value: the result is recomputed from the owner's
stored facts and an explicit `asOfDate`.

---

## 2. MVP Scope & Explicit Non-Goals

### 2.1 In scope (MVP)

- Exactly **one explicitly selected non-ARCHIVED goal** per GPS evaluation, owned by the
  authenticated user (Feature 003). A COMPLETED goal is a valid destination (the GPS reports
  `COMPLETED`); only an ARCHIVED goal is rejected, identically to a missing or other-owner goal.
- Two destination kinds:
  1. **Amount-based destination** — a goal with `completionCondition = AMOUNT_REACHED`
     (all `GoalType` values **except** `DEBT_FREEDOM`).
  2. **Debt-freedom destination** — a goal with `GoalType = DEBT_FREEDOM` and
     `completionCondition = DEBT_FREE`, whose route is derived from the Feature 002 debt portfolio
     projection (see §7.2). The goal's lifecycle `status` stays owned by Feature 003, which completes
     the goal iff the 002 portfolio is `COMPLETED` (no ACTIVE debts remain); this feature computes
     only the GPS route status and never writes the goal's stored status (see §8.1 and decision D-6).
- A single owner, single currency (`VND`), monthly (`MONTHLY`) projection cadence.
- A deterministic `asOfDate`, decimal money, and actual/assumed/calculated provenance.

### 2.2 Explicit non-goals

- **Roadmap-stage destination (005)** is deferred. Feature 005 is `Draft` with no stable stage
  contract; this feature MUST NOT depend on it. Only a goal is a valid destination in the MVP.
- **Allocation across multiple destinations (008)** — this feature does not split Available
  Capacity among debts and multiple goals. See §7.3 for the single-destination rule.
- **Timeline changes / effective-date projections (008)** — the MVP projects using the values
  effective on `asOfDate` only; no future-dated income/expense/rate change is modelled.
- **Scenario planning (006)** and **Financial Review (009)** — not part of this feature.
- **Transaction feeds, market returns, inflation, income growth, multi-currency, joint
  ownership, AI-computed money or ETA** — out of scope (constitution §§II, IX).
- **Debt payoff strategy optimization (avalanche/snowball)** — owned by 005.

---

## 3. Canonical Terminology (reused, never redefined)

This feature uses the `calculation-rules.md` §0 terms **verbatim** and computes none of them:

| Term | Meaning used here | Computed by |
|---|---|---|
| **Income** | sum of active incomes effective on `asOfDate` | Cash Flow (001) |
| **Expense** | sum of active expenses effective on `asOfDate` | Cash Flow (001) |
| **Mandatory Payment** | sum of ACTIVE debts' `minimumPayment` | Cash Flow (001/002) |
| **Net Cash Flow** | `Income − Expense − Mandatory Payment`; may be negative and is reported | Cash Flow |
| **Available Capacity** | `max(Net Cash Flow, 0)` | Cash Flow |
| **Contribution** | the money that actually reaches a destination in a period; capped by `remaining` | §6/§7 |
| **Allocation** | the ordered decision of where Available Capacity goes | **008 (not here)** |

Near-synonyms ("disposable income", "free cash", "remaining money", "surplus") MUST NOT be
introduced.

---

## 4. User Scenarios & Testing *(mandatory)*

### User Story 1 - Understand the current route (Priority: P1)

As a user with a financial profile, debts, and goals, I select one goal and see my current
position, destination, remaining distance, progress, status, and estimated arrival date, or a
stated reason no date can be given.

**Why this priority**: This is the product's central promise and yields value without a
transaction dashboard.

**Independent Test**: Supply a profile with known income, expenses, and debt, plus a selected
goal; verify the GPS shows the input-derived position, remaining distance, capacity comparison,
and a reproducible ETA.

**Acceptance Scenarios**:

1. **Given** a valid profile and a non-ARCHIVED amount-based goal (ACTIVE, or already COMPLETED),
   **When** the user opens Financial GPS for that goal, **Then** the result shows current position,
   destination, distance, progress, ETA (or an unavailable reason), status, blockers, next action,
   and per-value provenance. A COMPLETED goal returns status `COMPLETED`, not `404`.
2. **Given** unchanged inputs, assumptions, and `asOfDate`, **When** the GPS is recalculated,
   **Then** every value, status, and ETA is identical.
3. **Given** Available Capacity is `0` and the goal has `remaining > 0`, **When** the GPS is
   calculated, **Then** status is `BLOCKED`, the ETA is `UNAVAILABLE` with reason
   `NO_AVAILABLE_CAPACITY`, and no achievable date is implied.

### User Story 2 - Understand status and advice (Priority: P2)

As a user, I understand why my route is `ON_TRACK`, `AT_RISK`, `OFF_TRACK`, `BLOCKED`, or
`COMPLETED`, and what measurable change would improve it.

**Why this priority**: A label without an explanation is not a financial GPS.

**Independent Test**: Run otherwise identical inputs with sufficient capacity, insufficient
capacity, zero capacity, and a completed goal; confirm each expected status and its explanation.

**Acceptance Scenarios**:

1. **Given** projected capacity meets or exceeds the required capacity for a dated goal, **When**
   GPS is calculated, **Then** status is `ON_TRACK` and the explanation shows the comparison.
2. **Given** positive capacity that is insufficient for a dated goal, **When** GPS is calculated,
   **Then** status is `AT_RISK` or `OFF_TRACK` per the documented tolerance and the explanation
   states the shortfall and the number of periods late.
3. **Given** the destination completion condition is met, **When** GPS is calculated, **Then**
   status is `COMPLETED`.

### User Story 3 - Review a conservative projection (Priority: P3)

As a user, I can see which values are actual, assumed, or calculated, so I never treat a
projection as a guarantee.

**Why this priority**: Honest, conservative planning is required for informed decisions.

**Independent Test**: Include an assumption (or a missing input) in a projection and verify it is
labelled separately from actual values.

**Acceptance Scenarios**:

1. **Given** a GPS result contains an assumption, **When** the user reviews it, **Then** the
   system labels it `assumed` with its source and explains its effect on the ETA or status.
2. **Given** a required input is missing, **When** the user reviews the result, **Then** the
   missing input is named in `missingInputs`; no zero/placeholder is presented as a real value.

### Edge Cases

- No destination selected → the request is rejected (see §11); no route is fabricated.
- No financial profile exists → the missing input is reported, profile-dependent money is marked
  `UNAVAILABLE` (never a fabricated `0` fact), and no route is produced (§11.3).
- Zero or negative Net Cash Flow with `remaining > 0` → `BLOCKED` (`NO_AVAILABLE_CAPACITY`).
- A goal with no target date → an ETA is still produced, but `AT_RISK`/`OFF_TRACK` do not apply.
- A debt that cannot amortize under its planned payment → its 002 blocker is propagated (`BLOCKED`).
- An expired target date with `remaining > 0` → `dateFeasibility = EXPIRED_TARGET_DATE` and the
  status is judged by lateness (§8.4).
- A selected **amount-based** goal with `currentAmount >= targetAmount` → `COMPLETED` regardless of
  capacity.
- A selected **`DEBT_FREEDOM`** goal with `currentAmount >= targetAmount` but ACTIVE debts still in
  the 002 portfolio → **not** `COMPLETED`: `DEBT_FREE` completion is decided by the 002 portfolio
  status, not by amounts (decision D-6, §16; §8.1).
- Multiple non-archived goals → only the explicitly selected goal is the destination; others do not
  change this result (§7.3).

---

## 5. Functional Requirements *(mandatory)*

- **FR-001**: The system MUST calculate the GPS from the owner's current position (Features
  001/002), the explicitly selected goal (Feature 003), and documented assumptions, and MUST NOT
  introduce a second cash-flow, debt, or goal calculator.
- **FR-002**: Each GPS result MUST include current position, destination, distance, progress (or
  an explicit `not measurable` marker), route context, ETA (or `UNAVAILABLE` with a reason),
  status, blockers, next actions, explanations, and per-value provenance.
- **FR-003**: Status MUST be exactly one of `ON_TRACK`, `AT_RISK`, `OFF_TRACK`, `BLOCKED`,
  `COMPLETED`, evaluated in the documented precedence (§8).
- **FR-004**: Every status, ETA, blocker, and next action MUST be explained by naming the input
  values, the rule applied, and the threshold (including `latenessTolerance`) that decided it.
- **FR-005**: The same inputs, assumptions, `asOfDate`, and policy MUST yield an identical result
  (same values, ETA, status, blockers, explanations).
- **FR-006**: Every value MUST be labelled `actual`, `assumed`, or `calculated`; an assumption
  MUST also carry its source (`USER_SUPPLIED` or `SYSTEM_DEFAULT`).
- **FR-007**: The result MUST NOT present a projection as a guarantee and MUST NOT use an
  unexplained numeric health score as its primary result.
- **FR-008**: When an ETA cannot be calculated, the system MUST return `availability = UNAVAILABLE`
  with a machine-readable reason and MUST identify the missing or blocking condition.
- **FR-009**: The destination MUST be exactly one explicitly selected **non-ARCHIVED** goal (ACTIVE
  or COMPLETED) owned by the authenticated user. A COMPLETED goal is selectable and yields GPS
  status `COMPLETED`; an ARCHIVED goal is rejected as `404 RESOURCE_NOT_FOUND`, identical to a
  missing or other-owner goal. (Roadmap-stage destinations are deferred; see §2.2.)
- **FR-010**: For an amount-based destination, `distance`, `progress`, and `requiredMonthlyCapacity`
  MUST follow `calculation-rules.md` §5–§6 and the Feature 003 rules exactly (including `CEILING`
  for required capacity and for period counts).
- **FR-011**: For a dated destination, status MUST apply the configurable `latenessTolerance`
  policy (default 3 contribution periods) and MUST surface the tolerance value in the explanation.
- **FR-012**: For a `DEBT_FREEDOM` destination, distance and ETA MUST be derived from the Feature
  002 debt portfolio projection; no separate debt calculation may be introduced (§7.2).
- **FR-020**: This feature MUST NOT write, mutate, or silently override the Feature 003 goal
  lifecycle `status`; the GPS `status` is a route projection. For a `DEBT_FREEDOM` goal both are
  reconciled through resolved condition D-6 (§16): Feature 003 owns the lifecycle and marks the goal
  `COMPLETED` iff the Feature 002 portfolio is `COMPLETED`, and this feature reports the same 002
  fact as its route `COMPLETED`.
- **FR-013**: Access MUST be owner-scoped; a destination that does not exist, is archived, or
  belongs to another owner MUST be indistinguishable (`404 RESOURCE_NOT_FOUND`, no `403`).
- **FR-014**: Monetary values MUST use `BigDecimal` scale 2 and decimal strings on the wire; rates
  scale 6; ratios scale 4. Floating point and JavaScript `number` MUST NOT be used for money.
- **FR-015**: `asOfDate` is a first-class input. It is supplied by the request or seeded once at
  the boundary; changing it MAY change the ETA/status and MUST be explained, never served from a
  stale cache.
- **FR-016**: Missing required inputs MUST be reported (`missingInputs`) and MUST NOT be silently
  treated as financial facts; no route is fabricated for a missing destination.
- **FR-017**: Errors MUST be stable RFC 7807 `ProblemDetail` values with a stable `code`.
- **FR-018**: The result MUST state the route context / ordering policy applied (§7.3).
- **FR-019**: This feature MUST NOT add allocation (008), roadmap (005), scenario (006), timeline
  (008), or review (009) behavior.

### Key Entities

- **Financial GPS Result** — an explainable current-position-to-destination result with route
  context, distance, progress, ETA, status, blockers, next actions, explanations, and provenance.
- **Current Position** — the owner's Income, Expense, Mandatory Payment, Net Cash Flow, Available
  Capacity, savings, emergency fund, dependents, and debt totals (DTI, total outstanding).
- **Destination** — the selected goal (amount-based) or the debt portfolio (debt-freedom).
- **GPS Status** — the named, rule-based assessment of the route's projected viability.
- **Blocker** — a verifiable, coded condition that prevents or delays route progress.
- **Provenance** — the `actual` / `assumed` / `calculated` label (with source) for each value.

---

## 6. Result Contract (behavioural)

The GPS result MUST carry the following sections. Field names are indicative; the existing REST
contract (`contracts/rest-api.md`) owns the exact JSON transport, and this feature MUST NOT
redefine the 001/002/003 endpoints.

1. **`asOf`** — the evaluated date (ISO `YYYY-MM-DD`).
2. **`destination`** — `{ type: GOAL, id, name, goalType }` for a goal; `{ type: DEBT_FREEDOM }`
   for a debt-freedom goal (whose route source is the debt portfolio).
3. **`currentPosition`** — Income, Expense, Mandatory Payment, Net Cash Flow, Available Capacity,
   plus savings, emergency fund, dependents, total outstanding debt, and DTI. Each value carries
   provenance **and an availability state**: a value whose required input is absent is
   `UNAVAILABLE` (with a reason), never a fabricated `0` fact (§11.3).
4. **`distance`** — the remaining amount or condition (§7).
5. **`progressPercent`** — the progress ratio where measurable, else `null` with a reason (§7).
6. **`routeContext`** — a statement of the applied route/ordering policy (§7.3).
7. **`capacityComparison`** — `requiredMonthly` versus `projectedMonthly` (`Available Capacity`)
   for a **dated amount-based** destination; `null` (not applicable) for an undated destination and
   for **every** debt-freedom destination, because debt-freedom is compared by date, not monthly
   money (§7.2).
8. **`eta`** — `{ date, availability, periods, reason }`; `availability ∈ {CALCULATED,
   UNAVAILABLE}` (§9).
9. **`status`** — one of the five statuses.
10. **`blockers`** — coded, explainable conditions (§10).
11. **`nextActions`** — measurable, non-prescriptive actions linked to blockers/rules.
12. **`explanations`** — the rule evaluations that produced the above (§8.5).
13. **`provenance`** — the actual/assumed/calculated label (with source) for each value.
14. **`missingInputs`** — required inputs that were absent and affected the result (§11.3).

Empty collections are empty arrays/objects; nothing is omitted silently.

---

## 7. Destination, Distance, and Progress

### 7.1 Amount-based destination

- **destination** = the selected goal (`id`, `name`, `goalType`).
- **distance (remaining)** = `max(targetAmount − currentAmount, 0)` — never negative
  (`calculation-rules.md` §5; Feature 003 §4.2).
- **progressPercent** = Feature 003 progress: `1.0000` when `remaining = 0`, else
  `currentAmount / targetAmount` at scale 4 (display floored so progress never overstates
  completion).
- **requiredMonthlyCapacity** = Feature 003 §4.3 (undated → `null`; expired → full `remaining`;
  otherwise `ceil(remaining / monthsRemaining)`).
- **completionCondition** = `AMOUNT_REACHED`; completed exactly when `remaining = 0`.

### 7.2 Debt-freedom destination (`GoalType = DEBT_FREEDOM`)

To keep debt-freedom consistent with Feature 002 and forbid a competing debt calculation, every
derived value below is read from the Feature 002 portfolio projection
(`DebtSummaryResult.portfolioProjection`); this feature computes no debt arithmetic of its own.

- **distance** = Feature 002 `totalOutstandingDebt` (money — the sum of ACTIVE debts' outstanding
  balances).
- **ETA** = Feature 002 portfolio `projectedDebtFreeDate` with `eta.periods =
  totalMonthsRemaining`. `totalMonthsRemaining` is a **period count** (`Integer` months) and MUST
  NOT be presented, compared, or transported as a monetary value. When the portfolio projection is
  `BLOCKED`, the ETA is `UNAVAILABLE` with the propagated 002 reason code (§9.3).
- **target-date feasibility** is a **date** comparison: `projectedDebtFreeDate` (and the period
  count) against the goal's `targetDate`, feeding the `lateness = max(etaPeriods − monthsRemaining,
  0)` band in §8.4. There is no monetary comparison.
- **capacityComparison** = `NOT_APPLICABLE` (`null`): Feature 002 exposes no canonical monetary
  "required monthly capacity" for the whole portfolio under the single-destination rule, and a
  period count is not money. (The 002 monetary totals — `totalOutstandingDebt`,
  `totalMinimumMonthlyPayment`, `totalPlannedMonthlyPayment`, `totalMonthlyAccruedInterest` — are
  reported as position/route context, not as a capacity requirement.)
- **progressPercent** = `null` with reason `PROGRESS_NOT_MEASURABLE`, because there is no recorded,
  authoritative starting baseline for debt reduction (debts expose an optional `originalPrincipal`
  that is not a total-debt baseline). Distance is measured; progress is honestly reported as not
  measurable rather than invented.
- The goal's own `targetAmount`/`currentAmount` are **not** used to compute the debt-freedom route;
  they are optional advisory context (Feature 003 §4.1) and never determine completion. The goal's
  `targetDate`, when present, is used only for the tolerance comparison. The goal's **lifecycle
  `status` is owned by Feature 003 and is never written by this feature**; Feature 003 completes a
  `DEBT_FREEDOM` goal iff the Feature 002 portfolio is `COMPLETED` (decision **D-6**, §16), and this
  feature reports that same fact as its route `COMPLETED`.

### 7.3 Route context (single destination, no allocation)

The MVP models **one** destination. For an amount-based destination, the ETA assumes the entire
**Available Capacity** is contributed to the selected destination each period (this matches
`reference-cases.md` §C, which supplies a "capacity/mo"). This feature does not split Available
Capacity across debts and goals, does not apply the planned-over-minimum debt surplus, and does
not sequence multiple goals; those belong to Allocation (008) and Roadmap (005). `routeContext`
MUST state this applied policy (e.g. "available capacity directed to the single selected
destination; multi-destination allocation not applied — owned by 008"), so no user mistakes the
single-destination projection for a full allocation.

---

## 8. Status Evaluation (deterministic, ordered)

Status is evaluated in the `status-rules.md` precedence order: **`COMPLETED` → `BLOCKED` →
`ON_TRACK` → `AT_RISK` → `OFF_TRACK`**. The first condition that holds determines the status.

Helper values (all pure functions of `(goal, position, asOfDate, policy)`):

- `monthsRemaining` — Feature 003 §4.3: largest `m ≥ 0` with `asOfDate.plusMonths(m) ≤ targetDate`;
  `null` if undated; `0` if `targetDate ≤ asOfDate`.
- `etaPeriods` — for amount-based: `0` if `remaining = 0`, else `ceil(remaining /
  AvailableCapacity)` when `AvailableCapacity > 0` (CEILING so a period count never understates
  the work). For debt-freedom: 002 `totalMonthsRemaining`.
- `lateness = max(etaPeriods − monthsRemaining, 0)` for a dated destination.
- `latenessTolerance` — policy value, default **3 contribution periods** (`status-rules.md`).

### 8.1 `COMPLETED`

The destination's **route** has arrived:

- amount-based: `remaining = 0` (`currentAmount ≥ targetAmount`); or
- debt-freedom: Feature 002 portfolio status is `COMPLETED` (no ACTIVE debts remain).

`COMPLETED` here is the **GPS route status** (a projection). It is distinct from the Feature 003
goal lifecycle `status`, which Feature 003 alone owns; this feature MUST NOT write the goal's stored
status (FR-020). Decision **D-6** (§16) reconciles the two: Feature 003 completes a `DEBT_FREEDOM`
goal iff the Feature 002 portfolio is `COMPLETED`, so the route `COMPLETED` fact and the goal
lifecycle `COMPLETED` fact are the same 002 status — they cannot disagree while ACTIVE debts remain.
Both are evaluated from the **current** portfolio and are **not sticky**: if a new ACTIVE debt
appears after completion, the destination is no longer `COMPLETED` (see `GC-005`/`status-014`,
VR-24).

### 8.2 `BLOCKED`

No finite route exists to the selected destination. Any of:

- `remaining > 0` **and** `AvailableCapacity = 0` (Net Cash Flow `≤ 0`) →
  reason `NO_AVAILABLE_CAPACITY`.
- An ACTIVE debt cannot amortize under its planned payment → propagate the Feature 002 reason
  (`PAYMENT_DOES_NOT_COVER_INTEREST`, `PAYMENT_COVERS_ONLY_INTEREST`, `INTEREST_RATE_MISSING`,
  `PAYOFF_HORIZON_EXCEEDS_MAXIMUM`, or portfolio `PORTFOLIO_CONTAINS_BLOCKED_DEBTS`).
- debt-freedom destination with a `BLOCKED` portfolio projection.
- a required input is missing such that the route is undeterminable (§11.3) — e.g. no Financial
  Profile with `remaining > 0` → reason `MISSING_FINANCIAL_PROFILE`.

### 8.3 `ON_TRACK`

A finite route exists **and there is no lateness** (`lateness = 0`); being late within tolerance is
`AT_RISK`, never `ON_TRACK`:

- dated amount-based: `etaPeriods ≤ monthsRemaining` (equivalently `AvailableCapacity ≥
  requiredMonthlyCapacity`; equality is sufficient — Feature 003 SC2.2).
- undated amount-based: a finite `etaPeriods` exists (`AvailableCapacity > 0`).
- dated debt-freedom: projected debt-free date is at or before `targetDate` (`lateness = 0`).
- undated debt-freedom: a finite portfolio payoff exists.

### 8.4 `AT_RISK` and `OFF_TRACK` (dated destinations only)

- `AT_RISK`: `1 ≤ lateness ≤ latenessTolerance` (target missed, but within tolerance).
- `OFF_TRACK`: `lateness > latenessTolerance` (target missed beyond tolerance).

There is exactly one status per input: with the default tolerance of 3, a goal that finishes **2
periods late is `AT_RISK` and nothing else** — the phrase "within tolerance" never makes it
`ON_TRACK`.

Boundary rules:

- `lateness = latenessTolerance` → `AT_RISK`; `lateness = latenessTolerance + 1` → `OFF_TRACK`.
- An **expired target** (`monthsRemaining = 0`, `remaining > 0`) is judged by the same rule with
  `monthsRemaining = 0`, so its full `remaining` is the requirement and it is `ON_TRACK` only if
  payable within zero periods; otherwise `AT_RISK`/`OFF_TRACK` by lateness. (`dateFeasibility =
  EXPIRED_TARGET_DATE` is still reported.)
- Undated goals are never `AT_RISK`/`OFF_TRACK` (no target to miss); they are `COMPLETED`,
  `BLOCKED`, or `ON_TRACK`.

### 8.5 Explainability

Every status MUST ship its rule-evaluation expression: the compared values
(`requiredMonthlyCapacity`, `projectedMonthly`/`AvailableCapacity`), the shortfall or lateness, the
`latenessTolerance` value, and which threshold was crossed. This is `status-rules.md`'s
"status label ships with its rule expression" requirement.

---

## 9. ETA Behaviour

### 9.1 ETA shape

`eta = { date, availability, periods, reason }`:

- `availability = CALCULATED` with `date = asOfDate.plusMonths(etaPeriods)` and `periods =
  etaPeriods`.
- `availability = UNAVAILABLE` with `date = null`, `periods = null`, and a machine-readable
  `reason`.

### 9.2 Calculable ETA

- amount-based, `remaining > 0`, `AvailableCapacity > 0`: `etaPeriods = ceil(remaining /
  AvailableCapacity)` (matches `reference-cases.md` G-001, G-003).
- amount-based, `remaining = 0`: `etaPeriods = 0`, `date = asOfDate` (arrival already reached),
  status `COMPLETED`.
- debt-freedom: `etaPeriods = 002 totalMonthsRemaining` (a **period count**, not money), `date =
  002 projectedDebtFreeDate`. If the 002 portfolio is `BLOCKED`, the ETA is `UNAVAILABLE` (§9.3).

### 9.3 Unavailable ETA (never fabricated)

ETA MUST be `UNAVAILABLE` with a reason for, at least:

| Condition | reason |
|---|---|
| No Financial Profile and `remaining > 0` | `MISSING_FINANCIAL_PROFILE` |
| `AvailableCapacity = 0` and `remaining > 0` | `NO_AVAILABLE_CAPACITY` |
| debt planned payment does not cover interest | `PAYMENT_DOES_NOT_COVER_INTEREST` |
| debt planned payment covers only interest | `PAYMENT_COVERS_ONLY_INTEREST` |
| debt interest rate missing (002) | `INTEREST_RATE_MISSING` |
| debt payoff horizon exceeds 002 safety limit | `PAYOFF_HORIZON_EXCEEDS_MAXIMUM` |
| debt-freedom with a blocked portfolio | `PORTFOLIO_CONTAINS_BLOCKED_DEBTS` |

An undated goal with a positive Available Capacity **does** receive a calculable ETA; only the
dated-track status rules are skipped.

---

## 10. Blockers and Next Actions

- **blockers** are coded, verifiable conditions, each with `{ code, explanation, inputs }`. Codes
  reuse the §9.3 reasons plus `NO_AVAILABLE_CAPACITY` and `MISSING_FINANCIAL_PROFILE`. When a
  Feature 002 blocker propagates, its `reasonCode` and explanation are preserved, not reworded.
- **nextActions** are measurable and non-prescriptive, each linked to a blocker or a rule. Examples:
  raise a debt's planned payment to at least its monthly interest; increase Net Cash Flow by the
  stated monthly shortfall; supply a target date; complete the Financial Profile. The system MUST
  NOT issue a command ("you must…") or promise an outcome (constitution §§I, VII).
- If there are no blockers, both collections are empty — never padded with filler.

---

## 11. Missing Data & Error Semantics

### 11.1 Destination is mandatory

The request MUST identify exactly one destination (a goal id). A missing or malformed destination
parameter → `400 VALIDATION_FAILED`. An unknown, **archived**, or other-owner destination →
`404 RESOURCE_NOT_FOUND` (no `403`, no enumeration). A **COMPLETED** (non-archived) goal is a valid
destination and returns GPS status `COMPLETED`; it is not a `404` and is not re-opened for
contribution. If the owner has no goals, the client shows an empty-state prompting goal creation;
the server never fabricates a route.

### 11.2 Input validation

Invalid `asOf` → `400 VALIDATION_FAILED`. Unauthenticated → `401 AUTH_REQUIRED` (existing 007
behaviour). All problems are RFC 7807 `ProblemDetail` with a stable `code`.

### 11.3 Missing required financial inputs

- **No Financial Profile (unavailable, not a real zero):** a missing profile is **not** the same as
  a real `Available Capacity = 0`. The result MUST:
  - list `FINANCIAL_PROFILE` in `missingInputs`;
  - report the position money that depends on the profile (`Income`, `Expense`, `Net Cash Flow`,
    `Available Capacity`, savings, emergency fund) with availability `UNAVAILABLE` and reason
    `PROFILE_MISSING` — **never** as an available `"0.00"` value;
  - keep any independently sourced value (e.g. debt totals from 002) with its own provenance;
  - set ETA `UNAVAILABLE` with reason `MISSING_FINANCIAL_PROFILE` and status `BLOCKED` when
    `remaining > 0`, with a next action to complete the profile. (Consistent with Feature 003
    SC2.3, which reports `UNAVAILABLE` rather than inventing a value.)

  Contrast: `Available Capacity = 0` **with a profile present** is a real, **`calculated`** value
  (`max(Net Cash Flow, 0)` with Net Cash Flow `≤ 0`); it is reported as `"0.00"` with
  `availability = AVAILABLE` and provenance `calculated` — never `actual`, because nothing stores it
  — with status `BLOCKED` (`NO_AVAILABLE_CAPACITY`). The two cases MUST NOT be rendered identically.
- **Debt with a missing interest rate:** already `BLOCKED` by 002; the reason propagates (§9.3).
- **Undated goal:** valid; ETA is still produced; only the dated-track statuses are skipped.

### 11.4 Ownership isolation

Every query resolves the owner server-side; DTOs never accept an owner/account id. Cross-owner
access is indistinguishable from non-existence (`404`). The result is owner-scoped and participates
in the existing owner export/delete behaviour (no new persistence).

---

## 12. Determinism, Precision, and `asOfDate`

- **Determinism**: `(facts, assumptions, asOfDate, policy) → result`; identical inputs yield an
  identical result (`reference-cases.md` §E `DM-001..003`, `SC-001..002`). No clock read, network,
  randomness, or AI inside the calculation.
- **`asOfDate`**: first-class; supplied by the request or seeded once at the boundary. A shift MAY
  change the ETA/status and MUST be explained, never cached stale.
- **Money**: `BigDecimal`, scale 2, decimal strings on the wire; never `float`/`double`/JS number.
- **Rates**: scale 6. **Ratios** (incl. `progressPercent`, DTI): scale 4.
- **Rounding**: `HALF_UP` for display/period values; `CEILING` for any count or requirement that
  must not be understated (`requiredMonthlyCapacity`, `etaPeriods`).
- **Currency**: single-currency `VND`; `Available Capacity` and the destination MUST share the
  goal's currency (Feature 003 `CURRENCY_MISMATCH` guard); mismatch is a validation failure, never
  a silent conversion.

---

## 13. Assumptions

- The MVP evaluates one explicitly selected goal at a time; full route sequencing is a later
  feature (005/008).
- The ETA for an amount-based destination assumes the whole Available Capacity is contributed to
  that destination; it is the documented single-destination projection, not a multi-goal allocation.
- `Mandatory Payment` uses each debt's `minimumPayment`; the planned-over-minimum surplus is not
  applied here (owned by allocation/roadmap).
- The monthly cadence is `MONTHLY`; partial months are not modelled.
- `latenessTolerance` defaults to 3 contribution periods and is configurable policy surfaced in
  every explanation.

---

## 14. Success Criteria *(mandatory)*

- **SC-001**: A user with complete inputs can identify current position, destination, distance,
  status, and next action in under 60 seconds.
- **SC-002**: For 100 representative input sets, recalculation with unchanged inputs and the same
  `asOfDate` yields a byte-identical GPS result.
- **SC-003**: 95% of GPS results that carry a status or ETA display a user-readable explanation of
  the determining inputs, rule, and threshold.
- **SC-004**: In usability testing, at least 90% of participants correctly identify whether their
  selected goal is on track after reviewing a GPS result.
- **SC-005**: 100% of `UNAVAILABLE` ETAs name a machine-readable reason and a next action; no
  fabricated date appears.

---

## 15. Verification Reference Matrix (deterministic, testable)

Evaluated as of `asOfDate = 2026-10-01` unless noted. These are normative for TDD and are the
feature-level projection of `reference-cases.md`. Money is in VND; `cap` = Available Capacity.

| ID | Scenario (inputs) | Expected |
|---|---|---|
| VR-01 | Position CF-001 (`74M/30M/20M` → cap 24M); goal `target 108M, current 0`, `targetDate 2027-03-01` (5 mo) | distance 108M; `etaPeriods 5`; required 21.6M ≤ 24M; status `ON_TRACK` |
| VR-02 | same, `targetDate 2027-01-01` (3 mo) | `etaPeriods 5`, lateness 2 ≤ 3; status `AT_RISK`; shortfall stated |
| VR-03 | same, `targetDate 2026-11-01` (1 mo) | `etaPeriods 5`, lateness 4 > 3; status `OFF_TRACK` |
| VR-04 | goal `target 108M, current 108M` | distance 0; `etaPeriods 0`; progress `1.0000`; status `COMPLETED` |
| VR-05 | CF-002 (`30M/30M/0` → cap 0); goal `remaining 100M` | status `BLOCKED`; ETA `UNAVAILABLE`, reason `NO_AVAILABLE_CAPACITY` |
| VR-06 | CF-003 (`20M/30M/0` → NCF −10M reported, cap 0); `remaining > 0` | Net Cash Flow `−10M` reported, not concealed; status `BLOCKED` |
| VR-07 | Active debt `P 8M < monthlyInterest 10M` (002 REF-D05) | status `BLOCKED`; reason `PAYMENT_DOES_NOT_COVER_INTEREST` |
| VR-08 | Amount goal, `targetDate` null, cap 24M, `remaining 108M` | `etaPeriods 5`; status `ON_TRACK`; `AT_RISK`/`OFF_TRACK` never returned |
| VR-09 | boundary: `remaining 120M`, cap 24M, `targetDate 2027-03-01` (5 mo) → required 24M | required == cap → `ON_TRACK` (equality sufficient) |
| VR-10 | boundary lateness: VR-02 lateness 3 → `AT_RISK`; lateness 4 → `OFF_TRACK` | crosses `latenessTolerance 3` correctly |
| VR-11 | expired target: `remaining 108M`, cap 24M, `targetDate 2026-10-01` | `dateFeasibility EXPIRED_TARGET_DATE`; required 108M; `etaPeriods 5`, lateness 5 > 3 → `OFF_TRACK` |
| VR-12 | Debt-freedom goal with 002 REF-P01 portfolio (`AVAILABLE`, debt-free 2027-09-01, `totalMonthsRemaining` = period count); goal `targetDate 2027-12-31` | distance = 002 `totalOutstandingDebt` (money); ETA = 2027-09-01 with `periods = totalMonthsRemaining` (a **count**, not money); `capacityComparison = null` (`NOT_APPLICABLE`); status `ON_TRACK`; `progressPercent null`, reason `PROGRESS_NOT_MEASURABLE` |
| VR-13 | Debt-freedom goal with 002 REF-P02 portfolio (`BLOCKED`) | status `BLOCKED`; reason `PORTFOLIO_CONTAINS_BLOCKED_DEBTS`; blocked debts enumerated |
| VR-14 | No Financial Profile; `remaining > 0` | `missingInputs` contains `FINANCIAL_PROFILE`; Income/Expense/Net Cash Flow/Available Capacity reported `UNAVAILABLE` (reason `PROFILE_MISSING`), **not** an available `"0.00"`; status `BLOCKED`; ETA `UNAVAILABLE` reason `MISSING_FINANCIAL_PROFILE`; next action to complete profile |
| VR-15 | Same inputs + same `asOfDate`, recalc (DM-001) | identical values, ETA, status, blockers |
| VR-16 | Same inputs, `asOfDate` +1 month (DM-002/003) | ETA/progress/status may shift and the shift is explained; no stale cache |
| VR-17 | Review any result | every value carries `actual`/`assumed`/`calculated` (assumptions also carry source) |
| VR-18 | Request a goal owned by another user, or a non-existent/archived goal | `404 RESOURCE_NOT_FOUND` (identical for all three; no `403`) |
| VR-19 | Missing-profile contrast: profile present with `Income = Expense + Mandatory` → Net Cash Flow `0`, `remaining > 0` | `Available Capacity = "0.00"` reported with `availability = AVAILABLE` and provenance **`calculated`** (`max(Net Cash Flow, 0)`), never `actual`; status `BLOCKED` reason `NO_AVAILABLE_CAPACITY` (rendered differently from VR-14) |
| VR-20 | Debt-freedom units: 002 portfolio `AVAILABLE` with `totalMonthsRemaining = 18`, goal `targetDate` set so `lateness = 2`, tolerance 3 | `capacityComparison = null`; `eta.periods = 18` (count); `lateness 2 ≤ 3` → `AT_RISK` (never `ON_TRACK`); no monetary capacity comparison emitted |
| VR-21 | `DEBT_FREEDOM` goal with advisory `currentAmount 120M ≥ targetAmount 100M`, 002 portfolio `AVAILABLE` with ACTIVE debts (not `COMPLETED`) | goal lifecycle `status` stays `ACTIVE` (Feature 003, D-6) — **not** `COMPLETED`; GPS route is not `COMPLETED` (judged by ETA/tolerance); distance/ETA from 002. Contrasts with VR-04 (amount goal) |
| VR-22 | `DEBT_FREEDOM` goal, 002 portfolio `COMPLETED` (no ACTIVE debts remain) | GPS route `COMPLETED` (`status-013`); Feature 003 goal lifecycle is also `COMPLETED`; `progressPercent null`, reason `PROGRESS_NOT_MEASURABLE` |
| VR-23 | Select a **COMPLETED** (non-ARCHIVED) amount-based goal as the destination | HTTP 200 (not `404`); status `COMPLETED`; `etaPeriods 0`, `eta.date = asOf`; distance 0, progress `1.0000`. Only ARCHIVED/unknown/other-owner goals are `404` (VR-18) |
| VR-24 | `DEBT_FREEDOM` goal completed by a `COMPLETED` portfolio, then a **new ACTIVE debt** appears | goal lifecycle returns to `ACTIVE` and the GPS route is **no longer `COMPLETED`** (dynamic, not sticky; `GC-005`, `status-014`). An `ARCHIVED` goal would instead stay archived |

Shared-oracle cases consumed by this feature: `CF-001..003` (position), `G-001..005` and
`RC-001..002` (distance/ETA/capacity), `GC-001..005` (goal completion condition), `DC-004` and 002
`REF-D05..D07` (debt blockers), 002 `REF-P02`/`REF-P03` (portfolio blocked/completed),
`status-001..014`, `DM-001..003`. Allocation cases (`AL-*`) and scenario cases (`SC-*`) are owned by
008/006 and are **not** requirements of this feature.

---

## 16. Product Decisions Resolved in This Revision

These were resolved from the authoritative contracts and the review instructions; they are recorded
so the product owner can confirm or veto. They are **not** unresolved ambiguities — the spec is
deterministic under them.

- **D-1 (debt-freedom source of truth)**: A `DEBT_FREEDOM` goal's distance and ETA are derived from
  the Feature 002 debt portfolio projection, not from the goal's own `targetAmount`/`currentAmount`;
  the goal's `targetDate`, when present, is used only for the tolerance comparison. Rationale: the
  review requires debt-freedom to stay consistent with 002 and forbids a competing debt
  calculation. This is a deliberate composition of 002 and 003, not a new financial rule (§7.2).
- **D-2 (single-destination ETA)**: An amount-based destination's ETA assumes the whole Available
  Capacity is contributed to the single selected goal; multi-destination allocation is excluded
  (008) and stated in `routeContext` (§7.3).
- **D-3 (non-amortizing debt)**: Consistent with the normative `status-rules.md`, an ACTIVE debt
  that cannot amortize under its planned payment forces the GPS status to `BLOCKED` and propagates
  the 002 reason code.
- **D-4 (missing profile)**: A missing Financial Profile is reported via `missingInputs` with a
  `BLOCKED` result and no fabricated route, rather than silently assuming zero income. The
  profile-dependent money is reported `UNAVAILABLE`, distinct from a real `Available Capacity = 0`
  (§11.3).
- **D-5 (status bands)**: The five statuses use the single deterministic lateness band set from
  `status-rules.md`/§8.4: `lateness = 0` → `ON_TRACK`, `1..latenessTolerance` → `AT_RISK`,
  `> latenessTolerance` → `OFF_TRACK`. With tolerance 3, a goal 2 periods late is unambiguously
  `AT_RISK`.
- **D-6 (debt-freedom completion condition — former L-1; RESOLVED)**: A `DEBT_FREEDOM` goal
  (`GoalType = DEBT_FREEDOM`) is a **non-amount** goal whose `completionCondition` is `DEBT_FREE`,
  and it is `COMPLETED` iff the Feature 002 debt portfolio projection status is `COMPLETED` (no
  ACTIVE debts remain). The goal's `targetAmount`/`currentAmount`, when present, are **advisory
  only** and never determine completion; a `DEBT_FREEDOM` goal is therefore never `COMPLETED` while
  ACTIVE debts remain, even if `currentAmount >= targetAmount`. Feature 003 keeps ownership of the
  goal lifecycle and consumes 002's portfolio status (no duplicate debt calculation, no new entity,
  table, or API); Feature 004 never writes the goal status (FR-020). `AMOUNT_REACHED` goals are
  unchanged.

  This resolves the former blocking decision L-1 by option **(a) debt-linked completion** over option
  (b) status-quo amount-based completion (which would have allowed `COMPLETED` while ACTIVE debts
  remain). It also settles the dynamic-vs-historical question: `DEBT_FREE` completion follows the
  **current** portfolio and is **not sticky** — if a new ACTIVE debt appears after completion, the
  goal returns to `ACTIVE` (and the route is no longer `COMPLETED`); archiving the goal (`ARCHIVED`,
  terminal) is how a user preserves a reached milestone (`GC-005`, `status-014`, VR-24). It is
  realized by the amended Feature 003 (`specs/003-financial-goals/spec.md` §4.5, §9.4),
  the shared `calculation-rules.md` §5, `data-model.md`, `contracts/engine-contract.md`,
  `status-rules.md`, and `reference-cases.md` (§C2, `status-013`/`status-014`). The Feature 003 `Goal` aggregate
  amendment and nullable amount columns required to realize it are flagged there as an
  implementation delta; they are **not** implemented by this documentation change.

---

## 17. Cross-Spec Reconciliation Required (non-blocking documentation)

These are documentation inconsistencies discovered during the audit; they do not change the
financial behaviour defined above and must be reconciled so the specs agree:

- **`specs/financial-domain/status-rules.md`** previously described `ON_TRACK` as "at or before the
  target date within tolerance", which overlapped `AT_RISK`'s "after the target date yet within
  tolerance" and left a 2-periods-late goal ambiguous. Corrected in this revision to one
  deterministic lateness band set (`lateness = 0` → `ON_TRACK`; `1..latenessTolerance` → `AT_RISK`;
  `> latenessTolerance` → `OFF_TRACK`); `reference-cases.md` now pins `status-007..012`.
- **`specs/004-financial-gps/data-model.md`** previously stated `OFF_TRACK` as a fixed "more than
  three monthly contribution periods" count, contradicting the configurable `latenessTolerance`.
  Corrected; its status-transition wording is aligned to the bands above.
- **`specs/003-financial-goals/spec.md`** has been reconciled with decision **D-6**: it now models
  `DEBT_FREE` as a first-class completion condition for `GoalType = DEBT_FREEDOM` (§4.5), with
  acceptance/lifecycle cases (`SC1.6`, `REF-G09..G015`) and the shared oracle extended (§C2,
  `status-013`/`status-014`). Completion is dynamic, not sticky (D-6; `GC-005`). The `Goal` aggregate
  amendment and the nullable amount columns required to realize it are flagged there as an
  implementation delta (no application code changed in this documentation task).
- **`specs/001-financial-profile/spec.md`** currently returns zero totals for a missing profile (a
  200 with `"0.00"` facts). §11.3 requires the GPS to distinguish this from a real
  `Available Capacity = 0` by marking profile-dependent money `UNAVAILABLE`. The position boundary
  needs a profile-presence/availability signal (or the GPS layer must derive presence itself); this
  is flagged for the 001 follow-up. It does not change any 001 number.
- **API error standard**: the implemented convention is **RFC 7807** (`api/common/ProblemDetailAdvice`
  and 007), and the `ProblemDetail` property for field errors is `violations`. The 004
  `contracts/rest-api.md` and `research.md` said RFC 9457 / `fieldErrors`; corrected to RFC 7807 /
  `violations`. The stale `tasks.md` still contains a stray "RFC 9457" task text; it sits inside the
  DO-NOT-EXECUTE banner and will be dropped when that file is rewritten. (RFC 9457 is the later
  successor; standardizing on the implemented 7807 avoids a second convention.)
- **Stale `plan.md` / `tasks.md`** for this feature (React/TanStack, `domain/gps/`,
  `application/gps/`, `api/error/GlobalExceptionHandler`, a `V001` scaffold migration) do not match
  the implemented Vue/Pinia frontend or the pure-DDD backend and MUST NOT be executed. They are
  flagged in place as stale and handed to the architecture/planning workflow; they are **out of
  scope for this requirements task**.

---

## 18. Readiness Gate

| Gate | Result |
|---|---|
| Every functional requirement maps to acceptance scenarios | PASS (§5 ↔ §4/§15) |
| Status precedence is unambiguous (ON_TRACK vs AT_RISK) | PASS (§8 + `status-rules.md` lateness bands; `status-007..012`) |
| ETA, units, and debt-freedom monetary comparison | PASS (§7.2, §9.2: `totalMonthsRemaining` is a count; `capacityComparison` N/A) |
| Missing-data behaviour distinguishes unavailable from a real zero | PASS (§11.3; VR-14 vs VR-19) |
| Ownership / API error semantics | PASS (§11.4, §11.2; RFC 7807, `violations`) |
| Destination scope is unambiguous (ACTIVE vs COMPLETED) | PASS (FR-009/§2.1/§11.1: any non-ARCHIVED goal; VR-04/VR-22/VR-23 vs VR-18) |
| No requirement depends on unimplemented future features | PASS (goal-only; 005/006/008/009 excluded) |
| Goal lifecycle is reconcilable with Feature 003 | PASS (D-6; 003 §4.5 `DEBT_FREE`, dynamic; `GC-001`/`GC-005`) |
| No unresolved P1 domain decision remains | PASS (former L-1 resolved as D-6, dynamic-not-sticky, §16) |
| Spec describes behaviour, not speculative implementation | PASS |

**Status**: `Implementation Ready`. Every gate passes. The former blocking decision L-1 is resolved
as D-6 (§16). The resulting Feature 003 `Goal` aggregate amendment is an implementation follow-up
(flagged in 003 §4.5) that introduces no new entity, table, or API; all blocking gates here are
documentation-consistency gates and they pass.
