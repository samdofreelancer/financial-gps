import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as gpsApi from '@/api/gps'
import type { FinancialGpsView, MoneyView } from '@/api/gps'
import type { GoalView } from '@/api/goals'
import { useGoalStore } from '@/stores/goalStore'
import GpsView from '@/views/GpsView.vue'

vi.mock('@/api/gps', () => ({
  fetchFinancialGps: vi.fn(),
}))

const GOAL_ONE: GoalView = {
  id: 'goal-1',
  name: 'Emergency Fund',
  goalType: 'EMERGENCY_FUND',
  targetAmount: '108000000.00',
  currentAmount: '20000000.00',
  targetDate: '2027-03-01',
  priority: 1,
  status: 'ACTIVE',
  currency: 'VND',
  remaining: '88000000.00',
  progress: '0.20',
  completionCondition: 'AMOUNT_REACHED',
  derived: {},
}
const GOAL_TWO: GoalView = { ...GOAL_ONE, id: 'goal-2', name: 'Housing', goalType: 'HOUSING' }

function money(field: string, availability: 'AVAILABLE' | 'UNAVAILABLE' = 'AVAILABLE', amount: string | null = '0.00'): MoneyView {
  const available = availability === 'AVAILABLE'
  return {
    field,
    amount: available ? amount : null,
    currency: 'VND',
    availability,
    provenanceKind: available ? 'calculated' : 'unavailable',
    provenanceDetail: available ? 'calculated' : 'missing input',
    assumptionSource: null,
  }
}

function gpsView(overrides: Partial<FinancialGpsView> = {}): FinancialGpsView {
  return {
    asOf: '2026-10-11',
    destination: {
      type: 'GOAL',
      goalId: 'goal-1',
      goalName: 'Emergency Fund',
      goalType: 'EMERGENCY_FUND',
    },
    currentPosition: {
      income: money('income'),
      expense: money('expense'),
      mandatoryPayment: money('mandatoryPayment'),
      netCashFlow: money('netCashFlow'),
      availableCapacity: money('availableCapacity'),
      savings: money('savings', 'UNAVAILABLE'),
      emergencyFund: money('emergencyFund', 'UNAVAILABLE'),
      dependents: 0,
      totalOutstandingDebt: money('totalOutstandingDebt'),
      dti: {
        status: 'AVAILABLE',
        ratio: '0.12',
        reasonCode: null,
        explanation: null,
        provenanceKind: 'calculated',
        provenanceDetail: 'calculated',
      },
    },
    distance: {
      type: 'AMOUNT_BASED',
      remaining: '88000000.00',
      currency: 'VND',
      targetAmount: '108000000.00',
      currentAmount: '20000000.00',
    },
    progress: { progressPercent: '0.20', reason: null },
    capacityComparison: {
      requiredMonthly: '31000000.00',
      projectedMonthly: '31000000.00',
      coverage: 'MEETS_REQUIRED',
      shortfall: null,
      currency: 'VND',
      explanation: 'Available capacity meets the required monthly amount.',
    },
    eta: { availability: 'CALCULATED', date: '2027-06-01', periods: 20, reason: null, explanation: null },
    status: {
      status: 'ON_TRACK',
      explanation: 'On track with 0 months lateness.',
      condition: {
        type: 'ON_TRACK',
        reason: null,
        requiredMonthly: '31000000.00',
        projectedMonthly: '31000000.00',
        monthsRemaining: 5,
        etaPeriods: 3,
        lateness: 0,
        latenessTolerance: 3,
        projectedDebtFreeDate: null,
        totalMonthsRemaining: null,
        targetDate: '2027-03-01',
      },
    },
    blockers: [],
    nextActions: [],
    explanations: [
      {
        category: 'CAPACITY_COMPARISON',
        field: 'availableCapacity',
        rule: 'AC >= RM',
        inputs: [],
        threshold: '>=',
        outcome: 'available capacity meets required',
        provenanceKind: 'calculated',
        provenanceDetail: 'calculated',
      },
      // Same category, distinct field — the key `${category}:${field}` keeps both rendered.
      {
        category: 'CAPACITY_COMPARISON',
        field: 'requiredMonthly',
        rule: 'amount / months',
        inputs: [],
        threshold: '-',
        outcome: 'required monthly computed',
        provenanceKind: 'calculated',
        provenanceDetail: 'calculated',
      },
    ],
    provenance: [{ field: 'availableCapacity', kind: 'calculated', detail: 'calculated', assumptionSource: null }],
    missingInputs: [],
    ...overrides,
  }
}

