/** Typed test data — specs speak domain language, never raw option values. */

export type IncomeSource = 'salary' | 'business' | 'freelance' | 'rent' | 'investment' | 'other'

export type ExpenseCategory =
  | 'rent'
  | 'food'
  | 'transport'
  | 'utilities'
  | 'health'
  | 'education'
  | 'childcare'
  | 'debt'
  | 'other'

export type ExpenseType = 'FIXED' | 'VARIABLE'

export interface FinancialBasics {
  /** Vietnamese presentation, e.g. "100.000.000". */
  savings: string
  emergency: string
  dependents: string
}

export interface IncomeLine {
  /** Vietnamese presentation, e.g. "30.000.000". */
  amount: string
  source: IncomeSource
}

export interface ExpenseLine {
  /** Vietnamese presentation, e.g. "20.000.000". */
  amount: string
  category: ExpenseCategory
  type?: ExpenseType
}

export interface ServerTotals {
  income: string
  expenses: string
  freeCash: string
}

/** Canonical journey data — one user story, one set of numbers. */
export const journeyBasics: FinancialBasics = {
  savings: '100.000.000',
  emergency: '50.000.000',
  dependents: '2',
}

export const journeyIncome: IncomeLine = { amount: '30.000.000', source: 'salary' }

export const journeyExpense: ExpenseLine = { amount: '20.000.000', category: 'childcare', type: 'VARIABLE' }

export const totalsAfterIncome: ServerTotals = { income: '30.000.000', expenses: '0,00', freeCash: '30.000.000' }

export const totalsAfterExpense: ServerTotals = {
  income: '30.000.000',
  expenses: '20.000.000',
  freeCash: '10.000.000',
}

export const zeroTotals = { income: '0,00 VND', expenses: '0,00 VND', netCashFlow: '0,00 VND' } as const

/** Display label of the "nuôi con" option in the expense category dropdown. */
export const CHILDCARE_LABEL = 'Nuôi con'

/** A debt entered on /debts. Amounts are vi-VN presentation; rate is a 6dp decimal fraction. */
export interface DebtInput {
  creditor: string
  type?: 'CREDIT_CARD' | 'MORTGAGE' | 'AUTO_LOAN' | 'STUDENT_LOAN' | 'PERSONAL_LOAN' | 'OTHER'
  balance: string
  min: string
  planned: string
  rate?: string
}

/**
 * 002 debt journey data — income 30M with a 1.5M mandatory minimum is a clean 5.00% DTI, and the
 * dashboard position must show Free cash 28.5M once the debt exists.
 */
export const debtJourneyIncome: IncomeLine = { amount: '30.000.000', source: 'salary' }

export const solvableDebt: DebtInput = {
  creditor: 'Techcombank',
  type: 'CREDIT_CARD',
  balance: '15.000.000',
  min: '1.500.000',
  planned: '3.000.000',
  rate: '0.180000',
}

/** 10M @ 12% accrues 100,000/month: a planned 80,000 can never amortize (BLOCKED). */
export const blockedDebt: DebtInput = {
  creditor: 'Vay nóng',
  type: 'PERSONAL_LOAN',
  balance: '10.000.000',
  min: '50.000',
  planned: '80.000',
  rate: '0.120000',
}

export const debtJourneyTotals = {
  totalDebt: '15.000.000',
  totalMinimum: '1.500.000',
  dti: '5.00%',
  /** Dashboard: mandatory 1.5M reduces Free cash 30M - 1.5M. */
  mandatoryPayment: '1.500.000',
  freeCash: '28.500.000',
} as const

/** A goal entered on /goals. Amounts are vi-VN presentation; the date is ISO-8601. */
export interface GoalInput {
  name: string
  type?: 'DEBT_FREEDOM' | 'EMERGENCY_FUND' | 'SAVINGS' | 'HOUSING' | 'EDUCATION' | 'RETIREMENT' | 'OTHER'
  target: string
  current: string
  date?: string
  priority?: string
}

/**
 * 003 goal journey data — income 30M is the whole Available Capacity (no debts,
 * no expenses yet), so a 90M remainder over exactly 3 contribution periods
 * requires 30M/month: the MEETS_REQUIRED equality boundary (REF-A02). A 20M
 * expense then drops capacity to 10M: SHORTFALL with a 20M gap (REF-A03).
 */
export const goalJourneyIncome: IncomeLine = { amount: '30.000.000', source: 'salary' }

export const goalJourneyExpense: ExpenseLine = { amount: '20.000.000', category: 'food', type: 'VARIABLE' }

export const emergencyGoal: GoalInput = {
  name: 'Emergency Fund',
  type: 'EMERGENCY_FUND',
  target: '120.000.000',
  current: '30.000.000',
  priority: '1',
}

/** Target exactly three monthly periods out: required == available (equality).
 *
 * The day is clamped to the target month length (like Java's `plusMonths`),
 * so month-end runs (29th–31st) cannot overflow into a fourth month and make
 * `monthsRemaining != 3` depending on the run date.
 */
export function threePeriodsOut(): string {
  const asOf = new Date()
  const monthStart = new Date(asOf.getFullYear(), asOf.getMonth() + 3, 1)
  const lastDay = new Date(monthStart.getFullYear(), monthStart.getMonth() + 1, 0).getDate()
  monthStart.setDate(Math.min(asOf.getDate(), lastDay))
  const pad = (n: number): string => String(n).padStart(2, '0')
  return `${monthStart.getFullYear()}-${pad(monthStart.getMonth() + 1)}-${pad(monthStart.getDate())}`
}

export const goalJourneyExpected = {
  remaining: '90000000.00 VND',
  progress: '25%',
  required: '30000000.00',
  meetsCoverage: 'MEETS_REQUIRED',
  shortfallCoverage: 'SHORTFALL',
  shortfall: '20000000.00',
} as const
