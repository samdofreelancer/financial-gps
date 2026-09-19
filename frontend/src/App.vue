<template>
  <div class="app-shell">
    <header v-if="auth.isAuthenticated" class="topbar">
      <router-link to="/" class="brand" aria-label="Financial GPS home">
        <BrandLockup size="sm" />
      </router-link>
      <div class="topbar-right">
        <span class="muted">{{ auth.account?.email }}</span>
        <router-link to="/account" class="btn-ghost small">Account</router-link>
        <button type="button" class="btn-ghost small danger" @click="onLogout">Log out</button>
      </div>
    </header>

    <div v-if="auth.unavailable" class="error-box outage" role="alert">
      <span>{{ auth.error }}</span>
      <button type="button" class="btn-ghost small" :disabled="auth.loading" @click="onRetry">
        Retry
      </button>
    </div>

    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BrandLockup from './components/BrandLockup.vue'
import { useAuthStore } from './stores/authStore'

const router = useRouter()
const auth = useAuthStore()

// Prove the session on startup: GET /api/v1/account/me decides the header.
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