import { flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../api/auth'

vi.mock('../api/auth', () => ({
  register: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  me: vi.fn(),
}))

describe('protected route guard (session truth)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.resetModules()
  })

  async function freshRouter() {
    const { default: router } = await import('../router/index')
    return router
  }

  it('redirects anonymous /profile to /login?redirect=/profile', async () => {
    vi.mocked(authApi.me).mockRejectedValue({ response: { status: 401 } })
    const router = await freshRouter()
    await router.push('/profile')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.redirect).toBe('/profile')
  })

  it('lets an authenticated session reach /profile', async () => {
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', email: 'a@example.com' })
    const router = await freshRouter()
    await router.push('/profile')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/profile')
  })

  it('keeps authenticated users out of /login', async () => {
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', email: 'a@example.com' })
    const router = await freshRouter()
    await router.push('/login')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/profile')
  })

  it('never mistakes an unavailable backend for a signed-out visitor', async () => {
    vi.mocked(authApi.me).mockRejectedValue({
      response: { status: 502, data: { title: 'Bad gateway' } },
    })
    const router = await freshRouter()
    const { useAuthStore } = await import('../stores/authStore')

    await router.push('/profile')
    await flushPromises()

    // No misleading bounce to /login while the backend is down: the session is
    // unknown, so the route renders and the shell explains the outage.
    expect(router.currentRoute.value.path).toBe('/profile')
    expect(useAuthStore().unavailable).toBe(true)
  })
})
