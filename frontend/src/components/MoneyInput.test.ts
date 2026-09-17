import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MoneyInput from './MoneyInput.vue'

/**
 * 001 (F3): the money input is a *string* model. It must never coerce the typed value through a
 * JavaScript number, otherwise 0.1 + 0.2 / large amounts would lose the decimal truth.
 */
describe('MoneyInput', () => {
  it('emits the typed value as an untouched string', async () => {
    const wrapper = mount(MoneyInput, { props: { id: 'savings', modelValue: '0.10' } })

    await wrapper.find('input').setValue('0.30')

    expect(wrapper.emitted('update:modelValue')).toEqual([['0.30']])
  })

  it('keeps leading zeros and trailing decimals exactly as typed', async () => {
    const wrapper = mount(MoneyInput, { props: { id: 'savings', modelValue: '' } })

    await wrapper.find('input').setValue('007.10')

    expect(wrapper.emitted('update:modelValue')![0]).toEqual(['007.10'])
  })

  it('renders the label, the value, the decimal input mode and the hint', () => {
    const wrapper = mount(MoneyInput, {
      props: {
        id: 'savings',
        modelValue: '19.99',
        label: 'Liquid savings',
        hint: 'Liquid savings (actual).',
      },
    })

    const input = wrapper.find('input')
    expect((input.element as HTMLInputElement).value).toBe('19.99')
    expect(input.attributes('inputmode')).toBe('decimal')
    expect(wrapper.find('label').text()).toBe('Liquid savings')
    expect(wrapper.text()).toContain('Liquid savings (actual).')
  })
})
