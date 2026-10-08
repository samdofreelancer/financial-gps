<template>
  <div class="goal-backdrop" @click.self="onCancel">
    <div class="goal-dialog" role="dialog" aria-modal="true" aria-labelledby="goal-form-title">
      <h2 id="goal-form-title" class="goal-dialog__title">{{ isEdit ? 'Sửa mục tiêu' : 'Thêm mục tiêu' }}</h2>
      <form class="goal-form" @submit.prevent="onSubmit">
        <label class="field">Tên mục tiêu
          <input v-model="form.name" class="input" data-testid="goal-name" required maxlength="120" />
        </label>
        <div class="grid">
          <label class="field">Loại mục tiêu
            <select v-model="form.goalType" class="input" data-testid="goal-type">
              <option value="EMERGENCY_FUND">Quỹ dự phòng</option>
              <option value="DEBT_FREEDOM">Tự do nợ</option>
              <option value="SAVINGS">Tiết kiệm</option>
              <option value="HOUSING">Nhà ở</option>
              <option value="EDUCATION">Giáo dục</option>
              <option value="RETIREMENT">Hưu trí</option>
              <option value="OTHER">Khác</option>
            </select>
          </label>
          <label class="field">Ưu tiên (1 = cao nhất)
            <input v-model="form.priority" class="input" data-testid="goal-priority" type="number" min="1" step="1" />
          </label>
        </div>
        <div class="grid">
          <MoneyInput id="goal-target" v-model="form.targetAmount" currency="VND" label="Mục tiêu" />
          <MoneyInput id="goal-current" v-model="form.currentAmount" currency="VND" label="Đã có hiện tại" />
        </div>
        <label class="field">Ngày mục tiêu (để trống nếu chưa có)
          <input v-model="form.targetDate" class="input" data-testid="goal-date" type="date" />
        </label>
        <div v-if="validationError" class="error-box" role="alert">{{ validationError }}</div>
        <div v-if="error" class="error-box" role="alert">{{ error }}</div>
        <div class="actions">
          <button type="submit" class="btn btn--primary" :disabled="Boolean(validationError)">Lưu</button>
          <button type="button" class="btn" @click="onCancel">Hủy</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import MoneyInput from '../MoneyInput.vue'
import { isMoneyValid, type GoalPayload, type GoalView } from '../../api/goals'

const props = defineProps<{ line?: GoalView | null; error?: string }>()
const emit = defineEmits<{ (e: 'submit', payload: GoalPayload): void; (e: 'cancel'): void }>()

const form = reactive({
  name: props.line?.name ?? '',
  goalType: props.line?.goalType ?? 'EMERGENCY_FUND',
  targetAmount: props.line?.targetAmount ?? '',
  currentAmount: props.line?.currentAmount ?? '',
  targetDate: props.line?.targetDate ?? '',
  priority: String(props.line?.priority ?? '1'),
})

const isEdit = Boolean(props.line)

function onCancel(): void {
  emit('cancel')
}

const validationError = computed(() => {
  if (!form.name.trim()) return 'Tên mục tiêu là bắt buộc.'
  if (!isMoneyValid(form.targetAmount) || !isMoneyValid(form.currentAmount)) {
    return 'Số tiền phải là số thập phân không âm (ví dụ 200000000.00).'
  }
  const prio = Number(form.priority)
  if (!Number.isInteger(prio) || prio < 1) return 'Ưu tiên phải là số nguyên từ 1 trở lên.'
  return ''
})

function onSubmit(): void {
  if (validationError.value) return
  emit('submit', {
    name: form.name.trim(),
    goalType: form.goalType as GoalPayload['goalType'],
    targetAmount: form.targetAmount.trim(),
    currentAmount: form.currentAmount.trim(),
    targetDate: form.targetDate === '' ? null : form.targetDate,
    priority: Number(form.priority),
  })
}
</script>

<style scoped>
.goal-backdrop { position: fixed; inset: 0; z-index: 60; display: flex; justify-content: center; padding: 20px; overflow-y: auto; background: rgba(17, 24, 39, 0.45); }
.goal-dialog { width: min(720px, 100%); height: fit-content; margin: auto; padding: 22px 24px 20px; background: var(--fg-surface); border-radius: var(--fg-radius-card); box-shadow: var(--fg-shadow-float); }
.goal-dialog__title { margin: 0 0 14px; font-size: 17px; }
.goal-form { display: grid; gap: 12px; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field { display: grid; gap: 6px; font-size: 13px; }
.actions { display: flex; gap: 8px; }
.error-box { color: var(--fg-danger); }
@media (max-width: 560px) { .grid { grid-template-columns: 1fr; } }
</style>
