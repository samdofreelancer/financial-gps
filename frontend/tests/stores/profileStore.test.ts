import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '@/api/profile'
import type { ProfileView } from '@/api/profile'
import { useProfileStore } from '@/stores/profileStore'

vi.mock('@/api/profile', () => ({
  getProfile: vi.fn(),
  putProfile: vi.fn(),
  putIncome: vi.fn(),
  postIncome: vi.fn(),
  deleteIncome: vi.fn(),
  putExpense: vi.fn(),
  postExpense: vi.fn(),
  deleteExpense: vi.fn(),
}))

function view(overrides: Partial<ProfileView> = {}): ProfileView {
  return {
    currency: 'VND',
    savingsAmount: '100.00',
    emergencyFundAmount: '50.00',
    dependentsCount: 2,
    incomes: [],
    expenses: [],
    totalIncome: { amount: '80.00', currency: 'VND', provenance: 'calculated' },
    totalExpenses: { amount: '50.00', currency: 'VND', provenance: 'calculated' },
    netCashFlow: { amount: '30.00', currency: 'VND', provenance: 'calculated' },
    availableCapacity: { amount: '30.00', currency: 'VND', provenance: 'calculated' },
    provenance: [],
    asOf: '2026-09-16',
    ...overrides,
  }
}

describe('profileStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('keeps the server profile as the single source of truth', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())

    const store = useProfileStore()
    await store.refresh()

    expect(store.profile).toEqual(view())
    expect(store.loading).toBe(false)
    expect(store.error).toBe('')
  })

  it('reports a load failure without inventing numbers', async () => {
    vi.mocked(api.getProfile).mockRejectedValue(new Error('boom'))

    const store = useProfileStore()
    await store.refresh()

    expect(store.profile).toBeNull()
    expect(store.error).not.toBe('')
    expect(store.loading).toBe(false)
  })

  it('exposes no client-side totals of its own', () => {
    const store = useProfileStore()

    expect('totalIncome' in store).toBe(false)
    expect('netCashFlow' in store).toBe(false)
    expect('availableCapacity' in store).toBe(false)
    // Pinia adds $/internal keys; only the public store contract is asserted here.
    const publicKeys = Object.keys(store)
      .filter((key) => !key.startsWith('$') && !key.startsWith('_'))
      .sort()
    expect(publicKeys).toEqual(
      [
        'addExpense',
        'addIncome',
        'error',
        'loading',
        'profile',
        'refresh',
        'removeExpense',
        'removeIncome',
        'saveProfile',
        'updateExpense',
        'updateIncome',
      ].sort(),
    )
  })

  it('refetches the server position after adding an income', async () => {
    vi.mocked(api.getProfile)
      .mockResolvedValueOnce(view())
      .mockResolvedValueOnce(
        view({ totalIncome: { amount: '154.00', currency: 'VND', provenance: 'calculated' } }),
      )
    vi.mocked(api.postIncome).mockResolvedValue(undefined)

    const store = useProfileStore()
    await store.refresh()
    await store.addIncome({ amount: '74.00', source: 'salary' })

    expect(api.postIncome).toHaveBeenCalledWith({ amount: '74.00', source: 'salary' })
    expect(api.getProfile).toHaveBeenCalledTimes(2)
    expect(store.profile?.totalIncome.amount).toBe('154.00')
  })

  it('refetches after updating and removing lines', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())
    vi.mocked(api.putIncome).mockResolvedValue(undefined)
    vi.mocked(api.deleteIncome).mockResolvedValue(undefined)
    vi.mocked(api.putExpense).mockResolvedValue(undefined)
    vi.mocked(api.deleteExpense).mockResolvedValue(undefined)

    const store = useProfileStore()
    await store.updateIncome('i1', { amount: '80.00', source: 'salary' })
    await store.removeIncome('i1')
    await store.updateExpense('e1', { amount: '31.00', category: 'rent', expenseType: 'FIXED' })
    await store.removeExpense('e1')

    expect(api.putIncome).toHaveBeenCalledWith('i1', { amount: '80.00', source: 'salary' })
    expect(api.deleteIncome).toHaveBeenCalledWith('i1')
    expect(api.putExpense).toHaveBeenCalledWith('e1', {
      amount: '31.00',
      category: 'rent',
      expenseType: 'FIXED',
    })
    expect(api.deleteExpense).toHaveBeenCalledWith('e1')
    expect(api.getProfile).toHaveBeenCalledTimes(4)
  })

  it('stores the profile the server saved, without deriving anything', async () => {
    vi.mocked(api.putProfile).mockResolvedValue(view({ savingsAmount: '99.00' }))

    const store = useProfileStore()
    await store.saveProfile({
      currency: 'VND',
      savingsAmount: '99.00',
      emergencyFundAmount: '0.00',
      dependentsCount: 0,
    })

    expect(api.putProfile).toHaveBeenCalledWith({
      currency: 'VND',
      savingsAmount: '99.00',
      emergencyFundAmount: '0.00',
      dependentsCount: 0,
    })
    expect(store.profile?.savingsAmount).toBe('99.00')
  })
})