import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ExpenseForm from './ExpenseForm.vue'
import type { ProfileLine } from '../api/profile'

function line(overrides: Partial<ProfileLine> = {}): ProfileLine {
  return {
    id: 'e1',
    amount: '30.00',
    currency: 'VND',
    category: 'rent',
    expenseType: 'FIXED',
    provenance: 'actual',
    ...overrides,
  }
}

describe('ExpenseForm', () => {
  it('emits the existing contract body untouched', async () => {
    const wrapper = mount(ExpenseForm, { props: { error: '' } })

    await wrapper.find('#expense-amount').setValue('12.00')
    await wrapper.find('#expense-category').setValue('food')
    await wrapper.find('#expense-type').setValue('VARIABLE')
    await wrapper.findAll('button').find((b) => b.text() === 'Add expense')!.trigger('click')

    expect(wrapper.emitted('submit')![0]).toEqual([
      { amount: '12.00', category: 'food', expenseType: 'VARIABLE' },
    ])
  })

  it('prefills an existing expense, including its stored type', async () => {
    const wrapper = mount(ExpenseForm, { props: { line: line(), error: '' } })

    expect(wrapper.text()).toContain('Edit expense')
    expect((wrapper.find('#expense-amount').element as HTMLInputElement).value).toBe('30.00')
    expect((wrapper.find('#expense-category').element as HTMLInputElement).value).toBe('rent')
    expect((wrapper.find('#expense-type').element as HTMLSelectElement).value).toBe('FIXED')

    await wrapper.findAll('button').find((b) => b.text() === 'Update expense')!.trigger('click')

    expect(wrapper.emitted('submit')![0]).toEqual([
      { amount: '30.00', category: 'rent', expenseType: 'FIXED' },
    ])
  })

  it('defaults a new expense to Fixed and labels every field', () => {
    const wrapper = mount(ExpenseForm, { props: { error: '' } })

    expect((wrapper.find('#expense-type').element as HTMLSelectElement).value).toBe('FIXED')
    expect(wrapper.find('label[for="expense-amount"]').exists()).toBe(true)
    expect(wrapper.find('label[for="expense-category"]').exists()).toBe(true)
    expect(wrapper.find('label[for="expense-type"]').exists()).toBe(true)
  })
})
