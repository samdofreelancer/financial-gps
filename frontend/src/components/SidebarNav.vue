<template>
  <nav class="sidebar" :class="{ 'sidebar--collapsed': collapsed }" aria-label="Main navigation">
    <!--
      MISA's primary action ("Thêm ghi chép") lives above the menu; here the
      equivalent is updating the financial facts, so it aims at /profile.
    -->
    <router-link to="/profile" class="sidebar__cta">
      <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
        <path d="M8 3.2v9.6M3.2 8h9.6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
      </svg>
      <span class="sidebar__cta-text">Cập nhật profile</span>
    </router-link>

    <ul class="sidebar__menu">
      <li>
        <router-link
          class="sidebar__item"
          :class="{ 'sidebar__item--active': route.name === 'dashboard' }"
          :to="{ name: 'dashboard' }"
        >
          <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
            <rect x="2" y="2" width="5" height="5" rx="1.2" stroke="currentColor" stroke-width="1.4" />
            <rect x="9" y="2" width="5" height="5" rx="1.2" stroke="currentColor" stroke-width="1.4" />
            <rect x="2" y="9" width="5" height="5" rx="1.2" stroke="currentColor" stroke-width="1.4" />
            <rect x="9" y="9" width="5" height="5" rx="1.2" stroke="currentColor" stroke-width="1.4" />
          </svg>
          <span class="sidebar__label">Dashboard</span>
        </router-link>
      </li>
      <li>
        <router-link
          class="sidebar__item"
          :class="{ 'sidebar__item--active': route.name === 'profile' }"
          :to="{ name: 'profile' }"
        >
          <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
            <path
              d="M3 4h10M3 8h10M3 12h6"
              stroke="currentColor"
              stroke-width="1.4"
              stroke-linecap="round"
            />
          </svg>
          <span class="sidebar__label">Financial profile</span>
        </router-link>
      </li>
    </ul>

    <div class="sidebar__foot">
      <button
        type="button"
        class="sidebar__item sidebar__collapse"
        :aria-pressed="collapsed"
        :aria-label="collapsed ? 'Expand the menu' : 'Collapse the menu'"
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
import { ref } from 'vue'
import { useRoute } from 'vue-router'

/**
 * MISA Money Keeper-style left sidebar: light blue rail, white active pill
 * with the blue accent bar, primary CTA on top and the "Thu gọn" collapse
 * toggle at the bottom. Account and Log out live in the topbar identity chip
 * menu (AvatarMenu) instead — click the avatar to find them.
 */
const route = useRoute()
const collapsed = ref(false)
</script>

<style scoped>
/* MISA rail: light blue background, content-side divider, icon+label rows. */
.sidebar {
  display: flex; flex-direction: column;
  width: 232px; flex: 0 0 232px;
  min-height: 100%;
  padding: 16px 12px;
  background: var(--fg-sidebar-bg);
  border-right: 1px solid var(--fg-info-border);
  transition: width 0.18s ease, flex-basis 0.18s ease, padding 0.18s ease;
}
.sidebar--collapsed { width: 68px; flex-basis: 68px; padding: 16px 8px; }
.sidebar--collapsed .sidebar__label,
.sidebar--collapsed .sidebar__cta-text { display: none; }

/* Primary action — MISA's full-width blue "Thêm ghi chép" button. */
.sidebar__cta {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  min-height: 42px; margin-bottom: 16px; padding: 9px 12px;
  font-family: inherit; font-size: 14px; font-weight: 600;
  color: #fff; background: var(--fg-primary);
  border: 0; border-radius: var(--fg-radius-control);
  text-decoration: none; white-space: nowrap;
  transition: background 0.15s;
}
.sidebar__cta:hover { background: var(--fg-primary-hover); text-decoration: none; }
.sidebar__cta:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); }
.sidebar__cta svg { width: 18px; height: 18px; flex: 0 0 18px; }

.sidebar__menu { list-style: none; margin: 0; padding: 0; display: grid; gap: 2px; }
.sidebar__foot {
  margin-top: auto; display: grid; gap: 2px;
  border-top: 1px solid var(--fg-info-border); padding-top: 10px;
}

.sidebar__item {
  position: relative;
  display: flex; align-items: center; gap: 10px;
  width: 100%; min-height: 42px; padding: 9px 12px;
  font-family: inherit; font-size: 14px; font-weight: 500; text-align: left;
  color: var(--fg-label); background: none;
  border: 0; border-radius: var(--fg-radius-control);
  cursor: pointer; text-decoration: none; white-space: nowrap;
  transition: background 0.15s, color 0.15s;
}
.sidebar__item:hover { background: rgba(255, 255, 255, 0.85); color: var(--fg-primary); text-decoration: none; }
.sidebar__item:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); }

/* Active row — MISA paints a white pill, blue text and the left accent bar. */
.sidebar__item--active { background: var(--fg-surface); color: var(--fg-primary); font-weight: 600; }
.sidebar__item--active::before {
  content: '';
  position: absolute; left: -12px; top: 7px; bottom: 7px;
  width: 3px; border-radius: 0 3px 3px 0;
  background: var(--fg-primary);
}
.sidebar--collapsed .sidebar__item--active::before { display: none; }

.sidebar__item--danger { color: var(--fg-danger); }
.sidebar__item--danger:hover { background: var(--fg-danger-bg); color: var(--fg-danger); }
.sidebar__item svg { width: 20px; height: 20px; flex: 0 0 20px; opacity: 0.85; }
.sidebar__chevron--flip { transform: rotate(180deg); }
.sidebar__collapse { color: var(--fg-muted); }

@media (max-width: 900px) {
  .sidebar { width: 68px; flex-basis: 68px; padding: 16px 8px; }
  .sidebar__label,
  .sidebar__cta-text { display: none; }
  .sidebar__item--active::before { display: none; }
}
</style>

