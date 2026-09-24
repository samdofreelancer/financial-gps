import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BrandLockup from '@/components/BrandLockup.vue'

describe('BrandLockup', () => {
  it('renders the product name next to the decorative mark', () => {
    const wrapper = mount(BrandLockup)

    expect(wrapper.classes()).toContain('brand-lockup')
    expect(wrapper.classes()).not.toContain('sm')
    expect(wrapper.find('.brand-lockup__mark svg').exists()).toBe(true)
    expect(wrapper.find('.brand-lockup__name').text()).toBe('Financial GPS')
  })

  it('supports the compact size used by the top bar', () => {
    const wrapper = mount(BrandLockup, { props: { size: 'sm' } })

    expect(wrapper.classes()).toContain('sm')
  })
})