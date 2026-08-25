# Quickstart: Financial Domain Engine (validation guide)

> How to prove the engine works end-to-end using the normative reference cases as the oracle.
> This is a run guide, not implementation detail; wiring to JUnit is an implementation concern.

## Prerequisites

- JDK 21 and a Java build tool (Maven per the `004` plan).
- No database, server, or network needed — the engine is pure in-process.
- `specs/financial-domain/reference-cases.md` as the acceptance oracle.

## Setup

```text
backend/            # from the repo plan; domain lives under com.financialgps.domain
```

Build with the project's standard command (e.g., `mvn -pl backend test` or `./mvnw test`).

## Validation scenarios (driven by reference cases)

1. **Cash flow & capacity** — build a `FinancialInput` with income 74, expense 30, mandatory
   debt 20; run `CashFlowCalculator`; assert Net Cash Flow 24 and Available Capacity 24
   (`CF-001`). Repeat for `CF-002` (0/0) and `CF-003` (NCF −10, capacity 0).
2. **Debt amortization** — assert `DC-001` (1000 @12% pay 50 → new balance 940), `DC-002`
   (0% → 900), `DC-003` (payment > balance → 0, no negative), `DC-004` (payment < interest →
   ETA `UNAVAILABLE`, status `BLOCKED`).
3. **Goals & ETA** — `G-001`…`G-005`, including the `current > target` boundary (remaining 0,
   `COMPLETED`).
4. **Required capacity** — `RC-001/002` with `CEILING`.
5. **Determinism** — same inputs + same `asOfDate` → identical result (`DM-001`);
   `asOfDate+1day` may shift ETA and must explain it (`DM-002/003`).
6. **Status** — run `StatusEvaluator` over `status-001…006`, incl. tolerance policy
   (`AT_RISK` vs `OFF_TRACK`).
7. **Timeline** — apply `TM-001/002/003` and assert the change applies only from its effective
   period.
8. **Allocation & dependency** — `AL-001…006`, incl. completion reallocation (`AL-002`), blocked
   dependency (`AL-003`), cycle/self-loop rejection (`AL-004`, `AL-006`).
9. **Scenario isolation** — `SC-001/002`: running a timeline/allocation projection never mutates
   the actual input state.

## Expected outcomes

- Every reference case maps to at least one passing automated domain test.
- A failing case is a constitution violation until corrected (spec or test amendment is a
  documented change).
- `calculate` never touches DB/HTTP/clock/AI; a trace builder can verify no such dependency is
  installed in the engine module's classpath.