import { test } from '@playwright/test'
import { DashboardPage } from '../pages/DashboardPage'
import { DebtsPage } from '../pages/DebtsPage'
import { ProfilePage } from '../pages/ProfilePage'
import { registerFreshAccount } from '../support/auth-flow'
import {
  blockedDebt,
  debtJourneyIncome,
  debtJourneyTotals,
  journeyBasics,
  solvableDebt,
} from '../support/test-data'

/**
 * 002 T016 — the debt journey, ending on the integration that matters: the mandatory debt payment
 * reaches the Financial Position (Dashboard) and reduces Free cash.
 *
 * register → profile income → add solvable debt → summary + DTI + payoff ETA → add insolvent debt
 * → BLOCKED + portfolio blocked → fix the payment → blocker clears → archive → position restored.
 *
 * All money assertions read server-rendered text; the suite never computes amortization.
 */
test('debt journey: register → debt → DTI/ETA/blocker → fix → archive → dashboard position', async ({ page }) => {
  await registerFreshAccount(page, 'debt')

  // 1. Income 30M is the DTI denominator the summary reads from the profile.
  const profile = new ProfilePage(page)
  await profile.open()
  await profile.basics.save(journeyBasics)
  await profile.income.add(debtJourneyIncome)

  // 2. Add a solvable debt: 15M @ 18% with a 3M planned payment amortizes in 6 periods.
  const debts = new DebtsPage(page)
  await debts.open()
  await debts.addDebt(solvableDebt)
  await debts.expectTotalDebt(debtJourneyTotals.totalDebt)
  await debts.expectTotalMinimum(debtJourneyTotals.totalMinimum)
  await debts.expectDti(debtJourneyTotals.dti)
  await debts.expectDebtRow(solvableDebt.creditor, '6 kỳ')

  // 3. Add a debt that cannot amortize (payment < monthly interest): BLOCKED, portfolio blocked.
  await debts.addDebt(blockedDebt)
  await debts.expectDebtRow(blockedDebt.creditor, 'BLOCKED: PAYMENT_DOES_NOT_COVER_INTEREST')
  await debts.expectBlocked('PORTFOLIO_CONTAINS_BLOCKED_DEBTS')
  await debts.expectPayoffDate('Chưa dự báo được')

  // 4. Fix the planned payment (>= minimum): the individual and portfolio blockers clear.
  await debts.editPlanned(blockedDebt.creditor, '1.000.000')
  await debts.expectNotBlocked()
  await debts.expectDebtRow(blockedDebt.creditor, 'kỳ')

  // 5. Archive the second debt: it leaves the list and the totals.
  await debts.archiveDebt(blockedDebt.creditor)
  await debts.expectDebtAbsent(blockedDebt.creditor)
  await debts.expectTotalDebt(debtJourneyTotals.totalDebt)
  await debts.expectTotalMinimum(debtJourneyTotals.totalMinimum)

  // 6. INTEGRATION: the Dashboard charges the mandatory minimum and reduces Free cash 30M - 1.5M.
  const dashboard = new DashboardPage(page)
  await dashboard.open()
  await dashboard.expectMandatoryPayment(debtJourneyTotals.mandatoryPayment)
  await dashboard.expectFreeCash(debtJourneyTotals.freeCash)
})
