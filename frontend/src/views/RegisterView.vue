<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="auth-title">Create your account</h1>
      <p class="auth-subtitle">One account per person. Your data stays yours.</p>

      <div v-if="error" class="error-box">{{ error }}</div>

      <div class="field">
        <label for="register-email">Email</label>
        <input
          id="register-email"
          v-model="email"
          type="email"
          class="input"
          placeholder="you@example.com"
        />
      </div>

      <div class="field">
        <label for="register-password">Password</label>
        <input
          id="register-password"
          v-model="password"
          type="password"
          class="input"
          placeholder="At least 10 characters, with a letter and a digit"
          @keyup.enter="onSubmit"
        />
        <p class="hint">At least 10 characters, with at least one letter and one digit.</p>
      </div>

      <button type="button" class="btn" :disabled="auth.loading" @click="onSubmit">
        Create account
      </button>
      <p class="row-between">
        <span>Already have an account?</span>
        <router-link to="/login">Sign in</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { problemMessage } from '../api/http'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
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
    await auth.register(email.value.trim(), password.value)
    router.push('/profile')
  } catch (err) {
    error.value = problemMessage(err, 'Could not create the account. Please try again.')
  }
}
</script>
