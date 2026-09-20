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

function testId(wrapper: Awaited<ReturnType<typeof mountView>>, id: string) {
  const found = wrapper.find(`[data-testid="${id}"]`)
  if (!found.exists()) {
    throw new Error(`element not found: ${id}`)
  }
  return found
}

describe('ProfileView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the hero position from the server and reveals the stored basics on edit', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())

    const wrapper = await mountView()

    expect(api.getProfile).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Your financial position')
    expect(wrapper.text()).toContain('80,00')
    expect(wrapper.text()).toContain('50,00')
    expect(wrapper.text()).toContain('Free cash')
    expect(wrapper.text()).toContain('Money coming in')
    expect(wrapper.text()).toContain('Money going out')
    expect(wrapper.text()).toContain('salary')
    expect(wrapper.find('#savings').exists()).toBe(false)

    await testId(wrapper, 'basics-edit').trigger('click')

    expect((wrapper.find('#savings').element as HTMLInputElement).value).toBe('100.00')
    expect((wrapper.find('#emergency').element as HTMLInputElement).value).toBe('50.00')
    expect((wrapper.find('#dependents').element as HTMLInputElement).value).toBe('2')
  })

  it('shows friendly empty states before any data exists', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(
      view({
        savingsAmount: '0.00',
        emergencyFundAmount: '0.00',
        incomes: [],
        expenses: [],
      }),
    )

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('Start by adding your income and expenses.')
    expect(wrapper.text()).toContain('No income added yet.')
    expect(wrapper.text()).toContain('No expenses added yet.')
    expect(wrapper.text()).toContain('Complete your financial profile')
  })

  it('sends decimal strings unchanged when saving the basics and refetches', async () => {
    vi.mocked(api.getProfile)
      .mockResolvedValueOnce(view())
      .mockResolvedValue(view({ savingsAmount: '99.00' }))
    vi.mocked(api.putProfile).mockResolvedValue(view({ savingsAmount: '99.00' }))

    const wrapper = await mountView()
    await testId(wrapper, 'basics-edit').trigger('click')
    await wrapper.find('#savings').setValue('99.00')
    await button(wrapper, 'Save basics').trigger('click')
    await flushPromises()

    expect(api.putProfile).toHaveBeenCalledWith({
      currency: 'VND',
      savingsAmount: '99.00',
      emergencyFundAmount: '50.00',
      dependentsCount: 2,
    })
    expect(api.getProfile).toHaveBeenCalledTimes(2)
  })

  it('adds an income through POST and refetches', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())
    vi.mocked(api.postIncome).mockResolvedValue(undefined)

    const wrapper = await mountView()
    expect(wrapper.find('#income-amount').exists()).toBe(false)
    await testId(wrapper, 'add-income').trigger('click')
    await wrapper.find('#income-amount').setValue('12.00')
    await wrapper.find('#income-source').setValue('interest')
    await button(wrapper, 'Add income').trigger('click')
    await flushPromises()

    expect(api.postIncome).toHaveBeenCalledWith({ amount: '12.00', source: 'interest' })
    expect(api.getProfile).toHaveBeenCalledTimes(2)
  })

  it('blocks an invalid income without calling the API', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())

    const wrapper = await mountView()
    await testId(wrapper, 'add-income').trigger('click')
    await wrapper.find('#income-source').setValue('interest')
    await button(wrapper, 'Add income').trigger('click')
    await flushPromises()

    expect(api.postIncome).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Income needs a decimal amount and a source.')
  })

  it('edits an existing income through PUT and refetches', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())
    vi.mocked(api.putIncome).mockResolvedValue(undefined)

    const wrapper = await mountView()
    await testId(wrapper, 'edit-income-i1').trigger('click')
    await flushPromises()
    expect((wrapper.find('#income-amount').element as HTMLInputElement).value).toBe('74.00')

    await wrapper.find('#income-amount').setValue('80.00')
    await button(wrapper, 'Update income').trigger('click')
    await flushPromises()

    expect(api.putIncome).toHaveBeenCalledWith('i1', { amount: '80.00', source: 'salary' })
    expect(api.getProfile).toHaveBeenCalledTimes(2)
  })

  it('removes an income through DELETE and refetches', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())
    vi.mocked(api.deleteIncome).mockResolvedValue(undefined)

    const wrapper = await mountView()
    await testId(wrapper, 'remove-income-i1').trigger('click')
    await flushPromises()

    expect(api.deleteIncome).toHaveBeenCalledWith('i1')
    expect(api.getProfile).toHaveBeenCalledTimes(2)
  })

  it('adds and edits an expense through the existing API and refetches', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())
    vi.mocked(api.postExpense).mockResolvedValue(undefined)
    vi.mocked(api.putExpense).mockResolvedValue(undefined)

    const wrapper = await mountView()
    expect(wrapper.find('#expense-amount').exists()).toBe(false)
    await testId(wrapper, 'add-expense').trigger('click')
    await wrapper.find('#expense-amount').setValue('12.00')
    await wrapper.find('#expense-category').setValue('food')
    await wrapper.find('#expense-type').setValue('VARIABLE')
    await button(wrapper, 'Add expense').trigger('click')
    await flushPromises()

    expect(api.postExpense).toHaveBeenCalledWith({
      amount: '12.00',
      category: 'food',
      expenseType: 'VARIABLE',
    })
    expect(api.getProfile).toHaveBeenCalledTimes(2)

    await testId(wrapper, 'edit-expense-e1').trigger('click')
    await flushPromises()
    expect((wrapper.find('#expense-amount').element as HTMLInputElement).value).toBe('30.00')
    await wrapper.find('#expense-amount').setValue('35.00')
    await button(wrapper, 'Update expense').trigger('click')
    await flushPromises()

    expect(api.putExpense).toHaveBeenCalledWith('e1', {
      amount: '35.00',
      category: 'rent',
      expenseType: 'FIXED',
    })
  })

  it('renders the financial position exactly once', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())

    const wrapper = await mountView()

    const positionHeadings = wrapper
      .findAll('h2')
      .map((node) => node.text())
      .filter((text) => text.startsWith('Your financial position'))
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
    await testId(wrapper, 'add-income').trigger('click')
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
    await testId(wrapper, 'remove-income-i1').trigger('click')
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
    expect(wrapper.text()).toContain('Your next step')
  })

  it('orders the page from "where am I" to "what next"', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())

    const wrapper = await mountView()

    const headings = wrapper.findAll('h2').map((node) => node.text())
    expect(headings).toEqual([
      'Your financial position',
      'Your financial basics',
      'Money coming in',
      'Money going out',
      'Your next step',
    ])
  })

  it('keeps every section read-only until the user explicitly edits it', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(view())

    const wrapper = await mountView()

    // No editing controls at all on first paint — just the facts and the rows.
    expect(wrapper.find('input').exists()).toBe(false)
    expect(wrapper.find('select').exists()).toBe(false)
    expect(wrapper.findAll('[data-testid="income-item"]')).toHaveLength(1)

    await testId(wrapper, 'edit-income-i1').trigger('click')
    await flushPromises()

    expect((wrapper.find('#income-amount').element as HTMLInputElement).value).toBe('74.00')
  })

  it('guides a fresh account to save the basics when the server reports the missing profile record', async () => {
    vi.mocked(api.getProfile).mockResolvedValue(
      view({ incomes: [], expenses: [], savingsAmount: '0.00', emergencyFundAmount: '0.00' }),
    )
    vi.mocked(api.postIncome).mockRejectedValue({
      response: {
        status: 404,
        data: { code: 'RESOURCE_NOT_FOUND', title: 'Resource not found', detail: 'Resource not found.' },
      },
    })

    const wrapper = await mountView()
    await testId(wrapper, 'add-income').trigger('click')
    await wrapper.find('#income-amount').setValue('12.00')
    await wrapper.find('#income-source').setValue('interest')
    await button(wrapper, 'Add income').trigger('click')
    await flushPromises()

    expect(api.postIncome).toHaveBeenCalled()
    expect(wrapper.text()).toContain('Save your financial basics first')
    expect(wrapper.find('#savings').exists()).toBe(true)
  })
})

