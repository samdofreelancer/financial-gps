# Feature Specification: Financial Review & History

**Feature Branch**: `009-financial-review`

**Created**: 2026-08-25

**Status**: Draft

**Input**: User description: "Preserve a deterministic financial snapshot and a review history so
a user can see how their GPS status, ETA, and route change over time, and can answer 'why did my
ETA change?'. Store a concise audit trail of financial changes (changedAt, changedBy, source)
without turning the app into a full banking ledger."

**Depends on**: `001`–`008`; reading/projection behavior MUST conform to
`financial-domain/calculation-rules.md` and `status-rules.md`.

## Problem this solves (from the domain review)

The Financial GPS explains its *current* result, but nothing records *how the result changed over
time* or *which input change caused an ETA shift*. Constitution principles require financial truth,
determinism, and explainability; a GPS without a review trail cannot credibly answer "why is my
route different than last month?". This feature adds a compact, deterministic snapshot/replay
layer plus a financial change audit without a full banking ledger.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See a GPS checkpoint review (Priority: P1)

As a user, I can see a short history of my GPS status across checkpoints (e.g., monthly) so I can
spot when my route moved from on-track to at risk and why.

**Why this priority**: the review layer turns per-date snapshots into a trustworthy, usable tool
and is required by the constitution's explainability and audit principles.

**Independent Test**: Save GPS snapshots at two different `asOf` dates with an intervening input
change, then confirm the review shows both statuses and the driving input difference.

**Acceptance Scenarios**:

1. **Given** a saved GPS snapshot on 2026-08 (`ON_TRACK`) and a later one on 2026-09 after a
   rent increase (`AT_RISK`), **When** the user opens the review, **Then** both statuses, ETAs,
   and the changed input (rent +3) are shown.
2. **Given** a GPS recalculation with unchanged inputs and unchanged `asOf`, **When** the user
   compares checkpoints, **Then** the result is identical (no phantom drifting status).
3. **Given** a change that shifts the ETA, **When** the user inspects the review entry, **Then**
   the review identifies the changing input (income, expense, allocation, or policy) and its
   effective/entry date — the "why did my ETA change?" answer.

---

### User Story 2 - View the financial change audit trail (Priority: P2)

As a user (or an authorized reviewer), I can see every recorded financial change
(`changedAt`, `changedBy`, `source`, before→after) so I can trust what drove each projection.

**Acceptance Scenarios**:

1. **Given** a recorded change `salary 50 → 74` at a time, **When** the user opens the ledger
   entry, **Then** the system shows date, author/source (`USER` or `SYSTEM`), and the value
   change.
2. **Given** a change made through a scenario, **When** the change is shown, **Then** it is
   labelled as a projection event and does not claim to alter the persisted actual state.
3. **Given** an account that requests export or deletion, **When** the user goes through the data
   lifecycle, **Then** the review and ledger entries are covered by the same export/delete
   behavior (`007`).

---

### Edge Cases

- A snapshot contains the full set of inputs+assumptions, an `asOf`, the computed projection and
  the provenance; adding a snapshot never changes persisted inputs (isolation).
- A deleted account must delete (or export-then-delete) its snapshots and ledger; no orphaned
  data is left.
- Re-computation refreshes current derived state; old snapshots remain the frozen record of that
  date.
- Snapshotting every recalculation is not required for MVP; only explicit checkpoints (e.g.,
  monthly, or before/after a mutation) are kept.

### Functional Requirements

- **FR-001**: The system MUST let an owner save a financial/GPS snapshot at an `asOf` checkpoint
  and later view the captured status, ETA, route, and input snapshot.
- **FR-002**: A snapshot MUST include the inputs/provenance that produced it so it is
  deterministic and reproducible with the same `asOf`.
- **FR-003**: The system MUST compare two snapshots and report the driving input differences for
  any status/ETA change.
- **FR-004**: The system MUST keep a concise change ledger with `changedAt`, `changedBy/source`,
  and a before/after summary, without capturing a full banking statement.
- **FR-005**: The change ledger MUST distinguish a user change from a `SYSTEM`-derived value and
  from a scenario projection.
- **FR-006**: Saved snapshots MUST NOT alter the persisted actual financial state (isolation).

### Key Entities *(include if feature involves data)*

- **FinancialSnapshot (Checkpoint)**: the frozen `(inputs, asOf, projection, status, ETA,
  provenance)` at a point in time.
- **FinancialChange (Ledger entry)**: `(target, changedAt, changedByOrSource, before, after)`,
  with source labelled user/system/scenario.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can identify the input change responsible for a status/ETA shift in at
  least 90% of review comparisons (usability sample).
- **SC-002**: Two snapshots with identical inputs+`asOf` render identical status/ETA in 100% of
  cases.
- **SC-003**: Every ledger entry contains changedBy/source and before→after in 100% of recorded
  changes.
- **SC-004**: A snapshot capture or export does not change the persisted profile/inputs in any
  automated test.

## Assumptions

- The app snapshots a compact audit trail but is not a full banking ledger; transaction sync and
  statement reconciliation are out of scope.
- Review history is owner-scoped (only the owner sees it) per `007-authentication`.
- Historical backfill ("what would have happened last year") is out of scope; only recorded
  checkpoints are replayed.
## Requirements *(mandatory)*