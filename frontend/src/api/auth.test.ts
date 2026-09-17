import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import * as authApi from './auth'
import { problemMessage, isAuthRequired } from './http'

vi.mock('./auth', () => ({
  register: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  me: vi.fn(),
}))

describe('auth api contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('register posts the real 007 payload and returns the server account', async () => {
    vi.mocked(authApi.register).mockResolvedValue({
      id: '1',
      email: 'a@example.com',
      createdAt: '2026-09-17T00:00:00Z',
    })
    const account = await authApi.register({ email: 'a@example.com', password: 'correct horse battery1' })
    expect(authApi.register).toHaveBeenCalledWith({
      email: 'a@example.com',
      password: 'correct horse battery1',
    })
    expect(account.email).toBe('a@example.com')
  })

  it('maps INVALID_CREDENTIALS without inventing meanings', () => {
    const error = { response: { status: 401, data: { code: 'INVALID_CREDENTIALS' } } }
    expect(problemMessage(error, 'fallback')).toBe('Email or password is incorrect.')
    expect(isAuthRequired(error)).toBe(true)
  })

  it('treats AUTH_REQUIRED as the expected anonymous state', () => {
    const error = { response: { status: 401, data: { code: 'AUTH_REQUIRED' } } }
    expect(problemMessage(error, 'fallback')).toBe('Sign in to access this resource.')
    expect(isAuthRequired(error)).toBe(true)
  })
})

describe('auth store (session truth)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('starts anonymous and restores from GET /account/me', async () => {
    const { useAuthStore } = await import('../stores/authStore')
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', email: 'a@example.com' })
    const store = useAuthStore()
    expect(store.account).toBeNull()
    expect(store.isAuthenticated).toBe(false)
    await store.restore()
    expect(authApi.me).toHaveBeenCalledTimes(1)
    expect(store.account?.email).toBe('a@example.com')
    expect(store.isAuthenticated).toBe(true)
    expect(store.ready).toBe(true)
  })

  it('shares one GET /account/me when the guard and App startup restore at once', async () => {
    const { useAuthStore } = await import('../stores/authStore')
    let release!: (account: authApi.AuthAccount) => void
    vi.mocked(authApi.me).mockReturnValue(
      new Promise<authApi.AuthAccount>((resolve) => {
        release = resolve
      }),
    )
    const store = useAuthStore()
    const fromGuard = store.restore()
    const fromAppStartup = store.restore()
    release({ id: '1', email: 'a@example.com' })
    await Promise.all([fromGuard, fromAppStartup])
    expect(authApi.me).toHaveBeenCalledTimes(1)
    expect(store.account?.email).toBe('a@example.com')
    expect(store.ready).toBe(true)
  })

  it('probes the session again after a completed restore', async () => {
    const { useAuthStore } = await import('../stores/authStore')
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', email: 'a@example.com' })
    const store = useAuthStore()
    await store.restore()
    await store.restore()
    expect(authApi.me).toHaveBeenCalledTimes(2)
  })

  it('anonymous restore stays logged out without an error box', async () => {
    const { useAuthStore } = await import('../stores/authStore')
    vi.mocked(authApi.me).mockRejectedValue({ response: { status: 401 } })
    const store = useAuthStore()
    await store.restore()
    expect(store.account).toBeNull()
    expect(store.isAuthenticated).toBe(false)
    expect(store.ready).toBe(true)
  })

  it('logout clears the account even when the server session is already gone', async () => {
    const { useAuthStore } = await import('../stores/authStore')
    vi.mocked(authApi.me).mockResolvedValue({ id: '1', email: 'a@example.com' })
    vi.mocked(authApi.logout).mockRejectedValue({ response: { status: 401 } })
    const store = useAuthStore()
    await store.restore()
    expect(store.isAuthenticated).toBe(true)
    await store.logout()
    expect(store.account).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })
})
