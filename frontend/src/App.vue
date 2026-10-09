<template>
  <div class="app-shell">
    <!--
      GPS shell (signed in): slim white topbar with breadcrumb-style location,
      journey progress hint and identity chip. No masked-email greeting.
    -->
    <div v-if="auth.isAuthenticated" class="app-frame">
      <header class="topbar">
        <div class="topbar-left">
          <router-link to="/" class="brand" aria-label="Financial GPS home">
            <BrandLockup size="sm" />
          </router-link>
          <span class="topbar-divider" aria-hidden="true"></span>
          <nav class="topbar-crumb" aria-label="Vị trí hiện tại">
            <strong class="topbar-crumb__here">{{ pageName }}</strong>
          </nav>
        </div>
        <div class="topbar-right">
          <router-link to="/debts" class="topbar-bell" :aria-label="bellLabel" data-testid="debt-alert-bell">
            <svg viewBox="0 0 20 20" fill="none" focusable="false" aria-hidden="true">
              <path
                d="M10 2.6a4.6 4.6 0 0 0-4.6 4.6c0 3.4-1.2 4.6-1.9 5.4h13c-.7-.8-1.9-2-1.9-5.4A4.6 4.6 0 0 0 10 2.6ZM8.3 15.2a1.8 1.8 0 0 0 3.4 0"
                stroke="currentColor"
                stroke-width="1.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <!-- A blocked projection is an actionable problem, so it must not wait to be noticed. -->
            <span v-if="blockedCount" class="topbar-bell__badge" data-testid="debt-alert-badge">
              {{ blockedCount }}
            </span>
          </router-link>
          <AvatarMenu @logout="onLogout" />
        </div>
      </header>

      <div v-if="auth.unavailable" class="error-box outage" role="alert">
        <span>{{ auth.error }}</span>
        <button type="button" class="btn-ghost small" :disabled="auth.loading" @click="onRetry">
          Retry
        </button>
      </div>

      <div class="app-columns">
        <!-- Account and Log out live in the AvatarMenu; the sidebar is navigation only. -->
        <SidebarNav />
        <main class="app-main">
          <router-view />
        </main>
      </div>
    </div>

    <!-- Anonymous shell: plain top-down landing/auth layout, no sidebar. -->
    <template v-else>
      <div v-if="auth.unavailable" class="error-box outage" role="alert">
        <span>{{ auth.error }}</span>
        <button type="button" class="btn-ghost small" :disabled="auth.loading" @click="onRetry">
          Retry
        </button>
      </div>

      <main class="app-main">
        <router-view />
      </main>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AvatarMenu from './components/AvatarMenu.vue'
import BrandLockup from './components/BrandLockup.vue'
import SidebarNav from './components/SidebarNav.vue'
import { useAuthStore } from './stores/authStore'
import { useDebtStore } from './stores/debtStore'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const debts = useDebtStore()

const PAGE_NAMES: Record<string, string> = {
  dashboard: 'Tổng quan',
  profile: 'Hồ sơ tài chính',
  debts: 'Khoản nợ',
  goals: 'Mục tiêu',
  account: 'Tài khoản',
}
const pageName = computed(() => PAGE_NAMES[String(route.name ?? '')] ?? 'Tổng quan')

// The bell counts debts whose payoff cannot be projected. A blocked debt only clears when the
// user fixes its payment, so surfacing it here is what turns a silent problem into a task.
const blockedCount = computed(() => debts.summary?.blockedDebtCount ?? 0)
const bellLabel = computed(() =>
  blockedCount.value
    ? `${blockedCount.value} khoản nợ cần xử lý, xem trang Quản lý nợ`
    : 'Thông báo',
)

// Prove the session on startup: GET /api/v1/account/me decides the shell.
onMounted(() => {
  if (!auth.ready) {
    void auth.restore()
  }
})

// Keep the badge honest across screens; the debt store is cached, so this costs one request on
// arrival and nothing more while the user works on /debts.
watch(
  () => auth.isAuthenticated,
  (signedIn) => {
    if (signedIn && debts.summary === null) void debts.refresh()
  },
  { immediate: true },
)

// An outage never means "logged out": let the visitor retry the probe.
async function onRetry(): Promise<void> {
  await auth.restore()
}

async function onLogout(): Promise<void> {
  // Real logout: POST /api/v1/auth/logout clears the server session.
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
/* Signed-in frame: slim blurred topbar, then white sidebar + slate content. */
.app-frame { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.topbar {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  min-height: 60px; padding: 8px 20px 8px 16px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--fg-border-soft);
}
.topbar-left { display: flex; align-items: center; gap: 14px; min-width: 0; }
.topbar-divider { width: 1px; height: 24px; flex: 0 0 1px; background: var(--fg-border-soft); }
.topbar-crumb { display: flex; align-items: center; font-size: 14px; min-width: 0; white-space: nowrap; }
.topbar-crumb__here { color: var(--fg-ink); font-weight: 800; font-size: 15px; }
/* Keep brand ink in topbar — no more blue-on-white product-name paint. */
.topbar :deep(.brand-lockup__name) { color: var(--fg-ink); }
.topbar-right { display: flex; align-items: center; gap: 14px; }
.topbar-bell {
  position: relative;
  display: inline-flex; align-items: center; justify-content: center;
  width: 34px; height: 34px;
  color: var(--fg-muted); text-decoration: none; border-radius: 50%;
}
.topbar-bell:hover { color: var(--fg-primary); text-decoration: none; }
.topbar-bell:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); }
.topbar-bell svg { width: 22px; height: 22px; }
.topbar-bell__badge {
  position: absolute; top: 2px; right: 1px;
  min-width: 16px; height: 16px; padding: 0 4px;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 10px; font-weight: 700; line-height: 1;
  color: #fff; background: var(--fg-danger);
  border: 2px solid var(--fg-surface); border-radius: 999px;
}
.app-columns { flex: 1; display: flex; align-items: stretch; min-height: 0; background: var(--fg-content-bg); }
</style>
