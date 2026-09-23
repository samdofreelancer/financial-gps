import { expect, type Page } from '@playwright/test'
import { sel } from '../support/selectors'
import { BasePage } from './BasePage'
import { BasicsSection } from './components/BasicsSection'
import { ExpenseSection } from './components/ExpenseSection'
import { IncomeSection } from './components/IncomeSection'
import { PositionSection } from './components/PositionSection'

/**
 * /profile — Financial GPS orchestrator page.
 * A thin composer: navigation + section components. MoneyInput presentation
 * (vi-VN, e.g. 30.000.000 for 30M) and server-total assertions live in the
 * sections; this class only wires them to the page.
 */
export class ProfilePage extends BasePage {
  readonly basics: BasicsSection
  readonly income: IncomeSection
  readonly expense: ExpenseSection
  readonly position: PositionSection

  constructor(page: Page) {
    super(page)
    this.basics = new BasicsSection(page)
    this.income = new IncomeSection(page)
    this.expense = new ExpenseSection(page)
    this.position = new PositionSection(page)
  }

  async open(): Promise<void> {
    await this.goto('/profile')
    await expect(
      this.page.getByRole(sel.profileHeading.role, { name: sel.profileHeading.name, exact: true }),
    ).toBeVisible()
  }
}

