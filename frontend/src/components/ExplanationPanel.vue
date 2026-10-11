<script setup lang="ts">
import type { ExplanationView } from '@/api/gps'

interface Props {
  explanations: ExplanationView[]
}

const props = defineProps<Props>()

function getCategoryIcon(category: string): string {
  const icons: Record<string, string> = {
    POSITION: '📊',
    DISTANCE: '📏',
    PROGRESS: '📈',
    CAPACITY_COMPARISON: '⚖️',
    ETA: '🕐',
    STATUS: '🎯',
    BLOCKER: '🚫',
    NEXT_ACTION: '💡',
    PROVENANCE: '🔍'
  }
  return icons[category] || '📋'
}

function getProvenanceColor(kind: string): string {
  switch (kind) {
    case 'actual': return 'success'
    case 'assumed': return 'warning'
    case 'calculated': return 'primary'
    case 'unavailable': return 'danger'
    default: return 'secondary'
  }
}
</script>

<template>
  <div class="explanation-panel">
    <div v-for="exp in props.explanations" :key="exp.field" class="explanation-item">
      <div class="explanation-header">
        <span class="category-icon">{{ getCategoryIcon(exp.category) }}</span>
        <span class="category">{{ exp.category }}</span>
        <span class="field">{{ exp.field }}</span>
      </div>
      
      <div class="explanation-content">
        <div class="rule">
          <span class="label">Rule:</span>
          <span class="value">{{ exp.rule }}</span>
        </div>
        
        <div class="inputs" v-if="exp.inputs.length > 0">
          <span class="label">Inputs:</span>
          <ul>
            <li v-for="input in exp.inputs" :key="input.name">
              <span class="input-name">{{ input.name }}</span>
              <span class="input-value">{{ input.value }}</span>
              <span class="input-provenance" :class="input.provenanceKind">
                ({{ input.provenanceKind }})
              </span>
            </li>
          </ul>
        </div>
        
        <div class="threshold">
          <span class="label">Threshold:</span>
          <span class="value">{{ exp.threshold }}</span>
        </div>
        
        <div class="outcome">
          <span class="label">Outcome:</span>
          <span class="value">{{ exp.outcome }}</span>
        </div>
      </div>
      
      <div class="explanation-provenance">
        <span class="provenance-kind" :class="exp.provenanceKind">
          {{ exp.provenanceKind }}
        </span>
        <span class="provenance-detail">{{ exp.provenanceDetail }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.explanation-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.explanation-item {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 1rem;
}

.explanation-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border);
}

.category-icon {
  font-size: 1.2rem;
}

.category {
  font-weight: 600;
  text-transform: capitalize;
}

.field {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  font-family: monospace;
}

.explanation-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.explanation-content .label {
  font-weight: 500;
  color: var(--color-text);
}

.explanation-content .value {
  color: var(--color-text-muted);
  font-family: monospace;
  font-size: 0.9rem;
}

.inputs ul {
  margin: 0.25rem 0 0;
  padding-left: 1.25rem;
}

.inputs li {
  display: flex;
  gap: 0.5rem;
  align-items: baseline;
  margin-bottom: 0.25rem;
  font-size: 0.9rem;
}

.input-name {
  font-weight: 500;
}

.input-value {
  font-family: monospace;
  color: var(--color-text);
}

.input-provenance {
  font-size: 0.75rem;
  padding: 0.1rem 0.3rem;
  border-radius: 3px;
  text-transform: uppercase;
}

.input-provenance.actual { background: var(--color-success-bg); color: var(--color-success); }
.input-provenance.assumed { background: var(--color-warning-bg); color: var(--color-warning); }
.input-provenance.calculated { background: var(--color-primary-bg); color: var(--color-primary); }
.input-provenance.unavailable { background: var(--color-danger-bg); color: var(--color-danger); }

.explanation-provenance {
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