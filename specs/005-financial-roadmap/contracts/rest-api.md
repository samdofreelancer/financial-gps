# REST Contract: Financial Roadmap

**Feature**: 005 Financial Roadmap
**Date**: 2026-10-22
**Companion**: [spec.md](../spec.md) §8, [data-model.md](../data-model.md)

## Conventions

- Base path: `/api/v1`.
- Requests and responses use JSON. Money is a decimal **string** (never a JSON number), dates use
  ISO `YYYY-MM-DD`, ratios are decimal strings. `currency` is echoed **once at route level**; a
  roadmap never mixes currencies (single-currency MVP, 001/002/003 `Money` contract).
- An amount, date or ratio that is *not applicable* or *not calculable* is `null` and always carries
  a reason code (`etaReasonCode`), a blocker, or a route `explanations` entry — never an available
  `"0.00"`, never a silent absence.
- Validation and domain errors return **RFC 7807** `ProblemDetail` (`api/common/ProblemDetailAdvice`)
  with a stable `code`, a human-readable `detail` and `violations` when applicable.
- The roadmap is **read-only**. It creates, changes and deletes nothing; a read MUST NOT mutate a
  stored fact.
- Every derived value carries a `provenance` entry labelled `actual`, `user-assumed` or
  `calculated` (oracle §11).

## Resource Operations

| Method and path | Purpose |
|-----------------|---------|
| `GET /roadmap?asOf={date}` | Read the ordered roadmap of the caller |

- `asOf` is optional. When omitted, the server evaluates the roadmap at its business date
  (`RoadmapBusinessDate`); the evaluated date is always echoed in `asOf`.
- Authentication is required (session cookie). An unauthenticated request returns
  `401 AUTH_REQUIRED`.
- The roadmap is owner-scoped by construction: it is built from the caller's own profile, debts and
  goals. There is no owner or goal parameter, therefore **no cross-owner read exists** — the
  roadmap can only ever contain the caller's own data (spec FR-007).
- 005 adds **no write endpoint**. Stage priority is the existing goal priority and is edited
  through `PUT /goals/{id}`.

### Related (not part of this feature)

| Method and path | Owner |
|-----------------|-------|
| `GET /gps?goalId={uuid}&asOf={date}` | 004 — single-destination deep dive |
| `GET /goals`, `POST /goals`, `PUT /goals/{id}`, `DELETE /goals/{id}` | 003 — goal facts and `priority` |
| `GET /debts/summary` | 002 — portfolio facts and projection |

