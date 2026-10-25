import { client } from './http'

/**
 * Financial Roadmap client (spec 005, contracts/rest-api.md).
 *
 * Transport rules are those of 001–004: amounts and ratios are decimal **strings** and are never
 * converted to a JS number, dates are ISO-8601, `null` means "not applicable / not calculable" and
 * is never rendered as an available `0.00`. `currency` is echoed once at route level.
 *
 * The roadmap is read-only and owner-scoped by session: there is no owner or goal parameter.
 */
export type RouteStatus = 'IN_PROGRESS' | 'COMPLETED' | 'NO_DESTINATION'
export type StageKind = 'DEBT_ELIMINATION' | 'GOAL'
export type StageStatus = 'COMPLETED' | 'CURRENT' | 'UPCOMING'

export interface OrderPolicyView {
  policy: string
  scope: string
  description: string
  appliedOverrides: string[]
}

/** `{code, explanation}` only — 005 never fabricates a blocker input or provenance per blocker. */
export interface StageBlockerView {
  code: string
  explanation: string
}

export interface RoadmapStageView {
  order: number
  kind: StageKind
  /** `null` for the debt-elimination stage; never `null` for a GOAL stage. */
  goalId: string | null
  objective: string
  startCondition: string
  completionCondition: string
  /** The stage's own completion condition, independent of its positional `status`. */
  completionReached: boolean
  /** GOAL only — `null` for an undated goal and always `null` for the debt stage. */
  requiredMonthlyCapacity: string | null
  /** DEBT_ELIMINATION only — always `null` for a GOAL stage. */
  totalPlannedMonthlyPayment: string | null
  /** A 003 progress ratio (decimal string) or `null` for the debt stage. */
  progress: string | null
  /** `null` ⟹ `etaReasonCode` is present. Never fabricated. */
  projectedCompletionDate: string | null
  etaBasis: string
  etaAssumption: string
  etaReasonCode: string | null
  /** The stage's position on the roadmap only — never an allocation / funding status. */
  status: StageStatus
  /** Order of the first incomplete earlier stage; `null` for COMPLETED and CURRENT. */
  blockedBy: number | null
  blockers: StageBlockerView[]
  /** One actionable, amount-free sentence. */
  nextAction: string
}

export interface ProvenanceView {
  field: string
  kind: string
  detail: string
}

export interface RoadmapView {
  asOf: string
  currency: string
  routeStatus: RouteStatus
  currentStageOrder: number | null
  orderPolicy: OrderPolicyView
  stages: RoadmapStageView[]
  explanations: string[]
  provenance: ProvenanceView[]
}

/**
 * Read the caller's ordered roadmap. `asOf` is optional: when omitted the server evaluates the
 * roadmap at its business date and echoes that date in `asOf`.
 */
export async function fetchRoadmap(asOf?: string): Promise<RoadmapView> {
  const params = new URLSearchParams()
  if (asOf) {
    params.append('asOf', asOf)
  }
  const query = params.toString()
  const response = await client.get<RoadmapView>(
    `/api/v1/roadmap${query ? `?${query}` : ''}`,
  )
  return response.data
}
