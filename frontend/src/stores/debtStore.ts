import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  deleteDebt,
  getDebtSchedule,
  getDebtSummary,
  listDebts,
  markDebtPaid,
  postDebt,
  putDebt,
  undoDebtPaymentMark,
  type DebtPayload,
  type DebtSchedule,
  type DebtSummary,
  type DebtView,
} from '../api/debts'
import { problemMessage } from '../api/http'

/**
 * Server state only. No amortization here: projections, DTI and totals are
 * rendered from the server response and refreshed after every mutation.
 *
 * The payment calendar follows the same rule — `fetchSchedule` only carries the
 * server's rows across; a monotonically increasing token drops stale responses
 * when the reader flips between two debts faster than the network answers.
 */
export const useDebtStore = defineStore('debts', () => {
  const debts = ref<DebtView[]>([])
  const summary = ref<DebtSummary | null>(null)
  const loading = ref(false)
  const error = ref('')

  const schedule = ref<DebtSchedule | null>(null)
  const scheduleLoading = ref(false)
  const scheduleError = ref('')
  let scheduleToken = 0

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

  async function markPaid(id: string): Promise<void> {
    await markDebtPaid(id)
    await refresh()
  }

  async function undoPaymentMark(id: string): Promise<void> {
    await undoDebtPaymentMark(id)
    await refresh()
  }

  /** Load the amortization calendar of one debt; only the latest request wins. */
  async function fetchSchedule(id: string): Promise<void> {
    const token = ++scheduleToken
    scheduleLoading.value = true
    scheduleError.value = ''
    schedule.value = null
    try {
      const data = await getDebtSchedule(id)
      if (token === scheduleToken) schedule.value = data
    } catch (caught) {
      if (token === scheduleToken) {
        scheduleError.value = problemMessage(caught, 'Could not load the payment schedule.')
      }
    } finally {
      if (token === scheduleToken) scheduleLoading.value = false
    }
  }

  /** Closing the dialog drops any in-flight response too, so it never reopens stale. */
  function clearSchedule(): void {
    scheduleToken += 1
    schedule.value = null
    scheduleLoading.value = false
    scheduleError.value = ''
  }

  return {
    debts,
    summary,
    loading,
    error,
    refresh,
    addDebt,
    updateDebt,
    removeDebt,
    markPaid,
    undoPaymentMark,
    schedule,
    scheduleLoading,
    scheduleError,
    fetchSchedule,
    clearSchedule,
  }
})
