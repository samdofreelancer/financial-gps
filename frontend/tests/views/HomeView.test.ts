import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { describe, expect, it } from 'vitest'
import HomeView from '@/views/HomeView.vue'

async function mountHome() {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'home', component: HomeView },
      { path: '/login', name: 'login', component: { template: '<div />' } },
      { path: '/register', name: 'register', component: { template: '<div />' } },
    ],
  })
  await router.push('/')
  await router.isReady()
  const wrapper = mount(HomeView, { global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}

/** The public landing sells the product without inventing any numbers. */
describe('HomeView (public landing)', () => {
  it('renders the hero promise, CTAs and feature tiles', async () => {
    const wrapper = await mountHome()

    expect(wrapper.find('.landing__title').text()).toContain('personal finance system')
    expect(wrapper.find('.landing__bar').text()).toContain('Log in')
    expect(wrapper.find('.landing__signup').text()).toBe('Sign up')
    expect(wrapper.findAll('.landing__feature')).toHaveLength(4)
    expect(wrapper.findAll('.landing__why-card')).toHaveLength(3)
  })

  it('links the CTAs to sign up and log in', async () => {
    const wrapper = await mountHome()

    const hrefs = wrapper.findAll('a').map((a) => a.attributes('href'))
    expect(hrefs).toContain('/register')
    expect(hrefs).toContain('/login')
  })

  it('keeps the footer promise', async () => {
    const wrapper = await mountHome()
    expect(wrapper.find('.landing__footer').text()).toContain('Không quảng cáo')
  })
})
