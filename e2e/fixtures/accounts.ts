/** Unique account factory — every spec gets a fresh email so sessions never collide. */

const EMAIL_DOMAIN = process.env.E2E_EMAIL_DOMAIN ?? 'example.com'

/** Satisfies the 007 password policy: >= 10 chars, a letter and a digit. */
export const E2E_PASSWORD = 'e2e journey pass1'

export function uniqueEmail(prefix = 'e2e'): string {
  return `${prefix}+${Date.now().toString(36)}${Math.floor(Math.random() * 1e6).toString(36)}@${EMAIL_DOMAIN}`
}
