import { test, expect } from '@playwright/test'
import { registerFreshAccount, E2E_PASSWORD } from '../support/auth-flow'

/**
 * Financial GPS E2E Tests
 * Tests the GPS view against the Vite preview server (http://127.0.0.1:4173)
 * 
 * To run:
 * 1. Start backend: cd backend && ./mvnw spring-boot:run
 * 2. Start frontend preview: cd frontend && npm run preview
 * 3. Run tests: cd e2e && npm run test:only -- gps.spec.ts
 * 
 * Or use headed mode for debugging: npm run test:headed -- gps.spec.ts
 */

test.describe.configure({ retries: 0 })

/**
 * Seeds a basic Financial Profile (income + expense) through the API so GPS
 * position/capacity/ETA sections render with real numbers. Uses same-origin
 * fetch with the double-submit CSRF handshake, mirroring the SPA client.
 */
async function seedBasicProfile(
  page: import('@playwright/test').Page,
  income = '74000000.00',
  expense = '43000000.00',
) {
  await page.evaluate(
    async ({ incomeAmount, expenseAmount }) => {
      const xsrf = () =>
        document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)?.[1] ?? ''
      const api = async (method: string, path: string, body?: unknown) => {
        await fetch('/api/v1/auth/csrf', { credentials: 'include' })
        const r = await fetch(path, {
          method,
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': decodeURIComponent(xsrf()),
          },
          body: body === undefined ? undefined : JSON.stringify(body),
        })
        if (!r.ok) {
          throw new Error(`${method} ${path} -> ${r.status}`)
        }
      }
      await api('PUT', '/api/v1/profile', {
        currency: 'VND',
        savingsAmount: '20000000.00',
        emergencyFundAmount: '20000000.00',
        dependentsCount: 0,
      })
      await api('POST', '/api/v1/incomes', { amount: incomeAmount, source: 'Salary' })
      await api('POST', '/api/v1/expenses', {
        amount: expenseAmount,
        category: 'Living',
        expenseType: 'FIXED',
      })
    },
    { incomeAmount: income, expenseAmount: expense },
  )
}

