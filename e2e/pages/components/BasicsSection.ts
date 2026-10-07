import { expect, type Page } from '@playwright/test'
import { sel } from '../../support/selectors'
import type { FinancialBasics } from '../../support/test-data'

/**
 * "Your financial basics" card on /profile — facts + edit form.
 * Owns the basics-card testids and #savings/#emergency/#dependents inputs.
 */
export class BasicsSection {
  constructor(private readonly page: Page) {}

  async save(basics: FinancialBasics): Promise<void> {
    await this.page.getByTestId(sel.basics.edit).click()
    await this.page.locator(sel.basics.savings).fill(basics.savings)
    await this.page.locator(sel.basics.emergency).fill(basics.emergency)
    await this.page.locator(sel.basics.dependents).fill(basics.dependents)
    // The PUT creates the profile the income POST requires — don't return
    // until the server confirms, otherwise the next POST can 404.
    const saved = this.page.waitForResponse(
      (res) => res.url().includes('/api/v1/profile') && res.request().method() === 'PUT',
    )
    await this.page.getByRole(sel.basics.submit.role, { name: sel.basics.submit.name }).click()
    await saved
  }

  async expectShown(basics: FinancialBasics): Promise<void> {
    const card = this.page.getByTestId(sel.basics.card)
    await expect(card.getByText(basics.savings).first()).toBeVisible()
    await expect(card.getByText(basics.emergency).first()).toBeVisible()
    await expect(card.getByText(basics.dependents).first()).toBeVisible()
  }
}
