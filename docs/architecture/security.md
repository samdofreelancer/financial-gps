# Security Architecture

## Overview

The application handles personal financial data and therefore treats authentication, authorization, and session integrity as first-class system concerns. The security model is designed to be explicit, layered, and testable.

## Security principles

1. No user data without authenticated context.
2. Ownership is enforced at the application boundary.
3. Cross-user access is never distinguishable from absence of data.
4. Session state is server-controlled, not client-controlled.
5. State-changing browser requests must include CSRF validation.
6. Security controls must be enforced even when the UI is bypassed.

## Authentication model

The backend uses email/password accounts with BCrypt hashing. The application stores only the password hash, never the plaintext credentials.

Key properties:

- case-insensitive uniqueness on email
- display case preserved for user-visible data
- registration and login are validated through the standard problem model
- password policy violations return structured problem responses instead of silent failure

## Session model

The application uses server-side sessions with Spring Session JDBC.

Security controls in the current design include:

- HttpOnly session cookie
- idle timeout of 30 minutes
- session-id rotation at sign-in
- invalidation on logout
- explicit auth requirement for protected resource access

This choice protects the app against common browser-side session exposure risks while keeping the user experience simple.

## CSRF strategy

The app uses a double-submit token pattern:

- a CSRF seed is created through the auth endpoint
- the token value is stored in a client cookie
- the frontend replays the value in the `X-XSRF-TOKEN` header for state-changing requests
- server validation rejects mismatched or missing tokens

This protects authenticated browser flows against cross-site request forgery.

## Authorization model

The central principle is ownership-based access control.

The design includes:

- an owner identity for user-scoped resources
- authenticated user resolution from the current request context
- per-use-case owner verification before business mutation or read
- generic not-found semantics for unauthorized access attempts

This prevents direct enumeration of other users’ data and keeps authorization consistent across the application.

## Key security boundaries

### Boundary 1: public vs protected routes

Anonymous endpoints allow sign-up, sign-in, and CSRF seed retrieval. All user-scoped financial operations require an authenticated session.

### Boundary 2: domain vs infrastructure

The domain layer remains framework-free and should not rely on web or identity primitives. Security concerns are enforced at the adapter and application boundary rather than buried inside business logic.

### Boundary 3: server trust boundary

The server is the source of truth for authentication state, ownership, and session validity. Client cookies and headers are inputs, not trusted sources by themselves.

## Data safety behaviors

The project explicitly includes additional safeguards:

- deterministic export for account data
- explicit confirmation before account deletion
- zero-orphan and cascade-consistent deletion behavior
- standardized RFC 7807 problem responses for failure states

These details reduce accidental data loss and keep security failures consistent and auditable.

## Production concerns

In secure deployments, session cookies and auth flows must be configured for production conditions, especially:

- secure cookie settings for HTTPS deployments
- proper session persistence and storage isolation
- network exposure limited to necessary services
- explicit handling of idle and absolute session timeouts

## Security-related docs

- [adr-0001-authentication-and-session-model.md](../decisions/adr-0001-authentication-and-session-model.md)
- [adr-0002-csrf-double-submit-protection.md](../decisions/adr-0002-csrf-double-submit-protection.md)
- [adr-0003-ownership-based-authorization-and-resource-hiding.md](../decisions/adr-0003-ownership-based-authorization-and-resource-hiding.md)
- [overview.md](overview.md)
