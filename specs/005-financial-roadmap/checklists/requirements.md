# Specification Quality Checklist: Financial Roadmap

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-08-24
**Revised**: 2026-10-22 — implementation-ready rewrite + analyze consistency pass
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
- [x] The unauthenticated code is `AUTH_REQUIRED` everywhere, matching `ProblemDetailAdvice` / `RestAuthenticationEntryPoint`
- [x] The `invalid asOf` contract is backed by a task: T023 adds the `MethodArgumentTypeMismatchException` handler and T013 asserts `400 VALIDATION_FAILED` with `violations`
- [x] The shared-handler impact on 001–004 is documented as a deliberate repo-wide error-contract alignment (plan R-08, research R-17), `violations` keeps the existing `string[]` shape, and T023 reruns `e2e/specs/gps.spec.ts`
- [x] The sequence gate wins over a goal's own completion for `status`: a `COMPLETED` goal behind an incomplete earlier stage is `UPCOMING` with `blockedBy` (spec §7.1/§7.4, RM-006, data-model invariants)
- [x] Every §12 case maps to a task; RM-005 covers the `DEBT_FREEDOM`-only variant and RM-014 covers ownership end to end
- [x] The Constitution XII split is recorded: 005 delivers route order + policy disclosure, 008 owns allocation (plan Constitution Check)
- [x] Quickstart commands match AGENTS.md: `--env-file ~/.secrets/financial-gps.env`, and `-Dtest='*Roadmap*Test'` matches every roadmap test class

## Notes

- Re-validated 2026-10-22 against the implementation-ready rewrite of [spec.md](../spec.md).
  Every item still holds; no marker changed state.
  - *Content Quality*: the spec stays written for stakeholders — the JSON in §8 is the **wire
    contract**, the same level 004's spec documents, not an implementation detail.
  - *Requirement Completeness*: no `[NEEDS CLARIFICATION]` marker remains; the five defaults of
    §4 are recorded as decisions and can be overridden by an amendment.
  - *Feature Readiness*: every FR maps to the 14-case verification matrix of §12, and each matrix
    case maps to a task in `tasks.md`.
- The 2026-08-24 precondition ("Ready for planning after Financial GPS status rules are
  finalized") is satisfied: `specs/financial-domain/status-rules.md` exists and 005 does not
  redefine the Financial Status vocabulary. The spec is **Implementation Ready**, and the plan,
  data model, REST contract, quickstart and tasks are produced.
