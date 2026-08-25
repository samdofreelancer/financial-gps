# Tasks: Financial Domain Engine (pure Java domain core)

**Input**: ONLY `specs/financial-domain/calculation-rules.md`, `status-rules.md`,
`reference-cases.md`, `plan.md`. No other design document was consulted; every field, formula,
and threshold below quotes one of those four documents.

**Prerequisites**: `plan.md` (tech stack, project structure, TDD order, traceability),
`reference-cases.md` (normative oracle — 34 rows), `calculation-rules.md` (§0–§12),
`status-rules.md` (five statuses + tolerance policy).

**Tests**: MANDATORY for every increment — TDD red → green → refactor per the plan's
"TDD Order" section. Every task is one focused increment with its test written first.

**Organization**: By `plan.md` TDD order, not by user story. This feature has no `spec.md`
(it is a shared engine consumed by features `004/005/006/008/009`), so `[US#]` labels are
intentionally not used; the per-task Traceability field replaces them.

## Purity constraints (apply to EVERY task)

- Engine module deps: **JDK 21 + JUnit 5 only**. No Spring, no database/JDBC/JPA, no HTTP,
  no clock reads (`java.time.Clock`, `System.currentTimeMillis/nanoTime`), no
  `java.util.Random`, no AI libraries — anywhere under
  `backend/src/main/java/com/financialgps/domain/`.
- All arithmetic via `BigDecimal`; explicit `RoundingMode` at declared boundaries; never
  `float`/`double` (rules §1).
- `frequency` locked to `MONTHLY` (rules §10).
- Canonical terminology §0 verbatim — no near-synonyms (`ModelCompilerTest` guards this).

## Task format

Checkbox line = Task ID + primary path. Bullets: **Trace** (plan item + rule § + case IDs) ·
**Depends on** · **Test-first (RED)** · **Expected outcome (GREEN → refactor)**.

One deliberate addition beyond the plan's annotated test tree is flagged in "Deviations" at the
bottom of this file.

---

## Phase 1: Setup & domain values

- [ ] T001 Create Maven module skeleton in `backend/pom.xml` with package root `backend/src/main/java/com/financialgps/domain/` and subpackages `money/ model/ finance/ timeline/ projection/ policy/ status/ dependency/ engine/`; wire JUnit 5 under `backend/src/test/java/`
  - **Trace**: plan T001; plan Project Structure (source tree); plan Constraints (purity).
  - **Depends on**: none.
  - **Test-first (RED)**: n/a (scaffolding). Acceptance instead: `mvn -pl backend test` runs green on an empty suite and the module POM declares nothing beyond JDK 21 + JUnit 5.
  - **Expected outcome**: compilable pure-Java module; no web/persistence/AI artifact on the engine classpath.

- [ ] T002 Implement money value objects `Money`, `Currency`, `Rate`, `Ratio`, `RoundingPolicy` in `backend/src/main/java/com/financialgps/domain/money/`; tests first in `backend/src/test/java/com/financialgps/domain/MoneyTest.java` and `ValueObjectTest.java`
  - **Trace**: plan T002; rules §1 (decimal only, central scale 2, HALF_UP display / CEILING counts, rate fraction scale 6, rounding at declared boundaries); plan traceability rows §1-money and §1-Rate/Ratio → `T002`.
  - **Depends on**: T001.
  - **Test-first (RED)**: `MoneyTest` — scale-2 amounts, explicit `RoundingMode` on every operation, non-negative balances, `float`/`double` rejected. `ValueObjectTest` — `Rate` within 0…1 at scale 6, `Ratio` within 0…1 at scale 4, boundaries 0 and 1 valid, out-of-range rejected. Run → both fail (classes absent).
  - **Expected outcome**: both suites green; no primitive monetary math outside these types; shared rounding logic consolidated in `RoundingPolicy`.

