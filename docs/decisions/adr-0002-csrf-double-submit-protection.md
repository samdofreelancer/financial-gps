# ADR 0002: CSRF protection via double-submit tokens

- Status: Accepted
- Date: 2026-10-08

## Context

The application uses browser-based auth and session state with cookies. This means cross-site request forgery is a real risk if the system allows state-changing requests to be initiated by a malicious third-party site while the victim’s browser still carries valid cookies.

The project also expects a smooth browser UX: the user should not have to manage a complex token workflow manually, but the backend should still reject forged requests reliably.

## Decision

We will use a classic double-submit CSRF control:

1. A `GET /api/v1/auth/csrf` endpoint seeds a CSRF token value into the client as a cookie (`XSRF-TOKEN`).
2. The frontend reads the cookie and includes the token in the `X-XSRF-TOKEN` header for state-changing requests.
3. The backend validates the request header against the server-side expected token value and rejects state-changing requests when they do not match.
4. Requests that fail CSRF validation return the project’s standardized RFC 7807 problem payload.

This pattern is intentionally paired with session-based auth so that both the session and the cross-site request protection are enforced at the server boundary.

## Rationale

A double-submit token is a strong, widely understood protection for cookie-based browser auth. It is particularly effective because:

- the browser automatically sends the cookie, but the attacker cannot read the token value from a cross-site origin
- the server can verify that the request includes a non-cookie token value matching a server-issued secret
- the token can be rotated or refreshed in a predictable way without requiring the user to manage a separate credential

This approach is simpler and more robust than relying only on SameSite cookies, especially when the application needs to support browser-driven state changes with session cookies in place.

## Consequences

### Positive

- forged cross-site state-changing requests are rejected reliably
- browser UX remains simple because the token is automatically attached by the frontend
- the system aligns with secure web practices and server-side enforcement
- the same CSRF flow can be audited and tested consistently across endpoints

### Negative

- the frontend must include the token for all unsafe requests
- new endpoints that mutate state must remember to enforce the same pattern
- testing must validate both the happy path and the invalid-token scenarios

### Follow-up constraints

- all state-changing endpoints must validate the CSRF token consistently
- the project must not rely on a cookie-only check for mutation endpoints
- browser clients must handle token seed and replay without breaking form and API requests

## Alternatives considered

### SameSite-only cookie defenses

Rejected as insufficient by itself because cookie protections are not a complete substitute for explicit CSRF validation when browser behavior and compatibility requirements are considered.

### CSRF-free or trust-by-origin architecture

Rejected because the app is sensitive to authenticated state changes and cannot safely assume all origins are trusted.

### Server-generated hidden field pattern only

Rejected as less ergonomic for API-driven flows and less compatible with modern browser-driven state management patterns.

## Related artifacts

- [README.md](../../README.md)
- [docs/project-handbook.md](../project-handbook.md)
- [docs/troubleshooting/opencode-port-conflict.md](../troubleshooting/opencode-port-conflict.md)
