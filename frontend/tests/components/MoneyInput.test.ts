import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MoneyInput from '@/components/MoneyInput.vue'

/**
 * Spec 001 (T014): the field shows a currency amount ('.' groups thousands, ',' separates two
decimals) but the model stays an untouched decimal string, and only digits can ever reach it.
 */
describe('MoneyInput', () => {
  it('shows the model as a VND amount, with the currency inside the field', () => {
    const wrapper = mount(MoneyInput, { props: { id: 'savings', modelValue: '30000000.5' } })

    const input = wrapper.find('input')
    expect((input.element as HTMLInputElement).value).toBe('30.000.000,50')
    expect(input.attributes('inputmode')).toBe('decimal')
    expect(wrapper.find('.money-currency').text()).toBe('VND')
    expect(input.attributes('placeholder')).toBe('0,00')
  })

  it('groups while typing and emits the decimal string the API expects', async () => {
    const wrapper = mount(MoneyInput, { props: { id: 'income-amount', modelValue: '' } })

    const input = wrapper.find('input')
    await input.setValue('30000000')
    expect((input.element as HTMLInputElement).value).toBe('30.000.000')
    expect(wrapper.emitted('update:modelValue')!.at(-1)).toEqual(['30000000'])

    await input.setValue('30.000.000,25')
    expect((input.element as HTMLInputElement).value).toBe('30.000.000,25')
    expect(wrapper.emitted('update:modelValue')!.at(-1)).toEqual(['30000000.25'])
  })

  it('drops letters, signs and pasted noise instead of sending them to the API', async () => {
    const wrapper = mount(MoneyInput, { props: { id: 'savings', modelValue: '' } })

    const input = wrapper.find('input')
    await input.setValue('abc-12x')
    expect((input.element as HTMLInputElement).value).toBe('12')
    expect(wrapper.emitted('update:modelValue')!.at(-1)).toEqual(['12'])

    await input.setValue('-5')
    expect((input.element as HTMLInputElement).value).toBe('5')
    expect(wrapper.emitted('update:modelValue')!.at(-1)).toEqual(['5'])
  })

  it('keeps at most two decimals', async () => {
    const wrapper = mount(MoneyInput, { props: { id: 'savings', modelValue: '' } })

    const input = wrapper.find('input')
    await input.setValue('1,239')

    expect((input.element as HTMLInputElement).value).toBe('1,23')
    expect(wrapper.emitted('update:modelValue')!.at(-1)).toEqual(['1.23'])
  })

  it('pads to two decimals on blur, trims them on focus, and never rewrites the model', async () => {
    const wrapper = mount(MoneyInput, { props: { id: 'savings', modelValue: '30000000' } })

    const input = wrapper.find('input')
    await input.trigger('blur')
    expect((input.element as HTMLInputElement).value).toBe('30.000.000,00')

    await input.trigger('focus')
    expect((input.element as HTMLInputElement).value).toBe('30.000.000')

    // Formatting is presentation only: the model keeps the value the user entered.
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })

  it('keeps 40.000.000 intact when the user types the dots themselves', async () => {
    const wrapper = mount(MoneyInput, { props: { id: 'income-amount', modelValue: '' } })

    const input = wrapper.find('input')
    // Regression: a dot typed mid-amount used to be read as a decimal point, collapsing the value.
    for (const text of ['4', '40', '40.', '40.0', '40.00', '40.000', '40.000.000']) {
      await input.setValue(text)
    }

    expect((input.element as HTMLInputElement).value).toBe('40.000.000')
    expect(wrapper.emitted('update:modelValue')!.at(-1)).toEqual(['40000000'])
  })

  it('reads a comma as the decimal separator', async () => {
    const wrapper = mount(MoneyInput, { props: { id: 'savings', modelValue: '' } })

    const input = wrapper.find('input')
    await input.setValue('74,5')

    expect((input.element as HTMLInputElement).value).toBe('74,5')
    expect(wrapper.emitted('update:modelValue')!.at(-1)).toEqual(['74.5'])
  })

  it('follows an external model change and clears when the parent resets the field', async () => {
    const wrapper = mount(MoneyInput, { props: { id: 'savings', modelValue: '12.00' } })

    const input = wrapper.find('input')
    expect((input.element as HTMLInputElement).value).toBe('12,00')

    await wrapper.setProps({ modelValue: '1000000' })
    expect((input.element as HTMLInputElement).value).toBe('1.000.000,00')

    await wrapper.setProps({ modelValue: '' })
    expect((input.element as HTMLInputElement).value).toBe('')
  })

  it('shows the validation message, ties it to the field and keeps the label associated', () => {
    const wrapper = mount(MoneyInput, {
      props: {
        id: 'savings',
        modelValue: '',
        label: 'Liquid savings',
        error: 'Enter an amount with digits and up to 2 decimals.',
      },
    })

    const input = wrapper.find('input')
    expect(input.attributes('aria-invalid')).toBe('true')
    expect(input.attributes('aria-describedby')).toBe('savings-error')
    expect(wrapper.find('#savings-error').text()).toBe('Enter an amount with digits and up to 2 decimals.')
    expect(wrapper.find('#savings-error').attributes('role')).toBe('alert')
    expect(wrapper.find('label[for="savings"]').text()).toBe('Liquid savings')
  })

  it('renders the hint instead of the error when the field is valid', () => {
    const wrapper = mount(MoneyInput, {
      props: { id: 'savings', modelValue: '19.99', label: 'Savings', hint: 'Liquid savings (actual).' },
    })

    const input = wrapper.find('input')
    expect((input.element as HTMLInputElement).value).toBe('19,99')
    expect(input.attributes('aria-describedby')).toBe('savings-hint')
    expect(wrapper.text()).toContain('Liquid savings (actual).')
    expect(wrapper.find('.field-error').exists()).toBe(false)
  })
})
