<template>
  <div class="page">
    <header class="head">
      <h1 class="page-title">Mục tiêu tài chính</h1>
      <p class="lead">Đích đến, tiến độ và khả năng chi trả mỗi tháng.</p>
    </header>

    <div v-if="store.error && !store.goals.length" class="card placeholder" role="alert">
      <p><strong>Không tải được mục tiêu.</strong> {{ store.error }}</p>
      <button type="button" class="btn-ghost small" :disabled="store.loading" @click="store.fetchGoals()">
        Thử lại
      </button>
    </div>

    <div v-else-if="store.loading && !store.goals.length" class="card placeholder" data-testid="goals-loading">
      Đang tải mục tiêu của bạn…
    </div>

    <template v-else>
      <div class="goals-toolbar">
        <button type="button" class="btn add-goal" @click="openCreate">
          <span aria-hidden="true">+</span> Thêm mục tiêu
        </button>
        <p v-if="store.goals.length" class="goals-count">{{ store.goals.length }} đích đến · ưu tiên theo #</p>
      </div>

      <GoalList :goals="store.goals" @edit="startEdit" @remove="askRemove" @capacity="openCapacity" @use-template="openTemplate" />

      <section v-if="selected" class="detail">
        <h2 class="list-title">{{ selected.name }}</h2>
        <GoalProgressCard :goal="selected" />
        <div v-if="store.capacityLoading">Đang tải capacity…</div>
        <div v-else-if="store.capacityError" class="error-box" role="alert">{{ store.capacityError }}</div>
        <GoalCapacity v-else-if="capacity" :capacity="capacity" />
      </section>
    </template>

    <GoalForm v-if="formOpen" :line="editing" :error="formError" :busy="saving" @submit="onSubmit" @cancel="cancel" />

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
const saving = ref(false)
const pendingRemoval = ref<GoalView | null>(null)
const removing = ref(false)
const selectedId = ref<string | null>(null)

/** Always derived from the store list: never a stale snapshot after update/remove. */
const selected = computed(() =>
  selectedId.value ? (store.goals.find((g) => g.id === selectedId.value) ?? null) : null,
)

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

const TEMPLATE_PRESET: Record<string, { name: string; goalType: GoalPayload['goalType'] }> = {
  EMERGENCY: { name: 'Quỹ khẩn cấp 6 tháng', goalType: 'EMERGENCY_FUND' },
  EDUCATION: { name: 'Học phí cho con', goalType: 'EDUCATION' },
  RETIREMENT: { name: 'Nghỉ hưu chủ động', goalType: 'RETIREMENT' },
}

function openTemplate(kind: string): void {
  const preset = TEMPLATE_PRESET[kind]
  editing.value = null
  formError.value = ''
  formOpen.value = true
  if (preset) {
    // Prefill via editing placeholder: GoalForm reads `line`, so stage a template line.
    editing.value = {
      id: '',
      name: preset.name,
      goalType: preset.goalType,
      targetAmount: '0.00',
      currentAmount: '0.00',
      targetDate: null,
      priority: store.goals.length + 1,
      status: 'ACTIVE',
      currency: 'VND',
      remaining: '0.00',
      progress: '0',
      completionCondition: '',
      derived: {},
    } as GoalView
  }
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
  // Double-submit guard: the backend has no idempotency key, so a second
  // click/Enter while the first POST is in flight must not send another one.
  if (saving.value) return
  saving.value = true
  try {
    formError.value = ''
    if (editing.value?.id) {
      await store.updateGoal(editing.value.id, payload)
      toasts.success(`Đã cập nhật mục tiêu ${payload.name}.`)
    } else {
      await store.addGoal(payload)
      toasts.success(`Đã thêm mục tiêu ${payload.name}.`)
    }
    cancel()
  } catch (caught) {
    formError.value = problemMessage(caught, 'Không lưu được mục tiêu.')
  } finally {
    saving.value = false
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
    if (selectedId.value === goal.id) selectedId.value = null
    pendingRemoval.value = null
    toasts.success(`Đã lưu trữ mục tiêu ${goal.name}.`)
  } catch (caught) {
    toasts.error(problemMessage(caught, 'Không lưu trữ được mục tiêu.'))
  } finally {
    removing.value = false
  }
}

async function openCapacity(goal: GoalView): Promise<void> {
  selectedId.value = goal.id
  await store.fetchCapacity(goal.id)
}
</script>

<style scoped>
.page { display: grid; gap: 18px; max-width: 1120px; }
.head h1 { margin: 0; }
.head .lead { max-width: 640px; }
.placeholder { padding: 28px; text-align: center; color: var(--fg-muted); }
.goals-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.add-goal { width: auto; justify-self: start; padding: 10px 20px; border-radius: 999px; box-shadow: 0 6px 16px rgba(2, 132, 199, 0.28); }
.goals-count { margin: 0; font-size: 13px; color: var(--fg-muted); }
.list-title { margin: 6px 0 0; font-size: 16px; }
.detail { display: grid; gap: 12px; }
</style>
