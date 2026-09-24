# 001 Financial Profile — Implementation Plan

> Stage: Superpowers / HOW. Plan only — no code.
> Authority: spec.md + architecture.md + constitution v1.2.0 + financial-domain
> oracle (calculation-rules, reference-cases CF-001..003 / DM-001..003,
> data-model, engine-contract) + 004 rest-api + 007 boundary + evidenced code.

## 1. Scope

In: one owner-scoped profile (savings, emergency fund, dependents, currency),
income/expense line items, deterministic current position (Income, Expense,
Net Cash Flow, Available Capacity + provenance), owner isolation, money
precision, Vue display. Out: Debt/payoff (002), Goals (003), GPS status/ETA
(004), Roadmap (005), Scenarios (006), auth changes (007 reuse only),
multi-profile, currency conversion, transaction feeds, scores, guarantees.

## 2. Architecture summary

HTTP (Vue axios + SESSION/XSRF cookies) -> api.profile (DTO bind/validate,
OwnerId via CurrentOwnerProvider, no math) -> application.profile
(@Transactional, owner-filtered load, FinancialInput mapping, asOfDate,
canonical engine call) -> domain (Money/Income/Expense/FinancialInput,
CashFlowCalculator via FinancialEngine path, FinancialResult/position,
provenance) -> infra (JPA entities/repos, Flyway, OwnershipQueries).
Derived totals recomputed, never persisted.

## 3. Decisions resolved from existing evidence

- Position code home: reuse canonical CashFlowCalculator/FinancialEngine
path; NO domain/profile/CurrentPositionCalculator (no-duplicate-rules,
engine-contract, locked orchestration; 001 T006/T007 path is stale).
- Math owner: domain only; Spring/Vue never compute (constitution).
- Error shape: reuse api.common.ProblemDetailAdvice codes
VALIDATION_FAILED/ AUTH_REQUIRED/RESOURCE_NOT_FOUND; no api/error package.
- Vue re-homing: requirements from React paths (features/profile,
components/money, TanStack invalidation) map to frontend/src/views/,
components/, stores/, api/ with refetch-after-mutation.
- Single profile: one active profile per owner (001 assumptions + 004
data-model); enforce UNIQUE(owner_id).
- Range of Mandatory for 001: constant 0 (debt belongs to 002).

## 4. Blocking decisions

- BLOCKING DECISION M1 migration filename/number: evidenced V1__auth.sql vs
stale V001__profile / V001__financial_inputs / V002__debts / V003__goals.
Human must confirm next Flyway version (recommended V2__profile.sql single
file for profile+income+expense; reserves later numbers for 002/003) before
Slice 1. File-disjoint alternatives cannot merge silently.
- IMPLEMENTATION DETAIL: single-profile depth (UNIQUE + app upsert), hard
delete for income/expense rows while retaining inactive rows as history via
active flag (not archive table), PUT /profile create-or-replace, Vue file
placement below, profileId linkage = derive from owner profile not body.

## 5. Implementation slices

### Slice 1 - Persistence (after M1)
P1 backend/src/main/resources/db/migration/V2__profile.sql (pending M1
confirm): profile/income/expense tables; owner_id UUID NOT NULL REFERENCES
account(id) ON DELETE CASCADE + idx; profile UNIQUE(owner_id);
income/expense profile_id FK CASCADE; amount numeric(19,2) NOT NULL
CHECK>=0; savings/emergency numeric(19,2) CHECK>=0; dependents INT CHECK>=0;
expense_type CHECK FIXED/VARIABLE; currency CHAR(3); created/updated_at
TIMESTAMPTZ. Purpose: owner-scoped truth. Test-first:
AccountSchema-style migration test + CHECK/FK/cascade assertions on
Testcontainers PG. Depends: V1. Behavior: migrate clean; orphan scan covers
new tables via OwnershipQueries.
P2 entities+repos infrastructure/persistence/profile/:
ProfileEntity/IncomeEntity/ExpenseEntity (BigDecimal mapped, no math) and
ProfileRepository/IncomeRepository/ExpenseRepository with
findByOwnerId/findByIdAndOwnerId/deleteByIdAndOwnerId (fixture pattern).
Test-first: repo isolation + precision (19.99+0.01=20.00) tests.

