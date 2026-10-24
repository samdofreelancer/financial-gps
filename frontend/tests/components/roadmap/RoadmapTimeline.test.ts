import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import RoadmapTimeline from '@/components/roadmap/RoadmapTimeline.vue'
import type { RoadmapStageView, RoadmapView } from '@/api/roadmap'

function stage(overrides: Partial<RoadmapStageView> = {}): RoadmapStageView {
  return {
    order: 1,
    kind: 'DEBT_ELIMINATION',
    goalId: null,
    objective: 'Xóa khoản nợ còn lại',
    startCondition: 'Không có điều kiện tiên quyết',
    completionCondition: 'Danh mục nợ ở trạng thái COMPLETED',
    completionReached: false,
    requiredMonthlyCapacity: null,
    totalPlannedMonthlyPayment: '5000000.00',
    progress: null,
    projectedCompletionDate: '2027-04-30',
    etaBasis: 'PORTFOLIO_PROJECTION',
    etaAssumption: 'PLANNED_PAYMENT_INCLUDING_DEBT_EXTRA',
    etaReasonCode: null,
    status: 'CURRENT',
    blockedBy: null,
    blockers: [],
    nextAction: 'Tiếp tục trả đúng kế hoạch hàng tháng.',
    ...overrides,
  }
}

const DEBT_STAGE = stage()

const REACHED_GOAL_STAGE = stage({
  order: 2,
  kind: 'GOAL',
  goalId: '6f1c9d54-2f0b-4a1e-9c33-0b7d5a1e9f42',
  objective: 'Quỹ khẩn cấp',
  requiredMonthlyCapacity: '0.00',
  totalPlannedMonthlyPayment: null,
  progress: '1.0000',
  etaBasis: 'SINGLE_DESTINATION_HYPOTHESIS',
  etaAssumption: 'WHOLE_AVAILABLE_CAPACITY',
  status: 'UPCOMING',
  blockedBy: 1,
  completionReached: true,
})

function roadmap(overrides: Partial<RoadmapView> = {}): RoadmapView {
  return {
    asOf: '2026-10-22',
    currency: 'VND',
    routeStatus: 'IN_PROGRESS',
    currentStageOrder: 1,
    orderPolicy: {
      policy: 'DEBT_THEN_EMERGENCY_FUND_THEN_PRIORITY',
      scope: 'ROADMAP_STAGE_ORDER',
      description:
        'Roadmap stage order: debt elimination, then emergency-fund goals, then the remaining goals by priority.',
      appliedOverrides: [],
    },
    // Deliberately out of order: the timeline must render the server's `order`.
    stages: [REACHED_GOAL_STAGE, DEBT_STAGE],
    explanations: [],
    provenance: [],
    ...overrides,
  }
}

function mountTimeline(view: RoadmapView = roadmap()) {
  return mount(RoadmapTimeline, { props: { roadmap: view } })
}

describe('RoadmapTimeline', () => {
  it('renders the stages in ascending order even when the payload arrives unsorted', () => {
    const wrapper = mountTimeline()
    const items = wrapper.findAll('.roadmap-timeline__item')
    expect(items).toHaveLength(2)
    expect(items.map((item) => item.attributes('data-testid'))).toEqual([
      'roadmap-stage-1',
      'roadmap-stage-2',
    ])
    expect(items[0].text()).toContain('Xóa khoản nợ còn lại')
    expect(items[1].text()).toContain('Quỹ khẩn cấp')
  })

  it('highlights exactly the CURRENT stage and labels every positional status', () => {
    const wrapper = mountTimeline()

    const current = wrapper.findAll('.roadmap-timeline__item--current')
    expect(current).toHaveLength(1)
    expect(current[0].attributes('data-testid')).toBe('roadmap-stage-1')

    const statusBadges = wrapper
      .findAll('[data-testid="stage-status-badge"]')
      .map((badge) => badge.text())
    expect(statusBadges).toEqual(['Giai đoạn hiện tại', 'Sắp tới'])
  })

  it('shows the reached badge on an UPCOMING stage, beside its position', () => {
    const wrapper = mountTimeline()
    const upcoming = wrapper.find('[data-testid="roadmap-stage-2"]')

    expect(upcoming.exists()).toBe(true)
    // Position and own completion are two separate facts and both stay visible.
    expect(upcoming.find('[data-testid="stage-status-badge"]').text()).toBe('Sắp tới')
    expect(upcoming.find('[data-testid="stage-reached-badge"]').text()).toBe('Đã đạt mục tiêu')
    expect(upcoming.classes()).toContain('roadmap-timeline__item--reached')
    // The debt stage has not reached its own condition.
    expect(wrapper.find('[data-testid="roadmap-stage-1"] [data-testid="stage-reached-badge"]').exists()).toBe(false)
  })

  it('shows the applied stage-order policy with its scope', () => {
    const wrapper = mountTimeline()
    const policy = wrapper.find('[data-testid="roadmap-order-policy"]')

    expect(policy.text()).toContain('DEBT_THEN_EMERGENCY_FUND_THEN_PRIORITY')
    expect(policy.text()).toContain('ROADMAP_STAGE_ORDER')
    expect(policy.text()).toContain('then the remaining goals by priority')
    // The disclosure states that stage order is not the money order.
    expect(policy.text()).toContain('không phải thứ tự cấp vốn')
    expect(policy.text()).toContain('không phải là trạng thái cấp vốn')
  })

  it('renders an empty state instead of a dead list when there are no stages', () => {
    const wrapper = mountTimeline(roadmap({ stages: [], routeStatus: 'NO_DESTINATION', currentStageOrder: null }))

    expect(wrapper.find('[data-testid="roadmap-timeline-empty"]').exists()).toBe(true)
    expect(wrapper.findAll('.roadmap-timeline__item')).toHaveLength(0)
  })

  it('emits select(stage) when a stage is opened', async () => {
    const wrapper = mountTimeline()

    await wrapper.find('[data-testid="roadmap-stage-2"] button').trigger('click')

    const emitted = wrapper.emitted('select')
    expect(emitted).toHaveLength(1)
    expect((emitted![0][0] as RoadmapStageView).order).toBe(2)
    expect((emitted![0][0] as RoadmapStageView).objective).toBe('Quỹ khẩn cấp')
  })
})
