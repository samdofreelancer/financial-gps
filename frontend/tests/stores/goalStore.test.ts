import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { isMoneyValid, type GoalCapacityView, type GoalView } from '@/api/goals'
import { useGoalStore } from '@/stores/goalStore'

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

function goal(id = 'g1'): GoalView {
  return {
    id,
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

function capacity(id = 'g1'): GoalCapacityView {
  return {
    goalId: id,
    asOf: '2026-10-01',
    remaining: '90000000.00',
    monthsRemaining: 12,
    requiredMonthlyCapacity: '7500000.00',
    availableCapacity: '24000000.00',
    capacityCoverage: 'MEETS_REQUIRED',
    monthlyShortfall: '0.00',
    dateFeasibility: 'DATED',
    explanation: 'within capacity',
  }
}

describe('goal client guards', () => {
  it('accepts canonical non-negative decimals only', () => {
    expect(isMoneyValid('200000000.00')).toBe(true)
    expect(isMoneyValid('0.00')).toBe(true)
    expect(isMoneyValid('0')).toBe(true)
    expect(isMoneyValid('-1.00')).toBe(false)
    expect(isMoneyValid('100.123')).toBe(false)
    expect(isMoneyValid('1.')).toBe(false)
    expect(isMoneyValid('abc')).toBe(false)
    expect(isMoneyValid('')).toBe(false)
    expect(isMoneyValid('  10.00  ')).toBe(true)
  })
})

describe('goal store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchGoals loads the server list', async () => {
    vi.mocked(api.listGoals).mockResolvedValue([goal()])

    const store = useGoalStore()
    await store.fetchGoals()

    expect(store.goals).toHaveLength(1)
    expect(store.error).toBe('')
  })

  it('addGoal posts then refetches', async () => {
    vi.mocked(api.postGoal).mockResolvedValue(goal())
    vi.mocked(api.listGoals).mockResolvedValue([goal()])

    const store = useGoalStore()
    await store.addGoal({
      name: 'Emergency Fund',
      goalType: 'EMERGENCY_FUND',
      targetAmount: '120000000.00',
      currentAmount: '30000000.00',
      targetDate: '2027-12-31',
      priority: 1,
    })

    expect(api.postGoal).toHaveBeenCalledOnce()
    expect(store.goals).toHaveLength(1)
  })

  it('updateGoal drops the stale capacity entry', async () => {
    vi.mocked(api.getGoalCapacity).mockResolvedValue(capacity())
    vi.mocked(api.putGoal).mockResolvedValue(goal())
    vi.mocked(api.listGoals).mockResolvedValue([goal()])

    const store = useGoalStore()
    await store.fetchCapacity('g1')
    expect(store.capacities['g1']).toBeDefined()

    await store.updateGoal('g1', {
      name: 'Emergency Fund',
      goalType: 'EMERGENCY_FUND',
      targetAmount: '120000000.00',
      currentAmount: '120000000.00',
      targetDate: '2027-12-31',
      priority: 1,
    })
    expect(store.capacities['g1']).toBeUndefined()
  })

  it('removeGoal drops the capacity entry', async () => {
    vi.mocked(api.getGoalCapacity).mockResolvedValue(capacity())
    vi.mocked(api.deleteGoal).mockResolvedValue(undefined)
    vi.mocked(api.listGoals).mockResolvedValue([])

    const store = useGoalStore()
    await store.fetchCapacity('g1')
    await store.removeGoal('g1')

    expect(store.capacities['g1']).toBeUndefined()
    expect(store.goals).toHaveLength(0)
  })

  it('reports a failed capacity fetch', async () => {
    vi.mocked(api.getGoalCapacity).mockRejectedValue(new Error('offline'))

    const store = useGoalStore()
    await store.fetchCapacity('g1')

    expect(store.capacityError).toBe(
      'Could not reach the server. Check your connection and try again.',
    )
  })

  it('keeps only the newest capacity response', async () => {
    let settleSlow!: (value: GoalCapacityView) => void
    vi.mocked(api.getGoalCapacity)
      .mockImplementationOnce(
        () => new Promise<GoalCapacityView>((resolve) => { settleSlow = resolve }),
      )
      .mockResolvedValueOnce({ ...capacity('fast'), requiredMonthlyCapacity: '10000000.00' })

    const store = useGoalStore()
    const slow = store.fetchCapacity('slow')
    await store.fetchCapacity('fast')

    settleSlow({ ...capacity('slow'), requiredMonthlyCapacity: '1.00' })
    await slow

    // The slow response belongs to a selection that has moved on — it must not win.
    expect(store.capacities['fast']?.requiredMonthlyCapacity).toBe('10000000.00')
    expect(store.capacities['slow']).toBeUndefined()
    expect(store.capacityLoading).toBe(false)
  })
})
