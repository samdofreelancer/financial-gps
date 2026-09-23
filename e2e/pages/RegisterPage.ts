import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

/**
 * /register — verified against the live DOM (2026-09-23):
 * #register-email, #register-password, button "Create account".
 * A fresh registration lands on /dashboard (guestOnly guard agrees).
 */
export class RegisterPage extends BasePage {
  async open(): Promise<void> {
    await this.goto('/register')
    await expect(this.page.locator('#register-email')).toBeVisible()
  }

  async register(email: string, password: string): Promise<void> {
    await this.page.locator('#register-email').fill(email)
    await this.page.locator('#register-password').fill(password)
    await this.page.getByRole('button', { name: 'Create account' }).click()
  }

  async expectOnDashboard(): Promise<void> {
    await this.expectUrl('/dashboard')
    await this.expectText('Welcome back')
  }
}
