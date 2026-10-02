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

/** Display label of the "quỹ nuôi con" option in the expense category dropdown. */
export const CHILDCARE_LABEL = 'Quỹ nuôi con / childcare'

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
