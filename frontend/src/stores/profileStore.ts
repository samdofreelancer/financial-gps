import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  deleteExpense,
  deleteIncome,
  getProfile,
  postExpense,
  postIncome,
  putExpense,
  putIncome,
  putProfile,
  type ProfileView,
} from '../api/profile'
import { problemMessage } from '../api/http'

/**
 * Server state only. No financial formulas here: totals are rendered
 * from the server response and refreshed after every mutation.
 */
export const useProfileStore = defineStore('profile', () => {
  const profile = ref<ProfileView | null>(null)
  const loading = ref(false)
  const error = ref('')

  async function refresh(): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      profile.value = await getProfile()
    } catch (caught) {
      // One error vocabulary for the whole app: the RFC 7807 body decides the text.
      error.value = problemMessage(caught, 'Could not load the financial profile.')
    } finally {
      loading.value = false
    }
  }

  async function saveProfile(body: {
    currency: string
    savingsAmount: string
    emergencyFundAmount: string
    dependentsCount: number
  }): Promise<void> {
    profile.value = await putProfile(body)
  }

  async function addIncome(body: { amount: string; source: string }): Promise<void> {
    await postIncome(body)
    await refresh()
  }

  async function updateIncome(id: string, body: { amount: string; source: string }): Promise<void> {
    await putIncome(id, body)
    await refresh()
  }

  async function removeIncome(id: string): Promise<void> {
    await deleteIncome(id)
    await refresh()
  }

  async function addExpense(body: {
    amount: string
    category: string
    expenseType: 'FIXED' | 'VARIABLE'
  }): Promise<void> {
    await postExpense(body)
    await refresh()
  }

  async function updateExpense(
    id: string,
    body: { amount: string; category: string; expenseType: 'FIXED' | 'VARIABLE' },
  ): Promise<void> {
    await putExpense(id, body)
    await refresh()
  }

  async function removeExpense(id: string): Promise<void> {
    await deleteExpense(id)
    await refresh()
  }

  return {
    profile,
    loading,
    error,
    refresh,
    saveProfile,
    addIncome,
    updateIncome,
    removeIncome,
    addExpense,
    updateExpense,
    removeExpense,
  }
})
