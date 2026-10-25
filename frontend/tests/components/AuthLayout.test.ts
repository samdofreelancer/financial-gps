import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AuthLayout from '@/components/AuthLayout.vue'

/** Render router-links as plain anchors so the brand lockup inside stays inspectable. */
const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="String(to)"><slot /></a>',
}

/**
 * The sign-in frame reuses the signed-in shell's chrome: the white topbar with the
 * brand, the slate canvas and one white card carrying the form.
 */
describe('AuthLayout', () => {
  it('renders the topbar brand, the heading and the slotted form', () => {
    const wrapper = mount(AuthLayout, {
      props: { title: 'Sign in', subtitle: 'Sign in to keep tracking your financial GPS.' },
      slots: { default: '<input id="email" class="input" />' },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    expect(wrapper.find('.auth-card').exists()).toBe(true)
    expect(wrapper.find('.auth-bar').exists()).toBe(true)
    expect(wrapper.find('.auth-bar__brand .brand-lockup__name').text()).toBe('Financial GPS')
    expect(wrapper.find('.auth-form__title').text()).toBe('Sign in')
    expect(wrapper.find('.auth-form__subtitle').text()).toContain('financial GPS')
    expect(wrapper.find('input#email').exists()).toBe(true)
    expect(wrapper.find('.auth-utilities').exists()).toBe(true)
  })

  it('drops the subtitle when the screen does not pass one', () => {
    const wrapper = mount(AuthLayout, {
      props: { title: 'Create your account' },
      slots: { default: '<p>form</p>' },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    expect(wrapper.find('.auth-form__title').text()).toBe('Create your account')
    expect(wrapper.find('.auth-form__subtitle').exists()).toBe(false)
  })
})