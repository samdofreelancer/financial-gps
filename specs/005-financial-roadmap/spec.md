# Feature Specification: Financial Roadmap

**Feature Branch**: `005-financial-roadmap`

**Created**: 2026-08-24

**Revised**: 2026-10-22 — implementation-ready rewrite (see §4 Clarifications)

**Status**: Implementation Ready

**Input**: User description: "Turn goals into ordered stages such as debt elimination, emergency
fund, and house fund, each with completion conditions and ETA."

## 1. Overview & Problem Statement

Financial GPS (004) answers "where am I going, and when will I arrive" for **one selected
destination**. A user with a debt, an emergency-fund goal and a house goal still has to work out
the order by hand: which obstacle comes first, what has to be true before the next step becomes
available, and why the step after that is not current yet.

The Financial Roadmap turns the owner's facts into one ordered route of **stages**. Each stage
carries its objective, start condition, completion condition, its capacity figure, projected
completion date, progress, blockers and next action. The roadmap states which stage is current and
explains, in user-readable terms, why the stage after it is not current yet.

The roadmap is a **read model**: it is recomputed on read from the owner's profile, debts, goals and
their priorities. Nothing about the route is stored, so it can never drift from the facts it
describes.

## 2. MVP Scope & Explicit Non-Goals

### 2.1 In scope (MVP)

- An ordered stage list derived from the owner's debts and non-archived, non-`DEBT_FREEDOM` goals.
- Per-stage objective, start condition, completion condition, its capacity figure, projected
  completion date (when calculable), progress, blockers and next action.
- The current stage, derived deterministically from the stage order and the stage completion
  conditions.
- A user-readable explanation of the applied stage order and of every stage transition.
- A read-only owner-scoped endpoint (`GET /api/v1/roadmap`) and a Vue 3 screen that renders the
  route and the stage details.

### 2.2 Explicit non-goals

| Non-goal | Owner | Note |
|---|---|---|
| Splitting Available Capacity across destinations (allocation, funding, inter-stage shortfall) | **008** | `GpsRouteContext` already states *"multi-destination allocation not applied (owned by Feature 008)"*; the roadmap must not invent funding amounts. |
| Per-period route simulation, route-aware ETA, reallocation events | **008** | A stage ETA here is a labelled **single-destination hypothesis** (§7.6), not a route projection. |
| Goal dependencies (prerequisite graphs, cycle detection) | **008** | v1 stages are gated by **sequence**, not by a dependency graph (§7.4). The stage model reserves the field. |
| Timeline changes (effective-dated income/expense/debt edits) | **008** | — |
| Scenario what-ifs over the route | **006** | — |
| Financial snapshots and a change ledger | **009** | — |
| Persisting a roadmap, a stage, or a separate priority table | **005** | The roadmap is derived; stage priority is the existing `goal.priority` (003). |
| Reordering debts by interest rate (avalanche/snowball) | deferred | Explicitly deferred by `calculation-rules.md` §8; the debt tier is a single stage in v1. |
| Refinancing, investment allocation, multi-currency routes | — | Out of scope (unchanged from the 2026-08-24 draft). |
| Writing stages, reordering the route, or editing priorities from the roadmap screen | — | Priority is edited through the existing Goal API (`PUT /api/v1/goals/{id}`). |

## 3. Canonical Terminology (reused, never redefined)

Taken from `specs/financial-domain/calculation-rules.md` §0 — 005 adds **no** new money quantities and
renames none of these.

| Term | Meaning in this feature |
|---|---|
| **Financial Profile** | The owner's income/expense facts (001). |
| **Debt** | An ACTIVE debt of the owner (002). |
| **Goal** | A non-archived goal with a target amount and a completion condition (003). |
| **Net Cash Flow** | `Income − Expense − Mandatory Payment` (001/002). |
| **Available Capacity** | `max(Net Cash Flow, 0)` — the monthly amount available for discretionary allocation. |
| **Required Capacity** | The monthly capacity a **goal** needs to reach its target on time (`requiredMonthlyCapacity`, 003). The debt stage does not use this quantity. |
| **Total Planned Monthly Payment** | The stored total planned monthly payment of the debt portfolio (002 `totalPlannedMonthlyPayment`); the debt stage carries it directly. It is a fact, not a required capacity. |
| **remaining** / **progress** | Goal distance and ratio (003). |
| **Financial Status** | `ON_TRACK`, `AT_RISK`, `OFF_TRACK`, `BLOCKED`, `COMPLETED` (`status-rules.md`). |
| **Projection** | A value computed from facts as of an explicit date; never persisted. |
| **Roadmap** | The ordered route from the current position to the owner's destinations. |
| **Roadmap Stage** | A bounded objective with entry, completion, progress, capacity, ETA and next action. |

