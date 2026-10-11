<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGoalStore } from '@/stores/goalStore'
import { fetchFinancialGps } from '@/api/gps'
import { FinancialGpsView, StatusView } from '@/api/gps'
import MoneyDisplay from '@/components/MoneyDisplay.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import ExplanationPanel from '@/components/ExplanationPanel.vue'
import BlockerPanel from '@/components/BlockerPanel.vue'
import NextActionPanel from '@/components/NextActionPanel.vue'

const route = useRoute()
const router = useRouter()
const goalStore = useGoalStore()

const gpsData = ref<FinancialGpsView | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const selectedGoalId = ref<string | null>(null)

const goals = computed(() => goalStore.goals.filter(g => g.status !== 'ARCHIVED'))

async function loadGps() {
  if (!selectedGoalId.value) return
  loading.value = true
  error.value = null
  try {
    gpsData.value = await fetchFinancialGps(selectedGoalId.value)
  } catch (e: any) {
    error.value = e.response?.data?.detail || 'Failed to load GPS data'
    console.error(e)
  } finally {
    loading.value = false
  }
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString()
}

function getStatusColor(status: string): string {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'ON_TRACK': return 'success'
    case 'AT_RISK': return 'warning'
    case 'OFF_TRACK': return 'danger'
    case 'BLOCKED': return 'danger'
    default: return 'secondary'
  }
}

function getProgressPercent(progress: string | null): number {
  if (!progress) return 0
  return Math.round(parseFloat(progress) * 100)
}

onMounted(async () => {
  await goalStore.fetchGoals()
  // Auto-select first non-archived goal if available
  if (goals.value.length > 0) {
    selectedGoalId.value = goals.value[0].id
    await loadGps()
  }
})

// Watch for goal selection change
import { watch } from 'vue'
watch(selectedGoalId, () => {
  loadGps()
})
</script>

