import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PositionSummary from './PositionSummary.vue'
import type { ProfileView } from '../api/profile'

/**
 * 001 (F3 / US2 / FR-002-003): the summary renders the server position verbatim.
 * It must not derive any total itself — a deliberately inconsistent payload proves that the
 * component displays what the server said instead of recomputing Income/Expense/Net Cash
 * Flow/Available Capacity.
 */
function view(overrides: Partial<ProfileView> = {}): ProfileView {
  return {
    currency: 'VND',
    savingsAmount: '100.00',
    emergencyFundAmount: '50.00',
    dependentsCount: 2,
    incomes: [
      {
        id: 'i1',
        amount: '74.00',
        currency: 'VND',
        source: 'salary',
        provenance: 'actual',
      },
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
      { field: 'Income', kind: 'calculated', detail: 'sum of active incomes effective on 2026-09-16' },
    ],
    asOf: '2026-09-16',
    ...overrides,
  }
}

describe('PositionSummary', () => {
  it('renders Income, Expense, Net Cash Flow and Available Capacity from the server', () => {
    const wrapper = mount(PositionSummary, { props: { view: view() } })

    expect(wrapper.text()).toContain('80,00')
    expect(wrapper.text()).toContain('50,00')
    expect(wrapper.text()).toContain('30,00')
    expect(wrapper.text()).toContain('Net cash flow')
    expect(wrapper.text()).toContain('Available capacity')
  })

  it('labels totals as calculated and profile facts as actual (US2)', () => {
    const wrapper = mount(PositionSummary, { props: { view: view() } })

    const labels = wrapper.findAll('.prov').map((node) => node.text())
    expect(labels.filter((label) => label === 'calculated').length).toBeGreaterThanOrEqual(4)
    expect(wrapper.text()).toContain('actual')
    expect(wrapper.text()).toContain('Liquid savings')
    expect(wrapper.text()).toContain('Emergency fund')
    expect(wrapper.text()).toContain('Dependents')
  })

  it('shows the evaluated date so an as-of shift is explainable', () => {
    const wrapper = mount(PositionSummary, { props: { view: view() } })

    expect(wrapper.text()).toContain('2026-09-16')
  })

  it('never recomputes: contradictory server totals are displayed unchanged', () => {
    const wrapper = mount(PositionSummary, {
      props: {
        view: view({
          totalIncome: { amount: '1.00', currency: 'VND', provenance: 'calculated' },
          netCashFlow: { amount: '-99.00', currency: 'VND', provenance: 'calculated' },
          availableCapacity: { amount: '0.00', currency: 'VND', provenance: 'calculated' },
        }),
      },
    })

    expect(wrapper.text()).toContain('1,00')
    expect(wrapper.text()).toContain('-99,00')
    expect(wrapper.text()).not.toContain('74,00')
  })

  it('renders an empty state instead of inventing zeros for a missing position', () => {
    const wrapper = mount(PositionSummary, { props: { view: null } })

    expect(wrapper.text()).toContain('No position yet')

    const empty = wrapper.findAll('.prov')
    expect(empty).toHaveLength(0)
  })
})