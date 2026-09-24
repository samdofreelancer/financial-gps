import { describe, expect, it } from 'vitest'
import {
  caretAfterDigits,
  countDigits,
  formatAmountDisplay,
  groupWhole,
  parseAmountText,
} from '@/components/moneyText'

/**
 * Spec 001 (T014): the field reads like a VND amount ('.' groups thousands, ',' separates two
decimals) while the value handed to the API stays a decimal string with '.'.
 */
describe('moneyText', () => {
  describe('parseAmountText (typing)', () => {
    it('reads plain digits as whole units', () => {
      expect(parseAmountText('30000000')).toEqual({ raw: '30000000', display: '30.000.000' })
    })

    it('reads dots as thousands grouping when no decimal separator is present', () => {
      expect(parseAmountText('1.234.567')).toEqual({ raw: '1234567', display: '1.234.567' })
    })

    it('reads a comma as the decimal separator and dots as thousands grouping', () => {
      expect(parseAmountText('1234567,5')).toEqual({ raw: '1234567.5', display: '1.234.567,5' })
      expect(parseAmountText('30.000.000,25')).toEqual({
        raw: '30000000.25',
        display: '30.000.000,25',
      })
    })

    it('never lets a typed dot become the decimal separator, so grouping cannot corrupt a value', () => {
      // Regression: typing 40.000.000 used to flip to "40,00" as soon as the first dot was typed.
      expect(parseAmountText('40.')).toEqual({ raw: '40', display: '40' })
      expect(parseAmountText('40.0')).toEqual({ raw: '400', display: '400' })
      expect(parseAmountText('40.000')).toEqual({ raw: '40000', display: '40.000' })
      expect(parseAmountText('40.000.000')).toEqual({ raw: '40000000', display: '40.000.000' })
    })

    it('keeps the trailing comma while the decimals are still being typed', () => {
      expect(parseAmountText('30.000.000,')).toEqual({ raw: '30000000', display: '30.000.000,' })
    })

    it('drops letters, currency words, signs and pasted noise', () => {
      expect(parseAmountText('VND -1 234,5 d')).toEqual({ raw: '1234.5', display: '1.234,5' })
      expect(parseAmountText('abc')).toEqual({ raw: '', display: '' })
      expect(parseAmountText('-1.000')).toEqual({ raw: '1000', display: '1.000' })
    })

    it('keeps at most two decimals', () => {
      expect(parseAmountText('1,239')).toEqual({ raw: '1.23', display: '1,23' })
    })

    it('strips leading zeros but never loses the value', () => {
      expect(parseAmountText('007,10')).toEqual({ raw: '7.10', display: '7,10' })
      expect(parseAmountText('000')).toEqual({ raw: '0', display: '0' })
    })

    it('treats an empty or separator-only field as no amount at all', () => {
      expect(parseAmountText('')).toEqual({ raw: '', display: '' })
      expect(parseAmountText(',')).toEqual({ raw: '', display: '' })
    })

    it('keeps very large amounts exact (no float rounding)', () => {
      expect(parseAmountText('92233720368547758,07')).toEqual({
        raw: '92233720368547758.07',
        display: '92.233.720.368.547.758,07',
      })
    })
  })

  describe('formatAmountDisplay (server value)', () => {
    it('pads the resting look to two decimals', () => {
      expect(formatAmountDisplay('30000000')).toBe('30.000.000,00')
      expect(formatAmountDisplay('30000000.5')).toBe('30.000.000,50')
    })

    it('trims trailing zeros for the editing look', () => {
      expect(formatAmountDisplay('30000000.00', { decimals: 'trim' })).toBe('30.000.000')
      expect(formatAmountDisplay('30.50', { decimals: 'trim' })).toBe('30,5')
      expect(formatAmountDisplay('30.05', { decimals: 'trim' })).toBe('30,05')
    })

    it('keeps an empty model empty and never invents a value', () => {
      expect(formatAmountDisplay('')).toBe('')
      expect(formatAmountDisplay('   ')).toBe('')
    })

    it('returns an unexpected shape untouched instead of corrupting it', () => {
      expect(formatAmountDisplay('not-a-number')).toBe('not-a-number')
    })
  })

  describe('groupWhole', () => {
    it('groups thousands and ignores non-digits', () => {
      expect(groupWhole('1234567')).toBe('1.234.567')
      expect(groupWhole('1.2.3')).toBe('123')
      expect(groupWhole('')).toBe('')
    })
  })

  describe('caret helpers', () => {
    it('counts digits so the caret survives re-formatting', () => {
      expect(countDigits('1.234,56')).toBe(6)
      expect(countDigits('1.234,')).toBe(4)
    })

    it('maps a digit count back to a caret position', () => {
      expect(caretAfterDigits('1.234,56', 3)).toBe(4)
      expect(caretAfterDigits('1.234,56', 0)).toBe(0)
      expect(caretAfterDigits('1.234,56', 99)).toBe(8)
    })
  })
})
