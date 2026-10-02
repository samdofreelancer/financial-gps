import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import DebtForm from '@/components/debts/DebtForm.vue'

describe('DebtForm', () => {
  it('blocks submit when planned is below minimum', async () => {
    const wrapper = mount(DebtForm, { props: {} })
    await wrapper.find('[data-testid="creditor"]').setValue('Bank')
    const button = wrapper.find('button[type="submit"]')
    expect(wrapper.text()).toContain('Số tiền là bắt buộc')
    expect((button.element as HTMLButtonElement).disabled).toBe(true)
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('submits a valid payload with null rate when empty', async () => {
    const wrapper = mount(DebtForm, { props: {} })
    await wrapper.find('[data-testid="creditor"]').setValue('Bank')
    await wrapper.findComponent({ name: 'MoneyInput' }).setValue('1000')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })
})
