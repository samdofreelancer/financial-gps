# Feature Specification: Financial Profile

**Feature Branch**: `001-financial-profile`

**Created**: 2026-08-24

**Status**: Draft

**Input**: User description: "Capture an accurate current financial position as the starting
point for Financial GPS."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record current position (Priority: P1)

As a user, I record my recurring income, regular expenses, savings, emergency fund, and financial
dependents so I can see an accurate starting position.

**Why this priority**: A GPS route is unreliable without a truthful origin.

**Independent Test**: Enter complete profile values and confirm the displayed monthly income,
expenses, available cash flow, savings, and emergency-fund balance match the inputs.

**Acceptance Scenarios**:

1. **Given** a new profile, **When** the user provides income and expense amounts, **Then** the
   system displays their total monthly income, total monthly expenses, and available monthly cash
   flow.
2. **Given** a saved profile, **When** the user changes an amount, **Then** all position totals
   reflect the change without altering debts, goals, or scenarios.

---

### User Story 2 - Distinguish financial facts (Priority: P2)

As a user, I can identify which values are my supplied facts and which are derived totals so I do
not mistake a projection for actual money.

**Why this priority**: This supports financial truth and explainability.

**Independent Test**: Review a completed profile and confirm every displayed total identifies its
source values.

**Acceptance Scenarios**:

1. **Given** a completed profile, **When** the user views the financial position, **Then** actual
   input values are visibly distinct from calculated values.

### Edge Cases

- Income may be zero, variable, or entered from more than one source.
- An expense amount may be zero; negative income, expense, savings, or fund amounts are rejected.
- A user with expenses exceeding income sees a negative cash flow rather than a concealed value.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST let a user record monthly income, other income, fixed expenses,
  variable expenses, savings, emergency fund, and financial dependents.
- **FR-002**: The system MUST calculate total monthly income, total monthly expenses, and
  available monthly cash flow from the recorded values.
- **FR-003**: The system MUST display actual values separately from calculated totals.
- **FR-004**: The system MUST preserve an entered value until the user explicitly changes or
  removes it.
- **FR-005**: The system MUST validate that monetary amounts are not negative.

### Key Entities *(include if feature involves data)*

- **Financial Profile**: A user's current income, expenses, liquid savings, emergency fund, and
  dependents.
- **Income**: A monthly money inflow with amount and source.
- **Expense**: A monthly money outflow with amount, category, and fixed or variable type.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can complete a basic financial profile in under 5 minutes.
- **SC-002**: For 100 representative profiles, displayed monthly totals exactly equal the sum of
  supplied values.
- **SC-003**: 95% of users can identify their available monthly cash flow after completing the
  profile without assistance.


## Assumptions

- The first release serves one individual financial profile at a time.
- Values are recorded in a single currency selected by the user; currency conversion is out of
  scope.
- Transaction-by-transaction expense tracking is out of scope.

## Controlled source and category vocabulary (frontend UX boundary)

- Income source MUST be chosen from the frontend controlled vocabulary INCOME_SOURCES: salary, business, freelance, rent, investment, other.
- Expense category MUST be chosen from the frontend controlled vocabulary EXPENSE_CATEGORIES: rent, food, transport, utilities, health, education, childcare, debt, other.
- These vocabularies are frontend-only UX constraints. The backend DTO and validators still accept any non-blank source/category string, and the domain stores free text. Adding a new option is a frontend change only.
- The row icon in lineIcons.ts is derived from the stored source/category text; the controlled vocabulary keeps the icons predictable.

## Money amount input (frontend UX boundary)

- Every monetary field (financial basics, income, expense) MUST use the shared money input control
  (`frontend/src/components/MoneyInput.vue`).
- The control MUST display amounts with the Vietnamese dong presentation convention (`vi-VN`): `.`
  groups thousands and `,` separates the two decimal places (e.g. `30000000` renders as
  `30.000.000,00`), with the active currency shown as a suffix inside the field.
- The control MUST accept digits only. Letters, currency words, spaces, pasted grouping and a
  minus/plus sign are sanitized instead of stored, so a negative or non-numeric amount can never
  reach the model (FR-005).
- The control MUST accept at most two decimal places; further decimal digits are discarded while
  typing, matching the domain `decimal(2)` columns.
- The separator roles are fixed and unambiguous: `.` is always the thousands separator and `,` is
  always the decimal separator, mirroring the presentation the field shows. A typed dot carries no
  value (it is dropped and re-inserted by grouping), which is what keeps `40.000.000` from collapsing
  into a bogus `40,00` when the user types the dots themselves.
- The value that reaches the model/wire/domain stays an untouched decimal string with `.` as the
  decimal separator (`30000000.00`), so the existing POST/PUT contracts, backend DTO validation and
  persistence behaviour are unchanged.
- The control MUST pad to two decimals when the field loses focus and trim trailing zeros when it
  gains focus, so the field reads like an amount without blocking typing.
- Submitting an empty or non-numeric amount MUST be blocked client-side with an understandable
  message next to the field. `isDecimalAmount` in `frontend/src/api/profile.ts` remains the single
  client-side predicate and the server remains authoritative.
- These rules are a frontend presentation/validation boundary only. No new financial concept,
  score, recommendation or calculation is introduced by them.

