<template>
  <div class="page">
    <section class="card account-card">
      <h1 class="page-title">Account</h1>
      <p class="lead">Authenticated account status</p>

      <div class="info-box">
        <strong>Email:</strong>
        <span>{{ auth.account?.email || 'Not signed in' }}</span>
      </div>

      <div v-if="error" class="error-box" role="alert">{{ error }}</div>
      <button type="button" class="btn-ghost danger" :disabled="auth.loading" @click="onLogout">
        Log out
      </button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const auth = useAuthStore()
const error = ref('')

async function onLogout(): Promise<void> {
  error.value = ''
  // Real logout: POST /api/v1/auth/logout destroys the server session.
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.account-card { max-width: 560px; padding: 28px; }
</style>