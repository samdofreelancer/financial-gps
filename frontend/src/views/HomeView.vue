<template>
  <div class="landing">
    <!-- Top bar: brand left, auth actions right (moneykeeper.com/en style). -->
    <header class="landing__bar">
      <router-link to="/" class="landing__brand" aria-label="Financial GPS home">
        <BrandLockup size="sm" />
      </router-link>
      <nav class="landing__actions" aria-label="Account">
        <router-link to="/login" class="landing__link">Log in</router-link>
        <router-link to="/register" class="landing__signup">Sign up</router-link>
      </nav>
    </header>

    <!-- Hero: one big promise, three proof points, two CTAs. -->
    <section class="landing__hero">
      <h1 class="landing__title">A personal finance system that can still surprise you</h1>
      <ul class="landing__points">
        <li>track money, budgets and goals</li>
        <li>see your cash flow and capacity, calculated by the server</li>
        <li>level up your financial literacy</li>
      </ul>
      <div class="landing__cta">
        <router-link to="/register" class="btn landing__try">Try it free</router-link>
        <router-link to="/login" class="btn-ghost landing__demo">Log in</router-link>
      </div>
    </section>

    <!-- What the platform does: four feature tiles, MISA-blue accents. -->
    <section class="landing__section">
      <h2 class="landing__eyebrow">What the platform does</h2>
      <div class="landing__features">
        <article class="landing__feature">
          <h3>Income and expenses</h3>
          <p>Full tracking of every line, with totals the server calculates — never guessed.</p>
        </article>
        <article class="landing__feature">
          <h3>Cash flow control</h3>
          <p>Net cash flow and available capacity in one view, refreshed after every change.</p>
        </article>
        <article class="landing__feature">
          <h3>Goals</h3>
          <p>Name the goals that matter, stay motivated, watch the roadmap take shape.</p>
        </article>
        <article class="landing__feature">
          <h3>Private by design</h3>
          <p>Your data stays yours — no ads, no third-party sharing, ownership-checked access.</p>
        </article>
      </div>
    </section>

    <!-- Why: three promises, same rhythm as the reference site. -->
    <section class="landing__section">
      <h2 class="landing__eyebrow">Why Financial GPS</h2>
      <div class="landing__why">
        <article class="landing__why-card">
          <h3>Full picture of your money</h3>
          <p>Profile, cash flow and capacity on one screen. Watch the flows and measure progress.</p>
        </article>
        <article class="landing__why-card">
          <h3>Focus on what matters</h3>
          <p>Built around the goals you actually care about, not around tedious expense tracking.</p>
        </article>
        <article class="landing__why-card">
          <h3>Always with you</h3>
          <p>A capable interface on any computer, synced to your account over a secure session.</p>
        </article>
      </div>
    </section>

    <!-- Footer: ownership note + legal-ish links, quiet grey. -->
    <footer class="landing__footer">
      <span>Financial GPS — bản đồ tài chính cá nhân</span>
      <span class="landing__footer-note">Không quảng cáo, không bán dữ liệu</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import BrandLockup from '../components/BrandLockup.vue'

/**
 * Public landing page (moneykeeper.com/en rhythm): top bar with auth actions,
 * a hero that sells one promise, feature tiles and a quiet footer. No numbers
 * are invented — the product tour stays qualitative until the user signs in.
 */
</script>

<style scoped>
.landing { min-height: 100vh; display: flex; flex-direction: column; background: var(--fg-app-bg); }

/* Top bar ---------------------------------------------------------- */
.landing__bar {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 12px 28px;
  background: var(--fg-surface);
  border-bottom: 1px solid var(--fg-border-soft);
}
.landing__actions { display: flex; align-items: center; gap: 18px; }
.landing__link { font-size: 14px; font-weight: 600; color: var(--fg-label); }
.landing__link:hover { color: var(--fg-primary); text-decoration: none; }
.landing__signup {
  display: inline-flex; align-items: center;
  min-height: 36px; padding: 6px 16px;
  font-size: 14px; font-weight: 600;
  color: #fff; background: var(--fg-primary);
  border-radius: var(--fg-radius-control);
}
.landing__signup:hover { background: var(--fg-primary-hover); text-decoration: none; }

/* Hero -------------------------------------------------------------- */
.landing__hero {
  max-width: 980px; width: 100%;
  margin: 0 auto; padding: 72px 24px 56px;
  text-align: center;
}
.landing__title {
  margin: 0 auto; max-width: 720px;
  font-size: 34px; font-weight: 800; line-height: 1.2; color: var(--fg-ink);
}
.landing__points {
  margin: 20px auto 0; padding: 0; max-width: 560px;
  list-style: none; display: grid; gap: 8px;
  font-size: 15px; color: var(--fg-muted);
}
.landing__points li::before { content: '— '; color: var(--fg-primary); }
.landing__cta { margin-top: 28px; display: flex; justify-content: center; flex-wrap: wrap; gap: 12px; }
.landing__try { width: auto; min-width: 160px; }
.landing__demo { min-width: 140px; }

/* Feature / why sections -------------------------------------------- */
.landing__section { max-width: 980px; width: 100%; margin: 0 auto; padding: 24px 24px 8px; }
.landing__eyebrow {
  margin: 0 0 16px;
  font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase;
  color: var(--fg-primary);
}
.landing__features {
  display: grid; gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
}
.landing__feature, .landing__why-card {
  padding: 20px;
  background: var(--fg-surface);
  border: 1px solid var(--fg-border-soft);
  border-radius: var(--fg-radius-card);
  box-shadow: var(--fg-shadow-card);
}
.landing__feature h3, .landing__why-card h3 { margin: 0 0 6px; font-size: 15px; }
.landing__feature p, .landing__why-card p { margin: 0; font-size: 13px; line-height: 1.6; color: var(--fg-muted); }
.landing__why { display: grid; gap: 16px; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); margin-bottom: 16px; }

/* Footer ------------------------------------------------------------ */
.landing__footer {
  margin-top: auto;
  display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px;
  padding: 20px 28px;
  border-top: 1px solid var(--fg-border-soft);
  font-size: 13px; color: var(--fg-footer);
}
.landing__footer-note { color: var(--fg-muted); }

@media (max-width: 640px) {
  .landing__bar { padding: 10px 16px; }
  .landing__hero { padding: 48px 16px 36px; }
  .landing__title { font-size: 26px; }
  .landing__footer { padding: 16px; }
}
</style>