Terms 005 does **not** introduce: "free cash", "disposable income", "remaining money" (forbidden
synonyms of Available Capacity), and any new name for a quantity the oracle already owns.

`totalPlannedMonthlyPayment` is the 002 field name carried unchanged; `requiredMonthlyCapacity` is
the 003 field name. The two are never conflated: the debt stage has only the former, a goal stage
only the latter.

## 4. Clarifications

### Session 2026-10-22

The clarification loop for this feature was answered with the following defaults (the interactive
prompt channel was unavailable, so each answer is recorded here and can be overridden in a later
amendment):

- **Q: Does the roadmap decide how money is split between stages, or only the order and the
  explanation of the stages?**
  → **A: Order and explanation only.** Cash allocation, debt-extra funding, per-period route rows,
  route-aware ETA and goal dependencies belong to Feature 008 (ownership matrix:
  `specs/008-financial-timing-allocation/open-decisions.md` §7). A stage ETA is stated as an
  explicitly labelled single-destination hypothesis (§7.6).

- **Q: Where do the user's stage priorities live?**
  → **A: In the existing `goal.priority` field** (003), ordered by `Goal.ORDERING` =
  `(priority, createdAt, id)`. No new table, no new write endpoint, no duplicated ordering source.
  Priority is edited through `PUT /api/v1/goals/{id}`.

- **Q: What is the endpoint shape?**
  → **A: A new read-only `GET /api/v1/roadmap`** (owner-scoped, optional `asOf`), separate from
  004's `GET /api/v1/gps?goalId=…` deep dive.

- **Q: Does this feature ship the UI as well?**
  → **A: Yes** — backend domain/application/API, a Vue 3 view, Vitest unit tests and a Playwright
  E2E journey (one vertical slice).

- **Q: What exactly is a stage?**
  → **A: One debt-elimination stage (when the 002 portfolio projection status is not `COMPLETED` —
  `AVAILABLE` or `BLOCKED`, i.e. ACTIVE debt remains; §7.1) plus one stage per non-archived goal
  that is not `DEBT_FREEDOM` — ACTIVE or COMPLETED**, tiered as debt →
  emergency-fund goal → remaining goals by `(priority, createdAt, id)`. A `DEBT_FREEDOM` goal is
  not a separate stage: it mirrors the debt stage's completion condition, so a `DEBT_FREEDOM` goal
  alone (no debt) produces no stage at all (§7.1, §10).

## 5. User Scenarios & Testing *(mandatory)*

### User Story 1 - See the route as stages (Priority: P1)

As a user, I see an ordered roadmap from my current financial state through debt elimination,
emergency fund, and later goals so I know what to prioritize now.

**Why this priority**: The roadmap is what distinguishes Financial GPS from a reporting dashboard.

**Independent Test**: Create debt and goal data, read the roadmap, and verify the stage set, its
order, the current stage and the rendered timeline.

**Acceptance Scenarios**:

1. **Given** a profile with active debt and goals, **When** the user views the roadmap, **Then**
   the system shows ordered stages with their objectives and identifies the current stage.
2. **Given** a current stage's completion condition is met, **When** the roadmap is recalculated,
   **Then** the next still-incomplete stage in route order becomes current (v1 gates by sequence
   only, §7.4).
3. **Given** a profile with no debt and no stage-producing goal, **When** the user views the roadmap, **Then**
   the system states that no destination is available and invents no stage.

---

### User Story 2 - Understand transition conditions (Priority: P2)

As a user, I see what must happen to complete a stage and what blocks the next one.

**Why this priority**: An ETA without an actionable transition is not useful.

**Independent Test**: Review a stage before and after satisfying its condition and verify its
completion explanation, its start condition and its next action.

**Acceptance Scenarios**:

1. **Given** an incomplete stage, **When** the user opens it, **Then** the system shows its start
   condition, completion condition, its capacity figure, projected completion date, progress,
   blockers and next action.
2. **Given** a stage that is not current, **When** the user opens it, **Then** the system names the
   earlier stage that must complete first.

### Edge Cases

