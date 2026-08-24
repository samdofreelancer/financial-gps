# Feature Specification: Debt Management

**Feature Branch**: `002-debt-management`

**Created**: 2026-08-24

**Status**: Draft

**Input**: User description: "Show who the user owes, how much, and the projected debt-free
date."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record and understand debts (Priority: P1)

As a user, I record each debt and see my total debt, monthly payment burden, and projected
debt-free date.

**Why this priority**: Debt is a major route blocker and must be known before planning goals.

**Independent Test**: Enter multiple debts with balances and payments, then verify all portfolio
totals and the resulting projected payoff date.

**Acceptance Scenarios**:

1. **Given** no debts, **When** the user adds a debt, **Then** the system records its creditor,
   outstanding balance, interest rate, monthly payment, due date, and status.
2. **Given** recorded debts, **When** the user views debt summary, **Then** the system shows total
   debt, total monthly payment, debt-to-income ratio, and a projected debt-free date.

---

### User Story 2 - See debt blockers (Priority: P2)

As a user, I can see when a debt payment is insufficient or a payoff date cannot be projected.

**Why this priority**: An unexplained date would violate GPS explainability.

**Independent Test**: Enter a debt whose monthly payment does not cover monthly interest and
confirm the system identifies it as a blocker instead of presenting a payoff date.

**Acceptance Scenarios**:

1. **Given** a debt with an insufficient payment, **When** the user views its projection, **Then**
   the system states that payoff cannot be projected under the current inputs and explains why.

### Edge Cases

- A zero-balance debt is marked paid and excluded from outstanding debt.
- A missing interest rate is explicitly treated as a user assumption, never silently inferred.
- A debt with payment less than accrued interest has no finite payoff projection.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST record creditor, original principal when known, outstanding balance,
  interest rate, monthly payment, due date, planned payoff date, and status for each debt.
- **FR-002**: The system MUST calculate total outstanding debt, total monthly debt payment, and
  debt-to-income ratio.
- **FR-003**: The system MUST calculate a projected debt-free date from recorded debt terms and
  payments when a finite payoff is possible.
- **FR-004**: The system MUST explain any unavailable or invalid payoff projection.
- **FR-005**: The system MUST keep debt information separate from goals and scenarios.

### Key Entities *(include if feature involves data)*

- **Debt**: An obligation to a creditor, including balance, interest, payment, dates, and status.
- **Debt Summary**: The user's aggregate debt burden and payoff projection.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can record one debt in under 2 minutes.
- **SC-002**: For 100 representative debt schedules, calculated total balances and monthly
  payments exactly match supplied values.
- **SC-003**: Every unavailable payoff date is accompanied by a user-readable reason.

## Assumptions

- Payments occur monthly for initial projections.
- The user is responsible for supplying current balance and payment values.
- Debt payoff strategy optimization is out of scope until a roadmap stage requires it.
