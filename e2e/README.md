# e2e — Playwright end-to-end suite (clean architecture)

Real browser journeys across the running stack:

```
Playwright ──▶ Vite dev (:4173) ── /api ──▶ Spring Boot (:8080) ──▶ PostgreSQL
```

Unit tests mock the API layer; these specs prove the running backend honours
the contract (cookies, CSRF handshake, session persistence, server-calculated
totals, guard redirects, logout).

## Layout — dependency rule: specs → pages → components → support

```
e2e/
  specs/         USER LAYER — user journeys only. No selectors, no page.,
                 no raw test data. Talks to page objects + typed test-data.
    auth.spec.ts       register → dashboard → guard → logout → login
    profile.spec.ts    basics + income + expense → server totals
  pages/         INTERFACE ADAPTERS — one class per route. Thin composers:
                 navigation + section components, no raw selectors.
    BasePage.ts        shared goto/expectUrl/expectText
    RegisterPage.ts    /register
    LoginPage.ts       /login
    DashboardPage.ts   /dashboard
    ProfilePage.ts     /profile (basics/income/expense/position sections)
    AccountPage.ts     /account
    components/        reusable section/component objects
      AvatarMenu.ts      banner avatar menu (Account / Log out)
      BasicsSection.ts   "Your financial basics" card
      IncomeSection.ts   "Money coming in" card
      ExpenseSection.ts  "Money going out" card
      PositionSection.ts "Your financial position" hero (totals only)
  support/       SHARED KERNEL — no Playwright assertions on app state:
    selectors.ts       the ONLY place with DOM hooks (testids, #ids, roles)
    test-data.ts       typed domain data (IncomeLine, ExpenseLine, totals)
    auth-flow.ts       registerFreshAccount() — fresh account per test
  fixtures/
    accounts.ts        unique email factory + valid password (007 policy)
```

Rules:

- specs import from `pages/` + `support/` only — never `page.locator`, never a selector string.
- pages/sections import selectors from `support/selectors.ts` — never hardcode a hook.
- money values come from `support/test-data.ts` (`IncomeSource`, `ExpenseCategory`, …).
- each test registers its own account (`registerFreshAccount`) — order-independent, parallel-safe.

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

Stable hooks in this order (all catalogued in `support/selectors.ts`):

1. `data-testid` (e.g. `add-income`, `basics-edit`, `income-item`)
2. `#id` form controls (`#register-email`, `#savings`, `#income-amount`, …)
3. Role + accessible name (`Sign in`, `Create account`, `Add expense`)
4. Text assertions on server-rendered totals only

