import { expect, type Page } from '@playwright/test'
import { sel } from '../../support/selectors'
import type { ServerTotals } from '../../support/test-data'

/**
 * "Your financial position" hero card — server-calculated totals only.
 * Assertions read the rendered text; the suite never recomputes money.
 */
export class PositionSection {
  constructor(private readonly page: Page) {}

  async expectTotals(totals: ServerTotals): Promise<void> {
    const stats = this.page.getByTestId(sel.position.card).locator(sel.position.stats)
    // Term cells are unique (dt); the provenance <details> also mentions the
    // labels, so every assertion is scoped to the stats row (strict-safe).
    await expect(stats.getByText('Income', { exact: true })).toBeVisible()
    await expect(stats.getByText(totals.income).first()).toBeVisible()
    await expect(stats.getByText(totals.expenses).first()).toBeVisible()
    await expect(stats.getByText(totals.freeCash).first()).toBeVisible()
  }
}
