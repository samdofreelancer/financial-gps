<template>
  <form ref="formEl" class="line-form" @submit.prevent="onSubmit">
    <h3 class="form-title">{{ isEdit ? "Edit expense" : "Add expense" }}</h3>
    <MoneyInput
      id="expense-amount"
      v-model="amount"
      label="Amount"
      :currency="currency"
      :error="amountError"
      hint="Monthly amount, e.g. 5.000.000"
    />

    <label for="expense-category" class="field-label">Category</label>
    <select
      id="expense-category"
      v-model="category"
      class="input"
      :aria-invalid="categoryError ? 'true' : undefined"
      :aria-describedby="categoryError ? 'expense-category-error' : undefined"
    >
      <option value="">— choose a category —</option>
      <option v-for="option in CATEGORY_OPTIONS" :key="option.value" :value="option.value">
        {{ option.label }}
      </option>
    </select>
    <p v-if="categoryError" id="expense-category-error" class="field-error" role="alert">
      {{ categoryError }}
    </p>

    <label for="expense-type" class="field-label">Type</label>
    <select id="expense-type" v-model="expenseType" class="input">
      <option value="FIXED">Fixed</option>
      <option value="VARIABLE">Variable</option>
    </select>

    <div v-if="error" class="error-box" role="alert">{{ error }}</div>
    <div class="actions">
      <button type="button" class="btn-ghost" @click="onCancel">Cancel</button>
      <button type="button" class="btn" @click="onSubmit">
        {{ isEdit ? "Update expense" : "Add expense" }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue"
import MoneyInput from "./MoneyInput.vue"
import { CATEGORY_OPTIONS } from "./lineOptions"
import { isDecimalAmount, type ProfileLine } from "../api/profile"

const props = withDefaults(
  defineProps<{ line?: ProfileLine | null; error: string; currency?: string }>(),
  { currency: "VND" },
)
const emit = defineEmits<{ (e: "submit", body: { amount: string; category: string; expenseType: "FIXED" | "VARIABLE" }): void; (e: "cancel"): void }>()

const formEl = ref<HTMLFormElement | null>(null)
const amount = ref("")
const category = ref("")
const expenseType = ref<"FIXED" | "VARIABLE">("FIXED")
/** Field-level message for the amount; the page still guards the request and the server is final. */
const amountError = ref("")
/** Field-level message for the category, kept apart so only the offending control is flagged. */
const categoryError = ref("")

const isEdit = computed(() => Boolean(props.line))

watch(
  () => props.line,
  (line) => {
    amount.value = line?.amount ?? ""
    category.value = line?.category ?? ""
    expenseType.value = line?.expenseType ?? "FIXED"
    amountError.value = ""
    categoryError.value = ""
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
  categoryError.value = ""
  const value = amount.value.trim()
  if (!isDecimalAmount(value)) {
    amountError.value = "Enter an amount with digits and up to 2 decimals."
    return
  }
  if (!category.value) {
    categoryError.value = "Choose a category for this expense."
    return
  }
  emit("submit", {
    amount: value,
    category: category.value,
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
.field-error { margin: 0 0 10px; font-size: 13px; color: var(--fg-danger); }
.actions { display: flex; gap: 10px; margin-top: 8px; }
.actions .btn, .actions .btn-ghost { flex: 1; }
</style>
