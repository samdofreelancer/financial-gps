import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import NextStepCard from './NextStepCard.vue'

/**
 * The card is intentionally neutral: the current API exposes no goals, debts or milestones, so the
 * "next step" can only be honest guidance, never a fabricated recommendation or score.
 */
describe('NextStepCard', () => {
  it('asks for a complete profile while the position has no income or expenses yet', async () => {
    const wrapper = mount(NextStepCard, { props: { complete: false } })

    expect(wrapper.text()).toContain('Your next step')
    expect(wrapper.text()).toContain('Complete your financial profile to see your next financial milestone.')

    await wrapper.find('button').trigger('click')

    expect(wrapper.emitted('complete-profile')).toHaveLength(1)
  })

  it('drops the call to action once the profile has been filled in', () => {
    const wrapper = mount(NextStepCard, { props: { complete: true } })

    expect(wrapper.text()).toContain('Your profile is complete.')
    expect(wrapper.find('button').exists()).toBe(false)
  })
})
