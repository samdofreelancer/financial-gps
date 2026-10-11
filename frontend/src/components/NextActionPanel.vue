<script setup lang="ts">
import type { NextActionView } from '@/api/gps'

interface Props {
  actions: NextActionView[]
}

const props = defineProps<Props>()

function getActionIcon(type: string): string {
  const icons: Record<string, string> = {
    INCREASE_NET_CASH_FLOW: '💰',
    RAISE_DEBT_PAYMENT: '📈',
    COMPLETE_FINANCIAL_PROFILE: '📋',
    SUPPLY_TARGET_DATE: '📅',
    SUPPLY_INTEREST_RATE: '📊',
    REVIEW_DEBT_TERMS: '🔍'
  }
  return icons[type] || '💡'
}
</script>

<template>
  <div class="action-panel card">
    <h3>Suggested Next Steps</h3>
    
    <div v-if="props.actions.length === 0" class="no-actions">
      <span class="icon">✅</span>
      <p>No specific actions needed at this time.</p>
    </div>
    
    <div v-else class="actions-list">
      <div v-for="action in props.actions" :key="action.type + action.description" class="action-item">
        <div class="action-header">
          <span class="action-icon">{{ getActionIcon(action.type) }}</span>
          <span class="action-type">{{ action.type.replace(/_/g, ' ') }}</span>
        </div>
        <p class="action-description">{{ action.description }}</p>
        <div class="action-linked" v-if="action.linkedBlockerCodes.length > 0">
          <span class="label">Addresses:</span>
          <span class="codes">{{ action.linkedBlockerCodes.join(', ') }}</span>
        </div>
        <div class="action-provenance">
          <span class="provenance-kind" :class="action.provenanceKind">{{ action.provenanceKind }}</span>
          <span class="provenance-detail">{{ action.provenanceDetail }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.action-panel h3 {
  margin: 0 0 1rem;
}

.no-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2rem;
  text-align: center;
  color: var(--color-success);
}

.no-actions .icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.actions-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.action-item {
  padding: 1rem;
  border-radius: 8px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}

.action-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.action-icon {
  font-size: 1.25rem;
}

.action-type {
  font-weight: 600;
  text-transform: capitalize;
  font-size: 0.9rem;
}

.action-description {
  margin: 0 0 0.5rem;
  color: var(--color-text);
  line-height: 1.5;
}

.action-linked {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  margin-bottom: 0.5rem;
}

.action-linked .codes {
  font-family: monospace;
  color: var(--color-text);
}

.action-provenance {
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