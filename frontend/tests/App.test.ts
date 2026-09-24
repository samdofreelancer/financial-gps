import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/auth'
import App from '@/App.vue'

vi.mock('@/api/auth', () => ({
  register: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  me: vi.fn(),
}))

function testRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>home</div>' } },
      { path: '/login', name: 'login', component: { template: '<div>login</div>' } },
      { path: '/account', name: 'account', component: { template: '<div>account</div>' } },
      { path: '/dashboard', name: 'dashboard', component: { template: '<div>dashboard</div>' } },
      { path: '/profile', name: 'profile', component: { template: '<div>profile</div>' } },
    ],
  })
}

async function mountApp() {
  const router = testRouter()
  await router.push('/')
  await router.isReady()
  const wrapper = mount(App, { global: { plugins: [createPinia(), router] } })
  await flushPromises()
  return wrapper
}

/**
 * 007 keeps the backend authoritative, so the shell must never answer a question the backend
 * did not answer: "who am I?" is only known when GET /account/me actually replied.
 */
describe('App shell (session truth)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('surfaces an outage instead of pretending the visitor is signed out', async () => {
    vi.mocked(authApi.me).mockRejectedValue(new Error('network down'))

    const wrapper = await mountApp()

    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Could not reach the server')
    expect(wrapper.find('.topbar').exists()).toBe(false)
  })

  it('stays quiet when the visitor simply has no session', async () => {
    vi.mocked(authApi.me).mockRejectedValue({
      response: { status: 401, data: { code: 'AUTH_REQUIRED' } },
    })

    const wrapper = await mountApp()

    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.find('.topbar').exists()).toBe(false)
  })

  it('shows the signed-in account, the left sidebar and hides the outage banner', async () => {
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', email: 'a@example.com' })

    const wrapper = await mountApp()

    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.find('.topbar').text()).toContain('a@example.com')
    expect(wrapper.find('.sidebar').exists()).toBe(true)
    expect(wrapper.find('.sidebar').text()).toContain('Dashboard')
    // Account + Log out live in the topbar identity chip menu, not the sidebar.
    expect(wrapper.find('.avatar-menu__trigger').exists()).toBe(true)
    expect(wrapper.find('.sidebar').text()).not.toContain('Account')
  })
})
