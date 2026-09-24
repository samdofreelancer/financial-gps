<template>
  <li class="line" data-testid="expense-item">
    <span class="line-icon" aria-hidden="true">{{ icon }}</span>
    <span class="line-main">
      <span class="line-name">{{ line.category }}</span>
      <span class="line-meta">
        <MoneyDisplay :amount="line.amount" :currency="line.currency" hide-currency />
        / month
        <span v-if="line.expenseType" class="tag">{{ typeLabel }}</span>
      </span>
    </span>
    <span class="line-actions">
      <button
        type="button"
        class="btn-ghost small"
        :data-testid="`edit-expense-${line.id}`"
        @click="emit('edit', line)"
      >
        Edit
      </button>
      <button
        type="button"
        class="btn-ghost small danger"
        :data-testid="`remove-expense-${line.id}`"
        @click="emit('remove', line.id)"
      >
        Remove
      </button>
    </span>
  </li>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MoneyDisplay from './MoneyDisplay.vue'
import { expenseIcon } from './lineIcons'
import type { ProfileLine } from '../api/profile'

/**
 * One expense record, read-only. Fixed/Variable is the stored `expenseType` — the label is the only
 * thing translated here, the business meaning stays on the server.
 */
const props = defineProps<{ line: ProfileLine }>()
const emit = defineEmits<{
  (e: 'edit', line: ProfileLine): void
  (e: 'remove', id: string): void
}>()

const icon = computed(() => expenseIcon(props.line.category))
const typeLabel = computed(() => (props.line.expenseType === 'VARIABLE' ? 'Variable' : 'Fixed'))
</script>

<style scoped>
.line {
  display: flex; align-items: center; gap: 14px;
  padding: 12px 14px;
  background: var(--fg-app-bg); border-radius: var(--fg-radius-control);
}
.line-icon { flex: 0 0 auto; font-size: 20px; }
.line-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.line-name { font-weight: 600; font-size: 15px; color: var(--fg-ink); overflow-wrap: anywhere; }
.line-meta { font-size: 13px; color: var(--fg-muted); }
.tag {
  display: inline-block; margin-left: 6px; padding: 1px 8px;
  font-size: 12px; font-weight: 600; color: var(--fg-label);
  background: var(--fg-surface); border: 1px solid var(--fg-border-soft);
  border-radius: 999px;
}
.line-actions { display: flex; gap: 8px; flex: 0 0 auto; }
/* Row actions are the most-tapped controls on the page: keep them comfortably sized. */
.line-actions .btn-ghost.small { min-height: 36px; padding: 6px 14px; }

@media (max-width: 640px) {
  .line { flex-wrap: wrap; }
  .line-actions { width: 100%; justify-content: flex-end; }
  .line-actions .btn-ghost.small { min-height: 40px; padding: 8px 18px; }
}
</style>
