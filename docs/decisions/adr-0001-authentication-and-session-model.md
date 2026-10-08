# ADR 0001: Authentication and session model

- Status: Accepted
- Date: 2026-10-08

## Context

The project is a personal financial planning platform with user-specific financial data. The backend handles account registration, secure login, and access to user-owned resources. Because the application stores financial state tied to a person, the auth model must be secure, predictable, and resilient to common web-session attacks.

The application requirements already specify a strong security posture:

- email/password accounts
- BCrypt password hashing
- session-based authentication
- HttpOnly cookies
- server-side session storage
- session-id rotation on sign-in
- explicit logout invalidation
- secure timeout behavior

The system also needs to satisfy the practical realities of a web app: users should not need to reauthenticate unnecessarily, while the server should still fail closed when a session is stale or invalid.

## Decision

We will implement a server-side session model using Spring Session JDBC with a dedicated authenticated session cookie.

The key behavioral decisions are:

1. Accounts authenticate by email and password.
2. Passwords are stored as BCrypt hashes with a cost factor of 12.
3. Email uniqueness is case-insensitive at the database level, while the display case is preserved for user-facing presentation.
4. The server issues a server-side session and sets an HttpOnly SESSION cookie.
5. Idle timeout is set to 30 minutes.
6. Session IDs rotate when a user signs in.
7. Logout explicitly invalidates the server-side session and clears the client session state.
8. The backend treats authentication as a server-owned trust boundary, not as a client-controlled token.

This provides a secure default for session lifecycle management without exposing the raw session identifier to JavaScript access.

## Rationale

A server-side session model is the best fit for this system because:

- the app is stateful and interactive
- the data is highly personal and sensitive
- the backend can enforce authentication and session validity centrally
- the browser only needs to hold a non-scriptable session cookie, reducing attack surface
- session rotation and server-side invalidation are easier to reason about and audit than long-lived stateless tokens

BCrypt also aligns with the app’s security model by making password storage resistant to offline cracking while preserving a simple registration flow.

## Consequences

### Positive

- robust user authentication without storing plaintext passwords
- clear server-side enforcement of authenticated state
- predictable lifecycle management for idle sessions and explicit logout
- reduced risk of JavaScript access to sensitive session identifiers
- easier auditing of session invalidation and security behavior

### Negative

- server-side session storage increases state management complexity
- invalid or stale sessions must be enforced consistently across all routes and services
- the app depends on correct session-store configuration and secure cookie policy in production-like environments

### Follow-up constraints

- authentication flows must be covered by integration and contract tests
- session timeout behavior must be verified separately from login functionality
- production profile must continue to set secure cookie properties as needed

## Alternatives considered

### Stateless JWT-only auth

Rejected because it pushes trust into the client and makes revocation and session invalidation harder to enforce correctly. For a personal finance app storing sensitive data, server-side session control is simpler and safer.

### Client-side session or local storage tokens

Rejected because they expose session material to JavaScript, making XSS impact much more serious and weakening the application’s security posture.

### No session rotation

Rejected because session fixation is a known risk when a session identifier remains stable across sign-in, especially in a web app with sensitive account data.

## Related artifacts

- [docs/project-handbook.md](../project-handbook.md)
- [README.md](../../README.md)
- [docs/operations/ports-and-services.md](../operations/ports-and-services.md)
