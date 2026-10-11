import { client } from './http'

export interface GpsProvenance {
  field: string
  kind: 'actual' | 'assumed' | 'calculated' | 'unavailable'
  detail: string
  assumptionSource: string | null
}

export interface MoneyView {
  field: string
  amount: string | null
  currency: string
  availability: 'AVAILABLE' | 'UNAVAILABLE'
  provenanceKind: string
  provenanceDetail: string
  assumptionSource: string | null
}

export interface DtiView {
  status: 'AVAILABLE' | 'UNAVAILABLE'
  ratio: string | null
  reasonCode: string | null
  explanation: string | null
  provenanceKind: string
  provenanceDetail: string
}

export interface CurrentPositionView {
  income: MoneyView
  expense: MoneyView
  mandatoryPayment: MoneyView
  netCashFlow: MoneyView
  availableCapacity: MoneyView
  savings: MoneyView
  emergencyFund: MoneyView
  dependents: number | null
  totalOutstandingDebt: MoneyView
  dti: DtiView
}

export interface DistanceView {
  type: 'AMOUNT_BASED' | 'DEBT_FREEDOM'
  remaining: string
  currency: string
  targetAmount: string | null
  currentAmount: string | null
}

export interface ProgressView {
  progressPercent: string | null
  reason: string | null
}

export interface CapacityComparisonView {
  requiredMonthly: string
  projectedMonthly: string
  coverage: 'MEETS_REQUIRED' | 'SHORTFALL' | 'NOT_APPLICABLE'
  shortfall: string | null
  currency: string
  explanation: string
}

export interface EtaView {
  availability: 'CALCULATED' | 'UNAVAILABLE'
  date: string | null
  periods: number | null
  reason: string | null
  explanation: string | null
}

export interface ConditionView {
  type: string
  reason: string | null
  requiredMonthly: string | null
  projectedMonthly: string | null
  monthsRemaining: number | null
  etaPeriods: number | null
  lateness: number | null
  latenessTolerance: number | null
  projectedDebtFreeDate: string | null
  totalMonthsRemaining: number | null
  targetDate: string | null
}

export interface StatusView {
  status: 'COMPLETED' | 'BLOCKED' | 'ON_TRACK' | 'AT_RISK' | 'OFF_TRACK'
  explanation: string
  condition: ConditionView
}

export interface BlockerView {
  code: string
  explanation: string
  inputs: string[]
  provenanceKind: string
  provenanceDetail: string
}

export interface NextActionView {
  type: string
  description: string
  linkedBlockerCodes: string[]
  provenanceKind: string
  provenanceDetail: string
}

export interface ExplanationView {
  category: string
  field: string
  rule: string
  inputs: Array<{ name: string; value: string; provenanceKind: string }>
  threshold: string
  outcome: string
  provenanceKind: string
  provenanceDetail: string
}

export interface DestinationView {
  type: 'GOAL' | 'DEBT_FREEDOM'
  goalId: string | null
  goalName: string | null
  goalType: string | null
}

export interface FinancialGpsView {
  asOf: string
  destination: DestinationView
  currentPosition: CurrentPositionView
  distance: DistanceView
  progress: ProgressView
  capacityComparison: CapacityComparisonView
  eta: EtaView
  status: StatusView
  blockers: BlockerView[]
  nextActions: NextActionView[]
  explanations: ExplanationView[]
  provenance: GpsProvenance[]
  missingInputs: string[]
}

export async function fetchFinancialGps(goalId: string, asOf?: string): Promise<FinancialGpsView> {
  const params = new URLSearchParams()
  params.append('goalId', goalId)
  if (asOf) {
    params.append('asOf', asOf)
  }
  const response = await client.get<FinancialGpsView>(`/api/v1/gps?${params.toString()}`)
  return response.data
}