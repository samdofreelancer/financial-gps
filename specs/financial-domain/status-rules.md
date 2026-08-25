# Financial Domain: Status Rules

> **Advisory oracle**. Defines exactly how the five GPS statuses are derived. The `004-financial-gps`
> data model sets the status names; this file sets the evaluable conditions so a status is a
> reproducible function of a projection, never an unexplained score.

## Design intent (response to review)

The previous spec used "missed by more than three monthly contribution periods" for `OFF_TRACK`.
That conflates two very different cases:

- a 100M goal slipping 3 months across a 2030 timeline, and
- a small goal slipping 3 months across a 1-year timeline.

The absolute month count is arbitrary. This file replaces it with a **configurable tolerance
policy** and makes every THRESHOLD an explicit, documented policy value rather than a magic
number in code.

## Status model

Each status has an **entry condition** and **explanation** the result must report.

| Status | Entry condition (evaluated in the order below) |
|---:|---|
| `COMPLETED` | The selected completion condition is satisfied (`remaining <= 0` for amount). |
| `BLOCKED` | No finite route exists: non-positive available cash flow that prevents required progress, a debt whose payment does not cover accrued interest, a cyclic/unsatisfiable dependency, or a missing destination. |
| `ON_TRACK` | The projection meets the goal requirement (`projectedCapacity >= requiredCapacity`). For a dated goal this means the ETA is at or before the target date within tolerance. |
| `AT_RISK` | A positive capacity exists but a shortfall is projected; a finite ETA still exists, but the goal is completed only after the target date yet within `latenessTolerance`. |
| `OFF_TRACK` | The target date cannot be reached under the current allocation (ETA slips beyond `latenessTolerance`), but a positive route still exists. |

## Policy parameters (explicit, documented, changeable)

| Parameter | Default | Meaning |
|---|---|---|
| `latenessTolerance` | 3 contribution periods | Max allowed lateness past the target date before `AT_RISK` becomes `OFF_TRACK`. |
| `shortfallRatio (unused)` | — | Reserved for a future ratio-based rating; not a hardcoded score. |
| `asOfDate` | supplied by user or system clock | The single date all period counts and thresholds are measured from. |

These values MUST be exposed in the result's `explanations` so a user can see *which* policy
turned an `AT_RISK` into `OFF_TRACK` and why. They are application configuration, not financial
facts; the engine remains the same function with a parameter.

## Determinism

- Same inputs + same `asOfDate` + same policy values → same status. Verified by
  `reference-cases.md`.
- A status label MUST ship with its rule-evaluation expression (`capacityComparison`,
  `shortfall`, `whichThresholdWasCrossed`).

Definition of Done: a change to a status or a threshold updates this file,
`calculation-rules.md`, `reference-cases.md`, and the GPS feature spec together.