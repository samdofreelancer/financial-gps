# API Contract

HTTP surface of the backend (`/api/v1/*`). Same-origin in all environments —
the frontend reaches it via the Vite/nginx `/api` proxy, so cookies flow with no CORS.
DTO field details live in `backend/.../api/*Dtos.java`; this page is the index.

## Cross-cutting rules

- **Auth:** HttpOnly `SESSION` cookie (server-side Spring Session JDBC). Missing/invalid
  session → `401 AUTH_REQUIRED`. The actor always comes from `CurrentOwnerProvider`;
  clients never send an owner id.
- **CSRF:** `GET /api/v1/auth/csrf` seeds the readable `XSRF-TOKEN` cookie; every
  state-changing request echoes it as the `X-XSRF-TOKEN` header, else `403 CSRF_INVALID`.
  See [ADR 0002](../decisions/adr-0002-csrf-double-submit-protection.md).
- **Ownership:** cross-owner access is indistinguishable `404 RESOURCE_NOT_FOUND`.
  See [ADR 0003](../decisions/adr-0003-ownership-based-authorization-and-resource-hiding.md).
- **Errors:** uniform RFC 7807 problems (`AUTH_REQUIRED`, `INVALID_CREDENTIALS`,
  `CSRF_INVALID`, `REGISTRATION_FAILED`, `PASSWORD_POLICY_VIOLATION`,
  `CONFIRMATION_REQUIRED`, `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED`).
- **Money/decimal strings:** amounts travel as decimal strings (`numeric(19,2)` scale);
  the frontend formats with `formatMoney` and never computes totals.

## Endpoints

### Auth — `AuthController` (`/api/v1/auth`)

| Method | Path | Request | Success |
|---|---|---|---|
| POST | `/register` | `{email, password}` (≥10 chars, letter+digit) | `201` + `Location: /api/v1/account/me`, body `AccountView`; auto sign-in |
| POST | `/login` | `{email, password}` | `200 LoginResponse{id,email}`; session-id rotation |
| POST | `/logout` | — (idempotent) | `204`; server-side invalidation |
| POST | `/logout-all` | — | `204`; kills all sessions of the owner |
| GET | `/csrf` | — | `200`; seeds `XSRF-TOKEN` |

### Account — `AccountController` (`/api/v1/account`)

| Method | Path | Request | Success |
|---|---|---|---|
| GET | `/me` | — | `200 AccountView{id,email,createdAt}` (session probe) |
| GET | `/export` | header `X-Reauth-Password` (missing→400, wrong→401) | `200 application/json` deterministic byte-identical bundle |
| POST | `/password` | `{currentPassword, newPassword}` | `204`; kills other sessions, keeps current |
| DELETE | `/` | `{confirmation:"DELETE", password}` + re-auth | `204`; hard delete + `invalidateAll` |

### Profile — `ProfileController`, `IncomeController`, `ExpenseController`

| Method | Path | Request | Success |
|---|---|---|---|
| GET | `/api/v1/profile` | — | `200 ProfileView` (server-calculated totals + provenance) |
| PUT | `/api/v1/profile` | `{currency CHAR(3), savingsAmount, emergencyFundAmount, dependentsCount}` | `200 ProfileView` |
| POST | `/api/v1/incomes` | `{amount, source}` | `201 IncomeView` |
| PUT | `/api/v1/incomes/{id}` | same DTO | `200` |
| DELETE | `/api/v1/incomes/{id}` | — | `204` |
| POST | `/api/v1/expenses` | `{amount, category, expenseType=FIXED\|VARIABLE}` | `201 ExpenseView` |
| PUT | `/api/v1/expenses/{id}` | same DTO | `200` |
| DELETE | `/api/v1/expenses/{id}` | — | `204` |

Missing profile record on line writes surfaces as `404 RESOURCE_NOT_FOUND`;
the UI answers with "save your basics first".

### Debts — `DebtController` (`/api/v1/debts`)

Request `DebtRequest`: `creditor` (≤200), `debtType` (6-value enum), optional
`originalPrincipal` (unknown ≠ 0), `outstandingBalance`, `annualInterestRate`,
`minimumPayment`, `plannedPayment`, `dueDay`.

| Method | Path | Success |
|---|---|---|
| GET | `/` | list `DebtView` |
| POST | `/` | `201 DebtView` |
| GET | `/{id}` | `200 DebtView` |
| GET | `/summary` | DTI + payoff projection + `blockedDebtCount` |
| GET | `/{id}/schedule` | amortization calendar (principal/interest/ending balance) |
| POST / DELETE | `/{id}/payment-mark` | manual completion marker (balance unchanged) |
| PUT | `/{id}` | `200` |
| DELETE | `/{id}` | `204` (soft archive, stays out of totals) |

### Goals — `GoalController` (`/api/v1/goals`)

Request `GoalRequest`: `name` (≤120), `goalType` (7-value enum), `targetAmount`,
`currentAmount`, optional `targetDate` (ISO `yyyy-MM-dd`), `priority` (≥1).

| Method | Path | Success |
|---|---|---|
| GET | `/?status=` | list, filterable `ACTIVE`/`COMPLETED` at the controller |
| POST | `/` | `201 GoalView` |
| GET | `/{id}` | `200 GoalView` |
| GET | `/{id}/capacity` | required monthly capacity vs available (`MEETS_REQUIRED`/`SHORTFALL`) |
| PUT | `/{id}` | `200` |
| DELETE | `/{id}` | `204` (archive; archived goals leave capacity) |

### Root

| Method | Path | Success |
|---|---|---|
| GET | `/` | public `{app:"Financial GPS",status:"ok"}` (also the backend health surface next to `/actuator/health`) |

## Not yet exposed

Roadmap/scenario endpoints (`specs/005`, `006`) and account export/delete have no
SPA client yet (server supports export/delete; the UI only calls `GET /account/me`).
`frontend/src/api/*.ts` (5 clients: `http/auth/debts/goals/profile`) maps 1:1 to the
table above — any new endpoint needs a client function plus a store plus an e2e step.

## Related documents

- [data-model.md](data-model.md) — rows behind the DTOs
- [security.md](security.md) — session, CSRF, ownership
- [c4-container.md](c4-container.md) — how traffic reaches these endpoints
