<template>
  <div class="app-shell">
    <header v-if="auth.isAuthenticated" class="topbar">
      <router-link to="/" class="brand">Financial GPS</router-link>
      <div class="topbar-right">
        <span class="muted">{{ auth.account?.email }}</span>
        <router-link to="/account" class="btn-ghost small">Account</router-link>
        <button type="button" class="btn-ghost small" @click="onLogout">Log out</button>
      </div>
    </header>
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/authStore'

const router = useRouter()
const auth = useAuthStore()

async function onLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style>
:root {
  --primary: #0f8deb;
  --primary-hover: #0b79ce;
  --page-bg: #eef2f6;
  --surface: #ffffff;
  --text: #1e2c3a;
  --muted: #677b8f;
  --border: #d9e2eb;
  --error: #e25454;
  --success: #2aa46a;
  --radius-card: 18px;
  --radius-btn: 12px;
  --radius-input: 10px;
}
* { box-sizing: border-box; }
html, body, #app { height: 100%; }
body {
  margin: 0;
  background: var(--page-bg);
  color: var(--text);
  font-family: 'Roboto', -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
  font-size: 16px;
}
a { color: var(--primary); text-decoration: none; }
a:hover { text-decoration: underline; }

.app-shell { min-height: 100vh; display: flex; flex-direction: column; }
.app-main { flex: 1; min-width: 0; }

.topbar {
  position: sticky; top: 0; z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 1px 0 var(--border);
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 14px 24px;
}
.brand { font-size: 18px; font-weight: 700; color: var(--text); }
.topbar-right { display: flex; align-items: center; gap: 10px; }
.muted { color: var(--muted); }

.auth-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  padding: 40px 20px;
}
.auth-card {
  width: min(100%, 440px);
  background: var(--surface);
  border-radius: 20px;
  box-shadow: 0 18px 44px rgba(14, 61, 94, 0.12);
  border: 1px solid rgba(15, 141, 235, 0.08);
  padding: 32px 28px;
}
.auth-title { margin: 0 0 4px; font-size: 24px; font-weight: 700; }
.auth-subtitle { margin: 0 0 20px; font-size: 14px; color: var(--muted); }
.field { margin-bottom: 14px; }
.field label { display: block; margin-bottom: 6px; font-size: 14px; font-weight: 500; }

.error-box {
  margin: 0 0 12px; padding: 10px 12px; font-size: 14px;
  color: var(--error); background: rgba(226, 84, 84, 0.08);
  border: 1px solid rgba(226, 84, 84, 0.12); border-radius: 10px;
}

.card {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(15, 141, 235, 0.08);
  border-radius: var(--radius-card);
  box-shadow: 0 12px 32px rgba(18, 46, 79, 0.08);
}
.page { max-width: 880px; margin: 0 auto; padding: 28px 20px; }
.page-title { margin: 0 0 4px; font-size: 22px; font-weight: 700; }

.input {
  width: 100%; padding: 12px 14px; font-size: 16px; font-family: inherit; color: var(--text);
  border: 1px solid var(--border); border-radius: var(--radius-input); outline: none;
  transition: border 0.15s, box-shadow 0.15s; min-height: 46px; background: #fff;
}
.input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(0, 143, 211, 0.15); }

.btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  font-family: inherit; font-size: 16px; font-weight: 600; cursor: pointer;
  border: none; border-radius: var(--radius-btn); background: var(--primary); color: #fff;
  padding: 12px 20px; min-height: 46px; width: 100%;
  transition: background 0.15s, transform 0.08s ease;
}
.btn:hover { background: var(--primary-hover); }
.btn:active { transform: translateY(1px); }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-ghost {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  font-family: inherit; font-size: 16px; font-weight: 600; cursor: pointer;
  border: 1px solid var(--border); border-radius: var(--radius-btn);
  background: var(--surface); color: var(--text);
  padding: 12px 20px; min-height: 46px;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.btn-ghost:hover { background: rgba(0, 143, 211, 0.06); border-color: var(--primary); color: var(--primary); }
.btn-ghost.small { padding: 9px 14px; font-size: 14px; min-height: 40px; }
.btn-ghost.danger { color: var(--error); }
.btn-ghost.danger:hover { border-color: var(--error); color: var(--error); background: rgba(226, 84, 84, 0.06); }

.hint { margin: 4px 0 0; font-size: 14px; color: var(--muted); }
.row-between { margin-top: 18px; display: flex; justify-content: space-between; align-items: center; font-size: 14px; gap: 8px; }
</style>
