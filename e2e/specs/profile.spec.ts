import { test } from '@playwright/test'
import { E2E_PASSWORD, uniqueEmail } from '../fixtures/accounts'
import { ProfilePage } from '../pages/ProfilePage'
import { RegisterPage } from '../pages/RegisterPage'

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
  const email = uniqueEmail('profile')

  // 1. register → dashboard
  const register = new RegisterPage(page)
  await register.open()
  await register.register(email, E2E_PASSWORD)
  await register.expectOnDashboard()

  const profile = new ProfilePage(page)

  // 2. basics
  await profile.open()
  await profile.saveBasics({ savings: '100.000.000', emergency: '50.000.000', dependents: '2' })
  await profile.expectBasics({ savings: '100.000.000', emergency: '50.000.000', dependents: '2' })

  // 3. income → totals move
  await profile.addIncome({ amount: '30.000.000', source: 'salary' })
  await profile.expectIncomeRow('salary')
  await profile.expectPosition({ income: '30.000.000', expenses: '0,00', freeCash: '30.000.000' })

  // 4. expense (Quỹ nuôi con) → free cash drops
  await profile.expectExpenseCategoryOption('Quỹ nuôi con / childcare')
  await profile.addExpense({ amount: '20.000.000', category: 'childcare', type: 'VARIABLE' })
  await profile.expectExpenseRow('childcare')
  await profile.expectPosition({ income: '30.000.000', expenses: '20.000.000', freeCash: '10.000.000' })
})