<template>
  <div class="gps-view">
    <div class="page-header">
      <h1>Financial GPS</h1>
      <p class="subtitle">Navigate from your current position to your selected destination</p>
    </div>

    <div v-if="goals.length === 0" class="empty-state">
      <div class="empty-icon">🎯</div>
      <h2>No destination selected</h2>
      <p>Create a goal first to see your Financial GPS.</p>
      <a href="/goals" class="btn btn-primary">Create a Goal</a>
    </div>

    <div v-else class="gps-content">
      <!-- Goal Selector -->
      <div class="goal-selector">
        <label for="goal-select">Select Destination</label>
        <select
          id="goal-select"
          v-model="selectedGoalId"
          class="form-select"
          :disabled="loading"
        >
          <option v-for="goal in goals" :key="goal.id" :value="goal.id">
            {{ goal.name }} ({{ goal.goalType }}) - {{ goal.status }}
          </option>
        </select>
      </div>

      <!-- Loading / Error -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>Calculating your route...</p>
      </div>

      <div v-else-if="error" class="error-state">
        <p class="error-message">{{ error }}</p>
        <button class="btn btn-secondary" @click="loadGps">Retry</button>
      </div>

      <!-- GPS Result -->
      <div v-else-if="gpsData" class="gps-result">
        <!-- Current Position & Destination Summary -->
        <section class="gps-section position-destination">
          <div class="card position-card">
            <h3>Current Position <span class="as-of">as of {{ formatDate(gpsData.asOf) }}</span></h3>
            <div class="position-grid">
              <div class="position-item">
                <MoneyDisplay :amount="gpsData.currentPosition.income.amount" :currency="gpsData.currentPosition.income.currency" />
                <span class="label">Income</span>
                <span v-if="gpsData.currentPosition.income.availability === 'UNAVAILABLE'" class="unavailable-badge">Unavailable</span>
              </div>
              <div class="position-item">
                <MoneyDisplay :amount="gpsData.currentPosition.expense.amount" :currency="gpsData.currentPosition.expense.currency" />
                <span class="label">Expenses</span>
                <span v-if="gpsData.currentPosition.expense.availability === 'UNAVAILABLE'" class="unavailable-badge">Unavailable</span>
              </div>
              <div class="position-item">
                <MoneyDisplay :amount="gpsData.currentPosition.mandatoryPayment.amount" :currency="gpsData.currentPosition.mandatoryPayment.currency" />
                <span class="label">Mandatory Payment</span>
                <span v-if="gpsData.currentPosition.mandatoryPayment.availability === 'UNAVAILABLE'" class="unavailable-badge">Unavailable</span>
              </div>
              <div class="position-item highlight">
                <MoneyDisplay :amount="gpsData.currentPosition.netCashFlow.amount" :currency="gpsData.currentPosition.netCashFlow.currency" />
                <span class="label">Net Cash Flow</span>
                <span v-if="gpsData.currentPosition.netCashFlow.availability === 'UNAVAILABLE'" class="unavailable-badge">Unavailable</span>
              </div>
              <div class="position-item highlight">
                <MoneyDisplay :amount="gpsData.currentPosition.availableCapacity.amount" :currency="gpsData.currentPosition.availableCapacity.currency" />
                <span class="label">Available Capacity</span>
                <span v-if="gpsData.currentPosition.availableCapacity.availability === 'UNAVAILABLE'" class="unavailable-badge">Unavailable</span>
              </div>
              <div class="position-item">
                <MoneyDisplay :amount="gpsData.currentPosition.totalOutstandingDebt.amount" :currency="gpsData.currentPosition.totalOutstandingDebt.currency" />
                <span class="label">Total Debt</span>
              </div>
              <div class="position-item" v-if="gpsData.currentPosition.savings.amount">
                <MoneyDisplay :amount="gpsData.currentPosition.savings.amount" :currency="gpsData.currentPosition.savings.currency" />
                <span class="label">Savings</span>
              </div>
              <div class="position-item" v-if="gpsData.currentPosition.emergencyFund.amount">
                <MoneyDisplay :amount="gpsData.currentPosition.emergencyFund.amount" :currency="gpsData.currentPosition.emergencyFund.currency" />
                <span class="label">Emergency Fund</span>
              </div>
            </div>
          </div>

          <div class="card destination-card">
            <h3>Destination</h3>
            <div class="destination-info">
              <div class="destination-type">
                <span class="badge" :class="gpsData.destination.type === 'DEBT_FREEDOM' ? 'badge-info' : 'badge-primary'">
                  {{ gpsData.destination.type === 'DEBT_FREEDOM' ? '🏁 Debt Freedom' : '🎯 ' + gpsData.destination.goalType }}
                </span>
              </div>
              <h4 v-if="gpsData.destination.type === 'GOAL'">{{ gpsData.destination.goalName }}</h4>
              <h4 v-else>Debt Freedom</h4>
              
              <div class="distance-info">
                <div class="distance-main">
                  <span class="label">Distance Remaining</span>
                  <MoneyDisplay :amount="gpsData.distance.remaining" :currency="gpsData.distance.currency" size="large" />
                </div>
                <div class="progress-info" v-if="gpsData.progress.progressPercent">
                  <span class="label">Progress</span>
                  <div class="progress-bar">
                    <div 
                      class="progress-fill" 
                      :style="{ width: getProgressPercent(gpsData.progress.progressPercent) + '%' }"
                    ></div>
                  </div>
                  <span class="progress-text">{{ getProgressPercent(gpsData.progress.progressPercent) }}%</span>
                </div>
                <div class="progress-info" v-else>
                  <span class="label">Progress</span>
                  <span class="progress-text">{{ gpsData.progress.reason }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- Status & ETA -->
        <section class="gps-section status-eta">
          <div class="card status-card">
            <h3>Route Status</h3>
            <StatusBadge :status="gpsData.status.status" :explanation="gpsData.status.explanation" :condition="gpsData.status.condition" />
          </div>

          <div class="card eta-card">
            <h3>Estimated Arrival</h3>
            <div v-if="gpsData.eta.availability === 'CALCULATED'" class="eta-calculated">
              <div class="eta-date">
                <span class="label">Projected Date</span>
                <span class="value">{{ formatDate(gpsData.eta.date) }}</span>
              </div>
              <div class="eta-periods">
                <span class="label">Months Remaining</span>
                <span class="value">{{ gpsData.eta.periods }}</span>
              </div>
            </div>
            <div v-else class="eta-unavailable">
              <span class="label">ETA Unavailable</span>
              <span class="reason">{{ gpsData.eta.reason }}</span>
              <p class="explanation">{{ gpsData.eta.explanation }}</p>
            </div>
          </div>

          <div class="card capacity-card">
            <h3>Capacity Comparison</h3>
            <div v-if="gpsData.capacityComparison.coverage !== 'NOT_APPLICABLE'">
              <div class="capacity-row">
                <span class="label">Required / Month</span>
                <MoneyDisplay :amount="gpsData.capacityComparison.requiredMonthly" :currency="gpsData.capacityComparison.currency" />
              </div>
              <div class="capacity-row">
                <span class="label">Available Capacity</span>
                <MoneyDisplay :amount="gpsData.capacityComparison.projectedMonthly" :currency="gpsData.capacityComparison.currency" />
              </div>
              <div class="capacity-row" v-if="gpsData.capacityComparison.shortfall">
                <span class="label shortfall-label">Shortfall</span>
                <MoneyDisplay :amount="gpsData.capacityComparison.shortfall" :currency="gpsData.capacityComparison.currency" class="shortfall" />
              </div>
              <p class="capacity-explanation">{{ gpsData.capacityComparison.explanation }}</p>
            </div>
            <div v-else class="not-applicable">
              <p>{{ gpsData.capacityComparison.explanation }}</p>
            </div>
          </div>
        </section>

        <!-- Blockers & Next Actions -->
        <section class="gps-section blockers-actions">
          <BlockerPanel :blockers="gpsData.blockers" />
          <NextActionPanel :actions="gpsData.nextActions" />
        </section>

        <!-- Explanations -->
        <section class="gps-section explanations">
          <h2>How This Was Calculated</h2>
          <ExplanationPanel :explanations="gpsData.explanations" />
        </section>

        <!-- Provenance -->
        <section class="gps-section provenance">
          <h2>Data Provenance</h2>
          <details>
            <summary>View provenance details</summary>
            <table class="provenance-table">
              <thead>
                <tr>
                  <th>Field</th>
                  <th>Kind</th>
                  <th>Detail</th>
                  <th>Assumption Source</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in gpsData.provenance" :key="p.field">
                  <td>{{ p.field }}</td>
                  <td><span class="kind-badge" :class="p.kind">{{ p.kind }}</span></td>
                  <td>{{ p.detail }}</td>
                  <td>{{ p.assumptionSource || '—' }}</td>
                </tr>
              </tbody>
            </table>
          </details>
        </section>

        <!-- Missing Inputs -->
        <div v-if="gpsData.missingInputs.length > 0" class="missing-inputs">
          <h3>Missing Inputs</h3>
          <ul>
            <li v-for="input in gpsData.missingInputs" :key="input">{{ input }}</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.gps-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1.5rem;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  margin: 0 0 0.5rem;
  font-size: 2rem;
}

