import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ExpenseList from '@/components/ExpenseList.vue'
import type { ProfileLine } from '@/api/profile'

function line(overrides: Partial<ProfileLine> = {}): ProfileLine {
  return {
    id: 'e1',
    amount: '30.00',
    currency: 'VND',
    category: 'rent',
    expenseType: 'FIXED',
    provenance: 'actual',
    ...overrides,
  }
}

function mountList(props: Record<string, unknown> = {}) {
  return mount(ExpenseList, {
    props: { expenses: [line()], currency: 'VND', error: '', formOpen: false, ...props },
  })
}

describe('ExpenseList', () => {
  it('shows the stored records with their Fixed/Variable meaning', () => {
    const wrapper = mount(ExpenseList, {
      props: {
        expenses: [line(), line({ id: 'e2', amount: '3.00', category: 'food', expenseType: 'VARIABLE' })],
        currency: 'VND',
        error: '',
        formOpen: false,
      },
    })

    expect(wrapper.text()).toContain('Money going out')
    expect(wrapper.text()).toContain('rent')
    expect(wrapper.text()).toContain('30,00')
    expect(wrapper.text()).toContain('Fixed')
    expect(wrapper.text()).toContain('food')
    expect(wrapper.text()).toContain('Variable')
    expect(wrapper.findAll('[data-testid="expense-item"]')).toHaveLength(2)
  })

  it('does not label a record the server never classified', () => {
    const wrapper = mountList({ expenses: [line({ expenseType: undefined })] })

    expect(wrapper.find('.tag').exists()).toBe(false)
  })

  it('emits add, edit and remove for the existing handlers', async () => {
    const wrapper = mountList()

    await wrapper.find('[data-testid="add-expense"]').trigger('click')
    await wrapper.find('[data-testid="edit-expense-e1"]').trigger('click')
    await wrapper.find('[data-testid="remove-expense-e1"]').trigger('click')

    expect(wrapper.emitted('add')).toHaveLength(1)
    expect(wrapper.emitted('edit')![0]).toEqual([expect.objectContaining({ id: 'e1' })])
    expect(wrapper.emitted('remove')![0]).toEqual(['e1'])
  })

  it('tells the user what to do next when nothing has been added yet', async () => {
    const wrapper = mountList({ expenses: [] })

    expect(wrapper.text()).toContain('No expenses added yet.')
    expect(wrapper.text()).toContain('Add your recurring monthly expenses.')

    const cta = wrapper.findAll('button').find((b) => b.text() === '+ Add expense')!
    await cta.trigger('click')

    expect(wrapper.emitted('add')).toHaveLength(1)
  })

  it('keeps the empty state out of the way while the form is open', () => {
    const wrapper = mountList({ expenses: [], formOpen: true })

    expect(wrapper.text()).not.toContain('No expenses added yet.')
  })
})
