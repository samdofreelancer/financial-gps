# Feature Specification: Financial Roadmap

**Feature Branch**: `005-financial-roadmap`

**Created**: 2026-08-24

**Status**: Draft

**Input**: User description: "Turn goals into ordered stages such as debt elimination, emergency
fund, and house fund, each with completion conditions and ETA."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See the route as stages (Priority: P1)

As a user, I see an ordered roadmap from my current financial state through debt elimination,
emergency fund, and later goals so I know what to prioritize now.

**Why this priority**: Roadmap is what distinguishes Financial GPS from a reporting dashboard.

**Independent Test**: Create debt and goal data, generate a roadmap, and verify its stage order,
conditions, projected dates, progress, and current next action.

**Acceptance Scenarios**:

1. **Given** a profile with active debt and goals, **When** the user views the roadmap, **Then**
   the system shows ordered stages with their objectives and current stage.
2. **Given** a current stage's completion condition is met, **When** the roadmap is recalculated,
   **Then** the next eligible stage becomes current.

---

### User Story 2 - Understand transition conditions (Priority: P2)

As a user, I see what must happen to complete a stage and what blocks the next one.

**Why this priority**: An ETA without an actionable transition is not useful.

**Independent Test**: Review a stage before and after satisfying its condition and verify its
completion explanation and next action.

**Acceptance Scenarios**:

1. **Given** an incomplete stage, **When** the user opens it, **Then** the system shows its start
   condition, completion condition, required capacity, projected completion date, progress,
   blockers, and next action.

### Edge Cases

- A route with no active goals identifies that no destination is available.
- A blocked prerequisite keeps later stages visible but unavailable as current work.
- A completed route is shown as complete without inventing a new stage.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST generate an ordered roadmap from the user's debts, active goals,
  priority choices, and documented route rules.
- **FR-002**: Each roadmap stage MUST display objective, start condition, completion condition,
  required cash flow, projected completion date when calculable, progress, blockers, and next
  action.
- **FR-003**: The system MUST identify the current stage and prevent a later stage from being
  represented as complete when its prerequisite is incomplete.
- **FR-004**: The system MUST recalculate the route when relevant profile, debt, goal, or
  assumption data changes.
- **FR-005**: The system MUST explain stage order and transitions in user-readable terms.

### Key Entities *(include if feature involves data)*

- **Roadmap**: An ordered route from the current position to one or more goals.
- **Roadmap Stage**: A bounded objective with entry, completion, progress, capacity, ETA, and next
  action.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can identify the current roadmap stage and next action in under 30 seconds.
- **SC-002**: For 100 representative routes, every stage transition occurs only after its stated
  completion condition is satisfied.
- **SC-003**: 90% of tested users can explain why their next goal is not yet current after viewing
  the roadmap.

## Assumptions

- Initial default route order is debt elimination, emergency fund, then other user-prioritized
  accumulation goals; the user can set priorities where no prerequisite applies.
- Refinancing, investment allocation, and multi-currency routes are out of scope for the initial
  roadmap.
