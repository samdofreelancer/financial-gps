# Tasks: 007-authentication — Authentication, Authorization & Data Ownership

**Input**: ONLY `specs/007-authentication/spec.md` + `specs/007-authentication/plan.md`
(authoritative). `contracts/auth-api.md`, `data-model.md`, `research.md` are restatements of those
two and never introduce requirements of their own.

**Tests**: MANDATORY — strict TDD per plan: red test → minimal implementation → green → refactor.

**Organization**: by the plan's five-plus-two areas (`A1…A7`) instead of `[US#]`; the Area field +
final traceability matrix replace story labels. Spec's US1–US5 remain fully covered (see matrix).

**Scope guards baked into this list**: no task touches `com.financialgps.domain.*` or any
financial feature controller; the only cross-lane artifact is the *boundary guard test* (T005),
which asserts the boundary rather than implementing anything.

## Task format

Checkbox = ID + primary path. Bullets: **Area** · **Trace** (spec FR/SC/US + plan §/area) ·
**Depends on** · **Test-first (RED)** · **Expected outcome (GREEN)**.

---

## Area A1 — Platform Foundation

- [x] T001 Create `SecurityConfig` filter chain + route rules + Spring Session JDBC wiring in `backend/src/main/java/com/financialgps/platform/security/SecurityConfig.java`; test first in `backend/src/test/java/com/financialgps/platform/security/UnauthenticatedAccessSweepTest.java`
  - **Area**: A1 · **Trace**: plan §Boundary/§Architecture; spec FR-006, SC-005.
  - **Depends on**: none.
  - **Test-first (RED)**: sweep test — every protected route (stub `/api/v1/account/me`) without session → `401` ProblemDetail code `AUTH_REQUIRED`; `/auth/register`, `/auth/login`, `/auth/csrf` reachable (not 401). Run → fail (no config).
  - **Expected outcome**: route rules live in one place with a uniform `401 AUTH_REQUIRED` ProblemDetail; security baseline only — **authentication-cookie flags are NOT asserted here** (no cookies exist yet; that contract belongs to T009, swept in T019).

- [x] T002 Implement public CSRF warm-up endpoint `GET /api/v1/auth/csrf` in `backend/src/main/java/com/financialgps/api/auth/AuthController.java`; test first in `CsrfWarmUpTest.java`
  - **Area**: A1 · **Trace**: plan §CSRF warm-up + route rules; contracts/auth-api.md.
  - **Depends on**: T001.
  - **Test-first (RED)**: anonymous `GET /auth/csrf` → `200` + `XSRF-TOKEN` cookie present; repeat call idempotent; `POST /auth/login` without `X-XSRF-TOKEN` → `403 CSRF_INVALID`. Run → fail.
  - **Expected outcome**: SPA can obtain a token unauthenticated; state-changing requests enforce the header.

- [x] T003 [P] Add `AuthProperties` (`financial.auth.*`) + password policy component in `backend/src/main/java/com/financialgps/platform/security/AuthProperties.java` and `application/account/PasswordPolicy.java`; test first in `PasswordPolicyTest.java`
  - **Area**: A1 · **Trace**: plan §Configuration & secrets; spec FR-005.
  - **Depends on**: none (file-disjoint from T001/T002/T004).
  - **Test-first (RED)**: policy matrix — accepts 10-char letter+digit; rejects short/no-digit/no-letter/max>128; violation message lists active requirements; defaults match plan (10/true/true/128, bcrypt 12, timeout 30m). Run → fail.
  - **Expected outcome**: pure, fully unit-tested policy object ready for registration flow.

- [x] T004 [P] Add Flyway migration `V1__auth.sql` (account table, functional unique index `lower(email)`, Spring Session JDBC schema) in `backend/src/main/resources/db/migration/`; test first in `AccountSchemaTest.java`
  - **Area**: A1 · **Trace**: plan §Data model; research §5/§9; spec FR-001/FR-004 foundation.
  - **Depends on**: none (file-disjoint).
  - **Test-first (RED)**: Testcontainers — insert `User@Example.com` then `user@example.com` → second insert violates unique index (case-insensitive uniqueness); `SPRING_SESSION` tables exist. Run → fail (no migration).
  - **Expected outcome**: schema green under real PostgreSQL; display case preserved.

