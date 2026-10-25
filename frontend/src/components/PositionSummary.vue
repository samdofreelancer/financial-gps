<template>
  <section class="card position" data-testid="position-summary">
    <h2>Vị trí hiện tại <small>(server tính)</small></h2>
    <p v-if="!view" class="hint">Chưa có vị trí — hãy nhập thông tin để xem tổng số.</p>
    <template v-else>
      <dl class="totals">
        <div>
          <dt>Thu nhập hàng tháng</dt>
          <dd>
            <MoneyDisplay
              :amount="view.totalIncome.amount"
              :currency="view.totalIncome.currency"
              :provenance="view.totalIncome.provenance"
            />
          </dd>
        </div>
        <div>
          <dt>Chi tiêu hàng tháng</dt>
          <dd>
            <MoneyDisplay
              :amount="view.totalExpenses.amount"
              :currency="view.totalExpenses.currency"
              :provenance="view.totalExpenses.provenance"
            />
          </dd>
        </div>
        <div>
          <dt>Trả nợ bắt buộc</dt>
          <dd data-testid="mandatory-payment">
            <MoneyDisplay
              :amount="view.totalMandatoryPayment.amount"
              :currency="view.totalMandatoryPayment.currency"
              :provenance="view.totalMandatoryPayment.provenance"
            />
          </dd>
        </div>
        <div>
          <dt>Dòng tiền ròng</dt>
          <dd data-testid="net-cash-flow">
            <MoneyDisplay
              :amount="view.netCashFlow.amount"
              :currency="view.netCashFlow.currency"
              :provenance="view.netCashFlow.provenance"
            />
          </dd>
        </div>
        <div>
          <dt>Khả năng chi trả</dt>
          <dd>
            <MoneyDisplay
              :amount="view.availableCapacity.amount"
              :currency="view.availableCapacity.currency"
              :provenance="view.availableCapacity.provenance"
            />
          </dd>
        </div>
      </dl>

      <h3 class="facts-title">Thông tin của bạn</h3>
      <dl class="totals facts">
        <div>
          <dt>Tiết kiệm</dt>
          <dd>
            <MoneyDisplay :amount="view.savingsAmount" :currency="view.currency" provenance="actual" />
          </dd>
        </div>
        <div>
          <dt>Quỹ khẩn cấp</dt>
          <dd>
            <MoneyDisplay
              :amount="view.emergencyFundAmount"
              :currency="view.currency"
              provenance="actual"
            />
          </dd>
        </div>
        <div>
          <dt>Người phụ thuộc</dt>
          <dd class="count">{{ view.dependentsCount }} <em class="prov actual">actual</em></dd>
        </div>
      </dl>

      <p class="hint as-of">Tính đến {{ view.asOf }} (hàng tháng).</p>

      <details v-if="view.provenance.length" class="provenance">
        <summary>Cách tính từng tổng số</summary>
        <ul>
          <li v-for="entry in view.provenance" :key="entry.field">
            <strong>{{ entry.field }}</strong> <em class="prov calculated">{{ entry.kind }}</em>
            — {{ entry.detail }}
          </li>
        </ul>
      </details>
    </template>
  </section>
</template>

<script setup lang="ts">
import MoneyDisplay from './MoneyDisplay.vue'
import type { ProfileView } from '../api/profile'

/** The server result is authoritative: no total is computed in this component. */
defineProps<{ view: ProfileView | null }>()
</script>

<style scoped>
/* The card frame comes from the global .card token; only the inner rhythm lives here. */
.card { padding: 20px; }
h2 { margin: 0 0 12px; font-size: 16px; }
h2 small { font-size: 12px; font-weight: 500; color: var(--fg-muted); }
.totals { display: grid; gap: 10px; margin: 0; }
.totals > div { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; }
dt { color: var(--fg-muted); }
dd { margin: 0; font-weight: 600; color: var(--fg-ink); }
.facts-title {
  margin: 18px 0 8px;
  font-size: 13px; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase;
  color: var(--fg-muted);
}
.count { font-variant-numeric: tabular-nums; }
.as-of { margin-top: 12px; }
.provenance { margin-top: 12px; font-size: 13px; color: var(--fg-muted); }
.provenance ul { margin: 8px 0 0; padding-left: 18px; display: grid; gap: 4px; }
.prov {
  font-style: normal;
  font-size: 12px;
  border: 1px solid var(--fg-border-soft);
  border-radius: var(--fg-radius-control);
  padding: 1px 6px;
}
.prov.calculated { color: var(--fg-primary-hover); border-color: rgba(0, 143, 211, 0.35); }
</style>
