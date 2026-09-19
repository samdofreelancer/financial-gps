# 001 Financial Profile — Architecture (Archify / WHERE)

> Stage: Archify. No implementation, no plan, no task list.
> Authority: 001 spec/plan/tasks; constitution v1.2.0; financial-domain oracle;
> 004 plan/research/data-model/rest-api; 002/003/005/006 specs; 007 spec/plan/research/data-model/auth-api;
> evidenced code under backend/, frontend/, compose.yaml, README.md.

## 1. Current architecture (evidenced)

```text
Vue 3 SPA (Vite + Pinia + vue-router + axios)
  ↓ JSON + cookies (SESSION, XSRF-TOKEN/X-XSRF-TOKEN)
Spring Boot 3.2.2 Java 21 modular monolith (single Maven module)
  api.* → application.* → domain (pure, empty) ; platform ; infrastructure
  ↓ JPA/JDBC, ddl-auto:none
PostgreSQL 16 via Flyway (V1__auth.sql only); Testcontainers for tests
```

Backend lanes evidenced: api.{auth,account,common}, application.account (+OwnerId), platform.security (SecurityConfig, CurrentOwnerProvider), infrastructure.persistence.{account,ownership}, domain (only package-info.java purity contract). No profile/income/expense code exists. Frontend evidenced: Vue 3 + Pinia + router + axios (NOT React/TanStack from plans). DB evidenced: account + SPRING_SESSION tables only. No Money/BigDecimal usage yet.

## 2. Concept placement

| Concept | Domain | Persistence | API | Ownership | Downstream |
|---|---|---|---|---|---|
| FinancialProfile | Part of owner-free FinancialInput; scalars + derived CurrentPosition | profile(owner_id UNIQUE REFERENCES account CASCADE, currency, savings numeric(19,2), emergency numeric(19,2), dependents INT, timestamps) | GET/PUT /api/v1/profile; decimal strings + currency | session-OwnerId-owner_id; no client owner id; 404 cross-owner | root for 002-006 |
| Income | FinancialInput.incomes (Money+source+effectiveFrom+active) | income(owner_id FK CASCADE, profile_id FK CASCADE, amount numeric(19,2) CHECK>=0, source, active) | POST/PUT/DELETE /api/v1/incomes | findByIdAndOwnerId; dual FK | NCF and capacity |
| Expense | FinancialInput.expenses (+category, FIXED/VARIABLE) | expense(same keys, category, expense_type CHECK FIXED/VARIABLE) | POST/PUT/DELETE /api/v1/expenses | same as income | NCF negative reported |
| Savings | Scalar Money field, NOT entity | profile.savings_amount | profile body field | inherits profile row | 003 seed, 004 position |
| Emergency Fund | Scalar Money field, NOT entity | profile.emergency_fund_amount | profile body field | inherits profile row | 003 and 005 stage 2 |
| Dependents | Non-negative INT fact, NOT entity | profile.dependents_count CHECK>=0 | profile body field | inherits profile row | explanatory only |

Math: Income=sum active; Expense=sum active; Mandatory=0 until 002;
NetCashFlow=Income-Expense (may be negative, reported);
AvailableCapacity=max(NCF,0). Verbatim terms only.

## 3. Data flow

Read: Vue+axios+cookies - GET /api/v1/profile - SecurityConfig auth -
controller resolves OwnerId via CurrentOwnerProvider - app service loads
owner-filtered rows - maps to owner-free FinancialInput - pure CashFlow
derivation with (inputs, assumptions, asOfDate, policy) - DTO with facts +
CurrentPosition + actual/calculated provenance - Vue renders distinctly.

Update: controlled string money fields (no client math) - PUT /profile or
POST/PUT/DELETE incomes|expenses - CSRF + auth - Bean Validation to 400
VALIDATION_FAILED via ProblemDetailAdvice - transactional owner-filtered
persist (UNIQUE owner_id; CHECKs; FK CASCADE) - rebuild FinancialInput -
pure recalculation - return saved representation - refetch dependents.

