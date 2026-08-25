# Feature Specification: Financial Timeline & Allocation

**Feature Branch**: `008-financial-timing-allocation`

**Created**: 2026-08-25

**Status**: Draft

**Input**: User description: "Model a financial time dimension (effective-date income/expense/debt
changes instead of a flat monthly snapshot) and a cash allocation engine that decides where free
monthly cash is routed (debt, emergency fund, goals), so the GPS route and its ETA reflect real
planning rather than assuming future equals present."

**Depends on**: `001-financial-profile`, `002-debt-management`, `003-financial-goals`,
`004-financial-gps`. Calculation/rounding/status behavior MUST follow
`financial-domain/calculation-rules.md`, `status-rules.md`, and `reference-cases.md`.

## Problem this solves (from the domain review)

Today the GPS treats financials as a flat monthly snapshot ("salary 74, expense 30") and
implicitly extrapolates **future = present** forever. It also never answers the most valuable
route question: **"where does my Available Capacity actually go each month?"** This feature adds
the two core pieces the engine was missing: a **timeline** (effective-dated changes) and a **cash
allocation** (the route), plus **goal dependency** so a roadmap is not a flat list.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record future income/expense changes (Priority: P1)

As a user, I record that my salary will rise or my rent will change from a specific month, so
projections reflect my actual plans instead of assuming the present keeps repeating.

**Why this priority**: A flat "future = present" assumption is the largest source of inaccurate
ETAs; without it, scenario planning is built on a false base.

**Independent Test**: Add "salary +10% from 2027-04" and confirm the projected cash flow is
unchanged before 2027-04 and higher from 2027-04 onward.

**Acceptance Scenarios**:

1. **Given** an income linked to a salary of 74, **When** the user records a future increase of
   +10% effective 2027-04, **Then** projections before 2027-04 use 74 and from 2027-04 use 81.4,
   and the change is labelled as a plan/assumption.
2. **Given** an expense change effective 2027-02, **When** the GPS is projected, **Then** the
   change affects only periods from its effective date.
3. **Given** the user intends a dashboard "future = present" default, **When** no change is
   recorded for a line, **Then** the flat value is used and explicitly labelled as "assumed
   unchanged" (never silently treated as a forecast).

---

### User Story 2 - See and control where monthly cash goes (Cash Allocation) (Priority: P1)

As a user, I can see and adjust how my Available Capacity is routed each month (debt, emergency
fund, goals) so the GPS describes a real route, not just a balance.

**Why this priority**: This is the product's central route concept. Without it the GPS only
reports "you have X capacity" and never "X goes here, then there".

**Independent Test**: With Available Capacity 24 monthly, set "debt extra 15, emergency fund 9",
then complete the debt and confirm the freed 15 now routes to the next priority goal.

**Acceptance Scenarios**:

1. **Given** Available Capacity of 24, **When** allocations "debt 15, emergency fund 9" are set,
   **Then** the engine routes 15 to the debt and 9 to the goal each month.
2. **Given** a debt that is then paid off, **When** the next period is projected, **Then** the
   freed 15 is reallocated to the next priority destination and the engine states the new route.
3. **Given** a user-defined order differs from the default (debt→emergency→goals), **When** the
   user overrides priority or defines a dependency, **Then** the engine applies the override and
   discloses which policy was used and why.

---

### User Story 3 - Step through a goal prerequisite (Goal Dependency) (Priority: P1)

As a user, I declare that a goal can only start once a prerequisite is complete (e.g., an
emergency fund before a house fund), so the roadmap is a real sequence and not a list.

**Why this priority**: without dependencies every goal competes at once and the roadmap is
misleading; ordering definitions is where route logic loses clarity faster.

**Independent Test**: Create goal B that requires goal A; with A incomplete, B receives no
contribution; complete A and watch B start in the next period.

**Acceptance Scenarios**:

1. **Given** goal B depends on goal A and A is incomplete, **When** a projection is made, **Then**
   B receives no contribution and the result states the prerequisite blocker.
2. **Given** a then-completed goal A, **When** the next period is projected, **Then** B starts
   receiving its allocation.
3. **Given** a goal cycle A→B→A, **When** either is projected, **Then** the engine rejects the
   cycle as an invalid dependency and reports a validation `BLOCKED` rather than looping.

---

### Edge Cases

- A change effective in the past (before `asOfDate`) applies at the `asOfDate`; it is never
  applied retroactively to already-reported months without a labelled audit entry.
- Allocating more Available Capacity than exists: the engine caps at Net Cash Flow (see
  `max(NetCashFlow, 0)`) and states the shortfall; it never allocates imaginary money.
- A goal that depends on an incomplete prerequisite receives no contribution, but the engine
  still shows its required capacity so the user can see the future load.
- A timeline change plus a scenario must stay isolated: a scenario's timeline is a projection
  only (`006-scenario-planning`).
- Negative Net Cash Flow means zero discretionary allocation; the engine reports `BLOCKED`
  and does not fabricate a route.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST let a user attach an income, expense, or debt change to an
  effective date and project only from that date onward.
- **FR-002**: The system MUST compute Net Cash Flow and Available Capacity using the values
  effective on the `asOfDate`, per `calculation-rules.md` §3.
- **FR-003**: The system MUST keep an ordered cash allocation (debt, emergency fund, goals) and
  route Available Capacity to each destination in priority order, capping at Net Cash Flow.
- **FR-004**: When an allocation destination completes, the system MUST reallocate the freed
  capacity to the next priority destination and disclose the new route.
- **FR-005**: The system MUST record goal dependencies (prerequisites) and withhold contribution
  to a dependent goal whose prerequisites are incomplete unless an explicit override exists.
- **FR-006**: The system MUST detect a dependency cycle and reject it as invalid without looping.
- **FR-007**: The system MUST distinguish the default route order from a user-defined or
  rule-driven order and state which was applied and why.
- **FR-008**: The system MUST keep timeline/allocation inputs in the projection's provenance
  (labelled actual vs user-assumed vs calculated) so they are never mistaken for facts.

### Key Entities *(include if feature involves data)*

- **Financial Change / TimelineChange**: an `(effectiveFrom, amount/rate, frequency, source)`
  change on an income, expense, or debt line.
- **Cash Allocation**: an ordered `(sourceCash, destination, amount, priority, effectiveFrom)`
  rule that routes free monthly cash.
- **Goal Dependency**: a `(successorGoal, prerequisiteGoals, type)` edge with DAG validation.
- **Route**: the effective, ordered sequence of destination(s) produced by the allocation is the
  Financial GPS route for a selected destination.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For a timeline change, projections before vs after the change differ exactly at the
  effective date in automated cases (reference `TIM-001`, `TIM-002`).
- **SC-002**: On a debt that completes, the engine's reallocation of freed capacity to the next
  priority is deterministic and verified (reference `AL-002`).
- **SC-003**: A dependency holds without contribution while incomplete and begins the period
  after completion (reference `AL-003`).
- **SC-004**: A dependency cycle is rejected deterministically without the engine looping
  (reference `AL-004`).
- **SC-005**: 100% of allocation results state the order policy applied and why.

## Assumptions

- A user chooses an `asOf` date; the system clock is the default source. Rules remain
  deterministic for the same `asOf`.
- This feature introduces the time and allocation model; rich historical backfill ("what would
  have happened last year") is out of scope.
- Timeline and allocation feed deterministic projection; they do not introduce AI financial
  forecasting (inflation/market return remain out of scope).
   cycle as an invalid dependency and reports a validation `BLOCKED` rather than looping.