- [ ] T003 Implement immutable `FinancialPolicy` with `DebtPolicy`, `AllocationPolicy`, `StatusPolicy` in `backend/src/main/java/com/financialgps/domain/policy/`; test first in `backend/src/test/java/com/financialgps/domain/FinancialPolicyTest.java`
  - **Trace**: plan T003; rules §1 rounding defaults; `status-rules.md` Policy parameters (`latenessTolerance` default 3 contribution periods; `shortfallRatio` reserved, unused; values are configuration, not financial facts); plan Constitution Check row V.
  - **Depends on**: T002.
  - **Test-first (RED)**: assert immutability (no setters), documented defaults (`latenessTolerance = 3`; display `HALF_UP`, counting `CEILING`), `shortfallRatio` present but unused, null/negative configuration rejected. Run → fail.
  - **Expected outcome**: green; reusable policy object for all calculators; no threshold magic numbers outside `StatusPolicy`.

---

## Phase 2: Core models (structural, guarded by `ModelCompilerTest`)

Each T004 sub-task appends one `@Nested` group to `ModelCompilerTest.java` (the plan's single
T004 guard); groups stay small so the file is never a merge bottleneck.

- [ ] T004a Add `FinancialInput`, `Income`, `Expense` in `backend/src/main/java/com/financialgps/domain/model/`; guard via `@Nested` group `T004a` in `backend/src/test/java/com/financialgps/domain/ModelCompilerTest.java`
  - **Trace**: plan T004a; rules §0 canonical terms **Income**, **Expense**; §3 ("sum of active recurring incomes/expenses effective on asOfDate"); plan structure tree.
  - **Depends on**: T002 (models hold `Money`).
  - **Test-first (RED)**: assert the three canonical type names exist verbatim; `Income`/`Expense` carry an active flag and `effectiveFrom`; `FinancialInput` aggregates `List<Income>` + `List<Expense>`. Run → fail (types absent).
  - **Expected outcome**: group green; §0 names used exactly (never revenue/spending/outflow/inflow).

- [ ] T004b Add `Debt`, `Goal` in `backend/src/main/java/com/financialgps/domain/model/`; guard via `@Nested` group `T004b` in `ModelCompilerTest.java`
  - **Trace**: plan T004b with its note "Money/Rate/Ratio live ONLY in T002" (reused, never redefined); rules §4 (user supplies balance, rate, payment) and §5 (`max(targetAmount − currentAmount, 0)`, `completionCondition`).
  - **Depends on**: T002, T004a.
  - **Test-first (RED)**: `Debt` exposes `outstandingBalance`, `annualInterestRate`, `monthlyPayment`; `Goal` exposes `targetAmount`, `currentAmount`, optional target date, `completionCondition`, `priority`, `status`; `remaining` derivable per §5. Run → fail.
  - **Expected outcome**: group green; zero duplicated value types inside `model/`.

- [ ] T004c Add `TimelineChange`, `CashAllocationRule`, `GoalDependency` in `backend/src/main/java/com/financialgps/domain/model/`; guard via `@Nested` group `T004c` in `ModelCompilerTest.java`
  - **Trace**: plan T004c; rules §10 (`kind`, `effectiveFrom`, `amount/rate`, `frequency` — only `MONTHLY`), §7 (rule tuple `(sourceCash, destination, amount, priority, effectiveFrom)`), §9 (tuple `(successorGoal, prerequisiteGoals, type)`).
  - **Depends on**: T002, T004b.
  - **Test-first (RED)**: `TimelineChange.frequency` accepts only `MONTHLY` and rejects any other value at construction; allocation-rule tuple fields match §7; dependency tuple matches §9. Run → fail.
  - **Expected outcome**: group green; MONTHLY lock enforced by the type itself, not convention.

- [ ] T004d Add `Assumptions` + `FinancialAssumption` in `backend/src/main/java/com/financialgps/domain/model/`; guard via `@Nested` group `T004d` in `ModelCompilerTest.java`
  - **Trace**: plan T004d; plan traceability row "assumption provenance → `T004d`"; plan Constitution Check row V (labels `USER_SUPPLIED` / `SYSTEM_DEFAULT`); rules §10 (a change is "stored as a user assumption or actual input") and §11 (provenance actual / user-assumed / calculated).
  - **Depends on**: T004c.
  - **Test-first (RED)**: `FinancialAssumption.source` restricted to {`USER_SUPPLIED`, `SYSTEM_DEFAULT`} plus `userAssumed` flag; `Assumptions` is an immutable labelled set; assumptions are carried as data, never computed. Run → fail.
  - **Expected outcome**: group green; provenance representable for every later explanation; type reusable unchanged by Scenario Planning.

