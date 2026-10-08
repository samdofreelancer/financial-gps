import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DebtBlockerAlert from '@/components/debts/DebtBlockerAlert.vue'
import type { DebtSummary, DebtView } from '@/api/debts'

function blockedDebt(): DebtView {
  return {
    id: 'd9',
    creditor: 'Vay nóng',
    debtType: 'PERSONAL_LOAN',
    originalPrincipal: null,
    outstandingBalance: '10000000.00',
    annualInterestRate: '0.120000',
    minimumPayment: '50000.00',
    plannedPayment: '80000.00',
    dueDay: null,
    status: 'ACTIVE',
    currency: 'VND',
    projection: {
      status: 'BLOCKED',
      projectedPayoffDate: null,
      numberOfPayments: null,
      totalInterest: null,
      finalPayment: null,
      monthlyInterest: '100000.00',
      reasonCode: 'PAYMENT_DOES_NOT_COVER_INTEREST',
      explanation: null,
    },
  }
}

function summary(): DebtSummary {
  return {
    totalOutstandingDebt: '10000000.00',
    totalMinimumMonthlyPayment: '50000.00',
    totalPlannedMonthlyPayment: '80000.00',
    totalMonthlyAccruedInterest: '100000.00',
    currency: 'VND',
    debtToIncome: { status: 'AVAILABLE', ratio: '0.0100', reasonCode: null, explanation: null },
    portfolioProjection: {
      status: 'BLOCKED',
      projectedDebtFreeDate: null,
      totalMonthsRemaining: null,
      totalInterestRemaining: null,
      reasonCode: 'PORTFOLIO_CONTAINS_BLOCKED_DEBTS',
      explanation: null,
      blockedDebts: [
        {
          creditor: 'Vay nóng',
          reasonCode: 'PAYMENT_DOES_NOT_COVER_INTEREST',
          explanation: null,
        },
      ],
    },
    blockedDebtCount: 1,
    asOf: '2026-10-01',
  }
}

describe('DebtBlockerAlert', () => {
  it('stays hidden when nothing is blocked', () => {
    const clean = {
      ...summary(),
      portfolioProjection: {
        ...summary().portfolioProjection,
        status: 'AVAILABLE',
        reasonCode: null,
        blockedDebts: [],
      },
    }
    const wrapper = mount(DebtBlockerAlert, { props: { summary: clean, debts: [] } })
    expect(wrapper.find('[data-testid="blocker-alert"]').exists()).toBe(false)
  })

  it('names the blocked debt and emits fix for its creditor', async () => {
    const wrapper = mount(DebtBlockerAlert, { props: { summary: summary(), debts: [blockedDebt()] } })
    const alert = wrapper.find('[data-testid="blocker-alert"]')
    expect(alert.exists()).toBe(true)
    expect(alert.text()).toContain('Vay nóng')

    await wrapper.find('[data-testid="fix-Vay nóng"]').trigger('click')
    expect(wrapper.emitted('fix')).toEqual([['Vay nóng']])
  })
})
