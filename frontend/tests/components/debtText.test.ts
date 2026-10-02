import { describe, expect, it } from 'vitest'
import {
  blockerBadge,
  blockerMessage,
  dtiRating,
  needsHigherPayment,
  rateLabel,
} from '@/components/debtText'
import { maskEmail } from '@/api/auth'

/**
 * Presentation vocabulary only: these helpers decide how a server verdict is worded, never what
 * it is. The tests pin the two properties the debt screen depends on — nothing technical leaks
 * into the default view, and a missing rate is never dressed up as 0%.
 */
describe('blocker wording', () => {
  it('translates the codes a user actually hits', () => {
    expect(blockerMessage('PAYMENT_DOES_NOT_COVER_INTEREST')).toContain('nhỏ hơn tiền lãi phát sinh')
    expect(blockerMessage('INTEREST_RATE_MISSING')).toContain('Chưa nhập lãi suất')
    expect(blockerBadge('PAYMENT_DOES_NOT_COVER_INTEREST')).toBe('Trả không đủ lãi')
  })

  it('never echoes the raw enum or the server English prose', () => {
    for (const code of ['PAYMENT_DOES_NOT_COVER_INTEREST', 'PAYMENT_COVERS_ONLY_INTEREST']) {
      expect(blockerMessage(code)).not.toContain(code)
    }
    expect(blockerMessage('SOME_CODE_WE_HAVE_NEVER_SEEN')).not.toContain('SOME_CODE_WE_HAVE_NEVER_SEEN')
    expect(blockerMessage(null)).toContain('chưa thể dự báo')
  })

  it('knows which blockers are fixed by paying more', () => {
    expect(needsHigherPayment('PAYMENT_DOES_NOT_COVER_INTEREST')).toBe(true)
    expect(needsHigherPayment('PAYMENT_COVERS_ONLY_INTEREST')).toBe(true)
    expect(needsHigherPayment('INTEREST_RATE_MISSING')).toBe(false)
  })
})

describe('rateLabel', () => {
  it('renders a 6dp fraction as a yearly percentage', () => {
    expect(rateLabel('0.180000')).toBe('18%/năm')
    expect(rateLabel('0.120500')).toBe('12,05%/năm')
  })

  it('keeps a missing rate missing instead of implying 0%', () => {
    expect(rateLabel(null)).toBe('Chưa nhập')
    expect(rateLabel('')).toBe('Chưa nhập')
  })
})

describe('dtiRating', () => {
  it('grades against the 36% benchmark', () => {
    expect(dtiRating('0.10')?.label).toBe('Rất thoải mái')
    expect(dtiRating('0.278')?.label).toBe('Khá an toàn')
    expect(dtiRating('0.40')?.label).toBe('Cần chú ý')
    expect(dtiRating('0.55')?.label).toBe('Rủi ro cao')
  })

  it('clamps the bar instead of overflowing past the marker', () => {
    expect(dtiRating('0.90')?.fill).toBe(100)
    expect(dtiRating('0.90')?.tone).toBe('bad')
  })

  it('stays silent when the server could not compute the ratio', () => {
    expect(dtiRating(null)).toBeNull()
    expect(dtiRating('')).toBeNull()
  })
})

describe('maskEmail', () => {
  it('keeps the shape recognisable without exposing the address', () => {
    expect(maskEmail('ginseng1000years@gmail.com')).toBe('g••••••••@gmail.com')
    expect(maskEmail('ginseng1000years@gmail.com')).not.toContain('ginseng1000years')
  })

  it('degrades safely on an unexpected value', () => {
    expect(maskEmail('')).toBe('')
    expect(maskEmail(null)).toBe('')
    expect(maskEmail('no-at-sign')).toBe('••••••')
  })
})