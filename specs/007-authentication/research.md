# Research: 007-authentication

> Phase 0 output. Decisions are bounded by the spec (`spec.md`) and the locked architectural
> boundary with the Financial Domain Engine. No financial business rule is introduced here.

## 1. Framework & layer placement

- **Decision**: Spring Boot 3.x (Web/Security/Validation/Data JPA) in the platform/application
  packages of the existing `backend` module; feature code under
  `com.financialgps.{api,application,platform.security,infrastructure.persistence}`.
- **Rationale**: matches repo conventions already used by features 001–006; session cookies +
  CSRF + ownership filtering are exactly what Spring Security provides at the platform layer.
- **Alternatives**: hand-rolled auth (rejected — unnecessary crypto/session risk), placing auth
  inside the domain module (forbidden by the boundary).

## 2. Sessions: Spring Session JDBC

- **Decision**: Servlet sessions persisted via Spring Session JDBC; idle timeout from
  `financial.auth.session-idle-timeout` (default 30m).
- **Rationale**: server-side sessions are mandated by the spec (logout must terminate the session
  server-side); JDBC persistence makes logins survive restarts and gives a standard schema.
- **Alternatives**: container in-memory sessions (lost on restart; acceptable but weaker),
  JWT/stateless tokens (rejected — server-side revocation on logout/expiry is a hard requirement).

## 3. Password hashing

- **Decision**: BCrypt via `spring-security-crypto`, default strength 12 (configurable).
- **Alternatives**: Argon2id (stronger against GPUs but needs extra dependency + tuning; can be a
  later swap behind `PasswordHasher` interface), SHA-family (rejected outright).

## 4. CSRF

- **Decision**: keep CSRF protection enabled for state-changing requests using the cookie/header
  double-submit pattern (`XSRF-TOKEN` cookie read back as `X-XSRF-TOKEN`), same-origin SPA.
- **Rationale**: authentication is cookie-based, so CSRF is a real threat; disabling it would need
  a justification this spec does not offer.

## 5. Case-insensitive email uniqueness

- **Decision**: store display email verbatim + PostgreSQL unique functional index on `lower(email)`;
  all lookups go through `lower(email)`.

## 6. Anti-enumeration behavior

- **Decision**: login failures always return one identical body (`401 INVALID_CREDENTIALS`);
  duplicate-email registration returns a generic `409 REGISTRATION_FAILED` that never says
  "taken"; password-policy failures use a separate `422` whose content depends only on the
  submitted password.
- **Known residual risk (accepted, documented)**: distinct status codes between policy failure
  (422), success (201), and email-conflict (409) leave a weak probing signal. The spec's wording
  ("without revealing whether the email belongs to anyone") is satisfied by never disclosing
  existence in any message/body; fully uniform registration responses would break FR-005's
  requirement to explain password rejections. Trade-off recorded here deliberately.

## 7. Deletion mechanics

- **Decision**: hard delete in one transaction; every owner-scoped table carries
  `owner_id … REFERENCES account(id) ON DELETE CASCADE`; an ownership registry drives generic
  zero-orphan tests so future tables are covered automatically.
- **Alternatives**: soft-delete account flag (rejected — spec says records are "removed";
  resource-level archives remain the owning features' concern), application-level loop deletes
  (rejected — misses tables added later; DB-level cascade cannot be forgotten).

## 8. Domain purity enforcement across one Maven module

- **Decision**: enforce engine purity at package level — domain-lane `PurityTest` plus a mirrored
  platform-side guard asserting no `com.financialgps.domain.*` type references Spring/JDBC/auth/
  Clock/Random types.
- **Alternative (deferred)**: split `domain` into its own Maven module so purity holds at
  classpath level. Deferred because it restructures shared build config mid-stream; the package
  guard gives equivalent guarantees for source dependencies.

## 9. Migrations & test infrastructure

- **Decision**: Flyway for DDL (account table, functional index, enabling Spring Session schema);
  Testcontainers PostgreSQL for integration/matrix/cascade tests; MockMvc slices for controller
  contracts; Mockito units for services.
