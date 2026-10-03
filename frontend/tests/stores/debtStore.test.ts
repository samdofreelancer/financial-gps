import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { isPlannedValid, isRateValid, type DebtSchedule } from '@/api/debts'
import { useDebtStore } from '@/stores/debtStore'

vi.mock('@/api/debts', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/debts')>()
  return {
    ...actual,
    listDebts: vi.fn(),
    getDebtSummary: vi.fn(),
    getDebtSchedule: vi.fn(),
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

/** One period of the payment calendar, shaped exactly like the server response. */
function calendar(): DebtSchedule {
  return {
    status: 'AVAILABLE',
    payoffDate: '2028-08-02',
    numberOfPayments: 22,
    totalInterest: '46216000.00',
    finalPayment: '20570000.00',
    reasonCode: null,
    explanation: null,
    currency: 'VND',
    rows: [
      {
        period: 1,
        dueDate: '2026-10-28',
        payment: '20570000.00',
        principal: '16327757.86',
        interest: '4242242.14',
        endingBalance: '370000000.00',
      },
    ],
  }
}

describe('debt schedule state', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchSchedule carries the server rows across without computing anything', async () => {
    vi.mocked(api.getDebtSchedule).mockResolvedValue(calendar())

    const store = useDebtStore()
    const pending = store.fetchSchedule('d1')
    expect(store.scheduleLoading).toBe(true)

    await pending

    expect(api.getDebtSchedule).toHaveBeenCalledWith('d1')
    expect(store.schedule).toEqual(calendar())
    expect(store.scheduleLoading).toBe(false)
    expect(store.scheduleError).toBe('')
  })

  it('reports a failed fetch so the dialog can explain itself', async () => {
    vi.mocked(api.getDebtSchedule).mockRejectedValue(new Error('offline'))

    const store = useDebtStore()
    await store.fetchSchedule('d1')

    expect(store.schedule).toBeNull()
    expect(store.scheduleError).toBe('Could not reach the server. Check your connection and try again.')
  })

  it('keeps only the newest request when the reader flips between debts', async () => {
    let settleFirst!: (value: DebtSchedule) => void
    vi.mocked(api.getDebtSchedule)
      .mockImplementationOnce(() => new Promise<DebtSchedule>((resolve) => { settleFirst = resolve }))
      .mockResolvedValueOnce(calendar())

    const store = useDebtStore()
    const first = store.fetchSchedule('slow-debt')
    await store.fetchSchedule('fast-debt')
    expect(store.schedule).toEqual(calendar())

    settleFirst(calendar({ numberOfPayments: 99 }))
    await first

    // The slow response belongs to a dialog that has moved on — it must not win.
    expect(store.schedule?.numberOfPayments).toBe(22)
  })

  it('clearSchedule drops an in-flight response instead of reopening stale', async () => {
    let settle!: (value: DebtSchedule) => void
    vi.mocked(api.getDebtSchedule).mockReturnValue(
      new Promise<DebtSchedule>((resolve) => { settle = resolve }),
    )

    const store = useDebtStore()
    const pending = store.fetchSchedule('d1')
    store.clearSchedule()
    settle(calendar())
    await pending

    expect(store.schedule).toBeNull()
    expect(store.scheduleLoading).toBe(false)
    expect(store.scheduleError).toBe('')
  })
})
