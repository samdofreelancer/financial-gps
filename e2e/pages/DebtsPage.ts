import { expect, type Page } from '@playwright/test'
import { BasePage } from './BasePage'
import { sel } from '../support/selectors'
import type { DebtInput } from '../support/test-data'

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
    await expect(
      this.page.getByRole(sel.debts.heading.role, { name: sel.debts.heading.name, exact: true }),
    ).toBeVisible()
  }

  /** Add a debt through the form. The rate is left blank when omitted (missing ≠ 0%). */
  async addDebt(input: DebtInput): Promise<void> {
    await this.page.getByRole(sel.debts.add.role, { name: sel.debts.add.name, exact: true }).click()
    await this.page.getByTestId(sel.debts.creditor).fill(input.creditor)
    await this.page.getByTestId(sel.debts.type).selectOption(input.type ?? 'CREDIT_CARD')
    await this.page.locator(sel.debts.balance).fill(input.balance)
    await this.page.locator(sel.debts.min).fill(input.min)
    await this.page.locator(sel.debts.planned).fill(input.planned)
    if (input.rate) {
      await this.page.getByTestId(sel.debts.rate).fill(input.rate)
    }
    await this.page.getByRole(sel.debts.save.role, { name: sel.debts.save.name, exact: true }).click()
  }

  private row(creditor: string) {
    return this.page.getByTestId(sel.debts.item).filter({ hasText: creditor }).first()
  }

  /** Edit an existing debt's planned payment (and optionally its rate) from its row. */
  async editPlanned(creditor: string, planned: string, rate?: string): Promise<void> {
    await this.row(creditor).getByRole(sel.debts.edit.role, { name: sel.debts.edit.name, exact: true }).click()
    await this.page.locator(sel.debts.planned).fill(planned)
    if (rate) {
      await this.page.getByTestId(sel.debts.rate).fill(rate)
    }
    await this.page.getByRole(sel.debts.save.role, { name: sel.debts.save.name, exact: true }).click()
  }

  /** Soft-delete (archive) a debt from its row. Confirm the dialog, then wait for the row to go. */
  async archiveDebt(creditor: string): Promise<void> {
    await this.row(creditor)
      .getByRole(sel.debts.remove.role, { name: sel.debts.remove.name, exact: true })
      .click()
    const removed = this.page.waitForResponse(
      (res) => res.url().includes('/api/v1/debts') && res.request().method() === 'DELETE',
    )
    await this.page.getByRole('button', { name: 'Xóa khoản nợ', exact: true }).click()
    await removed.catch(() => undefined)
    await expect(this.row(creditor)).toHaveCount(0)
  }

  async expectTotalDebt(text: string): Promise<void> {
    await expect(this.page.getByTestId(sel.debts.totalDebt)).toContainText(text)
  }

  async expectTotalMinimum(text: string): Promise<void> {
    await expect(this.page.getByTestId(sel.debts.totalMinimum)).toContainText(text)
  }

  async expectDti(text: string): Promise<void> {
    await expect(this.page.getByTestId(sel.debts.dti)).toContainText(text)
  }

  async expectPayoffDate(text: string): Promise<void> {
    await expect(this.page.getByTestId(sel.debts.payoffDate)).toContainText(text)
  }

  async expectBlocked(text: string): Promise<void> {
    const alert = this.page.getByTestId(sel.debts.blockerAlert)
    await expect(alert).toBeVisible()
    await expect(alert).toContainText(text)
  }

  async expectNotBlocked(): Promise<void> {
    await expect(this.page.getByTestId(sel.debts.blockerAlert)).toHaveCount(0)
  }

  async expectDebtAbsent(creditor: string): Promise<void> {
    await expect(this.page.getByTestId(sel.debts.item).filter({ hasText: creditor })).toHaveCount(0)
  }

  /** The row-level projection text: "Còn <n> tháng · dự kiến hết nợ <date>" or the Vietnamese blocker sentence. */
  async expectDebtRow(creditor: string, text: string): Promise<void> {
    await expect(this.row(creditor)).toContainText(text)
  }
}
