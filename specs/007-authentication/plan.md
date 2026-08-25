# Implementation Plan: Authentication, Authorization & Data Ownership

**Branch**: `007-authentication` | **Date**: 2026-08-25 | **Status**: Draft
**Spec**: [spec](spec.md) · Boundary refs: [engine-contract](../financial-domain/contracts/engine-contract.md) ·
[calculation-rules](../financial-domain/calculation-rules.md) · [domain plan](../financial-domain/plan.md)

**Input**: `specs/007-authentication/spec.md` is authoritative for authentication, authorization,
and data-ownership behavior. The financial-domain documents are referenced ONLY to fix the
integration boundary; this plan introduces no financial rule and modifies no domain artifact.

## Summary

Build the platform capability that makes financial data private state: email/password accounts
with server-side browser sessions, ownership-based authorization over every financial resource,
owner-scoped data export, and confirmed account deletion with full cascade. All of it lives in
the **application/platform layer** of the Spring Boot backend. The pure domain engine
(`com.financialgps.domain`) is a consumer-facing function that never sees identity — it keeps
receiving only financial inputs, assumptions, `asOfDate`, and policy.

## Technical Context

**Language/Version**: Java 21 LTS
**Backend**: Spring Boot 3.x (Web, Security, Validation, Data JPA) — matches the existing
backend conventions (`com.financialgps.api.*`, `com.financialgps.application.*`,
`com.financialgps.infrastructure.persistence.*`, ProblemDetail error advice from feature 001).
**Database**: PostgreSQL (existing dev instance from the `004` setup); schema migrations via
Flyway. **Sessions**: Servlet sessions persisted with Spring Session JDBC (restart-safe,
standard schema, configurable idle timeout). **Password hashing**: BCrypt via
`spring-security-crypto`. **Testing**: JUnit 5, MockMvc, @WebMvcTest slices, @SpringBootTest,
Mockito, Testcontainers PostgreSQL.
**Frontend**: out of scope for this plan (the SPA consumes these endpoints; cookie + CSRF
contract below is designed for it).
**Excludes** (per spec Assumptions): password reset, email verification, admin users, OAuth/social
login, MFA, cross-device session management, additional roles.

## Constitution Check

| Principle | Response | Status |
|---|---|---|
| II Deterministic calculation | No change to any domain artifact; engine signature untouched | ✅ |
| VIII Testable domain | Domain stays pure and already oracle-tested; this feature adds platform-layer tests only | ✅ |
| X Privacy & minimization | This feature IS the enforcement: owner-scoped storage/access, export, delete-cascade | ✅ |
| XIII Review/history | Snapshots & ledger are included in export scope and cascade inventory (registry-driven) | ✅ |
| IX Simple before intelligent | Standard Spring Security patterns; no custom crypto; no AI anywhere | ✅ |

## Boundary with the Financial Domain Engine (locked)

- Dependency direction: `application/platform → domain` for *calling* `calculate(...)`; never
  `domain → platform`. Forbidden: `Financial Domain → Authentication`, `Financial Domain → User Identity`.
- The engine call signature stays `(input, assumptions, asOfDate, policy)`; this plan adds **no**
  parameter and **no** wrapper that injects userId/session/token/request/context into it.
- Identity resolution happens strictly before the engine call (see Security Flow); results are
  associated back to the owner when persisted/served.
- **Purity enforcement mechanism (resolves a cross-lane conflict)**: the domain tasks define a
  `PurityTest` forbidding Spring/DB/clock/randomness/AI in the engine. Because the repo uses ONE
  Maven module (`backend`) hosting both `com.financialgps.domain` and the platform packages,
  "no Spring on the classpath" cannot hold module-wide once this feature lands. Enforcement is
  therefore defined at **package level**: `PurityTest` (domain lane) plus a mirrored platform-side
  boundary test added by this plan assert that no type under `com.financialgps.domain.*`
  references any type from `org.springframework.*`, `jakarta.*`, `java.sql.*`, auth/platform
  packages, `java.time.Clock`, or `java.util.Random`. Alternative (splitting `domain` into its own
  Maven module) is deliberately deferred — recorded in research.md.
- This plan does NOT edit anything under `specs/financial-domain/`.

## Architecture

