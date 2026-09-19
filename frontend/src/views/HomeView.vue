<template>
  <div class="page">
    <section class="card" style="padding: 32px;">
      <p class="eyebrow">Overview</p>
      <h1 class="page-title">Financial GPS</h1>
      <p class="lead">
        Một dashboard tài chính cá nhân đang được xây dựng theo từng feature: profile,
        debt, goals, roadmap và scenario planning.
      </p>
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

<style scoped>
.page { max-width: 980px; margin: 0 auto; padding: 28px 20px; }
.card {
  background: rgba(255,255,255,0.95);
  border: 1px solid rgba(15, 141, 235, 0.08);
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(18, 46, 79, 0.08);
}
.eyebrow {
  margin: 0 0 10px;
  color: #0f8deb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.page-title { margin: 0; font-size: 32px; }
.lead { margin: 14px 0 0; color: #677b8f; line-height: 1.6; }
.grid {
  margin-top: 24px;
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}
.stat-card {
  background: #f7fafe;
  border: 1px solid #dfeaf5;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.stat-card span { color: #677b8f; font-size: 14px; }
.stat-card strong { font-size: 28px; }
</style>
