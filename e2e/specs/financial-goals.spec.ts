import { expect, test } from '@playwright/test'
import { GoalsPage } from '../pages/GoalsPage'
import { ProfilePage } from '../pages/ProfilePage'
import { registerFreshAccount } from '../support/auth-flow'
import {
  emergencyGoal,
  goalJourneyExpected,
  goalJourneyExpense,
  goalJourneyIncome,
  journeyBasics,
  threePeriodsOut,
} from '../support/test-data'

/**
 * 003 T014 — the goal journey: a destination with user-reported progress, the
 * required monthly capacity against Available Capacity (equality then
 * shortfall), completion on reaching the target, and archive → 404.
 *
 * register → profile income 30M → create dated goal → remaining/progress →
 * capacity MEETS_REQUIRED (equality) → +20M expense → SHORTFALL → fund to
 * target → COMPLETED → archive → direct GET returns 404.
 *
 * All money assertions read server-rendered text; the suite never computes
 * remaining, progress or required capacity.
 */
test('goal journey: register → goal → capacity equality/shortfall → complete → archive → 404', async ({
  page,
}) => {
  await registerFreshAccount(page, 'goals')

  // 1. Income 30M with no debts/expenses: Available Capacity is the whole 30M.
  const profile = new ProfilePage(page)
  await profile.open()
  await profile.basics.save(journeyBasics)
  await profile.income.add(goalJourneyIncome)

  // 2. Dated goal: 120M target, 30M current → remaining 90M, progress 25%.
  const goals = new GoalsPage(page)
  await goals.open()
  await goals.addGoal({ ...emergencyGoal, date: threePeriodsOut() })
  await goals.openCapacity(emergencyGoal.name)
  await goals.expectProgress(goalJourneyExpected.remaining, goalJourneyExpected.progress)

  // 3. 90M over 3 periods = 30M/month == available 30M: exactly affordable.
  await goals.expectCoverage(goalJourneyExpected.meetsCoverage)
  await goals.expectRequired(goalJourneyExpected.required)

  // 4. A 20M expense drops capacity to 10M: SHORTFALL with a 20M gap.
  await profile.open()
  const expenseSaved = page.waitForResponse(
    (res) => res.url().includes('/api/v1/expenses') && res.request().method() === 'POST',
  )
  await profile.expense.add(goalJourneyExpense)
  await expenseSaved
  await goals.open()
  await goals.openCapacity(emergencyGoal.name)
  await goals.expectCoverage(goalJourneyExpected.shortfallCoverage)
  await goals.expectCapacityShortfall(goalJourneyExpected.shortfall)

  // 5. Funding the goal to its target completes it; progress clamps at 100%.
  await goals.open()
  await goals.editCurrent(emergencyGoal.name, '120.000.000')
  await goals.expectCompleted(emergencyGoal.name)
  await goals.openCapacity(emergencyGoal.name)
  await goals.expectProgress('0.00 VND', '100%')

  // 6. Archive: the row leaves the list and the API answers 404 afterwards.
  await goals.open()
  await goals.archiveGoal(emergencyGoal.name)
  await goals.expectAbsent(emergencyGoal.name)
  expect(goals.lastCreatedId).not.toBeNull()
  const direct = await page.request.get(`/api/v1/goals/${goals.lastCreatedId}`)
  expect(direct.status()).toBe(404)
})
