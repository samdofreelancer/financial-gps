import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SidebarNav from '@/components/SidebarNav.vue'

function testRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div />' } },
      { path: '/dashboard', name: 'dashboard', component: { template: '<div />' } },
      { path: '/profile', name: 'profile', component: { template: '<div />' } },
      { path: '/login', name: 'login', component: { template: '<div />' } },
    ],
  })
}

async function mountSidebar(at = '/dashboard') {
  const router = testRouter()
  await router.push(at)
  await router.isReady()
  const wrapper = mount(SidebarNav, { global: { plugins: [createPinia(), router] } })
  await flushPromises()
  return { wrapper, router }
}

/** The MISA-style shell pins navigation to the left, never under the email. */
describe('SidebarNav (left navigation)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('carries the primary CTA, every destination and the collapse control', async () => {
    const { wrapper } = await mountSidebar()
    expect(wrapper.find('.sidebar__cta').text()).toContain('Cập nhật profile')
    const labels = wrapper.findAll('.sidebar__label').map((label) => label.text())
    expect(labels).toEqual(['Dashboard', 'Financial profile', 'Thu gọn'])
  })

  it('marks the destination matching the current route as active', async () => {
    const { wrapper } = await mountSidebar()
    const items = wrapper.findAll('.sidebar__item')
    const dashboardItem = items.find((item) => item.text().includes('Dashboard'))
    expect(dashboardItem?.classes()).toContain('sidebar__item--active')
    const profileItem = items.find((item) => item.text().includes('Financial profile'))
    expect(profileItem?.classes()).not.toContain('sidebar__item--active')
  })

  it('navigates to the profile when its item is clicked', async () => {
    const { wrapper, router } = await mountSidebar()
    const profileItem = wrapper
      .findAll('.sidebar__item')
      .find((item) => item.text().includes('Financial profile'))
    expect(profileItem).toBeDefined()
    await profileItem!.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/profile')
  })

  it('collapses to icon-only width and back (MISA "Thu gọn")', async () => {
    const { wrapper } = await mountSidebar()
    const collapse = wrapper.find('.sidebar__collapse')
    expect(wrapper.find('.sidebar').classes()).not.toContain('sidebar--collapsed')
    await collapse.trigger('click')
    expect(wrapper.find('.sidebar').classes()).toContain('sidebar--collapsed')
    expect(collapse.attributes('aria-pressed')).toBe('true')
    await collapse.trigger('click')
    expect(wrapper.find('.sidebar').classes()).not.toContain('sidebar--collapsed')
  })
})
