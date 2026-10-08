import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import GoalForm from '@/components/goals/GoalForm.vue'

/** Fill a valid form so only the assertion under test can block submit. */
async function fillValidForm(wrapper: ReturnType<typeof mount>) {
  await wrapper.find('[data-testid="goal-name"]').setValue('Emergency Fund')
  const moneyInputs = wrapper.findAllComponents({ name: 'MoneyInput' })
  await moneyInputs[0].setValue('120000000')
  await moneyInputs[1].setValue('30000000')
}

describe('GoalForm', () => {
  it('blocks submit on empty name', async () => {
    const wrapper = mount(GoalForm, { props: {} })
    const button = wrapper.find('button[type="submit"]')
    expect(wrapper.text()).toContain('Tên mục tiêu là bắt buộc')
    expect((button.element as HTMLButtonElement).disabled).toBe(true)
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('blocks submit on non-canonical amounts', async () => {
    const wrapper = mount(GoalForm, { props: {} })
    await wrapper.find('[data-testid="goal-name"]').setValue('Emergency Fund')
    // MoneyInput only emits canonical decimals, so drive the guard through
    // an empty field: untouched amounts are not valid money.
    const button = wrapper.find('button[type="submit"]')
    expect((button.element as HTMLButtonElement).disabled).toBe(true)
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('disables submit while busy so double Enter cannot double-post', async () => {
    const wrapper = mount(GoalForm, { props: { busy: true } })
    await fillValidForm(wrapper)
    const button = wrapper.find('button[type="submit"]')
    expect((button.element as HTMLButtonElement).disabled).toBe(true)
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('emits a canonical payload when valid and idle', async () => {
    const wrapper = mount(GoalForm, { props: {} })
    await fillValidForm(wrapper)
    const button = wrapper.find('button[type="submit"]')
    expect((button.element as HTMLButtonElement).disabled).toBe(false)
    await wrapper.find('form').trigger('submit')
    const emitted = wrapper.emitted('submit')
    expect(emitted).toHaveLength(1)
    expect(emitted![0][0]).toMatchObject({
      name: 'Emergency Fund',
      goalType: 'EMERGENCY_FUND',
      targetDate: null,
      priority: 1,
    })
  })
})
