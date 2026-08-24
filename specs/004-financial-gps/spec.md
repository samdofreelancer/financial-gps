# Feature Specification: Financial GPS

**Feature Branch**: `004-financial-gps`

**Created**: 2026-08-24

**Status**: Draft

**Input**: User description: "Turn a user's financial state, goals, and route into an explainable
GPS that answers where they are, where they are going, how far away it is, whether they are on
track, and what to do next."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Understand current route (Priority: P1)

As a user with a financial profile, debt, and goals, I see my current position, selected
destination, remaining distance, progress, status, and estimated arrival date so I know whether I
am moving in the right direction.

**Why this priority**: This is the product's central promise and creates value without a dashboard
of transactions.

**Independent Test**: Supply a profile with known income, expenses, debt, savings, and a goal;
verify the GPS shows each input-derived position and a reproducible projection.

**Acceptance Scenarios**:

1. **Given** a valid profile and an active goal, **When** the user opens Financial GPS, **Then**
   the system shows current position, destination, remaining amount or condition, progress,
   projected ETA, status, blockers, and next action.
2. **Given** unchanged input values, **When** the GPS is recalculated, **Then** it returns the
   same values and status.
3. **Given** an insufficient available cash flow, **When** the user opens Financial GPS, **Then**
   the system identifies the blocking condition and does not imply an achievable ETA.

---

### User Story 2 - Understand status and advice (Priority: P2)

As a user, I understand why my route is on track, at risk, off track, blocked, or complete and
what measurable change would improve it.

**Why this priority**: A label without an explanation is not a financial GPS.

**Independent Test**: Compare two otherwise identical inputs where one has sufficient monthly
capacity and the other does not; confirm the status and explanation change accordingly.

**Acceptance Scenarios**:

1. **Given** projected capacity meets the required capacity for a dated goal, **When** GPS is
   calculated, **Then** status is `ON_TRACK` and the explanation shows the comparison.
2. **Given** projected capacity is positive but insufficient for a dated goal, **When** GPS is
   calculated, **Then** status is `AT_RISK` or `OFF_TRACK` according to documented rules and the
   explanation states the shortfall.
3. **Given** all active goal completion conditions are met, **When** GPS is calculated, **Then**
   status is `COMPLETED`.

---

### User Story 3 - Review a conservative projection (Priority: P3)

As a user, I can see which inputs are actual, assumed, and calculated so I can judge the route
without treating a projection as a guarantee.

**Why this priority**: Honest, conservative planning is required for informed decisions.

**Independent Test**: Include an assumption in a projection and verify it is labelled separately
from actual profile values.

**Acceptance Scenarios**:

1. **Given** a GPS result contains an assumption, **When** the user reviews it, **Then** the
   system labels the assumption and explains its effect on the ETA or status.

### Edge Cases

- No active goal produces a clear request to create or select a destination, not a fabricated route.
- Zero or negative available cash flow produces `BLOCKED` when it prevents required progress.
- A goal with no target date can show a projected ETA but cannot be judged on a dated-track rule.
- If a debt cannot be paid down under recorded terms, its blocker is included in the GPS output.
- Multiple goals must not be combined into a single ambiguous destination; the selected goal or
  roadmap stage is explicit.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST calculate a GPS result from the user's current financial profile,
  debts, selected goal or roadmap stage, and documented assumptions.
- **FR-002**: Each GPS result MUST include current position, destination, distance, progress,
  route context, ETA when calculable, status, blockers, and next action.
- **FR-003**: The system MUST use only the statuses `ON_TRACK`, `AT_RISK`, `OFF_TRACK`,
  `BLOCKED`, and `COMPLETED` for the initial release.
- **FR-004**: The system MUST document and display the input values and rules that determine its
  status, ETA, blockers, and next action.
- **FR-005**: The system MUST produce the same GPS result for the same inputs and assumptions.
- **FR-006**: The system MUST distinguish actual values, user assumptions, and calculated
  projections in every GPS result.
- **FR-007**: The system MUST NOT present a projection as guaranteed advice or use an unexplained
  numeric health score as the primary result.
- **FR-008**: The system MUST state when an ETA cannot be calculated and identify the missing or
  blocking condition.

### Key Entities *(include if feature involves data)*

- **Financial GPS**: An explainable current-position-to-destination result with route, distance,
  progress, ETA, status, blockers, and next action.
- **Current Position**: The user's cash flow, debt burden, savings, emergency fund, and other
  input-derived facts relevant to the selected route.
- **Destination**: The selected goal or roadmap stage and its target or completion condition.
- **GPS Status**: The named, rule-based assessment of the route's projected viability.
- **Blocker**: A verifiable condition that prevents or slows route progress.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user with complete inputs can understand current position, destination, distance,
  status, and next action in under 60 seconds.
- **SC-002**: For 100 representative input sets, recalculation with unchanged inputs yields an
  identical GPS result.
- **SC-003**: 95% of GPS results that have a status or ETA display a user-readable explanation of
  the determining inputs and rule.
- **SC-004**: In usability testing, at least 90% of participants correctly identify whether their
  selected goal is on track after reviewing a GPS result.

## Assumptions

- The initial GPS evaluates one selected destination at a time; roadmap sequencing supplies the
  broader route context.
- Monthly available cash flow is the initial contribution capacity and does not include unrecorded
  investment returns, inflation, or income growth.
- A dated goal is `ON_TRACK` when projected capacity meets its documented required capacity;
  threshold distinctions between `AT_RISK` and `OFF_TRACK` will be defined in the technical plan
  and remain visible to the user.
