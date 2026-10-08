# ADR 0003: Ownership-based authorization and resource hiding

- Status: Accepted
- Date: 2026-10-08

## Context

The application processes personal financial data. A user must never be able to access or mutate another user’s records, even when the request contains a valid session. This is a core business requirement, not merely a UI concern.

The system needs to enforce ownership in a way that is deterministic, secure, and difficult to bypass through poor access control patterns. In practical terms, the application must behave as though unauthorized resources do not exist for the caller.

## Decision

We will enforce authorization using explicit owner identity semantics at the application boundary.

The design combines:

1. an `OwnerId` concept that marks the owner of a resource
2. a `CurrentOwnerProvider` that resolves the authenticated user for the active request
3. a resource access check at the application use-case boundary
4. cross-owner access returning a non-discoverable result equivalent to `RESOURCE_NOT_FOUND`

This means that if a user attempts to access or mutate another user’s data, the system hides the existence of the resource and returns a generic not-found-style error instead of a permission error that would reveal internal state.

## Rationale

This is a strong and safe authorization model because:

- ownership is explicit in the domain and application layer
- access is evaluated where use cases are executed, not only in controllers
- the system avoids leaking internal resource existence across users
- it narrows the blast radius of accidental or malicious misuse
- it preserves a consistent contract for clients and tests

By interpreting unauthorized access as a resource absence, the application reduces information leakage and enforces the principle that users should only see what they own.

## Consequences

### Positive

- strong boundary enforcement for user-scoped data
- consistent protection across API and application use cases
- reduced chance of unsafe owner bypass in future refactors
- better clarity in audit logs and security review

### Negative

- requires disciplined owner propagation through all use cases
- authorization logic must be maintained when new resources are introduced
- some clients may initially find “not found” semantics less explicit than a permission-denied response

### Follow-up constraints

- every new user-owned resource must define an owner boundary
- tests must validate both authorized access and cross-owner denial
- the system should continue to return standard problem responses instead of leaking internal detail

## Alternatives considered

### Role-only or controller-level authorization

Rejected because it can be bypassed when new use cases or adapters are added and does not encode ownership semantics deeply enough in the business logic.

### Permission-denied responses for cross-owner access

Rejected because it reveals that a resource exists and may allow enumeration or attack path discovery.

### Trusting IDs from the client

Rejected because user influence over an identifier is not sufficient to determine ownership; the server must derive ownership from authenticated state or an internal trusted context.

## Related artifacts

- [README.md](../../README.md)
- [docs/project-handbook.md](../project-handbook.md)
- [docs/architecture/business-domain.md](../architecture/business-domain.md)
