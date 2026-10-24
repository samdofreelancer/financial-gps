<template>
  <article class="roadmap-stage-details" data-testid="roadmap-stage-details">
    <header class="roadmap-stage-details__head">
      <p class="roadmap-stage-details__eyebrow">
        Giai đoạn {{ stage.order }} · {{ stageKindLabel(stage.kind) }}
      </p>
      <h2 class="roadmap-stage-details__title" data-testid="stage-objective">{{ stage.objective }}</h2>
      <div class="roadmap-stage-details__badges">
        <span
          class="roadmap-badge roadmap-badge--position"
          :class="`roadmap-badge--${stage.status.toLowerCase()}`"
          data-testid="stage-status-badge"
        >
          {{ stageStatusLabel(stage.status) }}
        </span>
        <!-- Own completion condition, beside (never instead of) the positional status. -->
        <span
          v-if="stage.completionReached"
          class="roadmap-badge roadmap-badge--reached"
          data-testid="stage-reached-badge"
        >
          Đã đạt mục tiêu
        </span>
      </div>

      <p class="roadmap-stage-details__position-note" data-testid="stage-position-note">
        Vị trí giai đoạn là thứ tự trên lộ trình, <strong>không phải là trạng thái cấp vốn</strong>.
        <template v-if="stage.status === 'UPCOMING'">
          Giai đoạn “Sắp tới” không có nghĩa là giai đoạn chưa được cấp vốn: việc cấp vốn do quy tắc
          phân bổ quyết định, không do vị trí giai đoạn.
        </template>
      </p>

      <p v-if="stage.blockedBy !== null" class="roadmap-stage-details__blocked" data-testid="stage-blocked-by">
        Chưa phải giai đoạn hiện tại vì giai đoạn {{ stage.blockedBy }} chưa hoàn thành điều kiện của nó.
      </p>
    </header>

    <section v-if="positionExplanations.length > 0" class="roadmap-stage-details__section">
      <h3 class="roadmap-stage-details__section-title">Vì sao giai đoạn này ở vị trí này?</h3>
      <ExplanationPanel :sentences="positionExplanations" />
    </section>

    <dl class="roadmap-stage-details__facts">
      <div class="roadmap-stage-details__fact">
        <dt class="roadmap-stage-details__label">Điều kiện bắt đầu</dt>
        <dd class="roadmap-stage-details__value" data-testid="stage-start-condition">
          {{ stage.startCondition }}
        </dd>
      </div>

      <div class="roadmap-stage-details__fact">
        <dt class="roadmap-stage-details__label">Điều kiện hoàn thành</dt>
        <dd class="roadmap-stage-details__value" data-testid="stage-completion-condition">
          {{ stage.completionCondition }}
        </dd>
      </div>

      <!-- Kind-dependent figure: a goal has a required capacity, the debt stage a planned payment. -->
      <div class="roadmap-stage-details__fact">
        <dt class="roadmap-stage-details__label" data-testid="stage-capacity-label">
          {{ capacityLabel }}
        </dt>
        <dd class="roadmap-stage-details__value" data-testid="stage-capacity-amount">
          <MoneyDisplay :amount="capacityAmount" :currency="currency" />
          <span v-if="capacityAmount === null" class="roadmap-stage-details__hint">
            Không áp dụng cho giai đoạn này.
          </span>
        </dd>
      </div>

      <div v-if="stage.progress !== null" class="roadmap-stage-details__fact">
        <dt class="roadmap-stage-details__label">Tiến độ</dt>
        <dd class="roadmap-stage-details__value" data-testid="stage-progress">
          {{ formatProgress(stage.progress) }}
        </dd>
      </div>

      <div class="roadmap-stage-details__fact">
        <dt class="roadmap-stage-details__label">Ngày hoàn thành dự kiến</dt>
        <dd class="roadmap-stage-details__value" data-testid="stage-eta">
          <template v-if="stage.projectedCompletionDate">
            <time :datetime="stage.projectedCompletionDate" data-testid="stage-eta-date">
              {{ formatDate(stage.projectedCompletionDate) }}
            </time>
          </template>
          <template v-else>
            <span class="roadmap-stage-details__unavailable" data-testid="stage-eta-unavailable">
              Chưa xác định được ngày hoàn thành
            </span>
            <span v-if="stage.etaReasonCode" class="roadmap-stage-details__reason" data-testid="stage-eta-reason">
              {{ etaReasonLabel(stage.etaReasonCode) }}
              <code class="roadmap-stage-details__code">{{ stage.etaReasonCode }}</code>
            </span>
          </template>
        </dd>
      </div>

      <div class="roadmap-stage-details__fact">
        <dt class="roadmap-stage-details__label">Cơ sở dự báo</dt>
        <dd class="roadmap-stage-details__value" data-testid="stage-eta-basis">
          {{ etaBasisLabel(stage.etaBasis) }}
        </dd>
      </div>

      <div class="roadmap-stage-details__fact">
        <dt class="roadmap-stage-details__label">Giả định của ngày dự kiến</dt>
        <dd class="roadmap-stage-details__value" data-testid="stage-eta-assumption">
          {{ etaAssumptionLabel(stage.etaAssumption) }}
        </dd>
      </div>
    </dl>

    <section class="roadmap-stage-details__section" data-testid="stage-blockers">
      <BlockerPanel :blockers="stage.blockers" />
    </section>

    <section class="roadmap-stage-details__section">
      <h3 class="roadmap-stage-details__section-title">Việc cần làm tiếp theo</h3>
      <p class="roadmap-stage-details__next-action" data-testid="stage-next-action">
        {{ stage.nextAction }}
      </p>
      <p class="roadmap-stage-details__note">
        Lộ trình không đề xuất một số tiền cụ thể và không kết luận giai đoạn đã được cấp bao nhiêu.
      </p>
    </section>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RoadmapStageView } from '@/api/roadmap'