---

## Phase 3: Calculators & evaluation (each = one red table → one class)

- [ ] T005 Implement `CashFlowCalculator.calculate(input, policy)` in `backend/src/main/java/com/financialgps/domain/finance/CashFlowCalculator.java`; test first in `CashFlowCalculatorTest.java`
  - **Trace**: plan T005; rules §3 (Net Cash Flow = Income − Expense − MandatoryPayment; Available Capacity = max(NCF, 0)); oracle §A cases `CF-001..003`; plan traceability rows §3 ×2.
  - **Depends on**: T002, T003, T004a.
  - **Test-first (RED)**: encode the three oracle rows verbatim — `CF-001` 74/30/20 → NCF 24, capacity 24; `CF-002` 30/30/0 → 0/0; `CF-003` 20/30/0 → NCF −10 reported (never concealed), capacity clamps to 0. Run → fail.
  - **Expected outcome**: green on all three rows; negative NCF is a first-class value; formula refactored into one private pure function.

- [ ] T006 Implement `DebtCalculator` monthly simple amortization in `backend/src/main/java/com/financialgps/domain/finance/DebtCalculator.java`; test first in `DebtCalculatorTest.java`
  - **Trace**: plan T006; rules §4 (`monthlyInterest = round(balance × annualRate / 12, scale 2, HALF_UP)`; final-period clamp; payment > balance clears with no over-credit; payment < interest ⇒ no finite payoff, `BLOCKED`, reason `PAYMENT_DOES_NOT_COVER_INTEREST`); oracle §B `DC-001..004`; plan traceability rows §4 ×2 (extras/rate-change legs flow through T008).
  - **Depends on**: T002, T003, T004b (sequenced after T005 per plan order).
  - **Test-first (RED)**: `DC-001` 1000 @0.12 pay 50 month 1 → new balance 940 (principal 40); `DC-002` rate 0 pay 100 → 900 linear; `DC-003` balance 50 @0.12 pay 100 → 0, no negative, no over-credit; `DC-004` 1000 @0.12 pay 8 (< interest 10) → balance grows, ETA `UNAVAILABLE`, status `BLOCKED`. Run → fail.
  - **Expected outcome**: green on all four rows; amortization matches the §4 formula digit-for-digit at scale 2.

- [ ] T007 Implement `GoalCalculator` in `backend/src/main/java/com/financialgps/domain/finance/GoalCalculator.java`; test first in `GoalCalculatorTest.java`
  - **Trace**: plan T007; rules §5 (remaining = max(target − current, 0); progress floored for display; required monthly capacity = remaining ÷ months, `CEILING`) and §6 (contribution min(c, remaining); ETA = earliest month accumulated ≥ remaining); oracle §C `G-001..005`, §D `RC-001..002`; plan traceability rows §5, §6.
  - **Depends on**: T002, T003, T004b (sequenced after T006 per plan order).
  - **Test-first (RED)**: `G-001` 108/0 cap 24 → remaining 108, ETA 5 (ceil); `G-002` 108/108 → remaining 0, ETA 0, `COMPLETED`; `G-003` 108/24 → 84, ETA 4; `G-004` 100/0 cap 0 → remaining 100, no finite ETA → `BLOCKED`; `G-005` 100/120 → remaining 0 (never negative), progress 100%, `COMPLETED`; `RC-001` 120 over 10 months → required 12 (CEILING, not 11.99); `RC-002` 121 over 10 → 13. Run → fail.
  - **Expected outcome**: green on all seven rows; CEILING used exactly where a count must not understate completion.

- [ ] T008 Implement `TimelineEngine.slice(inputs, asOfDate, changes)` in `backend/src/main/java/com/financialgps/domain/timeline/TimelineEngine.java`; test first in `TimelineEngineTest.java`
  - **Trace**: plan T008; rules §10 (periods delimited by effective dates; prior value applies before `effectiveFrom`; frequency locked `MONTHLY`; nothing silently extrapolated); oracle §G `TM-001..003`; plan traceability row §4-supported-actions (extras/rate change arrive as timeline changes).
  - **Depends on**: T004c, T005 (`TM-002` asserts a Net Cash Flow delta).
  - **Test-first (RED)**: `TM-001` salary ×1.1 effective 2027-04 → before 2027-04 old salary applies, from 2027-04 ×1.1; `TM-002` rent +3 from 2027-02 → NCF drops by exactly 3 only for periods from 2027-02; `TM-003` extra debt payment 5/mo from 2027-01 → debt ETA shortens AND the extra is labelled a user assumption in the explanation. Run → fail.
  - **Expected outcome**: green; period boundaries derive only from effective dates + `asOfDate`; non-monthly frequencies impossible to express.

