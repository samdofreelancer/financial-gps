import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * Confirmation that an action actually happened.
 *
 * The debt list is re-read from the server after every write, so a silent screen can look frozen.
 * A short, dismissible message closes that gap. Auto-dismiss is a courtesy, not the only exit:
 * every toast keeps its close button and the stack is announced politely to screen readers.
 */
const TOAST_TIMEOUT_MS = 5000

export interface Toast {
  id: number
  message: string
  kind: 'success' | 'error'
}

export const useToasts = defineStore('toasts', () => {
  const toasts = ref<Toast[]>([])
  const timers = new Map<number, ReturnType<typeof setTimeout>>()
  let nextId = 1

  function dismiss(id: number): void {
    const timer = timers.get(id)
    if (timer) {
      clearTimeout(timer)
      timers.delete(id)
    }
    toasts.value = toasts.value.filter((toast) => toast.id !== id)
  }

  function push(message: string, kind: Toast['kind']): void {
    const id = nextId++
    toasts.value = [...toasts.value, { id, message, kind }]
    timers.set(
      id,
      setTimeout(() => dismiss(id), TOAST_TIMEOUT_MS),
    )
  }

  /** Convenience wrappers so callers read as the action they just performed. */
  function success(message: string): void {
    push(message, 'success')
  }

  function error(message: string): void {
    push(message, 'error')
  }

  return { toasts, push, success, error, dismiss }
})
