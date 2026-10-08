/**
 * Percent text from a decimal ratio string via integer math — never binary floats.
 *
 * Half-up rounds at the 2nd decimal (display rounding for rates/DTI — unlike goal progress,
 * which floors to never overstate completion). Inputs are server-sent ratios with at most
 * 6 decimals, so the first-5-digits truncation below is exact for every real caller.
 */
export function formatRatioPercent(
  ratio: string | null | undefined,
  trimTrailingZeros = false,
): string | null {
  const match = /^\s*(-?)(\d+)(?:\.(\d+))?\s*$/.exec(ratio ?? '')
  if (!match) return null
  // Thousandths of a percent, then half-up to hundredths.
  const thousandths =
    Number(match[2]) * 100000 + Number(((match[3] ?? '') + '00000').slice(0, 5))
  const hundredths = Math.floor((thousandths + 5) / 10)
  const prefix = match[1] === '-' && hundredths !== 0 ? '-' : ''
  const whole = Math.floor(hundredths / 100)
  let frac = String(hundredths % 100).padStart(2, '0')
  // Trim mode mirrors Intl(min 0, max 2): "18.00" → "18%", "13.20" → "13.2%".
  if (trimTrailingZeros) {
    frac = frac.replace(/0+$/, '')
    if (frac === '') return `${prefix}${whole}%`
  }
  return `${prefix}${whole}.${frac}%`
}

/**
 * Canonical "NNNNN[.NN]" → integer minor units (cents). Rejects anything else
 * (separators, signs, >2 decimals) instead of guessing — callers treat null as
 * "not a renderable amount".
 */
export function parseDecimalCents(value: string | null | undefined): bigint | null {
  const match = /^\s*(\d+)(?:\.(\d{1,2}))?\s*$/.exec(value ?? '')
  if (!match) return null
  return BigInt(match[1]) * 100n + BigInt(((match[2] ?? '') + '00').slice(0, 2))
}

/** Minor units → canonical "NNNNN.NN". */
export function formatCents(cents: bigint): string {
  const abs = cents < 0n ? -cents : cents
  return `${abs / 100n}.${String(abs % 100n).padStart(2, '0')}`
}
