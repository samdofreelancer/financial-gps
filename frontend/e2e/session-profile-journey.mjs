#!/usr/bin/env node
/**
 * Real end-to-end journey across every layer the user actually touches:
 *
 *   this script ── HTTP ──▶ Vite dev server (:4173) ── /api proxy ──▶ Spring Boot (:8080)
 *                                                                      └─▶ PostgreSQL
 *
 * Why this exists: the unit tests mock `authApi`, so they prove the SPA *calls* the contract but
 * never that the running backend honours it (cookies, CSRF handshake, session persistence,
 * server-calculated totals, 401 after logout). This script drives the real stack over real
 * cookies and fails the build on the first contract mismatch.
 *
 * Requirement: backend + frontend are running (docker compose up). Override the SPA origin with
 * E2E_BASE_URL when the SPA runs somewhere else.
 *
 * Usage: npm run test:e2e        (from frontend/)
 */
const BASE = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const EMAIL_DOMAIN = process.env.E2E_EMAIL_DOMAIN ?? 'example.com'
const PASSWORD = 'e2e journey pass1' // satisfies the 007 policy: >= 10 chars, letter + digit

/** Minimal cookie jar — the SPA relies on cookies, so the script must too. */
class Jar {
  #cookies = new Map()

  get(name) {
    return this.#cookies.get(name)
  }

  has(name) {
    return this.#cookies.has(name)
  }

  cookieHeader() {
    return [...this.#cookies].map(([name, value]) => `${name}=${value}`).join('; ')
  }

  absorb(response) {
    for (const line of rawSetCookies(response)) {
      const [pair = '', ...attributes] = line.split(';')
      const separator = pair.indexOf('=')
      if (separator < 0) {
        continue
      }
      const name = pair.slice(0, separator).trim()
      const value = pair.slice(separator + 1).trim()
      const cleared = value === '' || attributes.some((a) => /^\s*max-age\s*=\s*0\s*$/i.test(a))
      if (cleared) {
        this.#cookies.delete(name)
      } else {
        this.#cookies.set(name, value)
      }
    }
    return this
  }
}

function rawSetCookies(response) {
  if (typeof response.headers.getSetCookie !== 'function') {
    throw new Error('this script needs Node 20+ (fetch Set-Cookie access)')
  }
  return response.headers.getSetCookie()
}

const badStatuses = []
let requestCount = 0

async function call(jar, method, path, options = {}) {
  const headers = { accept: 'application/json' }
  const cookies = jar.cookieHeader()
  if (cookies) {
    headers.cookie = cookies
  }
  if (options.csrf) {
    const token = jar.get('XSRF-TOKEN')
    if (!token) {
      throw new Error(`no XSRF-TOKEN cookie before ${method} ${path} — the CSRF handshake is broken`)
    }
    headers['x-xsrf-token'] = token
  }
  if (options.body !== undefined) {
    headers['content-type'] = 'application/json'
  }

  const response = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    redirect: 'manual',
  })
  jar.absorb(response)
  requestCount += 1
  if (response.status >= 500) {
    badStatuses.push(`${method} ${path} → ${response.status}`)
  }

  const text = await response.text()
  let json = null
  try {
    json = text ? JSON.parse(text) : null
  } catch {
    json = null
  }
  return { status: response.status, json, text, setCookies: rawSetCookies(response) }
}

const results = []

