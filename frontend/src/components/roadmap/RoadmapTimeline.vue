<template>
  <section class="roadmap-timeline" data-testid="roadmap-timeline">
    <!--
      The applied stage policy is shown with its `scope` so the visible order is never a hidden
      truth: it orders STAGES for presentation, not money (spec 005 §7.2).
    -->
    <div class="roadmap-timeline__policy" data-testid="roadmap-order-policy">
      <div class="roadmap-timeline__policy-head">
        <h2 class="roadmap-timeline__title">Thứ tự giai đoạn</h2>
        <div class="roadmap-timeline__chips">
          <span class="roadmap-timeline__policy-name" data-testid="roadmap-order-policy-name">
            {{ roadmap.orderPolicy.policy }}
          </span>
          <span class="roadmap-timeline__scope" data-testid="roadmap-order-scope">
            {{ roadmap.orderPolicy.scope }}
          </span>
        </div>
      </div>
      <p class="roadmap-timeline__policy-text">{{ roadmap.orderPolicy.description }}</p>
      <p class="roadmap-timeline__policy-note" data-testid="roadmap-order-note">
        Đây là thứ tự các giai đoạn trên lộ trình, không phải thứ tự cấp vốn. Vị trí giai đoạn không
        phải là trạng thái cấp vốn.
      </p>
    </div>

    <p v-if="orderedStages.length === 0" class="roadmap-timeline__empty" data-testid="roadmap-timeline-empty">
      Chưa có giai đoạn nào trên lộ trình.
    </p>

    <ol v-else class="roadmap-timeline__list">
      <li
        v-for="stage in orderedStages"
        :key="stageKey(stage)"
        class="roadmap-timeline__item"
        :class="{
          'roadmap-timeline__item--completed': stage.status === 'COMPLETED',
          'roadmap-timeline__item--current': stage.status === 'CURRENT',
          'roadmap-timeline__item--upcoming': stage.status === 'UPCOMING',
          'roadmap-timeline__item--reached': stage.completionReached,
        }"
        :data-testid="`roadmap-stage-${stage.order}`"
      >
        <button
          type="button"
          class="roadmap-timeline__stage"
          :aria-current="stage.status === 'CURRENT' ? 'step' : undefined"
          @click="select(stage)"
        >
          <span class="roadmap-timeline__order" aria-hidden="true">{{ stage.order }}</span>
          <span class="roadmap-timeline__body">
            <span class="roadmap-timeline__objective">{{ stage.objective }}</span>
            <span class="roadmap-timeline__badges">
              <span
                class="roadmap-badge roadmap-badge--position"
                :class="`roadmap-badge--${stage.status.toLowerCase()}`"
                data-testid="stage-status-badge"
              >
                {{ stageStatusLabel(stage.status) }}
              </span>
              <!-- Own completion condition, independent of position: a reached UPCOMING stage keeps both. -->
              <span
                v-if="stage.completionReached"
                class="roadmap-badge roadmap-badge--reached"
                data-testid="stage-reached-badge"
              >
                Đã đạt mục tiêu
              </span>
              <span
                v-if="stage.blockedBy !== null"
                class="roadmap-badge roadmap-badge--blocked"
                data-testid="stage-blocked-badge"
              >
                Chờ giai đoạn {{ stage.blockedBy }}
              </span>
            </span>
          </span>
          <span class="roadmap-timeline__kind">{{ stageKindLabel(stage.kind) }}</span>
        </button>
      </li>
    </ol>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RoadmapStageView, RoadmapView } from '@/api/roadmap'
import { stageKindLabel, stageStatusLabel } from './roadmapLabels'

const props = defineProps<{ roadmap: RoadmapView }>()

const emit = defineEmits<{ (event: 'select', stage: RoadmapStageView): void }>()

/**
 * Render in the server's `order` (1-based, contiguous). Sorting a copy keeps the timeline
 * monotonic even if a payload arrives assembled out of order, and never mutates the prop.
 */
const orderedStages = computed(() =>
  props.roadmap.stages.slice().sort((left, right) => left.order - right.order),
)

