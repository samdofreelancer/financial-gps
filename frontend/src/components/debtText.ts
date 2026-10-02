/**
 * Vietnamese presentation vocabulary for the debt screens.
 *
 * The server stays the single source of financial truth: it owns every amount, the projection
 * status and the reason codes. This module only decides *how a reason is worded*, which label a
 * badge carries and how a server ratio is coloured — never a number the server did not send.
 *
 * Raw codes and the server's English explanations are deliberately kept out of the default view:
 * they belong in a collapsed "Chi tiết kỹ thuật" block so they stay available without shouting.
 */

export type DebtTypeCode = 'CREDIT_CARD' | 'MORTGAGE' | 'AUTO_LOAN' | 'STUDENT_LOAN' | 'PERSONAL_LOAN' | 'OTHER'
export type DebtStatusCode = 'ACTIVE' | 'PAID_OFF' | 'ARCHIVED'

export const DEBT_TYPE_LABELS: Record<DebtTypeCode, string> = {
  CREDIT_CARD: 'Thẻ tín dụng',
  MORTGAGE: 'Vay mua nhà',
  AUTO_LOAN: 'Vay mua xe',
  STUDENT_LOAN: 'Vay học tập',
  PERSONAL_LOAN: 'Vay cá nhân',
  OTHER: 'Khoản nợ khác',
}

export const DEBT_STATUS_LABELS: Record<DebtStatusCode, string> = {
  ACTIVE: 'Đang trả',
  PAID_OFF: 'Đã trả hết',
  ARCHIVED: 'Đã ẩn',
}

/** Short badge text: the reader learns the situation, not the enum name. */
const BLOCKER_BADGE_LABELS: Record<string, string> = {
  PAYMENT_DOES_NOT_COVER_INTEREST: 'Trả không đủ lãi',
  PAYMENT_COVERS_ONLY_INTEREST: 'Chỉ đủ bù lãi',
  INTEREST_RATE_MISSING: 'Thiếu lãi suất',
  PAYOFF_HORIZON_EXCEEDS_MAXIMUM: 'Trả quá 30 năm',
}

/** One sentence per blocker, in the reader's language. No technical codes. */
const BLOCKER_MESSAGES: Record<string, string> = {
  PAYMENT_DOES_NOT_COVER_INTEREST:
    'Khoản trả mỗi tháng nhỏ hơn tiền lãi phát sinh, nên dư nợ sẽ tăng dần mỗi tháng.',
  PAYMENT_COVERS_ONLY_INTEREST:
    'Khoản trả mỗi tháng chỉ vừa đủ bù tiền lãi, nên dư nợ sẽ đứng yên và không bao giờ giảm.',
  INTEREST_RATE_MISSING:
    'Chưa nhập lãi suất của khoản nợ nên chưa thể tính lãi và dự báo ngày hết nợ.',
  PAYOFF_HORIZON_EXCEEDS_MAXIMUM:
    'Số kỹ trả vượt quá giới hạn mô phỏng 30 năm, nên hệ thống không dự báo được ngày hết nợ.',
  PORTFOLIO_CONTAINS_BLOCKED_DEBTS:
    'Có ít nhất một khoản nợ không thể dự báo, nên toàn bộ danh mục chưa có ngày hết nợ.',
  ZERO_OR_MISSING_INCOME: 'Chưa có thu nhập hằng tháng để tính tỷ lệ nợ trên thu nhập.',
}

const FALLBACK_BLOCKER = 'Khoản nợ này chưa thể dự báo ngày hết nợ với các thông tin hiện có.'

/** Vietnamese explanation for a server reason code; unknown codes fall back to a safe sentence. */
export function blockerMessage(reasonCode: string | null | undefined): string {
  if (!reasonCode) return FALLBACK_BLOCKER
  return BLOCKER_MESSAGES[reasonCode] ?? FALLBACK_BLOCKER
}

/** Short red/amber badge text for a debt row. */
export function blockerBadge(reasonCode: string | null | undefined): string {
  if (!reasonCode) return 'Chưa dự báo được'
  return BLOCKER_BADGE_LABELS[reasonCode] ?? 'Chưa dự báo được'
}

/** True when the fix is "pay more per month" rather than "tell us the rate". */
export function needsHigherPayment(reasonCode: string | null | undefined): boolean {
  return (
    reasonCode === 'PAYMENT_DOES_NOT_COVER_INTEREST' || reasonCode === 'PAYMENT_COVERS_ONLY_INTEREST'
  )
}

export function debtTypeLabel(type: DebtTypeCode | string): string {
  return DEBT_TYPE_LABELS[type as DebtTypeCode] ?? 'Khoản nợ khác'
}

export function debtStatusLabel(status: DebtStatusCode | string): string {
  return DEBT_STATUS_LABELS[status as DebtStatusCode] ?? 'Đang trả'
}

/** `0.180000` (6dp fraction) → "18%". Missing stays missing — never shown as 0%. */
export function rateLabel(rate: string | null | undefined): string {
  if (rate == null || rate === '') return 'Chưa nhập'
  const value = Number(rate)
  if (!Number.isFinite(value)) return 'Chưa nhập'
  const percent = value * 100
  const digits = Number.isInteger(percent) ? 0 : 2
  return `${new Intl.NumberFormat('vi-VN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: 2,
  }).format(percent)}%/năm`
}

export type Tone = 'good' | 'warn' | 'bad'

/** Recommended DTI ceiling used for the gauge marker and the rating wording. */
export const DTI_BENCHMARK = 0.36

export interface DtiRating {
  percent: number
  /** 0–100, clamped, for the gauge width. */
  fill: number
  tone: Tone
  label: string
  hint: string
}

/**
 * Rate a server-sent DTI ratio. Bands follow the usual lending guidance (<20% comfortable,
 * <36% the recommended ceiling, <50% stretched, above that high risk).
 */
export function dtiRating(ratio: string | null | undefined): DtiRating | null {
  if (ratio == null || ratio === '') return null
  const value = Number(ratio)
  if (!Number.isFinite(value)) return null

  const percent = value * 100
  // The bar is scaled to 2× the benchmark so the threshold marker sits at the halfway point.
  const fill = Math.min(100, Math.max(0, (value / (DTI_BENCHMARK * 2)) * 100))
  const threshold = `${Math.round(DTI_BENCHMARK * 100)}%`

  if (value < 0.2) {
    return {
      percent,
      fill,
      tone: 'good',
      label: 'Rất thoải mái',
      hint: `Dưới ngưỡng khuyến nghị ${threshold}`,
    }
  }
  if (value < DTI_BENCHMARK) {
    return {
      percent,
      fill,
      tone: 'good',
      label: 'Khá an toàn',
      hint: `Dưới ngưỡng khuyến nghị ${threshold}`,
    }
  }
  if (value < 0.5) {
    return {
      percent,
      fill,
      tone: 'warn',
      label: 'Cần chú ý',
      hint: `Đã vượt ngưỡng khuyến nghị ${threshold}`,
    }
  }
  return {
    percent,
    fill,
    tone: 'bad',
    label: 'Rủi ro cao',
    hint: `Vượt xa ngưỡng khuyến nghị ${threshold}`,
  }
}

/** Why the payoff date can or cannot be projected — the "Tại sao?" the summary links to. */
export const PAYOFF_EXPLANATION =
  'Chúng tôi chỉ dự báo được ngày hết nợ khi mọi khoản nợ đang mở đều có lãi suất và khoản trả hằng tháng lớn hơn tiền lãi phát sinh. Nếu chỉ một khoản nợ chưa đủ điều kiện, toàn bộ danh mục sẽ không có ngày hết nợ.'