import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const account = ref<{ email?: string } | null>({
    email: 'demo@financialgps.local',
  })

  const isAuthenticated = computed(() => account.value !== null)

  async function login(email: string) {
    account.value = { email }
  }

  async function logout() {
    account.value = null
  }

  return {
    account,
    isAuthenticated,
    login,
    logout,
  }
})