function record(name, ok, detail = '') {
  results.push({ name, ok, detail })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? ` — ${detail}` : ''}`)
}

function uniqueEmail() {
  return `e2e-${Date.now()}-${Math.random().toString(16).slice(2, 8)}@${EMAIL_DOMAIN}`
}

async function main() {
  const email = uniqueEmail()

  // ---------------------------------------------------------------- 1. anonymous baseline
  const anonymous = new Jar()
  await call(anonymous, 'GET', '/api/v1/auth/csrf')
  const beforeLogin = await call(anonymous, 'GET', '/api/v1/account/me')
  record(
    'anonymous GET /account/me is 401 AUTH_REQUIRED (reload before login → login screen)',
    beforeLogin.status === 401 && beforeLogin.json?.code === 'AUTH_REQUIRED',
    `status=${beforeLogin.status} code=${beforeLogin.json?.code}`,
  )

  // ---------------------------------------------------------------- 2. register
  const owner = new Jar()
  const warmUp = await call(owner, 'GET', '/api/v1/auth/csrf')
  const registered = await call(owner, 'POST', '/api/v1/auth/register', {
    body: { email, password: PASSWORD },
    csrf: true,
  })
  const sessionCookie = registered.setCookies.find((line) => line.startsWith('SESSION=')) ?? ''
  record(
    'register → 201 with the account view',
    registered.status === 201 && registered.json?.email === email && Boolean(registered.json?.id),
    `status=${registered.status} email=${registered.json?.email}`,
  )
  record(
    'register issues an HttpOnly SESSION cookie (Spring Session JDBC)',
    Boolean(sessionCookie) && /httponly/i.test(sessionCookie) && owner.has('SESSION'),
    sessionCookie.replace(/SESSION=[^;]*/, 'SESSION=<redacted>').trim(),
  )
  record(
    'register body never leaks the password hash',
    registered.text !== '' && !/password/i.test(registered.text),
    `keys=${Object.keys(registered.json ?? {}).join(',')}`,
  )
  record(
    'CSRF warm-up seeded the readable XSRF-TOKEN cookie',
    warmUp.status === 200 && Boolean(warmUp.setCookies.find((line) => line.startsWith('XSRF-TOKEN='))),
    `status=${warmUp.status}`,
  )

  // ---------------------------------------------------------------- 3. reload with the session
  const afterReload = await call(owner, 'GET', '/api/v1/account/me')
  record(
    'reload (GET /account/me with the SESSION cookie) → still authenticated',
    afterReload.status === 200 && afterReload.json?.email === email,
    `status=${afterReload.status} email=${afterReload.json?.email}`,
  )

  // ---------------------------------------------------------------- 4. duplicate email
  const stranger = new Jar()
  await call(stranger, 'GET', '/api/v1/auth/csrf')
  const duplicate = await call(stranger, 'POST', '/api/v1/auth/register', {
    body: { email, password: PASSWORD },
    csrf: true,
  })
  record(
    'duplicate email → 409 REGISTRATION_FAILED with a generic body',
    duplicate.status === 409 &&
      duplicate.json?.code === 'REGISTRATION_FAILED' &&
      !/exists|taken|duplicate/i.test(duplicate.text),
    `status=${duplicate.status} detail="${duplicate.json?.detail}"`,
  )

  // ---------------------------------------------------------------- 5. concurrent duplicate
  const racers = [new Jar(), new Jar()]
  for (const jar of racers) {
    await call(jar, 'GET', '/api/v1/auth/csrf')
  }
  const raceEmail = uniqueEmail()
  const raceResults = await Promise.all(
    racers.map((jar) =>
      call(jar, 'POST', '/api/v1/auth/register', {
        body: { email: raceEmail, password: PASSWORD },
        csrf: true,
      }),
    ),
  )
  const raceStatuses = raceResults.map((result) => result.status).sort()
  const raceWinners = raceStatuses.filter((status) => status === 201).length
  const racedLoser = raceResults.find((result) => result.status === 409)
  record(
    'two simultaneous registrations on one email → 201/409 only, never 5xx',
    raceStatuses.every((status) => status === 201 || status === 409) && raceWinners <= 1,
    `statuses=[${raceStatuses.join(',')}]`,
  )
  record(
    'a lost concurrent registration answers the documented conflict code',
    !racedLoser || racedLoser.json?.code === 'REGISTRATION_FAILED',
    racedLoser ? `code=${racedLoser.json?.code}` : 'no collision this run (headers let them serialize)',
  )

  // ---------------------------------------------------------------- 6. profile + mutations
  const emptyProfile = await call(owner, 'GET', '/api/v1/profile')
  record(
    'GET /profile → 200 with server-provided totals',
    emptyProfile.status === 200 && typeof emptyProfile.json?.totalIncome?.amount === 'string',
    `status=${emptyProfile.status} totalIncome=${emptyProfile.json?.totalIncome?.amount} provenance=${emptyProfile.json?.totalIncome?.provenance}`,
  )

  const savedProfile = await call(owner, 'PUT', '/api/v1/profile', {
    body: {
      currency: 'VND',
      savingsAmount: '100.00',
      emergencyFundAmount: '50.00',
      dependentsCount: 2,
    },
    csrf: true,
  })
  record(
    'PUT /profile stores the facts and echoes them as decimal strings',
    savedProfile.status === 200 &&
      savedProfile.json?.savingsAmount === '100.00' &&
      savedProfile.json?.emergencyFundAmount === '50.00' &&
      savedProfile.json?.dependentsCount === 2,
    `status=${savedProfile.status} savings=${savedProfile.json?.savingsAmount} dependents=${savedProfile.json?.dependentsCount}`,
  )

  const income = await call(owner, 'POST', '/api/v1/incomes', {
    body: { amount: '74.00', source: 'salary' },
    csrf: true,
  })
  record(
    'POST /incomes → 201',
    income.status === 201 && income.json?.amount === '74.00',
    `status=${income.status} amount=${income.json?.amount}`,
  )

  const expense = await call(owner, 'POST', '/api/v1/expenses', {
    body: { amount: '30.00', category: 'rent', expenseType: 'FIXED' },
    csrf: true,
  })
  record(
    'POST /expenses → 201',
    expense.status === 201 && expense.json?.amount === '30.00',
    `status=${expense.status} amount=${expense.json?.amount}`,
  )

  const computed = await call(owner, 'GET', '/api/v1/profile')
  record(
    'the server recalculates the position from the stored lines (no client math)',
    computed.status === 200 &&
      computed.json?.totalIncome?.amount === '74.00' &&
      computed.json?.totalExpenses?.amount === '30.00' &&
      computed.json?.netCashFlow?.amount === '44.00' &&
      computed.json?.availableCapacity?.amount === '44.00',
    `income=${computed.json?.totalIncome?.amount} expenses=${computed.json?.totalExpenses?.amount} net=${computed.json?.netCashFlow?.amount} capacity=${computed.json?.availableCapacity?.amount}`,
  )
  record(
    'every total is marked as calculated by the server',
    computed.json?.totalIncome?.provenance === 'calculated' &&
      computed.json?.netCashFlow?.provenance === 'calculated',
    `totalIncome.provenance=${computed.json?.totalIncome?.provenance}`,
  )

  // ---------------------------------------------------------------- 7. export
  const exported = await call(owner, 'GET', '/api/v1/account/export')
  record(
    'GET /account/export → 200 bundle containing the owner data',
    exported.status === 200 &&
      exported.json?.formatVersion === 1 &&
      exported.json?.account?.email === email &&
      exported.text.includes('74.00'),
    `status=${exported.status} formatVersion=${exported.json?.formatVersion}`,
  )

  // ---------------------------------------------------------------- 8. logout + reload
  const loggedOut = await call(owner, 'POST', '/api/v1/auth/logout', { csrf: true })
  record(
    'logout → 204 (server-side session terminated)',
    loggedOut.status === 204,
    `status=${loggedOut.status}`,
  )

  const afterLogout = await call(owner, 'GET', '/api/v1/account/me')
  record(
    'reload after logout → 401 again (the cookie alone is not identity)',
    afterLogout.status === 401 && afterLogout.json?.code === 'AUTH_REQUIRED',
    `status=${afterLogout.status} code=${afterLogout.json?.code}`,
  )

  // ---------------------------------------------------------------- 9. login + persistence
  await call(owner, 'GET', '/api/v1/auth/csrf')
  const loggedIn = await call(owner, 'POST', '/api/v1/auth/login', {
    body: { email, password: PASSWORD },
    csrf: true,
  })
  record(
    'login → 200 with the account view',
    loggedIn.status === 200 && loggedIn.json?.email === email,
    `status=${loggedIn.status} email=${loggedIn.json?.email}`,
  )
  const rotatedSession = loggedIn.setCookies.find((line) => line.startsWith('SESSION=')) ?? ''
  const firstSessionId = sessionCookie.replace(/^SESSION=/, '').split(';')[0]
  record(
    'login rotates the session id (fixation defense)',
    Boolean(rotatedSession) && owner.get('SESSION') !== firstSessionId,
    `rotated=${Boolean(rotatedSession)}`,
  )

  const afterLogin = await call(owner, 'GET', '/api/v1/profile')
  record(
    'the profile and its lines survived logout/login (persisted state)',
    afterLogin.status === 200 &&
      afterLogin.json?.totalIncome?.amount === '74.00' &&
      afterLogin.json?.incomes?.length === 1,
    `incomes=${afterLogin.json?.incomes?.length} totalIncome=${afterLogin.json?.totalIncome?.amount}`,
  )

  // ---------------------------------------------------------------- 10. isolation + summary
  const isolation = await call(stranger, 'GET', '/api/v1/account/me')
  record(
    'a different browser (rejected registration) never holds a session',
    isolation.status === 401,
    `status=${isolation.status}`,
  )

  record(
    'no request in the whole journey answered 5xx',
    badStatuses.length === 0,
    badStatuses.length ? badStatuses.join('; ') : `${requestCount} requests checked`,
  )

  const failed = results.filter((result) => !result.ok)
  console.log(
    `\n${results.length - failed.length}/${results.length} checks passed across ${requestCount} HTTP requests (${BASE}).`,
  )
  if (failed.length) {
    console.error(`\nFailed checks:\n${failed.map((f) => ` - ${f.name} (${f.detail})`).join('\n')}`)
    process.exitCode = 1
  }
}

main().catch((error) => {
  console.error(`\nE2E journey aborted: ${error.message}`)
  if (error.cause) {
    console.error(`Cause: ${error.cause.message ?? error.cause}`)
  }
  console.error(`Is the stack running? (docker compose up) — tried ${BASE}`)
  console.error('Override the SPA origin with E2E_BASE_URL (e.g. http://127.0.0.1:4173).')
  process.exitCode = 1
})
