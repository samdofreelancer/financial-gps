import { expect, test } from '@playwright/test'
import { DebtsPage } from '../pages/DebtsPage'
import { GoalsPage } from '../pages/GoalsPage'
import { ProfilePage } from '../pages/ProfilePage'
import { RoadmapPage } from '../pages/RoadmapPage'
import { registerFreshAccount } from '../support/auth-flow'
import {
  emergencyGoal,
  goalJourneyIncome,
  journeyBasics,
  solvableDebt,
  threePeriodsOut,
} from '../support/test-data'

/**
 * 005 T031 / T039 — the roadmap journey: an ordered stage list whose current stage is visible
 * without navigation, every stage explained, and a reached goal that stays positionally
 * UPCOMING because the earlier debt stage is still open.
 *
 * register → profile income 30M → debt 15M (planned 3M) → emergency goal 120M/30M (dated, three
 * periods out) → savings goal → /roadmap: stages 1 debt (current), 2 emergency fund, 3 savings;
 * stage 2 blocked by stage 1 → complete the emergency goal → stage 2 keeps its reached badge
 * beside the UPCOMING position, and the order policy is still shown as a stage order, not an
 * allocation order.
 *
 * All assertions read server-rendered text; the suite never computes capacity, progress or dates.
 */
test('roadmap journey: ordered stages, current stage, blocked detail, reached-but-upcoming stage', async ({
  page,
}) => {
  await registerFreshAccount(page, 'roadmap')

  // 1. Facts the route is derived from: income 30M and a 15M debt with a 3M planned payment.
  const profile = new ProfilePage(page)
  await profile.open()
  await profile.basics.save(journeyBasics)
  await profile.income.add(goalJourneyIncome)

  const debts = new DebtsPage(page)
  await debts.open()
  await debts.addDebt(solvableDebt)

  const goals = new GoalsPage(page)
  await goals.open()
  await goals.addGoal({ ...emergencyGoal, date: threePeriodsOut() })
  await goals.addGoal({
    name: 'Mua nhà',
    type: 'SAVINGS',
    target: '200.000.000',
    current: '0',
    priority: '2',
  })

  // 2. The route: debt elimination first, then the emergency-fund goal, then the remaining goal.
  const roadmap = new RoadmapPage(page)
  await roadmap.open()
  await roadmap.expectLoaded()
  await roadmap.expectStages(3)
  await roadmap.expectCurrentStage(1)
  await roadmap.expectStatus(2, 'Sắp tới')
  await roadmap.expectBlockedBadge(2, 1)
  await roadmap.expectNoReachedBadge(2)

  // 3. The debt stage carries the 002 plan-based figure and the plan-based ETA assumption.
  await roadmap.expectObjective('Eliminate the outstanding debt')
  await roadmap.expectCapacityLabel('Khoản trả hàng tháng theo kế hoạch')
  await roadmap.expectCapacityAmount('3.000.000,00 VND')
  await roadmap.expectEtaBasis('Feature 002')
  await roadmap.expectEtaAssumption('trả thêm')
  await roadmap.expectStageOrderPolicy()

  // 4. The blocked goal states why it is not current, how much it needs, and that position is not funding.
  await roadmap.openStage(2)
  await roadmap.expectObjective('Emergency Fund')
  await roadmap.expectDetailStatus('Sắp tới')
  await roadmap.expectBlockedByNote(1)
  await roadmap.expectNextAction('Stage 1 must complete')
  await roadmap.expectNextAction('not mean it is unfunded')
  await roadmap.expectPositionIsNotFunding()
  await roadmap.expectCapacityLabel('Năng lực hàng tháng cần thiết')
  await roadmap.expectCapacityAmount('30.000.000,00 VND')

  // 5. Transition: completing the goal's own condition does NOT move it past the open debt stage.
  await goals.open()
  await goals.editCurrent(emergencyGoal.name, '120.000.000')
  await goals.expectCompleted(emergencyGoal.name)

  await roadmap.open()
  await roadmap.expectStages(3)
  await roadmap.expectCurrentStage(1)
  await roadmap.expectStatus(2, 'Sắp tới')
  await roadmap.expectReachedBadge(2)

  await roadmap.openStage(2)
  await roadmap.expectDetailStatus('Sắp tới')
  await roadmap.expectReachedInDetails()
  await roadmap.expectNextAction('already reached its target')
  await roadmap.expectPositionIsNotFunding()
  await roadmap.expectStageOrderPolicy()
  await roadmap.expectEtaBasis('Feature 004')
})

test('roadmap journey: a fresh owner sees the empty destination instead of a fabricated stage', async ({
  page,
}) => {
  await registerFreshAccount(page, 'roadmap-empty')

  const roadmap = new RoadmapPage(page)
  await roadmap.open()
  await roadmap.expectEmptyDestination()
  await expect(page.getByText('Chưa có đích đến nào trên lộ trình')).toBeVisible()
})
