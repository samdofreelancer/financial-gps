import { expect, type Page } from '@playwright/test'
import { sel } from '../../support/selectors'
import type { IncomeLine } from '../../support/test-data'

/**
 * "Money coming in" card on /profile — add form + income rows.
 * Owns the add-income testid and #income-* controls.
 */
export class IncomeSection {
  constructor(private readonly page: Page) {}

  async add(line: IncomeLine): Promise<void> {
    await this.page.getByTestId(sel.income.add).first().click()
    await this.page.locator(sel.income.amount).fill(line.amount)
    await this.page.locator(sel.income.source).selectOption(line.source)
    await this.page.getByRole(sel.income.submit.role, { name: sel.income.submit.name, exact: true }).click()
  }

  async expectRow(source: string): Promise<void> {
    await expect(this.page.getByTestId(sel.income.row).filter({ hasText: source }).first()).toBeVisible()
  }
}
