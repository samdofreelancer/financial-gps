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
  { value: 'salary', label: 'Lương' },
  { value: 'business', label: 'Kinh doanh / nghề phụ' },
  { value: 'freelance', label: 'Freelance / hợp đồng' },
  { value: 'rent', label: 'Cho thuê nhà' },
  { value: 'investment', label: 'Đầu tư / lãi / cổ tức' },
  { value: 'other', label: 'Khác' },
]

export const EXPENSE_CATEGORIES: ReadonlyArray<LineOption> = [
  { value: 'rent', label: 'Thuê nhà / nhà ở' },
  { value: 'food', label: 'Ăn uống / thực phẩm' },
  { value: 'transport', label: 'Đi lại / xăng xe' },
  { value: 'utilities', label: 'Điện nước / internet / điện thoại' },
  { value: 'health', label: 'Sức khỏe / bảo hiểm' },
  { value: 'education', label: 'Học tập' },
  { value: 'childcare', label: 'Nuôi con' },
  { value: 'debt', label: 'Trả nợ / vay' },
  { value: 'other', label: 'Khác' },
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
