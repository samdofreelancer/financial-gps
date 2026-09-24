import { expect } from '@playwright/test'
import { BasePage } from './BasePage'
import { sel } from '../support/selectors'

/**
 * /login — #email, #password, button "Sign in".
 * A signed-in session lands on /dashboard unless a ?redirect= deep link survives.
 */
export class LoginPage extends BasePage {
  async open(redirect?: string): Promise<void> {
    await this.goto(redirect ? `/login?redirect=${encodeURIComponent(redirect)}` : '/login')
    await expect(this.page.locator(sel.login.email)).toBeVisible()
  }

  async login(email: string, password: string): Promise<void> {
    await this.page.locator(sel.login.email).fill(email)
    await this.page.locator(sel.login.password).fill(password)
    await this.page.getByRole(sel.login.submit.role, { name: sel.login.submit.name }).click()
  }

  async expectValidationError(message: string): Promise<void> {
    await expect(this.page.getByRole(sel.login.validationAlert.role)).toContainText(message)
  }

  async expectServerError(): Promise<void> {
    await this.expectUrl('/login')
    await expect(this.page.getByRole(sel.login.validationAlert.role)).toBeVisible()
  }
}

