import { expect, test } from '@playwright/test'
import { E2E_PASSWORD, uniqueEmail } from '../fixtures/accounts'
import { AccountPage } from '../pages/AccountPage'
import { DashboardPage } from '../pages/DashboardPage'
import { LoginPage } from '../pages/LoginPage'
import { RegisterPage } from '../pages/RegisterPage'

/**
 * Auth journey — register → dashboard → guard → account → logout → login.
 * Ports the coverage of the legacy `frontend/e2e/session-profile-journey.mjs`
 * (session cookie, fixation rotation, logout invalidation, 401 reload) to a
 * real browser: every assertion below drives the SPA, never fetch().
 */
test.describe.serial('auth journey', () => {
  const email = uniqueEmail('auth')

  test('register creates a session and lands on the dashboard', async ({ page }) => {
    const register = new RegisterPage(page)
    await register.open()
    await register.register(email, E2E_PASSWORD)
    await register.expectOnDashboard()
  })

  test('anonymous visit to /profile bounces to /login?redirect=/profile', async ({ browser }) => {
    // A fresh context holds no session cookie — the requiresAuth guard redirects.
    const anonymous = await browser.newPage()
    try {
      await anonymous.goto('/profile')
      await expect(anonymous).toHaveURL(/\/login\?redirect=/)
    } finally {
      await anonymous.close()
    }
  })

  test('dashboard shows the signed-in account with zeroed server totals', async ({ page }) => {
    // Same storage state is NOT shared across tests: sign in again explicitly.
    const login = new LoginPage(page)
    await login.open()
    await login.login(email, E2E_PASSWORD)

    const dashboard = new DashboardPage(page)
    await dashboard.expectSignedInAs(email)
    await dashboard.expectTotals({ income: '0,00 VND', expenses: '0,00 VND', netCashFlow: '0,00 VND' })
  })

  test('login rejects a wrong password without a session', async ({ page }) => {
    const login = new LoginPage(page)
    await login.open()
    await login.login(email, 'wrong password 9')
    // Still on /login with a server-mapped error, never bounced to /dashboard.
    await expect(page).toHaveURL('/login')
    await expect(page.getByRole('alert')).toBeVisible()
  })

  test('login validates empty fields client-side', async ({ page }) => {
    const login = new LoginPage(page)
    await login.open()
    await login.login('', '')
    await login.expectValidationError('Email is required.')
  })

  test('logout via /account returns to login and the session is gone', async ({ page }) => {
    const login = new LoginPage(page)
    await login.open()
    await login.login(email, E2E_PASSWORD)

    const account = new AccountPage(page)
    await account.openViaAvatarMenu()
    await account.expectSignedInAs(email)
    await account.logout()

    // Reloading a protected route after logout bounces back to login.
    await page.goto('/profile')
    await expect(page).toHaveURL(/\/login\?redirect=/)
  })

  test('login restores the session after logout', async ({ page }) => {
    const login = new LoginPage(page)
    await login.open()
    await login.login(email, E2E_PASSWORD)

    const dashboard = new DashboardPage(page)
    await dashboard.expectSignedInAs(email)
  })
})
