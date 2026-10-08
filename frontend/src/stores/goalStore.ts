import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  deleteGoal,
  getGoalCapacity,
  listGoals,
  postGoal,
  putGoal,
  type GoalCapacityView,
  type GoalPayload,
  type GoalView,
} from '../api/goals'
import { problemMessage } from '../api/http'

/**
 * Server state only. Remaining/progress/capacity are rendered from the server
 * response; the browser never re-implements goal math.
 */
export const useGoalStore = defineStore('goals', () => {
  const goals = ref<GoalView[]>([])
  const loading = ref(false)
  const error = ref('')

  const capacities = ref<Record<string, GoalCapacityView>>({})
  const capacityLoading = ref(false)
  const capacityError = ref('')
  let capacityToken = 0

  async function fetchGoals(): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      goals.value = await listGoals()
      // The list is the identity set: any cached capacity may belong to a goal
      // whose numbers just changed server-side.
      capacities.value = {}
    } catch (caught) {
      error.value = problemMessage(caught, 'Could not load goals.')
    } finally {
      loading.value = false
    }
  }

  async function addGoal(body: GoalPayload): Promise<void> {
    await postGoal(body)
    await fetchGoals()
  }

  async function updateGoal(id: string, body: GoalPayload): Promise<void> {
    await putGoal(id, body)
    delete capacities.value[id]
    await fetchGoals()
  }

  async function removeGoal(id: string): Promise<void> {
    await deleteGoal(id)
    delete capacities.value[id]
    await fetchGoals()
  }

  async function fetchCapacity(id: string): Promise<void> {
    const token = ++capacityToken
    capacityLoading.value = true
    capacityError.value = ''
    try {
      const view = await getGoalCapacity(id)
      // A newer request won: drop this late response instead of flashing stale data.
      if (token !== capacityToken) return
      capacities.value[id] = view
    } catch (caught) {
      if (token !== capacityToken) return
      capacityError.value = problemMessage(caught, 'Could not load goal capacity.')
    } finally {
      if (token === capacityToken) capacityLoading.value = false
    }
  }

  return {
    goals,
    loading,
    error,
    capacities,
    capacityLoading,
    capacityError,
    fetchGoals,
    addGoal,
    updateGoal,
    removeGoal,
    fetchCapacity,
  }
})
