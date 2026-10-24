# Data Model: Financial Roadmap (005)

**Phase**: 1 (Design & Contracts) — `$speckit-plan`
**Date**: 2026-10-22
**Source**: [spec.md](spec.md) §6, §7, [research.md](research.md) §3

> **Persistence: none.** The roadmap is a derived read model (spec FR-004, FR-010). This feature
> adds **no table, no migration, no entity and no write endpoint**. The only persisted facts it
> reads are the ones owned by 001 (profile), 002 (debt) and 003 (goal, including `goal.priority`).

---

## 1. Domain entities (pure, `com.financialgps.domain.roadmap`)

### 1.1 `Roadmap`

| Field | Type | Required | Source / rule |
|---|---|---|---|
| `asOf` | `LocalDate` | yes | Passed in explicitly by the application layer; never read from a clock in the domain. |
| `currency` | `String` (ISO-4217) | yes | The owner's currency, echoed once at route level; a roadmap never mixes currencies (`Money` contract, 001/002/003). |
| `routeStatus` | `RouteStatus` | yes | `IN_PROGRESS` \| `COMPLETED` \| `NO_DESTINATION` (spec D-6). |
| `currentStageOrder` | `Integer` | no (`null` when `COMPLETED` or `NO_DESTINATION`) | Order of the first stage whose completion condition is not met. |
| `stages` | `List<RoadmapStage>` | yes (may be empty) | Ordered by `order`, 1-based, contiguous. |
| `orderPolicy` | `OrderPolicy` | yes | `policy`, `description`, `appliedOverrides` (empty in v1). |
| `explanations` | `List<String>` | yes | User-readable statements about the order and the transitions; never the 004 capacity-comparison shortfall amount (RM-015). |
| `provenance` | `List<ProvenanceEntry>` | yes | One entry per derived field (oracle §11). |

Invariants:

- `stages` order is contiguous from 1 and strictly increasing in `order`.
- `routeStatus = NO_DESTINATION` ⟺ `stages.isEmpty()`.
- `routeStatus = COMPLETED` ⟺ `stages` non-empty and every stage `status = COMPLETED`.
- `routeStatus = IN_PROGRESS` ⟺ exactly one stage has `status = CURRENT`.
- `currentStageOrder` is null unless `routeStatus = IN_PROGRESS`.

### 1.2 `RoadmapStage`

| Field | Type | Required | Source / rule |
|---|---|---|---|
| `order` | `int` | yes | 1-based position in the route. |
| `kind` | `StageKind` | yes | `DEBT_ELIMINATION` \| `GOAL`. |
| `goalId` | `UUID` | only for `GOAL` | The 003 goal id; `null` for the debt stage. |
| `objective` | `String` | yes | Debt stage: fixed text. Goal stage: the goal name (003). |
| `startCondition` | `String` | yes | Debt stage: "no prerequisite". Goal stage: "all earlier stages are complete". |
| `completionCondition` | `String` | yes | Debt stage: portfolio COMPLETED. Goal stage: `AMOUNT_REACHED` (003); `DEBT_FREEDOM` goals produce no goal stage (§1.2, spec §7.1). |
| `requiredMonthlyCapacity` | `Money` | no (`null` = not applicable) | Goal stage: `GoalCapacity.requiredMonthlyCapacity` (003); `null` for an undated goal. Always `null` for the debt stage. |
| `totalPlannedMonthlyPayment` | `Money` | yes for `DEBT_ELIMINATION`; `null` for `GOAL` | Debt stage: the 002 `totalPlannedMonthlyPayment` fact, quoted unchanged. Always `null` for a goal stage. |
| `progress` | `BigDecimal` | no (`null` = not applicable) | Goal stage: `GoalProgress.progress` clamped to `[0, 1]`. Debt stage: `null` (spec D-3). |
| `projectedCompletionDate` | `LocalDate` | no (`null` = not calculable) | Debt stage: `projectedDebtFreeDate` (002). Goal stage: the ETA hypothesis (spec §7.6). |
| `etaBasis` | `EtaBasis` | yes | `PORTFOLIO_PROJECTION` (debt stage) \| `SINGLE_DESTINATION_HYPOTHESIS` (goal stage). |
| `etaReasonCode` | `String` | no | Present only when `projectedCompletionDate` is `null`; reuses `GpsEta.Reason` (004) verbatim. |
| `status` | `StageStatus` | yes | `COMPLETED` \| `CURRENT` \| `UPCOMING`. |
| `blockedBy` | `Integer` | no | Order of the first incomplete earlier stage; `null` for `COMPLETED`/`CURRENT`. |
| `blockers` | `List<Blocker>` | yes (may be empty) | Reused `{code, explanation}` shape. Goal-stage codes are 004 `GpsBlocker.Code` values; the debt stage carries 002 blocked-debt reason codes. `UNDATED_GOAL` is a next-action input token, not a blocker code. |
| `nextAction` | `String` | yes | One actionable, amount-free sentence per stage (004 vocabulary where it exists). Stage-state aware (goal stage): sequence-gate sentence for `UPCOMING`, 004 action **type** for `CURRENT`, completion sentence for `COMPLETED`; the debt stage uses the 002 projection explanation. Never carries the 004 capacity shortfall (RM-015). |

