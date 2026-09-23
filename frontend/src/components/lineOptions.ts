/**
 * Controlled vocabularies for Income/Expense add/edit forms.
 *
 * The backend DTO/validators still accept any non-blank source/category string, and the domain
 * stores free text. This file is a UX boundary only: it narrows what the user can type so the
 * profile stays readable and the row icons stay predictable. Adding a new option here does NOT
 * change the API/DB/domain — it is only a frontend controlled vocabulary.
 */

export interface LineOption {
  value: string
  label: string
}

export const INCOME_SOURCES: ReadonlyArray<LineOption> = [
  { value: 'salary', label: 'Salary' },
  { value: 'business', label: 'Business / side business' },
  { value: 'freelance', label: 'Freelance / contract' },
  { value: 'rent', label: 'Rent / rental income' },
  { value: 'investment', label: 'Investment / interest / dividend' },
  { value: 'other', label: 'Other' },
]

export const EXPENSE_CATEGORIES: ReadonlyArray<LineOption> = [
  { value: 'rent', label: 'Rent / housing' },
  { value: 'food', label: 'Food / groceries' },
  { value: 'transport', label: 'Transport / fuel' },
  { value: 'utilities', label: 'Utilities / internet / phone' },
  { value: 'health', label: 'Health / insurance' },
  { value: 'education', label: 'Education' },
  { value: 'childcare', label: 'Quỹ nuôi con / childcare' },
  { value: 'debt', label: 'Debt / loan payment' },
  { value: 'other', label: 'Other' },
]

/** Export aliases used by the forms. */
export const SOURCE_OPTIONS = INCOME_SOURCES
export const CATEGORY_OPTIONS = EXPENSE_CATEGORIES

/** Presentational labels only — used to pre-fill newly created rows before the server returns them. */
export const INCOME_SOURCE_LABEL: Record<string, string> = Object.fromEntries(
  INCOME_SOURCES.map((o) => [o.value, o.label]),
)
export const EXPENSE_CATEGORY_LABEL: Record<string, string> = Object.fromEntries(
  EXPENSE_CATEGORIES.map((o) => [o.value, o.label]),
)
