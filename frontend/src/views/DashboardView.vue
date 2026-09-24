<template>
  <div class="page">
    <section class="card hero">
      <p class="eyebrow">Dashboard</p>
      <h1 class="page-title">Welcome back</h1>
      <p class="lead">
        Signed in as <strong>{{ auth.account?.email }}</strong
        >. Every total below is calculated by the server — record your facts to
        see them move.
      </p>
      <div class="cta">
        <router-link to="/profile" class="btn">Open financial profile</router-link>
        <router-link to="/account" class="btn-ghost">Account settings</router-link>
      </div>
    </section>

    <p v-if="store.loading && !store.profile" class="hint">Loading your position…</p>
    <div v-if="store.error" class="error-box">{{ store.error }}</div>

    <PositionSummary :view="store.profile" />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import PositionSummary from '../components/PositionSummary.vue'
import { useAuthStore } from '../stores/authStore'
import { useProfileStore } from '../stores/profileStore'

/**
 * The signed-in home screen: the server-calculated position plus the two
 * working areas. Totals are never computed here — the profile store mirrors
 * GET /api/v1/profile, and PositionSummary renders it verbatim.
 */
const auth = useAuthStore()
const store = useProfileStore()

onMounted(() => {
  // One shared fetch: the profile store is the cache across dashboard/profile.
  if (!store.profile) {
    void store.refresh()
  }
})
</script>

<style scoped>
/* Page rhythm only: colours, cards, fields and buttons come from the global tokens. */
.page { display: flex; flex-direction: column; gap: 16px; }
/* MISA's "Tổng số dư" banner: brand gradient, white text, white primary CTA. */
.hero { padding: 28px; background: var(--fg-gradient-brand); border: 0; color: #fff; }
.hero .eyebrow { color: rgba(255, 255, 255, 0.9); }
.hero .page-title { color: #fff; }
.hero .lead { color: rgba(255, 255, 255, 0.92); }
.hero .lead strong { color: #fff; }
.cta { margin-top: 20px; display: flex; flex-wrap: wrap; gap: 12px; }
/* The global .btn is full-width inside forms; here the CTAs sit inline. */
.cta .btn { width: auto; min-width: 190px; background: #fff; color: var(--fg-primary); }
.cta .btn:hover { background: #f0f9ff; }
.cta .btn-ghost {
  min-width: 170px; background: transparent;
  border-color: rgba(255, 255, 255, 0.55); color: #fff;
}
.cta .btn-ghost:hover { background: rgba(255, 255, 255, 0.12); border-color: #fff; color: #fff; }
@media (max-width: 640px) { .page { padding: 20px 14px; } }
</style>
