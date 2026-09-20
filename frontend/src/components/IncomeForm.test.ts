import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import IncomeForm from './IncomeForm.vue'
import type { ProfileLine } from '../api/profile'

function line(overrides: Partial<ProfileLine> = {}): ProfileLine {
  return { id: 'i1', amount: '74.00', currency: 'VND', source: 'salary', provenance: 'actual', ...overrides }
}

describe('IncomeForm', () => {
  it('starts empty for a new income and emits the raw strings', async () => {
    const wrapper = mount(IncomeForm, { props: { error: '' } })

    expect(wrapper.text()).toContain('Add income')
    expect((wrapper.find('#income-amount').element as HTMLInputElement).value).toBe('')
    expect(wrapper.find('label[for="income-source"]').exists()).toBe(true)

    await wrapper.find('#income-amount').setValue('12.10')
    await wrapper.find('#income-source').setValue('interest')
    await wrapper.findAll('button').find((b) => b.text() === 'Add income')!.trigger('click')

    expect(wrapper.emitted('submit')![0]).toEqual([{ amount: '12.10', source: 'interest' }])
  })

  it('prefills the stored record when editing', async () => {
    const wrapper = mount(IncomeForm, { props: { line: line(), error: '' } })

    expect(wrapper.text()).toContain('Edit income')
    expect((wrapper.find('#income-amount').element as HTMLInputElement).value).toBe('74.00')
    expect((wrapper.find('#income-source').element as HTMLInputElement).value).toBe('salary')

    await wrapper.findAll('button').find((b) => b.text() === 'Update income')!.trigger('click')

    expect(wrapper.emitted('submit')![0]).toEqual([{ amount: '74.00', source: 'salary' }])
  })

  it('shows the server error and stays open, and lets the user cancel', async () => {
    const wrapper = mount(IncomeForm, { props: { error: 'Income needs a decimal amount and a source.' } })

    expect(wrapper.find('[role="alert"]').text()).toContain('Income needs a decimal amount')

    await wrapper.findAll('button').find((b) => b.text() === 'Cancel')!.trigger('click')

    expect(wrapper.emitted('cancel')).toHaveLength(1)
  })
})
