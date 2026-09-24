import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { nextTick } from 'vue'
import FinancialBasicsCard from '@/components/FinancialBasicsCard.vue'
import type { ProfileView } from '@/api/profile'

/**
 * Progressive disclosure (Financial GPS UX): the facts are readable by default and the editable
 * fields only appear on "Edit" — still using the existing field names, ids and PUT contract.
 */
function view(overrides: Partial<ProfileView> = {}): ProfileView {
  return {
    currency: 'VND',
    savingsAmount: '100.00',
    emergencyFundAmount: '50.00',
    dependentsCount: 2,
    incomes: [],
    expenses: [],
    totalIncome: { amount: '0.00', currency: 'VND', provenance: 'calculated' },
    totalExpenses: { amount: '0.00', currency: 'VND', provenance: 'calculated' },
    netCashFlow: { amount: '0.00', currency: 'VND', provenance: 'calculated' },
    availableCapacity: { amount: '0.00', currency: 'VND', provenance: 'calculated' },
    provenance: [],
    asOf: '2026-09-16',
    ...overrides,
  }
}

describe('FinancialBasicsCard', () => {
  it('reads the stored facts without showing a single input', () => {
    const wrapper = mount(FinancialBasicsCard, {
      props: { view: view(), saving: false, error: '' },
    })

    expect(wrapper.text()).toContain('Your financial basics')
    expect(wrapper.text()).toContain('Savings')
    expect(wrapper.text()).toContain('100,00')
    expect(wrapper.text()).toContain('Emergency fund')
    expect(wrapper.text()).toContain('50,00')
    expect(wrapper.text()).toContain('People depending on you')
    expect(wrapper.text()).toContain('2')
    expect(wrapper.find('input').exists()).toBe(false)
    expect(wrapper.text()).toContain('Values you entered, in VND')
  })

  it('reveals the existing fields with the stored values on Edit', async () => {
    const wrapper = mount(FinancialBasicsCard, {
      props: { view: view(), saving: false, error: '' },
    })

    await wrapper.find('[data-testid="basics-edit"]').trigger('click')

    expect((wrapper.find('#savings').element as HTMLInputElement).value).toBe('100,00')
    expect((wrapper.find('#emergency').element as HTMLInputElement).value).toBe('50,00')
    expect((wrapper.find('#dependents').element as HTMLInputElement).value).toBe('2')
    // Labels stay associated with their controls.
    expect(wrapper.find('label[for="savings"]').exists()).toBe(true)
    expect(wrapper.find('label[for="emergency"]').exists()).toBe(true)
    expect(wrapper.find('label[for="dependents"]').exists()).toBe(true)
  })

  it('sends the untouched decimal strings through the existing save event', async () => {
    const wrapper = mount(FinancialBasicsCard, {
      props: { view: view(), saving: false, error: '' },
    })

    await wrapper.find('[data-testid="basics-edit"]').trigger('click')
    await wrapper.find('#savings').setValue('99,10')
    await wrapper.find('.btn').trigger('click')

    expect(wrapper.emitted('save')![0]).toEqual([
      { currency: 'VND', savingsAmount: '99.10', emergencyFundAmount: '50.00', dependentsCount: 2 },
    ])
  })

  it('accepts a VND-formatted entry and sends the plain decimal string', async () => {
    const wrapper = mount(FinancialBasicsCard, {
      props: { view: view(), saving: false, error: '' },
    })

    await wrapper.find('[data-testid="basics-edit"]').trigger('click')
    await wrapper.find('#savings').setValue('100.000.000,50')
    await wrapper.find('.btn').trigger('click')

    expect(wrapper.emitted('save')![0]).toEqual([
      {
        currency: 'VND',
        savingsAmount: '100000000.50',
        emergencyFundAmount: '50.00',
        dependentsCount: 2,
      },
    ])
  })

  it('blocks an empty amount locally instead of sending it to the API', async () => {
    const wrapper = mount(FinancialBasicsCard, {
      props: { view: view(), saving: false, error: '' },
    })

    await wrapper.find('[data-testid="basics-edit"]').trigger('click')
    await wrapper.find('#savings').setValue('')
    await wrapper.find('.btn').trigger('click')

    expect(wrapper.emitted('save')).toBeUndefined()
    expect(wrapper.find('[role="alert"]').text()).toContain(
      'Enter savings and emergency fund with digits and up to 2 decimals.',
    )
  })

  it('can be opened by the page when a mutation reveals a missing profile record', async () => {
    const wrapper = mount(FinancialBasicsCard, {
      props: { view: view(), saving: false, error: '' },
    })

    ;(wrapper.vm as unknown as { startEdit: () => void }).startEdit()
    await nextTick()

    expect(wrapper.find('#savings').exists()).toBe(true)
  })
})
