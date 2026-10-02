import { expect, type Page } from '@playwright/test'
import { BasePage } from './BasePage'

/**
 * /debts — Debt Management page: summary card, blocker alert, debt list, debt form.
 * Assertions read server-rendered text only; the suite never computes amortization.
 */
export class DebtsPage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  async open(): Promise<void> {
    await this.goto('/debts')
    await expect(this.page.getByRole('heading', { name: 'Quản lý nợ', exact: true })).toBeVisible()
  }

  async addDebt(input: { creditor: string; balance: string; min: string; planned: string }): Promise<void> {
    await this.page.getByRole('button', { name: 'Thêm khoản nợ', exact: true }).click()
    await this.page.getByTestId('creditor').fill(input.creditor)
    await this.page.locator('#debt-balance').fill(input.balance)
    await this.page.locator('#debt-min').fill(input.min)
    await this.page.locator('#debt-planned').fill(input.planned)
    await this.page.getByRole('button', { name: 'Lưu', exact: true }).click()
  }

  async expectTotalDebt(text: string): Promise<void> {
    await expect(this.page.getByTestId('total-debt')).toContainText(text)
  }

  async expectDti(text: string): Promise<void> {
    await expect(this.page.getByTestId('dti')).toContainText(text)
  }
}
