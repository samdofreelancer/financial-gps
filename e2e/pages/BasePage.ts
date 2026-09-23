import { expect, type Page } from '@playwright/test'

/** Shared navigation + assertion vocabulary. Specs talk to page objects, never to `page.` directly. */
export class BasePage {
  constructor(protected readonly page: Page) {}

  async goto(path: string): Promise<void> {
    await this.page.goto(path)
  }

  async expectUrl(path: string): Promise<void> {
    await expect(this.page).toHaveURL(path)
  }

  async expectText(text: string | RegExp): Promise<void> {
    await expect(this.page.getByText(text).first()).toBeVisible()
  }
}
