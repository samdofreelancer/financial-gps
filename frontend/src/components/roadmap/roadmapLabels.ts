import type { StageKind, StageStatus } from '@/api/roadmap'

/**
 * Vietnamese display vocabulary for the roadmap (005).
 *
 * Display only: the wire values are never rewritten. `status` is the stage's position on the
 * roadmap — the reached badge (`completionReached`) is labelled separately so the two facts are
 * never merged on screen.
 */
const STAGE_STATUS_LABELS: Record<string, string> = {
  COMPLETED: 'Hoàn thành',
  CURRENT: 'Giai đoạn hiện tại',
  UPCOMING: 'Sắp tới',
}

const STAGE_KIND_LABELS: Record<string, string> = {
  DEBT_ELIMINATION: 'Xóa nợ',
  GOAL: 'Mục tiêu',
}

const ETA_BASIS_LABELS: Record<string, string> = {
  PORTFOLIO_PROJECTION: 'Dự báo theo kế hoạch trả nợ của danh mục (Feature 002)',
  SINGLE_DESTINATION_HYPOTHESIS:
    'Giả thuyết chỉ xét riêng giai đoạn này, dồn toàn bộ năng lực khả dụng (Feature 004)',
  ROUTE_PROJECTION: 'Dự báo theo phân bổ tiền của lộ trình (Feature 008)',
}

const ETA_ASSUMPTION_LABELS: Record<string, string> = {
  PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA:
    'Toàn bộ khoản trả theo kế hoạch, gồm cả phần trả thêm đã đăng ký, được thực hiện đầy đủ.',
  WHOLE_AVAILABLE_CAPACITY:
    'Toàn bộ năng lực khả dụng được dồn cho giai đoạn này; bỏ qua việc chia tiền giữa các giai đoạn.',
  ROUTE_ALLOCATION: 'Ngày dự kiến theo cách lộ trình phân bổ tiền cho các giai đoạn.',
}

const ETA_REASON_LABELS: Record<string, string> = {
  MISSING_FINANCIAL_PROFILE:
    'Chưa có hồ sơ tài chính nên chưa tính được ngày hoàn thành.',
  NO_AVAILABLE_CAPACITY:
    'Năng lực khả dụng bằng 0 nên chưa tính được ngày hoàn thành.',
  PAYOFF_HORIZON_EXCEEDS_MAXIMUM:
    'Thời gian trả nợ vượt quá giới hạn dự báo nên chưa có ngày hoàn thành.',
  PORTFOLIO_CONTAINS_BLOCKED_DEBTS:
    'Danh mục nợ có khoản chưa dự báo được nên chưa có ngày hoàn thành.',
}

export function stageStatusLabel(status: StageStatus | string): string {
  return STAGE_STATUS_LABELS[status] ?? String(status)
}

export function stageKindLabel(kind: StageKind | string): string {
  return STAGE_KIND_LABELS[kind] ?? String(kind)
}

export function etaBasisLabel(basis: string): string {
  return ETA_BASIS_LABELS[basis] ?? basis
}

export function etaAssumptionLabel(assumption: string): string {
  return ETA_ASSUMPTION_LABELS[assumption] ?? assumption
}

export function etaReasonLabel(reason: string): string {
  return ETA_REASON_LABELS[reason] ?? `Mã lý do: ${reason}`
}
