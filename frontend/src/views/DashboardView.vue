<template>
  <div class="page">
    <header class="head">
      <h1 class="page-title">Tổng quan</h1>
      <p class="lead">
        Dòng tiền, vị trí tài chính và bước tiếp theo — mọi con số do server tính. Đã đăng
        nhập với <strong>{{ auth.account?.email }}</strong
        >.
      </p>
    </header>

    <div class="toolbar">
      <router-link to="/profile" class="btn">Mở hồ sơ tài chính</router-link>
      <router-link to="/account" class="btn-ghost">Tài khoản</router-link>
    </div>

    <div v-if="store.profile" class="stat-grid">
      <div class="stat-tile stat-tile--accent">
        <span class="stat-tile__label">Dòng tiền ròng / tháng</span>
        <span class="stat-tile__value">{{ netCash }}</span>
        <span class="stat-tile__hint">Tiền tự do · {{ store.profile.currency }}</span>
      </div>
      <div class="stat-tile">
        <span class="stat-tile__label">Thu nhập</span>
        <span class="stat-tile__value">{{ income }}</span>
        <span class="stat-tile__hint">Thu nhập hàng tháng</span>
      </div>
      <div class="stat-tile">
        <span class="stat-tile__label">Chi tiêu</span>
        <span class="stat-tile__value">{{ expenses }}</span>
        <span class="stat-tile__hint">Chi tiêu hàng tháng</span>
      </div>
      <div class="stat-tile">
        <span class="stat-tile__label">Nợ bắt buộc</span>
        <span class="stat-tile__value">{{ mandatory }}</span>
        <span class="stat-tile__hint">Trả nợ hàng tháng</span>
      </div>
    </div>

    <p v-if="store.loading && !store.profile" class="hint">Đang tải vị trí của bạn…</p>
    <div v-if="store.error" class="error-box">{{ store.error }}</div>

    <PositionSummary :view="store.profile" />

    <section class="journey-nav">
      <router-link to="/profile" class="journey-card">
        <span class="journey-card__icon" aria-hidden="true">
          <svg viewBox="0 0 20 20" fill="none" focusable="false">
            <circle cx="10" cy="6.5" r="3" stroke="currentColor" stroke-width="1.5" />
            <path
              d="M4 16.5c1-3 3.2-4.5 6-4.5s5 1.5 6 4.5"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
            />
          </svg>
        </span>
        <strong>Điểm xuất phát</strong>
        <span>Cập nhật thu · chi · tích lũy</span>
      </router-link>
      <router-link to="/debts" class="journey-card">
        <span class="journey-card__icon" aria-hidden="true">
          <svg viewBox="0 0 20 20" fill="none" focusable="false">
            <path
              d="M10 2.5 12 8l5.5.5-4.2 3.5 1.2 5.5L10 14.5 5.5 17.5l1.2-5.5L2.5 8.5 8 8 10 2.5Z"
              stroke="currentColor"
              stroke-width="1.4"
              stroke-linejoin="round"
            />
          </svg>
        </span>
        <strong>Vượt đèo nợ</strong>
        <span>Xem ngày tự do nợ của bạn</span>
      </router-link>
      <router-link to="/goals" class="journey-card">
        <span class="journey-card__icon" aria-hidden="true">
          <svg viewBox="0 0 20 20" fill="none" focusable="false">
            <path
              d="M4 16.5V10a6 6 0 0 1 12 0v1"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
            />
            <path
              d="M4 16.5h12M14 4.5h4v4"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </span>
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
 * Tổng quan: header chuẩn .head + 4 stat tiles + vị trí server + 3 thẻ hành trình.
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
/* Header và toolbar theo chuẩn chung của debts/goals/profile (.head + .lead). */
.head h1 { margin: 0; }
.head .lead { max-width: 640px; }
.head .lead strong { color: var(--fg-ink); }
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; }
.toolbar .btn, .toolbar .btn-ghost { width: auto; min-width: 170px; }
.toolbar .btn-ghost { background: var(--fg-surface); }
.journey-nav { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.journey-card {
  background: var(--fg-surface); border: 1px solid var(--fg-border-soft); border-radius: var(--fg-radius-card);
  box-shadow: var(--fg-shadow-card); padding: 16px; display: grid; gap: 2px; text-decoration: none;
  transition: transform 0.12s, box-shadow 0.15s, border-color 0.15s;
}
.journey-card:hover { transform: translateY(-2px); border-color: var(--fg-primary-soft); box-shadow: var(--fg-shadow-float); text-decoration: none; }
.journey-card__icon { display: inline-flex; color: var(--fg-muted); }
.journey-card__icon svg { width: 24px; height: 24px; }
.journey-card strong { color: var(--fg-ink); font-size: 15px; }
.journey-card span:last-child { font-size: 13px; color: var(--fg-muted); }
@media (max-width: 860px) { .journey-nav { grid-template-columns: 1fr; } .toolbar .btn, .toolbar .btn-ghost { flex: 1; } }
</style>
