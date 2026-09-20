<template>
  <section class="card" data-testid="expense-card" aria-labelledby="expense-title">
    <div class="section-head">
      <h2 id="expense-title">Money going out</h2>
      <button
        type="button"
        class="btn-ghost small"
        data-testid="add-expense"
        @click="emit('add')"
      >
        + Add expense
      </button>
    </div>
    <p v-if="expenses.length" class="currency-note">Recurring monthly expenses in {{ currency }}.</p>

    <div v-if="error && !formOpen" class="error-box" role="alert">{{ error }}</div>

    <!-- The form is owned by the page (it talks to the API); the list only frames it. -->
    <slot name="form" />

    <ul v-if="expenses.length" class="lines">
      <ExpenseItem
        v-for="line in expenses"
        :key="line.id"
        :line="line"
        @edit="emit('edit', $event)"
        @remove="emit('remove', $event)"
      />
    </ul>

    <div v-else-if="!formOpen" class="empty" data-testid="expense-empty">
      <p class="empty-title">No expenses added yet.</p>
      <p class="hint">Add your recurring monthly expenses.</p>
      <button type="button" class="btn-ghost" @click="emit('add')">+ Add expense</button>
    </div>
  </section>
</template>

<script setup lang="ts">
import ExpenseItem from './ExpenseItem.vue'
import type { ProfileLine } from '../api/profile'

/**
 * "Money going out": the existing expense records as readable rows (Fixed/Variable shown), plus a
 * CTA and an empty state that says what to do next. No financial math lives here.
 */
defineProps<{
  expenses: ProfileLine[]
  currency: string
  error: string
  formOpen: boolean
}>()
const emit = defineEmits<{
  (e: 'add'): void
  (e: 'edit', line: ProfileLine): void
  (e: 'remove', id: string): void
}>()
</script>

<style scoped>
.card { padding: 24px; }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.section-head h2 { margin: 0; font-size: 18px; }
.currency-note { margin: 6px 0 0; font-size: 13px; color: var(--fg-muted); }
/* The card-level CTA is a primary action: keep it as readable as the row actions. */
.section-head .btn-ghost.small { min-height: 36px; padding: 6px 14px; }

.lines { list-style: none; margin: 16px 0 0; padding: 0; display: grid; gap: 10px; }

.empty {
  margin-top: 16px; padding: 20px;
  display: flex; flex-direction: column; align-items: flex-start; gap: 6px;
  background: var(--fg-info-bg); border: 1px dashed var(--fg-info-border);
  border-radius: var(--fg-radius-control);
}
.empty-title { margin: 0; font-weight: 600; font-size: 15px; color: var(--fg-ink); }
.empty .hint { margin: 0 0 8px; }
.empty .btn-ghost { min-height: 40px; }

@media (max-width: 640px) {
  .card { padding: 20px 16px; }
  .section-head { flex-wrap: wrap; }
  .section-head .btn-ghost.small { min-height: 40px; padding: 8px 18px; }
}
</style>
