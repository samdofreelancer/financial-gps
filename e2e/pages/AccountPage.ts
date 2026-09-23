import { expect } from '@playwright/test'
import { BasePage } from './BasePage'

/**
 * /account — verified against the live DOM (2026-09-23):
 * shows the signed-in email, button "Log out" returns to /login.
 * Reachable via the avatar menu ("Account menu for …" → menuitem "Account")
 * or by direct navigation.
 */
export class AccountPage extends BasePage {
  async open(): Promise<void> {
    await this.goto('/account')
    await expect(this.page.getByRole('heading', { name: 'Account' })).toBeVisible()
  }

  async openViaAvatarMenu(): Promise<void> {
    await this.page.getByRole('button', { name: /Account menu for/ }).click()
    await this.page.getByRole('menuitem', { name: 'Account' }).click()
    await this.expectUrl('/account')
  }

  async expectSignedInAs(email: string): Promise<void> {
    await expect(this.page.getByText(email).first()).toBeVisible()
  }

  async logout(): Promise<void> {
    await this.page.getByRole('button', { name: 'Log out' }).click()
    await this.expectUrl('/login')
  }

  async logoutViaAvatarMenu(): Promise<void> {
    await this.page.getByRole('button', { name: /Account menu for/ }).click()
    await this.page.getByRole('menuitem', { name: 'Log out' }).click()
    await this.expectUrl('/login')
  }
}
