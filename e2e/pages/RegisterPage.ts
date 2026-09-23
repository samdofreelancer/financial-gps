import { expect } from '@playwright/test'
import { BasePage } from './BasePage'
import { sel } from '../support/selectors'

/**
 * /register — #register-email, #register-password, button "Create account".
 * A fresh registration lands on /dashboard (guestOnly guard agrees).
 */
export class RegisterPage extends BasePage {
  async open(): Promise<void> {
    await this.goto('/register')
    await expect(this.page.locator(sel.register.email)).toBeVisible()
  }

  async register(email: string, password: string): Promise<void> {
    await this.page.locator(sel.register.email).fill(email)
    await this.page.locator(sel.register.password).fill(password)
    await this.page.getByRole(sel.register.submit.role, { name: sel.register.submit.name }).click()
  }

  async expectOnDashboard(): Promise<void> {
    await this.expectUrl('/dashboard')
    await this.expectText(sel.dashboard.heading)
  }
}

