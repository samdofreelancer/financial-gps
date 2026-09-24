<template>
  <AuthLayout title="Create your account" subtitle="One account per person. Your data stays yours.">
    <form class="auth-fields" novalidate @submit.prevent="onSubmit">
      <div class="field">
        <label for="register-email">Email</label>
        <input
          id="register-email"
          v-model="email"
          type="email"
          class="input"
          placeholder="you@example.com"
          autocomplete="email"
          @keyup.enter="onSubmit"
        />
      </div>

      <div class="field">
        <label for="register-password">Password</label>
        <div class="input-with-action">
          <input
            id="register-password"
            v-model="password"
            :type="revealed ? 'text' : 'password'"
            class="input"
            placeholder="At least 10 characters, with a letter and a digit"
            autocomplete="new-password"
            @keyup.enter="onSubmit"
          />
          <!--
            Field adornment (the MISA eye icon). Rendered as a span with role="button" so the
            only <button> in this view stays the submit control.
          -->
          <span
            class="input-action"
            role="button"
            tabindex="0"
            :aria-label="revealed ? 'Hide password' : 'Show password'"
            :aria-pressed="revealed"
            @click="revealed = !revealed"
            @keydown.enter.prevent="revealed = !revealed"
            @keydown.space.prevent="revealed = !revealed"
          >
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
              <path
                d="M2 12s3.6-6 10-6 10 6 10 6-3.6 6-10 6S2 12 2 12Z"
                stroke="currentColor"
                stroke-width="1.6"
              />
              <circle cx="12" cy="12" r="2.6" stroke="currentColor" stroke-width="1.6" />
              <path
                v-if="!revealed"
                d="M4.5 20L19.5 4"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linecap="round"
              />
            </svg>
          </span>
        </div>
        <p class="hint">At least 10 characters, with at least one letter and one digit.</p>
      </div>

      <div v-if="error" class="error-box" role="alert">{{ error }}</div>

      <button type="button" class="btn" :disabled="auth.loading" @click="onSubmit">
        {{ auth.loading ? 'Creating account…' : 'Create account' }}
      </button>
      <p class="auth-link-row">
        Already have an account? <router-link to="/login">Sign in</router-link>
      </p>
    </form>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AuthLayout from '../components/AuthLayout.vue'
import { problemMessage } from '../api/http'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const auth = useAuthStore()
const email = ref('')
const password = ref('')
const revealed = ref(false)
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
    // A fresh session goes straight to the dashboard (the guestOnly guard agrees).
    router.push('/dashboard')
  } catch (err) {
    error.value = problemMessage(err, 'Could not create the account. Please try again.')
  }
}
</script>