async function mountGps(goals: GoalView[], query: Record<string, string> = {}) {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/gps', name: 'gps', component: GpsView },
      { path: '/goals', name: 'goals', component: { template: '<div />' } },
    ],
  })
  await router.push({ path: '/gps', query })
  await router.isReady()
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useGoalStore()
  store.$patch({ goals, loading: false })
  const fetchGoals = vi.spyOn(store, 'fetchGoals').mockResolvedValue(undefined)
  const wrapper = mount(GpsView, { global: { plugins: [pinia, router] } })
  await flushPromises()
  return { wrapper, router, store, fetchGoals }
}

describe('GpsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('issues exactly one GPS request for the auto-selected goal on mount', async () => {
    vi.mocked(gpsApi.fetchFinancialGps).mockResolvedValue(gpsView())

    const { wrapper } = await mountGps([GOAL_ONE, GOAL_TWO])

    // onMounted auto-selects goal-1 → the watcher fires one loadGps; no second mount call.
    expect(gpsApi.fetchFinancialGps).toHaveBeenCalledTimes(1)
    expect(gpsApi.fetchFinancialGps).toHaveBeenCalledWith('goal-1', undefined)
    expect(wrapper.find('#goal-select').exists()).toBe(true)
    expect(wrapper.text()).toContain('Emergency Fund')
  })

  it('with no goals renders the empty state and never calls the GPS API', async () => {
    const { wrapper } = await mountGps([])

    expect(gpsApi.fetchFinancialGps).not.toHaveBeenCalled()
    expect(wrapper.find('.empty-state').exists()).toBe(true)
    expect(wrapper.text()).toContain('Create a goal first')
    expect(wrapper.find('a[href="/goals"]').exists()).toBe(true)
  })

  it('drops a late response when a newer asOf request already won', async () => {
    let resolveStale: (view: FinancialGpsView) => void
    const stale = new Promise<FinancialGpsView>((resolve) => {
      resolveStale = resolve
    })
    vi.mocked(gpsApi.fetchFinancialGps)
      .mockImplementationOnce(() => stale)
      .mockResolvedValueOnce(gpsView({ asOf: '2026-06-01' }))

    const { wrapper, router } = await mountGps([GOAL_ONE], { asOf: '2026-01-01' })
    // First request (asOf 2026-01-01) is still in flight.
    expect(gpsApi.fetchFinancialGps).toHaveBeenNthCalledWith(1, 'goal-1', '2026-01-01')

    // A newer ?asOf=2026-06-01 navigation starts a second request that resolves first.
    await router.push({ path: '/gps', query: { asOf: '2026-06-01' } })
    await flushPromises()
    expect(gpsApi.fetchFinancialGps).toHaveBeenNthCalledWith(2, 'goal-1', '2026-06-01')

    // The stale 2026-01-01 response lands last — it must NOT overwrite the newer view.
    resolveStale!(gpsView({ asOf: '2026-01-01' }))
    await flushPromises()

    expect(wrapper.text()).toContain('as of 2026-06-01')
    expect(wrapper.text()).not.toContain('2026-01-01')
  })

  it('keeps Savings and Emergency Fund rows visible when UNAVAILABLE', async () => {
    vi.mocked(gpsApi.fetchFinancialGps).mockResolvedValueOnce(gpsView())

    const { wrapper } = await mountGps([GOAL_ONE])

    const badges = wrapper.findAll('.unavailable-badge').map((b) => b.text())
    expect(badges.filter((t) => t === 'Unavailable').length).toBeGreaterThanOrEqual(2)
    const positionItems = wrapper.findAll('.position-item').map((i) => i.text())
    expect(positionItems.some((t) => t.includes('Savings') && t.includes('—'))).toBe(true)
    expect(positionItems.some((t) => t.includes('Emergency Fund') && t.includes('—'))).toBe(true)
    // Both same-category explanations render (distinct keys).
    expect(wrapper.text()).toContain('available capacity meets required')
    expect(wrapper.text()).toContain('required monthly computed')
  })
})