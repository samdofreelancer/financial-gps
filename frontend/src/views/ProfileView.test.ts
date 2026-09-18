import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '../api/profile'
import type { ProfileView } from '../api/profile'
import ProfileViewComponent from './ProfileView.vue'

vi.mock('../api/profile', async () => {
  const actual = await vi.importActual<typeof import('../api/profile')>('../api/profile')
  return {
    ...actual,
    getProfile: vi.fn(),
    putProfile: vi.fn(),
    putIncome: vi.fn(),
    postIncome: vi.fn(),
    deleteIncome: vi.fn(),
    putExpense: vi.fn(),
    postExpense: vi.fn(),
    deleteExpense: vi.fn(),
  }
})

function view(overrides: Partial<ProfileView> = {}): ProfileView {
  return {
    currency: 'VND',
    savingsAmount: '100.00',
    emergencyFundAmount: '50.00',
    dependentsCount: 2,
    incomes: [
      { id: 'i1', amount: '74.00', currency: 'VND', source: 'salary', provenance: 'actual' },
    ],
    expenses: [
      {
        id: 'e1',
        amount: '30.00',
        currency: 'VND',
        category: 'rent',
        expenseType: 'FIXED',
        provenance: 'actual',
      },
    ],
    totalIncome: { amount: '80.00', currency: 'VND', provenance: 'calculated' },
    totalExpenses: { amount: '50.00', currency: 'VND', provenance: 'calculated' },
    netCashFlow: { amount: '30.00', currency: 'VND', provenance: 'calculated' },
    availableCapacity: { amount: '30.00', currency: 'VND', provenance: 'calculated' },
    provenance: [],
    asOf: '2026-09-16',
    ...overrides,
  }
}

async function mountView() {
  const wrapper = mount(ProfileViewComponent, { global: { plugins: [createPinia()] } })
  await flushPromises()
  return wrapper
}

function button(wrapper: Awaited<ReturnType<typeof mountView>>, text: string) {
  const found = wrapper.findAll('button').find((node) => node.text() === text)
  if (!found) {
    throw new Error(`button not found: ${text}`)
  }
  return found
}

describe('ProfileView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the server-calculated position and the stored facts', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())

    const wrapper = await mountView()

    expect(api.getProfile).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('80,00')
    expect(wrapper.text()).toContain('50,00')
    expect(wrapper.text()).toContain('Net cash flow')
    expect(wrapper.text()).toContain('Available capacity')
    expect(wrapper.text()).toContain('calculated')
    expect(wrapper.text()).toContain('salary')
    expect((wrapper.find('#savings').element as HTMLInputElement).value).toBe('100.00')
    expect((wrapper.find('#dependents').element as HTMLInputElement).value).toBe('2')
  })

  it('sends decimal strings unchanged when saving the profile and refetches', async () => {
    vi.mocked(api.getProfile)
      .mockResolvedValueOnce(view())
      .mockResolvedValue(view({ savingsAmount: '99.00' }))
    vi.mocked(api.putProfile).mockResolvedValue(view({ savingsAmount: '99.00' }))

    const wrapper = await mountView()
    await wrapper.find('#savings').setValue('99.00')
    await button(wrapper, 'Save profile').trigger('click')
    await flushPromises()

    expect(api.putProfile).toHaveBeenCalledWith({
      currency: 'VND',
      savingsAmount: '99.00',
      emergencyFundAmount: '50.00',
      dependentsCount: 2,
    })
    expect(api.getProfile).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('99,00')
  })

  it('rejects a non-decimal amount in the browser instead of sending it', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())

    const wrapper = await mountView()
    await wrapper.find('#savings').setValue('10,50')
    await button(wrapper, 'Save profile').trigger('click')
    await flushPromises()

    expect(api.putProfile).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('decimal')
  })

  it('adds an income with the typed strings and refetches the totals', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())
    vi.mocked(api.postIncome).mockResolvedValue(undefined)

    const wrapper = await mountView()
    await wrapper.find('#income-amount').setValue('0.10')
    await wrapper.find('#income-source').setValue('interest')
    await button(wrapper, 'Add income').trigger('click')
    await flushPromises()

    expect(api.postIncome).toHaveBeenCalledWith({ amount: '0.10', source: 'interest' })
    expect(api.getProfile).toHaveBeenCalledTimes(2)
  })

  it('does not send an income without an amount and a source', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())

    const wrapper = await mountView()
    await wrapper.find('#income-amount').setValue('12.00')
    await button(wrapper, 'Add income').trigger('click')
    await flushPromises()

    expect(api.postIncome).not.toHaveBeenCalled()
  })

  it('edits an existing income through PUT and refetches', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())
    vi.mocked(api.putIncome).mockResolvedValue(undefined)

    const wrapper = await mountView()
    await button(wrapper, 'Edit').trigger('click')
    await flushPromises()
    expect((wrapper.find('#income-amount').element as HTMLInputElement).value).toBe('74.00')

    await wrapper.find('#income-amount').setValue('80.00')
    await button(wrapper, 'Update income').trigger('click')
    await flushPromises()

    expect(api.putIncome).toHaveBeenCalledWith('i1', { amount: '80.00', source: 'salary' })
    expect(api.getProfile).toHaveBeenCalledTimes(2)
  })

  it('removes a line through DELETE and refetches', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())
    vi.mocked(api.deleteIncome).mockResolvedValue(undefined)

    const wrapper = await mountView()
    await button(wrapper, 'Remove').trigger('click')
    await flushPromises()

    expect(api.deleteIncome).toHaveBeenCalledWith('i1')
    expect(api.getProfile).toHaveBeenCalledTimes(2)
  })

  it('renders the current position exactly once', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())

    const wrapper = await mountView()

    const positionHeadings = wrapper
      .findAll('h2')
      .map((node) => node.text())
      .filter((text) => text.startsWith('Current position'))
    expect(positionHeadings).toHaveLength(1)
  })

  it('surfaces the server error text for a rejected mutation instead of leaving it unhandled', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())
    vi.mocked(api.postIncome).mockRejectedValue({
      response: {
        status: 400,
        data: { code: 'VALIDATION_FAILED', detail: 'Request body is invalid.' },
      },
    })

    const wrapper = await mountView()
    await wrapper.find('#income-amount').setValue('0.10')
    await wrapper.find('#income-source').setValue('interest')
    await button(wrapper, 'Add income').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Request body is invalid.')
    expect(api.getProfile).toHaveBeenCalledTimes(1)
  })

  it('reports a failed removal through the shared error mapping', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())
    vi.mocked(api.deleteIncome).mockRejectedValue(new Error('offline'))

    const wrapper = await mountView()
    await button(wrapper, 'Remove').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Could not reach the server')
  })

  it('displays server totals even when they do not match the listed lines (no client math)', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(
      view({
        totalIncome: { amount: '1.00', currency: 'VND', provenance: 'calculated' },
        netCashFlow: { amount: '-44.00', currency: 'VND', provenance: 'calculated' },
        availableCapacity: { amount: '0.00', currency: 'VND', provenance: 'calculated' },
      }),
    )

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('1,00')
    expect(wrapper.text()).toContain('-44,00')
    expect(wrapper.text()).not.toContain('80,00')
  })
})