- [ ] T009 Implement `DependencyResolver.validate(deps)` in `backend/src/main/java/com/financialgps/domain/dependency/DependencyResolver.java`; test first in `DependencyResolverTest.java`
  - **Trace**: plan T009 (**deliberately before T010** — allocation consumes dependency resolution); rules §9 (DAG; reject cycle incl. self-loop); oracle §H `AL-003`, `AL-004`, `AL-006`.
  - **Depends on**: T004c. (File-disjoint from T008 — the only safe parallel window; see Dependencies section.)
  - **Test-first (RED)**: `AL-003` A requires B, B incomplete → resolver reports A gated (no contribution may start, not a silent skip); `AL-004` cycle A→B→A → validation error / `BLOCKED`, traversal provably terminates; `AL-006` self-loop A→A → validation error / `BLOCKED`. Run → fail.
  - **Expected outcome**: green; cycle/self-loop detection total; per-goal gating state queryable for any period.

- [ ] T010 Implement `AllocationCalculator.route(availableCapacity, rules, dependencies)` in `backend/src/main/java/com/financialgps/domain/finance/AllocationCalculator.java`; test first in `AllocationCalculatorTest.java`
  - **Trace**: plan T010; rules §7 (priority order; completed goal stops, freed capacity flows to next rule) and §8 (default order "interest-bearing debt → emergency fund → prioritized goals → remaining goals"; engine MUST state which order policy was applied and why); oracle §H `AL-001..006` (+ `CF-003` clamp interplay); plan traceability rows §7 ×2, §8.
  - **Depends on**: T005 (capacity), T007 (`remaining` caps), T009 (gating).
  - **Test-first (RED)**: `AL-001` capacity 24 → debt 15 + goal 9 exactly; `AL-002` debt completes → freed 15 reroutes to next priority goal deterministically; `AL-003` gated goal receives nothing while prerequisite incomplete; `AL-004`/`AL-006` surface as `BLOCKED` validation instead of routing; `AL-005` default vs user order differ → result states which order policy applied and why; total routed per period ≤ Available Capacity. Run → fail.
  - **Expected outcome**: green on all six AL behaviours; completion reallocation deterministic; route-order provenance present whenever a non-default order is used.

---

## Phase 4: Projection, status, orchestration

- [ ] T011 Implement `ProjectionEngine` (drives periods through the calculators) with `FinancialResult` + provenance/explanations and read-only `ProjectionFinancialState` in `backend/src/main/java/com/financialgps/domain/projection/`; test first in `ProjectionEngineTest.java`
  - **Trace**: plan T011; rules §2 (`(inputs, assumptions, asOfDate, policy)` is a pure function; changing only `asOfDate` MAY shift ETA/status and MUST be explained), §11 (projection state isolated — never mutates actual inputs; every value labelled actual / user-assumed / calculated); oracle §E `DM-001..003`, "Is Simulated vs Actual" `SC-001..002`; plan traceability rows §2, §11.
  - **Depends on**: T005–T010, T004d (assumption labelling).
  - **Test-first (RED)**: `DM-001` same inputs + same `asOfDate`, recalc → identical result (values, ETA, status); `DM-002` `asOfDate` +1 day → ETA may shift AND result explains the shift; `DM-003` different `asOfDate`, same contribution → progress/status reflects the difference (no stale cache); `SC-001` scenario income ×1.1 → actual income object unchanged (assert deep equality after run); `SC-002` scenario extra debt payment → actual balance unchanged, result labelled projection. Run → fail.
  - **Expected outcome**: green on all five rows; actual input objects bit-identical after any projection; every output field carries its provenance label.

