import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import GoalCapacity from '@/components/goals/GoalCapacity.vue'
import type { GoalCapacityView } from '@/api/goals'

function capacity(overrides: Partial<GoalCapacityView> = {}): GoalCapacityView {
  return {
    goalId: 'g1',
    asOf: '2026-10-01',
    remaining: '90000000.00',
    monthsRemaining: 12,
    requiredMonthlyCapacity: '7500000.00',
    availableCapacity: '24000000.00',
    capacityCoverage: 'MEETS_REQUIRED',
    monthlyShortfall: '0.00',
    dateFeasibility: 'DATED',
    explanation: 'within capacity',
    ...overrides,
  }
}

describe('GoalCapacity', () => {
  it('shows MEETS_REQUIRED with the required monthly figure', () => {
    const wrapper = mount(GoalCapacity, { props: { capacity: capacity() } })
    expect(wrapper.find('[data-testid="capacity-coverage"]').text()).toBe('MEETS_REQUIRED')
    expect(wrapper.find('[data-testid="capacity-required"]').text()).toBe('7500000.00')
  })

  it('shows SHORTFALL with the gap', () => {
    const wrapper = mount(GoalCapacity, {
      props: {
        capacity: capacity({
          availableCapacity: '1000000.00',
          capacityCoverage: 'SHORTFALL',
          monthlyShortfall: '6500000.00',
        }),
      },
    })
    expect(wrapper.find('[data-testid="capacity-coverage"]').text()).toBe('SHORTFALL')
    expect(wrapper.text()).toContain('6500000.00')
  })

  it('renders a dash when no monthly figure can be scheduled', () => {
    const wrapper = mount(GoalCapacity, {
      props: {
        capacity: capacity({
          monthsRemaining: null,
          requiredMonthlyCapacity: null,
          monthlyShortfall: null,
          capacityCoverage: 'NOT_APPLICABLE',
          dateFeasibility: 'UNDATED',
        }),
      },
    })
    expect(wrapper.find('[data-testid="capacity-coverage"]').text()).toBe('NOT_APPLICABLE')
    expect(wrapper.find('[data-testid="capacity-required"]').text()).toBe('—')
  })

  it('explains an expired target date', () => {
    const wrapper = mount(GoalCapacity, {
      props: {
        capacity: capacity({
          monthsRemaining: 0,
          requiredMonthlyCapacity: '90000000.00',
          dateFeasibility: 'EXPIRED_TARGET_DATE',
        }),
      },
    })
    expect(wrapper.find('[data-testid="capacity-expired"]').exists()).toBe(true)
  })
})
