<template>
  <div class="page">
    <header class="page-head">
      <div>
        <p class="journey-eyebrow">Tổng quan · Financial GPS</p>
        <h1 class="page-title">Welcome back</h1>
        <p class="page-head__sub">
          Signed in as <strong>{{ auth.account?.email }}</strong
          >. Mọi con số đều do server tính — cập nhật thực tế để thấy hành trình di chuyển.
        </p>
      </div>
      <div class="cta">
        <router-link to="/profile" class="btn">Open financial profile</router-link>
        <router-link to="/account" class="btn-ghost">Account settings</router-link>
      </div>
    </header>

    <div v-if="store.profile" class="stat-grid">
      <div class="stat-tile stat-tile--accent">
        <span class="stat-tile__label">Dòng tiền ròng / tháng</span>
        <span class="stat-tile__value">{{ netCash }}</span>
        <span class="stat-tile__hint">Free cash · {{ store.profile.currency }}</span>
      </div>
      <div class="stat-tile">
        <span class="stat-tile__label">Thu nhập</span>
        <span class="stat-tile__value">{{ income }}</span>
        <span class="stat-tile__hint">Monthly income</span>
      </div>
      <div class="stat-tile">
        <span class="stat-tile__label">Chi tiêu</span>
        <span class="stat-tile__value">{{ expenses }}</span>
        <span class="stat-tile__hint">Monthly expenses</span>
      </div>
      <div class="stat-tile">
        <span class="stat-tile__label">Nợ bắt buộc</span>
        <span class="stat-tile__value">{{ mandatory }}</span>
        <span class="stat-tile__hint">Debt payments</span>
      </div>
    </div>

    <p v-if="store.loading && !store.profile" class="hint">Loading your position…</p>
    <div v-if="store.error" class="error-box">{{ store.error }}</div>

    <PositionSummary :view="store.profile" />

    <section class="journey-nav">
      <router-link to="/profile" class="journey-card">
        <span class="journey-card__icon">📍</span>
        <strong>Điểm xuất phát</strong>
        <span>Cập nhật thu · chi · tích lũy</span>
      </router-link>
      <router-link to="/debts" class="journey-card">
        <span class="journey-card__icon">⛰️</span>
        <strong>Vượt đèo nợ</strong>
        <span>Xem ngày tự do nợ của bạn</span>
      </router-link>
      <router-link to="/goals" class="journey-card">
        <span class="journey-card__icon">🏁</span>
        <strong>Đích đến</strong>
        <span>Đặt mục tiêu đầu tiên</span>
      </router-link>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import PositionSummary from '../components/PositionSummary.vue'
import { formatMoney } from '../api/profile'
import { useAuthStore } from '../stores/authStore'
import { useProfileStore } from '../stores/profileStore'

/**
 * GPS home: journey header + 4 stat tiles + server position + 3 next-stop cards.
 * Totals are never computed here — tiles only format the store's strings.
 */
const auth = useAuthStore()
const store = useProfileStore()

const income = computed(() =>
  store.profile ? formatMoney(store.profile.totalIncome.amount, '', { compact: true }) : '—',
)
const expenses = computed(() =>
  store.profile ? formatMoney(store.profile.totalExpenses.amount, '', { compact: true }) : '—',
)
const mandatory = computed(() =>
  store.profile ? formatMoney(store.profile.totalMandatoryPayment.amount, '', { compact: true }) : '—',
)
const netCash = computed(() => {
  if (!store.profile) return '—'
  const t = formatMoney(store.profile.netCashFlow.amount, '', { compact: true }).trim()
  if (t.startsWith('-') || t.startsWith('0')) return t
  return `+${t}`
})

onMounted(() => {
  void store.refresh()
})
</script>

<style scoped>
.page-head__sub strong { color: var(--fg-ink); }
.cta { display: flex; flex-wrap: wrap; gap: 10px; }
.cta .btn, .cta .btn-ghost { width: auto; min-width: 170px; }
.cta .btn-ghost { background: var(--fg-surface); }
.journey-nav { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.journey-card {
  background: var(--fg-surface); border: 1px solid var(--fg-border-soft); border-radius: var(--fg-radius-card);
  box-shadow: var(--fg-shadow-card); padding: 16px; display: grid; gap: 2px; text-decoration: none;
  transition: transform 0.12s, box-shadow 0.15s, border-color 0.15s;
}
.journey-card:hover { transform: translateY(-2px); border-color: var(--fg-primary-soft); box-shadow: var(--fg-shadow-float); text-decoration: none; }
.journey-card__icon { font-size: 24px; }
.journey-card strong { color: var(--fg-ink); font-size: 15px; }
.journey-card span:last-child { font-size: 13px; color: var(--fg-muted); }
@media (max-width: 860px) { .journey-nav { grid-template-columns: 1fr; } .cta .btn, .cta .btn-ghost { flex: 1; } }
</style>
