import axios from 'axios'

export interface MoneyView {
  amount: string
  currency: string
  provenance: 'actual' | 'calculated'
}

export interface ProvenanceView {
  field: string
  kind: 'actual' | 'assumed' | 'calculated'
  detail: string
}

export interface ProfileLine {
  id: string
  amount: string
  currency: string
  source?: string
  category?: string
  expenseType?: 'FIXED' | 'VARIABLE'
  provenance: 'actual' | 'calculated'
}

export interface ProfileView {
  currency: string
  savingsAmount: string
  emergencyFundAmount: string
  dependentsCount: number
  incomes: ProfileLine[]
  expenses: ProfileLine[]
  totalIncome: MoneyView
  totalExpenses: MoneyView
  netCashFlow: MoneyView
  availableCapacity: MoneyView
  provenance: ProvenanceView[]
  asOf: string
}

const client = axios.create({ withCredentials: true })

async function csrf(): Promise<void> {
  await client.get('/api/v1/auth/csrf')
}

function xsrfHeader(): Record<string, string> {
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)
  return match ? { 'X-XSRF-TOKEN': decodeURIComponent(match[1]) } : {}
}

export async function getProfile(): Promise<ProfileView> {
  const { data } = await client.get<ProfileView>('/api/v1/profile')
  return data
}

export async function putProfile(body: {
  currency: string
  savingsAmount: string
  emergencyFundAmount: string
  dependentsCount: number
}): Promise<ProfileView> {
  await csrf()
  const { data } = await client.put<ProfileView>('/api/v1/profile', body, { headers: xsrfHeader() })
  return data
}

export async function postIncome(body: { amount: string; source: string }): Promise<void> {
  await csrf()
  await client.post('/api/v1/incomes', body, { headers: xsrfHeader() })
}

export async function putIncome(id: string, body: { amount: string; source: string }): Promise<void> {
  await csrf()
  await client.put(`/api/v1/incomes/${id}`, body, { headers: xsrfHeader() })
}

export async function deleteIncome(id: string): Promise<void> {
  await csrf()
  await client.delete(`/api/v1/incomes/${id}`, { headers: xsrfHeader() })
}

export async function postExpense(body: {
  amount: string
  category: string
  expenseType: 'FIXED' | 'VARIABLE'
}): Promise<void> {
  await csrf()
  await client.post('/api/v1/expenses', body, { headers: xsrfHeader() })
}

export async function putExpense(
  id: string,
  body: { amount: string; category: string; expenseType: 'FIXED' | 'VARIABLE' },
): Promise<void> {
  await csrf()
  await client.put(`/api/v1/expenses/${id}`, body, { headers: xsrfHeader() })
}

export async function deleteExpense(id: string): Promise<void> {
  await csrf()
  await client.delete(`/api/v1/expenses/${id}`, { headers: xsrfHeader() })
}

/** Presentation-only decimal validation; the server is authoritative. */
export function isDecimalAmount(value: string): boolean {
  return /^\d+(\.\d{1,2})?$/.test(value.trim())
}

/**
 * Display formatting only — the amount string is never converted to a JS number (money is
 * decimal-safe end-to-end: PostgreSQL → Java → JSON → Vue).
 */
export function formatMoney(amount: string, currency: string): string {
  const negative = amount.startsWith('-')
  const digits = negative ? amount.slice(1) : amount
  const [whole = '0', fraction = ''] = digits.split('.')
  if (!/^\d+$/.test(whole)) {
    return `${amount} ${currency}`
  }
  const grouped = new Intl.NumberFormat('vi-VN').format(BigInt(whole))
  const decimals = (fraction + '00').slice(0, 2)
  return `${negative ? '-' : ''}${grouped},${decimals} ${currency}`
}
