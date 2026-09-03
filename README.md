# finanial-gps

Financial GPS — personal financial planning platform (profile, debt, goals, GPS projection,
roadmap, scenario planning) with a pure, identity-free financial domain engine.

## Specs

Feature specs live in [`specs/`](specs/). Implementation order is driven by each feature's plan:

1. `001-financial-profile` → `002-debt-management` → `003-financial-goals` → `004-financial-gps`
2. `005-financial-roadmap` → `006-scenario-planning`
3. `007-authentication` — platform capability (auth/authz/ownership), independent of the domain engine
4. `008-financial-timing-allocation` → `009-financial-review`

`specs/financial-domain/` holds the shared domain contract (calculation rules, engine contract).

## Backend (implemented: 007-authentication)

Spring Boot 3.2 (Java 21) under `backend/`. The pure domain engine lives in
`com.financialgps.domain` and must never depend on Spring/JDBC/auth types (guarded by
`DomainBoundaryGuardTest`). Feature 007 delivers:

- Email/password accounts, BCrypt (cost 12), case-insensitive unique email (display case preserved)
- Server-side sessions via Spring Session JDBC (HttpOnly `SESSION` cookie, 30 min idle timeout,
  session-id rotation at sign-in, logout invalidates server-side)
- CSRF double-submit cookie handshake: `GET /api/v1/auth/csrf` seeds `XSRF-TOKEN`; state-changing
  requests echo it as the `X-XSRF-TOKEN` header
- Ownership-based authorization (`OwnerId` + `CurrentOwnerProvider`); cross-owner access is an
  indistinguishable `404 RESOURCE_NOT_FOUND`
- Owner data export (deterministic, byte-identical bundle) and confirmed account deletion with
  FK-cascade + zero-orphan guarantee
- Uniform RFC 7807 problems: `AUTH_REQUIRED`, `INVALID_CREDENTIALS`, `CSRF_INVALID`,
  `REGISTRATION_FAILED`, `PASSWORD_POLICY_VIOLATION`, `CONFIRMATION_REQUIRED`,
  `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED`

### Run

```bash
docker compose up -d                                # PostgreSQL 16 on localhost:5434
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local   # local profile = cookie.secure=false
```

### Test

```bash
cd backend
mvn test          # unit + MockMvc slices + Testcontainers PostgreSQL full-flow suite
```

Integration tests start a Testcontainers PostgreSQL automatically (Docker required).

### API quickstart (curl)

```bash
curl -c cj.txt localhost:8080/api/v1/auth/csrf -i
TOKEN=$(awk '/XSRF-TOKEN/ {print $7}' cj.txt)
curl -b cj.txt -c cj.txt -X POST localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $TOKEN" \
  -d '{"email":"user@example.com","password":"correct horse battery1"}' -i
curl -b cj.txt localhost:8080/api/v1/account/me
curl -b cj.txt localhost:8080/api/v1/account/export -o export.json
curl -b cj.txt -X DELETE localhost:8080/api/v1/account \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $TOKEN" \
  -d '{"confirmation":"DELETE"}' -i
```
