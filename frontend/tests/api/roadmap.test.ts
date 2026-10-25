import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/http', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/http')>()
  return { ...actual, client: { get: vi.fn() } }
})

import { client, problemMessage } from '@/api/http'
import { fetchRoadmap, type RoadmapView } from '@/api/roadmap'

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
        'Roadmap stage order: debt elimination, then emergency-fund goals, then the remaining goals by priority. This is not the allocation order.',
      appliedOverrides: [],
    },
    stages: [
      {
        order: 1,
        kind: 'DEBT_ELIMINATION',
        goalId: null,
        objective: 'Eliminate the outstanding debt',
        startCondition: 'No prerequisite — this is the first stage on the roadmap',
        completionCondition: 'Debt portfolio status is COMPLETED (no ACTIVE debt remains)',
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
        nextAction: 'Keep paying the planned monthly payment on every debt.',
      },
      {
        order: 2,
        kind: 'GOAL',
        goalId: '6f1c9d54-2f0b-4a1e-9c33-0b7d5a1e9f42',
        objective: 'Emergency fund',
        startCondition: 'Every earlier roadmap stage is complete.',
        completionCondition: 'AMOUNT_REACHED',
        completionReached: true,
        requiredMonthlyCapacity: '0.00',
        totalPlannedMonthlyPayment: null,
        progress: '1.0000',
        projectedCompletionDate: null,
        etaBasis: 'SINGLE_DESTINATION_HYPOTHESIS',
        etaAssumption: 'WHOLE_AVAILABLE_CAPACITY',
        etaReasonCode: 'NO_AVAILABLE_CAPACITY',
        status: 'UPCOMING',
        blockedBy: 1,
        blockers: [
          {
            code: 'NO_AVAILABLE_CAPACITY',
            explanation: 'Available Capacity is 0.00 VND, so no completion date can be calculated.',
          },
        ],
        nextAction: 'Stage 1 must complete before this stage becomes current on the roadmap.',
      },
    ],
    explanations: ['Stage order is a roadmap position, not an allocation order.'],
    provenance: [
      { field: 'stages[0].totalPlannedMonthlyPayment', kind: 'actual', detail: 'Feature 002 fact' },
    ],
    ...overrides,
  }
}

describe('roadmap API client', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('sends GET /api/v1/roadmap without asOf when the date is omitted', async () => {
    vi.mocked(client.get).mockResolvedValue({ data: roadmap() })

    const view = await fetchRoadmap()

    expect(client.get).toHaveBeenCalledTimes(1)
    expect(client.get).toHaveBeenCalledWith('/api/v1/roadmap')
    expect(view.asOf).toBe('2026-10-22')
  })

  it('passes asOf through as the only query parameter', async () => {
    vi.mocked(client.get).mockResolvedValue({ data: roadmap({ asOf: '2026-06-01' }) })

    const view = await fetchRoadmap('2026-06-01')

    expect(client.get).toHaveBeenCalledWith('/api/v1/roadmap?asOf=2026-06-01')
    expect(view.asOf).toBe('2026-06-01')
  })

  it('returns decimal strings unchanged and never coerces them to numbers', async () => {
    vi.mocked(client.get).mockResolvedValue({ data: roadmap() })

    const view = await fetchRoadmap()

    expect(view.currency).toBe('VND')
    expect(view.stages[0].totalPlannedMonthlyPayment).toBe('5000000.00')
    expect(view.stages[1].requiredMonthlyCapacity).toBe('0.00')
    expect(view.stages[1].progress).toBe('1.0000')
    expect(view.orderPolicy.scope).toBe('ROADMAP_STAGE_ORDER')
  })

  it('preserves null instead of inventing a zero amount, a date or a status', async () => {
    vi.mocked(client.get).mockResolvedValue({ data: roadmap() })

    const view = await fetchRoadmap()
    const debtStage = view.stages[0]
    const goalStage = view.stages[1]

    // null means "not applicable / not calculable" — it is never rewritten to "0.00".
    expect(debtStage.requiredMonthlyCapacity).toBeNull()
    expect(debtStage.progress).toBeNull()
    expect(goalStage.totalPlannedMonthlyPayment).toBeNull()
    expect(goalStage.projectedCompletionDate).toBeNull()
    // A null ETA always carries its reason code.
    expect(goalStage.etaReasonCode).toBe('NO_AVAILABLE_CAPACITY')
    // completionReached is independent of the positional status.
    expect(goalStage.completionReached).toBe(true)
    expect(goalStage.status).toBe('UPCOMING')
    expect(goalStage.blockedBy).toBe(1)
  })

  it('lets a ProblemDetail error reach the caller so the store can map it', async () => {
    const problem = {
      response: {
        status: 400,
        data: {
          code: 'VALIDATION_FAILED',
          detail: 'asOf: must be a valid ISO date',
          violations: ['asOf: must be a valid ISO date'],
        },
      },
    }
    vi.mocked(client.get).mockRejectedValue(problem)

    await expect(fetchRoadmap('not-a-date')).rejects.toBe(problem)
    // The shared mapper keeps the server-provided text; it never invents a meaning.
    expect(problemMessage(problem, 'fallback')).toBe('asOf: must be a valid ISO date')
  })
})
