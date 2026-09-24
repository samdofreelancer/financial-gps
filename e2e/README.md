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
  scripts/
    run-with-allure.mjs  `npm test` — Playwright run + Allure report
```

Rules:

- specs import from `pages/` + `support/` only — never `page.locator`, never a selector string.
- pages/sections import selectors from `support/selectors.ts` — never hardcode a hook.
- money values come from `support/test-data.ts` (`IncomeSource`, `ExpenseCategory`, …).
- each test registers its own account (`registerFreshAccount`) — order-independent, parallel-safe.

## Run

Two ways in, same suite:

```bash
# 1. stack in compose, suite on the host (fast loop; needs Node + Java locally)
docker compose up -d --wait   # backend + frontend + postgres (waits for the backend healthcheck)
cd e2e && npm install
npx playwright install chromium
npm test                      # suite + Allure report (./allure-report)

# 2. stack *and* suite in compose — one command, and the CI entry point
docker compose --profile e2e up --abort-on-container-exit --exit-code-from e2e
```

The `e2e` service (profile `e2e`) runs `npm ci && npm test` in the Playwright image
against `http://frontend:4173`, so it needs no host toolchain, and it writes its results
into the bind-mounted `./e2e` exactly like the host flow. That image ships no JRE, so
inside the container the suite runs but the Allure HTML report is skipped (the wrapper
warns and keeps Playwright's exit code) — raw `allure-results` are still written and
`npm run allure:generate` renders them on any host with Java.

Override the SPA origin: `E2E_BASE_URL=http://127.0.0.1:4173 npm test`
Override the email domain: `E2E_EMAIL_DOMAIN=example.com npm test`

| script | what it does |
| --- | --- |
| `npm test` | Playwright run, then always renders the Allure report — failures included. Flags are forwarded: `npm test -- --grep auth --headed` |
| `npm run test:only` | raw `playwright test` (fast loop, only refreshes `allure-results`) |
| `npm run test:headed` / `test:debug` | exploratory runs, no report |
| `npm run allure:generate` | re-render `allure-results` → `allure-report` |
| `npm run allure:open` / `allure:serve` | open the generated report / serve live results |

## CI (`.github/workflows/ci.yml` → job `e2e`)

The suite runs only when it can be affected: the `changes` job filters paths and the
`e2e` job is skipped unless `backend/**`, `frontend/**`, `e2e/**`, `compose.yaml` or the
workflow file moved (a job skipped by `if` reports "Success", so required checks stay
green).

The job re-declares nothing — it runs the compose command above, unchanged:

```bash
docker compose --profile e2e up --abort-on-container-exit --exit-code-from e2e
```

so `compose.yaml` is the single source of truth for the stack *and* the suite, in dev and
in CI: one file to edit when a port, healthcheck, service or Playwright version moves. The
runner installs no Node, Java or browser — the Playwright image already carries them — and
supplies only `DB_PASSWORD` (`ci-e2e-password`, ephemeral); every container reads the rest
from compose. Attached mode streams each container's log into the job output,
`--exit-code-from e2e` makes the step adopt the suite's exit code, and
`--abort-on-container-exit` stops the stack as soon as it is over. The Allure report, its
raw results and the failure traces are uploaded as the `e2e-report` artifact on every run
(`if-no-files-found: ignore`, because the JRE-less image skips the HTML render), and the
stack is always released with `docker compose --profile e2e down -v` afterwards.

## Reports (Allure)

Every run writes Allure results to `e2e/allure-results` and `npm test` renders
them into `e2e/allure-report` (open `allure-report/index.html`, or
`npm run allure:open`). Both folders are gitignored build output.

What the report carries:

- suite tree from the spec files + `describe` titles (`auth.spec.ts > auth journey`);
- every Playwright API step as an Allure step (`detail: true`);
- screenshot + trace of each failure as attachment, so a red run is debuggable from the report alone;
- an Environment widget (base URL, email domain, browser, Node, platform) so runs are told apart.

Notes:

- results are cleared at the start of `npm test` — one run, one report, no leftovers from an earlier run;
- the report is rendered even when tests fail (that is why `npm test` wraps Playwright instead of chaining `&&`), and Playwright's exit code is preserved for CI;
- rendering needs the **Allure CLI, bundled as the `allure-commandline` dev dependency, which runs on Java 8+** (the backend stack already requires Java 21). Without Java the tests still run and a warning explains why no report appeared;
- `ALLURE_RESULTS_DIR` (read by `allure-playwright`) can point the results elsewhere when needed.

## Selectors

Stable hooks in this order (all catalogued in `support/selectors.ts`):

1. `data-testid` (e.g. `add-income`, `basics-edit`, `income-item`)
2. `#id` form controls (`#register-email`, `#savings`, `#income-amount`, …)
3. Role + accessible name (`Sign in`, `Create account`, `Add expense`)
4. Text assertions on server-rendered totals only

