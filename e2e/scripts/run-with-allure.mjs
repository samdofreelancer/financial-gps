#!/usr/bin/env node
/**
 * The whole feedback loop in one command: run Playwright, then publish the Allure
 * report — always, including when tests fail.
 *
 * Why a wrapper instead of `playwright test && allure generate`: npm does not run
 * `posttest` when the test script fails, and `&&` skips the report on failure —
 * exactly the run the report exists for. This script renders the report in both
 * cases, keeps Playwright's exit code (so CI still fails on regressions) and
 * downgrades a missing/broken Allure CLI to a loud warning instead of hiding the
 * test outcome.
 *
 * Usage:
 *   npm test                      # suite + report
 *   npm test -- --grep auth       # any Playwright flag is forwarded
 *   npm run test:only             # raw Playwright run, no report
 */
import { spawn } from 'node:child_process'
import { existsSync, readdirSync, rmSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import allureCli from 'allure-commandline'

const e2eDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
/**
 * Must stay in sync with `reporter[allure-playwright].outputFolder` in
 * playwright.config.ts; ALLURE_RESULTS_DIR is the override the reporter itself
 * honours, so it is mirrored here.
 */
const resultsDir = path.resolve(e2eDir, process.env.ALLURE_RESULTS_DIR ?? 'allure-results')
const reportDir = path.resolve(e2eDir, process.env.ALLURE_REPORT_DIR ?? 'allure-report')
const playwrightCli = path.join(e2eDir, 'node_modules', '@playwright', 'test', 'cli.js')

/** Spawns a command in the e2e folder and resolves with its exit code. */
function run(command, args) {
  return new Promise((resolve) => {
    const child = spawn(command, args, { cwd: e2eDir, stdio: 'inherit' })
    child.on('error', (error) => {
      console.error(`[allure] could not start ${path.basename(command)}: ${error.message}`)
      resolve(1)
    })
    child.on('close', (code, signal) => resolve(signal ? 1 : (code ?? 1)))
  })
}

/** Renders ./allure-results into a fresh ./allure-report through the Allure CLI (needs Java). */
async function generateReport() {
  try {
    return await new Promise((resolve) => {
      const child = allureCli(['generate', resultsDir, '--clean', '-o', reportDir])
      child.on('error', (error) => {
        console.error(`[allure] report generation failed: ${error.message}`)
        resolve(1)
      })
      child.on('close', (code) => resolve(code ?? 1))
    })
  } catch (error) {
    console.error(`[allure] report generation failed: ${error.message}`)
    return 1
  }
}

// One run, one report: leftover results from an earlier run would merge into it.
rmSync(resultsDir, { recursive: true, force: true })

const testStatus = await run(process.execPath, [playwrightCli, 'test', ...process.argv.slice(2)])

let reportStatus = 1
if (existsSync(resultsDir) && readdirSync(resultsDir).length > 0) {
  reportStatus = await generateReport()
} else {
  console.error('[allure] no results were written — skipping report generation')
}

if (reportStatus !== 0) {
  console.error(
    '[allure] !!! the report was NOT generated. Run `npm run allure:generate` for the full error;' +
      ' the Allure CLI needs Java 8+ on PATH.',
  )
}

console.log(
  [
    '',
    '----------------------------------------------',
    ` Playwright : ${testStatus === 0 ? 'passed' : `failed (exit ${testStatus})`}`,
    ` Allure     : ${reportStatus === 0 ? reportDir : 'not generated (see warning above)'}`,
    ' Open with  : npm run allure:open',
    '----------------------------------------------',
  ].join('\n'),
)

process.exit(testStatus)
