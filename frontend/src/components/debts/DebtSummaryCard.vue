<template>
  <section class="summary-card" aria-label="Tổng quan nợ">
    <div class="row">
      <span>Tổng dư nợ</span>
      <strong data-testid="total-debt">{{ formatMoney(summary.totalOutstandingDebt, summary.currency) }}</strong>
    </div>
    <div class="row">
      <span>Tối thiểu / tháng</span>
      <strong data-testid="total-minimum">{{ formatMoney(summary.totalMinimumMonthlyPayment, summary.currency) }}</strong>
    </div>
    <div class="row">
      <span>Dự định trả / tháng</span>
      <strong>{{ formatMoney(summary.totalPlannedMonthlyPayment, summary.currency) }}</strong>
    </div>
    <div class="row">
      <span>DTI</span>
      <strong data-testid="dti">{{ dtiText }}</strong>
    </div>
    <div class="row">
      <span>Dự kiến hết nợ</span>
      <strong data-testid="payoff-date">{{ payoffText }}</strong>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatMoney } from '../../api/profile'
import type { DebtSummary } from '../../api/debts'

const props = defineProps<{ summary: DebtSummary }>()

const dtiText = computed(() =>
  props.summary.debtToIncome.status === 'AVAILABLE' && props.summary.debtToIncome.ratio
    ? `${(Number(props.summary.debtToIncome.ratio) * 100).toFixed(2)}%`
    : 'Chưa tính được (thiếu thu nhập)',
)

const payoffText = computed(
  () => props.summary.portfolioProjection.projectedDebtFreeDate ?? 'Chưa dự báo được',
)
</script>

<style scoped>
.summary-card { display: grid; gap: 8px; padding: 16px; border: 1px solid var(--fg-info-border); border-radius: 12px; }
.row { display: flex; justify-content: space-between; }
</style>
