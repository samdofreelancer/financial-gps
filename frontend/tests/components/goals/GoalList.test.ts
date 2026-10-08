import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import GoalList from '@/components/goals/GoalList.vue'
import type { GoalView } from '@/api/goals'

function goal(id = 'g1', status: GoalView['status'] = 'ACTIVE'): GoalView {
  return {
    id,
    name: 'Emergency Fund',
    goalType: 'EMERGENCY_FUND',
    targetAmount: '120000000.00',
    currentAmount: '30000000.00',
    targetDate: '2027-12-31',
    priority: 1,
    status,
    currency: 'VND',
    remaining: '90000000.00',
    progress: '0.2500',
    completionCondition: 'AMOUNT_REACHED',
    derived: { remaining: 'calculated', progress: 'calculated' },
  }
}

describe('GoalList', () => {
  it('renders one row per goal with its status badge', () => {
    const wrapper = mount(GoalList, { props: { goals: [goal('g1'), goal('g2', 'COMPLETED')] } })
    expect(wrapper.findAll('[data-testid="goal-row"]')).toHaveLength(2)
    expect(wrapper.find('[data-testid="goal-status-g1"]').text()).toBe('ACTIVE')
    expect(wrapper.find('[data-testid="goal-status-g2"]').text()).toBe('COMPLETED')
  })

  it('emits edit, capacity and remove for the right goal', async () => {
    const wrapper = mount(GoalList, { props: { goals: [goal('g1')] } })
    const buttons = wrapper.findAll('button')
    await buttons[0].trigger('click')
    await buttons[1].trigger('click')
    await buttons[2].trigger('click')
    expect(wrapper.emitted('edit')?.[0]).toEqual([expect.objectContaining({ id: 'g1' })])
    expect(wrapper.emitted('capacity')?.[0]).toEqual([expect.objectContaining({ id: 'g1' })])
    expect(wrapper.emitted('remove')?.[0]).toEqual([expect.objectContaining({ id: 'g1' })])
  })

  it('shows the empty state when there are no goals', () => {
    const wrapper = mount(GoalList, { props: { goals: [] } })
    expect(wrapper.find('[data-testid="goal-empty"]').exists()).toBe(true)
  })
})
