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

  async function fetchGoals(): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      goals.value = await listGoals()
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
    await fetchGoals()
  }

  async function removeGoal(id: string): Promise<void> {
    await deleteGoal(id)
    await fetchGoals()
  }

  async function fetchCapacity(id: string): Promise<void> {
    capacityLoading.value = true
    capacityError.value = ''
    try {
      capacities.value[id] = await getGoalCapacity(id)
    } catch (caught) {
      capacityError.value = problemMessage(caught, 'Could not load goal capacity.')
    } finally {
      capacityLoading.value = false
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
