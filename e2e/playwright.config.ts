import { defineConfig, devices } from '@playwright/test'

/**
 * End-to-end suite for the Financial GPS SPA.
 *
 * Stack under test (docker compose up):
 *   Playwright ── HTTP ──▶ Vite dev server (:4173) ── /api proxy ──▶ Spring Boot (:8080)
 *                                                                              └─▶ PostgreSQL
 *
 * Requirement: backend + frontend are running. Override the SPA origin with
 * E2E_BASE_URL when the SPA runs somewhere else.
 */
export default defineConfig({
  testDir: './specs',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
