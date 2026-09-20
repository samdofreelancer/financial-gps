<template>
  <form ref="formEl" class="line-form" @submit.prevent="onSubmit">
    <h3 class="form-title">{{ isEdit ? 'Edit expense' : 'Add expense' }}</h3>
    <MoneyInput id="expense-amount" v-model="amount" label="Amount" hint="e.g. 30.00" />
    <label for="expense-category" class="field-label">Category</label>
    <input id="expense-category" v-model="category" class="input" placeholder="rent" />
    <label for="expense-type" class="field-label">Type</label>
    <select id="expense-type" v-model="expenseType" class="input">
      <option value="FIXED">Fixed</option>
      <option value="VARIABLE">Variable</option>
    </select>
    <div v-if="error" class="error-box" role="alert">{{ error }}</div>
    <div class="actions">
      <button type="button" class="btn-ghost" @click="emit('cancel')">Cancel</button>
      <button type="button" class="btn" @click="onSubmit">
        {{ isEdit ? 'Update expense' : 'Add expense' }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import MoneyInput from './MoneyInput.vue'
import type { ProfileLine } from '../api/profile'

/**
 * Add/edit one expense record. The amount stays a raw decimal string and Fixed/Variable is sent
 * exactly as the existing contract names it (`FIXED` | `VARIABLE`).
 */
const props = defineProps<{ line?: ProfileLine | null; error: string }>()
const emit = defineEmits<{
  (
    e: 'submit',
    body: { amount: string; category: string; expenseType: 'FIXED' | 'VARIABLE' },
  ): void
  (e: 'cancel'): void
}>()

const formEl = ref<HTMLFormElement | null>(null)
const amount = ref('')
const category = ref('')
const expenseType = ref<'FIXED' | 'VARIABLE'>('FIXED')

const isEdit = computed(() => Boolean(props.line))

watch(
  () => props.line,
  (line) => {
    amount.value = line?.amount ?? ''
    category.value = line?.category ?? ''
    expenseType.value = line?.expenseType ?? 'FIXED'
  },
  { immediate: true },
)

onMounted(() => {
  formEl.value?.querySelector('input')?.focus()
})

function onSubmit(): void {
  emit('submit', {
    amount: amount.value.trim(),
    category: category.value.trim(),
    expenseType: expenseType.value,
  })
}
</script>

<style scoped>
.line-form {
  margin-top: 16px; padding: 16px;
  background: var(--fg-app-bg); border-radius: var(--fg-radius-control);
}
.form-title { margin: 0 0 12px; font-size: 15px; }
.field-label { display: block; margin-bottom: 6px; font-size: 13px; font-weight: 600; color: var(--fg-label); }
.line-form :deep(.input) { margin-bottom: 10px; }
.actions { display: flex; gap: 10px; margin-top: 8px; }
.actions .btn, .actions .btn-ghost { flex: 1; }
</style>
