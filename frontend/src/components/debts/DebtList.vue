<template>
  <ul class="debt-list">
    <li v-for="debt in debts" :key="debt.id" class="debt-item">
      <div class="main">
        <strong>{{ debt.creditor }}</strong>
        <span class="badge" :data-testid="`status-${debt.id}`">{{ debt.status }}</span>
        <span>{{ formatMoney(debt.outstandingBalance, debt.currency) }}</span>
      </div>
      <div class="sub">
        <span>Tối thiểu {{ formatMoney(debt.minimumPayment, debt.currency) }}</span>
        <span>Dự định {{ formatMoney(debt.plannedPayment, debt.currency) }}</span>
        <span v-if="debt.projection.status === 'AVAILABLE'">
          Hết nợ {{ debt.projection.projectedPayoffDate }} ({{ debt.projection.numberOfPayments }} kỳ)
        </span>
        <span v-else-if="debt.projection.status === 'BLOCKED'">
          BLOCKED: {{ debt.projection.reasonCode }}
        </span>
        <span v-else>Đã trả hết</span>
      </div>
      <div class="actions">
        <button type="button" @click="$emit('edit', debt)">Sửa</button>
        <button type="button" @click="$emit('remove', debt)">Xóa</button>
      </div>
    </li>
  </ul>
</template>

<script setup lang="ts">
import { formatMoney } from '../../api/profile'
import type { DebtView } from '../../api/debts'

defineProps<{ debts: DebtView[] }>()
defineEmits<{ (e: 'edit', debt: DebtView): void; (e: 'remove', debt: DebtView): void }>()
</script>

<style scoped>
.debt-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 12px; }
.debt-item { padding: 12px 16px; border: 1px solid var(--fg-info-border); border-radius: 12px; }
.main { display: flex; gap: 12px; align-items: center; }
.sub { display: flex; gap: 12px; font-size: 13px; color: var(--fg-muted); }
.badge { font-size: 12px; padding: 2px 8px; border-radius: 999px; background: var(--fg-info-border); }
.actions { display: flex; gap: 8px; margin-top: 8px; }
</style>
