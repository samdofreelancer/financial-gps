import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import AccountView from '../views/AccountView.vue'
import DashboardView from '../views/DashboardView.vue'
import ProfileView from '../views/ProfileView.vue'
import { useAuthStore } from '../stores/authStore'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView,
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { guestOnly: true },
    },
    {
      path: '/account',
      name: 'account',
      component: AccountView,
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView,
      meta: { requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

/**
 * Session is the truth (007): wait for `restore()` (GET /account/me) once at
 * startup, then route on the proven state. No localStorage, no demo account.
 */
let restored = false

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!restored) {
    restored = true
    await auth.restore()
  }
  // Outage: the session is UNKNOWN, so asserting "anonymous" would be a lie and
  // would bounce the visitor to /login for a backend problem. Render the route
  // and let the app shell surface the outage instead.
  if (auth.unavailable) {
    return true
  }
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'dashboard' }
  }
  // A signed-in visit to `/` is the dashboard, never the public landing page:
  // the landing (with its Sign in button) only exists for anonymous visitors.
  if (to.name === 'home' && auth.isAuthenticated) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