Invariants:

- `kind = DEBT_ELIMINATION` ⟹ `goalId = null`, `requiredMonthlyCapacity = null`,
  `totalPlannedMonthlyPayment != null`, and there is **at most one** debt stage per roadmap.
- `kind = GOAL` ⟹ `goalId != null`, `totalPlannedMonthlyPayment = null`, and no goal appears twice.
  `requiredMonthlyCapacity` is `null` only when the goal has no target date.
- A `DEBT_FREEDOM` goal never yields a `GOAL` stage (it mirrors the debt stage's condition), so a
  roadmap whose only non-archived goal is `DEBT_FREEDOM` has no goal stages.
- `status = COMPLETED` ⟹ `blockedBy = null` and the stage's completion condition holds.
- `status = CURRENT` ⟹ `blockedBy = null` and every earlier stage is `COMPLETED`.
- `status = UPCOMING` ⟹ `blockedBy != null` and `blockedBy < order`.
- `status = UPCOMING` does **not** imply the stage's own completion condition is unmet: the sequence
  gate holds a satisfied later stage at `UPCOMING` until every earlier stage completes (RM-006).
- `progress` is `null` or within `[0, 1]`.
- No field carries the 004 capacity-comparison shortfall amount; `nextAction` and `explanations`
  are amount-free in that respect (RM-015).
- `projectedCompletionDate = null` ⟹ `etaReasonCode != null` (never a silent absence).

### 1.3 Value objects and enumerations

| Type | Values | Notes |
|---|---|---|
| `RouteStatus` | `IN_PROGRESS`, `COMPLETED`, `NO_DESTINATION` | Route-level only. |
| `StageKind` | `DEBT_ELIMINATION`, `GOAL` | — |
| `StageStatus` | `COMPLETED`, `CURRENT`, `UPCOMING` | Stage-level only; does **not** redefine the 004 Financial Status vocabulary. |
| `EtaBasis` | `PORTFOLIO_PROJECTION`, `SINGLE_DESTINATION_HYPOTHESIS` | The label that keeps the ETA honest (spec §7.6): the debt stage quotes the 002 portfolio projection, a goal stage quotes the single-destination hypothesis. |
| `OrderPolicy` | `policy = DEBT_THEN_EMERGENCY_FUND_THEN_PRIORITY`, `description`, `appliedOverrides: List<String>` | `appliedOverrides` is empty in v1 (008 introduces real overrides). |

### 1.4 Input to the generator (application → domain)

```text
RoadmapInput
  asOf            : LocalDate
  profile         : ProfileSnapshot?      // null ⇒ MISSING_FINANCIAL_PROFILE
  debts           : DebtPortfolioFacts?   // null ⇒ no debt stage
  goals           : List<GoalSnapshot>    // non-archived goals (ACTIVE + COMPLETED), already owner-scoped; DEBT_FREEDOM goals are ignored by the generator
```

`ProfileSnapshot` carries `currency` (ISO-4217) and `availableCapacity : Money?` (`null` when the
profile is missing). The currency is what the roadmap echoes at route level; when the profile is
missing the adapter uses the existing `ProfileAssembler.currencyOf(profile)` default (`VND`), so the
route always echoes a currency and never invents a second default.

`GoalSnapshot` carries the 003 facts the roadmap needs — id, name, goal type, completion
condition, target/current amounts, target date, priority, `createdAt` — plus the **derived** values
that 003 already computes and 005 must not recompute: `remaining`, `progress`,
`requiredMonthlyCapacity`, `capacityCoverage`, `dateFeasibility`.

`DebtPortfolioFacts` carries the 002 facts: `status`, `totalOutstandingDebt`,
`totalPlannedMonthlyPayment`, `projectedDebtFreeDate`, `totalMonthsRemaining`, `blockedDebts`
(code + explanation), `reasonCode`, `explanation`.

**Rule**: the domain generator receives *facts and already-computed quantities*, never entities,
repositories, `OwnerId`-scoped queries or a clock.

**Ordering**: the goal list arrives already ordered by `Goal.ORDERING` (`GoalStore.findAllByOwner`
returns `(priority, createdAt, id)`, nulls first). The roadmap preserves that order inside each
tier and MUST NOT define a second ordering key.

**Available Capacity** is **not** recomputed by the roadmap: it comes from the canonical
`CurrentPositionCalculator` (004) that the profile adapter calls. The roadmap MUST NOT introduce a
second cash-flow formula.

---

## 2. Application model (`com.financialgps.application.roadmap.model.RoadmapModels`)

Transport-shaped records mirroring the domain read model:

| Record | Fields |
|---|---|
| `RoadmapView` | `asOf`, `currency`, `routeStatus`, `currentStageOrder`, `stages`, `orderPolicy`, `explanations`, `provenance` |
| `RoadmapStageView` | `order`, `kind`, `goalId`, `objective`, `startCondition`, `completionCondition`, `requiredMonthlyCapacity`, `totalPlannedMonthlyPayment`, `progress`, `projectedCompletionDate`, `etaBasis`, `etaReasonCode`, `status`, `blockedBy`, `blockers`, `nextAction` |
| `OrderPolicyView` | `policy`, `description`, `appliedOverrides` |
| `BlockerView` | `code`, `explanation` |
| `ProvenanceView` | `field`, `kind` (`actual` \| `user-assumed` \| `calculated`), `detail` |

Transport rules (identical to 001-004): amounts as decimal strings, ratios as decimal strings,
dates as ISO-8601, `currency` echoed once at route level, `null` for *not applicable / not
calculable* (never `0`).

---

## 3. Ports

### 3.1 Input ports (`application/roadmap/port.in`)

| Port | Signature | Transaction |
|---|---|---|
| `GetRoadmap` | `RoadmapModels.RoadmapView roadmap(OwnerId owner, LocalDate asOfDate)` and `roadmap(OwnerId owner)` | read-only |

### 3.2 Output ports (`application/roadmap/port.out`)

| Port | Purpose | Adapter (infrastructure) |
|---|---|---|
| `RoadmapGoalReader` | owner-scoped non-archived goals with 003-derived values | `infrastructure/persistence/goal/RoadmapGoalAdapter` (over `GoalStore`) |
| `RoadmapDebtReader` | owner-scoped 002 portfolio projection | `infrastructure/persistence/debt/RoadmapDebtAdapter` (over `GetDebtSummary`) |
| `RoadmapProfileReader` | owner-scoped profile facts + Available Capacity | `infrastructure/persistence/profile/RoadmapProfileAdapter` (over the 001 profile reader) |
| `RoadmapBusinessDate` | the business date when the client omits `asOf` | `infrastructure/time/RoadmapBusinessDateAdapter` (NEW — delegates to the existing `SystemBusinessDate`, mirroring `GpsBusinessDateAdapter`) |

Every output port MUST have at least one infrastructure adapter and MUST be wired through
`infrastructure/configuration/RoadmapUseCaseConfiguration` — enforced by
`ArchitectureGuardTest` (see the guard's "port has adapter / port is wired" rules).

---

## 4. Domain state transitions

| From | Event | To | Rule |
|---|---|---|---|
| — | Read with ACTIVE debt | Roadmap with a `DEBT_ELIMINATION` stage at order 1 | The debt tier is always first (oracle §8). |
| — | Read without ACTIVE debt | Roadmap without a debt stage | The portfolio status is `COMPLETED` or empty. |
| — | Read with goals | One `GOAL` stage per non-archived, non-`DEBT_FREEDOM` goal | Goals are ordered `(priority, createdAt, id)`; the `EMERGENCY_FUND` tier precedes the others. |
| Stage `UPCOMING` | An earlier stage becomes complete | The first still-incomplete stage becomes `CURRENT`; the later one stays `UPCOMING` with `blockedBy` = the new current stage | FR-003 — a later stage is never `COMPLETED` while an earlier one is not. |
| Any stage | Its own completion condition is met while an earlier stage is incomplete | The stage stays `UPCOMING` with `blockedBy` = the earlier incomplete stage; `progress` may already be `1.0` | The sequence gate wins for `status` (RM-006). |
| Stage `CURRENT` | Its completion condition is met | `COMPLETED`; the next **still-incomplete** stage becomes `CURRENT` (any already-satisfied stages before it are also `COMPLETED`) | Same rule; computed on read, never stored. |
| All stages `COMPLETED` | Read | `routeStatus = COMPLETED`, `currentStageOrder = null` | No stage is invented (spec edge case). |
| No debt, no stage-producing goal (no non-archived goal, or only a `DEBT_FREEDOM` goal) | Read | `routeStatus = NO_DESTINATION`, `stages = []` | The roadmap states that no destination is available. |

There is no lifecycle of its own: the roadmap has no persistence, so there is no create/update/
archive transition. Every transition above is a **pure derivation** from the read facts.

---

## 5. Validation rules

| Rule | Where | Failure |
|---|---|---|
| `priority >= 1` | 003 (`Goal`) | `400 VALIDATION_FAILED` / `GOAL_PRIORITY_INVALID` (existing) |
| Amounts are non-negative decimals with the owner's currency | 001/002/003 (`Money`) | `400 VALIDATION_FAILED` (existing) |
| Currency mismatch between profile capacity and goal currency | 003 (`GoalCapacityCalculator`) | `400 VALIDATION_FAILED` / `CURRENCY_MISMATCH` (existing) |
| `asOf` must be a valid ISO date | API (`@RequestParam LocalDate`) + shared `ProblemDetailAdvice` | `400 VALIDATION_FAILED` with `violations` (`string[]`, `"asOf: <message>"`); the `MethodArgumentTypeMismatchException` handler is added by T023 (Spring's default body otherwise) |
| Goal dependency cycle | 008 (not in v1) | n/a in 005 |

005 adds **no new validation rule** — it only reads facts that are already validated.
