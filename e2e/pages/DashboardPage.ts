import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

/**
 * /dashboard — the signed-in home screen. Every total is server-calculated;
 * the suite only asserts the rendered text, never recomputes money.
 */
export class DashboardPage extends BasePage {
  async open(): Promise<void> {
    await this.goto('/dashboard')
  }

  async expectSignedInAs(email: string): Promise<void> {
    await this.expectText('Welcome back')
    await this.expectText(email)
  }

  async openProfile(): Promise<void> {
    await this.page.getByRole('link', { name: 'Open financial profile' }).click()
    await this.expectUrl('/profile')
  }

  async expectTotals(params: { income: string; expenses: string; netCashFlow: string }): Promise<void> {
    const main = this.page.locator('main')
    await expect(main.getByText('Monthly income')).toBeVisible()
    await expect(main.getByText(params.income).first()).toBeVisible()
    await expect(main.getByText(params.expenses).first()).toBeVisible()
    await expect(main.getByText(params.netCashFlow).first()).toBeVisible()
  }
}
