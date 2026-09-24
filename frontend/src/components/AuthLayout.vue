<template>
  <div class="auth-page">
    <!--
      MISA Money Keeper sign-in shell: a 770x521 white card, radius 16, floating shadow,
      a gradient brand panel on the left and the form column (42px side padding) on the
      right — geometry extracted from moneykeeperapp.misa.vn/account/signin.
    -->
    <section class="auth-card">
      <aside class="auth-brand">
        <svg
          class="auth-brand__art"
          viewBox="0 0 385 521"
          preserveAspectRatio="xMidYMid slice"
          aria-hidden="true"
          focusable="false"
        >
          <defs>
            <linearGradient id="fgArtCard" x1="0" y1="0" x2="0.2" y2="1">
              <stop offset="0" stop-color="#ffffff" stop-opacity="0.95" />
              <stop offset="1" stop-color="#eef8ff" stop-opacity="0.88" />
            </linearGradient>
          </defs>

          <!-- ambient light spots -->
          <circle cx="340" cy="60" r="130" fill="#ffffff" opacity="0.07" />
          <circle cx="20" cy="500" r="150" fill="#ffffff" opacity="0.08" />
          <circle cx="215" cy="250" r="150" fill="#7cd5fd" opacity="0.07" />

          <!-- radar rings: the "GPS" idea, bottom right -->
          <g fill="none" stroke="#ffffff" stroke-opacity="0.16">
            <circle cx="352" cy="462" r="46" />
            <circle cx="352" cy="462" r="76" stroke-dasharray="3 9" />
          </g>

          <!-- the route the user walks towards the goal -->
          <path
            d="M-10 470 C 70 452 92 392 150 372 C 220 348 286 344 306 258"
            fill="none"
            stroke="#ffffff"
            stroke-opacity="0.5"
            stroke-width="2"
            stroke-linecap="round"
            stroke-dasharray="2 10"
          />

          <!-- destination pin -->
          <g transform="translate(306 250)">
            <circle r="26" fill="#ffffff" opacity="0.16" />
            <path d="M0 13c0 0 11-9 11-18a11 11 0 0 0-22 0c0 9 11 18 11 18Z" fill="#ffffff" />
            <circle cy="-5" r="4" fill="#0084e0" />
          </g>

          <!-- project balance card -->
          <g transform="translate(46 292)">
            <rect width="204" height="120" rx="16" fill="url(#fgArtCard)" />
            <rect x="20" y="18" width="58" height="8" rx="4" fill="#0057db" opacity="0.22" />
            <rect x="20" y="36" width="104" height="13" rx="6" fill="#0057db" opacity="0.82" />
            <path
              d="M20 104 L48 78 L76 88 L104 62 L132 72 L164 44"
              fill="none"
              stroke="#0db3f3"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
              opacity="0.8"
            />
          </g>

          <!-- goal chip -->
          <g transform="translate(58 440)">
            <rect width="212" height="56" rx="14" fill="#ffffff" opacity="0.18" />
            <circle cx="28" cy="28" r="13" fill="#ffffff" opacity="0.92" />
            <path
              d="M22 28.5 L26.5 33 L34 24.5"
              fill="none"
              stroke="#0090d8"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <rect x="52" y="18" width="84" height="7" rx="3.5" fill="#ffffff" opacity="0.75" />
            <rect x="52" y="31" width="54" height="7" rx="3.5" fill="#ffffff" opacity="0.4" />
            <circle cx="192" cy="28" r="9" fill="#7cd5fd" opacity="0.85" />
          </g>
        </svg>

        <div class="auth-brand__head">
          <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path
              d="M12 21.5s7-6.1 7-11.5a7 7 0 1 0-14 0c0 5.4 7 11.5 7 11.5Z"
              fill="#ffffff"
              fill-opacity="0.22"
              stroke="#ffffff"
              stroke-width="1.4"
            />
            <path
              d="M9.2 10.4l2 2 3.6-3.8"
              fill="none"
              stroke="#ffffff"
              stroke-width="1.6"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <span class="auth-brand__name">Financial GPS</span>
        </div>
        <p class="auth-brand__tagline">Bản đồ tài chính cá nhân</p>
        <p class="auth-brand__text">
          Theo dõi dòng tiền, khoản nợ và mục tiêu trong một không gian riêng tư — dữ liệu của bạn
          không bị chia sẻ cho bên thứ ba.
        </p>
      </aside>
      <div class="auth-form">
        <header class="auth-form__head">
          <BrandLockup />
          <h1 class="auth-form__title">{{ title }}</h1>
          <p v-if="subtitle" class="auth-form__subtitle">{{ subtitle }}</p>
        </header>

        <div class="auth-form__body">
          <slot />
        </div>
      </div>
    </section>

    <p class="auth-utilities">
      <span>Financial GPS</span>
      <span class="auth-utilities__divider" aria-hidden="true"></span>
      <span>Không quảng cáo, không bán dữ liệu</span>
    </p>
  </div>
</template>

<script setup lang="ts">
import BrandLockup from './BrandLockup.vue'

/**
 * Shared frame for /login and /register so both screens stay identical to the MISA
 * sign-in reference: only `title`, `subtitle` and the slotted fields change.
 */
defineProps<{ title: string; subtitle?: string }>()
</script>