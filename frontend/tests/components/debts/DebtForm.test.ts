import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import DebtForm from '@/components/debts/DebtForm.vue'
import type { DebtPayload, DebtView } from '@/api/debts'

/** Fill every required money field so only the assertion under test can block submit. */
async function fillValidForm(wrapper: ReturnType<typeof mount>) {
  await wrapper.find('[data-testid="creditor"]').setValue('Bank')
  const moneyInputs = wrapper.findAllComponents({ name: 'MoneyInput' })
  await moneyInputs[0].setValue('1000000')
  await moneyInputs[1].setValue('100000')
  await moneyInputs[2].setValue('200000')
}

describe('DebtForm', () => {
  it('blocks submit when planned is below minimum', async () => {
    const wrapper = mount(DebtForm, { props: {} })
    await wrapper.find('[data-testid="creditor"]').setValue('Bank')
    const button = wrapper.find('button[type="submit"]')
    expect(wrapper.text()).toContain('Số tiền là bắt buộc')
    expect((button.element as HTMLButtonElement).disabled).toBe(true)
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('submits a valid payload with null rate when empty', async () => {
    const wrapper = mount(DebtForm, { props: {} })
    await wrapper.find('[data-testid="creditor"]').setValue('Bank')
    await wrapper.findComponent({ name: 'MoneyInput' }).setValue('1000')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('sends null dueDay when the due day is left blank (no default 15)', async () => {
    const wrapper = mount(DebtForm, { props: {} })
    await fillValidForm(wrapper)
    // happy-dom does not chain a submit-button click into a form submit event; dispatch it directly.
    await wrapper.find('form').trigger('submit')
    const payload = wrapper.emitted('submit')?.[0]?.[0] as DebtPayload
    expect(payload.dueDay).toBeNull()
  })

  it('submits the chosen due day', async () => {
    const wrapper = mount(DebtForm, { props: {} })
    await fillValidForm(wrapper)
    await wrapper.find('[data-testid="due-day"]').setValue('28')
    await wrapper.find('form').trigger('submit')
    const payload = wrapper.emitted('submit')?.[0]?.[0] as DebtPayload
    expect(payload.dueDay).toBe(28)
  })

  it('blocks submit when the due day is out of range', async () => {
    const wrapper = mount(DebtForm, { props: {} })
    await fillValidForm(wrapper)
    await wrapper.find('[data-testid="due-day"]').setValue('32')
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('số nguyên từ 1 đến 31')
    expect((wrapper.find('button[type="submit"]').element as HTMLButtonElement).disabled).toBe(true)
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('pre-fills the due day of the debt being edited', async () => {
    const line = {
      id: 'd1',
      creditor: 'Shinhan Bank',
      debtType: 'PERSONAL_LOAN',
      originalPrincipal: null,
      outstandingBalance: '385658376.00',
      annualInterestRate: '0.132000',
      minimumPayment: '20570000.00',
      plannedPayment: '20570000.00',
      dueDay: 5,
      status: 'ACTIVE',
      currency: 'VND',
      projection: {
        status: 'AVAILABLE',
        projectedPayoffDate: '2028-08-02',
        numberOfPayments: 22,
        totalInterest: '0.00',
        finalPayment: '0.00',
        monthlyInterest: '0.00',
        reasonCode: null,
        explanation: null,
      },
    } as DebtView
    const wrapper = mount(DebtForm, { props: { line } })
    expect(wrapper.find('[data-testid="due-day"]').element.value).toBe('5')
  })
})