- A route with no stage-producing goal (and no debt) identifies that no destination is available.
- A blocked prerequisite keeps later stages visible but unavailable as current work. In v1 the
  prerequisite is the **sequence gate** (an earlier stage); the goal-dependency gate arrives with
  Feature 008 and reuses the same field.
- A completed route is shown as complete without inventing a new stage.
- A `DEBT_FREEDOM` goal never creates a stage: with no active debt and no other goal the route is
  `NO_DESTINATION` (the goal is still visible in the goal list), not `COMPLETED`.
- A goal with no target date has no schedulable **Required Capacity**; the stage reports the
  capacity as not applicable with an explanation instead of inventing one.
- A stage whose ETA cannot be calculated (no Available Capacity, blocked debt portfolio,
  `PAYOFF_HORIZON_EXCEEDS_MAXIMUM`) reports the ETA as unavailable with a reason code — never as a
  fabricated date.
- An owner whose Financial Profile is missing gets a roadmap with an explicit
  `MISSING_FINANCIAL_PROFILE` explanation; no capacity is invented.

## 6. Functional Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST generate an ordered roadmap from the owner's debts, non-archived
  goals (ACTIVE and COMPLETED) excluding `DEBT_FREEDOM` goals (they produce no stage, §7.1), their
  priorities and the documented route rules (`calculation-rules.md` §8).
- **FR-002**: Each roadmap stage MUST display objective, start condition, completion condition,
  its capacity figure (`requiredMonthlyCapacity` for a goal stage, `totalPlannedMonthlyPayment` for
  the debt stage), projected completion date when calculable, progress, blockers and next action.
- **FR-003**: The system MUST identify the current stage and MUST NOT represent a later stage as
  complete while an earlier stage is incomplete.
- **FR-004**: The system MUST recalculate the roadmap from the current facts on every read
  (stateless projection, no stored roadmap).
- **FR-005**: The system MUST explain the stage order and the stage transitions in user-readable
  terms, and MUST disclose which ordering policy was applied.
- **FR-006**: The roadmap MUST NOT partition money: it MUST NOT report funded amounts, shortfalls
  or a per-period schedule. This includes the 004 single-destination capacity shortfall: the
  roadmap may name the action (e.g. increase Net Cash Flow) but never its amount. Amounts that
  exist as facts (stored debt plan, goal amounts) MAY be shown and MUST be labelled with their
  provenance.
- **FR-007**: The roadmap MUST be owner-scoped. Another owner's goals, debts or stages MUST never
  appear, and no request MUST be able to address another owner's roadmap. The endpoint carries no
  owner or goal parameter, so the only possible cross-owner read is through the existing goal
  endpoints the roadmap reuses; those MUST keep answering `404 RESOURCE_NOT_FOUND` (RM-014).
- **FR-008**: A stage projected completion date MUST be labelled with its basis, and MUST be
  reported as unavailable — never fabricated — when it cannot be calculated.
- **FR-009**: The same facts and the same `asOf` date MUST produce an identical roadmap, including
  stage order, decimal scale and reason codes.
- **FR-010**: The roadmap MUST NOT require, persist or mutate a new user fact: stage priority is
  the existing `goal.priority`, and reading the roadmap MUST NOT change any stored fact.

### Key Entities *(include if feature involves data)*

- **Roadmap**: A derived, ordered route from the current position to the owner's destinations.
  Not persisted. Fields: `asOf`, `currency`, `routeStatus`, `currentStageOrder`, `stages`,
  `orderPolicy`, `explanations`, `provenance`.
- **Roadmap Stage**: A bounded objective with entry, completion, progress, capacity, ETA and next
  action. Not persisted. Fields: `order`, `kind`, `goalId`, `objective`, `startCondition`,
  `completionCondition`, `requiredMonthlyCapacity` (goal stage), `totalPlannedMonthlyPayment`
  (debt stage), `progress`, `projectedCompletionDate`, `etaBasis`, `etaReasonCode`, `status`,
  `blockedBy`, `blockers`, `nextAction`.
- **Stage Kind**: `DEBT_ELIMINATION` | `GOAL`.
- **Stage Status**: `COMPLETED` | `CURRENT` | `UPCOMING`.
- **Route Status**: `IN_PROGRESS` | `COMPLETED` | `NO_DESTINATION`.

## 7. The Roadmap Model

### 7.1 Stage set

