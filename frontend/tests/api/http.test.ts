import { describe, expect, it } from 'vitest'
import { isAuthRequired, problemMessage } from '@/api/http'

function axiosError(status: number, data: unknown): unknown {
  return { response: { status, data } }
}

describe('problemMessage', () => {
  it('maps known server codes to fixed presentation text', () => {
    expect(problemMessage(axiosError(401, { code: 'INVALID_CREDENTIALS' }), 'fallback')).toBe(
      'Email or password is incorrect.',
    )
    expect(problemMessage(axiosError(400, { code: 'VALIDATION_FAILED' }), 'fallback')).toBe(
      'Request body is invalid.',
    )
    expect(
      problemMessage(axiosError(400, { code: 'VALIDATION_FAILED', detail: 'Bad amount.' }), 'x'),
    ).toBe('Bad amount.')
    expect(problemMessage(axiosError(401, { code: 'AUTH_REQUIRED' }), 'fallback')).toBe(
      'Sign in to access this resource.',
    )
    expect(
      problemMessage(
        axiosError(422, { code: 'PASSWORD_POLICY_VIOLATION', violations: ['too short'] }),
        'fallback',
      ),
    ).toBe('too short')
  })

  it('falls back to detail, title, then the caller fallback', () => {
    expect(problemMessage(axiosError(409, { code: 'CONFLICT', detail: 'Stale.' }), 'fb')).toBe(
      'Stale.',
    )
    expect(problemMessage(axiosError(409, { code: 'CONFLICT', title: 'Conflict' }), 'fb')).toBe(
      'Conflict',
    )
    expect(problemMessage(axiosError(409, { code: 'CONFLICT' }), 'fb')).toBe('fb')
  })

  it('reports a connection failure when there is no response', () => {
    expect(problemMessage(new Error('offline'), 'fallback')).toBe(
      'Could not reach the server. Check your connection and try again.',
    )
    expect(problemMessage(null, 'fallback')).toBe(
      'Could not reach the server. Check your connection and try again.',
    )
  })
})

describe('isAuthRequired', () => {
  it('is true only for 401 responses', () => {
    expect(isAuthRequired(axiosError(401, { code: 'AUTH_REQUIRED' }))).toBe(true)
    expect(isAuthRequired(axiosError(403, {}))).toBe(false)
    expect(isAuthRequired(new Error('offline'))).toBe(false)
  })
})
