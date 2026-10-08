import { client, csrf, xsrfHeader } from './http'

export type GoalType =
  | 'DEBT_FREEDOM'
  | 'EMERGENCY_FUND'
  | 'SAVINGS'
  | 'HOUSING'
  | 'EDUCATION'
  | 'RETIREMENT'
  | 'OTHER'
export type GoalStatus = 'ACTIVE' | 'COMPLETED' | 'ARCHIVED'

export interface GoalView {
  id: string
  name: string
  goalType: GoalType
  targetAmount: string
  currentAmount: string
  targetDate: string | null
  priority: number
  status: GoalStatus
  currency: string
  remaining: string
  progress: string
  completionCondition: string
  derived: Record<string, string>
}

export interface GoalCapacityView {
  goalId: string
  asOf: string
  remaining: string
  monthsRemaining: number | null
  requiredMonthlyCapacity: string | null
  availableCapacity: string
  capacityCoverage: 'MEETS_REQUIRED' | 'SHORTFALL' | 'NOT_APPLICABLE'
  monthlyShortfall: string | null
  dateFeasibility: 'DATED' | 'UNDATED' | 'EXPIRED_TARGET_DATE' | 'COMPLETED'
  explanation: string
}

export interface GoalPayload {
  name: string
  goalType: GoalType
  targetAmount: string
  currentAmount: string
  targetDate: string | null
  priority: number
}

export async function listGoals(): Promise<GoalView[]> {
  const { data } = await client.get<GoalView[]>('/api/v1/goals')
  return data
}

export async function getGoal(id: string): Promise<GoalView> {
  const { data } = await client.get<GoalView>(`/api/v1/goals/${id}`)
  return data
}

export async function getGoalCapacity(id: string): Promise<GoalCapacityView> {
  const { data } = await client.get<GoalCapacityView>(`/api/v1/goals/${id}/capacity`)
  return data
}

export async function postGoal(body: GoalPayload): Promise<GoalView> {
  await csrf()
  const { data } = await client.post<GoalView>('/api/v1/goals', body, { headers: xsrfHeader() })
  return data
}

export async function putGoal(id: string, body: GoalPayload): Promise<GoalView> {
  await csrf()
  const { data } = await client.put<GoalView>(`/api/v1/goals/${id}`, body, { headers: xsrfHeader() })
  return data
}

export async function deleteGoal(id: string): Promise<void> {
  await csrf()
  await client.delete(`/api/v1/goals/${id}`, { headers: xsrfHeader() })
}

/** Client-side guard: non-negative canonical decimals (server is authoritative). */
export function isMoneyValid(value: string): boolean {
  return /^\d+(\.\d{1,2})?$/.test(value.trim())
}
