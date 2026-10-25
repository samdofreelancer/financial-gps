# ADR 0005: Pure, identity-free financial domain engine

- Status: Accepted
- Date: 2026-09-20

## Context

Projections (cash flow, debt payoff, goal capacity, roadmap allocation) must be
deterministic, explainable, and testable without a database, clock, session, or user.
If projection logic lived in controllers or stores, every UI change could silently
alter money math, and the same inputs could yield different outputs across runs.
See `specs/financial-domain/contracts/engine-contract.md`.

## Decision

All financial computation lives in a pure in-process engine,
`FinancialResult calculate(FinancialInput, Assumptions, asOfDate, FinancialPolicy)`:

1. **Identity-free:** no user, session, or owner id inside the engine. Ownership is
   resolved at the application boundary; the engine sees numbers only.
2. **No I/O:** no DB, HTTP, clock, randomness, or AI calls. The evaluation date is an
   explicit parameter (`BusinessDate` port at the edge).
3. **Locked orchestration order:** validate → timeline → cashflow → debt → dependency →
   allocation → goal/ETA → status → assemble. Calculators (`CashFlow/Debt/Goal/
   Allocation/Timeline/Projection/Dependency/Status`) are internal.
4. **Fixed numeric rules:** decimal scale-2 `HALF_UP` (counts use `CEILING`), rates as
   scale-6 fractions; `NetCashFlow = Income − Expense − MandatoryPayment`;
   `AvailableCapacity = max(NCF, 0)`; amortization `monthlyInterest =
   round(balance × rate/12)`, payment-below-interest → `BLOCKED/UNAVAILABLE`;
   `MONTHLY` is the only timeline; status has 5 levels with `latenessTolerance = 3`.
5. **Provenance travels with results:** every total carries `calculated` vs `actual`
   labels plus derivation details, which the UI renders verbatim (never recomputes).

## Rationale

- Same inputs always produce the same outputs — E2E can assert server-rendered text
  without reimplementing math.
- The frontend contract is simple and stable: display-only; all totals are server truths.
- Scenario planning (`006`) reuses the engine with different assumptions at zero
  additional trusted-code risk.

## Consequences

- Positive: engine unit tests are fast, exhaustive, and framework-free.
- Negative: any rule change (even a rounding mode) is a domain event — it must update
  `specs/financial-domain/*`, engine tests, and any E2E text asserting totals.
- Follow-up constraint: `DomainBoundaryGuardTest` keeps Spring/JDBC/auth types out of
  `domain/`; violations fail the build.

## Alternatives considered

- **Compute totals in the SPA.** Rejected: duplicates money logic in two languages,
  breaks determinism guarantees, and exposes rules to client tampering.
- **Database stored procedures.** Rejected: hides rules from code review and unit tests,
  couples math to Postgres.
- **Rules engine library (e.g. Drools).** Rejected: external DSL for rules that fit
  comfortably in typed pure functions with reference cases (`reference-cases.md`).

## Related artifacts

- `specs/financial-domain/` (engine contract, calculation/status rules, data model)
- `docs/architecture/business-domain.md`
- `docs/architecture/api-contract.md` (provenance in responses)
