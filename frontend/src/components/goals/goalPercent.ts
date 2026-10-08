/**
 * Spec §4.2 display rule: floor the stored scale-4 ratio at 2 decimals using
 * integer math on the decimal string — never binary floats (`0.29 * 10000` is
 * `2899.9999…` in float and would wrongly display `28.99%`). Clamped to 0–100.
 */
export function formatGoalPercent(progress: string): string {
  const match = /^\s*(-?)(\d+)(?:\.(\d+))?\s*$/.exec(progress)
  if (!match) return '0%'
  const sign = match[1] === '-' ? -1 : 1
  // Ratio × 10000 as an integer (server sends at most 4 decimals; extras are
  // truncated, which is floor-consistent).
  const scaled =
    sign * (Number(match[2]) * 10000 + Number(((match[3] ?? '') + '0000').slice(0, 4)))
  const clamped = Math.min(10000, Math.max(0, scaled))
  const whole = Math.floor(clamped / 100)
  const frac = clamped % 100
  return frac === 0 ? `${whole}%` : `${whole}.${String(frac).padStart(2, '0')}%`
}
