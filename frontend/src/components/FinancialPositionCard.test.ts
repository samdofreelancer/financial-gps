import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import FinancialPositionCard from './FinancialPositionCard.vue'
import type { ProfileView } from '../api/profile'

/**
 * The hero card is the "where am I?" answer: it renders the server position verbatim and never
 * derives a total of its own. Empty and loading states must not look like a broken page.
 */
function view(overrides: Partial<ProfileView> = {}): ProfileView {
  return {
    currency: 'VND',
    savingsAmount: '100.00',
    emergencyFundAmount: '50.00',
    dependentsCount: 2,
    incomes: [
      { id: 'i1', amount: '74.00', currency: 'VND', source: 'salary', provenance: 'actual' },
    ],
    expenses: [
      {
        id: 'e1',
        amount: '30.00',
        currency: 'VND',
        category: 'rent',
        expenseType: 'FIXED',
        provenance: 'actual',
      },
    ],
    totalIncome: { amount: '80.00', currency: 'VND', provenance: 'calculated' },
    totalExpenses: { amount: '50.00', currency: 'VND', provenance: 'calculated' },
    netCashFlow: { amount: '30.00', currency: 'VND', provenance: 'calculated' },
    availableCapacity: { amount: '30.00', currency: 'VND', provenance: 'calculated' },
    provenance: [
      { field: 'netCashFlow', kind: 'calculated', detail: 'Income − expenses' },
    ],
    asOf: '2026-09-16',
    ...overrides,
  }
}

describe('FinancialPositionCard', () => {
  it('shows the server position as the focus of the card', () => {
    const wrapper = mount(FinancialPositionCard, { props: { view: view() } })

    expect(wrapper.text()).toContain('Your financial position')
    expect(wrapper.find('.amount').text()).toBe('+30,00')
    expect(wrapper.text()).toContain('Income')
    expect(wrapper.text()).toContain('80,00')
    expect(wrapper.text()).toContain('Expenses')
    expect(wrapper.text()).toContain('50,00')
    expect(wrapper.text()).toContain('Free cash')
    expect(wrapper.text()).toContain('Monthly figures in VND')
    expect(wrapper.text()).toContain('2026-09-16')
  })

  it('renders a negative position with a sign, not colour alone', () => {
    const wrapper = mount(FinancialPositionCard, {
      props: { view: view({ netCashFlow: { amount: '-44.00', currency: 'VND', provenance: 'calculated' } }) },
    })

    expect(wrapper.find('.amount').text()).toBe('-44,00')
    expect(wrapper.find('.amount').classes()).toContain('negative')
  })

  it('translates the API field names in the derivation details', () => {
    const wrapper = mount(FinancialPositionCard, { props: { view: view() } })

    expect(wrapper.find('details').text()).toContain('Free cash')
    expect(wrapper.find('details').text()).not.toContain('netCashFlow')
  })

  it('invites the first income before any data exists', async () => {
    const wrapper = mount(FinancialPositionCard, {
      props: { view: view({ incomes: [], expenses: [], savingsAmount: '0.00', emergencyFundAmount: '0.00' }) },
    })

    expect(wrapper.text()).toContain('Start by adding your income and expenses.')
    expect(wrapper.find('.amount').exists()).toBe(false)

    await wrapper.find('.first-income').trigger('click')

    expect(wrapper.emitted('add-income')).toHaveLength(1)
  })

  it('distinguishes loading from an unavailable position', () => {
    const loading = mount(FinancialPositionCard, { props: { view: null, loading: true } })
    expect(loading.text()).toContain('Loading your position')

    const unavailable = mount(FinancialPositionCard, { props: { view: null, loading: false } })
    expect(unavailable.text()).toContain('unavailable')
  })
})