- [x] T005 Add platform-side domain-boundary guard in `backend/src/test/java/com/financialgps/platform/security/DomainBoundaryGuardTest.java`
  - **Area**: A1 · **Trace**: plan §Boundary with the Financial Domain Engine (locked); rules 3–5 of the feature brief.
  - **Depends on**: T001.
  - **Test-first (RED proof)**: temporarily add a probe class under `com.financialgps.domain` importing Spring/JDBC/`Clock`/`Random` → guard must FAIL naming type+violation; delete probe. Real assertion: no type under `com.financialgps.domain.*` references `org.springframework.*|jakarta.*|java.sql.*|java.time.Clock|java.util.Random|platform/auth packages`.
  - **Expected outcome**: permanent automated proof that auth never leaks into the engine (vacuously green today; stays meaningful as code grows).

---

## Area A2 — Registration

- [x] T006 Implement `RegisterOwnerService` in `backend/src/main/java/com/financialgps/application/account/RegisterOwnerService.java`; test first in `RegisterOwnerServiceTest.java`
  - **Area**: A2 · **Trace**: spec FR-001, FR-004, FR-005, SC-002; plan §Security flow/Register.
  - **Depends on**: T003, T004.
  - **Test-first (RED)**: unit tests — happy path persists account with BCrypt hash and **never** the raw password (SC-002); duplicate email differing only by case → `RegistrationRejected` carrying the GENERIC public payload (no "taken" hint); weak password → `PasswordPolicyViolation` listing requirements. Run → fail.
  - **Expected outcome**: green; service depends only on `PasswordPolicy`, `AccountRepository`, `PasswordHasher` abstractions.

- [x] T007 Wire registration endpoint `POST /api/v1/auth/register` in `backend/src/main/java/com/financialgps/api/auth/AuthController.java`; test first in `RegistrationEndpointTest.java`
  - **Area**: A2 · **Trace**: spec US1, FR-001–FR-005, SC-001/SC-002; plan §Security flow/Register; contracts §register.
  - **Depends on**: T002, T006.
  - **Test-first (RED)**: happy path → `201` + `Location: /api/v1/account/me` + signed-in session cookie (`GET /account/me` works immediately = SC-001 journey, empty workspace); weak password → `422 PASSWORD_POLICY_VIOLATION` listing requirements; duplicate email (case-insensitive) → `409 REGISTRATION_FAILED` generic body containing no existence wording; malformed DTO → `400 VALIDATION_FAILED`; response/DTO field audit shows no `password_hash` exposure. Run → fail.
  - **Expected outcome**: full registration slice green including auto sign-in.

---

## Area A3 — Login / Logout / Session

- [x] T008 [P] Implement `AuthenticateOwnerService` in `backend/src/main/java/com/financialgps/application/account/AuthenticateOwnerService.java`; test first in `AuthenticateOwnerServiceTest.java`
  - **Area**: A3 · **Trace**: spec FR-002, FR-004-analogue for login; plan §Security flow/Login.
  - **Depends on**: T003, T004 (accounts seeded directly via repository in tests — independent of A2).
  - **Test-first (RED)**: correct credentials → authenticated account result; unknown email vs wrong password return the SAME outcome type/value (service-level non-revelation); BCrypt verify used (constant-time). Run → fail.
  - **Expected outcome**: green; service exposes nothing that lets callers distinguish the two failure causes.

