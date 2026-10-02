import { client, csrf, xsrfHeader } from './http'

export type DebtType = 'CREDIT_CARD' | 'MORTGAGE' | 'AUTO_LOAN' | 'STUDENT_LOAN' | 'PERSONAL_LOAN' | 'OTHER'
export type DebtStatus = 'ACTIVE' | 'PAID_OFF' | 'ARCHIVED'

export interface DebtProjection {
  status: 'AVAILABLE' | 'BLOCKED' | 'COMPLETED'
  projectedPayoffDate: string | null
  numberOfPayments: number | null
  totalInterest: string | null
  finalPayment: string | null
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
}

export interface DebtSummary {
  totalOutstandingDebt: string
  totalMinimumMonthlyPayment: string
  totalPlannedMonthlyPayment: string
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
