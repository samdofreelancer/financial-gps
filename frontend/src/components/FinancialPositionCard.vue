<template>
  <section class="card hero" data-testid="position-card" aria-labelledby="position-title">
    <p class="eyebrow">Vị trí của bạn</p>
    <h2 id="position-title">Vị trí tài chính của bạn</h2>

    <template v-if="view">
      <!-- The single number that answers "where am I?": the server's Net Cash Flow / month. -->
      <div v-if="hasMonthlyActivity" class="figure">
        <span class="amount" :class="cashClass">{{ cashText }}</span>
        <span class="per">/ tháng</span>
      </div>
      <p v-else class="empty-lead">Bắt đầu bằng cách thêm thu nhập và chi tiêu.</p>

      <dl class="stats">
        <div>
          <dt>Thu nhập</dt>
          <dd>
            <MoneyDisplay
              :amount="view.totalIncome.amount"
              :currency="view.totalIncome.currency"
              hide-currency
            />
          </dd>
        </div>
        <div>
          <dt>Chi tiêu</dt>
          <dd>
            <MoneyDisplay
              :amount="view.totalExpenses.amount"
              :currency="view.totalExpenses.currency"
              hide-currency
            />
          </dd>
        </div>
        <div>
          <dt>Trả nợ</dt>
          <dd data-testid="mandatory-payment">
            <MoneyDisplay
              :amount="view.totalMandatoryPayment.amount"
              :currency="view.totalMandatoryPayment.currency"
              hide-currency
            />
          </dd>
        </div>
        <div>
          <dt>Dòng tiền ròng</dt>
          <dd>
            <MoneyDisplay
              :amount="view.netCashFlow.amount"
              :currency="view.netCashFlow.currency"
              hide-currency
            />
          </dd>
        </div>
      </dl>

      <button
        v-if="!hasMonthlyActivity"
        type="button"
        class="btn first-income"
        @click="emit('add-income')"
      >
        Thêm thu nhập đầu tiên
      </button>

      <p class="note">Số liệu hàng tháng bằng {{ view.currency }} · đến {{ view.asOf }}</p>

      <details v-if="view.provenance.length" class="provenance">
        <summary>Cách tính từng tổng số</summary>
        <ul>
          <li v-for="entry in view.provenance" :key="entry.field">
            <strong>{{ friendlyField(entry.field) }}</strong>
            <em class="prov calculated">{{ entry.kind }}</em> — {{ entry.detail }}
          </li>
        </ul>
      </details>
    </template>

    <p v-else-if="loading" class="empty-lead">Đang tải vị trí của bạn…</p>
    <p v-else class="empty-lead">
      Không có vị trí lúc này. Tải lại trang để thử lại.
    </p>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MoneyDisplay from './MoneyDisplay.vue'
import { formatMoney, type ProfileView } from '../api/profile'

/**
 * Hero card for the Financial GPS page. Every number is rendered from the server response — no
 * client-side financial math. The card states the currency once and keeps the per-total provenance
 * collapsed, so the first thing the user reads is the position, not the plumbing.
 */
const props = defineProps<{ view: ProfileView | null; loading?: boolean }>()
const emit = defineEmits<{ (e: 'add-income'): void }>()

/** Field names come from the API; only the wording shown to the user is translated here. */
const FIELD_LABELS: Record<string, string> = {
  totalIncome: 'Thu nhập',
  totalExpenses: 'Chi tiêu',
  totalMandatoryPayment: 'Trả nợ',
  netCashFlow: 'Dòng tiền ròng',
  availableCapacity: 'Khả năng chi trả',
  savingsAmount: 'Tiết kiệm',
  emergencyFundAmount: 'Quỹ khẩn cấp',
  dependentsCount: 'Người phụ thuộc',
}

function friendlyField(field: string): string {
  return FIELD_LABELS[field] ?? field
}

/**
 * A position exists as soon as the owner has recorded any monthly movement — including a debt with
 * a mandatory payment but no income/expense lines yet, which must not be mistaken for an empty page.
 */
const hasMonthlyActivity = computed(
  () =>
    !!props.view &&
    (props.view.incomes.length > 0 ||
      props.view.expenses.length > 0 ||
      !/^-?0(\.0{1,2})?$/.test(props.view.totalMandatoryPayment.amount)),
)

const cashText = computed(() => {
  const view = props.view!
  // The currency is stated once in the note below: the figure stays a clean number.
  const text = formatMoney(view.netCashFlow.amount, '').trim()
  const negative = view.netCashFlow.amount.startsWith('-')
  const zero = /^-?0(\.0{1,2})?$/.test(view.netCashFlow.amount)
  if (zero || negative) return text
  return `+${text}`
})

const cashClass = computed(() => {
  const amount = props.view!.netCashFlow.amount
  if (/^-?0(\.0{1,2})?$/.test(amount)) return 'neutral'
  return amount.startsWith('-') ? 'negative' : 'positive'
})
</script>

<style scoped>
.hero { padding: 28px 24px; position: relative; overflow: hidden; }
.hero::before { content: ''; position: absolute; inset: 0 0 auto 0; height: 4px; background: var(--fg-gradient-brand); }
.hero h2 { margin: 0; font-size: 20px; letter-spacing: -0.01em; }

.figure { display: flex; align-items: baseline; gap: 8px; margin: 18px 0 0; }
.amount {
  font-size: 46px; font-weight: 800; letter-spacing: -0.03em;
  color: var(--fg-ink); font-variant-numeric: tabular-nums;
}
.amount.positive { color: var(--fg-success); }
.amount.negative { color: var(--fg-danger); }
.per { font-size: 15px; color: var(--fg-muted); }

.empty-lead { margin: 14px 0 0; font-size: 15px; color: var(--fg-text); }

.stats {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px;
  margin: 20px 0 0; padding-top: 16px; border-top: 1px solid var(--fg-border-soft);
}
.stats > div {
  background: var(--fg-app-bg); border-radius: var(--fg-radius-control);
  padding: 12px 14px; min-width: 0;
}
.stats dt { font-size: 13px; color: var(--fg-muted); }
.stats dd {
  margin: 4px 0 0; font-weight: 700; font-size: 17px;
  color: var(--fg-ink); font-variant-numeric: tabular-nums; overflow-wrap: anywhere;
}

.first-income { margin-top: 18px; width: auto; min-width: 220px; }
.note { margin: 16px 0 0; font-size: 13px; color: var(--fg-muted); }
.provenance { margin-top: 10px; font-size: 13px; color: var(--fg-muted); }
.provenance summary { cursor: pointer; }
.provenance ul { margin: 8px 0 0; padding-left: 18px; display: grid; gap: 4px; }
.prov {
  font-style: normal; font-size: 12px;
  border: 1px solid var(--fg-border-soft); border-radius: var(--fg-radius-control);
  padding: 1px 6px;
}
.prov.calculated { color: var(--fg-primary-hover); border-color: rgba(0, 143, 211, 0.35); }

@media (max-width: 640px) {
  .hero { padding: 22px 16px; }
  .amount { font-size: 32px; }
  /* Three compact rows read better than three tall boxes on a phone. */
  .stats { grid-template-columns: 1fr; gap: 0; padding-top: 12px; }
  .stats > div {
    display: flex; align-items: baseline; justify-content: space-between; gap: 12px;
    background: none; padding: 8px 0; border-bottom: 1px solid var(--fg-border-soft);
  }
  .stats > div:last-child { border-bottom: 0; }
  .stats dt { font-size: 14px; }
  .stats dd { font-size: 16px; margin: 0; text-align: right; }
  .first-income { width: 100%; min-width: 0; }
}
</style>