```text
Browser (SPA, same origin)
   │  JSESSIONID (HttpOnly, Secure, SameSite=Lax) + XSRF-TOKEN cookie
   ▼
┌─────────────────────────── Spring Security FilterChain ───────────────────────────┐
│ CsrfFilter · SessionManagement (Spring Session JDBC) · AuthenticationFilter       │
│ AuthorizationFilter (route rules)                                                 │
└──────────────┬────────────────────────────────────────────────────────────────────┘
               ▼
┌────────── api layer ──────────┐   ┌──── application layer ────┐   ┌── infrastructure ──┐
│ AuthController                │→→│ RegisterOwnerService      │→→│ AccountRepository   │
│ SessionController             │   │ AuthenticateOwnerService  │   │ (JPA, Flyway DDL)   │
│ AccountController(export/     │   │ LogoutService / session    │   │ Spring Session JDBC │
│   delete /me)                 │   │   lifecycle               │   │ tables              │
│ Financial*Controllers         │   │ DataExportService         │   │ owned-resource      │
│   (other features, guarded)   │   │ DeleteAccountService      │   │ tables (owner_id)   │
│ ProblemDetail advice          │   │ CurrentOwnerProvider      │   └─────────────────────┘
└───────────────────────────────┘   │ OwnerScopedQuery policy   │
                                    └───────────┬───────────────┘
                                                ▼ identity-free call at the very end
                                   com.financialgps.domain FinancialEngine.calculate(...)
```

Request order is exactly the approved boundary: HTTP → Authentication/Security → Authenticated
User → Application Service → (optionally) Domain Engine → Result. Identity never crosses the
last arrow.

## Components / modules

| Package | Responsibility | Must NOT do |
|---|---|---|
| `com.financialgps.api.auth` | `AuthController` (register/login/logout), request/response DTOs, validation annotations | contain hashing/session logic |
| `com.financialgps.api.account` | `/account/me`, `/account/export`, `DELETE /account` endpoints | own cascade logic |
| `com.financialgps.platform.security` | `SecurityConfig` (filter chain, CSRF, cookie flags, route rules), `CurrentOwnerProvider`, `AuthProperties` | touch financial semantics |
| `com.financialgps.application.account` | `RegisterOwnerService`, `AuthenticateOwnerService`, `DataExportService`, `DeleteAccountService`; transactions; uniform error mapping | SQL/JPA details |
| `com.financialgps.infrastructure.persistence.account` | `AccountEntity`, `AccountRepository` (`existsByLowerEmail`, `findByLowerEmail`) | business decisions |
| `com.financialgps.infrastructure.persistence.ownership` | owned-table registry constant + generic orphan-check queries used by tests | — |
| `com.financialgps.domain` | **untouched** by this feature | receive identity (ever) |

## Layer boundaries (rules for every task)

1. `api` = HTTP shape only: bind/validate DTOs, delegate, map results to ProblemDetail on error.
2. `application` = all authorization-relevant logic and transactions; services receive an explicit
   `OwnerId value` (UUID) resolved by `CurrentOwnerProvider` — never a principal/session object.
3. `platform.security` = filter chain + identity resolution only; it must stay replaceable.
4. `infrastructure.persistence` = JPA entities/repositories; **every** financial-resource query
   takes `ownerId` as a parameter (owner-scoped repository convention).
5. No new dependency of `com.financialgps.domain` on anything added here (guarded by test).

## Data model

Full details: [data-model.md](data-model.md). Summary:

| Table | Purpose / key rules |
|---|---|
| `account` | `id UUID PK`, `email` (display case preserved), `password_hash` (BCrypt), `role` (`OWNER` only for now), `created_at`. **Unique functional index on `lower(email)`** → case-insensitive uniqueness with display case preserved. |
| Spring Session JDBC tables | Standard `SPRING_SESSION`/`SPRING_SESSION_ATTRIBUTES`; idle timeout via property; survives restarts. |
| Owned financial tables | Every table from the spec list (`profile`, `income`, `expense`, `debt`, `goal`, `timeline_change`, `cash_allocation_rule`, `gps_snapshot`, `review_ledger_entry`, …) carries `owner_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE`. New owner-scoped tables MUST register here — cascade and orphan tests are registry-driven so future tables are covered automatically. |

Deletion is **hard delete in one transaction**: delete the `account` row; every owned row goes
with it via FK cascade. No soft-delete flag on `account` (resource-level archive stays a concern
of features 002/003/009 — this feature only guarantees the cascade).

## Security flow

**Register** (`POST /api/v1/auth/register`)
1. Validate DTO (email format; password policy from `AuthProperties`: default min 10 chars,
   ≥1 letter + ≥1 digit, max 128).
2. Policy violation → `422 PASSWORD_POLICY_VIOLATION` listing requirements (FR-005). This path is
   independent of email existence.
3. Hash password (BCrypt, cost from config, default 12); insert account.
4. Email already exists (case-insensitive) → `409 REGISTRATION_FAILED` generic body — identical
   title/code/message for every email-based rejection; never "email taken" (FR-004).
