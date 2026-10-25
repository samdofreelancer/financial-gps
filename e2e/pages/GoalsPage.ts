import { expect, type Page } from '@playwright/test'
import { BasePage } from './BasePage'
import { sel } from '../support/selectors'
import type { GoalInput } from '../support/test-data'

/**
 * /goals — Financial Goals page: goal list, goal form, progress card, capacity view.
 * Assertions read server-rendered text only; the suite never computes goal math.
 */
export class GoalsPage extends BasePage {
  /** Server-assigned id of the most recently created goal (for direct API checks). */
  lastCreatedId: string | null = null

  constructor(page: Page) {
    super(page)
  }

  async open(): Promise<void> {
    await this.goto('/goals')
    await expect(
      this.page.getByRole(sel.goals.heading.role, { name: sel.goals.heading.name, exact: true }),
    ).toBeVisible()
  }

  /** The form lives in a dialog: scope the save button to it, never a page-level 'Lưu'. */
  private dialog() {
    return this.page.getByRole('dialog')
  }

  /** Create a goal through the form; remembers the server-assigned id from POST. */
  async addGoal(input: GoalInput): Promise<void> {
    await this.page.getByRole(sel.goals.add.role, { name: sel.goals.add.name, exact: true }).click()
    await this.page.getByTestId(sel.goals.name).fill(input.name)
    await this.page.getByTestId(sel.goals.type).selectOption(input.type ?? 'EMERGENCY_FUND')
    await this.page.getByTestId(sel.goals.priority).fill(input.priority ?? '1')
    await this.page.locator(sel.goals.target).fill(input.target)
    await this.page.locator(sel.goals.current).fill(input.current)
    if (input.date) {
      await this.page.getByTestId(sel.goals.date).fill(input.date)
    }
    const created = this.page.waitForResponse(
      (res) => res.url().includes('/api/v1/goals') && res.request().method() === 'POST',
    )
    await this.dialog()
      .getByRole(sel.goals.save.role, { name: sel.goals.save.name, exact: true })
      .click()
    const response = await created
    try {
      const body = (await response.json()) as { id?: string }
      this.lastCreatedId = body.id ?? null
    } catch {
      this.lastCreatedId = null
    }
    await this.expectRow(input.name)
  }

  private row(name: string) {
    return this.page.getByTestId(sel.goals.item).filter({ hasText: name }).first()
  }

  /** Update the current amount of an existing goal (re-evaluates completion). */
  async editCurrent(name: string, current: string): Promise<void> {
    await this.row(name).getByRole(sel.goals.edit.role, { name: sel.goals.edit.name, exact: true }).click()
    await this.page.locator(sel.goals.current).fill(current)
    const saved = this.page.waitForResponse(
      (res) => res.url().includes('/api/v1/goals') && res.request().method() === 'PUT',
    )
    await this.dialog()
      .getByRole(sel.goals.save.role, { name: sel.goals.save.name, exact: true })
      .click()
    await saved
  }

  /** Open the capacity view of one goal from its row. */
  async openCapacity(name: string): Promise<void> {
    await this.row(name)
      .getByRole(sel.goals.capacity.role, { name: sel.goals.capacity.name, exact: true })
      .click()
    await expect(this.page.getByTestId(sel.goals.capacityCard)).toBeVisible()
  }

  /** Soft-delete (archive) a goal from its row. Confirm, then wait for the row to go. */
  async archiveGoal(name: string): Promise<void> {
    await this.row(name)
      .getByRole(sel.goals.remove.role, { name: sel.goals.remove.name, exact: true })
      .click()
    const removed = this.page.waitForResponse(
      (res) => res.url().includes('/api/v1/goals') && res.request().method() === 'DELETE',
    )
    await this.page
      .getByRole('alertdialog')
      .getByRole(sel.goals.confirmRemove.role, {
        name: sel.goals.confirmRemove.name,
        exact: true,
      })
      .click()
    // Await the DELETE directly: swallowing it would hide a failed archive
    // while the row assertion below still passes on optimistic UI.
    await removed
    await expect(this.row(name)).toHaveCount(0)
  }

  async expectRow(name: string): Promise<void> {
    await expect(this.row(name)).toBeVisible()
  }

  async expectAbsent(name: string): Promise<void> {
    await expect(this.page.getByTestId(sel.goals.item).filter({ hasText: name })).toHaveCount(0)
  }

  async expectProgress(remaining: string, progress: string): Promise<void> {
    await expect(this.page.getByTestId(sel.goals.remaining)).toContainText(remaining)
    await expect(this.page.getByTestId(sel.goals.progressValue)).toContainText(progress)
  }

  async expectCoverage(coverage: string): Promise<void> {
    await expect(this.page.getByTestId(sel.goals.coverage)).toContainText(coverage)
  }

  async expectRequired(required: string): Promise<void> {
    await expect(this.page.getByTestId(sel.goals.required)).toContainText(required)
  }

  async expectCapacityShortfall(shortfall: string): Promise<void> {
    await expect(this.page.getByTestId(sel.goals.capacityCard)).toContainText(shortfall)
  }

  async expectCompleted(name: string): Promise<void> {
    await expect(this.row(name)).toContainText('Đã đạt')
  }
}
