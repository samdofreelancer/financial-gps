# Feature Specification: Financial Goals

**Feature Branch**: `003-financial-goals`

**Created**: 2026-08-24

**Status**: Implementation Ready

**Input**: User description: "Define destinations such as debt freedom, emergency fund, house,
and other long-term goals."

**Normative context**: `specs/financial-domain/calculation-rules.md` (§0 terminology, §1, §2, §5),
`specs/financial-domain/data-model.md`, `specs/financial-domain/reference-cases.md` (§C, §D),
`001-financial-profile`, `002-debt-management`, `004-financial-gps`.

## 1. Executive Summary & Problem Statement

This feature captures **destinations**: measurable goals with a target, an explicit completion
condition, user-reported progress, an optional target date, and a deterministic required monthly
capacity. Together with position (001) and route constraints from debts (002), it forms the
"Destination + Progress + Capacity" slice of Financial GPS.

Feature 003 deliberately does NOT decide where money actually goes (Allocation), which goal is
funded first (Roadmap), what-if comparison (Scenario), or the GPS track status (`ON_TRACK`/
`AT_RISK`/`OFF_TRACK`). It only reports facts and deterministic comparisons for those later
features to consume.

## 2. User Scenarios & Testing

### Functional Requirements (traceability keys)

| Requirement Key | Description | User Stories | Acceptance Scenarios |
|---|---|---|---|
| FR-001 | Owner-scoped goal CRUD with lifecycle ACTIVE / COMPLETED / ARCHIVED | US1 | SC1.1 – SC1.5 |
| FR-002 | Deterministic remaining, progress, and completion evaluation | US1 | SC1.1, SC1.4 |
| FR-003 | Required monthly capacity for dated goals; capacity comparison | US2 | SC2.1 – SC2.4 |
| FR-004 | Explainable, as-of-date-anchored derivations; actual vs calculated provenance | US1, US2 | SC1.3, SC2.2 |
| FR-005 | Owner isolation and REST contract parity with 002 | US1, US2 | SC1.5, SC2.5 |

### User Story 1 - Create measurable destination (Priority: P1)

As a user, I create a financial goal with a target, a completion condition, and my actual current
amount, so I know where I am trying to go and how far I have come.

**Why this priority**: GPS requires a clear destination before any route can be computed.

**Independent Test**: Create a goal with target and current amounts, then verify remaining amount
and progress are shown and that `currentAmount` is never auto-derived from the profile.

**Acceptance Scenarios**:

1. **SC1.1 - Record goal**:
   - **Given** an authenticated user with a financial profile,
   - **When** the user creates a goal named "Emergency Fund", goal type `SAVINGS`, `targetAmount`
     `200000000.00`, `currentAmount` `50000000.00`, `targetDate` `2027-12-31`, `priority` `1`,
   - **Then** the goal is saved with status `ACTIVE`, currency derived from the profile (`VND`),
     and an immutable server-assigned ID.
2. **SC1.2 - Validation of amounts**:
   - **Given** a goal create/update request,
   - **When** `targetAmount < 0` or `currentAmount < 0` or either is not a canonical decimal with
     at most 2 places,
   - **Then** the system rejects the request with HTTP 400 (`VALIDATION_FAILED`).
3. **SC1.3 - Derived values are calculated, not stored as facts**:
   - **Given** a goal with `targetAmount` `120000000.00` and `currentAmount` `30000000.00`,
   - **When** the goal view is computed,
   - **Then** `remaining` is `90000000.00`, `progress` is `0.2500` (25.00%), and both are labelled
     `calculated` while `targetAmount`/`currentAmount` are labelled `actual`.
4. **SC1.4 - Completion condition**:
   - **Given** an active amount-based goal,
   - **When** `currentAmount >= targetAmount` (including updates that push it past target),
   - **Then** status transitions to `COMPLETED`, `remaining` is `0.00`, and `progress` is `1.0000`
     (100%). A `targetAmount` of `0.00` is COMPLETED at creation.
5. **SC1.5 - Archive (soft delete)**:
   - **Given** an existing goal belonging to the authenticated user,
   - **When** the user sends `DELETE /api/v1/goals/{id}`,
   - **Then** the goal transitions to `ARCHIVED`, is excluded from active goal lists and capacity
     views, and subsequent `GET`/`PUT`/`DELETE` return HTTP 404 (`RESOURCE_NOT_FOUND`).

### User Story 2 - Assess goal capacity (Priority: P2)

As a user, I see the required monthly capacity for each dated goal and how it compares with my
Available Capacity, so I can judge whether the chosen target date is affordable.