5. Success → auto sign-in per US1: create authenticated session, `201` + `Location: /api/v1/account/me`.

**Login** (`POST /api/v1/auth/login`)
1. Look up by `lower(email)`; verify BCrypt (constant-time compare).
2. Unknown email OR wrong password → identical `401 INVALID_CREDENTIALS` body both ways
   (non-revelation asserted byte-for-byte in tests).
3. Success → session fixation defense: `changeSessionId()` then set authentication; return `204`.

**Logout** (`POST /api/v1/auth/logout`) — invalidate session server-side, clear cookie, `204`.

**Idle expiry (FR-009)** — container-managed via Spring Session `maxInactiveInterval`
(`financial.auth.session-idle-timeout`, default 30m). Any request after expiry → `401 AUTH_REQUIRED`;
client redirects to login.

## Ownership enforcement flow (FR-006…FR-010, FR-013)

1. Filter chain requires authentication on everything except `/auth/register`, `/auth/login`,
   CSRF warm-up; unauthenticated → uniform `401 AUTH_REQUIRED` (SC-005).
2. `CurrentOwnerProvider` resolves the session's account to an immutable `OwnerId(UUID)`.
3. Controllers pass `ownerId` down; repositories filter by it — an attacker-supplied ID can never
   widen the query (FR-007, FR-008).
4. Single-resource access addressed by ID that exists but belongs to another owner →
   `404 RESOURCE_NOT_FOUND`, indistinguishable from a missing resource (FR-010, no existence leak).
5. Export/delete operate only on the caller's own `ownerId`; there is no endpoint accepting an
   arbitrary owner parameter (FR-013, SC-008).
6. Enforcement proof = automated **authorization matrix**: 2 accounts × {GET/LIST/UPDATE/ARCHIVE/
   DELETE} × each owned resource type → assert 0 cross-owner successes (SC-003).

## Account lifecycle & cascade (US5, FR-011–FR-014)

- **Export** `GET /api/v1/account/export` → one machine-readable JSON document assembled from
  read-only queries scoped to the caller: profile, incomes, expenses, debts, goals,
  timeline changes, allocation rules, GPS snapshots, review-ledger entries (+ scenarios when that
  feature lands). Deterministic ordering by (`id`) so exports are comparable (SC-006 equality check).
- **Export-then-delete path** (FR-012): UI flow = call export → call delete; documented in
  quickstart.md. No server-side staging copy (privacy minimization).
- **Delete** `DELETE /api/v1/account` with body `{"confirmation": "DELETE"}`:
  missing/mismatched confirmation → `400 CONFIRMATION_REQUIRED`; confirmed → single transaction:
  delete `account` row → FK cascades remove all owned rows (incl. archives, ledger);
  session invalidated; `204`. Irreversible — no grace window in v1 (documented per spec Assumptions).
- Zero-orphan guarantee (FR-014, SC-007): generic test iterates every table owning an `owner_id`
  column (from the ownership registry / information_schema) and asserts 0 rows remain.

## Error catalogue (all RFC 7807 ProblemDetail via the shared advice)

| HTTP | `code` | When |
|---|---|---|
| 401 | `AUTH_REQUIRED` | no/invalid/expired session on protected route |
| 401 | `INVALID_CREDENTIALS` | login failed (body identical for unknown email vs wrong password) |
| 403 | `CSRF_INVALID` | missing/invalid XSRF token on state-changing request |
| 404 | `RESOURCE_NOT_FOUND` | missing **or** cross-owner resource (no distinction) |
| 409 | `REGISTRATION_FAILED` | duplicate email — generic body, no existence hint |
| 422 | `PASSWORD_POLICY_VIOLATION` | policy failure; lists current requirements |
| 400 | `CONFIRMATION_REQUIRED` | deletion without exact `confirmation:"DELETE"` |
| 400 | `VALIDATION_FAILED` | malformed DTO |

## Configuration & secrets

`financial.auth.*` via `@ConfigurationProperties(AuthProperties)`, overridable by env vars:

| Key | Default | Notes |
|---|---|---|
| `session-idle-timeout` | `30m` | FR-009 |
| `bcrypt-strength` | `12` | hash cost |
| `cookie.secure` | `true` | `false` only for local http dev profile |
| `cookie.same-site` | `lax` | same-origin SPA |
| `password.min-length` / `require-digit` / `require-letter` | `10` / `true` / `true` | FR-005 |

Secrets (DB password) come from environment only; nothing secret is committed. No key material
exists (sessions are opaque server-side ids).

## Testing strategy (TDD; every area starts red)

