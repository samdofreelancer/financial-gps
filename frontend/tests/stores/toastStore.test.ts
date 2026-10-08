import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useToasts } from '@/stores/toastStore'

describe('toast store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('pushes success and error toasts with distinct kinds', () => {
    const store = useToasts()
    store.success('Saved.')
    store.error('Failed.')

    expect(store.toasts).toHaveLength(2)
    expect(store.toasts[0]).toMatchObject({ message: 'Saved.', kind: 'success' })
    expect(store.toasts[1]).toMatchObject({ message: 'Failed.', kind: 'error' })
  })

  it('auto-dismisses after the timeout and honors manual dismiss', () => {
    const store = useToasts()
    store.success('First.')
    store.success('Second.')
    const [first] = store.toasts

    store.dismiss(first.id)
    expect(store.toasts.map((t) => t.message)).toEqual(['Second.'])

    vi.advanceTimersByTime(5000)
    expect(store.toasts).toHaveLength(0)
  })
})
