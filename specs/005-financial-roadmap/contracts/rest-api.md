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

## Two Independent Orders (contract rule)

`orderPolicy` describes the **roadmap stage order** only (`scope = ROADMAP_STAGE_ORDER`). It is not
the allocation order of `calculation-rules.md` §7–§8, which Feature 008 owns.

- `status` (stage status) is the stage's position on the roadmap. It is **not** an allocation or
  funding status. `UPCOMING` does not mean the stage is unfunded or that money must not go to it.
- v1 returns **no** `allocationStatus`, funded amount or shortfall amount; 005 has no allocation
  data to report.
- `completionReached` reports the stage's own completion condition independently of `status`.
- A later stage may already hold money while an earlier stage is incomplete; the roadmap never
  contradicts that allocation (spec §7.2, §7.4, RM-016).

## Roadmap Response Shape

```json
{
  "asOf": "2026-10-22",
  "currency": "VND",
  "routeStatus": "IN_PROGRESS",
  "currentStageOrder": 1,
  "orderPolicy": {
    "policy": "DEBT_THEN_EMERGENCY_FUND_THEN_PRIORITY",
    "scope": "ROADMAP_STAGE_ORDER",
    "description": "Roadmap stage order: debt elimination, then emergency-fund goals, then the remaining goals by priority. This is not the allocation order.",
    "appliedOverrides": []
  },
  "stages": [
    {
      "order": 1,
      "kind": "DEBT_ELIMINATION",
      "goalId": null,
      "objective": "Eliminate the outstanding debt",
      "startCondition": "No prerequisite — this is the first stage on the roadmap",
      "completionCondition": "Debt portfolio status is COMPLETED (no ACTIVE debt remains)",
      "completionReached": false,
      "requiredMonthlyCapacity": null,
      "totalPlannedMonthlyPayment": "5000000.00",
      "progress": null,
      "projectedCompletionDate": "2027-04-30",
      "etaBasis": "PORTFOLIO_PROJECTION",
      "etaAssumption": "PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA",
      "etaReasonCode": null,
      "status": "CURRENT",
      "blockedBy": null,
      "blockers": [],
      "nextAction": "Keep paying the planned monthly payment on every debt. The payoff date assumes every planned payment, including any Debt Extra, is made in full."
    },
    {
      "order": 2,
      "kind": "GOAL",
      "goalId": "6f1c9d54-2f0b-4a1e-9c33-0b7d5a1e9f42",
      "objective": "Emergency fund",
      "startCondition": "Every earlier roadmap stage is complete. Roadmap position only: the allocation rules decide funding, not this condition.",
      "completionCondition": "AMOUNT_REACHED",
      "completionReached": false,
      "requiredMonthlyCapacity": "3000000.00",
      "totalPlannedMonthlyPayment": null,
      "progress": "0.2500",
      "projectedCompletionDate": "2027-01-31",
      "etaBasis": "SINGLE_DESTINATION_HYPOTHESIS",
      "etaAssumption": "WHOLE_AVAILABLE_CAPACITY",
      "etaReasonCode": null,
      "status": "UPCOMING",
      "blockedBy": 1,
      "blockers": [],
      "nextAction": "Stage 1 must complete before this stage becomes current on the roadmap. Its position does not mean it is unfunded; how money is allocated is decided by the allocation rules, not by stage status."
    }
  ],
  "explanations": [
    "The route is ordered by the default stage policy: debt elimination, then the emergency-fund goal, then the remaining goals by priority.",
    "Stage order is a roadmap position, not an allocation order: how money is split across stages is decided by the allocation rules, not by stage status.",
    "Stage 2 is not current because stage 1 is not complete yet; its position does not mean it is unfunded.",
    "The debt-stage date is the 002 plan-based projection. It assumes every stored planned payment, including each planned Debt Extra, is made in full, and it is not a route-funded date."
  ],
  "provenance": [
    {"field": "stages[0].totalPlannedMonthlyPayment", "kind": "actual", "detail": "Total planned monthly payment stored on the Feature 002 debt portfolio"},
    {"field": "stages[0].projectedCompletionDate", "kind": "calculated", "detail": "Feature 002 plan-based portfolio projection as of 2026-10-22; assumes the stored planned payment, including Debt Extra, is paid in full"},
    {"field": "stages[0].etaAssumption", "kind": "user-assumed", "detail": "The planned extra payment is made in full; 005 does not simulate allocation"},
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
| `orderPolicy.scope` | string | Always `ROADMAP_STAGE_ORDER` in v1. States that the policy orders **stages**, not money: it is not the allocation order. |
| `orderPolicy.description` | string | Human-readable statement of the applied **stage** order, including that it is not the allocation order. |
| `orderPolicy.appliedOverrides` | string[] | Empty in v1 (Feature 008 introduces overrides). |
| `stages` | array | Ordered by `order`, 1-based, contiguous. May be empty. |
| `explanations` | string[] | At least one entry when `stages` is non-empty. It MUST separate stage order from allocation (`UPCOMING` is not a funding statement) and MUST carry the debt ETA assumption when a debt stage exists. The 004 capacity-comparison shortfall amount is never quoted (RM-015). |
| `provenance` | array | One entry per derived field. |

### Stage level

| Field | Type | Rules |
|---|---|---|
| `order` | integer | 1-based position. |
| `kind` | `DEBT_ELIMINATION` \| `GOAL` | At most one `DEBT_ELIMINATION` stage, always at order 1. |
| `goalId` | uuid \| `null` | `null` for the debt stage; never null for a `GOAL` stage. |
| `objective` | string | The stage goal. |
| `startCondition` | string | Debt stage: "no prerequisite". Goal stage: "all earlier roadmap stages are complete" plus the explicit statement that this is a roadmap position only and does not gate funding. |
| `completionCondition` | string | The stage's completion condition in user-readable form. |
| `completionReached` | boolean | The stage's **own** completion condition (debt: portfolio COMPLETED; goal: `AMOUNT_REACHED`), independent of `status`. `true` with `status = "UPCOMING"` is valid and MUST be rendered as a reached badge (RM-006, RM-016). |
| `requiredMonthlyCapacity` | decimal string \| `null` | Goal stage only: the 003 required capacity; `null` for an undated goal and always `null` for the debt stage. The route `explanations` carries the `UNDATED_GOAL` wording (004 defines no such blocker code). Never `"0.00"` for "not applicable". |
| `totalPlannedMonthlyPayment` | decimal string \| `null` | Debt stage only: the 002 `totalPlannedMonthlyPayment` fact, quoted unchanged; always `null` for a goal stage. Not a required capacity. |
| `progress` | decimal string \| `null` | `null` for the debt stage (a portfolio has no progress ratio). A `DEBT_FREEDOM` goal produces no stage at all, so it never appears here. The scale follows the 003 `GoalProgress` value (four decimals). |
| `projectedCompletionDate` | ISO date \| `null` | `null` ⟹ `etaReasonCode` is present. Never fabricated. The date is a forwarded projection: plan-based for the debt stage, single-destination hypothesis for a goal stage; after 008, route-aware (see "ETA Sources"). |
| `etaBasis` | `PORTFOLIO_PROJECTION` \| `SINGLE_DESTINATION_HYPOTHESIS` \| `ROUTE_PROJECTION` (reserved, 008) | The debt stage uses `PORTFOLIO_PROJECTION` (the 002 **plan-based** projection); a goal stage uses `SINGLE_DESTINATION_HYPOTHESIS`. `ROUTE_PROJECTION` is reserved for the 008 route-aware projection and is never emitted by v1. |
| `etaAssumption` | `PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA` \| `WHOLE_AVAILABLE_CAPACITY` \| `ROUTE_ALLOCATION` (reserved, 008) | What the date assumes. Debt: the stored `plannedPayment`, including the requested Debt Extra, is paid in full. Goal: the whole Available Capacity is directed at the stage. `ROUTE_ALLOCATION` is reserved for 008. |
| `etaReasonCode` | string \| `null` | Reuses `GpsEta.Reason` verbatim (004): `MISSING_FINANCIAL_PROFILE`, `NO_AVAILABLE_CAPACITY`, `PAYOFF_HORIZON_EXCEEDS_MAXIMUM`, `PORTFOLIO_CONTAINS_BLOCKED_DEBTS`, plus the payment/interest reasons when the underlying projection supplies them. |
| `status` | `COMPLETED` \| `CURRENT` \| `UPCOMING` | The stage's **position on the roadmap only**. A stage is never positionally `COMPLETED` while an earlier stage is not. `UPCOMING` is not an allocation status and never means "unfunded". |
| `blockedBy` | integer \| `null` | Order of the first incomplete earlier stage; `null` for `COMPLETED` and `CURRENT`. |
| `blockers` | array | `{ "code": "...", "explanation": "..." }`. Codes are 004 `GpsBlocker.Code` values for goal stages and 002 blocked-debt reason codes for the debt stage. `UNDATED_GOAL` / `CAPACITY_SHORTFALL` are 004 next-action input tokens, never blocker codes. |
| `nextAction` | string | One actionable, amount-free sentence. Never empty. An `UPCOMING` goal stage names the earlier stage that must complete and states that its position is not a funding statement (and says the stage is reached when `completionReached = true`); a `CURRENT` goal stage uses the 004 action **type** (never a description that embeds money); a `COMPLETED` stage states that the objective is met. The 004 capacity shortfall amount is never transported (RM-015). |

## ETA Sources and the 005 → 008 Transition

| Stage kind | `etaBasis` | `etaAssumption` | Source |
|---|---|---|---|
| `DEBT_ELIMINATION` | `PORTFOLIO_PROJECTION` | `PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA` | 002 portfolio projection: every debt amortizes on its stored `plannedPayment` (`minimumPayment + requested Debt Extra`) |
| `GOAL` | `SINGLE_DESTINATION_HYPOTHESIS` | `WHOLE_AVAILABLE_CAPACITY` | `GpsEtaCalculator.ceilPeriods` over Available Capacity (004 D-2) |
| any (reserved) | `ROUTE_PROJECTION` | `ROUTE_ALLOCATION` | 008 route-aware projection, forwarded unchanged by 005 |

Contract rules:

- The debt-stage date is **plan-based, not route-funded**. `plannedPayment` requests a Debt Extra; it
  does not prove the allocation funded it. When Available Capacity cannot fund the whole requested
  extra, the response still carries the plan-based labels and an explanation states that the date
  assumes the planned extra is paid in full (RM-017).
- 005 never returns `FundedExtra`, `Effective Payment`, a funded amount or a payment shortfall.
- 008 owns the route-aware ETA (`specs/008-financial-timing-allocation/open-decisions.md` C-03). When
  it ships, 005 forwards the supplied date and labels unchanged; it performs no allocation simulation
  and no second ETA computation (RM-018).
- Until 008 is implemented, `ROUTE_PROJECTION` / `ROUTE_ALLOCATION` never appear in a response.

## Error Semantics

| Situation | Status | Body |
|---|---|---|
| No session / expired session | `401` | `ProblemDetail` (`AUTH_REQUIRED`) |
| Invalid `asOf` | `400` | `ProblemDetail` (`VALIDATION_FAILED`) with `violations`, a `string[]` in the existing `<parameter>: <message>` shape (e.g. `asOf: must be a valid ISO date`). Requires the shared `MethodArgumentTypeMismatchException` handler added in T023 — Spring's default body carries no `code`/`violations`. |
| Cross-owner access to another owner's goal (e.g. `GET /goals/{id}`) | `404` | `ProblemDetail` (`RESOURCE_NOT_FOUND`) — resource hiding, never `403`. `GET /roadmap` has no owner or goal parameter, so this case cannot arise through it; the rule stays on the reused goal endpoints (RM-014). |
| Missing Financial Profile | `200` | Roadmap with `MISSING_FINANCIAL_PROFILE` in `blockers`/`explanations`; no capacity value |
| No debt (or a COMPLETED portfolio) and no stage-producing goal | `200` | `routeStatus = "NO_DESTINATION"`, `stages = []`. Includes the case where the only non-archived goal is `DEBT_FREEDOM` (it creates no stage). |
| A later stage's own condition is met while an earlier stage is incomplete | `200` | The stage is `status = "UPCOMING"` with `blockedBy` and `completionReached = true`; an explanation preserves the reached state (RM-006, RM-016). |
| Debt Extra requested but not fully fundable from Available Capacity | `200` | The debt-stage ETA stays plan-based: `etaBasis = "PORTFOLIO_PROJECTION"`, `etaAssumption = "PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA"`, and an explanation says the date assumes the planned extra is paid in full (RM-017). No funded/shortfall amount is added. |
| Feature 008 not implemented | `200` | The v1 ETA labels are used; `ROUTE_PROJECTION` / `ROUTE_ALLOCATION` never appear (RM-018). |

## Example — no destination

```json
{
  "asOf": "2026-10-22",
  "currency": "VND",
  "routeStatus": "NO_DESTINATION",
  "currentStageOrder": null,
  "orderPolicy": {
    "policy": "DEBT_THEN_EMERGENCY_FUND_THEN_PRIORITY",
    "scope": "ROADMAP_STAGE_ORDER",
    "description": "Roadmap stage order: debt elimination, then emergency-fund goals, then the remaining goals by priority. This is not the allocation order.",
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
  "startCondition": "Every earlier roadmap stage is complete. Roadmap position only: the allocation rules decide funding, not this condition.",
  "completionCondition": "AMOUNT_REACHED",
  "completionReached": false,
  "requiredMonthlyCapacity": "8000000.00",
  "totalPlannedMonthlyPayment": null,
  "progress": "0.1000",
  "projectedCompletionDate": null,
  "etaBasis": "SINGLE_DESTINATION_HYPOTHESIS",
  "etaAssumption": "WHOLE_AVAILABLE_CAPACITY",
  "etaReasonCode": "NO_AVAILABLE_CAPACITY",
  "status": "UPCOMING",
  "blockedBy": 1,
  "blockers": [
    {"code": "NO_AVAILABLE_CAPACITY", "explanation": "Available Capacity is 0.00 VND, so no completion date can be calculated for this stage."}
  ],
  "nextAction": "Stage 1 must complete before this stage becomes current on the roadmap. Its position does not mean it is unfunded; how money is allocated is decided by the allocation rules, not by stage status."
}
```

## Example — a reached goal held at `UPCOMING` (RM-006 / RM-016)

The goal already holds enough money to meet its own condition, but the debt stage is still open. The
response preserves the financial completion and never presents the position as a funding statement;
there is no `allocationStatus` or funded amount.

```json
{
  "order": 2,
  "kind": "GOAL",
  "goalId": "0b1e7a2c-6d4f-4b8e-9a11-2c3d4e5f6a7b",
  "objective": "Emergency fund",
  "startCondition": "Every earlier roadmap stage is complete. Roadmap position only: the allocation rules decide funding, not this condition.",
  "completionCondition": "AMOUNT_REACHED",
  "completionReached": true,
  "requiredMonthlyCapacity": "0.00",
  "totalPlannedMonthlyPayment": null,
  "progress": "1.0000",
  "projectedCompletionDate": "2026-10-22",
  "etaBasis": "SINGLE_DESTINATION_HYPOTHESIS",
  "etaAssumption": "WHOLE_AVAILABLE_CAPACITY",
  "etaReasonCode": null,
  "status": "UPCOMING",
  "blockedBy": 1,
  "blockers": [],
  "nextAction": "Stage 1 must complete before this stage becomes current on the roadmap. The goal itself has already reached its target; its position is not a funding statement."
}
```

## Example — debt ETA with a requested but unfunded Debt Extra (RM-017)

`plannedPayment` is 5,000,000.00 while `minimumPayment` is 3,500,000.00; Available Capacity cannot
fund the whole 1,500,000.00 requested extra. The date stays the 002 plan-based projection and the
assumption is explicit. No funded amount or shortfall is added.

```json
{
  "order": 1,
  "kind": "DEBT_ELIMINATION",
  "goalId": null,
  "objective": "Eliminate the outstanding debt",
  "startCondition": "No prerequisite — this is the first stage on the roadmap",
  "completionCondition": "Debt portfolio status is COMPLETED (no ACTIVE debt remains)",
  "completionReached": false,
  "requiredMonthlyCapacity": null,
  "totalPlannedMonthlyPayment": "5000000.00",
  "progress": null,
  "projectedCompletionDate": "2028-02-29",
  "etaBasis": "PORTFOLIO_PROJECTION",
  "etaAssumption": "PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA",
  "etaReasonCode": null,
  "status": "CURRENT",
  "blockedBy": null,
  "blockers": [],
  "nextAction": "Keep paying the planned amount on every debt; the payoff date assumes the planned extra is paid in full."
}
```