import MoneyDisplay from '@/components/MoneyDisplay.vue'
import BlockerPanel from '@/components/BlockerPanel.vue'
import ExplanationPanel from '@/components/ExplanationPanel.vue'
import { formatGoalPercent } from '@/components/goals/goalPercent'
import {
  etaAssumptionLabel,
  etaBasisLabel,
  etaReasonLabel,
  stageKindLabel,
  stageStatusLabel,
} from './roadmapLabels'

const props = defineProps<{ stage: RoadmapStageView; currency: string }>()

const isDebtStage = computed(() => props.stage.kind === 'DEBT_ELIMINATION')

/** Exactly one of the two kind-specific figures is applicable — never both, never an invented one. */
const capacityAmount = computed(() =>
  isDebtStage.value ? props.stage.totalPlannedMonthlyPayment : props.stage.requiredMonthlyCapacity,
)

const capacityLabel = computed(() =>
  isDebtStage.value ? 'Khoản trả hàng tháng theo kế hoạch' : 'Năng lực hàng tháng cần thiết',
)

/**
 * Position reasoning, restated only from the stage's own server facts (`status`,
 * `completionReached`). Presentation only: it adds no financial claim — no funded amount, no
 * shortfall and no allocation status — and never rewrites either server field.
 */
const positionExplanations = computed<string[]>(() => {
  const sentences: string[] = []
  if (props.stage.status === 'CURRENT') {
    sentences.push(
      'Đây là giai đoạn hiện tại trên lộ trình: các giai đoạn trước đã hoàn thành và giai đoạn này chưa đạt điều kiện hoàn thành của nó.',
    )
  }
  if (props.stage.completionReached && props.stage.status !== 'COMPLETED') {
    sentences.push(
      'Điều kiện hoàn thành riêng của giai đoạn đã đạt; vị trí trên lộ trình vẫn do các giai đoạn trước quyết định và không phải là tuyên bố về việc cấp vốn.',
    )
  }
  if (props.stage.status === 'COMPLETED') {
    sentences.push(
      'Giai đoạn này đã hoàn thành trên lộ trình: điều kiện của nó đã đạt và mọi giai đoạn trước cũng đã hoàn thành.',
    )
  }
  return sentences
})