Income/expense downstream: owner row numeric(19,2) - active sums - monthly
Income/Expense - NetCashFlow - AvailableCapacity=max(NCF,0) - read-only
recompute for 002 DTI/Mandatory, 003 capacity, 004 position/ETA/status,
005 stages, 006 in-memory overrides. Profile edits never mutate debt/goal rows.

## 4. Boundaries and constraints

Domain stays pure: no Spring/Jakarta/SQL/Clock/Random/platform/app/infra
(DomainBoundaryGuardTest + package-info). Engine user-agnostic:
calculate(inputs, assumptions, asOfDate, policy); FinancialInput owner-free.
App layer owns OwnerId mapping, transactions, asOfDate seeding, engine call.
Infra owns JPA/Flyway/sessions. Controllers own HTTP shape only, no math.

Ownership: SESSION cookie (Spring Session JDBC, 30m idle, rotation) -
SecurityConfig (all /api/v1 protected except 3 handshakes; CSRF double
submit) - CurrentOwnerProvider ONLY session reader - OwnerId explicit param -
owner-filtered repos (findByOwnerId/findByIdAndOwnerId) - owner_id FK CASCADE
+ index + OwnershipQueries registry + export bundle + zero-orphan delete.
Client owner ids never trusted. Cross-owner/missing is 404 RESOURCE_NOT_FOUND.

Constraints: determinism (same inputs+assumptions+asOf+policy = same result);
BigDecimal + explicit rounding (HALF_UP display, CEILING required capacity);
numeric(19,2)/numeric(9,6)/numeric(7,4); decimal strings on wire; VND single
currency; canonical terms verbatim; actual/assumed/calculated provenance;
negative NCF reported; projections recomputed not persisted; scenario
isolation; MONTHLY frequency; one profile per owner; 007 must not regress;
traceability Requirement-Rule-Concept-Design-Task-Test-Impl.

## 5. Impact, gaps, decisions

Impact: backend domain (Money, Income/Expense/FinancialInput, shared
CashFlowCalculator reuse, policy.rounding); application (OwnerId services,
transactions, FinancialInput mapping, export wiring); api.profile DTOs +
controllers reusing ProblemDetailAdvice; persistence (next Flyway migration
after V1 with CHECKs/FKs/unique/indexes + owner repos); frontend axios client,
money/profile types, profile page/form/summary/provenance, Pinia+router,
refetch-after-mutation; tests (CF/DM reference, MockMvc, Testcontainers
precision/constraints/isolation, sweep/matrix for new routes, Vitest + e2e).

Existing/reusable: SecurityConfig/session/CSRF, CurrentOwnerProvider/OwnerId,
ProblemDetailAdvice, AccountEntity/Repo, OwnershipQueries, V1 migration +
harness, IntegrationTestBase + sweep/matrix/cascade tests, purity guard,
normative oracle + 004 data-model/rest-api + 007 docs.

Missing: all 001 tables/migration/entities/repos, domain types + derivation,
API + validation mapping, UI + client + store wiring, export participants,
new-route 401/404 coverage, PurityTest/ReferenceCaseRunner execution.

Conflicting: React+TanStack plans vs Vue+Pinia code (001 T003/T009/T011 paths
do not exist); backend package map vs 007 lanes (api/error absent,
api.common exists; no domain impl); migration lineage V001__profile vs
V001__financial_inputs vs delivered V1__auth; CurrentPositionCalculator vs
locked CashFlowCalculator + FinancialEngine order (no duplicate rules);
001 plan Spring-owns-math vs constitution domain-owns-math (latter governs).

Risky: bypassable ownership; export/DTO leaks; float/JS-number money; hidden
rounding; stale persisted projections; client-computed totals; assumption as
fact; duplicate domain logic breaking 002-006; non-monthly/silent future
values; migration collision; scope creep (multi-profile, conversion, feeds).

Open decisions for Superpowers: migration number/grouping; single-profile
enforcement depth; income/expense delete vs active=false; position code home
(shared CashFlowCalculator required); Vue file placement; PUT replace vs PATCH
and profileId linkage validation. No ADR taken here.