- [x] T009 Wire `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/account/me` in `backend/src/main/java/com/financialgps/api/auth/SessionController.java`; test first in `SessionFlowTest.java`
  - **Area**: A3 · **Trace**: spec US2, US4, FR-002, FR-003, FR-009; plan §Security flow/Login+Logout.
  - **Depends on**: T008 (+T002 for CSRF on POSTs).
  - **Test-first (RED)**: login success → `204`, session id **rotated** vs pre-login cookie (fixation defense), issued `JSESSIONID` carries the authentication-cookie contract `HttpOnly` + `Secure` + `SameSite=Lax` (owned by THIS task; swept again in T019), `/account/me` returns account summary; failed login → identical `401 INVALID_CREDENTIALS` body byte-for-byte for unknown-email vs wrong-password; logout → `204`, old cookie then rejected (`401`) proving server-side invalidation. Run → fail.
  - **Expected outcome**: session lifecycle green end-to-end incl. fixation defense.

- [x] T010 Consolidate anti-enumeration suite in `backend/src/test/java/com/financialgps/api/auth/AntiRevelationTest.java`
  - **Area**: A3 · **Trace**: spec FR-004 + login analogue; plan §Security flow non-revelation bullets; research §6.
  - **Depends on**: T007, T009.
  - **Test-first (RED)**: byte-equality assertions — login(unknown email) == login(wrong password) responses; registration rejection body scanned: must not contain "taken", "exists", "duplicate"; both failure classes use catalogue codes only. Run → fail if any drift.
  - **Expected outcome**: single permanent suite guarding every non-revelation contract.

- [x] T011 Idle session expiry test (SC-004) in `backend/src/test/java/com/financialgps/platform/security/IdleSessionExpiryTest.java`
  - **Area**: A3 · **Trace**: spec US4, FR-009, SC-004; plan §Security flow/Idle expiry.
  - **Depends on**: T009.
  - **Test-first (RED)**: profile override `financial.auth.session-idle-timeout=PT2S` → login → wait past timeout → protected call `401 AUTH_REQUIRED` → re-login succeeds; Spring Session row expired in DB. Run → fail (timeout not wired).
  - **Expected outcome**: expiry proven through real Spring Session JDBC, not mocked time.

---

## Area A4 — Ownership Authorization

- [x] T012 Implement `CurrentOwnerProvider` + `OwnerId` value type in `backend/src/main/java/com/financialgps/platform/security/`; test first in `CurrentOwnerProviderTest.java`
  - **Area**: A4 · **Trace**: spec FR-008; plan §Ownership enforcement flow steps 1–2, §Layer boundaries rule 2.
  - **Depends on**: T009.
  - **Test-first (RED)**: authenticated session resolves to the immutable `OwnerId(uuid)`; anonymous/no-session → `AuthRequiredException` mapped to uniform `401 AUTH_REQUIRED`. Run → fail.
  - **Expected outcome**: identity resolution stops at the platform layer; services downstream see only `OwnerId`.

- [x] T013 Prove ownership enforcement + identical-404 semantics via the test-fixture owned resource in `backend/src/test/java/com/financialgps/platform/security/OwnedResourceIsolationTest.java` (fixture controller/entity live in test sources only)
  - **Area**: A4 · **Trace**: spec FR-007, FR-010; plan §Ownership enforcement flow steps 3–4 + §Implementation scope note.
  - **Depends on**: T012.
  - **Test-first (RED)**: seed owner A's fixture resource; B GET/PUT/DELETE by A's id → `404 RESOURCE_NOT_FOUND` whose body is byte-identical to the truly-missing-id case; A's own requests succeed; attacker-supplied foreign `ownerId` can never widen a query (queries take `OwnerId` from context only). Run → fail.
  - **Expected outcome**: IDOR-proof pattern demonstrated once, reusable for every real financial resource; fixture is NOT a production financial controller.

