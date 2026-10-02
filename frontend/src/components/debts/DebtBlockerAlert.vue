<template>
  <div v-if="blocked.length" class="blocker" role="alert">
    <strong>Không thể dự báo ngày hết nợ (BLOCKED).</strong>
    <ul>
      <li v-for="b in blocked" :key="b.creditor + b.reasonCode">
        {{ b.creditor }} — {{ b.reasonCode }}: {{ b.explanation }}
      </li>
    </ul>
    <p v-if="reasonCode">Mã lỗi danh mục: {{ reasonCode }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DebtSummary, DebtView } from '../../api/debts'

const props = defineProps<{ summary?: DebtSummary | null; debts?: DebtView[] }>()

const blocked = computed(() => {
  const fromPortfolio = props.summary?.portfolioProjection.blockedDebts ?? []
  const fromDebts = (props.debts ?? [])
    .filter((d) => d.projection.status === 'BLOCKED')
    .map((d) => ({
      creditor: d.creditor,
      reasonCode: d.projection.reasonCode ?? 'BLOCKED',
      explanation: d.projection.explanation ?? '',
    }))
  const merged = [...fromPortfolio]
  for (const b of fromDebts) {
    if (!merged.some((m) => m.creditor === b.creditor && m.reasonCode === b.reasonCode)) {
      merged.push(b)
    }
  }
  return merged
})

const reasonCode = computed(() => props.summary?.portfolioProjection.reasonCode ?? null)
</script>

<style scoped>
.blocker { padding: 12px 16px; border: 1px solid var(--fg-danger); border-radius: 12px; }
</style>
