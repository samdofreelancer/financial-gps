# Auth & Account API Contract (007)

> In-process backend HTTP API consumed by the same-origin SPA. All bodies are JSON.
> Errors are RFC 7807 `application/problem+json` with a stable `code` field.
> Cookies: `JSESSIONID` (HttpOnly, Secure, SameSite=Lax) and `XSRF-TOKEN` (readable by the SPA,
> sent back as `X-XSRF-TOKEN` header on every state-changing request).

## Endpoints

### GET /api/v1/auth/csrf

Public, safe, idempotent CSRF warm-up for the SPA. No authentication required.

- `200` — ensures an `XSRF-TOKEN` cookie exists (sets it when absent); no response body semantics
  beyond cookies. The SPA then sends `X-XSRF-TOKEN` on every state-changing request.
- Repeated calls while a valid token exists do not rotate it (idempotent).

### POST /api/v1/auth/register

Request:
```json
{ "email": "User@Example.com", "password": "correct horse battery" }
```

- `201 Created` — account created AND signed in (session cookie set).
  Headers: `Location: /api/v1/account/me`. Body: `{ "accountId": "<uuid>", "email": "User@Example.com" }`
  (display case preserved).
- `422` `PASSWORD_POLICY_VIOLATION` — body lists active requirements (FR-005). Depends only on the
  submitted password.
- `409` `REGISTRATION_FAILED` — email conflict (case-insensitive). Generic body, identical for all
  email-based rejections; never states that the email exists (FR-004):
  ```json
  { "title": "Registration failed", "code": "REGISTRATION_FAILED" }
  ```
- `400` `VALIDATION_FAILED` — malformed DTO.

### POST /api/v1/auth/login

Request: `{ "email": "...", "password": "..." }`

- `204 No Content` + session cookie. Session ID is rotated on success (fixation defense).
- `401` `INVALID_CREDENTIALS` — **identical body** for unknown email and wrong password
  (`{ "title": "Invalid credentials", "code": "INVALID_CREDENTIALS" }`).

### POST /api/v1/auth/logout

- `204` — server-side session invalidated; cookies cleared. Requires authentication.

### GET /api/v1/account/me

- `200` — `{ "accountId", "email", "createdAt" }`; proof of an authenticated empty workspace
  right after registration (SC-001 journey endpoint).
- `401 AUTH_REQUIRED` when session missing/expired.

### GET /api/v1/account/export

- `200` — `application/json`, deterministic bundle per data-model.md (FR-011, SC-006).
  Optional `Content-Disposition: attachment; filename="financialgps-export.json"`.

### DELETE /api/v1/account

Request: `{ "confirmation": "DELETE" }`

- `400 CONFIRMATION_REQUIRED` when header/body confirmation missing or not exactly `DELETE`.
- `204` — account row deleted in one transaction; FK cascade removes every owned record;
  current session invalidated (FR-012/FR-014, SC-007).

## Route protection summary

| Routes | Access |
|---|---|
| `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/csrf` | public (register/login still CSRF-enforced) |
| everything else under `/api/v1/**` | authenticated; unauthenticated → `401 AUTH_REQUIRED` |

## Internal application contracts (non-HTTP)

```java
OwnerId CurrentOwnerProvider.requireCurrent();      // throws AuthRequiredException → 401
Account register(RegisterCommand cmd);              // throws PasswordPolicyViolation | RegistrationRejected
Optional<Account> authenticate(String email, String rawPassword);
ExportedData exportAll(OwnerId owner);              // deterministic ordering by id
void deleteAccount(OwnerId owner, String confirmation); // throws ConfirmationRequiredException
```

No method in these signatures — nor in `com.financialgps.domain` — accepts a session, token,
principal, request, or user id beyond the explicit `OwnerId` at the application boundary.