- [x] T014 Build authorization matrix harness in `backend/src/test/java/com/financialgps/platform/security/AuthorizationMatrixTest.java`
  - **Area**: A4 · **Trace**: spec US3, FR-006/FR-007/FR-013, SC-003, SC-008; plan §Ownership enforcement flow step 6.
  - **Depends on**: T013.
  - **Test-first (RED)**: matrix = 2 accounts × {GET, LIST, UPDATE, ARCHIVE, DELETE} × every registered owned resource type (registry-driven so future tables join automatically) → assert ZERO cross-owner successes and zero data leaks; unauthenticated variants all `401`. Run → fail until harness + registry exist.
  - **Expected outcome**: SC-003 provable in one command; new resources are covered by registering, not by writing new tests.

---

## Area A5 — Data Export

- [x] T015 Implement `DataExportService` + exporter registry in `backend/src/main/java/com/financialgps/application/account/DataExportService.java`; test first in `DataExportServiceTest.java`
  - **Area**: A5 · **Trace**: spec FR-011, SC-006; plan §Account lifecycle & cascade/Export + extension-contract clause; data-model.md bundle shape.
  - **Depends on**: T012, T004.
  - **Test-first (RED)**: seeded rows across registry tables → bundle contains every section, arrays sorted by `id`, empty collections serialized as `[]`; **extension point proven** by registering a fake exporter that contributes a top-level section — no compile/runtime reference to feature 006; sections limited to caller's `OwnerId`. Run → fail.
  - **Expected outcome**: deterministic assembly engine; scenario export later plugs in from 006's side only.

- [x] T016 Wire `GET /api/v1/account/export` in `backend/src/main/java/com/financialgps/api/account/AccountController.java`; test first in `ExportEndpointTest.java`
  - **Area**: A5 · **Trace**: spec US5-1, FR-011, FR-013, SC-006, SC-008; contracts §export.
  - **Depends on**: T015.
  - **Test-first (RED)**: signed-in owner → `200` JSON matching data-model shape (`formatVersion`, account block, sections); two consecutive exports with unchanged data are byte-identical; exported content equals stored records field-by-field; unauthenticated → `401`; cross-owner impossible (no owner parameter exists). Run → fail.
  - **Expected outcome**: SC-006 green end-to-end.

---

## Area A6 — Account Deletion

- [x] T017 Implement `DeleteAccountService` confirmation gate + transactional delete in `backend/src/main/java/com/financialgps/application/account/DeleteAccountService.java`; test first in `DeleteAccountServiceTest.java`
  - **Area**: A6 · **Trace**: spec FR-012, FR-013; plan §Account lifecycle & cascade/Delete.
  - **Depends on**: T004, T012 (`OwnerId` type only — session interaction abstracted behind `SessionInvalidationPort`, mocked in these tests; no HTTP/session flow required).
  - **Test-first (RED)**: missing/mismatched confirmation → `ConfirmationRequiredException` → `400 CONFIRMATION_REQUIRED`; exact `"DELETE"` → account deleted within a single transaction and `SessionInvalidationPort.invalidate(ownerId)` invoked (mock verified). Run → fail.
  - **Expected outcome**: confirmation gate + transactional delete green at service level with zero HTTP/session-suite dependency — the real "old cookie → 401" proof lives in T018's integration pass.

- [x] T018 Prove FK cascade + zero-orphan guarantee in `backend/src/test/java/com/financialgps/infrastructure/persistence/ownership/CascadeZeroOrphanTest.java`
  - **Area**: A6 · **Trace**: spec FR-013, FR-014, SC-007, SC-008; plan §Data model (registry) + cascade bullet.
  - **Depends on**: T017, T015.
  - **Test-first (RED)**: seed owner A with rows in EVERY registry table (incl. archived + ledger states) and owner B as control; delete A's account via the service → information_schema-driven scan asserts **0 remaining rows in any table having an `owner_id` column**; B's rows untouched; B can still export/delete. Integration pass: the same deletion through `DELETE /api/v1/account` leaves the caller's old session cookie rejected (`401`). Run → fail.
  - **Expected outcome**: SC-007 zero-orphan proof is generic (future tables auto-covered), not table-by-table.

---

## Area A7 — Security Hardening & Documentation

