import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/api/roadmap', () => ({ fetchRoadmap: vi.fn() }))

import { fetchRoadmap, type RoadmapView } from '@/api/roadmap'
import { useRoadmapStore } from '@/stores/roadmapStore'

function roadmap(overrides: Partial<RoadmapView> = {}): RoadmapView {
  return {
    asOf: '2026-10-22',
    currency: 'VND',
    routeStatus: 'IN_PROGRESS',
    currentStageOrder: 1,
    orderPolicy: {
      policy: 'DEBT_THEN_EMERGENCY_FUND_THEN_PRIORITY',
      scope: 'ROADMAP_STAGE_ORDER',
      description: 'Stage order, not the allocation order.',
      appliedOverrides: [],
    },
    stages: [],
    explanations: [],
    provenance: [],
    ...overrides,
  }
}

describe('roadmap store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads the roadmap and passes asOf through untouched', async () => {
    vi.mocked(fetchRoadmap).mockResolvedValue(roadmap({ asOf: '2026-06-01' }))

    const store = useRoadmapStore()
    await store.load('2026-06-01')

    expect(fetchRoadmap).toHaveBeenCalledWith('2026-06-01')
    expect(store.roadmap?.asOf).toBe('2026-06-01')
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('never keeps a roadmap evaluated at another asOf when the new read fails', async () => {
    vi.mocked(fetchRoadmap).mockResolvedValueOnce(roadmap({ asOf: '2026-01-01' }))

    const store = useRoadmapStore()
    await store.load('2026-01-01')
    expect(store.roadmap?.asOf).toBe('2026-01-01')

    vi.mocked(fetchRoadmap).mockRejectedValueOnce({
      response: { status: 400, data: { code: 'VALIDATION_FAILED', detail: 'asOf: must be a valid ISO date' } },
    })
    await store.load('2026-06-01')

    // The old date's route must not linger as if it described the requested date.
    expect(store.roadmap).toBeNull()
    expect(store.error).toBe('asOf: must be a valid ISO date')
  })

  it('maps a connection failure to a Vietnamese message', async () => {
    vi.mocked(fetchRoadmap).mockRejectedValue(new Error('offline'))

    const store = useRoadmapStore()
    await store.load()

    expect(store.error).toBe('Không kết nối được máy chủ. Hãy kiểm tra kết nối và thử lại.')
    expect(store.loading).toBe(false)
  })

  it('keeps only the newest response when dates are switched quickly', async () => {
    let settleSlow!: (view: RoadmapView) => void
    vi.mocked(fetchRoadmap)
      .mockImplementationOnce(
        () => new Promise<RoadmapView>((resolve) => { settleSlow = resolve }),
      )
      .mockResolvedValueOnce(roadmap({ asOf: '2026-06-01' }))

    const store = useRoadmapStore()
    const slow = store.load('2026-01-01')
    await store.load('2026-06-01')

    settleSlow(roadmap({ asOf: '2026-01-01' }))
    await slow

    expect(store.roadmap?.asOf).toBe('2026-06-01')
    expect(store.loading).toBe(false)
  })
})
