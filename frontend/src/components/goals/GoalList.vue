<template>
  <ul v-if="goals.length" class="goal-list" data-testid="goal-list">
    <li v-for="goal in goals" :key="goal.id" class="goal-row" data-testid="goal-row">
      <div class="goal-row__head">
        <strong>{{ goal.name }}</strong>
        <span class="badge" :data-testid="`goal-status-${goal.id}`" :class="badgeClass(goal.status)">
          {{ goal.status }}
        </span>
        <span class="priority" :title="`Priority ${goal.priority}`">#{{ goal.priority }}</span>
      </div>
      <div class="goal-row__meta">{{ goal.goalType }} · còn lại {{ goal.remaining }} {{ goal.currency }}</div>
      <div class="goal-row__actions">
        <button type="button" class="btn btn--sm" @click="$emit('edit', goal)">Sửa</button>
        <button type="button" class="btn btn--sm" @click="$emit('capacity', goal)">Capacity</button>
        <button type="button" class="btn btn--sm btn--danger" @click="$emit('remove', goal)">Lưu trữ</button>
      </div>
    </li>
  </ul>
  <p v-else class="empty" data-testid="goal-empty">Chưa có mục tiêu nào. Thêm mục tiêu đầu tiên của bạn.</p>
</template>

<script setup lang="ts">
import type { GoalView } from '../../api/goals'

defineProps<{ goals: GoalView[] }>()
defineEmits<{ (e: 'edit', goal: GoalView): void; (e: 'remove', goal: GoalView): void; (e: 'capacity', goal: GoalView): void }>()

function badgeClass(status: string): string {
  if (status === 'COMPLETED') return 'badge--done'
  return 'badge--active'
}
</script>

<style scoped>
.goal-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
.goal-row { padding: 12px 14px; background: var(--fg-surface); border: 1px solid var(--fg-info-border); border-radius: var(--fg-radius-card); display: grid; gap: 6px; }
.goal-row__head { display: flex; align-items: center; gap: 8px; }
.badge { font-size: 11px; padding: 2px 8px; border-radius: 999px; background: var(--fg-info-bg); }
.badge--done { background: #dcfce7; color: #166534; }
.badge--active { background: #dbeafe; color: #1d4ed8; }
.priority { margin-left: auto; font-size: 12px; color: var(--fg-muted); }
.goal-row__meta { font-size: 13px; color: var(--fg-muted); }
.goal-row__actions { display: flex; gap: 8px; }
.empty { color: var(--fg-muted); }
</style>
