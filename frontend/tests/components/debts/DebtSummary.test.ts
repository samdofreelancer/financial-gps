import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import DebtSummaryCard from '@/components/debts/DebtSummaryCard.vue'
import DebtBlockerAlert from '@/components/debts/DebtBlockerAlert.vue'
import type { DebtSummary } from '@/api/debts'

function summary(): DebtSummary {
  return {
    totalOutstandingDebt: '30000000.00',
    totalMinimumMonthlyPayment: '3000000.00',
    totalPlannedMonthlyPayment: '5000000.00',
    currency: 'VND',
    debtToIncome: { status: 'AVAILABLE', ratio: '0.1000', reasonCode: null, explanation: null },
    portfolioProjection: {
      status: 'BLOCKED',
      projectedDebtFreeDate: null,
      totalMonthsRemaining: null,
      totalInterestRemaining: null,
      reasonCode: 'PORTFOLIO_CONTAINS_BLOCKED_DEBTS',
      explanation: 'blocked',
      blockedDebts: [
        { creditor: 'Bank', reasonCode: 'PAYMENT_DOES_NOT_COVER_INTEREST', explanation: 'too low' },
      ],
    },
    blockedDebtCount: 1,
    asOf: '2026-10-01',
  }
}

describe('DebtSummaryCard', () => {
  it('renders totals and DTI percentage', () => {
    const wrapper = mount(DebtSummaryCard, { props: { summary: summary() } })
    expect(wrapper.text()).toContain('10.00%')
    expect(wrapper.text()).toContain('Chưa dự báo được')
  })
})

describe('DebtBlockerAlert', () => {
  it('enumerates blocked debts with reason codes', () => {
    const wrapper = mount(DebtBlockerAlert, { props: { summary: summary(), debts: [] } })
    expect(wrapper.text()).toContain('BLOCKED')
    expect(wrapper.text()).toContain('PAYMENT_DOES_NOT_COVER_INTEREST')
    expect(wrapper.text()).toContain('PORTFOLIO_CONTAINS_BLOCKED_DEBTS')
  })
})