- [x] T019 Uniform-error & security-header sweep in `backend/src/test/java/com/financialgps/api/error/SecurityErrorSweepTest.java`
  - **Area**: A7 · **Trace**: plan §Error catalogue; spec SC-005; contracts route table.
  - **Depends on**: T007, T009, T013, T017.
  - **Test-first (RED)**: parameterized suite asserting every catalogue row returns its documented status+code (`401 AUTH_REQUIRED`, `401 INVALID_CREDENTIALS`, `403 CSRF_INVALID`, `404 RESOURCE_NOT_FOUND`, `409 REGISTRATION_FAILED`, `422 PASSWORD_POLICY_VIOLATION`, `400 CONFIRMATION_REQUIRED`, `400 VALIDATION_FAILED`); `Set-Cookie` flags (`HttpOnly`, `Secure`, `SameSite=Lax`); no stack traces/infra details in any body. Run → fail on first drift.
  - **Expected outcome**: one suite that fails the build the moment an error contract regresses.

- [x] T020 Execute quickstart validation end-to-end per `specs/007-authentication/quickstart.md`
  - **Area**: A7 · **Trace**: plan §Testing strategy (all layers); spec US1–US5 acceptance scenarios.
  - **Depends on**: T001–T019.
  - **Test-first (RED)**: run the tagged quickstart suite (scenarios 1–8) — any failing scenario is a task defect or spec drift to resolve before completion.
  - **Expected outcome**: full feature demonstrable via quickstart; manual curl smoke reproduced with real tokens.

---

## Dependencies & execution order (plan order preserved)

```text
A1: T001 → {T002, T005}
        └→ [P] T003 ∥ [P] T004            (file-disjoint; start anytime after kickoff)
A2: T006(←T003,T004) → T007(←T002,T006)
A3: T008(←T003,T004) [P vs A2] → T009(←T008) → {T010(←T007,T009), T011(←T009)}
A4: T012(←T009) → T013 → T014
A5: T015(←T012) → T016
A6: T017(←T004,T012) → T018(←T015,T017)   ·   session-invalidation integration asserted in T018
A7: T019(←T007,T009,T013,T017) → T020(all)
```

Parallel windows: **T003 ∥ T004** (and vs T001/T002) inside A1 · **A2 tasks ∥ A3 tasks** after
their shared A1 prerequisites (per plan's A1→{A2,A3} diagram) · **after T012**, {T013→T014,
T015→T016, T017} form a three-way window (T017 no longer waits for the session suite). Remaining
hard orderings: T018 needs T015+T017; T020 needs everything.

## Required-testing-coverage checklist (brief item → task)

| Brief coverage item | Task(s) |
|---|---|
| Registration happy path | T007 |
| Duplicate-email non-revelation | T006, T007, T010 |
| Password policy | T003, T006, T007 |
| Login success/failure | T008, T009 |
| Unknown-email vs wrong-password equality | T008, T009, T010 |
| Logout invalidation | T009 |
| Session fixation defense | T008, T009 |
| Idle session expiry | T011 |
| CSRF behavior | T002 (+enforcement in every POST task, swept in T019) |
| Unauthenticated access | T001, T014 |
| Owner isolation | T013, T014 |
| Cross-owner resource → identical 404 | T013 |
| Export completeness | T015, T016 |
| Deletion confirmation | T017 |
| FK cascade | T018 |
| Zero orphan records | T018 |
| Financial Domain boundary guard | T005 |

---

## Traceability matrix (Spec Requirement → Plan Item → Task → Automated Test)

