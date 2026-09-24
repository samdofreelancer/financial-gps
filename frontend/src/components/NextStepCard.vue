<template>
  <section class="card next-step" data-testid="next-step-card" aria-labelledby="next-step-title">
    <h2 id="next-step-title">Your next step</h2>
    <p v-if="complete" class="lead">
      Your profile is complete. Keep your income and expenses up to date so your position stays
      accurate.
    </p>
    <p v-else class="lead">
      Complete your financial profile to see your next financial milestone.
    </p>
    <button v-if="!complete" type="button" class="btn-ghost" @click="emit('complete-profile')">
      Complete profile
    </button>
  </section>
</template>

<script setup lang="ts">
/**
 * Deliberately neutral: the current API exposes the position (income/expenses/savings), not goals,
 * debts or milestones, so no recommendation, score or "progress" is invented here. The card only
 * tells the user where to go next.
 */
defineProps<{ complete: boolean }>()
const emit = defineEmits<{ (e: 'complete-profile'): void }>()
</script>

<style scoped>
.next-step { padding: 24px; }
.next-step h2 { margin: 0 0 8px; font-size: 18px; }
.next-step .lead { margin: 0 0 12px; }
.next-step .btn-ghost { min-height: 40px; max-width: 260px; }

@media (max-width: 640px) {
  .next-step { padding: 20px 16px; }
  .next-step .btn-ghost { max-width: none; width: 100%; }
}
</style>
