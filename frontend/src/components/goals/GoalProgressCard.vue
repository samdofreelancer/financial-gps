<template>
  <div class="progress-card" data-testid="goal-progress">
    <div class="row"><span>Còn lại (calculated)</span><strong data-testid="goal-remaining">{{ goal.remaining }} {{ goal.currency }}</strong></div>
    <div class="row"><span>Tiến độ (calculated)</span><strong data-testid="goal-progress-value">{{ percent }}%</strong></div>
    <div class="bar" role="progressbar" :aria-valuenow="percent" aria-valuemin="0" aria-valuemax="100">
      <div class="bar__fill" :style="{ width: percent + '%' }"></div>
    </div>
    <div class="row row--small">
      <span>Mục tiêu (actual): {{ goal.targetAmount }}</span>
      <span>Hiện tại (actual): {{ goal.currentAmount }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { GoalView } from '../../api/goals'

const props = defineProps<{ goal: GoalView }>()

/** Server ratio scale 4 → floored 2-decimal display so progress never overstates completion. */
const percent = computed(() => {
  const ratio = Number(props.goal.progress)
  if (!Number.isFinite(ratio)) return 0
  return Math.floor(ratio * 10000) / 100
})
</script>

<style scoped>
.progress-card { display: grid; gap: 8px; padding: 12px 14px; background: var(--fg-surface); border: 1px solid var(--fg-info-border); border-radius: var(--fg-radius-card); }
.row { display: flex; justify-content: space-between; gap: 8px; font-size: 13px; }
.row--small { font-size: 12px; color: var(--fg-muted); }
.bar { height: 8px; border-radius: 999px; background: var(--fg-info-bg); overflow: hidden; }
.bar__fill { height: 100%; background: var(--fg-primary); }
</style>
