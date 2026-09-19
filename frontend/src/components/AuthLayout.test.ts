import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AuthLayout from './AuthLayout.vue'

/** The sign-in frame mirrors the MISA reference: brand panel left, form column right. */
describe('AuthLayout', () => {
  it('renders the brand panel, the heading and the slotted form', () => {
    const wrapper = mount(AuthLayout, {
      props: { title: 'Sign in', subtitle: 'Sign in to keep tracking your financial GPS.' },
      slots: { default: '<input id="email" class="input" />' },
    })

    expect(wrapper.find('.auth-card').exists()).toBe(true)
    expect(wrapper.find('.auth-brand__name').text()).toBe('Financial GPS')
    expect(wrapper.find('.auth-form__title').text()).toBe('Sign in')
    expect(wrapper.find('.auth-form__subtitle').text()).toContain('financial GPS')
    expect(wrapper.find('input#email').exists()).toBe(true)
    expect(wrapper.find('.auth-utilities').exists()).toBe(true)
  })

  it('drops the subtitle when the screen does not pass one', () => {
    const wrapper = mount(AuthLayout, {
      props: { title: 'Create your account' },
      slots: { default: '<p>form</p>' },
    })

    expect(wrapper.find('.auth-form__title').text()).toBe('Create your account')
    expect(wrapper.find('.auth-form__subtitle').exists()).toBe(false)
  })
})