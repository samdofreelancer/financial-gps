import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import DebtSummaryCard from '@/components/debts/DebtSummaryCard.vue'
import DebtBlockerAlert from '@/components/debts/DebtBlockerAlert.vue'
import DebtList from '@/components/debts/DebtList.vue'
import type { DebtSummary, DebtView } from '@/api/debts'

function summary(): DebtSummary {
  return {
    totalOutstandingDebt: '30000000.00',
    totalMinimumMonthlyPayment: '3000000.00',
    totalPlannedMonthlyPayment: '5000000.00',
    totalMonthlyAccruedInterest: '4500000.00',
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

/** A debt whose payment cannot even cover its interest, so the whole BLOCKED story is exercised. */
function blockedDebt(): DebtView {
  return {
    id: 'd1',
    creditor: 'Bank',
    debtType: 'CREDIT_CARD',
    originalPrincipal: '40000000.00',
    outstandingBalance: '40000000.00',
    annualInterestRate: '0.180000',
    minimumPayment: '2000000.00',
    plannedPayment: '3000000.00',
    dueDay: 15,
    status: 'ACTIVE',
    currency: 'VND',
    projection: {
      status: 'BLOCKED',
      projectedPayoffDate: null,
      numberOfPayments: null,
      totalInterest: null,
      finalPayment: null,
      monthlyInterest: '6000000.00',
      reasonCode: 'PAYMENT_DOES_NOT_COVER_INTEREST',
      explanation: 'Monthly planned payment is less than monthly accrued interest.',
    },
  }
}

describe('DebtSummaryCard', () => {
  it('renders totals and DTI percentage', () => {
    const wrapper = mount(DebtSummaryCard, { props: { summary: summary() } })
    expect(wrapper.text()).toContain('10.00%')
    expect(wrapper.text()).toContain('Chưa dự báo được')
  })

  it('shows the monthly interest that explains a blocked portfolio', () => {
    const wrapper = mount(DebtSummaryCard, { props: { summary: summary() } })
    expect(wrapper.find('[data-testid="total-interest"]').text()).toContain('4.500.000')
    // VND has no minor unit, so the meaningless ",00" tail is dropped here.
    expect(wrapper.find('[data-testid="total-debt"]').text()).toContain('30.000.000')
    expect(wrapper.find('[data-testid="total-debt"]').text()).not.toContain(',00')
  })

  it('rates DTI against its benchmark instead of showing a bare percentage', () => {
    const wrapper = mount(DebtSummaryCard, { props: { summary: summary() } })
    // The 4-band scale: <20% is "Rất thoải mái"; "Khá an toàn" starts at 20% (see debtText.test).
    expect(wrapper.text()).toContain('Rất thoải mái')
    expect(wrapper.text()).toContain('36%')
  })

  it('masks every amount when the reader asks for privacy', () => {
    const visible = mount(DebtSummaryCard, { props: { summary: summary() } })
    const hidden = mount(DebtSummaryCard, { props: { summary: summary(), hidden: true } })
    expect(hidden.find('[data-testid="total-debt"]').text()).not.toContain('30.000.000')
    expect(hidden.text()).toContain('••••••••')
    expect(hidden.find('[data-testid="payment-split"]').text()).toContain('••••••••')
    expect(visible.find('[data-testid="total-debt"]').text()).toContain('30.000.000')
  })

  it('explains why the payoff date may be missing on demand', async () => {
    const wrapper = mount(DebtSummaryCard, { props: { summary: summary() } })
    expect(wrapper.text()).not.toContain('Chúng tôi chỉ dự báo được')
    await wrapper.find('[data-testid="payoff-why"]').trigger('click')
    expect(wrapper.text()).toContain('Chúng tôi chỉ dự báo được')
  })

  it('places the interest marker at the interest threshold, not at the fill end', () => {
    const wrapper = mount(DebtSummaryCard, { props: { summary: summary() } })
    const styleNumber = (selector: string, prop: string): number => {
      const style = wrapper.find(selector).attributes('style') ?? ''
      const match = style.match(new RegExp(`${prop}:\\s*([\\d.]+)`))
      return match ? Number(match[1]) : Number.NaN
    }
    // planned 5.000.000 / interest 4.500.000, one shared scale of 5.500.000 (110% of the larger).
    expect(styleNumber('.compare__fill', 'width')).toBeCloseTo((5000000 / 5500000) * 100, 5)
    expect(styleNumber('.compare__mark', 'left')).toBeCloseTo((4500000 / 5500000) * 100, 5)
    // Regression: the marker used to sit exactly at the fill end, hiding a BLOCKED shortfall.
    expect(styleNumber('.compare__mark', 'left')).toBeLessThan(styleNumber('.compare__fill', 'width'))
  })

  it('says the comparison is payment vs next-period interest and splits the payment', () => {
    const wrapper = mount(DebtSummaryCard, { props: { summary: summary() } })
    expect(wrapper.text()).toContain('Khoản trả có bù được lãi kỳ tới?')
    expect(wrapper.text()).toContain('Dự định trả (gốc + lãi)')
    expect(wrapper.text()).toContain('Lãi kỳ tới')
    const split = wrapper.find('[data-testid="payment-split"]')
    // 5.000.000 − 4.500.000 = 500.000 VND of the payment actually reduces the debt.
    expect(split.text()).toContain('giảm nợ')
    expect(split.text()).toContain('500.000')
  })

  it('reports a shortfall when the interest outruns the payment', () => {
    const s = summary()
    s.totalPlannedMonthlyPayment = '4000000.00'
    s.totalMonthlyAccruedInterest = '4500000.00'
    const wrapper = mount(DebtSummaryCard, { props: { summary: s } })
    const split = wrapper.find('[data-testid="payment-split"]')
    expect(split.text()).toContain('thiếu')
    expect(split.text()).toContain('500.000')
    expect(split.text()).toContain('dư nợ sẽ tăng')
    expect(split.classes()).toContain('compare__split--short')
  })
})

describe('DebtBlockerAlert', () => {
  it('enumerates blocked debts with reason codes', () => {
    const wrapper = mount(DebtBlockerAlert, { props: { summary: summary(), debts: [] } })
    expect(wrapper.text()).toContain('BLOCKED')
    expect(wrapper.text()).toContain('PAYMENT_DOES_NOT_COVER_INTEREST')
    expect(wrapper.text()).toContain('PORTFOLIO_CONTAINS_BLOCKED_DEBTS')
  })

  it('leads with Vietnamese guidance instead of the raw enum', () => {
    const wrapper = mount(DebtBlockerAlert, {
      props: { summary: summary(), debts: [blockedDebt()] },
    })
    expect(wrapper.text()).toContain('Khoản trả mỗi tháng nhỏ hơn tiền lãi phát sinh')
    expect(wrapper.text()).toContain('Trả không đủ lãi')
    // No untranslated server prose leaks into the default view: the collapsed
    // "Chi tiết kỹ thuật" block is the only place the server's English is allowed to live.
    const defaultMessage = wrapper.find('.blocker__list').text()
    expect(defaultMessage).not.toContain('Balance will grow')
    expect(defaultMessage).not.toContain('is less than monthly accrued interest')
  })

  it('names the payment that unblocks the debt and offers a way to apply it', async () => {
    const wrapper = mount(DebtBlockerAlert, {
      props: { summary: summary(), debts: [blockedDebt()] },
    })
    // 6.000.000 of interest: anything above it clears the blocker.
    expect(wrapper.text()).toContain('6.000.001')
    await wrapper.find('[data-testid="fix-Bank"]').trigger('click')
    expect(wrapper.emitted('fix')).toEqual([['Bank']])
  })

  it('shows nothing when every debt projects cleanly', () => {
    const clean = summary()
    clean.portfolioProjection.status = 'AVAILABLE'
    clean.portfolioProjection.blockedDebts = []
    clean.blockedDebtCount = 0
    const wrapper = mount(DebtBlockerAlert, { props: { summary: clean, debts: [] } })
    expect(wrapper.find('[data-testid="blocker-alert"]').exists()).toBe(false)
  })
})

describe('DebtList', () => {
  it('names the rate and the due day instead of only showing balances', () => {
    const wrapper = mount(DebtList, { props: { debts: [blockedDebt()] } })
    expect(wrapper.text()).toContain('18%/năm')
    expect(wrapper.text()).toContain('Ngày 15 hằng tháng')
    expect(wrapper.text()).toContain('Đang trả')
  })

  it('formats fractional interest rates compactly and warns when a due date is near', () => {
    const debt = blockedDebt()
    debt.annualInterestRate = '0.132'
    debt.dueDay = 28
    const wrapper = mount(DebtList, { props: { debts: [debt], asOf: '2026-10-25' } })

    expect(wrapper.text()).toContain('13,2%/năm')
    expect(wrapper.find('[data-testid="debt-due-hint"]').text()).toBe('Còn 3 ngày đến hạn')
  })

  it('keeps the rate visible in privacy mode and offers a reversible manual paid marker', async () => {
    const debt = blockedDebt()
    const wrapper = mount(DebtList, { props: { debts: [debt], hidden: true } })
    const markButton = wrapper.find('[data-testid="payment-mark-d1"]')

    expect(wrapper.text()).toContain('18%/năm')
    expect(markButton.text()).toContain('Đánh dấu đã trả')
    await markButton.trigger('click')
    expect(wrapper.emitted('mark-paid')).toHaveLength(1)

    await wrapper.setProps({ debts: [{ ...debt, paidThisPeriod: true }] })
    expect(wrapper.find('[data-testid="payment-mark-d1"]').text()).toContain('Đã trả kỳ này')
    await wrapper.find('[data-testid="payment-mark-d1"]').trigger('click')
    expect(wrapper.emitted('undo-paid')).toHaveLength(1)
  })

  it('splits the first payment into interest and principal using exact decimal arithmetic', () => {
    const debt = blockedDebt()
    debt.plannedPayment = '20570000.00'
    debt.projection.status = 'AVAILABLE'
    debt.projection.monthlyInterest = '4242242.14'
    const wrapper = mount(DebtList, { props: { debts: [debt] } })

    expect(wrapper.find('[data-testid="debt-payment-split"]').text()).toContain('16.327.757,86')
    expect(wrapper.find('[data-testid="debt-payment-split"]').text()).toContain('4.242.242,14')
  })

  it('explains that balance grows when the planned payment is below monthly interest', () => {
    const wrapper = mount(DebtList, { props: { debts: [blockedDebt()] } })
    expect(wrapper.find('[data-testid="debt-payment-split"]').text()).toContain('dư nợ tăng 3.000.000')
    expect(wrapper.find('[data-testid="debt-payment-split"]').text()).toContain('lãi 6.000.000')
  })

  it('keeps a readable badge while the code stays on the tooltip', () => {
    const wrapper = mount(DebtList, { props: { debts: [blockedDebt()] } })
    expect(wrapper.text()).toContain('Trả không đủ lãi')
    expect(wrapper.html()).toContain('PAYMENT_DOES_NOT_COVER_INTEREST')
  })

  it('offers a labelled empty state rather than a blank page', () => {
    const wrapper = mount(DebtList, { props: { debts: [] } })
    expect(wrapper.find('[data-testid="debts-empty"]').exists()).toBe(true)
  })
})
