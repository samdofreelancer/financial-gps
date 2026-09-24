import type { Locator, Page } from '@playwright/test'

/**
 * Locator catalogue — the ONLY place that knows DOM hooks (data-testid, #id,
 * roles, CSS classes). Page/section objects reference these names; specs never
 * see a raw selector string.
 *
 * Verified against the live DOM (2026-09-23).
 */
export const sel = {
  register: {
    email: '#register-email',
    password: '#register-password',
    submit: { role: 'button' as const, name: 'Create account' },
    heading: { role: 'heading' as const, name: 'Create your account' },
  },
  login: {
    email: '#email',
    password: '#password',
    submit: { role: 'button' as const, name: 'Sign in' },
    validationAlert: { role: 'alert' as const },
  },
  dashboard: {
    heading: 'Welcome back',
    openProfileLink: { role: 'link' as const, name: 'Open financial profile' },
  },
  basics: {
    card: 'basics-card',
    edit: 'basics-edit',
    savings: '#savings',
    emergency: '#emergency',
    dependents: '#dependents',
    submit: { role: 'button' as const, name: 'Save basics' },
  },
  income: {
    card: 'income-card',
    add: 'add-income',
    row: 'income-item',
    amount: '#income-amount',
    source: '#income-source',
    submit: { role: 'button' as const, name: 'Add income' },
  },
  expense: {
    card: 'expense-card',
    add: 'add-expense',
    row: 'expense-item',
    amount: '#expense-amount',
    category: '#expense-category',
    type: '#expense-type',
    submit: { role: 'button' as const, name: 'Add expense' },
    cancel: { role: 'button' as const, name: 'Cancel' },
  },
  position: {
    card: 'position-card',
    stats: '.stats',
  },
  account: {
    heading: { role: 'heading' as const, name: 'Account' },
    avatarMenu: /Account menu for/,
    accountItem: { role: 'menuitem' as const, name: 'Account' },
    logoutItem: { role: 'menuitem' as const, name: 'Log out' },
    logoutButton: { role: 'button' as const, name: 'Log out' },
  },
  profileHeading: { role: 'heading' as const, name: 'Financial GPS' },
} as const

/** Typed accessor so sections stay free of raw `page.` selector strings. */
export function locators(page: Page): {
  byTestId: (id: string) => Locator
  text: (t: string | RegExp) => Locator
} {
  return {
    byTestId: (id: string) => page.getByTestId(id),
    text: (t: string | RegExp) => page.getByText(t),
  }
}