test.describe('Financial GPS', () => {
  let email: string

  test.beforeEach(async ({ page }) => {
    // Register and login a fresh user for each test
    const { email: testEmail } = await registerFreshAccount(page, `gps_test_${Date.now()}`)
    email = testEmail
    
    // Navigate to GPS page
    await page.goto('/gps')
    await page.waitForLoadState('networkidle')
  })

  test.describe('Empty state (no goals)', () => {
    test('shows empty state prompting goal creation', async ({ page }) => {
      // Should show empty state with create goal link
      await expect(page.locator('.empty-state')).toBeVisible()
      await expect(page.locator('.empty-state h2')).toContainText('No destination selected')
      await expect(page.locator('.empty-state a[href="/goals"]')).toBeVisible()
    })
  })

  test.describe('With goals', () => {
    test.beforeEach(async ({ page }) => {
      // Seed profile first so GPS has real position/capacity numbers (quickstart §4)
      await seedBasicProfile(page)

      // Create a test goal via API or UI
      // First go to goals page and create one
      await page.goto('/goals')
      await page.waitForLoadState('networkidle')
      
      // Create an amount-based goal
      await page.click('button:has-text("Thêm mục tiêu")')
      await page.waitForSelector('[role="dialog"]')
      
      await page.fill('[data-testid="goal-name"]', 'Emergency Fund')
      await page.selectOption('[data-testid="goal-type"]', 'EMERGENCY_FUND')
      await page.fill('#goal-target', '50000000')
      await page.fill('#goal-current', '10000000')
      await page.fill('[data-testid="goal-date"]', '2027-12-31')
      await page.fill('[data-testid="goal-priority"]', '1')

      // Gate on the create POST: goto /gps must only happen after the goal is
      // committed, otherwise the one-shot fetchGoals on mount can win the race
      // and the page renders empty forever.
      const created = page.waitForResponse(
        (res) => res.url().includes('/api/v1/goals') && res.request().method() === 'POST',
      )
      await page.click('button[type="submit"]:has-text("Lưu")')
      const createResponse = await created
      expect(createResponse.ok()).toBe(true)
      await page.waitForLoadState('networkidle')

      // Go back to GPS
      await page.goto('/gps')
      await page.waitForLoadState('networkidle')
    })

    test('loads GPS data for selected goal', async ({ page }) => {
      // Should show goal selector with our goal
      await expect(page.locator('#goal-select')).toBeVisible()
      await expect(page.locator('#goal-select option')).toContainText(['Emergency Fund'])

      // Select the goal
      await page.selectOption('#goal-select', { label: 'Emergency Fund (EMERGENCY_FUND) - ACTIVE' })

      // Wait for GPS data to load
      await expect(page.locator('.gps-result')).toBeVisible({ timeout: 30000 })
      
      // Verify key sections are present
      await expect(page.locator('.position-destination')).toBeVisible()
      await expect(page.locator('.status-eta')).toBeVisible()
      await expect(page.locator('.blockers-actions')).toBeVisible()
      await expect(page.locator('.explanations')).toBeVisible()
      await expect(page.locator('.provenance')).toBeVisible()
    })

    test('shows current position with all fields', async ({ page }) => {
      await expect(page.locator('.gps-result')).toBeVisible()
      
      // Check position card fields
      const positionCard = page.locator('.position-card')
      await expect(positionCard.locator('text=Income')).toBeVisible()
      await expect(positionCard.locator('text=Expenses')).toBeVisible()
      await expect(positionCard.locator('text=Mandatory Payment')).toBeVisible()
      await expect(positionCard.locator('text=Net Cash Flow')).toBeVisible()
      await expect(positionCard.locator('text=Available Capacity')).toBeVisible()
      await expect(positionCard.locator('text=Total Debt')).toBeVisible()
    })

    test('shows destination with distance and progress', async ({ page }) => {
      await expect(page.locator('.gps-result')).toBeVisible()
      
      const destCard = page.locator('.destination-card')
      await expect(destCard.locator('text=Emergency Fund')).toBeVisible()
      await expect(destCard.locator('.distance-main')).toBeVisible()
      await expect(destCard.locator('.progress-bar')).toBeVisible()
    })

    test('shows status badge with explanation', async ({ page }) => {
      await expect(page.locator('.gps-result')).toBeVisible()
      
      const statusCard = page.locator('.status-card')
      await expect(statusCard.locator('.status-badge')).toBeVisible()
      await expect(statusCard.locator('.status-label')).toBeVisible()
      await expect(statusCard.locator('.status-explanation')).toBeVisible()
    })

    test('shows ETA section', async ({ page }) => {
      await expect(page.locator('.gps-result')).toBeVisible()
      
      const etaCard = page.locator('.eta-card')
      await expect(etaCard).toBeVisible()
      // Either calculated or unavailable
      await expect(etaCard.locator('h3')).toContainText('Estimated Arrival')
    })

    test('shows capacity comparison', async ({ page }) => {
      await expect(page.locator('.gps-result')).toBeVisible()
      
      const capCard = page.locator('.capacity-card')
      await expect(capCard).toBeVisible()
      await expect(capCard.locator('text=Required / Month')).toBeVisible()
      await expect(
        capCard.locator('.capacity-row:has-text("Available Capacity")'),
      ).toBeVisible()
    })

    test('shows blockers panel', async ({ page }) => {
      await expect(page.locator('.gps-result')).toBeVisible()
      
      const blockerPanel = page.locator('.blocker-panel')
      await expect(blockerPanel).toBeVisible()
      await expect(blockerPanel.locator('h3')).toContainText('Blockers')
    })

    test('shows next actions panel', async ({ page }) => {
      await expect(page.locator('.gps-result')).toBeVisible()
      
      const actionPanel = page.locator('.action-panel')
      await expect(actionPanel).toBeVisible()
      await expect(actionPanel.locator('h3')).toContainText('Suggested Next Steps')
    })

    test('shows explanations with provenance', async ({ page }) => {
      await expect(page.locator('.gps-result')).toBeVisible()
      
      const expPanel = page.locator('.explanation-panel')
      await expect(expPanel).toBeVisible()
      
      // Should have multiple explanation items
      const items = expPanel.locator('.explanation-item')
      await expect(items.first()).toBeVisible()
      
      // Check provenance badges (at least one; the panel lists many)
      await expect(expPanel.locator('.provenance-kind').first()).toBeVisible()
    })

    test('shows provenance table', async ({ page }) => {
      await expect(page.locator('.gps-result')).toBeVisible()
      
      const provenanceSection = page.locator('.provenance')
      await expect(provenanceSection).toBeVisible()
      
      // Click to expand
      await provenanceSection.locator('summary').click()
      
      // Table should be visible with at least one row
      await expect(provenanceSection.locator('.provenance-table')).toBeVisible()
      await expect(provenanceSection.locator('.provenance-table tbody tr')).not.toHaveCount(0)
    })
  })

  test.describe('Debt Freedom goal', () => {
    test.beforeEach(async ({ page }) => {
      // Create a debt freedom goal
      await page.goto('/goals')
      await page.waitForLoadState('networkidle')
      
      await page.click('button:has-text("Thêm mục tiêu")')
      await page.waitForSelector('[role="dialog"]')
      
      await page.fill('[data-testid="goal-name"]', 'Debt Freedom')
      await page.selectOption('[data-testid="goal-type"]', 'DEBT_FREEDOM')
      await page.fill('[data-testid="goal-date"]', '2028-12-31')
      await page.fill('[data-testid="goal-priority"]', '1')

      const created = page.waitForResponse(
        (res) => res.url().includes('/api/v1/goals') && res.request().method() === 'POST',
      )
      await page.click('button[type="submit"]:has-text("Lưu")')
      const createResponse = await created
      expect(createResponse.ok()).toBe(true)
      await page.waitForLoadState('networkidle')

      await page.goto('/gps')
      await page.waitForLoadState('networkidle')
    })

    test('shows debt freedom destination type', async ({ page }) => {
      await expect(page.locator('.gps-result')).toBeVisible()
      
      // Should show debt freedom badge
      await expect(page.locator('.destination-card .badge-info')).toContainText('Debt Freedom')
      
      // Progress should show PROGRESS_NOT_MEASURABLE
      await expect(page.locator('.progress-text')).toContainText('PROGRESS_NOT_MEASURABLE')
    })

    test('capacity comparison shows NOT_APPLICABLE for debt freedom', async ({ page }) => {
      await expect(page.locator('.gps-result')).toBeVisible()
      
      const capCard = page.locator('.capacity-card')
      await expect(capCard.locator('.not-applicable')).toBeVisible()
      await expect(capCard.locator('text=compared by date, not monthly money')).toBeVisible()
    })
  })

  test.describe('Goal switching', () => {
    test('can switch between goals', async ({ page }) => {
      // Create two goals first
      await page.goto('/goals')
      await page.waitForLoadState('networkidle')
      
      // Goal 1
      await page.click('button:has-text("Thêm mục tiêu")')
      await page.waitForSelector('[role="dialog"]')
      await page.fill('[data-testid="goal-name"]', 'Goal One')
      await page.selectOption('[data-testid="goal-type"]', 'EMERGENCY_FUND')
      await page.fill('#goal-target', '10000000')
      await page.fill('#goal-current', '5000000')
      await page.fill('[data-testid="goal-priority"]', '1')
      const createdOne = page.waitForResponse(
        (res) => res.url().includes('/api/v1/goals') && res.request().method() === 'POST',
      )
      await page.click('button[type="submit"]:has-text("Lưu")')
      expect((await createdOne).ok()).toBe(true)
      await page.waitForLoadState('networkidle')
      
      // Goal 2
      await page.click('button:has-text("Thêm mục tiêu")')
      await page.waitForSelector('[role="dialog"]')
      await page.fill('[data-testid="goal-name"]', 'Goal Two')
      await page.selectOption('[data-testid="goal-type"]', 'RETIREMENT')
      await page.fill('#goal-target', '100000000')
      await page.fill('#goal-current', '10000000')
      await page.fill('[data-testid="goal-priority"]', '2')
      const createdTwo = page.waitForResponse(
        (res) => res.url().includes('/api/v1/goals') && res.request().method() === 'POST',
      )
      await page.click('button[type="submit"]:has-text("Lưu")')
      expect((await createdTwo).ok()).toBe(true)
      await page.waitForLoadState('networkidle')
      
      // Go to GPS
      await page.goto('/gps')
      await page.waitForLoadState('networkidle')
      
      // Should have both goals in selector (exactly the two just created)
      await expect(page.locator('#goal-select option')).toHaveCount(2)
      
      // Select first goal
      await page.selectOption('#goal-select', { index: 0 })
      await page.waitForLoadState('networkidle')
      await expect(page.locator('.destination-card h4')).toContainText('Goal One')

      // Select second goal
      await page.selectOption('#goal-select', { index: 1 })
      await page.waitForLoadState('networkidle')
      await expect(page.locator('.destination-card h4')).toContainText('Goal Two')
    })
  })

  test.describe('asOf date parameter', () => {
    test('accepts asOf date in URL', async ({ page }) => {
      // Create a goal first
      await page.goto('/goals')
      await page.waitForLoadState('networkidle')
      
      await page.click('button:has-text("Thêm mục tiêu")')
      await page.waitForSelector('[role="dialog"]')
      await page.fill('[data-testid="goal-name"]', 'Test Goal')
      await page.selectOption('[data-testid="goal-type"]', 'EMERGENCY_FUND')
      await page.fill('#goal-target', '10000000')
      await page.fill('#goal-current', '1000000')
      await page.fill('[data-testid="goal-priority"]', '1')
      const created = page.waitForResponse(
        (res) => res.url().includes('/api/v1/goals') && res.request().method() === 'POST',
      )
      await page.click('button[type="submit"]:has-text("Lưu")')
      expect((await created).ok()).toBe(true)
      await page.waitForLoadState('networkidle')
      
      // Get the goal ID from URL or list
      // Navigate to GPS with asOf parameter
      await page.goto('/gps?asOf=2026-01-01')
      await page.waitForLoadState('networkidle')
      
      // Should load and show asOf date
      await expect(page.locator('.gps-result')).toBeVisible()
      await expect(page.locator('.as-of')).toContainText('2026-01-01')
    })
  })
})

/**
 * Helper to run a specific test file:
 * cd e2e && npm run test:only -- gps.spec.ts
 * 
 * Headed mode for debugging:
 * cd e2e && npm run test:headed -- gps.spec.ts
 * 
 * Debug mode:
 * cd e2e && npm run test:debug -- gps.spec.ts
 */