<template>
  <div class="capacity" data-testid="goal-capacity">
    <div class="row">
      <span>Coverage</span>
      <strong data-testid="capacity-coverage" :class="coverageClass">{{ capacity.capacityCoverage }}</strong>
    </div>
    <div class="row"><span>Cần mỗi tháng</span><span data-testid="capacity-required">{{ capacity.requiredMonthlyCapacity ?? '—' }}</span></div>
    <div class="row"><span>Khả dụng</span><span>{{ capacity.availableCapacity }}</span></div>
    <div v-if="capacity.monthlyShortfall != null" class="row"><span>Thiếu hụt</span><span>{{ capacity.monthlyShortfall }}</span></div>
    <div v-if="capacity.dateFeasibility === 'EXPIRED_TARGET_DATE'" class="warn" data-testid="capacity-expired">
      Target date expired as of {{ capacity.asOf }} — the full remainder is due now.
    </div>
    <p class="explain">{{ capacity.explanation }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { GoalCapacityView } from '../../api/goals'

const props = defineProps<{ capacity: GoalCapacityView }>()

const coverageClass = computed(() =>
  props.capacity.capacityCoverage === 'SHORTFALL' ? 'coverage--short' : 'coverage--ok',
)
</script>

<style scoped>
.capacity { display: grid; gap: 8px; padding: 18px; background: linear-gradient(180deg, var(--fg-surface) 0%, var(--fg-info-bg) 100%); border: 1px solid var(--fg-info-border); border-radius: var(--fg-radius-card); font-size: 14px; box-shadow: var(--fg-shadow-card); }
.row { display: flex; justify-content: space-between; gap: 8px; }
.row strong { font-variant-numeric: tabular-nums; }
.row { display: flex; justify-content: space-between; gap: 8px; }
.coverage--ok { color: #166534; }
.coverage--short { color: var(--fg-danger); }
.warn { color: var(--fg-danger); font-weight: 600; }
.explain { margin: 4px 0 0; color: var(--fg-muted); font-size: 12px; }
</style>
