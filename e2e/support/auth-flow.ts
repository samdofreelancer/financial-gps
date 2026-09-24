import type { Page } from '@playwright/test'
import { E2E_PASSWORD, uniqueEmail } from '../fixtures/accounts'
import { RegisterPage } from '../pages/RegisterPage'

/**
 * Authenticated fixture — every spec file gets its own fresh account without
 * repeating register logic. The email is exposed so specs can assert "signed
 * in as …" without inventing identities.
 */
export interface AuthContext {
  page: Page
  email: string
}

export async function registerFreshAccount(page: Page, prefix: string): Promise<AuthContext> {
  const email = uniqueEmail(prefix)
  const register = new RegisterPage(page)
  await register.open()
  await register.register(email, E2E_PASSWORD)
  await register.expectOnDashboard()
  return { page, email }
}

export { E2E_PASSWORD }
