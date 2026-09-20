<template>
  <form ref="formEl" class="line-form" @submit.prevent="onSubmit">
    <h3 class="form-title">{{ isEdit ? "Edit income" : "Add income" }}</h3>
    <MoneyInput
      id="income-amount"
      v-model="amount"
      label="Amount"
      :currency="currency"
      :error="amountError"
      hint="Monthly amount, e.g. 30.000.000"
    />

    <label for="income-source" class="field-label">Source</label>
    <select
      id="income-source"
      v-model="source"
      class="input"
      :aria-invalid="sourceError ? 'true' : undefined"
      :aria-describedby="sourceError ? 'income-source-error' : undefined"
    >
      <option value="">— choose a source —</option>
      <option v-for="option in SOURCE_OPTIONS" :key="option.value" :value="option.value">
        {{ option.label }}
      </option>
    </select>
    <p v-if="sourceError" id="income-source-error" class="field-error" role="alert">
      {{ sourceError }}
    </p>

    <div v-if="error" class="error-box" role="alert">{{ error }}</div>
    <div class="actions">
      <button type="button" class="btn-ghost" @click="onCancel">Cancel</button>
      <button type="button" class="btn" @click="onSubmit">
        {{ isEdit ? "Update income" : "Add income" }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue"
import MoneyInput from "./MoneyInput.vue"
import { SOURCE_OPTIONS } from "./lineOptions"
import { isDecimalAmount, type ProfileLine } from "../api/profile"

const props = withDefaults(
  defineProps<{ line?: ProfileLine | null; error: string; currency?: string }>(),
  { currency: "VND" },
)
const emit = defineEmits<{ (e: "submit", body: { amount: string; source: string }): void; (e: "cancel"): void }>()

const formEl = ref<HTMLFormElement | null>(null)
const amount = ref("")
const source = ref("")
/** Field-level message for the amount; the page still guards the request and the server is final. */
const amountError = ref("")
/** Field-level message for the source, kept apart so only the offending control is flagged. */
const sourceError = ref("")

const isEdit = computed(() => Boolean(props.line))

watch(
  () => props.line,
  (line) => {
    amount.value = line?.amount ?? ""
    source.value = line?.source ?? ""
    amountError.value = ""
    sourceError.value = ""
  },
  { immediate: true },
)

onMounted(() => {
  formEl.value?.querySelector("input")?.focus()
})

function onCancel(): void {
  emit("cancel")
}

function onSubmit(): void {
  amountError.value = ""
  sourceError.value = ""
  const value = amount.value.trim()
  if (!isDecimalAmount(value)) {
    amountError.value = "Enter an amount with digits and up to 2 decimals."
    return
  }
  if (!source.value) {
    sourceError.value = "Choose a source for this income."
    return
  }
  emit("submit", { amount: value, source: source.value })
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
.field-error { margin: 0 0 10px; font-size: 13px; color: var(--fg-danger); }
.actions { display: flex; gap: 10px; margin-top: 8px; }
.actions .btn, .actions .btn-ghost { flex: 1; }
</style>