### Slice 2 - Domain (pure, JDK only)
D1 domain/model/: Money(BigDecimal scale2 + Currency VND + explicit
RoundingMode; non-negative guard), Income/Expense (Money+source/category+
expenseType+effectiveFrom+active), FinancialInput (owner-free lists; debts/
goals empty for 001), Assumptions/FinancialPolicy.rounding passthrough.
D2 reuse CashFlowCalculator.calculate(input,policy)->(NetCashFlow,
AvailableCapacity) via FinancialEngine path (validate->timeline->cashflow
subset; full orchestration with empty debt/goal/allocation lists). No new
calculator class. D3 FinancialResult/position view: Income, Expense, NetCash
Flow (negative reported), AvailableCapacity=max(NCF,0), savings/emergency/
dependents echo + provenance actual/calculated. Test-first: CF-001..003,
DM-001..003, plus domain matrix below; PurityTest/guard stays green.

### Slice 3 - Application
A1 application/profile/: ProfileService (GET: load owner profile+active
lines->FinancialInput->engine->position DTO; PUT: upsert scalars by
owner_id), IncomeService/ExpenseService (create/update/delete by
id+owner_id->404 else; map->engine->return saved view). All take OwnerId
explicit; @Transactional; asOfDate= explicit param or LocalDate.now seeded
at boundary (clock never in domain); no formulas here. Test-first: service
unit (mocked repos, engine real/fake) for mapping+not-found+no-math.
A2 export participants: profile+incomes+expenses into deterministic bundle
(sorted by id, [] when empty) for 007 DataExportService.

### Slice 4 - API
C1 api/profile/: ProfileController GET|PUT /api/v1/profile;
IncomeController POST|PUT|DELETE /api/v1/incomes[/{id}]; ExpenseController
same for /api/v1/expenses. DTOs: decimal-string money + currency ISO,
source/category, FIXED/VARIABLE, dependents INT; NO ownerId/accountId/
profileId-from-client (profile derived from OwnerId). Bean Validation
(@NotBlank/@Pattern decimal/@PositiveOrZero) -> 400 VALIDATION_FAILED via
existing advice; domain negative -> mapped 400; missing/cross-owner -> 404
RESOURCE_NOT_FOUND identical; unauth -> 401 via SecurityConfig (no change).
Return server-saved representation with facts+position+provenance. Test-first:
@WebMvcTest contract + Testcontainers MockMvc matrix (auth/validation/404/
cross-owner/no ownerId leak).

### Slice 6 - Integration
I1 Testcontainers PG: migration order V1->V2, precision, UNIQUE(owner_id),
FK cascade account->profile->lines, owner_id index/registry discovery.
I2 Ownership matrix (fixture pattern): A cannot read/update/delete B (404
identical to missing); body with ownerId/accountId ignored; unauth 401 for
all 8 routes; export determinism; delete-account zero orphans incl new
tables. I3 Boundary guard: domain diff adds no Spring/Jakarta/SQL/Clock/
Random/platform/app/infra/api refs; engine signature unchanged (no identity).

### Slice 7 - Verification
V1 spec acceptance: complete profile <5min path; SC-002 sums equal inputs;
SC-003 cash-flow findable; US2 provenance visible; edge cases (zero income/
expense allowed; negatives rejected; expenses>income shows negative NCF +
zero capacity; multi-source sums). V2 no-regress: 007 sweeps/matrix/cascade
green; determinism recalculation identical. V3 e2e: register->login->csrf->
PUT profile->add incomes/expenses->verify position->edit->verify update
without debt/goal change->logout isolation.

## 6. TDD strategy

