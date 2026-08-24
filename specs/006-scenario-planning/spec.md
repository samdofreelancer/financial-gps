# Feature Specification: Scenario Planning

**Feature Branch**: `006-scenario-planning`

**Created**: 2026-08-24

**Status**: Draft

**Input**: User description: "Model what-if changes to income, expenses, debt payoff, or monthly
saving and compare their effects on the route."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Model a financial change safely (Priority: P1)

As a user, I create a scenario such as a salary increase, higher rent, early debt payoff, or
greater monthly saving to see its projected effect without changing my real financial data.

**Why this priority**: Safe comparison makes the GPS actionable while protecting financial truth.

**Independent Test**: Create a scenario that changes income, calculate its route, then confirm the
actual profile and current GPS remain unchanged after the scenario is deleted.

**Acceptance Scenarios**:

1. **Given** an actual financial profile, **When** the user creates a salary-change scenario,
   **Then** the system produces a separate projection while retaining the actual profile.
2. **Given** a scenario, **When** the user edits or deletes it, **Then** actual income, expenses,
   debts, goals, and current route are unchanged.

---

### User Story 2 - Compare route outcomes (Priority: P2)

As a user, I compare my current route against a scenario so I can understand the expected change
to debt-free date, goal ETAs, roadmap, and blockers.

**Why this priority**: A scenario's value is its explainable difference from reality.

**Independent Test**: Compare a baseline and salary-increase scenario and verify that all
differences cite changed assumptions.

**Acceptance Scenarios**:

1. **Given** a baseline and a scenario, **When** the user compares them, **Then** the system shows
   current and scenario ETAs, status, and route differences side by side.

### Edge Cases

- A scenario with no changes has the same result as its baseline and is labelled accordingly.
- An invalid scenario value is rejected without modifying actual data.
- A scenario that makes cash flow non-positive clearly identifies the resulting blocker.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST let users create named scenarios that modify financial assumptions
  such as income, expenses, debt payment timing, or monthly contributions.
- **FR-002**: The system MUST calculate a scenario's GPS and roadmap independently from actual
  financial data.
- **FR-003**: The system MUST not modify actual profile, debt, goal, roadmap, or GPS data when a
  scenario is created, edited, or deleted.
- **FR-004**: The system MUST compare baseline and scenario results, including debt-free date,
  goal ETAs, status, route changes, and blockers when calculable.
- **FR-005**: The system MUST identify each changed scenario assumption and explain its effect on
  the comparison.

### Key Entities *(include if feature involves data)*

- **Scenario**: A named set of hypothetical changes applied only to a separate projection.
- **Scenario Comparison**: The explainable difference between actual route results and scenario
  route results.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can create and compare a single-variable scenario in under 3 minutes.
- **SC-002**: For 100 scenario edits and deletions, actual financial data remains identical before
  and after the scenario operation.
- **SC-003**: Every displayed scenario route difference identifies at least one contributing changed
  assumption.

## Assumptions

- Initial scenarios are personal and private to the user's profile.
- Scenarios change declared assumptions only; they do not forecast market returns, inflation, or
  external economic events.
- Scenario comparison begins with one baseline and one scenario at a time.