## Roadmap Response Shape

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
      "requiredMonthlyCapacity": "5000000.00",
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
  "explanations": [
    "The route is ordered by the default policy: debt elimination, then the emergency-fund goal, then the remaining goals by priority.",
    "Stage 2 is not current because stage 1 is not complete yet."
  ],
  "provenance": [
    {"field": "stages[0].requiredMonthlyCapacity", "kind": "actual", "detail": "Planned monthly payment stored on the Feature 002 debt portfolio"},
    {"field": "stages[0].projectedCompletionDate", "kind": "calculated", "detail": "Feature 002 portfolio projection as of 2026-10-22"},
    {"field": "stages[1].progress", "kind": "calculated", "detail": "Feature 003 goal progress as of 2026-10-22"},
    {"field": "stages[1].requiredMonthlyCapacity", "kind": "calculated", "detail": "Feature 003 required capacity over the remaining months to the target date"},
    {"field": "stages[1].projectedCompletionDate", "kind": "calculated", "detail": "Single-destination hypothesis over Available Capacity 31000000.00 VND; ignores competition between stages"}
  ]
}
```

## Field Rules

### Route level

| Field | Type | Rules |
|---|---|---|
| `asOf` | string (ISO date) | The evaluated date. Always present. |
| `currency` | string (ISO-4217) | The owner's currency, echoed once. Every amount in the document is in this currency. When no profile exists the existing single-currency default (`VND`, `ProfileAssembler.currencyOf`) is used. |
| `routeStatus` | `IN_PROGRESS` \| `COMPLETED` \| `NO_DESTINATION` | `NO_DESTINATION` ⟺ `stages` is empty. `COMPLETED` ⟺ `stages` is non-empty and every stage is `COMPLETED`. `IN_PROGRESS` ⟺ exactly one stage is `CURRENT`. |
| `currentStageOrder` | integer \| `null` | `null` unless `routeStatus = IN_PROGRESS`. Points at the stage with `status = "CURRENT"`. |
| `orderPolicy.policy` | string | Always `DEBT_THEN_EMERGENCY_FUND_THEN_PRIORITY` in v1. |
| `orderPolicy.description` | string | Human-readable statement of the applied order. |
| `orderPolicy.appliedOverrides` | string[] | Empty in v1 (Feature 008 introduces overrides). |
| `stages` | array | Ordered by `order`, 1-based, contiguous. May be empty. |
| `explanations` | string[] | At least one entry when `routeStatus = IN_PROGRESS` (why the current stage is current, and why the next one is not). |
| `provenance` | array | One entry per derived field. |

### Stage level

| Field | Type | Rules |
|---|---|---|
| `order` | integer | 1-based position. |
| `kind` | `DEBT_ELIMINATION` \| `GOAL` | At most one `DEBT_ELIMINATION` stage, always at order 1. |
| `goalId` | uuid \| `null` | `null` for the debt stage; never null for a `GOAL` stage. |
| `objective` | string | The stage goal. |
| `startCondition` | string | Debt stage: "no prerequisite". Goal stage: "all earlier stages are complete". |
| `completionCondition` | string | The stage's completion condition in user-readable form. |
| `requiredMonthlyCapacity` | decimal string \| `null` | `null` when not applicable (undated goal); the route `explanations` carries the `UNDATED_GOAL` wording (004 defines no such blocker code). Never `"0.00"` for "not applicable". For the debt stage it quotes the 002 planned monthly payment (committed capacity), not a 003 required capacity. |
| `progress` | decimal string \| `null` | `null` for the debt stage (a portfolio has no progress ratio). A `DEBT_FREEDOM` goal produces no stage at all, so it never appears here. The scale follows the 003 `GoalProgress` value (four decimals). |
| `projectedCompletionDate` | ISO date \| `null` | `null` ⟹ `etaReasonCode` is present. Never fabricated. |
| `etaBasis` | `PORTFOLIO_PROJECTION` \| `SINGLE_DESTINATION_HYPOTHESIS` | The debt stage uses `PORTFOLIO_PROJECTION` (the 002 portfolio projection); a goal stage uses `SINGLE_DESTINATION_HYPOTHESIS`. |
| `etaReasonCode` | string \| `null` | Reuses `GpsEta.Reason` verbatim (004): `MISSING_FINANCIAL_PROFILE`, `NO_AVAILABLE_CAPACITY`, `PAYOFF_HORIZON_EXCEEDS_MAXIMUM`, `PORTFOLIO_CONTAINS_BLOCKED_DEBTS`, plus the payment/interest reasons when the underlying projection supplies them. |
| `status` | `COMPLETED` \| `CURRENT` \| `UPCOMING` | A stage is never `COMPLETED` while an earlier stage is not. |
| `blockedBy` | integer \| `null` | Order of the first incomplete earlier stage; `null` for `COMPLETED` and `CURRENT`. |
| `blockers` | array | `{ "code": "...", "explanation": "..." }`. Codes are 004 `GpsBlocker.Code` values for goal stages and 002 blocked-debt reason codes for the debt stage. `UNDATED_GOAL` / `CAPACITY_SHORTFALL` are 004 next-action input tokens, never blocker codes. |
| `nextAction` | string | One actionable sentence. Never empty. |

## Error Semantics

| Situation | Status | Body |
|---|---|---|
| No session / expired session | `401` | `ProblemDetail` (`AUTH_REQUIRED`) |
| Invalid `asOf` | `400` | `ProblemDetail` (`VALIDATION_FAILED`) with `violations`, a `string[]` in the existing `<parameter>: <message>` shape (e.g. `asOf: must be a valid ISO date`). Requires the shared `MethodArgumentTypeMismatchException` handler added in T023 — Spring's default body carries no `code`/`violations`. |
| Cross-owner access to another owner's goal (e.g. `GET /goals/{id}`) | `404` | `ProblemDetail` (`RESOURCE_NOT_FOUND`) — resource hiding, never `403`. `GET /roadmap` has no owner or goal parameter, so this case cannot arise through it; the rule stays on the reused goal endpoints (RM-014). |
| Missing Financial Profile | `200` | Roadmap with `MISSING_FINANCIAL_PROFILE` in `blockers`/`explanations`; no capacity value |
| No debt (or a COMPLETED portfolio) and no stage-producing goal | `200` | `routeStatus = "NO_DESTINATION"`, `stages = []`. Includes the case where the only non-archived goal is `DEBT_FREEDOM` (it creates no stage). |

## Example — no destination

```json
{
  "asOf": "2026-10-22",
  "currency": "VND",
  "routeStatus": "NO_DESTINATION",
  "currentStageOrder": null,
  "orderPolicy": {
    "policy": "DEBT_THEN_EMERGENCY_FUND_THEN_PRIORITY",
    "description": "Debt elimination, then emergency-fund goals, then the remaining goals by priority.",
    "appliedOverrides": []
  },
  "stages": [],
  "explanations": ["No destination is available: there is no active debt and no goal to work towards."],
  "provenance": []
}
```

## Example — a blocked later stage

```json
{
  "order": 2,
  "kind": "GOAL",
  "goalId": "6f1c9d54-2f0b-4a1e-9c33-0b7d5a1e9f42",
  "objective": "House fund",
  "startCondition": "All earlier stages are complete",
  "completionCondition": "AMOUNT_REACHED",
  "requiredMonthlyCapacity": "8000000.00",
  "progress": "0.1000",
  "projectedCompletionDate": null,
  "etaBasis": "SINGLE_DESTINATION_HYPOTHESIS",
  "etaReasonCode": "NO_AVAILABLE_CAPACITY",
  "status": "UPCOMING",
  "blockedBy": 1,
  "blockers": [
    {"code": "NO_AVAILABLE_CAPACITY", "explanation": "Available Capacity is 0.00 VND, so no completion date can be calculated for this stage."}
  ],
  "nextAction": "Complete stage 1 first; this stage becomes current when stage 1 is complete."
}
```
