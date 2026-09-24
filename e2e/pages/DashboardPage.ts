import { expect } from '@playwright/test'
import { BasePage } from './BasePage'
import { sel } from '../support/selectors'

/**
 * /dashboard — the signed-in home screen. Every total is server-calculated;
 * the suite only asserts the rendered text, never recomputes money.
 */
export class DashboardPage extends BasePage {
  async open(): Promise<void> {
    await this.goto('/dashboard')
  }

  async expectSignedInAs(email: string): Promise<void> {
    await this.expectText(sel.dashboard.heading)
    await this.expectText(email)
  }

  async openProfile(): Promise<void> {
    await this.page
      .getByRole(sel.dashboard.openProfileLink.role, { name: sel.dashboard.openProfileLink.name })
      .click()
    await this.expectUrl('/profile')
  }

  async openProtectedAfterLogout(): Promise<void> {
    // Guard check: a sessionless visit to /profile must bounce to /login.
    await this.goto('/profile')
    await expect(this.page).toHaveURL(/\/login\?redirect=/)
  }

  async expectTotals(params: { income: string; expenses: string; netCashFlow: string }): Promise<void> {
    const main = this.page.locator('main')
    await expect(main.getByText('Monthly income')).toBeVisible()
    await expect(main.getByText(params.income).first()).toBeVisible()
    await expect(main.getByText(params.expenses).first()).toBeVisible()
    await expect(main.getByText(params.netCashFlow).first()).toBeVisible()
  }
}

