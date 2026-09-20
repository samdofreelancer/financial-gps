import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import IncomeForm from './IncomeForm.vue'
import type { ProfileLine } from '../api/profile'

function line(overrides: Partial<ProfileLine> = {}): ProfileLine {
  return { id: 'i1', amount: '74.00', currency: 'VND', source: 'salary', provenance: 'actual', ...overrides }
}

describe('IncomeForm', () => {
  it('starts empty for a new income and emits the raw decimal string', async () => {
    const wrapper = mount(IncomeForm, { props: { error: '' } })

    expect(wrapper.text()).toContain('Add income')
    expect((wrapper.find('#income-amount').element as HTMLInputElement).value).toBe('')
    expect(wrapper.find('label[for="income-source"]').exists()).toBe(true)
    expect((wrapper.find('#income-source').element as HTMLSelectElement).value).toBe('')

    await wrapper.find('#income-amount').setValue('12,10')
    await wrapper.find('#income-source').setValue('freelance')
    await wrapper.findAll('button').find((b) => b.text() === 'Add income')!.trigger('click')

    expect((wrapper.find('#income-amount').element as HTMLInputElement).value).toBe('12,10')
    expect(wrapper.emitted('submit')![0]).toEqual([{ amount: '12.10', source: 'freelance' }])
  })

  it('shows the amount the Vietnamese way while sending the untouched decimal string', async () => {
    const wrapper = mount(IncomeForm, { props: { error: '' } })

    await wrapper.find('#income-amount').setValue('30.000.000,5')
    await wrapper.find('#income-source').setValue('salary')
    await wrapper.findAll('button').find((b) => b.text() === 'Add income')!.trigger('click')

    expect((wrapper.find('#income-amount').element as HTMLInputElement).value).toBe('30.000.000,5')
    expect(wrapper.emitted('submit')![0]).toEqual([{ amount: '30000000.5', source: 'salary' }])
  })

  it('blocks a missing amount with a message next to the field, without emitting', async () => {
    const wrapper = mount(IncomeForm, { props: { error: '' } })

    await wrapper.find('#income-source').setValue('salary')
    await wrapper.findAll('button').find((b) => b.text() === 'Add income')!.trigger('click')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.find('[role="alert"]').text()).toContain(
      'Enter an amount with digits and up to 2 decimals.',
    )
  })

  it('flags the source control itself when no source was chosen', async () => {
    const wrapper = mount(IncomeForm, { props: { error: '' } })

    await wrapper.find('#income-amount').setValue('30000000')
    await wrapper.findAll('button').find((b) => b.text() === 'Add income')!.trigger('click')

    const select = wrapper.find('#income-source')
    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(select.attributes('aria-invalid')).toBe('true')
    expect(select.attributes('aria-describedby')).toBe('income-source-error')
    expect(wrapper.find('#income-source-error').text()).toBe('Choose a source for this income.')
  })

  it('prefills the stored record when editing', async () => {
    const wrapper = mount(IncomeForm, { props: { line: line(), error: '' } })

    expect(wrapper.text()).toContain('Edit income')
    expect((wrapper.find('#income-amount').element as HTMLInputElement).value).toBe('74,00')
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
