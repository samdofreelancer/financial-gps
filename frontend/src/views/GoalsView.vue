<template>
  <div class="page">
    <header class="head">
      <h1 class="page-title">Mục tiêu tài chính</h1>
      <p class="lead">Đích đến, tiến độ và khả năng chi trả mỗi tháng.</p>
    </header>

    <div v-if="store.error" class="error-box" role="alert">{{ store.error }}</div>

    <div v-if="store.loading && !store.goals.length" class="card placeholder" data-testid="goals-loading">
      Đang tải mục tiêu của bạn…
    </div>

    <template v-else>
      <button type="button" class="btn add-goal" @click="openCreate">
        <span aria-hidden="true">+</span> Thêm mục tiêu
      </button>

      <GoalList :goals="store.goals" @edit="startEdit" @remove="askRemove" @capacity="openCapacity" />

      <section v-if="selected" class="detail">
        <h2 class="list-title">{{ selected.name }}</h2>
        <GoalProgressCard :goal="selected" />
        <div v-if="store.capacityLoading">Đang tải capacity…</div>
        <div v-else-if="store.capacityError" class="error-box" role="alert">{{ store.capacityError }}</div>
        <GoalCapacity v-else-if="capacity" :capacity="capacity" />
      </section>
    </template>

    <GoalForm v-if="formOpen" :line="editing" :error="formError" @submit="onSubmit" @cancel="cancel" />

    <ConfirmDialog
      v-if="pendingRemoval"
      title="Lưu trữ mục tiêu này?"
      :message="`Mục tiêu ${pendingRemoval.name} sẽ bị ẩn khỏi danh sách.`"
      consequence="Mục tiêu đã lưu trữ không còn tính vào capacity và không thể xem lại."
      confirm-text="Lưu trữ"
      :busy="removing"
      @confirm="onRemove"
      @cancel="pendingRemoval = null"
    />

    <ToastStack />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useGoalStore } from '../stores/goalStore'
import { useToasts } from '../stores/toastStore'
import { problemMessage } from '../api/http'
import type { GoalPayload, GoalView } from '../api/goals'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import ToastStack from '../components/ToastStack.vue'
import GoalCapacity from '../components/goals/GoalCapacity.vue'
import GoalForm from '../components/goals/GoalForm.vue'
import GoalList from '../components/goals/GoalList.vue'
import GoalProgressCard from '../components/goals/GoalProgressCard.vue'

const store = useGoalStore()
const toasts = useToasts()

const formOpen = ref(false)
const editing = ref<GoalView | null>(null)
const formError = ref('')
const pendingRemoval = ref<GoalView | null>(null)
const removing = ref(false)
const selected = ref<GoalView | null>(null)

const capacity = computed(() =>
  selected.value ? store.capacities[selected.value.id] ?? null : null,
)

onMounted(() => {
  void store.fetchGoals()
})

function openCreate(): void {
  editing.value = null
  formError.value = ''
  formOpen.value = true
}

function startEdit(goal: GoalView): void {
  editing.value = goal
  formError.value = ''
  formOpen.value = true
}

function cancel(): void {
  formOpen.value = false
  editing.value = null
  formError.value = ''
}

async function onSubmit(payload: GoalPayload): Promise<void> {
  try {
    formError.value = ''
    if (editing.value) {
      await store.updateGoal(editing.value.id, payload)
      toasts.success(`Đã cập nhật mục tiêu ${payload.name}.`)
    } else {
      await store.addGoal(payload)
      toasts.success(`Đã thêm mục tiêu ${payload.name}.`)
    }
    cancel()
  } catch (caught) {
    formError.value = problemMessage(caught, 'Không lưu được mục tiêu.')
  }
}

function askRemove(goal: GoalView): void {
  pendingRemoval.value = goal
}

async function onRemove(): Promise<void> {
  const goal = pendingRemoval.value
  if (!goal) return
  removing.value = true
  try {
    await store.removeGoal(goal.id)
    if (selected.value?.id === goal.id) selected.value = null
    pendingRemoval.value = null
    toasts.success(`Đã lưu trữ mục tiêu ${goal.name}.`)
  } catch (caught) {
    toasts.error(problemMessage(caught, 'Không lưu trữ được mục tiêu.'))
  } finally {
    removing.value = false
  }
}

async function openCapacity(goal: GoalView): Promise<void> {
  selected.value = goal
  await store.fetchCapacity(goal.id)
}
</script>

<style scoped>
.page { display: grid; gap: 16px; max-width: 1180px; }
.head h1 { margin: 0; }
.placeholder { padding: 28px; text-align: center; color: var(--fg-muted); }
.add-goal { width: auto; justify-self: start; padding: 8px 18px; }
.list-title { margin: 6px 0 0; font-size: 16px; }
.detail { display: grid; gap: 12px; }
</style>
