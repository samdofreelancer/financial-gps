import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchRoadmap, type RoadmapView } from '../api/roadmap'
import { problemMessage } from '../api/http'

/** The RFC 7807 body stays authoritative; only a connection failure gets our own wording. */
function roadmapErrorMessage(caught: unknown): string {
  const response = (caught as { response?: unknown } | null)?.response
  if (!response) {
    return 'Không kết nối được máy chủ. Hãy kiểm tra kết nối và thử lại.'
  }
  return problemMessage(caught, 'Không tải được lộ trình tài chính.')
}

/**
 * Server state only. The roadmap is recomputed by the server on every read, so the store never
 * caches across `asOf` values: each load replaces the view and a late response for an older date
 * can never overwrite a newer one.
 */
export const useRoadmapStore = defineStore('roadmap', () => {
  const roadmap = ref<RoadmapView | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  /** Monotonic token: a newer request wins, so a late response never overwrites it. */
  let token = 0

  async function load(asOf?: string): Promise<void> {
    const current = ++token
    loading.value = true
    error.value = null
    // No stale roadmap across dates: a view evaluated at another `asOf` must not stay on screen
    // while the new one is loading (a failed reload then shows the error, not yesterday's route).
    roadmap.value = null
    try {
      const view = await fetchRoadmap(asOf)
      if (current !== token) return
      roadmap.value = view
    } catch (caught) {
      if (current !== token) return
      error.value = roadmapErrorMessage(caught)
    } finally {
      if (current === token) loading.value = false
    }
  }

  return { roadmap, loading, error, load }
})
