<template>
  <div class="confirm-backdrop" @click.self="onCancel">
    <div class="confirm" role="alertdialog" aria-modal="true" :aria-labelledby="titleId" :aria-describedby="bodyId">
      <h2 :id="titleId" class="confirm__title">{{ title }}</h2>
      <div :id="bodyId" class="confirm__body">
        <p>{{ message }}</p>
        <!-- Destructive actions state their consequence up front instead of a bare "are you sure?". -->
        <p v-if="consequence" class="confirm__consequence">{{ consequence }}</p>
      </div>
      <div class="confirm__actions">
        <button ref="cancelButton" type="button" class="btn-ghost" @click="onCancel">{{ cancelText }}</button>
        <button type="button" class="confirm__danger" :disabled="busy" @click="onConfirm">
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * Confirmation for an action that cannot be undone from this screen.
 *
 * Escape cancels and focus lands on the safe choice first, so a reflexive Enter never deletes
 * anything. Backdrop clicks cancel rather than confirm — the destructive side is never the default.
 */
const props = withDefaults(
  defineProps<{
    title: string
    message: string
    consequence?: string
    confirmText?: string
    cancelText?: string
    busy?: boolean
  }>(),
  { confirmText: 'Xác nhận', cancelText: 'Huỷ', busy: false, consequence: '' },
)

const emit = defineEmits<{ (e: 'confirm'): void; (e: 'cancel'): void }>()

const titleId = 'confirm-title'
const bodyId = 'confirm-body'
const cancelButton = ref<HTMLButtonElement | null>(null)

function onConfirm(): void {
  if (props.busy) return
  emit('confirm')
}

function onCancel(): void {
  if (props.busy) return
  emit('cancel')
}

function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') onCancel()
}

onMounted(() => {
  cancelButton.value?.focus()
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.confirm-backdrop {
  position: fixed; inset: 0; z-index: 60;
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
  background: rgba(17, 24, 39, 0.45);
}
.confirm {
  width: min(440px, 100%);
  padding: 22px 24px 20px;
  background: var(--fg-surface);
  border-radius: var(--fg-radius-card);
  box-shadow: var(--fg-shadow-float);
}
.confirm__title { margin: 0 0 10px; font-size: 17px; }
.confirm__body { font-size: 14px; color: var(--fg-text); }
.confirm__body p { margin: 0 0 8px; }
.confirm__consequence {
  padding: 10px 12px;
  font-size: 13px;
  color: var(--fg-danger);
  background: var(--fg-danger-bg);
  border: 1px solid var(--fg-danger-border);
  border-radius: var(--fg-radius-control);
}
.confirm__actions {
  display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px;
}
.confirm__actions .btn-ghost { width: auto; min-width: 104px; }
.confirm__danger {
  display: inline-flex; align-items: center; justify-content: center;
  width: auto; min-width: 104px; min-height: 40px; padding: 8px 20px;
  font-family: inherit; font-size: 14px; font-weight: 600;
  color: #fff; background: var(--fg-danger);
  border: 1px solid transparent; border-radius: var(--fg-radius-control);
  cursor: pointer;
  transition: background 0.15s;
}
.confirm__danger:hover:not(:disabled) { background: #d63c3c; }
.confirm__danger:disabled { opacity: 0.55; cursor: not-allowed; }
.confirm__danger:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); }
</style>