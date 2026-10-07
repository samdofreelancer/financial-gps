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

  it('drops the ",00" tail only when compact VND formatting is asked for', () => {
    expect(formatMoney('384000000.00', 'VND', { compact: true })).toBe('384.000.000 VND')
    // The two-decimal default is untouched, so every other screen keeps its look.
    expect(formatMoney('384000000.00', 'VND')).toBe('384.000.000,00 VND')
  })

  it('never rounds away a real fraction when compacting', () => {
    expect(formatMoney('1234.50', 'VND', { compact: true })).toBe('1.234,50 VND')
  })
})
