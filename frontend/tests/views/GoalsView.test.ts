import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { GoalView } from '@/api/goals'
import GoalsView from '@/views/GoalsView.vue'

vi.mock('@/api/goals', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/goals')>()
  return {
    ...actual,
    listGoals: vi.fn(),
    getGoal: vi.fn(),
    getGoalCapacity: vi.fn(),
    postGoal: vi.fn(),
    putGoal: vi.fn(),
    deleteGoal: vi.fn(),
  }
})

const api = await import('@/api/goals')

function goal(): GoalView {
  return {
    id: 'g1',
    name: 'Emergency Fund',
    goalType: 'EMERGENCY_FUND',
    targetAmount: '120000000.00',
    currentAmount: '30000000.00',
    targetDate: '2027-12-31',
    priority: 1,
    status: 'ACTIVE',
    currency: 'VND',
    remaining: '90000000.00',
    progress: '0.2500',
    completionCondition: 'AMOUNT_REACHED',
    derived: { remaining: 'calculated', progress: 'calculated' },
  }
}

async function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const wrapper = mount(GoalsView, { global: { plugins: [pinia] } })
  await flushPromises()
  return wrapper
}

describe('GoalsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(api.listGoals).mockResolvedValue([goal()])
  })

  it('loads the goal list on mount', async () => {
    const wrapper = await mountView()
    expect(api.listGoals).toHaveBeenCalledOnce()
    expect(wrapper.findAll('[data-testid="goal-row"]')).toHaveLength(1)
  })

  it('opens the capacity view from the row button', async () => {
    vi.mocked(api.getGoalCapacity).mockResolvedValue({
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
    })
    const wrapper = await mountView()

    expect(wrapper.find('[data-testid="goal-capacity"]').exists()).toBe(false)
    const buttons = wrapper.find('[data-testid="goal-row"]').findAll('button')
    await buttons[1].trigger('click')
    await flushPromises()

    expect(api.getGoalCapacity).toHaveBeenCalledWith('g1')
    expect(wrapper.find('[data-testid="goal-capacity"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="goal-progress"]').exists()).toBe(true)
  })
})
