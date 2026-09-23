import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '@/api/profile'
import type { ProfileView } from '@/api/profile'
import { useAuthStore } from '@/stores/authStore'
import DashboardView from '@/views/DashboardView.vue'

vi.mock('@/api/profile', () => ({
  getProfile: vi.fn(),
  putProfile: vi.fn(),
  putIncome: vi.fn(),
  postIncome: vi.fn(),
  deleteIncome: vi.fn(),
  putExpense: vi.fn(),
  postExpense: vi.fn(),
  deleteExpense: vi.fn(),
  // PositionSummary → MoneyDisplay renders through formatMoney.
  formatMoney: (amount: string, currency: string) => `${amount} ${currency}`,
}))

function view(overrides: Partial<ProfileView> = {}): ProfileView {
  return {
    currency: 'VND',
    savingsAmount: '100.00',
    emergencyFundAmount: '50.00',
    dependentsCount: 2,
    incomes: [],
    expenses: [],
    totalIncome: { amount: '80.00', currency: 'VND', provenance: 'calculated' },
    totalExpenses: { amount: '50.00', currency: 'VND', provenance: 'calculated' },
    netCashFlow: { amount: '30.00', currency: 'VND', provenance: 'calculated' },
    availableCapacity: { amount: '30.00', currency: 'VND', provenance: 'calculated' },
    provenance: [],
    asOf: '2026-09-20',
    ...overrides,
  }
}

async function mountDashboard() {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/dashboard', name: 'dashboard', component: DashboardView },
      { path: '/profile', name: 'profile', component: { template: '<div />' } },
      { path: '/account', name: 'account', component: { template: '<div />' } },
    ],
  })
  await router.push('/dashboard')
  await router.isReady()
  const pinia = createPinia()
  setActivePinia(pinia)
  useAuthStore().account = { id: '1', email: 'a@example.com' }
  const wrapper = mount(DashboardView, { global: { plugins: [pinia, router] } })
  await flushPromises()
  return wrapper
}

describe('DashboardView (signed-in home)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads the server position once and renders the calculated totals', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())

    const wrapper = await mountDashboard()

    expect(api.getProfile).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('a@example.com')
    expect(wrapper.text()).toContain('Current position')
    expect(wrapper.text()).toContain('80.00 VND')
    expect(wrapper.text()).toContain('30.00 VND')
  })

  it('links straight to the profile and account screens', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())

    const wrapper = await mountDashboard()

    const hrefs = wrapper.findAll('a').map((a) => a.attributes('href'))
    expect(hrefs).toContain('/profile')
    expect(hrefs).toContain('/account')
  })

  it('surfaces a load failure instead of inventing numbers', async () => {
    vi.mocked(api.getProfile).mockRejectedValue(new Error('boom'))

    const wrapper = await mountDashboard()

    expect(wrapper.find('.error-box').exists()).toBe(true)
    expect(wrapper.text()).toContain('Could not reach the server')
    expect(wrapper.text()).not.toContain('80.00 VND')
  })
})
