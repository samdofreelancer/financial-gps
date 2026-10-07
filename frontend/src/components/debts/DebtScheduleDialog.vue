<template>
  <div class="sched-backdrop" @click.self="onClose">
    <div class="sched" role="dialog" aria-modal="true" :aria-labelledby="titleId">
      <header class="sched__head">
        <div>
          <h2 :id="titleId" class="sched__title">Lịch trả nợ — {{ debt.creditor }}</h2>
          <p v-if="debt.dueDay" class="sched__hint">Kỳ trả vào ngày {{ debt.dueDay }} hằng tháng</p>
        </div>
        <button type="button" class="sched__close" aria-label="Đóng" title="Đóng lịch trả nợ" @click="onClose">
          <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
            <path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
          </svg>
        </button>
      </header>

      <div v-if="loading" class="sched__state" data-testid="schedule-loading">Đang tải lịch trả nợ…</div>
      <div v-else-if="error" class="error-box" role="alert">{{ error }}</div>
      <div v-else-if="schedule && schedule.status === 'BLOCKED'" class="sched__state" data-testid="schedule-blocked">
        <p>{{ blockerMessage(schedule.reasonCode) }}</p>
        <p class="sched__hint">Cập nhật lãi suất hoặc tăng khoản trả mỗi tháng để có lịch trả nợ.</p>
      </div>
      <div v-else-if="schedule && schedule.status === 'COMPLETED'" class="sched__state" data-testid="schedule-completed">
        Khoản nợ này đã trả hết — không còn kỳ nào trong lịch.
      </div>
      <template v-else-if="schedule">
        <p class="sched__meta" data-testid="schedule-meta">
          Hết nợ {{ date(schedule.payoffDate) }} ({{ schedule.numberOfPayments }} kỳ) · Tổng lãi
          {{ money(schedule.totalInterest ?? '0') }}
        </p>
        <div class="sched__table-wrap">
          <table class="sched__table" data-testid="schedule-table">
            <thead>
              <tr>
                <th scope="col">Kỳ</th>
                <th scope="col">Ngày trả</th>
                <th scope="col">Khoản trả</th>
                <th scope="col">Gốc</th>
                <th scope="col">Lãi</th>
                <th scope="col">Dư nợ cuối kỳ</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in schedule.rows" :key="row.period" data-testid="schedule-row">
                <td>{{ row.period }}</td>
                <td>{{ date(row.dueDate) }}</td>
                <td>{{ money(row.payment) }}</td>
                <td>{{ money(row.principal) }}</td>
                <td>{{ money(row.interest) }}</td>
                <td>{{ money(row.endingBalance) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <div class="sched__actions">
        <button type="button" class="btn" @click="onClose">Đóng</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import type { DebtSchedule, DebtView } from '../../api/debts'
import { formatMoney } from '../../api/profile'
import { blockerMessage } from '../debtText'

/**
 * Read-only payment calendar for one debt: date, principal, interest and ending balance per
 * period, straight from the server response. Blocked and paid-off debts explain themselves
 * instead of showing an empty table; Escape and the backdrop close it, mirroring the other dialogs.
 */
const props = withDefaults(
  defineProps<{
    debt: DebtView
    schedule: DebtSchedule | null
    loading?: boolean
    error?: string
  }>(),
  { loading: false, error: '' },
)

const emit = defineEmits<{ (e: 'close'): void }>()

const titleId = 'debt-schedule-title'

function money(value: string): string {
  // compact drops the meaningless ",00" tail on whole VND amounts, matching every figure on the page.
  return formatMoney(value, props.schedule?.currency ?? props.debt.currency, { compact: true })
}

/** Server dates are ISO-8601; the calendar reads naturally as dd/MM/yyyy. */
function date(value: string | null): string {
  if (!value) return '—'
  const [year, month, day] = value.split('-')
  return year && month && day ? `${day}/${month}/${year}` : value
}

function onClose(): void {
  emit('close')
}

function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') onClose()
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* Same popup language as ConfirmDialog/DebtForm: fixed veil, centred card, scroll on overflow. */
.sched-backdrop {
  position: fixed; inset: 0; z-index: 60;
  display: flex; justify-content: center;
  padding: 20px; overflow-y: auto;
  background: rgba(17, 24, 39, 0.45);
}
.sched {
  width: min(860px, 100%);
  height: fit-content; margin: auto;
  padding: 20px 22px;
  background: var(--fg-surface);
  border-radius: var(--fg-radius-card);
  box-shadow: var(--fg-shadow-float);
}
.sched__head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.sched__title { margin: 0; font-size: 17px; }
.sched__hint { margin: 4px 0 0; font-size: 13px; color: var(--fg-muted); }
.sched__meta { margin: 14px 0 10px; font-size: 13px; color: var(--fg-text); }

.sched__table-wrap { overflow-x: auto; }
.sched__table {
  width: 100%; border-collapse: collapse;
  font-size: 13px; font-variant-numeric: tabular-nums;
}
.sched__table th {
  padding: 8px 10px; text-align: right;
  font-size: 12px; font-weight: 600; color: var(--fg-muted);
  border-bottom: 1px solid var(--fg-border-soft); white-space: nowrap;
}
.sched__table td {
  padding: 8px 10px; text-align: right;
  color: var(--fg-ink);
  border-bottom: 1px solid var(--fg-border-soft); white-space: nowrap;
}
.sched__table th:first-child,
.sched__table td:first-child { text-align: left; }
.sched__table tbody tr:last-child td { border-bottom: 0; }

.sched__state { padding: 26px 0 8px; font-size: 14px; color: var(--fg-text); text-align: center; }
.sched__state p { margin: 0 0 6px; }
.sched__actions { display: flex; justify-content: flex-end; margin-top: 16px; }
.sched__actions .btn { width: auto; min-width: 104px; }
.error-box { color: var(--fg-danger); }

.sched__close {
  display: inline-flex; align-items: center; justify-content: center;
  width: 30px; height: 30px; padding: 0;
  color: var(--fg-muted); background: var(--fg-surface);
  border: 1px solid var(--fg-border-soft); border-radius: var(--fg-radius-control);
  cursor: pointer; transition: color 0.15s, border-color 0.15s;
}
.sched__close:hover { color: var(--fg-primary); border-color: var(--fg-primary); }
.sched__close:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); }
.sched__close svg { width: 14px; height: 14px; }

@media (max-width: 560px) {
  .sched-backdrop { padding: 12px; }
  .sched { padding: 16px 14px; }
}
</style>