**Why this priority**: A destination without a route capacity is not actionable.

**Independent Test**: Set a goal target date and compare required monthly capacity against
Available Capacity from Financial Position.

**Acceptance Scenarios**:

1. **SC2.1 - Required monthly capacity**:
   - **Given** a dated goal with `remaining` `120000000.00` and exactly 12 contribution periods
     remaining (see §4.3),
   - **When** the capacity view is computed as of `asOfDate`,
   - **Then** `requiredMonthlyCapacity` is `10000000.00`.
2. **SC2.2 - Capacity comparison**:
   - **Given** `requiredMonthlyCapacity` `10000000.00` and Available Capacity `10000000.00`,
   - **When** the comparison is evaluated,
   - **Then** `capacityCoverage` is reported as `MEETS_REQUIRED` (equality is sufficient:
     `availableCapacity >= requiredMonthlyCapacity`). With Available Capacity
     `9000000.00` it is `SHORTFALL` and the monthly shortfall `1000000.00` is stated.
3. **SC2.3 - Undated goal**:
   - **Given** a goal with no `targetDate`,
   - **When** the capacity view is computed,
   - **Then** `requiredMonthlyCapacity` is `null`, `monthsRemaining` is `null`, and the explanation
     states that no required capacity can be scheduled without a target date; the goal remains
     valid.
4. **SC2.4 - Non-positive Available Capacity**:
   - **Given** Net Cash Flow `0.00` or negative, hence Available Capacity `0.00`,
   - **When** a dated goal with positive `remaining` is evaluated,
   - **Then** `capacityCoverage` is `SHORTFALL` with full `requiredMonthlyCapacity` as the
     shortfall; zero or negative capacity is never concealed.
5. **SC2.5 - Owner isolation**:
   - **Given** User A owns goal G,
   - **When** User B requests `GET`/`PUT`/`DELETE /api/v1/goals/{G}`,
   - **Then** the server returns HTTP 404 (`RESOURCE_NOT_FOUND`), identical to a non-existent ID.

### Edge Cases

- A goal with no target date remains valid and shows a timeline only when projected capacity exists.
- A target amount of zero is immediately `COMPLETED` with progress `1.0000`.
- `currentAmount > targetAmount` (over-target) yields `remaining` `0.00`, progress `1.0000` (never
  greater than 100%, never negative), and status `COMPLETED`.
- A negative current or target amount is rejected.
- A target date equal to or earlier than `asOfDate` with positive `remaining` is an expired target:
  `monthsRemaining` is `0`, `requiredMonthlyCapacity` equals the full `remaining` (due immediately),
  and `dateFeasibility` is `EXPIRED_TARGET_DATE`.
- Available Capacity equal to required capacity is exactly affordable (`MEETS_REQUIRED`); it is
  not treated as a shortfall.

## 3. Explicit Non-Goals (Scope Boundaries)

- **Allocation / Contribution routing**: 003 does not decide which goal receives money or how
  Available Capacity is split. That belongs to the Allocation rules (008/GPS route).
- **Roadmap stage planning / GPS track status**: `ON_TRACK`, `AT_RISK`, `OFF_TRACK`, `BLOCKED`,
  `COMPLETED` GPS statuses belong to 004/status-rules; 003 only supplies facts and comparisons.
- **Scenario planning**: No what-if mutation of real goals (006).
- **Priority-driven ordering execution**: `priority` is captured and exposed for the future
  Roadmap (005); 003 sorts goal lists by it but allocates nothing.
- **Market/price forecasts and joint ownership**: single individual profile MVP only.
- **Auto-deriving `currentAmount` from the profile's savings/emergency fund**: rejected to avoid
  duplication (see §4.1).

## 4. Domain Model & Deterministic Rules

### 4.1 Goal Aggregate & Attributes

| Attribute | Type | Nullable | Validation / Domain Rules | Description |
|---|---|---|---|---|
| `id` | `UUID` | No | Immutable; server-generated | Unique identifier of the goal |
| `ownerId` | `OwnerId` (UUID) | No | Scoped to authenticated user | Data boundary owner; never client-supplied |
| `name` | `String` | No | Non-blank; max 120 chars | e.g. "Emergency Fund", "House down payment" |
| `goalType` | `GoalType` (Enum) | No | `DEBT_FREEDOM`, `EMERGENCY_FUND`, `SAVINGS`, `HOUSING`, `EDUCATION`, `RETIREMENT`, `OTHER` | Classification |
| `targetAmount` | `Money` | No | `>= 0`; scale 2 | Required target for amount-based goals |
| `currentAmount` | `Money` | No | `>= 0`; scale 2 | **User-supplied actual progress toward this specific goal** (see below) |
| `targetDate` | `LocalDate` | Yes | Optional; may be past (expired) | Planned achievement date |
| `priority` | `Integer` | No | `>= 1`; default `1` | Deterministic ordering key (§4.4) |
| `completionCondition` | `String` / enum | No | `AMOUNT_REACHED` for amount-based goals | Amount-based goals complete iff `remaining <= 0` |
| `status` | `GoalStatus` (Enum) | No | `ACTIVE`, `COMPLETED`, `ARCHIVED` | Lifecycle state |
| `createdAt` / `updatedAt` | `Instant` | No | Server-managed | Audit timestamps |