| Stage | Present when | Source of truth |
|---|---|---|
| `DEBT_ELIMINATION` | The 002 portfolio projection status is not `COMPLETED` — i.e. it is `AVAILABLE` or `BLOCKED`, so the owner still carries ACTIVE debt | `GetDebtSummary` (002) |
| `GOAL` (emergency-fund tier) | A non-archived goal with `goalType = EMERGENCY_FUND` exists | `GoalStore` (003) |
| `GOAL` (other tiers) | Any other non-archived goal exists | `GoalStore` (003) |

A `DEBT_FREEDOM` goal does **not** create its own stage: its completion condition is the debt
stage's completion condition (003 D-6 / 004 D-6). It is visible through the goal list, not as a
second stage.

ARCHIVED goals are never stages. A **COMPLETED** goal keeps its stage — the route shows the finished
part of the journey — but the stage's reported `status` still follows the sequence gate (§7.4): it is
`COMPLETED` only when every earlier stage is complete, otherwise it is `UPCOMING` with `blockedBy`
(RM-006). A route whose stages are all completed reports `routeStatus = COMPLETED` (RM-004).

### 7.2 Stage order (route order)

The order is the oracle's default route order (`calculation-rules.md` §8), tier by tier:

```text
debt-elimination stage → emergency-fund goals → remaining goals
```

Within a tier the order is deterministic and stable: `(priority, createdAt, id)` — exactly
`Goal.ORDERING` (003 §4.4), which sorts ascending with **nulls first** on `createdAt` and on `id`.
A goal without a creation date therefore *precedes* the dated goals of the same priority, and `id`
breaks the tie among equally undated goals. `GoalStore.findAllByOwner` already returns the goals in
that order, so the roadmap **preserves** it inside each tier and MUST NOT define a second ordering
key. Persistence or query order MUST NOT change the result.

Oracle §8 lists the goal tiers as "goals by priority → remaining goals". In v1 these two sub-tiers
collapse into one tier ordered by `(priority, createdAt, id)`, because a target date affects status,
never order (008 decision D-12). The applied policy and the collapsed tiers are disclosed in
`orderPolicy` so the visible order is never a hidden truth.

The applied order policy is reported on every roadmap:
`policy = DEBT_THEN_EMERGENCY_FUND_THEN_PRIORITY`, with a human-readable description and the list of
applied overrides (empty in v1).

### 7.3 Stage fields

| Field | `DEBT_ELIMINATION` stage | `GOAL` stage |
|---|---|---|
| `objective` | "Eliminate the outstanding debt" | The goal name (003) |
| `startCondition` | "No prerequisite — this is the first stage" | "All earlier stages are complete" |
| `completionCondition` | "Debt portfolio status is COMPLETED (no ACTIVE debt remains)" | The goal's completion condition (003: `AMOUNT_REACHED`); `DEBT_FREEDOM` goals produce no goal stage (§7.1) |
| `requiredMonthlyCapacity` | `null` — a debt stage has no 003 required capacity | `requiredMonthlyCapacity` (003); not applicable when the goal has no target date |
| `totalPlannedMonthlyPayment` | The stored total planned monthly payment (002 `totalPlannedMonthlyPayment`), quoted unchanged | `null` — a goal stage has no 002 planned payment |
| `progress` | Not applicable — a debt portfolio has no progress ratio; the stage reports the remaining outstanding debt instead | `progress` (003), clamped to `[0, 1]` |
| `projectedCompletionDate` | `projectedDebtFreeDate` from the 002 portfolio projection | The stage ETA hypothesis (§7.6) |
| `blockers` | 002 blocked-debt reason codes in the shared `{code, explanation}` shape | 004 `GpsBlocker.Code` values (`NO_AVAILABLE_CAPACITY`, `MISSING_FINANCIAL_PROFILE`, `PAYOFF_HORIZON_EXCEEDS_MAXIMUM`, `PORTFOLIO_CONTAINS_BLOCKED_DEBTS`, …). `UNDATED_GOAL` and `CAPACITY_SHORTFALL` are 004 **next-action input tokens**, not blocker codes — the roadmap never fabricates a blocker code |
| `nextAction` | From the 002 projection explanation, phrased as one amount-free action (004 action vocabulary where it exists) | Stage-state aware: an `UPCOMING` stage names the blocking earlier stage; a `CURRENT` stage derives an amount-free sentence from the 004 action **type**; a `COMPLETED` stage states that the objective is met. Never copies a 004 `description` that embeds money. |