| Layer | Tooling | Covers |
|---|---|---|
| Unit | JUnit 5 + Mockito | services: password policy, duplicate-email branch, export assembly ordering, confirmation gate |
| Web slice | `@WebMvcTest` + MockMvc | DTO validation, status codes, ProblemDetail bodies per error catalogue |
| Full flow | `@SpringBootTest` + MockMvc + **Testcontainers PostgreSQL** | register→login→use→logout journey; session persistence across context refresh; CSRF behavior |
| Authorization matrix | Testcontainers | SC-003/SC-008: 2 accounts × operations × resource types → 0 cross-owner successes; cross-owner id access → 404 body identical to missing-resource case |
| Non-revelation | body-equality assertions | login unknown-email vs wrong-password bodies byte-equal; registration duplicate-email body free of existence hints |
| Session expiry (SC-004) | Testcontainers + short timeout property / clock manipulation via Spring Session API | login → expire → next request 401 → re-login works |
| Cascade & orphans (SC-007) | Testcontainers + registry/information_schema scan | seed all owned tables for owner A (+ archives, ledger), delete account, assert 0 rows remain in EVERY owner-scoped table; account B untouched |
| Export completeness (SC-006) | Testcontainers | seeded dataset vs exported JSON — field-by-field equality, deterministic order |
| Boundary guard | plain unit/architecture test | no type under `com.financialgps.domain.*` references platform/auth/JDBC/Spring/Clock/Random types |

## Implementation areas & task dependencies (input for `/tasks`)

Per the spec's planning note — five areas inside one feature:

```text
A1 Platform foundation        SecurityConfig · session-JDBC schema · AuthProperties ·
                              ProblemDetail codes · domain-boundary guard test
   └─> A2 Registration         service + endpoint + policy/non-revelation tests
        └─> A3 Login/Logout/Session   fixation defense · idle expiry · /account/me
             └─> A4 Ownership core    CurrentOwnerProvider · OwnerId convention ·
                  |                   authorization-matrix harness (resource types registered)
                  ├─> A5 Data export  DataExportService + endpoint (deterministic bundle)
                  └─> A6 Account deletion  confirmation gate + cascade + orphan tests
                       └─> A7 Hardening & docs  uniform-error sweep · quickstart run · matrix green
```

- A5 and A6 are file-disjoint once A4 exists (safe parallel window).
- Every task = red test first (exact assertion listed in its description) → minimal code → refactor.
- Suggested MVP slice: A1–A4 (private workspace provable); A5–A7 complete the ownership story.

## Traceability (spec → design → test → area)

| Spec item | Design element | Test | Area |
|---|---|---|---|
| US1/FR-001 | Register flow, auto sign-in | full-flow happy path; SC-001 journey | A2 |
| FR-002 | Login flow, session cookie | login endpoint tests; restart persistence | A3 |
| FR-003 | Logout invalidation | logout test (session dead server-side) | A3 |
| FR-004 | Generic `409 REGISTRATION_FAILED` | non-revelation body test | A2 |
| FR-005 | Password policy + `422` listing rules | policy unit + slice tests | A2 |
| FR-006 | Route rules: everything protected | unauthenticated sweep (`401` on every financial route) | A1/A4 |
| FR-007/FR-008 | Owner-scoped repositories + `OwnerId` param convention | matrix harness | A4 |
| FR-009 | Spring Session idle timeout property | expiry test | A3 |
| FR-010 | Cross-owner → `404` indistinguishable | matrix + body-equality vs missing | A4 |
| US2 | Login/logout endpoints | journey tests | A3 |
| US4/SC-004 | Idle timeout config | expiry test with short timeout | A3 |
| US5/FR-011/SC-006 | DataExportService deterministic bundle | completeness/equality test | A5 |
| FR-012/SC-007 | Confirmation-gated hard delete + FK cascade | cascade/orphan tests | A6 |
| FR-013/SC-008 | No arbitrary-owner parameter anywhere | matrix negative cases | A4/A6 |
| FR-014 | Registry-driven zero-orphan check | information_schema scan test | A6 |
| SC-002 | BCrypt storage only; hash never returned | unit + entity/DTO field audit | A2 |
| SC-005 | Uniform `401 AUTH_REQUIRED` | endpoint sweep test | A1 |
| Constitution X/XIII | Export+delete cover snapshots & ledger via registry | included in A5/A6 tests | A5/A6 |

## Complexity Tracking

No constitution violations. The only deliberate deviation is documentation-level: purity
enforcement defined at package level instead of Maven-module level (see Boundary section;
rationale and deferred alternative in research.md).

<!-- /PLAN -->



