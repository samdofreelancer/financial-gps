<template>
  <label :for="id" class="field-label"><slot>{{ label }}</slot></label>
  <div class="money-field" :class="{ 'money-field--invalid': Boolean(error) }">
    <input
      :id="id"
      ref="inputEl"
      class="input money-input"
      type="text"
      inputmode="decimal"
      autocomplete="off"
      :value="text"
      :placeholder="placeholder"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="error ? errorId : hint ? hintId : undefined"
      @input="onInput"
      @focus="onFocus"
      @blur="onBlur"
    />
    <span v-if="currency" class="money-currency">{{ currency }}</span>
  </div>
  <p v-if="error" :id="errorId" class="field-error" role="alert">{{ error }}</p>
  <p v-else-if="hint" :id="hintId" class="hint">{{ hint }}</p>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { caretAfterDigits, countDigits, formatAmountDisplay, parseAmountText } from './moneyText'

/**
 * Money field with a decimal-string model.
 *
 * What the user sees is a currency amount (`30.000.000,00`); what the model carries is the untouched
 * decimal string the API expects (`30000000.00`). Only digits and the two separators can ever reach
 * the model, at most two decimals are kept and a minus sign is impossible - so the existing
 * POST/PUT contract, backend validation and persistence stay exactly as they are.
 */
const props = withDefaults(
  defineProps<{
    modelValue: string
    id: string
    label?: string
    hint?: string
    /** Field-level validation message; the server stays authoritative for the final word. */
    error?: string
    /** Currency code shown inside the field. Defaults to the application currency. */
    currency?: string
    placeholder?: string
  }>(),
  { currency: 'VND', placeholder: '0,00' },
)

const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const inputEl = ref<HTMLInputElement | null>(null)
const text = ref(formatAmountDisplay(props.modelValue))

const hintId = computed(() => `${props.id}-hint`)
const errorId = computed(() => `${props.id}-error`)

watch(
  () => props.modelValue,
  (value) => {
    // Skip the echo of our own edit: the field already shows what the user is typing.
    if (parseAmountText(text.value).raw === value) return
    text.value = formatAmountDisplay(value)
  },
  { immediate: true },
)

function onInput(event: Event): void {
  const element = event.target as HTMLInputElement
  const typed = element.value
  const caret = element.selectionStart ?? typed.length
  const digitsBefore = countDigits(typed.slice(0, caret))

  const parsed = parseAmountText(typed)
  text.value = parsed.display
  // Assign directly too: when a rejected character leaves the text unchanged Vue has nothing to
  // patch, and the browser would otherwise keep showing the rejected input.
  element.value = parsed.display
  emit('update:modelValue', parsed.raw)

  // A decimal separator typed at the end of the digits must stay behind the caret, otherwise the
  // decimals typed next are inserted into the whole part (52.000.000,25 -> 5.200.000.025,).
  const position =
    typed.endsWith(',') && parsed.display.endsWith(',')
      ? parsed.display.length
      : caretAfterDigits(parsed.display, digitsBefore)
  if (element.selectionStart !== position) {
    element.setSelectionRange(position, position)
  }
}

function onFocus(): void {
  const element = inputEl.value
  if (!element) return
  // Trailing zeros come off while editing so "30.000.000,00" cannot swallow the next digit typed.
  const parsed = parseAmountText(text.value)
  const display = parsed.raw === '' ? '' : formatAmountDisplay(parsed.raw, { decimals: 'trim' })
  text.value = display
  element.value = display
}

function onBlur(): void {
  const element = inputEl.value
  if (!element) return
  // Resting look: a full two-decimal amount, e.g. "30000000" -> "30.000.000,00".
  // Presentation only — the model keeps exactly what the user entered.
  const parsed = parseAmountText(text.value)
  const display = parsed.raw === '' ? '' : formatAmountDisplay(parsed.raw, { decimals: 2 })
  text.value = display
  element.value = display
}
</script>

<style scoped>
.money-field {
  position: relative;
}
.money-field .money-input {
  padding-right: 64px;
  font-variant-numeric: tabular-nums;
}
.money-currency {
  position: absolute;
  top: 50%;
  right: 14px;
  transform: translateY(-50%);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--fg-muted);
  pointer-events: none;
}
.money-field--invalid .money-input {
  border-color: var(--fg-danger);
}
.field-error {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--fg-danger);
}
</style>