Every amount is transported as a decimal string. The owner's currency is echoed **once at route
level** (`currency`), matching the existing `Money` contract (001/002/003); 005 never mixes
currencies inside one roadmap.

`nextAction` selection is deterministic and stage-state aware: an `UPCOMING` goal stage names the
blocking earlier stage; a `CURRENT` goal stage takes the first action the 004 factory returns and
renders its **type** as an amount-free sentence; a `COMPLETED` stage states that the objective is
met. If the factory returns no action (an ON_TRACK destination), use "Stay on the current plan; the
destination is on track." The 004 `description` strings are never copied. The
`INCREASE_NET_CASH_FLOW` action is rendered without the shortfall figure, e.g. "Increase Net Cash
Flow to meet this destination's required monthly capacity."

**No shortfall amount.** 005 never transports the 004 capacity-comparison shortfall: when coverage is
`SHORTFALL`, the 004 `INCREASE_NET_CASH_FLOW` description embeds it, so the roadmap derives the
sentence from the action type instead. Neither `nextAction` nor route `explanations` contains the
shortfall figure; the GPS deep dive (`GET /gps?goalId=…`) remains the place where 004 shows it
(FR-006, RM-015).

### 7.4 Current stage and transitions

The route is a **sequence**. A stage's start condition is "every earlier stage is complete".

1. Stages are evaluated in order.
2. The **current stage** is the first stage whose completion condition is NOT met.
3. The reported status is positional: every stage before the current one is `COMPLETED` (their
   conditions hold by construction), the current one is `CURRENT`, and every stage after it is
   `UPCOMING` — **even if its own completion condition already holds** — carrying `blockedBy` = the
   order of the first incomplete earlier stage. The sequence gate wins over a stage's own condition
   for `status` (RM-006).
4. If every stage is complete, the route status is `COMPLETED` and there is **no** current stage.
5. If there are no stages at all, the route status is `NO_DESTINATION`.

A stage is never reported as `COMPLETED` while an earlier stage is incomplete (FR-003). Goal
dependencies (Feature 008) will add a second gate on top of the sequence gate; the v1 gate is the
sequence only, and the `blockedBy` field is already the place where that gate is reported.

### 7.5 Stage status vocabulary

`COMPLETED`, `CURRENT`, `UPCOMING` — the stage-level view. The Financial Status vocabulary
(`ON_TRACK`, `AT_RISK`, `OFF_TRACK`, `BLOCKED`, `COMPLETED`) is owned by 004 and is reported per
destination inside the GPS view; 005 does not redefine it.

### 7.6 Stage ETA basis

The two stage kinds quote two different, already-canonical sources. The **debt stage** quotes the
002 portfolio projection — `etaBasis = PORTFOLIO_PROJECTION`, date = `projectedDebtFreeDate`,
`null` with the 002 reason code when the portfolio is blocked. A **goal stage** quotes the
**single-destination hypothesis**: *if the whole Available Capacity were directed at this stage,
how long would it take to complete?* It reuses the 004 ETA machinery
(`GpsEtaCalculator.ceilPeriods`, 004 decision D-2) and MUST be labelled with
`etaBasis = SINGLE_DESTINATION_HYPOTHESIS`, together with the capacity amount the hypothesis used.

Consequences, all mandatory:

- The hypothesis **ignores competition** between stages. The roadmap MUST state this in the route
  `explanations` (a user must not read it as a promise).
- The ETA is `UNAVAILABLE` with a reason code (`NO_AVAILABLE_CAPACITY`,
  `MISSING_FINANCIAL_PROFILE`, `PAYOFF_HORIZON_EXCEEDS_MAXIMUM`, …) when it cannot be calculated.
  It is never fabricated.
- Once Feature 008 lands, the stage ETA becomes route-aware and the roadmap MUST disclose that
  change. Until then the hypothesis label stays in the response.

## 8. Result Contract (behavioural)

`GET /api/v1/roadmap` returns one JSON document:

```json
{
  "asOf": "2026-10-22",
  "currency": "VND",
  "routeStatus": "IN_PROGRESS",
  "currentStageOrder": 1,
  "orderPolicy": {
    "policy": "DEBT_THEN_EMERGENCY_FUND_THEN_PRIORITY",
    "description": "Debt elimination, then emergency-fund goals, then the remaining goals by priority.",
    "appliedOverrides": []
  },
  "stages": [
    {
      "order": 1,
      "kind": "DEBT_ELIMINATION",
      "goalId": null,
      "objective": "Eliminate the outstanding debt",
      "startCondition": "No prerequisite — this is the first stage",
      "completionCondition": "Debt portfolio status is COMPLETED (no ACTIVE debt remains)",
      "requiredMonthlyCapacity": null,
      "totalPlannedMonthlyPayment": "5000000.00",
      "progress": null,
      "projectedCompletionDate": "2027-04-30",
      "etaBasis": "PORTFOLIO_PROJECTION",
      "etaReasonCode": null,
      "status": "CURRENT",
      "blockedBy": null,
      "blockers": [],
      "nextAction": "Keep paying the minimum on every debt; the payoff date moves with each payment."
    },
    {
      "order": 2,
      "kind": "GOAL",
      "goalId": "6f1c9d54-2f0b-4a1e-9c33-0b7d5a1e9f42",
      "objective": "Emergency fund",
      "startCondition": "All earlier stages are complete",
      "completionCondition": "AMOUNT_REACHED",
      "requiredMonthlyCapacity": "3000000.00",
      "totalPlannedMonthlyPayment": null,
      "progress": "0.2500",
      "projectedCompletionDate": "2027-01-31",
      "etaBasis": "SINGLE_DESTINATION_HYPOTHESIS",
      "etaReasonCode": null,
      "status": "UPCOMING",
      "blockedBy": 1,
      "blockers": [],
      "nextAction": "Complete stage 1 first; this stage becomes current when stage 1 is complete."
    }
  ],
  "explanations": ["The route is ordered by the default policy: debt, then emergency fund, then goals by priority."],
  "provenance": [
    {"field": "stages[0].totalPlannedMonthlyPayment", "kind": "actual", "detail": "total planned monthly payment stored on the Feature 002 debt portfolio"},
    {"field": "stages[0].projectedCompletionDate", "kind": "calculated", "detail": "002 portfolio projection as of 2026-10-22"},
    {"field": "stages[1].requiredMonthlyCapacity", "kind": "calculated", "detail": "003 required capacity over the remaining months to the target date"},
    {"field": "stages[1].progress", "kind": "calculated", "detail": "Feature 003 goal progress as of 2026-10-22"},
    {"field": "stages[1].projectedCompletionDate", "kind": "calculated", "detail": "single-destination hypothesis over Available Capacity 31000000.00 VND"}
  ]
}
```

Contract rules:

- Decimal amounts and ratios are strings; dates are ISO-8601; `currency` is echoed once at route
  level (single-currency MVP, 001/002/003 `Money` contract).
- `null` means *not applicable / not calculable*, and the matching reason appears in `blockers` or in
  the route `explanations`. `null` never means "zero".
- `provenance` labels every derived value `actual`, `user-assumed` or `calculated` (oracle §11).

The full endpoint contract lives in [contracts/rest-api.md](contracts/rest-api.md).

## 9. Determinism, Precision, and `asOf`

- The roadmap is a pure function of (profile facts, debt facts, goal facts and priorities,
  `asOf`). Two identical reads MUST produce identical output.
- `asOf` comes from the `BusinessDate` port when the client does not supply it; a client-supplied
  `asOf` is honoured and echoed in the response (004 precedent).
- The roadmap is recomputed on read (FR-004): no cached roadmap, no stored roadmap, no background
  job. Reading MUST NOT mutate a stored fact (FR-010).
- Monetary scale and rounding come from the existing `FinancialPolicy` / `GoalCalculationPolicy`;
  005 MUST NOT introduce a second rounding mode.
- The domain layer of this feature stays framework-free: no Spring, JPA, HTTP, clock, randomness
  or AI. The `asOf` date is passed in explicitly.

## 10. Missing Data & Error Semantics

