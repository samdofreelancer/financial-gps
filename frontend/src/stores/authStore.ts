import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '../api/auth'
import type { AuthAccount } from '../api/auth'

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

  const isAuthenticated = computed(() => account.value !== null)

  async function restore(): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      account.value = await authApi.me()
    } catch {
      // No/invalid session is the normal anonymous state — not an error box.
      account.value = null
    } finally {
      loading.value = false
      ready.value = true
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
    isAuthenticated,
    restore,
    login,
    register,
    logout,
  }
})

