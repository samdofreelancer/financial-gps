<template>
  <section class="card basics" data-testid="basics-card" aria-labelledby="basics-title">
    <div class="head">
      <h2 id="basics-title">Your financial basics</h2>
      <button
        v-if="!editing"
        type="button"
        class="btn-ghost small"
        data-testid="basics-edit"
        @click="startEdit"
      >
        Edit
      </button>
    </div>

    <!-- Read-only by default: the facts are shown, the form only appears when asked for. -->
    <template v-if="!editing">
      <template v-if="view">
        <dl class="facts">
          <div>
            <dt>Savings</dt>
            <dd>
              <MoneyDisplay :amount="view.savingsAmount" :currency="view.currency" hide-currency />
            </dd>
          </div>
          <div>
            <dt>Emergency fund</dt>
            <dd>
              <MoneyDisplay
                :amount="view.emergencyFundAmount"
                :currency="view.currency"
                hide-currency
              />
            </dd>
          </div>
          <div>
            <dt>People depending on you</dt>
            <dd class="count">{{ view.dependentsCount }}</dd>
          </div>
        </dl>
        <p class="note">Values you entered, in {{ view.currency }}.</p>
      </template>
      <p v-else class="hint">Loading your facts…</p>
    </template>

    <form v-else class="edit-form" @submit.prevent="onSave">
      <MoneyInput id="savings" v-model="savings" label="Savings" hint="Decimal amount, e.g. 100.00." />
      <MoneyInput
        id="emergency"
        v-model="emergency"
        label="Emergency fund"
        hint="Decimal amount, e.g. 50.00."
      />
      <label for="dependents" class="field-label">People depending on you</label>
      <input id="dependents" v-model.number="dependents" type="number" min="0" class="input" />
      <div v-if="error" class="error-box" role="alert">{{ error }}</div>
      <div class="actions">
        <button type="button" class="btn-ghost" :disabled="saving" @click="cancelEdit">Cancel</button>
        <button type="button" class="btn" :disabled="saving" @click="onSave">
          {{ saving ? 'Saving…' : 'Save basics' }}
        </button>
      </div>
    </form>
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import MoneyDisplay from './MoneyDisplay.vue'
import MoneyInput from './MoneyInput.vue'
import type { ProfileView } from '../api/profile'

/**
 * Progressive disclosure: the facts are shown read-only by default and the existing profile fields
 * are only revealed when the user asks to edit. Field names, ids and validation stay unchanged —
 * `currency: 'VND'` is still what the existing PUT /api/v1/profile contract receives.
 */
const props = defineProps<{ view: ProfileView | null; saving: boolean; error: string }>()
const emit = defineEmits<{
  (e: 'save', body: {
    currency: string
    savingsAmount: string
    emergencyFundAmount: string
    dependentsCount: number
  }): void
}>()

const editing = ref(false)
const savings = ref('0.00')
const emergency = ref('0.00')
const dependents = ref(0)

function startEdit(): void {
  savings.value = props.view?.savingsAmount ?? '0.00'
  emergency.value = props.view?.emergencyFundAmount ?? '0.00'
  dependents.value = props.view?.dependentsCount ?? 0
  editing.value = true
}

function cancelEdit(): void {
  editing.value = false
}

function onSave(): void {
  emit('save', {
    currency: 'VND',
    savingsAmount: savings.value.trim(),
    emergencyFundAmount: emergency.value.trim(),
    dependentsCount: dependents.value,
  })
  // Keep the form open after a failed save (the parent surfaces the error);
  // close it when saving finished without error.
  watch(
    () => props.saving,
    (now, before) => {
      if (before && !now && !props.error) editing.value = false
    },
  )
}

defineExpose({ startEdit })
</script>

<style scoped>
.basics { padding: 24px; }
.head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.head h2 { margin: 0; font-size: 18px; }
/* Same control size as the section CTAs so the whole page feels like one system. */
.head .btn-ghost.small { min-height: 36px; padding: 6px 14px; }
.facts { display: grid; gap: 12px; margin: 16px 0 0; }
.facts > div {
  display: flex; justify-content: space-between; align-items: baseline; gap: 12px;
  background: var(--fg-app-bg); border-radius: var(--fg-radius-control);
  padding: 12px 14px;
}
.facts dt { font-size: 14px; color: var(--fg-muted); }
.facts dd {
  margin: 0; font-weight: 700; font-size: 16px; color: var(--fg-ink);
  font-variant-numeric: tabular-nums;
}
.count { font-variant-numeric: tabular-nums; }
.note { margin: 12px 0 0; font-size: 13px; color: var(--fg-muted); }
.edit-form { margin-top: 16px; }
.edit-form .field-label { display: block; margin-bottom: 6px; font-size: 13px; font-weight: 600; color: var(--fg-label); }
.edit-form :deep(.input) { margin-bottom: 10px; }
.actions { display: flex; gap: 10px; margin-top: 12px; }
.actions .btn, .actions .btn-ghost { flex: 1; }

@media (max-width: 640px) {
  .basics { padding: 20px 16px; }
  .head .btn-ghost.small { min-height: 40px; padding: 8px 18px; }
}
</style>
