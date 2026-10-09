<template>
  <ul class="debt-list">
    <li v-for="debt in debts" :key="debt.id" class="card debt-item" data-testid="debt-item">
      <div class="debt-item__main">
        <div class="debt-item__identity">
          <strong class="debt-item__name">{{ debt.creditor }}</strong>
          <span class="debt-item__type">{{ debtTypeLabel(debt.debtType) }}</span>
        </div>
        <div class="debt-item__figures">
          <span class="debt-item__balance">{{ money(debt.outstandingBalance, debt.currency) }}</span>
          <span class="debt-item__chips">
            <span class="chip" :class="`chip--${debt.status.toLowerCase()}`" :data-testid="`status-${debt.id}`">
              {{ debtStatusLabel(debt.status) }}
            </span>
            <span
              v-if="debt.projection.status === 'BLOCKED'"
              class="chip chip--danger"
              :title="debt.projection.reasonCode ?? undefined"
            >
              {{ blockerBadge(debt.projection.reasonCode) }}
            </span>
          </span>
        </div>
      </div>

      <dl class="debt-item__stats">
        <div>
          <dt>Tối thiểu</dt>
          <dd>{{ money(debt.minimumPayment, debt.currency) }}</dd>
        </div>
        <div>
          <dt>Dự định trả</dt>
          <dd>{{ money(debt.plannedPayment, debt.currency) }}</dd>
        </div>
        <div>
          <dt>Lãi suất</dt>
          <dd>{{ rateLabel(debt.annualInterestRate) }}</dd>
        </div>
        <div v-if="debt.dueDay">
          <dt>Đến hạn</dt>
          <dd>
            Ngày {{ debt.dueDay }} hằng tháng
            <span
              v-if="dueHint(debt.dueDay)"
              class="due-hint"
              :class="{ 'due-hint--soon': dueHint(debt.dueDay)?.urgent }"
              data-testid="debt-due-hint"
            >{{ dueHint(debt.dueDay)?.label }}</span>
          </dd>
        </div>
      </dl>

      <p
        v-if="debt.status === 'ACTIVE' && debt.projection.monthlyInterest !== null"
        class="debt-item__payment-split"
        data-testid="debt-payment-split"
      >
        <template v-if="principalIsNonNegative(debt.plannedPayment, debt.projection.monthlyInterest)">
          Kỳ đầu: gốc {{ money(principalPayment(debt.plannedPayment, debt.projection.monthlyInterest), debt.currency) }}
        </template>
        <template v-else>
          Kỳ đầu: dư nợ tăng {{ money(absolute(principalPayment(debt.plannedPayment, debt.projection.monthlyInterest)), debt.currency) }}
        </template>
        · lãi {{ money(debt.projection.monthlyInterest, debt.currency) }}
      </p>

      <p class="debt-item__projection">
        <template v-if="debt.projection.status === 'AVAILABLE'">
          Còn {{ debt.projection.numberOfPayments }} tháng · dự kiến hết nợ {{ debt.projection.projectedPayoffDate }}
        </template>
        <template v-else-if="debt.projection.status === 'BLOCKED'">
          {{ blockerMessage(debt.projection.reasonCode) }}
        </template>
        <template v-else>Đã trả hết</template>
      </p>

      <!-- Icon buttons with their own accessible name: no unstyled browser defaults. -->
      <div class="debt-item__actions">
        <button
          v-if="debt.status === 'ACTIVE'"
          type="button"
          class="payment-action"
          :class="{ 'payment-action--done': debt.paidThisPeriod }"
          :aria-pressed="Boolean(debt.paidThisPeriod)"
          :title="debt.paidThisPeriod ? 'Bấm để hoàn tác ghi nhận' : 'Chỉ đánh dấu thủ công; không xử lý thanh toán hoặc thay đổi dư nợ.'"
          :data-testid="`payment-mark-${debt.id}`"
          @click="debt.paidThisPeriod ? emit('undo-paid', debt) : emit('mark-paid', debt)"
        >
          <span aria-hidden="true">{{ debt.paidThisPeriod ? '✓' : '＋' }}</span>
          {{ debt.paidThisPeriod ? 'Đã trả kỳ này' : 'Đánh dấu đã trả' }}
        </button>
        <button
          type="button"
          class="icon-btn"
          aria-label="Lịch trả nợ"
          title="Xem lịch trả nợ từng tháng"
          data-testid="debt-schedule"
          @click="emit('schedule', debt)"
        >
          <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
            <rect x="2.5" y="3.5" width="11" height="10" rx="1.5" stroke="currentColor" stroke-width="1.3" />
            <path d="M2.5 6.5h11M5.5 2.5v2M10.5 2.5v2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
          </svg>
        </button>
        <button type="button" class="icon-btn" aria-label="Sửa" title="Sửa khoản nợ này" @click="emit('edit', debt)">
          <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
            <path d="M11.2 2.6l2.2 2.2M3 13h2.2l7.4-7.4-2.2-2.2L3 10.8V13Z" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
        <button type="button" class="icon-btn icon-btn--danger" aria-label="Xóa" title="Xóa khoản nợ này" @click="emit('remove', debt)">
          <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
            <path d="M3 4.5h10M6.5 4.5V3h3v1.5M4.5 4.5l.6 8.2a.9.9 0 0 0 .9.8h4a.9.9 0 0 0 .9-.8l.6-8.2" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
      </div>
    </li>
  </ul>

  <div v-if="!debts.length" class="card empty" data-testid="debts-empty">
    <p class="empty__title">Chưa có khoản nợ nào.</p>
    <p class="empty__hint">
      Thêm khoản nợ đầu tiên để xem tổng dư, khoản trả hằng tháng và ngày dự kiến hết nợ.
    </p>
  </div>
