import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { DebtSchedule, DebtSummary, DebtView } from '@/api/debts'
import { useDebtStore } from '@/stores/debtStore'
import DebtsView from '@/views/DebtsView.vue'

vi.mock('@/api/debts', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/debts')>()
  return {
    ...actual,
    listDebts: vi.fn(),
    getDebtSummary: vi.fn(),
    getDebtSchedule: vi.fn(),
    postDebt: vi.fn(),
    putDebt: vi.fn(),
    deleteDebt: vi.fn(),
  }
})

const api = await import('@/api/debts')

function debt(): DebtView {
  return {
    id: 'd1',
    creditor: 'Shinhan Bank',
    debtType: 'PERSONAL_LOAN',
    originalPrincipal: '500000000.00',
    outstandingBalance: '386327757.86',
    annualInterestRate: '0.132000',
    minimumPayment: '20570000.00',
    plannedPayment: '20570000.00',
    dueDay: 28,
    status: 'ACTIVE',
    currency: 'VND',
    projection: {
      status: 'AVAILABLE',
      projectedPayoffDate: '2028-08-02',
      numberOfPayments: 22,
      totalInterest: '46216000.00',
      finalPayment: '20570000.00',
      monthlyInterest: '4242242.14',
      reasonCode: null,
      explanation: null,
    },
  }
}

function summary(): DebtSummary {
  return {
    totalOutstandingDebt: '386327757.86',
    totalMinimumMonthlyPayment: '20570000.00',
    totalPlannedMonthlyPayment: '20570000.00',
    totalMonthlyAccruedInterest: '4242242.14',
    currency: 'VND',
    debtToIncome: { status: 'AVAILABLE', ratio: '0.2780', reasonCode: null, explanation: null },
    portfolioProjection: {
      status: 'AVAILABLE',
      projectedDebtFreeDate: '2028-08-02',
      totalMonthsRemaining: 22,
      totalInterestRemaining: '46216000.00',
      reasonCode: null,
      explanation: null,
      blockedDebts: [],
    },
    blockedDebtCount: 0,
    asOf: '2026-10-02',
  }
}

/** Three of the 22 periods — enough to prove one row per period without a giant fixture. */
function calendar(): DebtSchedule {
  return {
    status: 'AVAILABLE',
    payoffDate: '2028-08-02',
    numberOfPayments: 22,
    totalInterest: '46216000.00',
    finalPayment: '20570000.00',
    reasonCode: null,
    explanation: null,
    currency: 'VND',
    rows: [
      {
        period: 1,
        dueDate: '2026-10-28',
        payment: '20570000.00',
        principal: '16327757.86',
        interest: '4242242.14',
        endingBalance: '370000000.00',
      },
      {
        period: 2,
        dueDate: '2026-11-28',
        payment: '20570000.00',
        principal: '16549928.74',
        interest: '4020071.26',
        endingBalance: '353450071.26',
      },
      {
        period: 3,
        dueDate: '2026-12-28',
        payment: '20570000.00',
        principal: '16773656.31',
        interest: '3796343.69',
        endingBalance: '336676414.95',
      },
    ],
  }
}

async function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const wrapper = mount(DebtsView, { global: { plugins: [pinia] } })
  await flushPromises()
  return wrapper
}

describe('DebtsView payment calendar', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(api.listDebts).mockResolvedValue([debt()])
    vi.mocked(api.getDebtSummary).mockResolvedValue(summary())
    vi.mocked(api.getDebtSchedule).mockResolvedValue(calendar())
  })

  it('opens the schedule dialog from the calendar button, one row per period', async () => {
    const wrapper = await mountView()

    // Nothing is fetched or shown before the reader asks for the calendar.
    expect(wrapper.find('[data-testid="schedule-table"]').exists()).toBe(false)
    expect(api.getDebtSchedule).not.toHaveBeenCalled()

    await wrapper.find('[data-testid="debt-schedule"]').trigger('click')
    await flushPromises()

    expect(api.getDebtSchedule).toHaveBeenCalledWith('d1')
    expect(wrapper.find('[data-testid="schedule-table"]').exists()).toBe(true)

    const rows = wrapper.findAll('[data-testid="schedule-row"]')
    expect(rows).toHaveLength(3)
    expect(rows[0].text()).toContain('16.327.757,86 VND')
    expect(rows[0].text()).toContain('4.242.242,14 VND')
    expect(rows[0].text()).toContain('370.000.000 VND')

    const headers = wrapper.findAll('[data-testid="schedule-table"] th').map((th) => th.text())
    expect(headers).toEqual(['Kỳ', 'Ngày trả', 'Khoản trả', 'Gốc', 'Lãi', 'Dư nợ cuối kỳ'])
  })

  it('shows the loading state while the calendar is still in flight', async () => {
    let release!: (value: DebtSchedule) => void
    vi.mocked(api.getDebtSchedule).mockReturnValue(
      new Promise<DebtSchedule>((resolve) => { release = resolve }),
    )

    const wrapper = await mountView()
    await wrapper.find('[data-testid="debt-schedule"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="schedule-loading"]').exists()).toBe(true)

    release(calendar())
    await flushPromises()
    expect(wrapper.findAll('[data-testid="schedule-row"]')).toHaveLength(3)
  })

  it('closes the dialog and drops the loaded schedule', async () => {
    const wrapper = await mountView()
    await wrapper.find('[data-testid="debt-schedule"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="schedule-table"]').exists()).toBe(true)

    await wrapper.find('.sched__actions .btn').trigger('click')

    expect(wrapper.find('[data-testid="schedule-table"]').exists()).toBe(false)
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
    expect(useDebtStore().schedule).toBeNull()
  })
})