Order: domain (CF/DM + matrix) -> application (mapping/scoping) ->
persistence (migration/constraints/precision/cascade) -> API (contract/
matrix) -> frontend (Vitest) -> e2e (spec journey). Every step RED test
with exact assertion -> minimal code -> refactor; no production code
without failing test; no client math; no duplicate calculator.

## 7. Test matrix

Domain (JUnit table-driven, BigDecimal string compare): empty/zeros;
income-only; expense-only; NCF positive/zero/negative (CF-002/CF-003);
capacity clamp max(NCF,0) while NCF stays negative; inactive income/expense
excluded; multi-source/category sums incl 19.99+0.01; determinism DM-001
(same asOf identical), DM-002/003 (asOf shift explained, no stale cache);
canonical path (via CashFlowCalculator/FinancialEngine, no second impl);
rounding HALF_UP display vs CEILING counts; Money rejects negative/non-
decimal; provenance labels on every total.
Ownership: A/B read/update/delete negatives; forged ownerId ignored;
missing==cross-owner 404 body; unauth 401 sweep; session expiry 401.
Persistence: numeric(19,2) round-trip; CHECK rejects negative; UNIQUE
second profile fails; FK missing profile fails; cascade account delete
removes profile+lines; owner_id indexed/registered.
API: 200/201/204 happy; 400 VALIDATION_FAILED (negative, bad decimal,
blank source, bad expenseType); 401 unauth; 404 missing/cross-owner;
DTO has no owner internals; mutation returns server representation.
Frontend: field validation; decimal-safe (0.1+0.2 strings); server totals
render; refetch after each mutation; actual/calculated distinct; no
optimistic totals.
E2E: spec US1+US2 journey above with isolated account; timings for SC-001.

## 8. Dependency order

M1 confirm -> P1 migration test -> P2 entities/repos -> D1/D2/D3 domain ->
A1/A2 services+export -> C1 controllers -> F1/F2/F3 Vue -> I1/I2/I3 ->
V1/V2/V3. Financial-domain engine sub-tasks (Money->models->CashFlow->
orchestration->runner) are the domain prerequisite if engine absent; 001
adds no engine fork. 002/003/004 consume position DTOs later.

## 9. Risk controls

No-math-outside-domain (review diff for BigDecimal outside domain; Vue no
arithmetic); no ownerId in DTO/query; every repo method owner-filtered;
money strings end-to-end (reject float/number); rounding at declared
boundaries only; no persisted derived totals (position recomputed);
provenance on every total; scope freeze (no debt/goal/GPS/roadmap/scenario
code); migration gate (M1 before Slice 1); 007 green gate before merge.

## 10. Definition of Done

Spec US1/US2+FR-001..005+edges pass; oracle CF/DM green via canonical path;
precision preserved PG<->DTO<->display; ownership matrix zero cross-success
+ missing==cross-owner 404; unauth 401; validation 400; no owner leak; no
client truth; purity/boundary guards green; migration+export+cascade green;
Vue provenance visible; e2e journey green; M1 recorded; no future-feature
code; human review approved.

### Slice 5 - Frontend (Vue reality) [moved here by append order; executes
as Slice 5 before Slice 6 above]
F1 frontend/src/api/profile.ts (axios, same-origin /api proxy, cookies+XSRF):
getProfile/putProfile, CRUD incomes/expenses; decimal strings untouched.
F2 frontend/src/stores/profileStore.ts (Pinia): server state only (no
computed totals); actions call api then refetch profile; no money math.
F3 frontend/src/views/ProfileView.vue + components/MoneyInput.vue (string
model, presence/non-negative/format checks; server authoritative),
MoneyDisplay.vue (Intl.NumberFormat display), PositionSummary.vue (Income,
Expense, Net Cash Flow, Available Capacity, savings, emergency, dependents
with actual vs calculated labels per FR-003/US2). Route /profile in
router/index.ts. Test-first: Vitest (decimal-safe, server-totals render,
provenance labels, refetch on mutation); no client-truth assertion.
