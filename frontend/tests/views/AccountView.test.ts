import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AccountView from '@/views/AccountView.vue'

vi.mock('@/stores/authStore', () => ({ useAuthStore: vi.fn() }))

import { useAuthStore } from '@/stores/authStore'

function testRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/account', name: 'account', component: { template: '<div />' } },
      { path: '/login', name: 'login', component: { template: '<div />' } },
    ],
  })
  return router
}

describe('AccountView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('shows the signed-in email and logs out back to login', async () => {
    const logout = vi.fn().mockResolvedValue(undefined)
    vi.mocked(useAuthStore).mockReturnValue({
      account: { id: '1', email: 'a@example.com' },
      loading: false,
      logout,
    } as never)
    const router = testRouter()
    await router.push('/account')
    await router.isReady()

    const wrapper = mount(AccountView, { global: { plugins: [createPinia(), router] } })
    expect(wrapper.text()).toContain('a@example.com')

    await wrapper.find('button').trigger('click')
    await flushPromises()
    expect(logout).toHaveBeenCalledOnce()
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('shows the anonymous state when there is no account', () => {
    vi.mocked(useAuthStore).mockReturnValue({
      account: null,
      loading: false,
      logout: vi.fn(),
    } as never)
    const wrapper = mount(AccountView, {
      global: { plugins: [createPinia(), testRouter()] },
    })
    expect(wrapper.text()).toContain('Not signed in')
  })
})
