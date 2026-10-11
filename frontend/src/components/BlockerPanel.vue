<script setup lang="ts">
import type { BlockerView } from '@/api/gps'

interface Props {
  blockers: BlockerView[]
}

const props = defineProps<Props>()

function getBlockerIcon(code: string): string {
  const icons: Record<string, string> = {
    NO_AVAILABLE_CAPACITY: '💰',
    MISSING_FINANCIAL_PROFILE: '📋',
    PAYMENT_DOES_NOT_COVER_INTEREST: '📉',
    PAYMENT_COVERS_ONLY_INTEREST: '📊',
    INTEREST_RATE_MISSING: '❓',
    PAYOFF_HORIZON_EXCEEDS_MAXIMUM: '⏱️',
    PORTFOLIO_CONTAINS_BLOCKED_DEBTS: '🔗'
  }
  return icons[code] || '🚫'
}

function getSeverity(code: string): string {
  if (code === 'MISSING_FINANCIAL_PROFILE') return 'info'
  if (code === 'PORTFOLIO_CONTAINS_BLOCKED_DEBTS') return 'high'
  return 'high'
}
</script>

<template>
  <div class="blocker-panel card">
    <h3>Blockers</h3>
    
    <div v-if="props.blockers.length === 0" class="no-blockers">
      <span class="icon">✅</span>
      <p>No blockers - your route is clear!</p>
    </div>
    
    <div v-else class="blockers-list">
      <div v-for="blocker in props.blockers" :key="blocker.code" class="blocker-item" :class="getSeverity(blocker.code)">
        <div class="blocker-header">
          <span class="blocker-icon">{{ getBlockerIcon(blocker.code) }}</span>
          <span class="blocker-code">{{ blocker.code }}</span>
          <span class="blocker-severity" :class="getSeverity(blocker.code)">{{ getSeverity(blocker.code).toUpperCase() }}</span>
        </div>
        <p class="blocker-explanation">{{ blocker.explanation }}</p>
        <div class="blocker-inputs" v-if="blocker.inputs.length > 0">
          <span class="label">Affected inputs:</span>
          <span class="inputs">{{ blocker.inputs.join(', ') }}</span>
        </div>
        <div class="blocker-provenance">
          <span class="provenance-kind" :class="blocker.provenanceKind">{{ blocker.provenanceKind }}</span>
          <span class="provenance-detail">{{ blocker.provenanceDetail }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.blocker-panel h3 {
  margin: 0 0 1rem;
}

.no-blockers {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2rem;
  text-align: center;
  color: var(--color-success);
}

.no-blockers .icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.blockers-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.blocker-item {
  padding: 1rem;
  border-radius: 8px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}

.blocker-item.high {
  border-left: 4px solid var(--color-danger);
}

.blocker-item.info {
  border-left: 4px solid var(--color-info);
}

.blocker-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.blocker-icon {
  font-size: 1.25rem;
}

.blocker-code {
  font-weight: 600;
  font-family: monospace;
  font-size: 0.9rem;
}

.blocker-severity {
  margin-left: auto;
  padding: 0.15rem 0.5rem;
  border-radius: 3px;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
}

.blocker-severity.high {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.blocker-severity.info {
  background: var(--color-info-bg);
  color: var(--color-info);
}

.blocker-explanation {
  margin: 0 0 0.5rem;
  color: var(--color-text);
  line-height: 1.5;
}

.blocker-inputs {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  margin-bottom: 0.5rem;
}

.blocker-inputs .inputs {
  font-family: monospace;
  color: var(--color-text);
}

.blocker-provenance {
  display: flex;
  gap: 0.5rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--color-border);
  font-size: 0.8rem;
}

.provenance-kind {
  padding: 0.15rem 0.4rem;
  border-radius: 3px;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
}

.provenance-kind.actual { background: var(--color-success-bg); color: var(--color-success); }
.provenance-kind.assumed { background: var(--color-warning-bg); color: var(--color-warning); }
.provenance-kind.calculated { background: var(--color-primary-bg); color: var(--color-primary); }
.provenance-kind.unavailable { background: var(--color-danger-bg); color: var(--color-danger); }

.provenance-detail {
  color: var(--color-text-muted);
  flex: 1;
}
</style>