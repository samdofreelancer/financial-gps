# e2e — Playwright end-to-end suite (Page Object Model)

Real browser journeys across the running stack:

```
Playwright ──▶ Vite dev (:4173) ── /api ──▶ Spring Boot (:8080) ──▶ PostgreSQL
```

Unit tests mock the API layer; these specs prove the running backend honours
the contract (cookies, CSRF handshake, session persistence, server-calculated
totals, guard redirects, logout).

## Layout

```
e2e/
  pages/       Page objects — one class per route, Playwright wrapped inside.
               Specs never touch `page.` directly except via the object.
    BasePage.ts        shared goto/expect helpers
    RegisterPage.ts    /register  (#register-email, #register-password)
    LoginPage.ts       /login     (#email, #password)
    DashboardPage.ts   /dashboard (PositionSummary totals)
    ProfilePage.ts     /profile   (basics + income/expense forms)
    AccountPage.ts     /account   (Log out)
  fixtures/
    accounts.ts        unique email factory + valid password (007 policy)
  specs/
    auth.spec.ts       register → dashboard → guard → logout → login
    profile.spec.ts    basics + income + expense → server totals
```

## Run

```bash
docker compose up            # backend + frontend + postgres
cd e2e && npm install
npx playwright install chromium
npm test
```

Override the SPA origin: `E2E_BASE_URL=http://127.0.0.1:4173 npm test`
Override the email domain: `E2E_EMAIL_DOMAIN=example.com npm test`

## Selectors

Specs prefer stable hooks in this order:

1. `data-testid` (e.g. `add-income`, `basics-edit`, `income-item`)
2. `#id` form controls (`#register-email`, `#savings`, `#income-amount`, …)
3. Role + accessible name (`Sign in`, `Create account`, `Add expense`)
4. Text assertions on server-rendered totals only

The suite runs serially (`workers: 1`) with a unique email per spec so
parallel accounts never share a session.
