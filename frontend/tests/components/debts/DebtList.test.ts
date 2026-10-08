import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DebtList from '@/components/debts/DebtList.vue'
import type { DebtView } from '@/api/debts'

function debt(): DebtView {
  return {
    id: 'd1',
    creditor: 'Shinhan Bank',
    debtType: 'PERSONAL_LOAN',
    originalPrincipal: null,
    outstandingBalance: '1000.00',
    annualInterestRate: '0.120000',
    minimumPayment: '50.00',
    plannedPayment: '100.00',
    dueDay: 15,
    status: 'ACTIVE',
    currency: 'VND',
    projection: {
      status: 'AVAILABLE',
      projectedPayoffDate: '2027-08-01',
      numberOfPayments: 11,
      totalInterest: '56.80',
      finalPayment: '56.80',
      monthlyInterest: '10.00',
      reasonCode: null,
      explanation: null,
    },
  }
}

describe('DebtList', () => {
  it('renders one row per debt and emits row actions', async () => {
    const wrapper = mount(DebtList, { props: { debts: [debt()] } })
    expect(wrapper.findAll('[data-testid="debt-item"]')).toHaveLength(1)
    expect(wrapper.find('[data-testid="status-d1"]').text()).toBe('Đang trả')

    await wrapper.find('[data-testid="debt-schedule"]').trigger('click')
    expect(wrapper.emitted('schedule')?.[0]).toEqual([expect.objectContaining({ id: 'd1' })])
  })

  it('masks amounts when hidden', () => {
    const wrapper = mount(DebtList, { props: { debts: [debt()], hidden: true } })
    expect(wrapper.text()).toContain('••••••')
    expect(wrapper.text()).not.toContain('1.000')
  })

  it('shows the empty state when there are no debts', () => {
    const wrapper = mount(DebtList, { props: { debts: [] } })
    expect(wrapper.find('[data-testid="debts-empty"]').exists()).toBe(true)
  })
})
