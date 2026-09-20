<template>
  <div ref="root" class="avatar-menu" @keydown.escape="onEscape">
    <button
      ref="trigger"
      type="button"
      class="avatar-menu__trigger"
      aria-haspopup="menu"
      :aria-expanded="open"
      :aria-label="`Account menu for ${auth.account?.email ?? 'the signed-in user'}`"
      @click="toggle"
    >
      <span class="avatar-menu__chip" aria-hidden="true">{{ initial }}</span>
    </button>

    <!--
      The identity chip menu: Account moved here from the sidebar, and Log out
      lives right under it — click the avatar, log out from there.
    -->
    <div v-if="open" class="avatar-menu__panel" role="menu" aria-label="Account menu">
      <div class="avatar-menu__head">
        <span class="avatar-menu__chip avatar-menu__chip--lg" aria-hidden="true">{{ initial }}</span>
        <span class="avatar-menu__id">
          <strong>{{ auth.account?.email }}</strong>
          <small>Signed in</small>
        </span>
      </div>

      <router-link class="avatar-menu__item" role="menuitem" :to="{ name: 'account' }" @click="close">
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
          <circle cx="8" cy="5.4" r="2.6" stroke="currentColor" stroke-width="1.4" />
          <path
            d="M2.8 13.4c.9-2.3 2.9-3.4 5.2-3.4s4.3 1.1 5.2 3.4"
            stroke="currentColor"
            stroke-width="1.4"
            stroke-linecap="round"
          />
        </svg>
        Account
      </router-link>

      <div class="avatar-menu__divider" role="separator"></div>

      <button
        type="button"
        class="avatar-menu__item avatar-menu__item--danger"
        role="menuitem"
        @click="onLogout"
      >
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false">
          <path
            d="M6.5 2.5H3.6A1.1 1.1 0 0 0 2.5 3.6v8.8a1.1 1.1 0 0 0 1.1 1.1h2.9M10.5 5l3 3-3 3M13 8H6"
            stroke="currentColor"
            stroke-width="1.4"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        Log out
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

/**
 * The topbar identity chip: clicking the avatar opens the account menu with
 * Account (moved here from the sidebar) and Log out right under it. This
 * component only emits `logout` — App.vue still owns the server call.
 */
const emit = defineEmits<{ (event: 'logout'): void }>()

const auth = useAuthStore()
const route = useRoute()

const root = ref<HTMLElement | null>(null)
const trigger = ref<HTMLElement | null>(null)
const open = ref(false)

const initial = computed(() => (auth.account?.email ?? '?').charAt(0).toUpperCase())

function toggle(): void {
  open.value = !open.value
}

function close(): void {
  open.value = false
}

function onEscape(): void {
  if (!open.value) return
  close()
  trigger.value?.focus()
}

function onLogout(): void {
  close()
  emit('logout')
}

function onDocumentClick(event: MouseEvent): void {
  if (open.value && root.value && event.target instanceof Node && !root.value.contains(event.target)) {
    close()
  }
}

// Any navigation (menu item picked, guard redirect) closes the panel.
watch(
  () => route.fullPath,
  () => close(),
)

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
})
</script>

<style scoped>
.avatar-menu { position: relative; }
.avatar-menu__trigger {
  display: inline-flex; align-items: center; justify-content: center;
  width: 36px; height: 36px; padding: 0;
  background: none; border: 0; border-radius: 50%;
  cursor: pointer;
}
.avatar-menu__trigger:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); }
.avatar-menu__chip {
  display: inline-flex; align-items: center; justify-content: center;
  width: 36px; height: 36px; border-radius: 50%;
  font-size: 14px; font-weight: 700; color: #fff;
  background: var(--fg-gradient-brand);
}
.avatar-menu__chip--lg { width: 34px; height: 34px; }

.avatar-menu__panel {
  position: absolute; top: calc(100% + 10px); right: 0; z-index: 30;
  width: 248px; padding: 6px;
  background: var(--fg-surface);
  border: 1px solid var(--fg-border-soft); border-radius: var(--fg-radius-control);
  box-shadow: var(--fg-shadow-float);
  animation: avatar-menu-pop 0.12s ease-out;
}
@keyframes avatar-menu-pop {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
.avatar-menu__head {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 6px; padding: 8px 10px 10px;
  border-bottom: 1px solid var(--fg-border-soft);
}
.avatar-menu__id { min-width: 0; }
.avatar-menu__id strong {
  display: block; font-size: 13px; color: var(--fg-ink);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.avatar-menu__id small { font-size: 12px; color: var(--fg-muted); }
.avatar-menu__item {
  display: flex; align-items: center; gap: 10px;
  width: 100%; min-height: 34px; padding: 7px 10px;
  font-family: inherit; font-size: 13px; font-weight: 500; text-align: left;
  color: var(--fg-label); background: none;
  border: 0; border-radius: 6px;
  cursor: pointer; text-decoration: none;
  transition: background 0.15s, color 0.15s;
}
.avatar-menu__item:hover { background: rgba(0, 143, 211, 0.08); color: var(--fg-primary); text-decoration: none; }
.avatar-menu__item:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); }
.avatar-menu__item--danger { color: var(--fg-danger); }
.avatar-menu__item--danger:hover { background: var(--fg-danger-bg); color: var(--fg-danger); }
.avatar-menu__item svg { width: 16px; height: 16px; flex: 0 0 16px; opacity: 0.75; }
.avatar-menu__divider { height: 1px; margin: 6px 4px; background: var(--fg-border-soft); }
</style>
