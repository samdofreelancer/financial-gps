import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

/**
 * /login — verified against the live DOM (2026-09-23):
 * #email, #password, button "Sign in".
 * A signed-in session lands on /dashboard unless a ?redirect= deep link survives.
 */
export class LoginPage extends BasePage {
  async open(redirect?: string): Promise<void> {
    await this.goto(redirect ? `/login?redirect=${encodeURIComponent(redirect)}` : '/login')
    await expect(this.page.locator('#email')).toBeVisible()
  }

  async login(email: string, password: string): Promise<void> {
    await this.page.locator('#email').fill(email)
    await this.page.locator('#password').fill(password)
    await this.page.getByRole('button', { name: 'Sign in' }).click()
  }

  async expectValidationError(message: string): Promise<void> {
    await expect(this.page.getByRole('alert')).toContainText(message)
  }
}
