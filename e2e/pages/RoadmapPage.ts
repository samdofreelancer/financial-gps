import { expect, type Locator, type Page } from '@playwright/test'
import { BasePage } from './BasePage'
import { sel } from '../support/selectors'

/**
 * /roadmap — the ordered stage timeline plus the selected stage's details.
 *
 * Assertions read server-rendered text only: the suite never computes capacity, progress or an
 * ETA, and it never decides which stage is current — it reads the status the server reported.
 */
export class RoadmapPage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  async open(): Promise<void> {
    await this.goto('/roadmap')
    await expect(
      this.page.getByRole(sel.roadmap.heading.role, { name: sel.roadmap.heading.name, exact: true }),
    ).toBeVisible()
  }

  private stageItem(order: number): Locator {
    return this.page.getByTestId(sel.roadmap.stage(order))
  }

  /** The details panel of the selected stage (the server's current stage on first load). */
  private details(): Locator {
    return this.page.getByTestId(sel.roadmap.selectedStage)
  }

  async expectStages(count: number): Promise<void> {
    for (let order = 1; order <= count; order += 1) {
      await expect(this.stageItem(order)).toBeVisible()
    }
    await expect(this.stageItem(count + 1)).toHaveCount(0)
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByTestId(sel.roadmap.timeline)).toBeVisible()
  }

  async expectEmptyDestination(): Promise<void> {
    await expect(this.page.getByTestId(sel.roadmap.empty)).toBeVisible()
    await expect(this.page.getByTestId(sel.roadmap.timeline)).toHaveCount(0)
  }

  /** SC-001: the current stage and its detail are visible without navigating anywhere. */
  async expectCurrentStage(order: number): Promise<void> {
    await expect(this.stageItem(order).getByTestId(sel.roadmap.statusBadge)).toHaveText(
      'Giai đoạn hiện tại',
    )
    await expect(this.details()).toContainText(`Giai đoạn ${order}`)
  }

  async expectStatus(order: number, label: string): Promise<void> {
    await expect(this.stageItem(order).getByTestId(sel.roadmap.statusBadge)).toHaveText(label)
  }

  async expectBlockedBadge(order: number, blockerOrder: number): Promise<void> {
    await expect(this.stageItem(order).getByTestId(sel.roadmap.blockedBadge)).toHaveText(
      `Chờ giai đoạn ${blockerOrder}`,
    )
  }

  /** `completionReached` is its own badge, shown even while the stage is positionally UPCOMING. */
  async expectReachedBadge(order: number): Promise<void> {
    await expect(this.stageItem(order).getByTestId(sel.roadmap.reachedBadge)).toBeVisible()
  }

  async expectNoReachedBadge(order: number): Promise<void> {
    await expect(this.stageItem(order).getByTestId(sel.roadmap.reachedBadge)).toHaveCount(0)
  }

  async openStage(order: number): Promise<void> {
    await this.stageItem(order).getByRole('button').click()
    await expect(this.details()).toContainText(`Giai đoạn ${order}`)
  }

  async expectObjective(text: string): Promise<void> {
    await expect(this.details().getByTestId(sel.roadmap.objective)).toHaveText(text)
  }

  async expectDetailStatus(label: string): Promise<void> {
    await expect(this.details().getByTestId(sel.roadmap.statusBadge)).toHaveText(label)
  }

  async expectStartCondition(text: string): Promise<void> {
    await expect(this.details().getByTestId(sel.roadmap.startCondition)).toContainText(text)
  }

  async expectCompletionCondition(text: string): Promise<void> {
    await expect(this.details().getByTestId(sel.roadmap.completionCondition)).toContainText(text)
  }

  async expectCapacityLabel(text: string): Promise<void> {
    await expect(this.details().getByTestId(sel.roadmap.capacityLabel)).toContainText(text)
  }

  async expectCapacityAmount(text: string): Promise<void> {
    await expect(this.details().getByTestId(sel.roadmap.capacityAmount)).toContainText(text)
  }

  async expectEtaBasis(text: string): Promise<void> {
    await expect(this.details().getByTestId(sel.roadmap.etaBasis)).toContainText(text)
  }

  async expectEtaAssumption(text: string): Promise<void> {
    await expect(this.details().getByTestId(sel.roadmap.etaAssumption)).toContainText(text)
  }

  async expectBlockedByNote(blockerOrder: number): Promise<void> {
    await expect(this.details().getByTestId(sel.roadmap.blockedBy)).toContainText(
      `giai đoạn ${blockerOrder} chưa hoàn thành`,
    )
  }

  async expectNextAction(text: string): Promise<void> {
    await expect(this.details().getByTestId(sel.roadmap.nextAction)).toContainText(text)
  }

  /** Position is stated as a roadmap position, never as a funding status (RM-016). */
  async expectPositionIsNotFunding(): Promise<void> {
    await expect(this.details().getByTestId(sel.roadmap.positionNote)).toContainText(
      'không phải là trạng thái cấp vốn',
    )
  }

  async expectReachedInDetails(): Promise<void> {
    await expect(this.details().getByTestId(sel.roadmap.reachedBadge)).toBeVisible()
  }

  /** The applied policy carries its scope, so stage order is visibly not the allocation order. */
  async expectStageOrderPolicy(): Promise<void> {
    await expect(this.page.getByTestId(sel.roadmap.policyScope)).toHaveText('ROADMAP_STAGE_ORDER')
    await expect(this.page.getByTestId(sel.roadmap.policyNote)).toContainText(
      'không phải thứ tự cấp vốn',
    )
  }
}