</template>

<script setup lang="ts">
import { formatMoney } from '../../api/profile'
import { blockerBadge, blockerMessage, debtStatusLabel, debtTypeLabel, rateLabel } from '../debtText'
import type { DebtView } from '../../api/debts'

/**
 * The debt rows. Each one carries the facts a reader needs to judge it at a glance — balance,
 * both payments, the rate and the due day — instead of forcing a click to find out what it is.
 *
 * Status is written in the reader's language ("Trả không đủ lãi"); the machine code survives as
 * the badge tooltip and in the blocker panel's technical block.
 */
const props = withDefaults(defineProps<{ debts: DebtView[]; hidden?: boolean; asOf?: string }>(), { hidden: false })
const emit = defineEmits<{
  (e: 'edit', debt: DebtView): void
  (e: 'remove', debt: DebtView): void
  (e: 'schedule', debt: DebtView): void
  (e: 'mark-paid', debt: DebtView): void
  (e: 'undo-paid', debt: DebtView): void
}>()

function money(value: string, currency: string): string {
  if (props.hidden) return '••••••'
  return formatMoney(value, currency, { compact: true })
}

/** The fixed monthly payment first covers interest; only the remainder reduces principal. */
function cents(amount: string): bigint {
  const [whole = '0', fraction = ''] = amount.split('.')
  return BigInt(whole) * 100n + BigInt((fraction + '00').slice(0, 2))
}

function principalIsNonNegative(payment: string, interest: string): boolean {
  return cents(payment) >= cents(interest)
}

function principalPayment(payment: string, interest: string): string {
  const result = cents(payment) - cents(interest)
  const magnitude = result < 0n ? -result : result
  const sign = result < 0n ? '-' : ''
  return `${sign}${magnitude / 100n}.${(magnitude % 100n).toString().padStart(2, '0')}`
}

function absolute(amount: string): string {
  return amount.startsWith('-') ? amount.slice(1) : amount
}

/** Upcoming due dates are reminders only; without payment history we never label a debt overdue. */
function dueHint(dueDay: number): { label: string; urgent: boolean } | null {
  if (!props.asOf || !/^\d{4}-\d{2}-\d{2}$/.test(props.asOf)) return null
  const [year, month, day] = props.asOf.split('-').map(Number)
  const today = Date.UTC(year, month - 1, day)
  let due = new Date(Date.UTC(year, month - 1, Math.min(dueDay, new Date(Date.UTC(year, month, 0)).getUTCDate())))
  if (due.getTime() < today) {
    const nextMonth = new Date(Date.UTC(year, month, 1))
    due = new Date(Date.UTC(
      nextMonth.getUTCFullYear(),
      nextMonth.getUTCMonth(),
      Math.min(dueDay, new Date(Date.UTC(nextMonth.getUTCFullYear(), nextMonth.getUTCMonth() + 1, 0)).getUTCDate()),
    ))
  }
  const days = Math.round((due.getTime() - today) / 86_400_000)
  if (days === 0) return { label: 'Đến hạn hôm nay', urgent: true }
  if (days <= 5) return { label: `Còn ${days} ngày đến hạn`, urgent: true }
  return null
}
</script>

<style scoped>
.debt-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 12px; }
.debt-item { position: relative; padding: 18px; border-radius: var(--fg-radius-card); }
.debt-item:hover { box-shadow: var(--fg-shadow-float); }

