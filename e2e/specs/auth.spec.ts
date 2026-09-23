import { expect, test } from '@playwright/test'
import { AccountPage } from '../pages/AccountPage'
import { DashboardPage } from '../pages/DashboardPage'
import { LoginPage } from '../pages/LoginPage'
import { E2E_PASSWORD, registerFreshAccount } from '../support/auth-flow'
import { zeroTotals } from '../support/test-data'

/**
 * Auth journey — register → dashboard → guard → account → logout → login.
 * Every test registers its own fresh account via registerFreshAccount (no
 * shared emails, no cross-test sessions), so the suite is order-independent
 * and fullyParallel-safe. Browser-driven only, never fetch().
 */
test.describe('auth journey', () => {
  test('register creates a session and lands on the dashboard', async ({ page }) => {
    const { email } = await registerFreshAccount(page, 'auth-register')
    const dashboard = new DashboardPage(page)
    await dashboard.expectSignedInAs(email)
  })

  test('anonymous visit to /profile bounces to /login?redirect=', async ({ browser }) => {
    // A fresh context holds no session cookie — the requiresAuth guard redirects.
    const anonymous = await browser.newPage()
    try {
      await anonymous.goto('/profile')
      await expect(anonymous).toHaveURL(/\/login\?redirect=/)
    } finally {
      await anonymous.close()
    }
  })

  test('fresh account sees zeroed server totals', async ({ page }) => {
    const { email } = await registerFreshAccount(page, 'auth-totals')
    const dashboard = new DashboardPage(page)
    await dashboard.expectSignedInAs(email)
    await dashboard.expectTotals(zeroTotals)
  })

  test('login rejects a wrong password without a session', async ({ page }) => {
    const { email } = await registerFreshAccount(page, 'auth-wrongpass')
    // Drop the session: a clean login page with no cookies.
    await page.context().clearCookies()
    const login = new LoginPage(page)
    await login.open()
    await login.login(email, 'wrong password 9')
    // Still on /login with a server-mapped error, never bounced to /dashboard.
    await login.expectServerError()
  })

  test('login validates empty fields client-side', async ({ page }) => {
    const login = new LoginPage(page)
    await login.open()
    await login.login('', '')
    await login.expectValidationError('Email is required.')
  })

  test('logout via /account returns to login and the session is gone', async ({ page }) => {
    await registerFreshAccount(page, 'auth-logout')
    const account = new AccountPage(page)
    await account.openViaAvatarMenu()
    await account.logout()

    // Reloading a protected route after logout bounces back to login.
    await new DashboardPage(page).openProtectedAfterLogout()
  })

  test('login restores the session after logout', async ({ page }) => {
    const { email } = await registerFreshAccount(page, 'auth-relogin')
    const account = new AccountPage(page)
    await account.open()
    await account.logout()

    const login = new LoginPage(page)
    await login.open()
    await login.login(email, E2E_PASSWORD)
    const dashboard = new DashboardPage(page)
    await dashboard.expectSignedInAs(email)
  })
})