function stageKey(stage: RoadmapStageView): string {
  return `${stage.order}-${stage.kind}-${stage.goalId ?? 'debt'}`
}

function select(stage: RoadmapStageView): void {
  emit('select', stage)
}
</script>

<style scoped>
.roadmap-timeline {
  display: grid;
  gap: 14px;
}

.roadmap-timeline__policy {
  padding: 16px 18px;
  background: var(--fg-surface);
  border: 1px solid var(--fg-border-soft);
  border-radius: var(--fg-radius-card);
  box-shadow: var(--fg-shadow-card);
  display: grid;
  gap: 6px;
}

.roadmap-timeline__policy-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.roadmap-timeline__title {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
}

.roadmap-timeline__chips {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.roadmap-timeline__policy-name {
  padding: 2px 8px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  font-weight: 600;
  color: var(--fg-muted);
  background: var(--fg-content-bg);
  border: 1px solid var(--fg-border-soft);
  border-radius: 999px;
}

.roadmap-timeline__scope {
  padding: 2px 8px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  font-weight: 600;
  color: var(--fg-primary-hover);
  background: var(--fg-info-bg);
  border: 1px solid var(--fg-info-border);
  border-radius: 999px;
}

.roadmap-timeline__policy-text {
  margin: 0;
  font-size: 13px;
  color: var(--fg-text);
}

.roadmap-timeline__policy-note {
  margin: 0;
  font-size: 12px;
  color: var(--fg-muted);
}

.roadmap-timeline__empty {
  margin: 0;
  padding: 22px;
  text-align: center;
  color: var(--fg-muted);
  background: var(--fg-surface);
  border: 1px dashed var(--fg-border);
  border-radius: var(--fg-radius-card);
}

.roadmap-timeline__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 10px;
}

.roadmap-timeline__item {
  position: relative;
}

.roadmap-timeline__item + .roadmap-timeline__item::before {
  content: '';
  position: absolute;
  left: 27px;
  top: -10px;
  width: 2px;
  height: 10px;
  background: var(--fg-border-soft);
}

.roadmap-timeline__stage {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 16px;
  font-family: inherit;
  font-size: 14px;
  text-align: left;
  color: var(--fg-text);
  background: var(--fg-surface);
  border: 1px solid var(--fg-border-soft);
  border-radius: var(--fg-radius-card);
  box-shadow: var(--fg-shadow-card);
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.08s;
}

.roadmap-timeline__stage:hover {
  border-color: var(--fg-primary);
  box-shadow: var(--fg-shadow-float);
}

.roadmap-timeline__stage:active {
  transform: translateY(1px);
}

.roadmap-timeline__stage:focus-visible {
  outline: none;
  box-shadow: var(--fg-focus-ring);
}

.roadmap-timeline__order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  font-size: 13px;
  font-weight: 800;
  color: var(--fg-muted);
  background: var(--fg-content-bg);
  border: 1px solid var(--fg-border-soft);
  border-radius: 50%;
}

.roadmap-timeline__body {
  display: grid;
  gap: 5px;
  min-width: 0;
  flex: 1;
}

.roadmap-timeline__objective {
  font-weight: 700;
  color: var(--fg-ink);
}

.roadmap-timeline__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.roadmap-timeline__kind {
  font-size: 12px;
  color: var(--fg-muted);
  white-space: nowrap;
}

/* Positional states — the current stage is the one the route is on right now. */
.roadmap-timeline__item--current .roadmap-timeline__stage {
  border-color: var(--fg-primary);
  box-shadow: var(--fg-shadow-float);
}

.roadmap-timeline__item--current .roadmap-timeline__order {
  color: #fff;
  background: var(--fg-gradient-brand);
  border-color: transparent;
}

.roadmap-timeline__item--completed .roadmap-timeline__order {
  color: var(--fg-success);
  background: var(--fg-journey-bg);
  border-color: var(--fg-journey);
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

.roadmap-badge--blocked {
  color: var(--fg-warn);
  background: var(--fg-warn-bg);
  border-color: rgba(217, 119, 6, 0.3);
}
</style>
