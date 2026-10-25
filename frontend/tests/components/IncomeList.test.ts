import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import IncomeList from '@/components/IncomeList.vue'
import type { ProfileLine } from '@/api/profile'

function line(overrides: Partial<ProfileLine> = {}): ProfileLine {
  return { id: 'i1', amount: '74.00', currency: 'VND', source: 'salary', provenance: 'actual', ...overrides }
}

function mountList(props: Record<string, unknown> = {}) {
  return mount(IncomeList, {
    props: { incomes: [line()], currency: 'VND', error: '', formOpen: false, ...props },
  })
}

describe('IncomeList', () => {
  it('shows each record as a readable row instead of a form', () => {
    const wrapper = mount(IncomeList, {
      props: {
        incomes: [line(), line({ id: 'i2', amount: '5.00', source: 'side business' })],
        currency: 'VND',
        error: '',
        formOpen: false,
      },
    })

    expect(wrapper.text()).toContain('Thu nhập')
    expect(wrapper.text()).toContain('salary')
    expect(wrapper.text()).toContain('74,00')
    expect(wrapper.text()).toContain('/ tháng')
    expect(wrapper.text()).toContain('side business')
    expect(wrapper.text()).toContain('Thu nhập định kỳ hàng tháng, đơn vị VND')
    expect(wrapper.find('input').exists()).toBe(false)
    expect(wrapper.findAll('[data-testid="income-item"]')).toHaveLength(2)
  })

  it('emits add, edit and remove for the existing handlers', async () => {
    const wrapper = mountList()

    await wrapper.find('[data-testid="add-income"]').trigger('click')
    await wrapper.find('[data-testid="edit-income-i1"]').trigger('click')
    await wrapper.find('[data-testid="remove-income-i1"]').trigger('click')

    expect(wrapper.emitted('add')).toHaveLength(1)
    expect(wrapper.emitted('edit')![0]).toEqual([expect.objectContaining({ id: 'i1' })])
    expect(wrapper.emitted('remove')![0]).toEqual(['i1'])
  })

  it('tells the user what to do next when nothing has been added yet', async () => {
    const wrapper = mountList({ incomes: [] })

    expect(wrapper.text()).toContain('Chưa có thu nhập nào.')
    expect(wrapper.text()).toContain('Thêm lương, thu nhập kinh doanh hoặc các khoản thu định kỳ khác.')

    const cta = wrapper.findAll('button').find((b) => b.text() === '+ Thêm thu nhập')!
    await cta.trigger('click')

    expect(wrapper.emitted('add')).toHaveLength(1)
  })

  it('keeps the empty state out of the way while the form is open', () => {
    const wrapper = mountList({ incomes: [], formOpen: true })

    expect(wrapper.text()).not.toContain('Chưa có thu nhập nào.')
  })

  it('surfaces a mutation error, but not twice while the form is open', () => {
    const closed = mountList({ error: 'Không xóa được khoản thu nhập.' })
    expect(closed.find('[role="alert"]').text()).toContain('Không xóa được khoản thu nhập.')

    const open = mountList({ error: 'Không xóa được khoản thu nhập.', formOpen: true })
    expect(open.find('[role="alert"]').exists()).toBe(false)
  })
})
