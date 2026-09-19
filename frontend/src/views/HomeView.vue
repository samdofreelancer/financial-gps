<template>
  <div class="page">
    <section class="card hero">
      <p class="eyebrow">Overview</p>
      <h1 class="page-title">Financial GPS</h1>
      <p class="lead">
        Một dashboard tài chính cá nhân đang được xây dựng theo từng feature: profile,
        debt, goals, roadmap và scenario planning.
      </p>
      <div class="cta">
        <router-link to="/login" class="btn">Sign in</router-link>
        <router-link to="/register" class="btn-ghost">Create an account</router-link>
      </div>
      <div class="grid">
        <div class="stat-card">
          <span>Cash flow</span>
          <strong>+$2,540</strong>
        </div>
        <div class="stat-card">
          <span>Debt</span>
          <strong>$18,200</strong>
        </div>
        <div class="stat-card">
          <span>Goal progress</span>
          <strong>68%</strong>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
// Static preview of the dashboard shell; the real totals arrive with each feature.
</script>

<style scoped>
.hero { padding: 28px; }
.cta { margin-top: 20px; display: flex; flex-wrap: wrap; gap: 12px; }
/* The global .btn is full-width inside forms; here the CTAs sit inline. */
.cta .btn { width: auto; min-width: 148px; }
.cta .btn-ghost { min-width: 172px; }
.grid {
  margin-top: 24px;
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}
.stat-card {
  display: flex; flex-direction: column; gap: 6px;
  padding: 18px;
  background: var(--fg-info-bg);
  border: 1px solid var(--fg-info-border);
  border-radius: 12px;
}
.stat-card span { color: var(--fg-muted); font-size: 13px; }
.stat-card strong { font-size: 24px; color: var(--fg-ink); font-variant-numeric: tabular-nums; }
</style>