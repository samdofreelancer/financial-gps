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
        <input
          id="password"
          v-model="password"
          type="password"
          class="input"
          placeholder="Password"
          @keyup.enter="onSubmit"
        />
      </div>

      <button type="button" class="btn" :disabled="auth.loading" @click="onSubmit">Sign in</button>
      <p class="row-between">
        <span>New here?</span>
        <router-link to="/register">Create an account</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { problemMessage } from '../api/http'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const email = ref('')
const password = ref('')
const error = ref('')

async function onSubmit(): Promise<void> {
  error.value = ''
  if (!email.value.trim()) {
    error.value = 'Email is required.'
    return
  }
  if (!password.value) {
    error.value = 'Password is required.'
    return
  }

  try {
    await auth.login(email.value.trim(), password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/profile'
    router.push(redirect || '/profile')
  } catch (err) {
    error.value = problemMessage(err, 'Could not sign in. Please try again.')
  }
}
</script>