| Situation | Behaviour |
|---|---|
| No session / expired session | `401 AUTH_REQUIRED` (existing auth behaviour — `RestAuthenticationEntryPoint`) |
| Malformed `asOf` | `400 VALIDATION_FAILED` with `violations` (`string[]`, `asOf: <message>`), mapped by the shared `ProblemDetailAdvice` (contract §Error Semantics) |
| Cross-owner access | The endpoint has no owner or goal parameter, so it cannot address another owner's roadmap; it MUST only ever read the caller's data. The `404 RESOURCE_NOT_FOUND` rule (resource hiding, never `403`) stays on the reused goal endpoints (RM-014) |
| Missing Financial Profile | Roadmap returns with `MISSING_FINANCIAL_PROFILE` in `blockers`/explanations; Available Capacity is never invented |
| No debt (or a COMPLETED portfolio) and no stage-producing goal | `routeStatus = NO_DESTINATION`, `stages = []`, `currentStageOrder = null` — includes the case where the only non-archived goal is `DEBT_FREEDOM` |
| All stages complete | `routeStatus = COMPLETED`, `currentStageOrder = null`, no invented stage |
| Undated goal | `requiredMonthlyCapacity = null`; the route `explanations` names the missing target date with `UNDATED_GOAL` wording (004 defines no such blocker code) |
| Zero Available Capacity | Stage ETA `UNAVAILABLE` with `NO_AVAILABLE_CAPACITY` |
| Blocked debt portfolio | Debt stage carries the 002 blocked-debt codes; ETA may be `UNAVAILABLE` |

## 11. Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can identify the current roadmap stage and the next action in under 30 seconds
  (validated by the E2E journey: the current stage and its next action are visible on the roadmap
  screen without navigation).
- **SC-002**: For every roadmap in the verification matrix (§12), a stage transition occurs only
  after its stated completion condition is satisfied — asserted by test, not by inspection.
- **SC-003**: 90% of tested users can explain why their next goal is not yet current after viewing
  the roadmap (validated by the route explanation naming the blocking earlier stage; the remaining
  share is a usability study outside the automated suite).
- **SC-004**: The same facts and `asOf` produce a byte-identical roadmap across two reads
  (determinism test in the domain suite).
- **SC-005**: The backend suite stays green and the roadmap adds no formula that duplicates an
  existing one (architecture guard: the roadmap domain package imports only `domain` and the JDK).

## 12. Verification Reference Matrix (deterministic, testable)

| Case | Setup | Expected |
|---|---|---|
| **RM-001** | No debt, one ACTIVE goal (priority 1) | One `GOAL` stage, `status = CURRENT`, `routeStatus = IN_PROGRESS` |
| **RM-002** | ACTIVE debt + two goals | Stages: debt stage, then goals in `(priority, createdAt, id)` order |
| **RM-003** | ACTIVE debt + `EMERGENCY_FUND` goal (priority 5) + another goal (priority 1) | Order: debt stage, emergency-fund goal, other goal — the emergency tier precedes the priority tier |
| **RM-004** | All stages satisfied (debt portfolio COMPLETED, every goal COMPLETED) | `routeStatus = COMPLETED`, `currentStageOrder = null`, no invented stage |
| **RM-005** | No debt, no stage-producing goal — no non-archived goal, and separately only a `DEBT_FREEDOM` goal | `routeStatus = NO_DESTINATION`, `stages = []` in both variants (a `DEBT_FREEDOM` goal creates no stage, §7.1) |
| **RM-006** | Debt stage incomplete, goal stage complete | The goal stage is `UPCOMING` with `blockedBy = 1` — a later stage is never `COMPLETED` while an earlier one is not |
| **RM-007** | Undated goal | `requiredMonthlyCapacity = null` + `UNDATED_GOAL` wording in the route `explanations`; ETA uses the hypothesis if capacity exists |
| **RM-008** | Available Capacity = 0 | Stage ETA unavailable with `NO_AVAILABLE_CAPACITY`; no date is fabricated |
| **RM-009** | Missing Financial Profile | `MISSING_FINANCIAL_PROFILE` reported; no capacity value |
| **RM-010** | Goal with `createdAt = null` and equal priority | The undated goal precedes the dated one (nulls first on `createdAt`, exactly `Goal.ORDERING`); `id` breaks the tie among undated goals, and the order is stable across reads |
| **RM-011** | Two goals, same priority, different `createdAt` | Earlier `createdAt` first |
| **RM-012** | ARCHIVED goal present | No stage is created for it |
| **RM-013** | Same facts read twice with the same `asOf` | Byte-identical response |
| **RM-014** | Two owners, each with goals: owner A reads their roadmap, then requests owner B's goal by id | Each roadmap contains only its own owner's goals; the foreign goal read answers `404 RESOURCE_NOT_FOUND`; no data leaks |
| **RM-015** | A goal whose 004 Financial Status is `AT_RISK` or `OFF_TRACK` with `coverage = SHORTFALL` (required capacity 8,000,000.00, Available Capacity 4,500,000.00) | `nextAction` names the action without the 3,500,000.00 shortfall; neither `nextAction` nor `explanations` contains the shortfall figure, and no `shortfall` field exists anywhere in the response; `requiredMonthlyCapacity`/`progress` remain the stage's own fields, each with provenance |

