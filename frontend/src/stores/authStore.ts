import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '../api/auth'
import type { AuthAccount } from '../api/auth'
import { isAuthRequired, problemMessage } from '../api/http'

/**
 * Real session authentication (007). The backend owns the truth via the
 * HttpOnly `SESSION` cookie (Spring Session JDBC): this store only mirrors
 * what `GET /api/v1/account/me` proves. No localStorage truth, no demo
 * account, no fake login.
 */
export const useAuthStore = defineStore('auth', () => {
  const account = ref<AuthAccount | null>(null)
  const ready = ref(false)
  const loading = ref(false)
  const error = ref('')

  /**
   * True when the last probe could not decide anything because the backend or the network
   * failed. The session is UNKNOWN in that state — it is not anonymous (see `restore`).
   */
  const unavailable = ref(false)

  /**
   * One session probe shared by every caller. On a hard reload the router
   * guard and App startup both ask for the session; that must stay a single
   * GET /api/v1/account/me, so concurrent callers await the same request.
   *
   * Only 401 means "anonymous". Any other failure (5xx, offline, DNS, proxy)
   * leaves the session UNKNOWN and sets `unavailable`: silently collapsing an
   * infrastructure outage into "logged out" sends everyone hunting for a
   * session bug while the backend is actually broken.
   */
  let inflight: Promise<void> | null = null

  const isAuthenticated = computed(() => account.value !== null)

  async function restore(): Promise<void> {
    if (inflight) {
      return inflight
    }
    inflight = (async () => {
      loading.value = true
      error.value = ''
      unavailable.value = false
      try {
        account.value = await authApi.me()
      } catch (caught) {
        if (isAuthRequired(caught)) {
          // 401 is the normal anonymous state — not an error box.
          account.value = null
        } else {
          // The session is unknown, not absent. Keep the outage visible and
          // let the shell explain it; the server text stays authoritative.
          unavailable.value = true
          error.value = problemMessage(caught, 'Unable to restore your session.')
        }
      } finally {
        loading.value = false
        ready.value = true
      }
    })()
    try {
      await inflight
    } finally {
      inflight = null
    }
  }

  async function login(email: string, password: string): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      account.value = await authApi.login({ email, password })
    } finally {
      loading.value = false
    }
  }

  async function register(email: string, password: string): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      account.value = await authApi.register({ email, password })
    } finally {
      loading.value = false
    }
  }

  async function logout(): Promise<void> {
    loading.value = true
    try {
      await authApi.logout()
    } catch {
      // Logout is idempotent client-side: the session is gone or was already
      // gone; either way the local account must be cleared.
    } finally {
      account.value = null
      loading.value = false
    }
  }

  return {
    account,
    ready,
    loading,
    error,
    unavailable,
    isAuthenticated,
    restore,
    login,
    register,
    logout,
  }
})

