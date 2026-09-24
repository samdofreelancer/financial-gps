import { test } from '@playwright/test'
import { ProfilePage } from '../pages/ProfilePage'
import { registerFreshAccount } from '../support/auth-flow'
import {
  CHILDCARE_LABEL,
  journeyBasics,
  journeyExpense,
  journeyIncome,
  totalsAfterExpense,
  totalsAfterIncome,
} from '../support/test-data'

/**
 * User-profile journey — basics + income + expense → server totals move.
 * All money assertions read what the server rendered
 * (vi-VN presentation: 30000000 → "30.000.000"); the suite never computes totals.
 *
 * One test, one flow: register → basics → income → expense in a single browser
 * session. Splitting it across tests would need shared storage state; a single
 * linear flow is truer to the user journey and immune to session resets.
 */
test('user profile journey: register → basics → income → expense → totals', async ({ page }) => {
  // 1. register → dashboard
  await registerFreshAccount(page, 'profile')

  const profile = new ProfilePage(page)

  // 2. basics
  await profile.open()
  await profile.basics.save(journeyBasics)
  await profile.basics.expectShown(journeyBasics)

  // 3. income → totals move
  await profile.income.add(journeyIncome)
  await profile.income.expectRow(journeyIncome.source)
  await profile.position.expectTotals(totalsAfterIncome)

  // 4. expense (Quỹ nuôi con) → free cash drops
  await profile.expense.expectCategoryOffered(CHILDCARE_LABEL)
  await profile.expense.add(journeyExpense)
  await profile.expense.expectRow(journeyExpense.category)
  await profile.position.expectTotals(totalsAfterExpense)
})

