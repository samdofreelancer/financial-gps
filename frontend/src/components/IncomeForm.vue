<template>
  <form ref="formEl" class="line-form" @submit.prevent="onSubmit">
    <h3 class="form-title">{{ isEdit ? 'Edit income' : 'Add income' }}</h3>
    <MoneyInput id="income-amount" v-model="amount" label="Amount" hint="e.g. 74.00" />
    <label for="income-source" class="field-label">Source</label>
    <input id="income-source" v-model="source" class="input" placeholder="salary" />
    <div v-if="error" class="error-box" role="alert">{{ error }}</div>
    <div class="actions">
      <button type="button" class="btn-ghost" @click="emit('cancel')">Cancel</button>
      <button type="button" class="btn" @click="onSubmit">
        {{ isEdit ? 'Update income' : 'Add income' }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import MoneyInput from './MoneyInput.vue'
import type { ProfileLine } from '../api/profile'

/**
 * Add/edit one income record. Amounts stay raw decimal strings (never JS numbers) so what the user
 * typed is exactly what the server receives. Mounting focuses the amount field: the form opens
 * where the user is looking, and keyboard users land on the first control.
 */
const props = defineProps<{ line?: ProfileLine | null; error: string }>()
const emit = defineEmits<{
  (e: 'submit', body: { amount: string; source: string }): void
  (e: 'cancel'): void
}>()

const formEl = ref<HTMLFormElement | null>(null)
const amount = ref('')
const source = ref('')

const isEdit = computed(() => Boolean(props.line))

watch(
  () => props.line,
  (line) => {
    amount.value = line?.amount ?? ''
    source.value = line?.source ?? ''
  },
  { immediate: true },
)

onMounted(() => {
  formEl.value?.querySelector('input')?.focus()
})

function onSubmit(): void {
  emit('submit', { amount: amount.value.trim(), source: source.value.trim() })
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
