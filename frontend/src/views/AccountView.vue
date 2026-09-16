<template>
  <div class="page">
    <section class="card" style="padding: 32px;">
      <h1 class="page-title">Account</h1>
      <p class="lead">Authenticated account status</p>

      <div class="info-box">
        <strong>Email:</strong>
        <span>{{ auth.account?.email || 'Not signed in' }}</span>
      </div>

      <button type="button" class="btn-ghost danger" @click="onLogout">Log out</button>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const auth = useAuthStore()

async function onLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.page { max-width: 820px; margin: 0 auto; padding: 28px 20px; }
.card {
  background: rgba(255,255,255,0.95);
  border: 1px solid rgba(15, 141, 235, 0.08);
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(18, 46, 79, 0.08);
}
.page-title { margin: 0; }
.lead { color: #677b8f; margin: 8px 0 22px; }
.info-box {
  display: flex;
  gap: 10px;
  align-items: center;
  background: #f7fafe;
  border: 1px solid #dfeaf5;
  border-radius: 12px;
  padding: 16px 18px;
  margin-bottom: 18px;
}
.btn-ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: inherit;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid #d9e2eb;
  border-radius: 12px;
  background: #fff;
  color: #1e2c3a;
  padding: 10px 16px;
}
.btn-ghost.danger { color: #e25454; }
</style>
