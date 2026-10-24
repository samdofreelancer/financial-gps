import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import RoadmapStageDetails from '@/components/roadmap/RoadmapStageDetails.vue'
import type { RoadmapStageView } from '@/api/roadmap'

function goalStage(overrides: Partial<RoadmapStageView> = {}): RoadmapStageView {
  return {
    order: 2,
    kind: 'GOAL',
    goalId: '6f1c9d54-2f0b-4a1e-9c33-0b7d5a1e9f42',
    objective: 'Quỹ khẩn cấp',
    startCondition:
      'Mọi giai đoạn trước trên lộ trình đã hoàn thành. Đây chỉ là vị trí trên lộ trình: quy tắc phân bổ quyết định việc cấp vốn.',
    completionCondition: 'AMOUNT_REACHED',
    completionReached: false,
    requiredMonthlyCapacity: '3000000.00',
    totalPlannedMonthlyPayment: null,
    progress: '0.2500',
    projectedCompletionDate: '2027-01-31',
    etaBasis: 'SINGLE_DESTINATION_HYPOTHESIS',
    etaAssumption: 'WHOLE_AVAILABLE_CAPACITY',
    etaReasonCode: null,
    status: 'UPCOMING',
    blockedBy: 1,
    blockers: [],
    nextAction:
      'Giai đoạn 1 phải hoàn thành trước khi giai đoạn này trở thành hiện tại. Vị trí của nó không phải là tuyên bố cấp vốn.',
    ...overrides,
  }
}

function debtStage(overrides: Partial<RoadmapStageView> = {}): RoadmapStageView {
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
    nextAction: 'Tiếp tục trả đúng khoản trả theo kế hoạch hàng tháng.',
    ...overrides,
  }
}

function mountDetails(stage: RoadmapStageView, currency = 'VND') {
  return mount(RoadmapStageDetails, { props: { stage, currency } })
}

