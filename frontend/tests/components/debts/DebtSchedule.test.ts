import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import DebtScheduleDialog from '@/components/debts/DebtScheduleDialog.vue'
import type { DebtSchedule, DebtView } from '@/api/debts'

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

/**
 * Three periods of a 22-period calendar — the exact split the reader asked for
 * (month → principal / interest / ending balance), digit-for-digit from the server.
 */
function schedule(overrides: Partial<DebtSchedule> = {}): DebtSchedule {
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
    ...overrides,
  }
}

describe('DebtScheduleDialog', () => {
  it('renders one row per period with principal, interest and ending balance', () => {
    const wrapper = mount(DebtScheduleDialog, { props: { debt: debt(), schedule: schedule() } })

    const rows = wrapper.findAll('[data-testid="schedule-row"]')
    expect(rows).toHaveLength(3)

    // October: gốc / lãi / dư nợ cuối kỳ, exactly as the server split them.
    expect(rows[0].text()).toContain('16.327.757,86 VND')
    expect(rows[0].text()).toContain('4.242.242,14 VND')
    expect(rows[0].text()).toContain('370.000.000 VND')
    // The last rendered period still carries its own ending balance, not the payoff one.
    expect(rows[2].text()).toContain('336.676.414,95 VND')
  })

  it('names the columns the reader asked for', () => {
    const wrapper = mount(DebtScheduleDialog, { props: { debt: debt(), schedule: schedule() } })
    const headers = wrapper.findAll('th').map((th) => th.text())
    expect(headers).toEqual(['Kỳ', 'Ngày trả', 'Khoản trả', 'Gốc', 'Lãi', 'Dư nợ cuối kỳ'])
  })

  it('reads dates as dd/MM/yyyy and summarises payoff and total interest', () => {
    const wrapper = mount(DebtScheduleDialog, { props: { debt: debt(), schedule: schedule() } })

    const rows = wrapper.findAll('[data-testid="schedule-row"]')
    expect(rows[0].text()).toContain('28/10/2026')
    expect(wrapper.text()).not.toContain('2026-10-28')

    const meta = wrapper.find('[data-testid="schedule-meta"]').text()
    expect(meta).toContain('02/08/2028')
    expect(meta).toContain('22 kỳ')
    expect(meta).toContain('46.216.000 VND')
  })

  it('explains a blocked calendar instead of showing an empty table', () => {
    const blocked = schedule({
      status: 'BLOCKED',
      payoffDate: null,
      numberOfPayments: null,
      totalInterest: null,
      reasonCode: 'PAYMENT_DOES_NOT_COVER_INTEREST',
      rows: [],
    })
    const wrapper = mount(DebtScheduleDialog, { props: { debt: debt(), schedule: blocked } })

    expect(wrapper.find('[data-testid="schedule-blocked"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="schedule-table"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('dư nợ sẽ tăng dần mỗi tháng')
  })

  it('says when the debt is already paid off', () => {
    const done = schedule({ status: 'COMPLETED', payoffDate: null, rows: [] })
    const wrapper = mount(DebtScheduleDialog, { props: { debt: debt(), schedule: done } })
    expect(wrapper.find('[data-testid="schedule-completed"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="schedule-table"]').exists()).toBe(false)
  })

  it('shows loading and failure states rather than a blank dialog', () => {
    const loading = mount(DebtScheduleDialog, {
      props: { debt: debt(), schedule: null, loading: true },
    })
    expect(loading.find('[data-testid="schedule-loading"]').exists()).toBe(true)

    const failed = mount(DebtScheduleDialog, {
      props: { debt: debt(), schedule: null, error: 'Could not load the payment schedule.' },
    })
    expect(failed.find('[role="alert"]').text()).toContain('Could not load the payment schedule.')
  })

  it('closes from the button and from Escape', async () => {
    const wrapper = mount(DebtScheduleDialog, { props: { debt: debt(), schedule: schedule() } })

    await wrapper.find('.sched__actions .btn').trigger('click')
    expect(wrapper.emitted('close')).toHaveLength(1)

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(wrapper.emitted('close')).toHaveLength(2)
    wrapper.unmount()
  })
})
