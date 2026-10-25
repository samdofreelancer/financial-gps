import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import GoalProgressCard from '@/components/goals/GoalProgressCard.vue'
import type { GoalView } from '@/api/goals'

function goalWithProgress(progress: string): GoalView {
  return {
    id: '00000000-0000-0000-0000-000000000001',
    name: 'Emergency Fund',
    goalType: 'EMERGENCY_FUND',
    targetAmount: '3.00',
    currentAmount: '1.00',
    targetDate: null,
    priority: 1,
    status: 'ACTIVE',
    currency: 'VND',
    remaining: '2.00',
    progress,
    completionCondition: 'AMOUNT_REACHED',
    derived: { remaining: 'calculated', progress: 'calculated' },
  }
}

/**
 * Spec §4.2 display rule: the stored scale-4 ratio is floored (never rounded)
 * at 2 decimals for display, so the screen never overstates completion.
 */
describe('GoalProgressCard (display floor)', () => {
  it('floors 0.3333 to 33.33%, not 33.34%', () => {
    const wrapper = mount(GoalProgressCard, { props: { goal: goalWithProgress('0.3333') } })
    expect(wrapper.find('[data-testid="goal-progress-value"]').text()).toBe('33.33%')
  })

  it('floors 0.9999 to 99.99%, never 100% while still active', () => {
    const wrapper = mount(GoalProgressCard, { props: { goal: goalWithProgress('0.9999') } })
    expect(wrapper.find('[data-testid="goal-progress-value"]').text()).toBe('99.99%')
  })

  it('does not fall for the binary-float trap: 0.2900 shows 29%, not 28.99%', () => {
    // 0.29 * 10000 is 2899.9999… in floating point — string integer math avoids it.
    const wrapper = mount(GoalProgressCard, { props: { goal: goalWithProgress('0.2900') } })
    expect(wrapper.find('[data-testid="goal-progress-value"]').text()).toBe('29%')
  })

  it('shows exactly 100% for a completed goal', () => {
    const wrapper = mount(GoalProgressCard, {
      props: { goal: { ...goalWithProgress('1.0000'), status: 'COMPLETED' } },
    })
    expect(wrapper.find('[data-testid="goal-progress-value"]').text()).toBe('100%')
  })
})
