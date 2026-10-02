import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { isPlannedValid, isRateValid } from '@/api/debts'
import { useDebtStore } from '@/stores/debtStore'

vi.mock('@/api/debts', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/debts')>()
  return {
    ...actual,
    listDebts: vi.fn(),
    getDebtSummary: vi.fn(),
    postDebt: vi.fn(),
    putDebt: vi.fn(),
    deleteDebt: vi.fn(),
  }
})

const api = await import('@/api/debts')

describe('debt client guards', () => {
  it('planned must cover minimum as decimal strings', () => {
    expect(isPlannedValid('1500000.00', '3000000.00')).toBe(true)
    expect(isPlannedValid('1500000.00', '1499999.99')).toBe(false)
    expect(isPlannedValid('abc', '1.00')).toBe(false)
  })

  it('rate accepts empty (missing) and 6dp fractions', () => {
    expect(isRateValid(null)).toBe(true)
    expect(isRateValid('')).toBe(true)
    expect(isRateValid('0.180000')).toBe(true)
    expect(isRateValid('0.1234567')).toBe(false)
    expect(isRateValid('abc')).toBe(false)
  })
})

describe('debt store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('refresh loads debts and summary', async () => {
    vi.mocked(api.listDebts).mockResolvedValue([])
    vi.mocked(api.getDebtSummary).mockResolvedValue({
      totalOutstandingDebt: '0.00',
      totalMinimumMonthlyPayment: '0.00',
      totalPlannedMonthlyPayment: '0.00',
      currency: 'VND',
      debtToIncome: { status: 'AVAILABLE', ratio: '0.0000', reasonCode: null, explanation: null },
      portfolioProjection: {
        status: 'COMPLETED',
        projectedDebtFreeDate: '2026-10-01',
        totalMonthsRemaining: 0,
        totalInterestRemaining: '0.00',
        reasonCode: 'DEBT_ALREADY_PAID',
        explanation: 'none',
        blockedDebts: [],
      },
      blockedDebtCount: 0,
      asOf: '2026-10-01',
    })

    const store = useDebtStore()
    await store.refresh()

    expect(store.summary?.totalOutstandingDebt).toBe('0.00')
    expect(store.error).toBe('')
  })
})
