import { defineConfig, devices } from '@playwright/test'
import type { AllureReporterOptions } from 'allure-playwright'

/**
 * End-to-end suite for the Financial GPS SPA.
 *
 * Stack under test (docker compose up):
 *   Playwright ── HTTP ──▶ Vite dev server (:4173) ── /api proxy ──▶ Spring Boot (:8080)
 *                                                                              └─▶ PostgreSQL
 *
 * Requirement: backend + frontend are running. Override the SPA origin with
 * E2E_BASE_URL when the SPA runs somewhere else.
 *
 * Isolation model: tests are order-independent — each registers its own fresh
 * account via support/auth-flow (no shared emails, no storageState). Playwright
 * gives every test a clean browser context, so fullyParallel is safe.
 *
 * Reporting: every run writes Allure results to ./allure-results, failures carry
 * their screenshot and trace as attachments. `npm test` renders those results
 * into ./allure-report once the run is over — also when tests fail — through
 * scripts/run-with-allure.mjs. `npm run test:only` runs raw Playwright and only
 * refreshes the results folder; `npm run allure:open` opens the last report.
 */
const baseURL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
const emailDomain = process.env.E2E_EMAIL_DOMAIN ?? 'example.com'

/** Consumed by the `allure-playwright` reporter — see the reporting note above. */
const allureReporter: AllureReporterOptions = {
  outputFolder: process.env.ALLURE_RESULTS_DIR ?? 'allure-results',
  // Playwright API steps (page.goto, locator.click, expect …) become Allure steps.
  detail: true,
  // Spec file + describe titles become the Allure suite / sub-suite labels.
  suiteTitle: true,
  // Shows up in the Allure report's "Environment" widget — tells a run apart.
  environmentInfo: {
    'Base URL': baseURL,
    'Email domain': emailDomain,
    Browser: 'chromium (Desktop Chrome)',
    'Node.js': process.version,
    Platform: `${process.platform} ${process.arch}`,
  },
}

export default defineConfig({
  testDir: './specs',
  fullyParallel: true,
  retries: 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['allure-playwright', allureReporter]],
  outputDir: process.env.PLAYWRIGHT_OUTPUT_DIR ?? 'test-results',
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})

