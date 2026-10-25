<script setup lang="ts">
import { computed } from 'vue'
import type { StatusView, ConditionView } from '@/api/gps'

interface Props {
  status: StatusView['status']
  explanation: string
  condition: ConditionView
}

const props = defineProps<Props>()

function getStatusConfig(status: string) {
  const configs: Record<string, { label: string; color: string; icon: string }> = {
    COMPLETED: { label: 'Completed', color: 'success', icon: '✓' },
    ON_TRACK: { label: 'On Track', color: 'success', icon: '→' },
    AT_RISK: { label: 'At Risk', color: 'warning', icon: '⚠' },
    OFF_TRACK: { label: 'Off Track', color: 'danger', icon: '✕' },
    BLOCKED: { label: 'Blocked', color: 'danger', icon: '🚫' }
  }
  return configs[status] || { label: status, color: 'secondary', icon: '?' }
}

const config = computed(() => getStatusConfig(props.status))
</script>

<template>
  <div class="status-badge" :class="config.color">
    <div class="status-header">
      <span class="status-icon">{{ config.icon }}</span>
      <span class="status-label">{{ config.label }}</span>
    </div>
    <p class="status-explanation">{{ explanation }}</p>
    
    <details class="condition-details">
      <summary>Show details</summary>
      <div class="condition-grid">
        <div v-if="condition.type === 'COMPLETED'" class="condition-item">
          <span class="label">Completion Reason</span>
          <span class="value">{{ condition.reason }}</span>
        </div>
        <div v-else-if="condition.type === 'BLOCKED'" class="condition-item">
          <span class="label">Blocker</span>
          <span class="value">{{ condition.reason }}</span>
        </div>
        <div v-else class="condition-item">
          <span class="label">Condition</span>
          <span class="value">{{ condition.type }}</span>
        </div>
        
        <div v-if="condition.requiredMonthly" class="condition-item">
          <span class="label">Required / Month</span>
          <span class="value">{{ condition.requiredMonthly }}</span>
        </div>
        <div v-if="condition.projectedMonthly" class="condition-item">
          <span class="label">Available Capacity</span>
          <span class="value">{{ condition.projectedMonthly }}</span>
        </div>
        <div v-if="condition.monthsRemaining !== null" class="condition-item">
          <span class="label">Months to Target</span>
          <span class="value">{{ condition.monthsRemaining }}</span>
        </div>
        <div v-if="condition.etaPeriods !== null" class="condition-item">
          <span class="label">ETA (months)</span>
          <span class="value">{{ condition.etaPeriods }}</span>
        </div>
        <div v-if="condition.lateness !== null" class="condition-item">
          <span class="label">Lateness</span>
          <span class="value" :class="condition.lateness === 0 ? 'success' : condition.lateness <= (condition.latenessTolerance || 0) ? 'warning' : 'danger'">
            {{ condition.lateness }} months
          </span>
        </div>
        <div v-if="condition.latenessTolerance !== null" class="condition-item">
          <span class="label">Tolerance</span>
          <span class="value">{{ condition.latenessTolerance }} months</span>
        </div>
        <div v-if="condition.projectedDebtFreeDate" class="condition-item">
          <span class="label">Projected Debt-Free Date</span>
          <span class="value">{{ condition.projectedDebtFreeDate }}</span>
        </div>
        <div v-if="condition.totalMonthsRemaining !== null" class="condition-item">
          <span class="label">Total Months Remaining</span>
          <span class="value">{{ condition.totalMonthsRemaining }}</span>
        </div>
        <div v-if="condition.targetDate" class="condition-item">
          <span class="label">Target Date</span>
          <span class="value">{{ condition.targetDate }}</span>
        </div>
      </div>
    </details>
  </div>
</template>

<style scoped>
.status-badge {
  padding: 1rem;
  border-radius: 8px;
  background: var(--color-surface);
}

.status-badge.success {
  border-left: 4px solid var(--color-success);
}

.status-badge.warning {
  border-left: 4px solid var(--color-warning);
}

.status-badge.danger {
  border-left: 4px solid var(--color-danger);
}

.status-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.status-icon {
  font-size: 1.5rem;
}

.status-label {
  font-size: 1.25rem;
  font-weight: 600;
}

.status-explanation {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.condition-details {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border);
}

.condition-details summary {
  cursor: pointer;
  font-size: 0.85rem;
  color: var(--color-primary);
  margin-bottom: 0.5rem;
}

.condition-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.5rem;
}

.condition-item {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.condition-item .label {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.condition-item .value {
  font-size: 0.9rem;
  font-weight: 500;
}

.condition-item .value.success { color: var(--color-success); }
.condition-item .value.warning { color: var(--color-warning); }
.condition-item .value.danger { color: var(--color-danger); }
</style>