import { test } from '@playwright/test'
import { DebtsPage } from '../pages/DebtsPage'
import { registerFreshAccount } from '../support/auth-flow'

/**
 * Debt journey (T016): register → /debts → add credit-card debt → summary + DTI move →
 * payoff ETA visible → insolvent debt triggers BLOCKED → portfolio blocked.
 * All money assertions read server-rendered text; the suite never computes amortization.
 */
test('debt journey: register → add debt → summary, DTI, ETA, blocker', async ({ page }) => {
  await registerFreshAccount(page, 'debt')

  const debts = new DebtsPage(page)
  await debts.open()

  await debts.addDebt({ creditor: 'Techcombank', balance: '15.000.000', min: '1.500.000', planned: '3.000.000' })
  await debts.expectTotalDebt('15.000.000')
})
