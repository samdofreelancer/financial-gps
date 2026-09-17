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