**`currentAmount` definition (FR / anti-duplication)**: `currentAmount` is the user-reported
amount already accumulated *for this goal specifically*. It is **not** derived from, and must not
be recomputed from, the Financial Profile's `savings` or `emergencyFund` aggregates, which are
global position facts owned by 001. A profile value and a goal's `currentAmount` may intentionally
diverge (e.g. savings earmarked for a different goal). Feature 003 never copies profile money into
a goal; the user maintains the link explicitly if desired.

### 4.2 Derived Progress Rules (deterministic; pure function of inputs + asOfDate)

Following normative `calculation-rules.md` §5 verbatim:

- `remaining = max(targetAmount − currentAmount, 0)` — never negative.
- `progress` (`Ratio`, scale 4, `HALF_UP` for display, 0…1):
  - If `remaining == 0` (i.e., `currentAmount >= targetAmount`, or `targetAmount == 0`):
    `progress = 1.0000` (100%).
  - Else: `progress = currentAmount / targetAmount`, floored at 2-decimal display so progress
    never overstates completion; the stored ratio is scale 4.
- `status` transitions to `COMPLETED` exactly when the completion condition holds
  (`remaining <= 0` for `AMOUNT_REACHED`). An over-target goal is `COMPLETED`, not "negative".

### 4.3 Deterministic Date & Capacity Rules

Everything below is a pure function of `(goal, financialPosition, asOfDate, policy)`; no wall
clock, no hidden state. `asOfDate` is supplied by the user or seeded from the system clock once at
the boundary — never read inside calculations.

- **Months model**: the engine projects on a monthly cadence (`MONTHLY`). Contributions are
  counted in whole monthly periods; partial months are not modelled.
- **`monthsRemaining(asOfDate, targetDate)`**: the largest integer `m >= 0` such that
  `asOfDate.plusMonths(m) <= targetDate`. Equivalently, full monthly contribution periods that
  still fit before the target date. For `targetDate <= asOfDate`, `monthsRemaining = 0`.
  If `targetDate` is null, `monthsRemaining = null`.
- **Required monthly capacity** (dated goal):
  - If `remaining == 0`: `requiredMonthlyCapacity = 0.00` (already `COMPLETED`).
  - If `targetDate` is null: `requiredMonthlyCapacity = null`, `monthsRemaining = null`
    (undated goal; no schedule).
  - If `monthsRemaining >= 1`:
    `requiredMonthlyCapacity = ceil(remaining / monthsRemaining, scale 2)` (`CEILING` rounding so
    the requirement is never understated).
  - If `targetDate <= asOfDate` and `remaining > 0` (**expired target**): `monthsRemaining = 0`,
    `requiredMonthlyCapacity = remaining` (the whole amount is due now), and
    `dateFeasibility = EXPIRED_TARGET_DATE` with a human-readable explanation.
- **Capacity comparison**: uses the canonical `Available Capacity = max(NetCashFlow, 0)` from the
  Financial Position (`CashFlowCalculator` from 001/002; 003 does not recompute it):
  - `availableCapacity >= requiredMonthlyCapacity` (and `requiredMonthlyCapacity` is not null and
    more than zero): `capacityCoverage = MEETS_REQUIRED`.
  - `0 < availableCapacity < requiredMonthlyCapacity`: `capacityCoverage = SHORTFALL` with
    `monthlyShortfall = requiredMonthlyCapacity − availableCapacity`.
  - `availableCapacity == 0.00` and `requiredMonthlyCapacity > 0`: `capacityCoverage = SHORTFALL`
    with the full required amount as shortfall.
  - `requiredMonthlyCapacity == 0.00` (complete goal): `capacityCoverage = NOT_APPLICABLE`.
  - Undated goal: `capacityCoverage = NOT_APPLICABLE` until a target date is supplied.

### 4.4 Priority Semantics (deterministic, for future Roadmap)