- [ ] T012 Implement `StatusEvaluator.evaluate(result, statusPolicy)` in `backend/src/main/java/com/financialgps/domain/status/StatusEvaluator.java`; test first in `StatusEvaluatorTest.java`
  - **Trace**: plan T012; `status-rules.md` (entry conditions evaluated in order `COMPLETED → BLOCKED → ON_TRACK → AT_RISK → OFF_TRACK`; `latenessTolerance`; a status MUST ship with its rule-evaluation expression — capacity comparison, shortfall, which threshold was crossed — and expose policy values in explanations); oracle §F `status-001..006`.
  - **Depends on**: T011 (evaluates a projection/result), T003 (`StatusPolicy`).
  - **Test-first (RED)**: `status-001` adequate capacity, dated goal → `ON_TRACK` (`projectedCapacity ≥ requiredCapacity`); `status-002` positive capacity, finite ETA past target within tolerance → `AT_RISK`; `status-003` ETA slips beyond tolerance → `OFF_TRACK`; `status-004` non-positive NCF or payment < interest → `BLOCKED`; `status-005` completion condition met → `COMPLETED`; `status-006` two identical inputs differing only in target-date horizon → both rated by the same watched tolerance, never an absolute month count. Run → fail.
  - **Expected outcome**: green on all six rows; each returned status carries its rule-evaluation expression and the tolerance value that decided it.

- [ ] T013 Implement `FinancialEngine.calculate(input, assumptions, asOfDate, policy)` in `backend/src/main/java/com/financialgps/domain/engine/FinancialEngine.java`; test first in `FinancialEngineTest.java`
  - **Trace**: plan T013 + plan "FinancialEngine orchestration (locked)" (validate → timeline → cash flow → debt → dependency → allocation → goal/ETA → status → assemble; typed domain exception on validation violation; no step reads clock/DB/HTTP or AI); rules §2; plan Constitution rows II, III, V, VII.
  - **Depends on**: T011, T012 (assembles everything).
  - **Test-first (RED)**: end-to-end `DM-001` through the public entry point — full valid input → identical `FinancialResult` on recalculation, carrying position, destination, distance, progressPercent, eta (or `UNAVAILABLE` + reason), status, blockers, nextActions, explanation set, and per-field provenance (actual/assumed/calculated); invalid input (e.g., negative amount, cyclic dependency at the boundary) → typed domain exception, never silent proceed. Run → fail.
  - **Expected outcome**: green; orchestration order matches the locked sequence exactly; the engine class remains pure assembly of the already-tested steps.

---

## Phase 5: Oracle gate & architecture gate

- [ ] T014 Implement `ReferenceCaseRunner` (table-driven whole-suite oracle run) in `backend/src/main/java/com/financialgps/domain/projection/ReferenceCaseRunner.java`; test first in `ReferenceCaseRunnerTest.java`
  - **Trace**: plan T014 ("table-driven whole suite"); oracle `reference-cases.md` complete — 34 rows: CF×3, DC×4, G×5, RC×2, DM×3, status×6, TM×3, AL×6, SC×2; plan Performance Goal (reference table < 5s).
  - **Depends on**: T013 (rows execute through the public entry point / assembled engine).
  - **Test-first (RED)**: build the full 34-row table; the test fails while ANY row is absent from the table or mismatched against its documented expected output, and reports which case IDs are uncovered. Run → fail (table incomplete or values diverge).
  - **Expected outcome**: every one of the 34 normative rows green through the runner; a failing row is treated as a constitution violation until docs/tests are formally amended; suite completes well under 5s.

- [ ] T015 Implement `PurityTest` (architectural dependency isolation) in `backend/src/test/java/com/financialgps/domain/PurityTest.java`
  - **Trace**: plan T015; plan traceability row "No DB/HTTP/clock/random/AI dependency" and §12 AI-boundary row → `PurityTest` (`T015`); plan Test responsibility split (owns dependency isolation ONLY — never asserts canonical field names or business values).
  - **Depends on**: T001 mechanically; scheduled last as the final architecture gate over the finished module.
  - **Test-first (RED proof)**: first prove the guard can fail — temporarily add a probe class under `domain/` that uses `java.util.Random` and an import of `java.sql.*`, run `PurityTest` → must fail naming the violation; delete the probe. Then assert for real: engine-module classpath contains no `org.springframework.*`, `jakarta.*`, `java.sql.*`, `java.net.http.*` types, and main sources reference no `Clock`, `System.currentTimeMillis`, `System.nanoTime`, `Random`, and no AI library.
  - **Expected outcome**: purity gate green on the finished module; kept as a permanent CI check so later features cannot silently pollute the engine classpath.

