<template>
  <nav class="sidebar" :class="{ 'sidebar--collapsed': collapsed }" aria-label="Điều hướng chính">
    <div class="sidebar__brand-note">
      <span class="sidebar__journey-dot" aria-hidden="true"></span>
      <span class="sidebar__label sidebar__journey-text">Hành trình tài chính</span>
    </div>

    <ul class="sidebar__menu">
      <li>
        <p class="sidebar__group sidebar__label">Tổng quan</p>
        <router-link
          class="sidebar__item"
          :class="{ 'sidebar__item--active': route.name === 'dashboard' }"
          :to="{ name: 'dashboard' }"
        >
          <svg viewBox="0 0 20 20" fill="none" aria-hidden="true" focusable="false">
            <path d="M10 17.5 3.5 10 5.8 7.7 10 11.9l4.2-4.2L16.5 10 10 17.5Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
            <circle cx="10" cy="4.5" r="2" stroke="currentColor" stroke-width="1.5"/>
          </svg>
          <span class="sidebar__label">Tổng quan</span>
        </router-link>
      </li>
      <li>
        <p class="sidebar__group sidebar__label">Điểm xuất phát</p>
        <router-link
          class="sidebar__item"
          :class="{ 'sidebar__item--active': route.name === 'profile' }"
          :to="{ name: 'profile' }"
        >
          <svg viewBox="0 0 20 20" fill="none" aria-hidden="true" focusable="false">
            <circle cx="10" cy="6.5" r="3" stroke="currentColor" stroke-width="1.5"/>
            <path d="M4 16.5c1-3 3.2-4.5 6-4.5s5 1.5 6 4.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <span class="sidebar__label">Hồ sơ tài chính</span>
        </router-link>
      </li>
      <li>
        <p class="sidebar__group sidebar__label">Hành trình</p>
        <router-link
          class="sidebar__item"
          :class="{ 'sidebar__item--active': route.name === 'debts' }"
          :to="{ name: 'debts' }"
        >
          <svg viewBox="0 0 20 20" fill="none" aria-hidden="true" focusable="false">
            <path d="M10 2.5 12 8l5.5.5-4.2 3.5 1.2 5.5L10 14.5 5.5 17.5l1.2-5.5L2.5 8.5 8 8 10 2.5Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
          </svg>
          <span class="sidebar__label">Khoản nợ</span>
          <span v-if="blockedCount" class="sidebar__badge">{{ blockedCount }}</span>
        </router-link>
      </li>
      <li>
        <router-link
          class="sidebar__item"
          :class="{ 'sidebar__item--active': route.name === 'goals' }"
          :to="{ name: 'goals' }"
        >
          <svg viewBox="0 0 20 20" fill="none" aria-hidden="true" focusable="false">
            <path d="M4 16.5V10a6 6 0 0 1 12 0v1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M4 16.5h12M14 4.5h4v4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="sidebar__label">Mục tiêu</span>
        </router-link>
      </li>
    </ul>

    <div class="sidebar__foot">
      <router-link :to="cta.to" class="sidebar__cta">
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
          <path d="M8 3.2v9.6M3.2 8h9.6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
        </svg>
        <span class="sidebar__cta-text">{{ cta.label }}</span>
      </router-link>
      <button
        type="button"
        class="sidebar__item sidebar__collapse"
        :aria-pressed="collapsed"
        :aria-label="collapsed ? 'Mở rộng menu' : 'Thu gọn menu'"
        @click="collapsed = !collapsed"
      >
        <svg
          viewBox="0 0 16 16"
          fill="none"
          aria-hidden="true"
          focusable="false"
          :class="{ 'sidebar__chevron--flip': collapsed }"
        >
          <path
            d="M10 3.5 5.5 8l4.5 4.5"
            stroke="currentColor"
            stroke-width="1.4"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <span class="sidebar__label">Thu gọn</span>
      </button>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useDebtStore } from '../stores/debtStore'

/**
 * GPS sidebar: white rail, grouped journey nav (Tổng quan → Xuất phát → Hành trình → Đích),
 * contextual CTA at the bottom. Badge mirrors the topbar debt alert.
 */
const route = useRoute()
const collapsed = ref(false)
const debts = useDebtStore()

