<template>
  <AuthLayout title="Sign in" subtitle="Sign in to keep tracking your financial GPS.">
    <form class="auth-fields" novalidate @submit.prevent="onSubmit">
      <div class="field">
        <label for="email">Email</label>
        <input
          id="email"
          v-model="email"
          type="email"
          class="input"
          placeholder="you@example.com"
          autocomplete="email"
          @keyup.enter="onSubmit"
        />
      </div>

      <div class="field">
        <label for="password">Password</label>
        <div class="input-with-action">
          <input
            id="password"
            v-model="password"
            :type="revealed ? 'text' : 'password'"
            class="input"
            placeholder="Your password"
            autocomplete="current-password"
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
      </div>

      <div v-if="error" class="error-box" role="alert">{{ error }}</div>

      <button type="button" class="btn" :disabled="auth.loading" @click="onSubmit">
        {{ auth.loading ? 'Signing in…' : 'Sign in' }}
      </button>
      <p class="auth-link-row">
        New here? <router-link to="/register">Create an account</router-link>
      </p>
    </form>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthLayout from '../components/AuthLayout.vue'
import { problemMessage } from '../api/http'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const route = useRoute()
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
    await auth.login(email.value.trim(), password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/profile'
    router.push(redirect || '/profile')
  } catch (err) {
    error.value = problemMessage(err, 'Could not sign in. Please try again.')
  }
}
</script>