---

## Dependencies & execution order

Critical path (plan order preserved verbatim):

```text
T001 → T002 → T003 → T004a → T004b → T004c → T004d
     → T005 → T006 → T007 → T008 → T009 → T010
     → T011 → T012 → T013 → T014 → T015
```

Cross-edges beyond the linear chain:

| Task | Additional inputs from |
|---|---|
| T005 | T002, T003, T004a |
| T006 | T002, T003, T004b |
| T007 | T002, T003, T004b |
| T008 | T004c, T005 |
| T010 | T005, T007, T009 |
| T011 | T005–T010, T004d |
| T012 | T011, T003 |
| T013 | T011, T012 |
| T014 | T013 |
| T015 | T001 (runs last as the gate) |

**Parallel window**: after T005 and T004c complete, `T008` (timeline/) and `T009` (dependency/)
touch disjoint files and can run concurrently — plan.md's only hard ordering constraint there is
T009 before T010. Everything else stays sequential: the four T004 sub-tasks all append to the
same `ModelCompilerTest.java`, and the remaining order is mandated by plan.md's TDD sequence.

**Checkpoints**: after T003 foundation compiles green · after T007 cash-flow+debt+goal slice
independently provable · after T010 routing provable in isolation · after T014 whole oracle
green (engine functionally done) · after T015 architecture gate green.

---

## Coverage validation (why this list is complete)

### Normative rule → tasks

| Rule | Covered by |
|---|---|
| §0 canonical terminology | T004a–T004d (`ModelCompilerTest` guard) |
| §1 money, precision, rounding | T002, T003 |
| §2 determinism + `asOfDate` | T011, T013, T014 |
| §3 Net Cash Flow & Available Capacity | T005 |
| §4 amortization + supported actions | T006 (+ extras/rate change as timeline changes in T008) |
| §5 goals | T007 |
| §6 contribution & ETA | T007 |
| §7 ordered allocation + cap | T010 |
| §8 route order policy | T010 |
| §9 dependency gating | T009, T010 |
| §10 timeline / MONTHLY | T004c (type lock), T008 |
| §11 actual vs projection isolation | T011 |
| §12 AI boundary | T015 (+ `SC-*` labelling in T011) |
| status-rules: five statuses, tolerance, explanations | T012 |
| Assumption provenance (`USER_SUPPLIED`/`SYSTEM_DEFAULT`) | T004d; surfaced via `TM-003` (T008), provenance set (T011), entry point (T013) |

### Reference case → test task (all 34 rows)

| Oracle section | Cases | Test task |
|---|---|---|
| A. Cash flow | CF-001..003 | T005 |
| B. Debt amortization | DC-001..004 | T006 |
| C. Goals & ETA | G-001..005 | T007 |
| D. Required capacity | RC-001..002 | T007 |
| E. Determinism & as-of | DM-001..003 | T011 (unit), T013 (end-to-end), T014 (suite) |
| F. Status | status-001..006 | T012 |
| G. Timeline | TM-001..003 | T008 |
| H. Allocation & dependency | AL-001..006 | T010 (all six behaviours; AL-003/004/006 also pinned at resolver level by T009) |
| Simulated vs actual | SC-001..002 | T011 |

## Deviations (explicit, none introduce business rules)

1. **`FinancialPolicyTest.java` added** — the plan's annotated test tree assigns no file to
   plan item T003, but the same plan mandates red-first for every increment. This is the single
   test file beyond the annotated tree; its assertions only restate documented defaults
   (rules §1, status-rules Policy parameters).
2. **No `[US#]` story labels** — this feature has no spec.md/user stories; per instruction,
   traceability runs to plan items instead.
3. **File location** — the setup script auto-detected a different feature directory; this
   tasks.md intentionally targets `specs/financial-domain/` per explicit instruction.

## Definition of done for this file

- [ ] All 18 checkboxes completable in one focused step each
- [ ] Every task has Trace · Depends on · Test-first (RED) · Expected outcome
- [ ] 34/34 oracle rows mapped · every calculation-rules § and status-rules item covered







