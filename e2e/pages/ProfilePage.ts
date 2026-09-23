import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

/**
 * /profile — Financial GPS orchestrator page, verified against the live DOM (2026-09-23):
 *
 * - basics form:   [data-testid="basics-edit"] → #savings, #emergency, #dependents → "Save basics"
 * - income form:   [data-testid="add-income"] → #income-amount, #income-source → "Add income"
 * - expense form:  [data-testid="add-expense"] → #expense-amount, #expense-category,
 *                  #expense-type → "Add expense"
 * - rows:          [data-testid="income-item"], [data-testid="expense-item"]
 * - position hero: [data-testid="position-card"] with Income / Expenses / Free cash
 *
 * Amounts are typed in the Vietnamese presentation (e.g. 30.000.000 for 30M);
 * assertions read the server-rendered totals only.
 */
export class ProfilePage extends BasePage {
  async open(): Promise<void> {
    await this.goto('/profile')
    await expect(this.page.getByRole('heading', { name: 'Financial GPS', exact: true })).toBeVisible()
  }

  // -- financial basics -------------------------------------------------------

  async saveBasics(params: { savings: string; emergency: string; dependents: string }): Promise<void> {
    await this.page.getByTestId('basics-edit').click()
    await this.page.locator('#savings').fill(params.savings)
    await this.page.locator('#emergency').fill(params.emergency)
    await this.page.locator('#dependents').fill(params.dependents)
    await this.page.getByRole('button', { name: 'Save basics' }).click()
  }

  async expectBasics(params: { savings: string; emergency: string; dependents: string }): Promise<void> {
    const card = this.page.getByTestId('basics-card')
    await expect(card.getByText(params.savings).first()).toBeVisible()
    await expect(card.getByText(params.emergency).first()).toBeVisible()
    await expect(card.getByText(params.dependents).first()).toBeVisible()
  }

  // -- income -----------------------------------------------------------------

  async addIncome(params: { amount: string; source: string }): Promise<void> {
    await this.page.getByTestId('add-income').first().click()
    await this.page.locator('#income-amount').fill(params.amount)
    await this.page.locator('#income-source').selectOption(params.source)
    await this.page.getByRole('button', { name: 'Add income', exact: true }).click()
  }

  async expectIncomeRow(sourceValue: string): Promise<void> {
    await expect(this.page.getByTestId('income-item').filter({ hasText: sourceValue }).first()).toBeVisible()
  }

  // -- expense ----------------------------------------------------------------

  async addExpense(params: { amount: string; category: string; type?: 'FIXED' | 'VARIABLE' }): Promise<void> {
    await this.page.getByTestId('add-expense').first().click()
    await this.page.locator('#expense-amount').fill(params.amount)
    await this.page.locator('#expense-category').selectOption(params.category)
    if (params.type) {
      await this.page.locator('#expense-type').selectOption(params.type)
    }
    await this.page.getByRole('button', { name: 'Add expense', exact: true }).click()
  }

  async expectExpenseRow(categoryValue: string): Promise<void> {
    await expect(this.page.getByTestId('expense-item').filter({ hasText: categoryValue }).first()).toBeVisible()
  }

  async expectExpenseCategoryOption(label: string): Promise<void> {
    // The dropdown must offer the category (e.g. "Quỹ nuôi con / childcare").
    await this.page.getByTestId('add-expense').first().click()
    await expect(this.page.locator('#expense-category').locator(`option:text("${label}")`)).toHaveCount(1)
    await this.page.getByRole('button', { name: 'Cancel' }).first().click()
  }

  // -- server totals ------------------------------------------------------------

  async expectPosition(params: { income: string; expenses: string; freeCash: string }): Promise<void> {
    const card = this.page.getByTestId('position-card')
    // Term cells are unique (dt); the provenance <details> also mentions the
    // labels, so scope every assertion to the stats row to stay strict-safe.
    const stats = card.locator('.stats')
    await expect(stats.getByText('Income', { exact: true })).toBeVisible()
    await expect(stats.getByText(params.income).first()).toBeVisible()
    await expect(stats.getByText(params.expenses).first()).toBeVisible()
    await expect(stats.getByText(params.freeCash).first()).toBeVisible()
  }
}
