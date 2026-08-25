# Quickstart: 007-authentication validation guide

> How to prove registration, login, ownership isolation, export, and deletion work end-to-end.
> Run guide only — wiring lives in tasks/tests.

## Prerequisites

- JDK 21, Maven; Docker (Testcontainers) for integration tests.
- PostgreSQL reachable for manual runs (`compose.yaml` from the `004` setup).
- Build/test: `mvn -pl backend test`

## Automated validation scenarios (map to spec SC)

1. **Register journey (US1, SC-001)** — register → `201` + signed-in cookie → `GET /account/me`
   returns the new account with an empty workspace. Duplicate email (case-insensitive variant!)
   → generic `409 REGISTRATION_FAILED` whose body contains no existence hint (FR-004).
2. **Password policy (FR-005)** — weak password → `422` listing requirements.
3. **Login/logout (US2, FR-002/003)** — login sets a rotated session id; logout invalidates it:
   the old cookie no longer authenticates.
4. **Non-revelation (FR-004)** — byte-equality assertions: unknown-email vs wrong-password login
   responses are identical; registration rejection body contains no "taken"/"exists" wording.
5. **Idle expiry (US4, SC-004)** — with `financial.auth.session-idle-timeout=PT2S` test profile,
   login → wait past timeout → protected call returns `401 AUTH_REQUIRED` → re-login works.
6. **Authorization matrix (US3, FR-006–010, SC-003)** — two seeded accounts; for each owned
   resource type, B performs GET/LIST/UPDATE/ARCHIVE/DELETE against A's ids → every attempt fails
   (single-resource reads: `404` body identical to a truly missing id), and A's data is unchanged.
   Unauthenticated sweep: every financial route returns `401`.
7. **Export (US5, FR-011, SC-006)** — seed a full dataset for A; exported JSON equals the stored
   records field-by-field; two consecutive exports are byte-identical.
8. **Delete + cascade (US5, FR-012–014, SC-007/008)** — wrong/no confirmation → `400`;
   confirmed delete removes A's account row and leaves **zero rows** in every table having an
   `owner_id` column (registry/information_schema-driven check); B's rows intact; A's old session
   now `401`.

## Boundary guard

Run the platform-side purity/boundary test: no type under `com.financialgps.domain.*` may
reference Spring/JDBC/auth-platform/Clock/Random types. This must stay green after any auth task —
it is the automated form of "the engine never receives identity".

## Manual smoke (optional, curl)

```bash
# csrf warm-up (seeds XSRF-TOKEN into the cookie jar)
curl -c cj.txt localhost:8080/api/v1/auth/csrf -i
# register (signed in on success)
curl -b cj.txt -c cj.txt -X POST localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' -H 'X-XSRF-TOKEN: <token-from-jar>' \
  -d '{"email":"user@example.com","password":"correct horse battery"}' -i
# me / export
curl -b cj.txt localhost:8080/api/v1/account/me
curl -b cj.txt localhost:8080/api/v1/account/export -o export.json
# delete (irreversible)
curl -b cj.txt -X DELETE localhost:8080/api/v1/account \
  -H 'Content-Type: application/json' -H 'X-XSRF-TOKEN: <token-from-jar>' \
  -d '{"confirmation":"DELETE"}' -i
```

Expected outcomes: every check above green ⇒ US1–US5 and SC-001…SC-008 demonstrable without
touching `com.financialgps.domain`.
