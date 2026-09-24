/**
 * Money text helpers for the shared money input control.
 *
 * Financial truth rule (specs/001-financial-profile/spec.md): the value that travels through the
 * model → API is always a decimal *string* with `.` as the decimal separator and never a JavaScript
 * number. These helpers only translate between that wire value and the Vietnamese dong presentation
 * the user types and reads (`.` groups thousands, `,` separates at most two decimals).
 *
 * Everything here is pure and side-effect free so the control itself stays small and testable.
 */

/** Grouping only — BigInt keeps very large amounts exact (no float rounding). */
export function groupWhole(whole: string): string {
  const digits = whole.replace(/\D/g, '')
  if (digits === '') return ''
  return new Intl.NumberFormat('vi-VN').format(BigInt(digits))
}

function stripLeadingZeros(whole: string): string {
  return whole.replace(/^0+(?=\d)/, '')
}

function trimTrailingZeros(fraction: string): string {
  return fraction.replace(/0+$/, '')
}

export interface ParsedAmountText {
  /** Wire value: `.` decimal separator, no grouping, at most two decimals, `''` when empty. */
  raw: string
  /** What the field shows: `.` groups thousands, `,` is the decimal separator being typed. */
  display: string
}

interface TypedParts {
  whole: string
  fraction: string
  separator: boolean
}

/**
 * Decide what the typed separators mean, deterministically.
 *
 * `.` is ALWAYS the thousands separator and `,` is ALWAYS the decimal separator, mirroring the
 * Vietnamese dong presentation the field itself shows. A dot therefore never carries value: it is
 * dropped and re-inserted by grouping, so typing `40.000.000` digit by digit cannot collapse into a
 * bogus `40,00` the way an "is this dot a decimal point?" guess would.
 */
function splitTyped(text: string): TypedParts {
  const commaAt = text.indexOf(',')
  if (commaAt >= 0) {
    return {
      whole: text.slice(0, commaAt).replace(/\./g, ''),
      fraction: text.slice(commaAt + 1).replace(/[.,]/g, ''),
      separator: true,
    }
  }

  return { whole: text.replace(/\./g, ''), fraction: '', separator: false }
}

/**
 * Sanitising parser used while the user types: whatever they paste or mash in, only digits and the
 * two separators survive, at most two decimals are kept (a third decimal digit is rejected), and a
 * minus sign can never get through (amounts are non-negative — FR-005).
 */
export function parseAmountText(text: string): ParsedAmountText {
  const stripped = (text ?? '').replace(/[^\d.,]/g, '')
  if (!/\d/.test(stripped)) {
    return { raw: '', display: '' }
  }

  const { whole: typedWhole, fraction: typedFraction, separator } = splitTyped(stripped)
  const fraction = typedFraction.slice(0, 2)
  const whole = stripLeadingZeros(typedWhole) || '0'

  return {
    raw: fraction === '' ? whole : `${whole}.${fraction}`,
    display: `${groupWhole(whole)}${separator ? `,${fraction}` : ''}`,
  }
}

/**
 * Server/wire value → what the field shows. `decimals: 2` pads for the resting (blurred) look,
 * `decimals: 'trim'` removes trailing zeros so the user can keep typing on focus. Unknown shapes are
 * returned untouched instead of being silently corrupted.
 */
export function formatAmountDisplay(raw: string, options: { decimals?: 2 | 'trim' } = {}): string {
  const value = (raw ?? '').trim()
  if (value === '') return ''

  const match = /^(\d+)(?:\.(\d+))?$/.exec(value)
  if (!match) return value

  const whole = stripLeadingZeros(match[1]) || '0'
  const typed = (match[2] ?? '').slice(0, 2)
  const fraction = options.decimals === 'trim' ? trimTrailingZeros(typed) : (typed + '00').slice(0, 2)

  return `${groupWhole(whole)}${fraction === '' ? '' : `,${fraction}`}`
}

/** Digits before the caret — used to keep the caret where the user was typing after re-formatting. */
export function countDigits(text: string): number {
  return (text.match(/\d/g) ?? []).length
}

/** Inverse of `countDigits` for the formatted text. */
export function caretAfterDigits(display: string, digits: number): number {
  if (digits <= 0) return 0
  let seen = 0
  for (let index = 0; index < display.length; index += 1) {
    if (/\d/.test(display[index])) {
      seen += 1
      if (seen === digits) return index + 1
    }
  }
  return display.length
}