describe('RoadmapStageDetails', () => {
  it('shows the start and completion conditions and the positional status', () => {
    const stage = goalStage()
    const wrapper = mountDetails(stage)

    expect(wrapper.find('[data-testid="stage-objective"]').text()).toBe('Quỹ khẩn cấp')
    expect(wrapper.find('[data-testid="stage-start-condition"]').text()).toBe(stage.startCondition)
    expect(wrapper.find('[data-testid="stage-completion-condition"]').text()).toBe('AMOUNT_REACHED')
    expect(wrapper.find('[data-testid="stage-status-badge"]').text()).toBe('Sắp tới')
  })

  it('renders the server blockers through the shared BlockerPanel', () => {
    const wrapper = mountDetails(
      goalStage({
        blockers: [
          {
            code: 'NO_AVAILABLE_CAPACITY',
            explanation: 'Available Capacity is 0.00 VND, so no completion date can be calculated.',
          },
        ],
      }),
    )

    const blockers = wrapper.find('[data-testid="stage-blockers"]')
    expect(blockers.text()).toContain('NO_AVAILABLE_CAPACITY')
    expect(blockers.text()).toContain('no completion date can be calculated')
    // No fabricated blocker provenance: the roadmap blocker is a {code, explanation} pair only.
    expect(blockers.find('.provenance-kind').exists()).toBe(false)
  })

  it('shows the amount-free next action verbatim and never adds money advice', () => {
    const stage = goalStage()
    const wrapper = mountDetails(stage)
    const nextAction = wrapper.find('[data-testid="stage-next-action"]')

    expect(nextAction.text()).toBe(stage.nextAction)
    expect(nextAction.text()).not.toContain('VND')
    expect(nextAction.text()).not.toMatch(/\d[.,]\d{2}/)
    expect(wrapper.text()).not.toContain('Thiếu hụt')
  })

  it('renders an unavailable ETA with its reason code instead of a fabricated date', () => {
    const wrapper = mountDetails(
      goalStage({
        projectedCompletionDate: null,
        etaReasonCode: 'NO_AVAILABLE_CAPACITY',
      }),
    )

    expect(wrapper.find('[data-testid="stage-eta-date"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="stage-eta-unavailable"]').text()).toContain(
      'Chưa xác định được ngày hoàn thành',
    )
    const reason = wrapper.find('[data-testid="stage-eta-reason"]').text()
    expect(reason).toContain('NO_AVAILABLE_CAPACITY')
    expect(reason).toContain('Năng lực khả dụng bằng 0')
  })

  it('renders the ISO completion date as a time element when the ETA exists', () => {
    const wrapper = mountDetails(goalStage())
    const date = wrapper.find('[data-testid="stage-eta-date"]')

    expect(date.exists()).toBe(true)
    expect(date.attributes('datetime')).toBe('2027-01-31')
    expect(wrapper.find('[data-testid="stage-eta-reason"]').exists()).toBe(false)
  })

  it('labels the capacity figure by kind — required capacity for a goal', () => {
    const wrapper = mountDetails(goalStage())

    expect(wrapper.find('[data-testid="stage-capacity-label"]').text()).toBe(
      'Năng lực hàng tháng cần thiết',
    )
    const amount = wrapper.find('[data-testid="stage-capacity-amount"]')
    expect(amount.text()).toContain('3.000.000')
    // The debt-only planned payment is never conflated with the goal's required capacity.
    expect(amount.text()).not.toContain('5.000.000')
    expect(wrapper.find('[data-testid="stage-progress"]').text()).toBe('25%')
  })

  it('labels the capacity figure by kind — planned monthly payment for the debt stage', () => {
    const wrapper = mountDetails(debtStage())

    expect(wrapper.find('[data-testid="stage-capacity-label"]').text()).toBe(
      'Khoản trả hàng tháng theo kế hoạch',
    )
    const amount = wrapper.find('[data-testid="stage-capacity-amount"]')
    expect(amount.text()).toContain('5.000.000')
    expect(amount.text()).not.toContain('Năng lực')
    // A debt portfolio has no progress ratio: nothing is invented in its place.
    expect(wrapper.find('[data-testid="stage-progress"]').exists()).toBe(false)
  })

  it('renders an em dash, never an available 0.00, when the capacity is null', () => {
    const wrapper = mountDetails(goalStage({ requiredMonthlyCapacity: null, progress: null }))
    const amount = wrapper.find('[data-testid="stage-capacity-amount"]')

    expect(amount.text()).toContain('—')
    expect(amount.text()).not.toContain('0,00')
    expect(amount.text()).toContain('Không áp dụng cho giai đoạn này.')
  })

  it('shows the reached badge on an UPCOMING stage, independently of its position', () => {
    const wrapper = mountDetails(goalStage({ status: 'UPCOMING', completionReached: true }))

    expect(wrapper.find('[data-testid="stage-status-badge"]').text()).toBe('Sắp tới')
    expect(wrapper.find('[data-testid="stage-reached-badge"]').text()).toBe('Đã đạt mục tiêu')
    // The stage keeps its own explanation of why it is not current yet.
    expect(wrapper.text()).toContain('Điều kiện hoàn thành riêng của giai đoạn đã đạt')
    expect(wrapper.text()).toContain('không phải là tuyên bố về việc cấp vốn')
  })

  it('hides the reached badge when the stage’s own condition is not met', () => {
    const wrapper = mountDetails(goalStage({ completionReached: false }))

    expect(wrapper.find('[data-testid="stage-reached-badge"]').exists()).toBe(false)
  })

  it('states the ETA basis and the ETA assumption', () => {
    const goal = mountDetails(goalStage())
    expect(goal.find('[data-testid="stage-eta-basis"]').text()).toContain(
      'Giả thuyết chỉ xét riêng giai đoạn này',
    )
    expect(goal.find('[data-testid="stage-eta-assumption"]').text()).toContain(
      'Toàn bộ năng lực khả dụng',
    )

    const debt = mountDetails(debtStage())
    expect(debt.find('[data-testid="stage-eta-basis"]').text()).toContain(
      'Dự báo theo kế hoạch trả nợ của danh mục',
    )
    expect(debt.find('[data-testid="stage-eta-assumption"]').text()).toContain(
      'phần trả thêm đã đăng ký',
    )
  })

  it('states that stage position is not a funding statement, especially for UPCOMING', () => {
    const wrapper = mountDetails(goalStage({ status: 'UPCOMING' }))
    const note = wrapper.find('[data-testid="stage-position-note"]').text()

    expect(note).toContain('không phải là trạng thái cấp vốn')
    expect(note).toContain('không có nghĩa là giai đoạn chưa được cấp vốn')
    expect(wrapper.find('[data-testid="stage-blocked-by"]').text()).toContain(
      'giai đoạn 1 chưa hoàn thành',
    )
  })
})
