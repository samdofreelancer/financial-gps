<template>
  <div class="app-shell">
    <!--
      MISA Money Keeper shell (signed in): a full-width white top bar carrying
      the brand lockup (blue product name), the greeting and the identity chip
      on the right; below it the light-blue sidebar rail and the content column.
    -->
    <div v-if="auth.isAuthenticated" class="app-frame">
      <header class="topbar">
        <div class="topbar-left">
          <router-link to="/" class="brand" aria-label="Financial GPS home">
            <BrandLockup size="sm" />
          </router-link>
          <span class="topbar-divider" aria-hidden="true"></span>
          <p class="topbar-greeting">
            Xin chào <strong>{{ auth.account?.email }}</strong> 👋
          </p>
        </div>
        <div class="topbar-right">
          <span class="topbar-bell" aria-hidden="true">
            <svg viewBox="0 0 20 20" fill="none" focusable="false">
              <path
                d="M10 2.6a4.6 4.6 0 0 0-4.6 4.6c0 3.4-1.2 4.6-1.9 5.4h13c-.7-.8-1.9-2-1.9-5.4A4.6 4.6 0 0 0 10 2.6ZM8.3 15.2a1.8 1.8 0 0 0 3.4 0"
                stroke="currentColor"
                stroke-width="1.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </span>
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
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AvatarMenu from './components/AvatarMenu.vue'
import BrandLockup from './components/BrandLockup.vue'
import SidebarNav from './components/SidebarNav.vue'
import { useAuthStore } from './stores/authStore'

const router = useRouter()
const auth = useAuthStore()

// Prove the session on startup: GET /api/v1/account/me decides the shell.
onMounted(() => {
  if (!auth.ready) {
    void auth.restore()
  }
})

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
/* Signed-in frame: white top bar, then sidebar rail + light-blue content. */
.app-frame { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.topbar {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  min-height: 64px; padding: 10px 20px 10px 16px;
  background: var(--fg-surface);
  border-bottom: 1px solid var(--fg-border-soft);
  /* MISA's thin brand-blue strip along the very top of the window. */
  border-top: 4px solid var(--fg-primary);
}
.topbar-left { display: flex; align-items: center; gap: 16px; min-width: 0; }
.topbar-divider { width: 1px; height: 28px; flex: 0 0 1px; background: var(--fg-border-soft); }
.topbar-greeting {
  margin: 0; font-size: 15px; color: var(--fg-text);
  min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.topbar-greeting strong { color: var(--fg-ink); }
/* MISA paints the product name in its brand blue inside the top bar. */
.topbar :deep(.brand-lockup__name) { color: var(--fg-primary); }
.topbar-right { display: flex; align-items: center; gap: 14px; }
.topbar-bell { display: inline-flex; align-items: center; justify-content: center; width: 34px; height: 34px; color: var(--fg-muted); }
.topbar-bell svg { width: 22px; height: 22px; }
.app-columns { flex: 1; display: flex; align-items: stretch; min-height: 0; background: var(--fg-content-bg); }
</style>
