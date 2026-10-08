import { describe, expect, it } from 'vitest'
import { formatCents, formatRatioPercent, parseDecimalCents } from '@/components/ratioPercent'

describe('formatRatioPercent (half-up, integer math)', () => {
  it('formats server scale-4 ratios with two decimals', () => {
    expect(formatRatioPercent('0.2703')).toBe('27.03%')
    expect(formatRatioPercent('0.0250')).toBe('2.50%')
    expect(formatRatioPercent('0.05')).toBe('5.00%')
  })

  it('rounds half up at the second decimal', () => {
    expect(formatRatioPercent('0.12055')).toBe('12.06%')
    expect(formatRatioPercent('0.12054')).toBe('12.05%')
  })

  it('does not fall for the binary-float trap', () => {
    // Number("0.29") * 100 is 28.999999999999996 in float.
    expect(formatRatioPercent('0.29')).toBe('29.00%')
    expect(formatRatioPercent('0.123456')).toBe('12.35%')
  })

  it('trims trailing zeros on request', () => {
    expect(formatRatioPercent('0.18', true)).toBe('18%')
    expect(formatRatioPercent('0.1205', true)).toBe('12.05%')
  })

  it('rejects non-ratios', () => {
    expect(formatRatioPercent(null)).toBeNull()
    expect(formatRatioPercent('')).toBeNull()
    expect(formatRatioPercent('abc')).toBeNull()
  })
})

describe('parseDecimalCents / formatCents', () => {
  it('parses canonical amounts to integer cents', () => {
    expect(parseDecimalCents('5000000.00')).toBe(500000000n)
    expect(parseDecimalCents('100')).toBe(10000n)
    expect(parseDecimalCents('0.05')).toBe(5n)
  })

  it('rejects non-canonical amounts instead of guessing', () => {
    expect(parseDecimalCents(null)).toBeNull()
    expect(parseDecimalCents('')).toBeNull()
    expect(parseDecimalCents('1.234')).toBeNull()
    expect(parseDecimalCents('-5.00')).toBeNull()
    expect(parseDecimalCents('5.000.000')).toBeNull()
  })

  it('formats cents back to canonical decimals', () => {
    expect(formatCents(500000000n)).toBe('5000000.00')
    expect(formatCents(-2000000n)).toBe('20000.00')
    expect(formatCents(5n)).toBe('0.05')
  })
})
