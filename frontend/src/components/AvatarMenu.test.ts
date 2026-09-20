import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '../api/auth'
import { useAuthStore } from '../stores/authStore'
import AvatarMenu from './AvatarMenu.vue'

vi.mock('../api/auth', () => ({
  register: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  me: vi.fn(),
}))

function testRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/dashboard', name: 'dashboard', component: { template: '<div />' } },
      { path: '/account', name: 'account', component: { template: '<div />' } },
      { path: '/login', name: 'login', component: { template: '<div />' } },
    ],
  })
}

let mounted: VueWrapper | null = null

async function mountMenu() {
  const router = testRouter()
  await router.push('/dashboard')
  await router.isReady()
  const pinia = createPinia()
  setActivePinia(pinia)
  useAuthStore().account = { id: '1', email: 'a@example.com' }
  // Attached to the document so focus() and the document-level click listener
  // behave like the real shell; afterEach unmounts (removing the listener).
  const wrapper = mount(AvatarMenu, { attachTo: document.body, global: { plugins: [pinia, router] } })
  mounted = wrapper
  await flushPromises()
  return { wrapper, router }
}

async function openMenu(wrapper: VueWrapper) {
  await wrapper.find('.avatar-menu__trigger').trigger('click')
  return wrapper.find('[role="menu"]')
}

/** Clicking the avatar is the way to reach Account and Log out. */
describe('AvatarMenu (identity chip menu)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    mounted?.unmount()
    mounted = null
  })

  it('starts closed and shows only the avatar chip', async () => {
    const { wrapper } = await mountMenu()
    expect(wrapper.find('.avatar-menu__trigger').text()).toBe('A')
    expect(wrapper.find('[role="menu"]').exists()).toBe(false)
  })

  it('opens a menu with the email, Account and Log out', async () => {
    const { wrapper } = await mountMenu()
    const menu = await openMenu(wrapper)
    expect(menu.exists()).toBe(true)
    expect(menu.text()).toContain('a@example.com')
    const items = menu.findAll('.avatar-menu__item').map((item) => item.text())
    expect(items).toHaveLength(2)
    expect(items[0]).toContain('Account')
    expect(items[1]).toContain('Log out')
  })

  it('navigates to /account and closes when Account is clicked', async () => {
    const { wrapper, router } = await mountMenu()
    await openMenu(wrapper)
    const accountItem = wrapper.findAll('[role="menuitem"]')[0]
    await accountItem.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/account')
    expect(wrapper.find('[role="menu"]').exists()).toBe(false)
  })

  it('emits logout instead of calling the API itself (App owns the call)', async () => {
    const { wrapper } = await mountMenu()
    await openMenu(wrapper)
    const logoutItem = wrapper
      .findAll('[role="menuitem"]')
      .find((item) => item.classes().includes('avatar-menu__item--danger'))
    expect(logoutItem).toBeDefined()
    await logoutItem!.trigger('click')
    expect(wrapper.emitted('logout')).toHaveLength(1)
    expect(authApi.logout).not.toHaveBeenCalled()
    expect(wrapper.find('[role="menu"]').exists()).toBe(false)
  })

  it('closes when a click lands outside the menu', async () => {
    const { wrapper } = await mountMenu()
    await openMenu(wrapper)
    document.body.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await flushPromises()
    expect(wrapper.find('[role="menu"]').exists()).toBe(false)
  })

  it('closes on Escape and hands focus back to the trigger', async () => {
    const { wrapper } = await mountMenu()
    await openMenu(wrapper)
    await wrapper.find('[role="menu"]').trigger('keydown', { key: 'Escape' })
    expect(wrapper.find('[role="menu"]').exists()).toBe(false)
    expect(document.activeElement).toBe(wrapper.find('.avatar-menu__trigger').element)
  })
})
