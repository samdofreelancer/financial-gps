import { client, csrf, xsrfHeader } from './http'

export type DebtType = 'CREDIT_CARD' | 'MORTGAGE' | 'AUTO_LOAN' | 'STUDENT_LOAN' | 'PERSONAL_LOAN' | 'OTHER'
export type DebtStatus = 'ACTIVE' | 'PAID_OFF' | 'ARCHIVED'

export interface DebtProjection {
  status: 'AVAILABLE' | 'BLOCKED' | 'COMPLETED'
  projectedPayoffDate: string | null
  numberOfPayments: number | null
  totalInterest: string | null
  finalPayment: string | null
  /** Interest accrued in the first period (balance × rate / 12); null when the rate is unknown. */
  monthlyInterest: string | null
  reasonCode: string | null
  explanation: string | null
}

export interface DebtView {
  id: string
  creditor: string
  debtType: DebtType
  /** Nullable: null means the origination amount is unknown, never 0.00 (spec §4.1). */
  originalPrincipal: string | null
  outstandingBalance: string
  annualInterestRate: string | null
  minimumPayment: string
  plannedPayment: string
  dueDay: number | null
  status: DebtStatus
  currency: string
  projection: DebtProjection
  /** True only when this month's payment was manually marked done; balance is unchanged. */
  paidThisPeriod?: boolean
}

export interface DebtSummary {
  totalOutstandingDebt: string
  totalMinimumMonthlyPayment: string
  totalPlannedMonthlyPayment: string
  /** Sum of the per-debt monthly interest; null when any active debt has an unknown rate. */
  totalMonthlyAccruedInterest: string | null
  currency: string
  debtToIncome: { status: string; ratio: string | null; reasonCode: string | null; explanation: string | null }
  portfolioProjection: {
    status: string
    projectedDebtFreeDate: string | null
    totalMonthsRemaining: number | null
    totalInterestRemaining: string | null
    reasonCode: string | null
    explanation: string | null
    blockedDebts: { creditor: string; reasonCode: string; explanation: string }[]
  }
  blockedDebtCount: number
  asOf: string
}

/** One period of the payment calendar, computed server-side by the amortization engine. */
export interface DebtScheduleRow {
  period: number
  dueDate: string
  payment: string
  principal: string
  interest: string
  endingBalance: string
}

/**
 * GET /debts/{id}/schedule — the server stays the single source of truth for the calendar; the
 * browser never re-implements amortization. BLOCKED/COMPLETED come with an empty row list.
 */
export interface DebtSchedule {
  status: 'AVAILABLE' | 'BLOCKED' | 'COMPLETED'
  payoffDate: string | null
  numberOfPayments: number | null
  totalInterest: string | null
  finalPayment: string | null
  reasonCode: string | null
  explanation: string | null
  currency: string
  rows: DebtScheduleRow[]
}

export interface DebtPayload {
  creditor: string
  debtType: DebtType
  /** Nullable: omit the field entirely when the origination amount is unknown (never send 0). */
  originalPrincipal: string | null
  outstandingBalance: string
  annualInterestRate: string | null
  minimumPayment: string
  plannedPayment: string
  dueDay: number | null
}

export async function listDebts(): Promise<DebtView[]> {
  const { data } = await client.get<DebtView[]>('/api/v1/debts')
  return data
}

export async function getDebtSummary(): Promise<DebtSummary> {
  const { data } = await client.get<DebtSummary>('/api/v1/debts/summary')
  return data
}

export async function postDebt(body: DebtPayload): Promise<DebtView> {
  await csrf()
  const { data } = await client.post<DebtView>('/api/v1/debts', body, { headers: xsrfHeader() })
  return data
}

export async function putDebt(id: string, body: DebtPayload): Promise<DebtView> {
  await csrf()
  const { data } = await client.put<DebtView>(`/api/v1/debts/${id}`, body, { headers: xsrfHeader() })
  return data
}

export async function getDebtSchedule(id: string): Promise<DebtSchedule> {
  const { data } = await client.get<DebtSchedule>(`/api/v1/debts/${id}/schedule`)
  return data
}

/** Manually mark this month's payment as done; no payment is initiated. */
export async function markDebtPaid(id: string): Promise<DebtView> {
  await csrf()
  const { data } = await client.post<DebtView>(`/api/v1/debts/${id}/payment-mark`, {}, { headers: xsrfHeader() })
  return data
}

/** Undo the current month's manual payment marker. */
export async function undoDebtPaymentMark(id: string): Promise<DebtView> {
  await csrf()
  const { data } = await client.delete<DebtView>(`/api/v1/debts/${id}/payment-mark`, { headers: xsrfHeader() })
  return data
}

export async function deleteDebt(id: string): Promise<void> {
  await csrf()
  await client.delete(`/api/v1/debts/${id}`, { headers: xsrfHeader() })
}

/** Client-side guard: planned must cover minimum (server is authoritative). */
export function isPlannedValid(minimumPayment: string, plannedPayment: string): boolean {
  if (!/^\d+(\.\d{1,2})?$/.test(minimumPayment.trim())) return false
  if (!/^\d+(\.\d{1,2})?$/.test(plannedPayment.trim())) return false
  const [aWhole = '0', aFrac = ''] = minimumPayment.trim().split('.')
  const [bWhole = '0', bFrac = ''] = plannedPayment.trim().split('.')
  const a = BigInt(aWhole + (aFrac + '00').slice(0, 2))
  const b = BigInt(bWhole + (bFrac + '00').slice(0, 2))
  return b >= a
}

/** Rate guard: empty/null means missing (never silently 0%); otherwise a 6dp fraction. */
export function isRateValid(rate: string | null | undefined): boolean {
  if (rate == null || rate === '') return true
  return /^\d+(\.\d{1,6})?$/.test(rate.trim())
}

/** Due-day guard: empty/null means "unknown" (never a guessed day 15); otherwise 1–31. */
export function isDueDayValid(day: string | number | null | undefined): boolean {
  if (day == null) return true
  // v-model on <input type="number"> writes a number back into the form state (Vue looseToNumber).
  const text = String(day).trim()
  if (text === '') return true
  if (!/^\d{1,2}$/.test(text)) return false
  const value = Number(text)
  return value >= 1 && value <= 31
}
