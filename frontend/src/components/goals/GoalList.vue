<template>
  <ul v-if="goals.length" class="goal-grid" data-testid="goal-list">
    <li v-for="goal in goals" :key="goal.id" class="goal-card" data-testid="goal-row">
      <div class="goal-card__top">
        <span class="goal-card__icon" aria-hidden="true">{{ iconFor(goal.goalType) }}</span>
        <div class="goal-card__head">
          <strong class="goal-card__name">{{ goal.name }}</strong>
          <span class="goal-card__meta">{{ typeLabel(goal.goalType) }} · còn lại {{ goal.remaining }} {{ goal.currency }}</span>
        </div>
        <span class="badge" :data-testid="`goal-status-${goal.id}`" :class="badgeClass(goal.status)">
          {{ statusLabel(goal.status) }}
        </span>
      </div>
      <div class="goal-card__progress">
        <span class="goal-card__pct">{{ percentFor(goal) }}</span>
        <span class="goal-card__bar" role="presentation"><span class="goal-card__fill" :style="{ width: percentFor(goal) }"></span></span>
      </div>
      <div class="goal-card__actions">
        <button type="button" class="chip-btn" @click="$emit('edit', goal)">Sửa</button>
        <button type="button" class="chip-btn chip-btn--primary" @click="$emit('capacity', goal)">Khả năng chi trả</button>
        <button type="button" class="chip-btn chip-btn--danger" @click="$emit('remove', goal)">Lưu trữ</button>
      </div>
    </li>
  </ul>
  <div v-else class="empty-state" data-testid="goal-empty">
    <span class="empty-state__art" aria-hidden="true">🏁</span>
    <p class="empty-state__title">Chưa có đích đến nào — hãy cắm cờ đầu tiên</p>
    <p class="empty-state__hint">Mục tiêu giúp GPS tính mỗi tháng bạn cần đi bao nhiêu và khi nào tới nơi. Bắt đầu từ 1 trong 3 mẫu phổ biến:</p>
    <div class="empty-state__templates">
      <button type="button" class="template-chip" @click="$emit('use-template', 'EMERGENCY')">💰 Quỹ khẩn cấp 6 tháng</button>
      <button type="button" class="template-chip" @click="$emit('use-template', 'EDUCATION')">🎓 Học phí cho con</button>
      <button type="button" class="template-chip" @click="$emit('use-template', 'RETIREMENT')">🏖️ Nghỉ hưu chủ động</button>
    </div>
    <p class="empty">Chưa có mục tiêu nào. Thêm mục tiêu đầu tiên của bạn.</p>
  </div>
</template>

<script setup lang="ts">
import type { GoalView } from '../../api/goals'
import { formatGoalPercent } from './goalPercent'

defineProps<{ goals: GoalView[] }>()
defineEmits<{
  (e: 'edit', goal: GoalView): void
  (e: 'remove', goal: GoalView): void
  (e: 'capacity', goal: GoalView): void
  (e: 'use-template', kind: string): void
}>()

function badgeClass(status: string): string {
  if (status === 'COMPLETED') return 'badge--done'
  return 'badge--active'
}

function statusLabel(status: string): string {
  if (status === 'COMPLETED') return 'Đã đạt'
  return 'Đang đi'
}

function typeLabel(t: string): string {
  const map: Record<string, string> = {
    EMERGENCY: 'Khẩn cấp', EDUCATION: 'Giáo dục', RETIREMENT: 'Nghỉ hưu',
    HOUSE: 'Nhà ở', CAR: 'Xe', TRAVEL: 'Du lịch', OTHER: 'Khác',
  }
  return map[t] ?? t
}

function iconFor(t: string): string {
  const map: Record<string, string> = {
    EMERGENCY: '💰', EDUCATION: '🎓', RETIREMENT: '🏖️', HOUSE: '🏠', CAR: '🚗', TRAVEL: '✈️',
  }
  return map[t] ?? '🎯'
}

function percentFor(goal: GoalView): string {
  try { return formatGoalPercent(goal.progress) } catch { return '0%' }
}
</script>

<style scoped>
.goal-grid { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.goal-card {
  padding: 16px; background: var(--fg-surface); border: 1px solid var(--fg-border-soft);
  border-radius: var(--fg-radius-card); box-shadow: var(--fg-shadow-card);
  display: grid; gap: 12px; transition: transform 0.12s, box-shadow 0.15s;
}
.goal-card:hover { transform: translateY(-2px); box-shadow: var(--fg-shadow-float); }
.goal-card__top { display: flex; gap: 12px; align-items: flex-start; }
.goal-card__icon { width: 42px; height: 42px; flex: 0 0 42px; display: grid; place-items: center; font-size: 22px; background: var(--fg-info-bg); border: 1px solid var(--fg-info-border); border-radius: 14px; }
.goal-card__head { min-width: 0; flex: 1; display: grid; gap: 2px; }
.goal-card__name { font-size: 15px; color: var(--fg-ink); overflow-wrap: anywhere; }
.goal-card__meta { font-size: 12.5px; color: var(--fg-muted); }
.badge { font-size: 11px; font-weight: 800; padding: 3px 10px; border-radius: 999px; background: var(--fg-info-bg); white-space: nowrap; }
.badge--done { background: #dcfce7; color: #166534; }
.badge--active { background: #dbeafe; color: #1d4ed8; }
.goal-card__progress { display: grid; grid-template-columns: 56px 1fr; align-items: center; gap: 10px; }
.goal-card__pct { font-weight: 800; color: var(--fg-ink); font-variant-numeric: tabular-nums; }
.goal-card__bar { height: 10px; border-radius: 999px; background: var(--fg-content-bg); overflow: hidden; }
.goal-card__fill { display: block; height: 100%; background: var(--fg-gradient-brand); border-radius: 999px; }
.goal-card__actions { display: flex; gap: 8px; flex-wrap: wrap; }
.chip-btn {
  font-family: inherit; font-size: 13px; font-weight: 700; padding: 7px 14px; border-radius: 999px;
  border: 1px solid var(--fg-border-soft); background: var(--fg-surface); color: var(--fg-label); cursor: pointer;
}
.chip-btn:hover { border-color: var(--fg-primary); color: var(--fg-primary); }
.chip-btn--primary { background: var(--fg-ink); border-color: var(--fg-ink); color: #fff; }
.chip-btn--primary:hover { background: #1e293b; color: #fff; }
.chip-btn--danger { color: var(--fg-danger); }
.chip-btn--danger:hover { border-color: var(--fg-danger); background: var(--fg-danger-bg); color: var(--fg-danger); }
.empty { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
@media (max-width: 860px) { .goal-grid { grid-template-columns: 1fr; } }
</style>
