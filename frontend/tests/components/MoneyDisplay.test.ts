import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MoneyDisplay from '@/components/MoneyDisplay.vue'

/**
 * 001 (F3): display formatting only — the amount string always comes from the server and is
 * rendered verbatim (grouped, two decimals), labelled actual or calculated.
 */
describe('MoneyDisplay', () => {
  it('renders the server amount with its provenance label', () => {
    const wrapper = mount(MoneyDisplay, {
      props: { amount: '1234.50', currency: 'VND', provenance: 'calculated' },
    })

    expect(wrapper.text()).toContain('1.234,50')
    expect(wrapper.text()).toContain('VND')
    expect(wrapper.text()).toContain('calculated')
  })

  it('labels user-supplied facts as actual', () => {
    const wrapper = mount(MoneyDisplay, {
      props: { amount: '74.00', currency: 'VND', provenance: 'actual' },
    })

    expect(wrapper.text()).toContain('74,00')
    expect(wrapper.text()).toContain('actual')
  })

  it('does not lose precision for amounts beyond the JS safe-integer range', () => {
    const wrapper = mount(MoneyDisplay, {
      props: { amount: '99999999999999999.99', currency: 'VND', provenance: 'calculated' },
    })

    expect(wrapper.text()).toContain('99.999.999.999.999.999,99')
  })

  it('renders a negative Net Cash Flow exactly as the server reported it', () => {
    const wrapper = mount(MoneyDisplay, {
      props: { amount: '-44.00', currency: 'VND', provenance: 'calculated' },
    })

    expect(wrapper.text()).toContain('-44,00')
  })
})
