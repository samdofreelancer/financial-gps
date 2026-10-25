<template>
  <div class="auth-page">
    <!--
      Same frame as the signed-in shell: the white topbar with the brand on the left,
      a quiet way back home on the right, and one white card floating on the slate
      canvas. The brand gradient shows up only as the card's top accent, the way the
      sidebar CTA and the accent stat tile use it — no second logo, no blue wall.
    -->
    <header class="auth-bar">
      <router-link to="/" class="auth-bar__brand" aria-label="Financial GPS home">
        <BrandLockup size="sm" />
      </router-link>
      <router-link to="/" class="auth-bar__home">
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
          <path
            d="M9.5 3.5 5 8l4.5 4.5"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        Về trang chủ
      </router-link>
    </header>

    <main class="auth-main">
      <section class="auth-card">
        <header class="auth-form__head">
          <p class="auth-eyebrow">Bản đồ tài chính cá nhân</p>
          <h1 class="auth-form__title">{{ title }}</h1>
          <p v-if="subtitle" class="auth-form__subtitle">{{ subtitle }}</p>
        </header>

        <div class="auth-form__body">
          <slot />
        </div>

        <p class="auth-note">
          <svg viewBox="0 0 20 20" fill="none" aria-hidden="true" focusable="false">
            <rect
              x="4.2"
              y="8.6"
              width="11.6"
              height="8"
              rx="2"
              stroke="currentColor"
              stroke-width="1.4"
            />
            <path
              d="M7 8.6V6.4a3 3 0 0 1 6 0v2.2"
              stroke="currentColor"
              stroke-width="1.4"
              stroke-linecap="round"
            />
          </svg>
          <span>
            Theo dõi dòng tiền, khoản nợ và mục tiêu trong một không gian riêng tư — dữ liệu
            của bạn không bị chia sẻ cho bên thứ ba.
          </span>
        </p>
      </section>
    </main>

    <footer class="auth-utilities">
      <span>Financial GPS — bản đồ tài chính cá nhân</span>
      <span class="auth-utilities__note">Không quảng cáo, không bán dữ liệu</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import BrandLockup from './BrandLockup.vue'

/**
 * Shared frame for /login and /register. It deliberately reuses the signed-in
 * shell's chrome (topbar, slate canvas, white card, brand gradient accent) so
 * the two screens read as the same product before and after the session starts;
 * only `title`, `subtitle` and the slotted fields change.
 */
defineProps<{ title: string; subtitle?: string }>()
</script>