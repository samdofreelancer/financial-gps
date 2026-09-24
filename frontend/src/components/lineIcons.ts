/**
 * Presentation-only icon hints for income/expense rows.
 *
 * The domain stores a free-text source/category, so the icon is guessed from the words the user
 * typed. This is decoration: it never changes a value, a total or a rule, and an unknown word
 * simply falls back to the neutral icon for its section.
 */

const INCOME_ICONS: ReadonlyArray<readonly [RegExp, string]> = [
  [/salary|lương|luong|payroll|wage/i, '💼'],
  [/business|shop|store|kinh doanh|buôn|ban hang|bán hàng/i, '🏪'],
  [/freelance|contract|dự án|du an|gig/i, '📈'],
  [/rent(al)? income|cho thuê|cho thue/i, '🏘️'],
  [/interest|lãi|tiet kiem|tiết kiệm|dividend|cổ tức/i, '🏦'],
]

const EXPENSE_ICONS: ReadonlyArray<readonly [RegExp, string]> = [
  [/rent|thuê|thue|mortgage|housing/i, '🏠'],
  [/food|meal|ăn|an uong|grocery|groceries|đi chợ/i, '🍚'],
  [/transport|travel|fuel|gas|xăng|xe|commute/i, '🚌'],
  [/utilit|electric|water|internet|phone|điện|nước/i, '💡'],
  [/health|medical|insurance|bảo hiểm|thuốc/i, '🩺'],
  [/education|school|tuition|học|hoc phi/i, '🎓'],
  [/childcare|child|kid|nuôi con|nuoi con|quỹ nuôi|quy nuoi|giữ trẻ|nhà trẻ|mẫu giáo|tiền sữa|tiền học/i, '👶'],
  [/debt|loan|credit|trả nợ|interest/i, '🏦'],
]

function pick(
  table: ReadonlyArray<readonly [RegExp, string]>,
  label: string | undefined,
  fallback: string,
): string {
  const text = (label ?? '').trim()
  if (!text) {
    return fallback
  }
  const match = table.find(([pattern]) => pattern.test(text))
  return match ? match[1] : fallback
}

/** 💰 — the neutral "this is money you receive" marker. */
export function incomeIcon(source: string | undefined): string {
  return pick(INCOME_ICONS, source, '💰')
}

/** 🧾 — the neutral "this is money you spend" marker. */
export function expenseIcon(category: string | undefined): string {
  return pick(EXPENSE_ICONS, category, '🧾')
}