## 13. Product Decisions Resolved in This Revision

- **D-1 (roadmap vs allocation)**: 005 orders and explains stages; 008 partitions money. The roadmap
  never reports a funded amount or a shortfall amount, including the 004 single-destination capacity
  shortfall.
- **D-2 (no new persistence)**: Stage priority is the existing `goal.priority`; 005 adds no table,
  no migration and no write endpoint. Migration numbering stays clear of the `V10..V12` range
  reserved by 008 (decision D-18 there).
- **D-3 (debt stage progress)**: The debt stage reports the remaining outstanding debt and the
  projected debt-free date; `progress` is `null` with an explanation, because a portfolio has no
  progress ratio and 005 must not invent one.
- **D-4 (stage ETA basis)**: A goal stage's ETA is the labelled single-destination hypothesis of
  004 D-2, not a route projection (008 owns route-aware ETA).
- **D-5 (stage gating)**: v1 gates a stage by the sequence only; the goal-dependency gate arrives
  with 008 and reuses `blockedBy`.
- **D-6 (route status vocabulary)**: `IN_PROGRESS` / `COMPLETED` / `NO_DESTINATION` at route level;
  `COMPLETED` / `CURRENT` / `UPCOMING` at stage level. The Financial Status vocabulary of 004 is
  not redefined.
- **D-7 (no competing formulas)**: The roadmap reuses `Goal.ORDERING`, `GoalProgressCalculator`,
  `GoalCapacityCalculator`, `GpsEtaCalculator.ceilPeriods`, `GetDebtSummary` and the 002/003
  projections. It defines no money formula of its own.
- **D-8 (single endpoint)**: One read-only endpoint. The GPS view (004) remains the deep dive for a
  single destination; the roadmap is the route overview.

## 14. Cross-Spec Reconciliation

| # | Item | Action |
|---|---|---|
| **XR-1** | The 2026-08-24 draft ordered "debt elimination, emergency fund, then other user-prioritized accumulation goals"; `calculation-rules.md` §8 orders "committed debt extra → emergency-fund goal → goals by priority → remaining goals" | Reconciled: the debt tier is expressed as one `DEBT_ELIMINATION` stage (the committed-debt tier) and the goal sub-tiers collapse into one `(priority, createdAt, id)` tier (§7.2). A follow-up amendment to `calculation-rules.md` §8 is a documentation task (T004) so the oracle states the collapsed order explicitly. |
| **XR-2** | The draft said "required cash flow" | Renamed to **Required Capacity** (§3) — the canonical term from the oracle. |
| **XR-3** | The draft's FR-004 implied event-driven recalculation | Fixed to "recalculated on read" (FR-004, §9). |
| **XR-4** | The draft's SC-002 ("100 representative routes") was not testable | Replaced by the verification matrix (§12) and SC-002/SC-004. |
| **XR-5** | 008's ownership matrix lists "Roadmap stage presentation → 005" | Accepted; 008's route plan is the future source for route-aware stage ETAs (D-4). |
| **XR-6** | 008 decision D-05 option B assigns avalanche/snowball ordering to 005; `calculation-rules.md` §8 still calls 005 "the debt strategy feature" | Deferred: v1 has one debt tier with no rate-based reordering, exactly as `calculation-rules.md` §8 states. A later 005 amendment may add it; T004 corrects the stale feature label in the oracle without changing a formula. |

## 15. Readiness Gate

| Gate | Status |
|---|---|
| Scope and non-goals explicit, allocation ownership assigned to 008 | ✅ |
| Functional requirements trace to the oracle (`calculation-rules.md` §8) and to the implemented domain | ✅ |
| Result contract defined, with `null` semantics and provenance | ✅ |
| Determinism, `asOf` handling and precision pinned | ✅ |
| Error semantics (401/404/missing data) defined | ✅ |
| Verification matrix with 14 deterministic cases | ✅ |
| No new money formula, no new persistence | ✅ |
| Plan, data model, REST contract, quickstart and tasks produced | ✅ |
| Ambiguities closed or explicitly defaulted | ✅ (5 defaults in §4) |

**Ready for implementation** once the tasks in `tasks.md` are executed in order.