- `priority` is a positive integer chosen by the user. **Lower value = higher priority**
  (`1` is highest). Ties break deterministically by `createdAt`, then by `id`.
- 003 uses `priority` only for stable ordering in list views and exports. It MUST NOT influence
  remaining/progress/capacity math. It is the stable ordering key consumed by 005-Roadmap's
  "Goal order" rule and by route ordering (`calculation-rules.md` §7, §8).

### 4.5 Lifecycle States

```text
[ Created ] ──► ACTIVE ──► (remaining == 0 or condition met) ──► COMPLETED
    │              │                                                 │
    │              └──────────────► ARCHIVED ◄────────────────────────┘
    │                      (DELETE /api/v1/goals/{id}; soft delete)
    └─ ARCHIVED row is never hard-deleted except via Account cascade
```

- `ACTIVE`: counts toward progress views and capacity evaluation.
- `COMPLETED`: `remaining == 0.00`; excluded from required-capacity demands but retained for
  history.
- `ARCHIVED`: soft-deleted; excluded from lists, summaries, and capacity views; GET/PUT/DELETE on
  it return 404 after archiving.

## 5. Precision, Rounding, and Currency

- Monetary amounts: `BigDecimal`, scale 2, `HALF_UP` for display/period values; `CEILING` for
  required capacity so it is never understated.
- Progress: `Ratio`, scale 4. Displayed floored at 2 decimals so progress never overstates
  completion.
- Single currency MVP, derived from the owner's Financial Profile (default `VND`); no
  cross-currency conversion.
- Wire format: canonical plain decimal strings (`"90000000.00"`); floats prohibited.

## 6. REST API Specification

Parity with `002-debt-management` §10. All endpoints require session auth; mutations require CSRF.

| Method | Endpoint | Description | Success | Errors |
|---|---|---|---|---|
| `GET` | `/api/v1/goals` | List goals for current owner (excludes ARCHIVED; `?status=` filter allowed) | 200 | 401 |
| `POST` | `/api/v1/goals` | Create a goal | 201 | 400 VALIDATION_FAILED, 401 |
| `GET` | `/api/v1/goals/{id}` | Goal detail incl. derived remaining/progress/capacity | 200 | 401, 404 |
| `PUT` | `/api/v1/goals/{id}` | Update name/type/amounts/targetDate/priority | 200 | 400, 401, 404 |
| `DELETE` | `/api/v1/goals/{id}` | Soft-delete to `ARCHIVED` | 204 | 401, 404 |
| `GET` | `/api/v1/goals/{id}/capacity` | Required capacity, months, coverage vs Available Capacity, explanation | 200 | 401, 404 |

### Create/Update Request (`GoalRequest`)

```json
{
  "name": "Emergency Fund",
  "goalType": "EMERGENCY_FUND",
  "targetAmount": "200000000.00",
  "currentAmount": "50000000.00",
  "targetDate": "2027-12-31",
  "priority": 1
}
```

No `ownerId` field is accepted; ownership resolves server-side via
`CurrentOwnerProvider.requireCurrentOwner()`.

### Goal View (`GoalView`)

```json
{
  "id": "...",
  "name": "Emergency Fund",
  "goalType": "EMERGENCY_FUND",
  "targetAmount": "200000000.00",
  "currentAmount": "50000000.00",
  "targetDate": "2027-12-31",
  "priority": 1,
  "status": "ACTIVE",
  "currency": "VND",
  "remaining": "150000000.00",
  "progress": "0.2500",
  "completionCondition": "AMOUNT_REACHED",
  "derived": { "remaining": "calculated", "progress": "calculated",
               "targetAmount": "actual", "currentAmount": "actual" }
}
```

### Capacity View (`GoalCapacityView`)

```json
{
  "goalId": "...",
  "asOf": "2026-10-01",
  "remaining": "90000000.00",
  "monthsRemaining": 12,
  "requiredMonthlyCapacity": "7500000.00",
  "availableCapacity": "24000000.00",
  "capacityCoverage": "MEETS_REQUIRED",
  "monthlyShortfall": "0.00",
  "dateFeasibility": "DATED",
  "explanation": "Required 7,500,000.00 VND/month is within Available Capacity 24,000,000.00 VND."
}
```

`dateFeasibility` ∈ `DATED | UNDATED | EXPIRED_TARGET_DATE | COMPLETED`. For `UNDATED`,
`requiredMonthlyCapacity`/`monthsRemaining` are `null`. For `EXPIRED_TARGET_DATE`,
`monthsRemaining` is `0` and `requiredMonthlyCapacity` equals `remaining`.

## 7. Security & Account Ownership Boundary

