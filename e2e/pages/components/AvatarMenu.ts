import { expect, type Page } from '@playwright/test'
import { sel } from '../../support/selectors'

/**
 * Avatar account menu in the app banner — shared chrome, not page content.
 * Extracted so AccountPage/ProfilePage/DashboardPage never duplicate the
 * "Account menu for …" selector knowledge.
 */
export class AvatarMenu {
  constructor(private readonly page: Page) {}

  async open(): Promise<void> {
    await this.page.getByRole('button', { name: sel.account.avatarMenu }).click()
  }

  async goToAccount(): Promise<void> {
    await this.open()
    await this.page.getByRole(sel.account.accountItem.role, { name: sel.account.accountItem.name }).click()
    await expect(this.page).toHaveURL('/account')
  }

  async logout(): Promise<void> {
    await this.open()
    await this.page.getByRole(sel.account.logoutItem.role, { name: sel.account.logoutItem.name }).click()
    await expect(this.page).toHaveURL('/login')
  }
}