.subtitle {
  color: var(--color-text-muted);
  margin: 0;
}

.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: var(--color-surface);
  border-radius: 8px;
  border: 1px solid var(--color-border);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.goal-selector {
  margin-bottom: 1.5rem;
}

.goal-selector label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-select {
  width: 100%;
  max-width: 400px;
  padding: 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background: var(--color-surface);
}

.loading-state, .error-state {
  text-align: center;
  padding: 3rem;
}

.spinner {
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.gps-section {
  margin-bottom: 2rem;
}

.gps-section h2 {
  margin: 0 0 1rem;
  font-size: 1.25rem;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 0.5rem;
}

.card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 1.5rem;
}

.card h3 {
  margin: 0 0 1rem;
  font-size: 1.1rem;
}

.as-of {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  font-weight: normal;
}

.position-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.position-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.position-item.highlight {
  border-top: 3px solid var(--color-primary);
  padding-top: 0.5rem;
}

.position-item .label {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.unavailable-badge {
  font-size: 0.7rem;
  background: var(--color-warning-bg);
  color: var(--color-warning);
  padding: 0.1rem 0.3rem;
  border-radius: 3px;
  align-self: flex-start;
}

.destination-card {
  background: linear-gradient(135deg, var(--color-primary-bg), var(--color-surface));
}

.destination-type {
  margin-bottom: 0.5rem;
}

.distance-info {
  margin-top: 1rem;
}

.distance-main {
  margin-bottom: 1rem;
}

.distance-main .label {
  display: block;
  font-size: 0.8rem;
  color: var(--color-text-muted);
  margin-bottom: 0.25rem;
}

.progress-bar {
  height: 8px;
  background: var(--color-border);
  border-radius: 4px;
  overflow: hidden;
  margin: 0.5rem 0;
}

.progress-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.status-eta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
}

.eta-calculated, .capacity-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--color-border);
}

.eta-calculated:last-child, .capacity-row:last-child {
  border-bottom: none;
}

.eta-unavailable {
  color: var(--color-danger);
}

.eta-unavailable .reason {
  display: block;
  font-weight: 500;
  margin-bottom: 0.5rem;
}

.eta-unavailable .explanation {
  margin: 0;
  font-size: 0.9rem;
  color: var(--color-text-muted);
}

.capacity-explanation {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border);
  font-size: 0.9rem;
  color: var(--color-text-muted);
}

.not-applicable {
  color: var(--color-text-muted);
  font-style: italic;
}

.shortfall-label {
  color: var(--color-danger);
}

.shortfall {
  color: var(--color-danger);
}

.blockers-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
}

.provenance-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1rem;
  font-size: 0.85rem;
}

.provenance-table th,
.provenance-table td {
  padding: 0.5rem;
  text-align: left;
  border-bottom: 1px solid var(--color-border);
}

.kind-badge {
  display: inline-block;
  padding: 0.15rem 0.4rem;
  border-radius: 3px;
  font-size: 0.75rem;
  font-weight: 500;
  text-transform: uppercase;
}

.kind-badge.actual { background: var(--color-success-bg); color: var(--color-success); }
.kind-badge.assumed { background: var(--color-warning-bg); color: var(--color-warning); }
.kind-badge.calculated { background: var(--color-primary-bg); color: var(--color-primary); }
.kind-badge.unavailable { background: var(--color-danger-bg); color: var(--color-danger); }

.missing-inputs {
  padding: 1rem;
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  border-radius: 8px;
  color: var(--color-warning);
}

.missing-inputs h3 {
  margin: 0 0 0.5rem;
}
</style>