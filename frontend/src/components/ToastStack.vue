<template>
  <!--
    Screen-reader announcements plus a visible stack. `aria-live="polite"` means a screen reader
    finishes whatever it is saying before reading the new confirmation, instead of interrupting.
  -->
  <div class="toasts" role="status" aria-live="polite">
    <div v-for="toast in toasts" :key="toast.id" class="toast" :class="`toast--${toast.kind}`">
      <svg class="toast__icon" viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
        <template v-if="toast.kind === 'success'">
          <path d="M3.5 8.5l3 3 6-6.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
        </template>
        <template v-else>
          <circle cx="8" cy="8" r="6.2" stroke="currentColor" stroke-width="1.4" />
          <path d="M8 5v4M8 10.8v.6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
        </template>
      </svg>
      <span class="toast__text">{{ toast.message }}</span>
      <button type="button" class="toast__close" aria-label="Đóng thông báo" @click="dismiss(toast.id)">
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
          <path d="M4.5 4.5l7 7M11.5 4.5l-7 7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useToasts } from '../stores/toastStore'

/**
 * Confirms that an action actually happened. Without it a save/delete is silent and the user
 * cannot tell a slow network from a no-op. Messages are short and Vietnamese; the auto-dismiss
 * timer is long enough to read but never traps the message on screen.
 */
const { toasts, dismiss } = useToasts()
</script>

<style scoped>
.toasts {
  position: fixed; right: 20px; bottom: 20px; z-index: 50;
  display: grid; gap: 10px; justify-items: end;
  max-width: min(360px, calc(100vw - 40px));
}
.toast {
  display: flex; align-items: flex-start; gap: 10px;
  width: 100%; padding: 12px 14px;
  font-size: 13px; color: var(--fg-ink);
  background: var(--fg-surface);
  border: 1px solid var(--fg-border-soft);
  border-left: 3px solid var(--fg-primary);
  border-radius: var(--fg-radius-control);
  box-shadow: var(--fg-shadow-float);
  animation: toast-pop 0.16s ease-out;
}
.toast--success { border-left-color: var(--fg-success); }
.toast--error { border-left-color: var(--fg-danger); }
.toast__icon { width: 16px; height: 16px; flex: 0 0 16px; margin-top: 1px; color: var(--fg-primary); }
.toast--success .toast__icon { color: var(--fg-success); }
.toast--error .toast__icon { color: var(--fg-danger); }
.toast__text { flex: 1; min-width: 0; overflow-wrap: anywhere; }
.toast__close {
  flex: 0 0 auto; display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; padding: 0;
  color: var(--fg-muted); background: none; border: 0; border-radius: 4px; cursor: pointer;
}
.toast__close:hover { color: var(--fg-ink); background: rgba(0, 0, 0, 0.05); }
.toast__close:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); }
.toast__close svg { width: 12px; height: 12px; }

@keyframes toast-pop {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 640px) {
  .toasts { left: 14px; right: 14px; bottom: 14px; max-width: none; justify-items: stretch; }
}
</style>