const blockedCount = computed(() => debts.summary?.blockedDebtCount ?? 0)

const cta = computed(() => {
  switch (route.name) {
    case 'debts': return { to: '/debts', label: 'Thêm khoản nợ' }
    case 'goals': return { to: '/goals', label: 'Thêm mục tiêu' }
    case 'profile': return { to: '/profile', label: 'Cập nhật hồ sơ' }
    default: return { to: '/profile', label: 'Cập nhật hồ sơ' }
  }
})
</script>

<style scoped>
.sidebar {
  display: flex; flex-direction: column;
  width: 248px; flex: 0 0 248px;
  min-height: 100%;
  padding: 18px 14px;
  background: var(--fg-sidebar-bg);
  border-right: 1px solid var(--fg-border-soft);
  transition: width 0.18s ease, flex-basis 0.18s ease, padding 0.18s ease;
}
.sidebar--collapsed { width: 72px; flex-basis: 72px; padding: 18px 10px; }
.sidebar--collapsed .sidebar__label,
.sidebar--collapsed .sidebar__cta-text,
.sidebar--collapsed .sidebar__brand-note { display: none; }

.sidebar__brand-note { display: flex; align-items: center; gap: 8px; padding: 0 8px 14px; }
.sidebar__journey-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--fg-journey); box-shadow: 0 0 0 4px var(--fg-journey-bg); flex: 0 0 8px; }
.sidebar__journey-text { font-size: 11px; font-weight: 800; letter-spacing: 0.09em; text-transform: uppercase; color: var(--fg-muted); }

.sidebar__menu { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
.sidebar__group { margin: 4px 8px 4px; font-size: 11px; font-weight: 700; letter-spacing: 0.07em; text-transform: uppercase; color: var(--fg-muted-soft); }

.sidebar__foot {
  margin-top: auto; display: grid; gap: 8px;
  border-top: 1px solid var(--fg-border-soft); padding-top: 12px;
}

.sidebar__item {
  position: relative;
  display: flex; align-items: center; gap: 11px;
  width: 100%; min-height: 44px; padding: 10px 12px;
  font-family: inherit; font-size: 14px; font-weight: 600; text-align: left;
  color: var(--fg-text); background: none;
  border: 0; border-radius: 12px;
  cursor: pointer; text-decoration: none; white-space: nowrap;
  transition: background 0.15s, color 0.15s;
}
.sidebar__item:hover { background: var(--fg-info-bg); color: var(--fg-primary-hover); text-decoration: none; }
.sidebar__item:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); }

.sidebar__item--active { background: var(--fg-ink); color: #fff; font-weight: 700; }
.sidebar__item--active:hover { background: var(--fg-ink); color: #fff; }

.sidebar__item svg { width: 20px; height: 20px; flex: 0 0 20px; }
.sidebar__badge {
  margin-left: auto; min-width: 22px; height: 22px; padding: 0 6px;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 800; color: #fff; background: var(--fg-danger); border-radius: 999px;
}
.sidebar--collapsed .sidebar__badge { display: none; }

/* Contextual CTA at the bottom — no longer a confusing top button. */
.sidebar__cta {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  min-height: 44px; padding: 10px 12px;
  font-family: inherit; font-size: 14px; font-weight: 700;
  color: #fff; background: var(--fg-gradient-brand);
  border: 0; border-radius: 12px;
  text-decoration: none; white-space: nowrap;
  box-shadow: 0 6px 16px rgba(2, 132, 199, 0.28);
  transition: filter 0.15s, transform 0.08s;
}
.sidebar__cta:hover { filter: brightness(1.05); text-decoration: none; }
.sidebar__cta:active { transform: translateY(1px); }
.sidebar__cta:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); }
.sidebar__cta svg { width: 18px; height: 18px; flex: 0 0 18px; }

.sidebar__chevron--flip { transform: rotate(180deg); }
.sidebar__collapse { color: var(--fg-muted); }

@media (max-width: 900px) {
  .sidebar { width: 72px; flex-basis: 72px; padding: 18px 10px; }
  .sidebar__label,
  .sidebar__cta-text,
  .sidebar__brand-note { display: none; }
}
</style>