Identical rules to `002-debt-management` §9:

1. Every `goal` row has `owner_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE`.
2. Repository queries filter by authenticated owner: `findAllByOwnerId`, `findByIdAndOwnerId`,
   `archiveByIdAndOwner`.
3. DTOs never accept `ownerId`/`accountId`.
4. Cross-owner access returns HTTP 404 `RESOURCE_NOT_FOUND` (no 403, no enumeration).
5. `GoalExportSection` registers section `"goals"` into `ExportOwnerDataUseCase`; account deletion
   cascades.

## 8. Domain Invariants

1. Non-negativity: `targetAmount >= 0`, `currentAmount >= 0`.
2. `remaining = max(targetAmount − currentAmount, 0)` always; never negative.
3. `progress` is `1.0000` iff `remaining == 0`; otherwise in `[0, 1)`; never exceeds 100%.
4. `status == COMPLETED` iff the completion condition holds; `ARCHIVED` is terminal for user flows.
5. `priority >= 1`; list ordering is `(priority, createdAt, id)`.
6. `requiredMonthlyCapacity` uses `CEILING`; undated goals have `null` capacity; expired dates
   expose full `remaining` with `EXPIRED_TARGET_DATE`.
7. Owner isolation on every query.
8. Feature 003 never recomputes `Available Capacity`; it always reads it from the Financial
   Position boundary (`CashFlowCalculator`).

## 9. Test Oracle (normative for TDD)

Evaluated as of `asOfDate = 2026-10-01` unless noted.

### Table 9.1: Progress & remaining

| Case ID | targetAmount | currentAmount | Expected remaining | Expected progress | Expected status | Notes |
|---|---|---|---|---|---|---|
| REF-G01 | 120000000.00 | 30000000.00 | 90000000.00 | 0.2500 | ACTIVE | Standard partial progress |
| REF-G02 | 120000000.00 | 120000000.00 | 0.00 | 1.0000 | COMPLETED | Exact completion |
| REF-G03 | 120000000.00 | 150000000.00 | 0.00 | 1.0000 | COMPLETED | Over-target clamps |
| REF-G04 | 0.00 | 0.00 | 0.00 | 1.0000 | COMPLETED | Zero target complete at creation |
| REF-G05 | 120000000.00 | 0.00 | 120000000.00 | 0.0000 | ACTIVE | Nothing saved yet |
| REF-G06 | -1.00 | 0.00 | HTTP 400 | — | — | Negative target rejected |
| REF-G07 | 100.00 | -5.00 | HTTP 400 | — | — | Negative current rejected |

### Table 9.2: Required capacity & coverage

| Case ID | remaining | asOfDate | targetDate | monthsRemaining | Expected requiredMonthly | Notes |
|---|---|---|---|---|---|---|
| REF-C01 | 120000000.00 | 2026-10-01 | 2027-10-01 | 12 | 10000000.00 | Exact divisor |
| REF-C02 | 121000000.00 | 2026-10-01 | 2027-10-01 | 12 | 10083333.34 (CEILING of 10083333.33…) | Never understated |
| REF-C03 | 90000000.00 | 2026-10-01 | null | null | null | Undated |
| REF-C04 | 5000000.00 | 2026-10-01 | 2026-10-01 | 0 | 5000000.00 | Target today: expired, full remainder due |
| REF-C05 | 5000000.00 | 2026-10-01 | 2025-12-31 | 0 | 5000000.00 | Target in the past |
| REF-C06 | 0.00 | 2026-10-01 | 2027-01-01 | 3 | 0.00 | Completed goal needs nothing |

monthsRemaining definition: largest integer `m` with `asOfDate.plusMonths(m) <= targetDate`.

### Table 9.3: Coverage vs Available Capacity

| Case ID | requiredMonthly | availableCapacity | Expected coverage | Expected shortfall | Notes |
|---|---|---|---|---|---|
| REF-A01 | 10000000.00 | 24000000.00 | MEETS_REQUIRED | 0.00 | Spare Available Capacity |
| REF-A02 | 10000000.00 | 10000000.00 | MEETS_REQUIRED | 0.00 | Exact equality is sufficient |
| REF-A03 | 10000000.00 | 9000000.00 | SHORTFALL | 1000000.00 | Positive but insufficient |
| REF-A04 | 10000000.00 | 0.00 | SHORTFALL | 10000000.00 | Zero/negative Net Cash Flow → zero capacity |
| REF-A05 | null | 24000000.00 | NOT_APPLICABLE | null | Undated goal |
| REF-A06 | 0.00 | 0.00 | NOT_APPLICABLE | null | Completed goal |