| Spec requirement | Plan item | Task | Automated test |
|---|---|---|---|
| US1 Register (FR-001), SC-001 journey | §Security flow/Register; A2 | T006, T007 | `RegistrationEndpointTest` (happy path + me-empty-workspace journey) |
| FR-004 duplicate-email non-revelation | §Security flow/Register step 4; A2 | T006, T007, T010 | `RegisterOwnerServiceTest`, `RegistrationEndpointTest`, `AntiRevelationTest` |
| FR-005 password policy | §Configuration/Password policy; A1+A2 | T003, T006, T007 | `PasswordPolicyTest`, service + slice tests |
| SC-002 never plaintext | §Security flow/Register step 3; A2 | T006, T007 | `RegisterOwnerServiceTest` storage assertions + DTO field audit |
| US2 / FR-002 login | §Security flow/Login; A3 | T008, T009 | `AuthenticateOwnerServiceTest`, `SessionFlowTest` |
| FR-003 logout | §Security flow/Logout; A3 | T009 | `SessionFlowTest` (server-side invalidation) |
| Session fixation defense | §Security flow/Login step 3; A3 | T008, T009 | `SessionFlowTest` rotation assertion |
| Unknown-email vs wrong-password equality | §Security flow non-revelation; A3 | T008, T009, T010 | `SessionFlowTest` + `AntiRevelationTest` byte-equality |
| US4 / FR-009 / SC-004 idle expiry | §Idle expiry; A3 | T011 | `IdleSessionExpiryTest` |
| CSRF behavior | §CSRF warm-up; A1 | T002 (+T019 sweep) | `CsrfWarmUpTest`, `SecurityErrorSweepTest` (`403 CSRF_INVALID`) |
| US3 / FR-006 unauthenticated access, SC-005 | §Ownership flow step 1; A1/A4 | T001, T014 | `UnauthenticatedAccessSweepTest`, matrix unauthenticated variants |
| FR-007 / FR-008 owner scoping & resolution | §Ownership flow steps 2–3; A4 | T012, T013, T014 | `CurrentOwnerProviderTest`, `OwnedResourceIsolationTest`, `AuthorizationMatrixTest` |
| FR-010 cross-owner → identical 404 | §Ownership flow step 4; A4 | T013 | `OwnedResourceIsolationTest` body-equality |
| SC-003 zero cross-owner successes | §Ownership flow step 6; A4 | T014 | `AuthorizationMatrixTest` |
| US5-1 / FR-011 export, SC-006 completeness | §Export; A5 | T015, T016 | `DataExportServiceTest`, `ExportEndpointTest` determinism/equality |
| Export extension contract (scenarios = 006) | §Export extension-contract clause; A5 | T015 | fake-exporter registration test in `DataExportServiceTest` |
| FR-012 delete after confirmation | §Delete; A6 | T017 | `DeleteAccountServiceTest` gate + transaction |
| FR-013 cross-owner export/delete rejected | §Ownership flow step 5; A6 | T014, T018 | matrix negatives + control-owner checks in `CascadeZeroOrphanTest` |
| FR-014 / SC-007 cascade, zero orphans | §Data model registry + cascade; A6 | T018 | `CascadeZeroOrphanTest` information_schema scan |
| SC-008 no foreign export/delete succeeds | §Ownership flow step 5; A4/A6 | T014, T018 | matrix negatives + cascade test controls |
| Boundary: engine stays identity-free (plan §Boundary; domain plan/calc-rules untouched) | §Boundary with the Financial Domain Engine; A1 | T005 | `DomainBoundaryGuardTest` |

## Deviations (explicit)

1. **Area labels replace `[US#]`** — the plan organizes work into areas A1–A7 (spec's planning
   note); US coverage is preserved and shown in the matrix above.
2. **T013/T014 use a test-fixture owned resource** — required to prove enforcement now without
   implementing any Financial*Controller (forbidden). The fixture lives in test sources only and is
   documented as NOT a production financial controller.
3. **T008 seeds accounts via repository** instead of calling the register flow — keeps the
   plan's A2 ∥ A3 parallel window honest.

## Done when

- [x] All 20 checkboxes complete; every task's RED test was observed failing first
- [x] Coverage checklist above: every row maps to a green automated test
- [x] Traceability matrix has no row whose Test column is empty
- [x] No task modified `com.financialgps.domain.*` or any financial feature controller
      (`DomainBoundaryGuardTest` green)





