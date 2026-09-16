<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="auth-title">Welcome back</h1>
      <p class="auth-subtitle">Sign in to keep tracking your financial GPS.</p>

      <div v-if="error" class="error-box">{{ error }}</div>

      <div class="field">
        <label for="email">Email</label>
        <input id="email" v-model="email" type="email" class="input" placeholder="you@example.com" />
      </div>

      <div class="field">
        <label for="password">Password</label>
        <input id="password" v-model="password" type="password" class="input" placeholder="Password" />
      </div>

      <button type="button" class="btn" @click="onSubmit">Sign in</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const auth = useAuthStore()
const email = ref('demo@financialgps.local')
const password = ref('')
const error = ref('')

async function onSubmit() {
  if (!email.value.trim()) {
    error.value = 'Email is required.'
    return
  }

  await auth.login(email.value.trim())
  router.push('/')
}
</script>
