<template>
  <span class="brand-lockup" :class="{ sm: size === 'sm' }">
    <span class="brand-lockup__mark" aria-hidden="true">
      <svg viewBox="0 0 24 24" focusable="false">
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
    </span>
    <span class="brand-lockup__name">Financial GPS</span>
  </span>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ size?: 'md' | 'sm' }>(), { size: 'md' })
</script>