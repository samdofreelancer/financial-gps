import axios from 'axios'

/**
 * Shared same-origin HTTP client. The SPA is served behind the Vite `/api`
 * proxy (or same-origin in production), so `SESSION` / `XSRF-TOKEN` cookies
 * flow without any CORS setup. Credentials must always be included: the
 * backend authenticates via the HttpOnly `SESSION` cookie (007).
 */
export const client = axios.create({ withCredentials: true })

/** Anonymous CSRF warm-up: seeds the readable `XSRF-TOKEN` cookie. */
export async function csrf(): Promise<void> {
  await client.get('/api/v1/auth/csrf')
}

/** Double-submit header echoed back on every state-changing request. */
export function xsrfHeader(): Record<string, string> {
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)
  return match ? { 'X-XSRF-TOKEN': decodeURIComponent(match[1]) } : {}
}

interface ProblemBody {
  code?: string
  title?: string
  detail?: string
  violations?: string[]
}

function isAxiosProblem(error: unknown): { status?: number; data?: ProblemBody } | null {
  const response = (error as { response?: { status?: number; data?: ProblemBody } })?.response
  return response ?? null
}

/**
 * Presentation-only error mapping. The server body stays authoritative; this
 * only selects which server-provided text to show. Never invents meanings.
 */
export function problemMessage(error: unknown, fallback: string): string {
  const response = isAxiosProblem(error)
  if (!response) {
    return 'Could not reach the server. Check your connection and try again.'
  }
  const data = response.data ?? {}
  switch (data.code) {
    case 'INVALID_CREDENTIALS':
      return 'Email or password is incorrect.'
    case 'REGISTRATION_FAILED':
      return 'Registration failed. Try again with different details.'
    case 'PASSWORD_POLICY_VIOLATION':
      return data.violations?.length ? data.violations.join(' ') : data.detail || fallback
    case 'VALIDATION_FAILED':
      return data.detail || 'Request body is invalid.'
    case 'AUTH_REQUIRED':
      return 'Sign in to access this resource.'
    case 'CSRF_INVALID':
      return 'Security token expired. Please try again.'
    default:
      return data.detail || data.title || fallback
  }
}

/** True when the failure is simply "no/invalid session" (expected, not an error to alarm about). */
export function isAuthRequired(error: unknown): boolean {
  return isAxiosProblem(error)?.status === 401
}
