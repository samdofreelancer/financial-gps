import { describe, expect, it } from 'vitest'
import { formatMoney, isDecimalAmount } from '@/api/profile'

describe('profile money safety', () => {
  it('accepts decimal strings and rejects negatives/numbers-as-truth', () => {
    expect(isDecimalAmount('74.00')).toBe(true)
    expect(isDecimalAmount('0.00')).toBe(true)
    expect(isDecimalAmount('-5.00')).toBe(false)
    expect(isDecimalAmount('abc')).toBe(false)
    expect(isDecimalAmount('10.123')).toBe(false)
  })

  it('formats server strings for display only', () => {
    expect(formatMoney('74.00', 'VND')).toContain('74')
  })
})
