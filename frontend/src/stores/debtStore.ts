import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  deleteDebt,
  getDebtSummary,
  listDebts,
  postDebt,
  putDebt,
  type DebtPayload,
  type DebtSummary,
  type DebtView,
} from '../api/debts'
import { problemMessage } from '../api/http'

/**
 * Server state only. No amortization here: projections, DTI and totals are
 * rendered from the server response and refreshed after every mutation.
 */
export const useDebtStore = defineStore('debts', () => {
  const debts = ref<DebtView[]>([])
  const summary = ref<DebtSummary | null>(null)
  const loading = ref(false)
  const error = ref('')

  async function refresh(): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      const [list, sum] = await Promise.all([listDebts(), getDebtSummary()])
      debts.value = list
      summary.value = sum
    } catch (caught) {
      error.value = problemMessage(caught, 'Could not load debts.')
    } finally {
      loading.value = false
    }
  }

  async function addDebt(body: DebtPayload): Promise<void> {
    await postDebt(body)
    await refresh()
  }

  async function updateDebt(id: string, body: DebtPayload): Promise<void> {
    await putDebt(id, body)
    await refresh()
  }

  async function removeDebt(id: string): Promise<void> {
    await deleteDebt(id)
    await refresh()
  }

  return { debts, summary, loading, error, refresh, addDebt, updateDebt, removeDebt }
})
