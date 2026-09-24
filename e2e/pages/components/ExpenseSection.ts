import { expect, type Page } from '@playwright/test'
import { sel } from '../../support/selectors'
import type { ExpenseLine } from '../../support/test-data'

/**
 * "Money going out" card on /profile — add form + expense rows.
 * Owns the add-expense testid and #expense-* controls.
 */
export class ExpenseSection {
  constructor(private readonly page: Page) {}

  async add(line: ExpenseLine): Promise<void> {
    await this.page.getByTestId(sel.expense.add).first().click()
    await this.page.locator(sel.expense.amount).fill(line.amount)
    await this.page.locator(sel.expense.category).selectOption(line.category)
    if (line.type) {
      await this.page.locator(sel.expense.type).selectOption(line.type)
    }
    await this.page.getByRole(sel.expense.submit.role, { name: sel.expense.submit.name, exact: true }).click()
  }

  async expectRow(category: string): Promise<void> {
    await expect(this.page.getByTestId(sel.expense.row).filter({ hasText: category }).first()).toBeVisible()
  }

  async expectCategoryOffered(label: string): Promise<void> {
    await this.page.getByTestId(sel.expense.add).first().click()
    await expect(this.page.locator(sel.expense.category).locator(`option:text("${label}")`)).toHaveCount(1)
    await this.page.getByRole(sel.expense.cancel.role, { name: sel.expense.cancel.name }).first().click()
  }
}
