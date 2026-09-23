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
