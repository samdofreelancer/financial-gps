<template>
  <div class="page roadmap-view">
    <header class="page-head">
      <div>
        <p class="journey-eyebrow">Hành trình tài chính</p>
        <h1 class="page-title">Lộ trình tài chính</h1>
        <p class="page-head__sub">
          Các giai đoạn theo thứ tự từ vị trí hiện tại đến đích. Vị trí giai đoạn không phải là trạng
          thái cấp vốn.
        </p>
      </div>
      <p v-if="roadmap" class="roadmap-view__asof" data-testid="roadmap-as-of">
        Tính đến {{ roadmap.asOf }} · {{ roadmap.currency }}
      </p>
    </header>

    <div v-if="store.loading" class="card roadmap-view__placeholder" data-testid="roadmap-loading">
      Đang tải lộ trình tài chính…
    </div>

    <div
      v-else-if="store.error"
      class="card roadmap-view__placeholder"
      role="alert"
      data-testid="roadmap-error"
    >
      <p class="roadmap-view__error">
        <strong>Không tải được lộ trình tài chính.</strong>
        {{ store.error }}
      </p>
      <button type="button" class="btn-ghost small" :disabled="store.loading" @click="reload">
        Thử lại
      </button>
    </div>

    <template v-else-if="roadmap && roadmap.routeStatus === 'NO_DESTINATION'">
      <div class="empty-state" data-testid="roadmap-empty">
        <div class="empty-state__art" aria-hidden="true">🧭</div>
        <h2 class="empty-state__title">Chưa có đích đến nào trên lộ trình</h2>
        <p class="empty-state__hint">
          Lộ trình cần ít nhất một khoản nợ đang hoạt động hoặc một mục tiêu tài chính còn hiệu lực. Hãy
          thêm mục tiêu để bắt đầu hành trình.
        </p>
        <router-link to="/goals" class="btn">Thêm mục tiêu</router-link>
      </div>

      <section v-if="roadmap.explanations.length > 0" class="roadmap-view__explanations">
        <h2 class="roadmap-view__section-title">Vì sao chưa có lộ trình?</h2>
        <ExplanationPanel :sentences="roadmap.explanations" />
      </section>
    </template>

    <template v-else-if="roadmap">
      <div v-if="roadmap.routeStatus === 'COMPLETED'" class="roadmap-view__completed" data-testid="roadmap-completed">
        <span class="roadmap-view__completed-icon" aria-hidden="true">✓</span>
        <div>
          <h2 class="roadmap-view__completed-title">Toàn bộ lộ trình đã hoàn thành</h2>
          <p class="roadmap-view__completed-text">
            Mọi giai đoạn đã đạt điều kiện hoàn thành của nó, nên hiện không còn giai đoạn hiện tại.
          </p>
        </div>
      </div>

      <div class="roadmap-view__layout">
        <RoadmapTimeline :roadmap="roadmap" @select="selectStage" />

        <section v-if="selectedStage" class="roadmap-view__details" data-testid="roadmap-selected-stage">
          <RoadmapStageDetails :stage="selectedStage" :currency="roadmap.currency" />
        </section>
        <p v-else class="card roadmap-view__placeholder" data-testid="roadmap-no-selection">
          Chọn một giai đoạn để xem điều kiện, năng lực và việc cần làm tiếp theo.
        </p>
      </div>

      <section class="roadmap-view__explanations">
        <h2 class="roadmap-view__section-title">Vì sao lộ trình này?</h2>
        <ExplanationPanel :sentences="roadmap.explanations" />
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { RoadmapStageView } from '@/api/roadmap'
import { useRoadmapStore } from '@/stores/roadmapStore'
import RoadmapTimeline from '@/components/roadmap/RoadmapTimeline.vue'
import RoadmapStageDetails from '@/components/roadmap/RoadmapStageDetails.vue'
import ExplanationPanel from '@/components/ExplanationPanel.vue'

const store = useRoadmapStore()
const route = useRoute()

const selectedOrder = ref<number | null>(null)

const roadmap = computed(() => store.roadmap)

/** Derived from the store response — never a stale stage snapshot after a reload. */
const selectedStage = computed(
  () => roadmap.value?.stages.find((stage) => stage.order === selectedOrder.value) ?? null,
)

/** FR-015 style: an explicit `?asOf=` query is a first-class input, otherwise the server's date wins. */
function asOfQuery(): string | undefined {
  const value = route.query.asOf
  return typeof value === 'string' && value.length > 0 ? value : undefined
}

async function load(): Promise<void> {
  selectedOrder.value = null
  await store.load(asOfQuery())
  // SC-001: the current stage and its next action are visible without navigating anywhere.
  selectedOrder.value = store.roadmap?.currentStageOrder ?? null
}

function selectStage(stage: RoadmapStageView): void {
  selectedOrder.value = stage.order
}

function reload(): void {
  void load()
}

watch(
  () => route.query.asOf,
  () => {
    void load()
  },
)

onMounted(() => {
  void load()
})
</script>

<style scoped>
.roadmap-view {
  max-width: 1120px;
}

.roadmap-view__asof {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--fg-muted);
  white-space: nowrap;
}

.roadmap-view__placeholder {
  padding: 28px;
  text-align: center;
  color: var(--fg-muted);
  display: grid;
  gap: 10px;
  justify-items: center;
}

.roadmap-view__error {
  margin: 0;
  color: var(--fg-danger);
}

.roadmap-view__layout {
  display: grid;
  grid-template-columns: minmax(300px, 400px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.roadmap-view__details {
  min-width: 0;
}

.roadmap-view__completed {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 18px;
  background: var(--fg-journey-bg);
  border: 1px solid rgba(22, 163, 74, 0.3);
  border-radius: var(--fg-radius-card);
  box-shadow: var(--fg-shadow-card);
}

.roadmap-view__completed-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  font-size: 16px;
  font-weight: 800;
  color: #fff;
  background: var(--fg-success);
  border-radius: 50%;
}

.roadmap-view__completed-title {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: #166534;
}

.roadmap-view__completed-text {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--fg-text);
}

.roadmap-view__explanations {
  display: grid;
  gap: 10px;
}

.roadmap-view__section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
}

@media (max-width: 1000px) {
  .roadmap-view__layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