/** Integer string math (never a float) for the 003 scale-4 ratio. */
function formatProgress(progress: string | null): string {
  return progress === null ? '—' : formatGoalPercent(progress)
}

/** Parse the ISO date as a LOCAL calendar date — `new Date('2027-12-31')` is UTC midnight. */
function formatDate(dateIso: string): string {
  const [year, month, day] = dateIso.split('-').map(Number)
  if (!year || !month || !day) return dateIso
  return new Date(year, month - 1, day).toLocaleDateString()
}
</script>

<style scoped>
.roadmap-stage-details {
  display: grid;
  gap: 14px;
  padding: 20px 22px;
  background: var(--fg-surface);
  border: 1px solid var(--fg-border-soft);
  border-radius: var(--fg-radius-card);
  box-shadow: var(--fg-shadow-card);
}

.roadmap-stage-details__head {
  display: grid;
  gap: 8px;
}

.roadmap-stage-details__eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--fg-primary-hover);
}

.roadmap-stage-details__title {
  margin: 0;
  font-size: 19px;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.roadmap-stage-details__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.roadmap-stage-details__position-note {
  margin: 0;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--fg-text);
  background: var(--fg-info-bg);
  border: 1px solid var(--fg-info-border);
  border-radius: var(--fg-radius-control);
}

.roadmap-stage-details__blocked {
  margin: 0;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--fg-warn);
  background: var(--fg-warn-bg);
  border: 1px solid rgba(217, 119, 6, 0.3);
  border-radius: var(--fg-radius-control);
}

.roadmap-stage-details__facts {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.roadmap-stage-details__fact {
  display: grid;
  gap: 3px;
  padding: 10px 12px;
  background: var(--fg-app-bg);
  border: 1px solid var(--fg-border-soft);
  border-radius: var(--fg-radius-control);
}

.roadmap-stage-details__label {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--fg-muted);
}

.roadmap-stage-details__value {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--fg-text);
}

.roadmap-stage-details__hint,
.roadmap-stage-details__reason {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--fg-muted);
}

.roadmap-stage-details__unavailable {
  display: block;
  font-weight: 600;
  color: var(--fg-warn);
}

.roadmap-stage-details__code {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: var(--fg-muted);
}

.roadmap-stage-details__section {
  display: grid;
  gap: 8px;
}

.roadmap-stage-details__section-title {
  margin: 0;
  font-size: 14px;
  font-weight: 800;
}

.roadmap-stage-details__next-action {
  margin: 0;
  padding: 12px 14px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--fg-ink);
  background: var(--fg-journey-bg);
  border: 1px solid rgba(22, 163, 74, 0.25);
  border-radius: var(--fg-radius-control);
}

.roadmap-stage-details__note {
  margin: 0;
  font-size: 12px;
  color: var(--fg-muted);
}

.roadmap-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 700;
  border-radius: 999px;
  border: 1px solid transparent;
}

.roadmap-badge--current {
  color: var(--fg-primary-hover);
  background: var(--fg-info-bg);
  border-color: var(--fg-info-border);
}

.roadmap-badge--completed {
  color: var(--fg-success);
  background: var(--fg-journey-bg);
  border-color: rgba(22, 163, 74, 0.3);
}

.roadmap-badge--upcoming {
  color: var(--fg-muted);
  background: var(--fg-content-bg);
  border-color: var(--fg-border-soft);
}

.roadmap-badge--reached {
  color: #166534;
  background: #dcfce7;
  border-color: rgba(22, 163, 74, 0.35);
}
</style>
