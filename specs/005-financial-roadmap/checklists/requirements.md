# Specification Quality Checklist: Financial Roadmap

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-08-24
**Revised**: 2026-10-24 — implementation-ready rewrite, analyze consistency pass, and the round-5
order/ETA review (stage order vs allocation order, plan-based debt ETA, 005 → 008 forwarding)
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details; focused on user value; written for stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements, success criteria, scenarios, edge cases, scope, and assumptions are complete

## Feature Readiness

- [x] Functional requirements have acceptance coverage
- [x] Primary flows are independently testable

## Cross-Artifact Consistency (analyze pass)

- [x] The stage set is identical in spec §4/§7.1, `data-model.md` §1.2, `contracts/rest-api.md`, research R-05/R-14 and task T019: one debt stage plus one stage per non-archived, non-`DEBT_FREEDOM` goal; a `DEBT_FREEDOM`-only owner is `NO_DESTINATION`
- [x] Currency has exactly one home (route-level `currency`) in spec §6/§7.3/§8, `data-model.md` §1.1/§2, the REST contract and tasks T007/T024
- [x] The cross-owner `404` rule is attached to the reused goal endpoints (FR-007, spec §10, contract Error Semantics, task T014) — `GET /roadmap` has no owner parameter
- [x] The ETA reason vocabulary matches Feature 004 (`MISSING_FINANCIAL_PROFILE`; no `PROFILE_MISSING`), and `blockers` carries only real `GpsBlocker.Code` values — `UNDATED_GOAL` / `CAPACITY_SHORTFALL` stay explanation/action tokens, never fabricated blocker codes
- [x] The two capacity quantities stay separate: `requiredMonthlyCapacity` is goal-only (003) and `totalPlannedMonthlyPayment` is debt-only (002 fact); each is `null` on the other kind, so summing `requiredMonthlyCapacity` cannot mix quantities
- [x] FR-006 is testable: RM-015 asserts the 004 single-destination shortfall amount never appears in `nextAction`/`explanations`; only the action **type** is reused
- [x] The unauthenticated code is `AUTH_REQUIRED` everywhere, matching `ProblemDetailAdvice` / `RestAuthenticationEntryPoint`
- [x] The `invalid asOf` contract is backed by a task: T023 adds the `MethodArgumentTypeMismatchException` handler and T013 asserts `400 VALIDATION_FAILED` with `violations`
- [x] The shared-handler impact on 001–004 is documented as a deliberate repo-wide error-contract alignment (plan R-08, research R-17), `violations` keeps the existing `string[]` shape, and T023 reruns `e2e/specs/gps.spec.ts`
- [x] The sequence gate wins over a goal's own completion for positional `status`: a satisfied goal behind an incomplete earlier stage is `UPCOMING` with `blockedBy`, while `completionReached` still reports its own condition (spec §7.1/§7.4, RM-006, data-model invariants, tasks T011/T015/T032)
- [x] Every §12 case maps to a task; the matrix has 18 cases; RM-005 covers the `DEBT_FREEDOM`-only variant, RM-014 covers ownership end to end, RM-015 covers the no-shortfall rule, and RM-016/RM-017/RM-018 cover order separation, the plan-based debt ETA and the 008 ETA seam
- [x] The Constitution XII split is recorded: 005 delivers the **roadmap stage order** + policy disclosure (`scope = ROADMAP_STAGE_ORDER`), 008 owns allocation and route-aware ETA (plan Constitution Check, Cross-Feature Dependencies)
- [x] **Stage order is separated from allocation order everywhere**: spec §2.2/§3/§4/§7.2/§7.4, `data-model.md` §2.1, the REST contract "Two Independent Orders", research R-02/R-18, plan R-02/R-18 and tasks T013/T015/T032/T033. No artifact states or implies that money waits for an earlier stage
- [x] **No allocation data in v1**: `allocationStatus`, funded amount and payment shortfall are absent from spec, model, contract and tasks; `UPCOMING` is documented as a roadmap position, never an unfunded marker
- [x] **The debt-stage ETA is plan-based and labelled**: `PORTFOLIO_PROJECTION` / `PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA` with the explicit "assumes the planned Debt Extra is paid in full, not route-funded" statement in spec §7.6, the contract "ETA Sources", RM-017 and task T017 — consistent with `calculation-rules.md` §4 and 008 C-01
- [x] **008 owns the route-aware ETA and 005 forwards it**: `ROUTE_PROJECTION` / `ROUTE_ALLOCATION` are documented as reserved and never emitted by v1 (spec D-10, data-model §2.2, contract, research R-19, tasks T013/T017/T032); 005 contains no allocation loop and no second ETA algorithm
- [x] **T004 is completed**: `calculation-rules.md` §8.1 records the roadmap-stage-order vs allocation-order distinction without changing the waterfall; the stale "debt strategy feature (005)" label is corrected
- [x] Quickstart commands match AGENTS.md: `--env-file ~/.secrets/financial-gps.env`, and `-Dtest='*Roadmap*Test'` matches every roadmap test class

## Notes

- Re-validated 2026-10-22 against the implementation-ready rewrite of [spec.md](../spec.md).
  Every item still holds; no marker changed state.
- 2026-10-23 review pass: the debt stage now carries the 002 `totalPlannedMonthlyPayment` fact
  separately from the goal-only `requiredMonthlyCapacity` (003), and RM-015 asserts the 004
  capacity-comparison shortfall is never transported in `nextAction`/`explanations`.
  - *Content Quality*: the spec stays written for stakeholders — the JSON in §8 is the **wire
    contract**, the same level 004's spec documents, not an implementation detail.
  - *Requirement Completeness*: no `[NEEDS CLARIFICATION]` marker remains; the five defaults of
    §4 are recorded as decisions and can be overridden by an amendment.
  - *Feature Readiness*: every FR maps to the 18-case verification matrix of §12, and each matrix
    case maps to a task in `tasks.md`.
- 2026-10-24 review round 5 (F-01…F-04): the spec now separates the **roadmap stage order** from the
  008 **allocation order**, adds `completionReached` beside the positional `status`, states that the
  debt-stage ETA is the 002 plan-based projection (not a route-funded ETA) with an explicit
  `etaAssumption`, and fixes the 005 → 008 ETA seam (`ROUTE_PROJECTION` / `ROUTE_ALLOCATION`
  reserved). The same change is carried through `data-model.md`, `contracts/rest-api.md`,
  `research.md`, `plan.md`, `tasks.md`, `quickstart.md` and `calculation-rules.md` §8.1.
  - *Content Quality*: unchanged — the new fields are wire-level facts, not implementation detail.
  - *Requirement Completeness*: the 18-case matrix covers the new rules; no marker was reopened.
  - *Feature Readiness*: implementation stays blocked until the remaining stage gate is cleared
    (Constitution XII exception and the shared `ProblemDetailAdvice` handler in T023 need explicit
    user approval).
- The 2026-08-24 precondition ("Ready for planning after Financial GPS status rules are
  finalized") is satisfied: `specs/financial-domain/status-rules.md` exists and 005 does not
  redefine the Financial Status vocabulary. The spec is **Implementation Ready**, and the plan,
  data model, REST contract, quickstart and tasks are produced.
