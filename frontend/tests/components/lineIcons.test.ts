import { describe, expect, it } from 'vitest'
import { expenseIcon, incomeIcon } from '@/components/lineIcons'

/**
 * Icons are decoration derived from the free-text source/category: they must never be mistaken for
 * stored data, and an unknown word must still produce an icon for its section.
 */
describe('lineIcons', () => {
  it('maps common income words to an icon', () => {
    expect(incomeIcon('Salary')).toBe('💼')
    expect(incomeIcon('side business')).toBe('🏪')
    expect(incomeIcon('Freelance project')).toBe('📈')
  })

  it('maps common expense words to an icon', () => {
    expect(expenseIcon('Rent')).toBe('🏠')
    expect(expenseIcon('food')).toBe('🍚')
    expect(expenseIcon('electricity bill')).toBe('💡')
  })

  it('falls back to the neutral icon for unknown or missing text', () => {
    expect(incomeIcon('misc')).toBe('💰')
    expect(incomeIcon(undefined)).toBe('💰')
    expect(incomeIcon('')).toBe('💰')
    expect(expenseIcon('misc')).toBe('🧾')
    expect(expenseIcon(undefined)).toBe('🧾')
  })
})