.debt-item__main { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.debt-item__identity { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.debt-item__name { font-size: 15px; color: var(--fg-ink); overflow-wrap: anywhere; }
.debt-item__type { font-size: 12px; color: var(--fg-muted); }
.debt-item__figures {
  display: flex; flex-direction: column; align-items: flex-end; gap: 6px; flex: 0 0 auto;
  min-width: 200px;
}
.debt-item__balance {
  font-size: 17px; font-weight: 700; color: var(--fg-ink);
  font-variant-numeric: tabular-nums; white-space: nowrap;
}
.debt-item__chips { display: flex; gap: 6px; flex-wrap: wrap; justify-content: flex-end; }

.chip {
  padding: 2px 8px; border-radius: 999px;
  font-size: 11px; font-weight: 600;
  color: var(--fg-label); background: var(--fg-app-bg);
  border: 1px solid var(--fg-border-soft);
}
.chip--paid_off { color: var(--fg-success); border-color: rgba(22, 163, 74, 0.3); }
.chip--danger { color: var(--fg-danger); background: var(--fg-danger-bg); border-color: var(--fg-danger-border); }

.debt-item__stats {
  margin: 14px 0 0; padding: 12px 14px;
  display: grid; grid-template-columns: repeat( auto-fit, minmax(112px, 1fr)); gap: 8px;
  background: transparent; padding: 0; border-radius: 0;
}
.debt-item__stats > div { min-width: 0; padding: 10px 12px; background: var(--fg-app-bg); border-radius: var(--fg-radius-control); }
.debt-item__stats dt { font-size: 12px; color: var(--fg-muted); }
.debt-item__stats dd {
  margin: 2px 0 0; font-size: 13px; font-weight: 600; color: var(--fg-ink);
  font-variant-numeric: tabular-nums; overflow-wrap: anywhere;
}
.due-hint { display: block; margin-top: 4px; color: var(--fg-muted); font-size: 11px; font-weight: 500; }
.due-hint--soon { color: #b45309; font-weight: 700; }
.debt-item__projection { margin: 12px 0 0; font-size: 13px; color: var(--fg-text); }
.debt-item__payment-split { margin: 10px 0 0; font-size: 12px; color: var(--fg-muted); }

/* Row actions flow under the header so they never cover the balance. */
.debt-item__actions { display: flex; align-items: center; gap: 8px; margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--fg-border-soft); flex-wrap: wrap; }
.payment-action {
  display: inline-flex; align-items: center; gap: 6px; min-height: 34px; padding: 0 11px;
  color: var(--fg-on-primary, #fff); background: var(--fg-primary); border: 1px solid var(--fg-primary);
  border-radius: var(--fg-radius-control); font: inherit; font-size: 12px; font-weight: 700;
  white-space: nowrap; cursor: pointer;
}
.payment-action:hover { filter: brightness(0.94); }
.payment-action:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); }
.payment-action--done { color: var(--fg-success); background: rgba(22, 163, 74, 0.1); border-color: rgba(22, 163, 74, 0.3); }
.icon-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 30px; height: 30px; padding: 0;
  color: var(--fg-muted); background: var(--fg-surface);
  border: 1px solid var(--fg-border-soft); border-radius: var(--fg-radius-control);
  cursor: pointer; transition: color 0.15s, border-color 0.15s, background 0.15s;
}
.icon-btn:hover { color: var(--fg-primary); border-color: var(--fg-primary); background: var(--fg-info-bg); }
.icon-btn:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); }
.icon-btn--danger:hover { color: var(--fg-danger); border-color: var(--fg-danger); background: var(--fg-danger-bg); }
.icon-btn svg { width: 16px; height: 16px; }

.empty { margin-top: 12px; padding: 24px; text-align: center; }
.empty__title { margin: 0; font-size: 15px; font-weight: 600; color: var(--fg-ink); }
.empty__hint { margin: 6px 0 0; font-size: 13px; color: var(--fg-muted); }

@media (max-width: 640px) {
  .debt-item { padding: 16px 14px 60px; }
  .debt-item__main { flex-direction: column; align-items: flex-start; }
  .debt-item__figures { align-items: flex-start; padding-right: 0; }
  .debt-item__chips { justify-content: flex-start; }
  .debt-item__stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  /* Actions drop to their own row on narrow screens so they never cover the balance. */
  .debt-item__actions { position: static; margin-top: 12px; flex-wrap: wrap; }
  .payment-action { min-height: 40px; }
  .icon-btn { width: 40px; height: 40px; }
}
</style>
