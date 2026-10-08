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
    positionCard: 'position-card',
    positionSummary: 'position-summary',
    mandatoryPayment: 'mandatory-payment',
    netCashFlow: 'net-cash-flow',
  },
  debts: {
    heading: { role: 'heading' as const, name: 'Quản lý nợ', exact: true },
    add: { role: 'button' as const, name: 'Thêm khoản nợ', exact: true },
    creditor: 'creditor',
    type: 'debt-type',
    balance: '#debt-balance',
    min: '#debt-min',
    planned: '#debt-planned',
    rate: 'rate',
    save: { role: 'button' as const, name: 'Lưu', exact: true },
    cancel: { role: 'button' as const, name: 'Hủy', exact: true },
    edit: { role: 'button' as const, name: 'Sửa', exact: true },
    remove: { role: 'button' as const, name: 'Xóa', exact: true },
    item: 'debt-item',
    totalDebt: 'total-debt',
    totalMinimum: 'total-minimum',
    dti: 'dti',
    payoffDate: 'payoff-date',
    blockerAlert: 'blocker-alert',
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
  goals: {
    heading: { role: 'heading' as const, name: 'Mục tiêu tài chính', exact: true },
    add: { role: 'button' as const, name: 'Thêm mục tiêu', exact: true },
    name: 'goal-name',
    type: 'goal-type',
    priority: 'goal-priority',
    target: '#goal-target',
    current: '#goal-current',
    date: 'goal-date',
    save: { role: 'button' as const, name: 'Lưu', exact: true },
    cancel: { role: 'button' as const, name: 'Hủy', exact: true },
    edit: { role: 'button' as const, name: 'Sửa', exact: true },
    capacity: { role: 'button' as const, name: 'Capacity', exact: true },
    remove: { role: 'button' as const, name: 'Lưu trữ', exact: true },
    confirmRemove: { role: 'button' as const, name: 'Lưu trữ', exact: true },
    item: 'goal-row',
    empty: 'goal-empty',
    remaining: 'goal-remaining',
    progressValue: 'goal-progress-value',
    capacityCard: 'goal-capacity',
    coverage: 'capacity-coverage',
    required: 'capacity-required',
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
