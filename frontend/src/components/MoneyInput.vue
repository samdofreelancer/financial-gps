<template>
  <label :for="id" class="field-label"><slot>{{ label }}</slot></label>
  <input
    :id="id"
    :value="modelValue"
    inputmode="decimal"
    autocomplete="off"
    class="input"
    placeholder="0.00"
    @input="onInput(($event.target as HTMLInputElement).value)"
  />
  <p v-if="hint" class="hint">{{ hint }}</p>
</template>

<script setup lang="ts">
/**
 * Decimal-safe money field: the model is the raw string (never a number), so the amount the user
 * typed is exactly what the server receives.
 */
defineProps<{ modelValue: string; id: string; label?: string; hint?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

/** Decimal-string only: never converts to number for truth. */
function onInput(value: string): void {
  emit('update:modelValue', value)
}
</script>

<style scoped>
.field-label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 500;
}
</style>
