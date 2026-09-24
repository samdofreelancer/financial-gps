import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/auth'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'

vi.mock('@/api/auth', () => ({
  register: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  me: vi.fn(),
}))

function router() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/login', component: LoginView },
      { path: '/register', component: RegisterView },
      { path: '/dashboard', component: { template: '<div>dashboard</div>' } },
      { path: '/profile', component: { template: '<div>profile</div>' } },
    ],
  })
}

describe('LoginView (real 007 login)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('submits email+password to POST /login and lands on the dashboard', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ id: '1', email: 'a@example.com' })
    const r = router()
    r.push('/login')
    await r.isReady()
    const wrapper = mount(LoginView, { global: { plugins: [createPinia(), r] } })
    await wrapper.find('#email').setValue('a@example.com')
    await wrapper.find('#password').setValue('correct horse battery1')
    await wrapper.find('button').trigger('click')
    await flushPromises()
    expect(authApi.login).toHaveBeenCalledWith({
      email: 'a@example.com',
      password: 'correct horse battery1',
    })
    expect(r.currentRoute.value.path).toBe('/dashboard')
  })

  it('keeps the ?redirect= deep link after a successful sign-in', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ id: '1', email: 'a@example.com' })
    const r = router()
    r.push({ path: '/login', query: { redirect: '/profile' } })
    await r.isReady()
    const wrapper = mount(LoginView, { global: { plugins: [createPinia(), r] } })
    await wrapper.find('#email').setValue('a@example.com')
    await wrapper.find('#password').setValue('correct horse battery1')
    await wrapper.find('button').trigger('click')
    await flushPromises()
    expect(r.currentRoute.value.path).toBe('/profile')
  })

  it('shows INVALID_CREDENTIALS instead of routing on failure', async () => {
    vi.mocked(authApi.login).mockRejectedValue({
      response: { status: 401, data: { code: 'INVALID_CREDENTIALS' } },
    })
    const r = router()
    r.push('/login')
    await r.isReady()
    const wrapper = mount(LoginView, { global: { plugins: [createPinia(), r] } })
    await wrapper.find('#email').setValue('a@example.com')
    await wrapper.find('#password').setValue('wrong password 99')
    await wrapper.find('button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Email or password is incorrect.')
    expect(r.currentRoute.value.path).toBe('/login')
  })

  it('requires both fields before calling the API', async () => {
    const r = router()
    r.push('/login')
    await r.isReady()
    const wrapper = mount(LoginView, { global: { plugins: [createPinia(), r] } })
    await wrapper.find('button').trigger('click')
    await flushPromises()
    expect(authApi.login).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Email is required.')
  })
})

describe('RegisterView (real 007 register)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('submits to POST /register and lands on the dashboard', async () => {
    vi.mocked(authApi.register).mockResolvedValue({ id: '1', email: 'b@example.com' })
    const r = router()
    r.push('/register')
    await r.isReady()
    const wrapper = mount(RegisterView, { global: { plugins: [createPinia(), r] } })
    await wrapper.find('#register-email').setValue('b@example.com')
    await wrapper.find('#register-password').setValue('correct horse battery1')
    await wrapper.find('button').trigger('click')
    await flushPromises()
    expect(authApi.register).toHaveBeenCalledWith({
      email: 'b@example.com',
      password: 'correct horse battery1',
    })
    expect(r.currentRoute.value.path).toBe('/dashboard')
  })

  it('shows the password policy instead of routing on 422', async () => {
    vi.mocked(authApi.register).mockRejectedValue({
      response: {
        status: 422,
        data: {
          code: 'PASSWORD_POLICY_VIOLATION',
          violations: ['Password must be at least 10 characters long.'],
        },
      },
    })
    const r = router()
    r.push('/register')
    await r.isReady()
    const wrapper = mount(RegisterView, { global: { plugins: [createPinia(), r] } })
    await wrapper.find('#register-email').setValue('b@example.com')
    await wrapper.find('#register-password').setValue('short')
    await wrapper.find('button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('at least 10 characters')
    expect(r.currentRoute.value.path).toBe('/register')
  })
})
