import { client, csrf, xsrfHeader } from './http'

/**
 * Real 007 authentication contract (read from AuthController + SecurityConfig,
 * not invented):
 *
 * - POST /api/v1/auth/register {email, password} → 201 {id, email, createdAt}
 *   + auto sign-in via Set-Cookie SESSION. Failures: 422
 *   PASSWORD_POLICY_VIOLATION, 409 REGISTRATION_FAILED, 400 VALIDATION_FAILED.
 * - POST /api/v1/auth/login {email, password} → 200 {id, email} + rotated
 *   SESSION cookie (fixation defense). Failure: 401 INVALID_CREDENTIALS
 *   (identical body for unknown email vs wrong password).
 * - POST /api/v1/auth/logout → 204, server-side session invalidation.
 * - GET /api/v1/account/me → 200 {id, email, createdAt} for session restore,
 *   401 AUTH_REQUIRED otherwise.
 * - GET /api/v1/auth/csrf → 200, seeds the XSRF-TOKEN double-submit cookie.
 */
export interface AuthAccount {
  id: string
  email: string
  createdAt?: string
}

/**
 * A recognisable but non-identifying stand-in for the signed-in email, used wherever the full
 * address has no business being on screen (the top bar greeting). The account page and the
 * account menu are where the real address belongs.
 *
 * `ginseng1000years@gmail.com` → `g••••••••@gmail.com`
 */
export function maskEmail(email: string | null | undefined): string {
  const value = (email ?? '').trim()
  const at = value.indexOf('@')
  // Nothing recognisable to keep: empty input, or no local part before the '@'.
  if (at <= 0) return value ? '••••••' : ''
  const local = value.slice(0, at)
  const domain = value.slice(at)
  // Keep the first character so the person still recognises their own account; the mask length
  // is fixed so it leaks neither the rest of the name nor how long it is.
  return `${local.charAt(0)}${'•'.repeat(8)}${domain}`
}

export async function register(body: { email: string; password: string }): Promise<AuthAccount> {
  await csrf()
  const { data } = await client.post<AuthAccount>('/api/v1/auth/register', body, {
    headers: xsrfHeader(),
  })
  return data
}

export async function login(body: { email: string; password: string }): Promise<AuthAccount> {
  await csrf()
  const { data } = await client.post<AuthAccount>('/api/v1/auth/login', body, {
    headers: xsrfHeader(),
  })
  return data
}

export async function logout(): Promise<void> {
  await csrf()
  await client.post('/api/v1/auth/logout', {}, { headers: xsrfHeader() })
}

export async function me(): Promise<AuthAccount> {
  const { data } = await client.get<AuthAccount>('/api/v1/account/me')
  return data
}
