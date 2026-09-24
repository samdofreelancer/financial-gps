import { expect, type Page } from '@playwright/test'
import { BasePage } from './BasePage'
import { AvatarMenu } from './components/AvatarMenu'
import { sel } from '../support/selectors'

/**
 * /account — shows the signed-in email; "Log out" returns to /login.
 * Avatar-menu navigation lives in the shared AvatarMenu component.
 */
export class AccountPage extends BasePage {
  readonly avatarMenu: AvatarMenu

  constructor(page: Page) {
    super(page)
    this.avatarMenu = new AvatarMenu(page)
  }

  async open(): Promise<void> {
    await this.goto('/account')
    await expect(this.page.getByRole(sel.account.heading.role, { name: sel.account.heading.name })).toBeVisible()
  }

  async openViaAvatarMenu(): Promise<void> {
    await this.avatarMenu.goToAccount()
  }

  async expectSignedInAs(email: string): Promise<void> {
    await this.expectText(email)
  }

  async logout(): Promise<void> {
    await this.page
      .getByRole(sel.account.logoutButton.role, { name: sel.account.logoutButton.name })
      .click()
    await this.expectUrl('/login')
  }

  async logoutViaAvatarMenu(): Promise<void> {
    await this.avatarMenu.logout()
  }
}

