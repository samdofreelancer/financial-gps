# Feature Specification: Authentication, Authorization & Data Ownership

**Feature Branch**: `007-authentication`

**Created**: 2026-08-25

**Status**: Draft

**Input**: User description: "Add email/password session-based authentication with
role-based authorization. Users must be able to register with email and password, log in and
log out with a browser session cookie, and be authorized so each user can only access, modify,
and delete their own financial profile, debts, goals, and GPS results. Protect all financial
endpoints behind authentication."

**Scope**: `007` covers three related concerns and is titled accordingly:

1. **Authentication** — register, log in, log out, browser session (US1, US2, US4).
2. **Authorization** — ownership-based access control over financial resources (US3).
3. **Account & Data Ownership** — export, account deletion, and the owner-scoped resource
   lifecycle including archive → hard-delete cascade (US5, Data Lifecycle, FR-011–FR-014).

This is not a bare login feature: it is the platform capability that makes financial data
*private state* rather than public calculation.

**Planning note**: when this feature goes through `/plan`, split implementation into five areas
rather than one increment blob: `authentication` · `authorization` · `ownership` · `data-export`
· `account-deletion`. All five stay inside this single feature; the plan should keep them as
separable workstreams, each with its own independent test criteria.

## Architectural Boundary *(clarification — no new requirements)*

> This section is an architectural/documentation clarification only. It adds no business rule and
> changes none of the user stories, functional requirements, or success criteria below.

Authentication, Authorization & Data Ownership (`007-authentication`) is an
**application/platform capability** and is independent from the Financial Domain Engine.

### Independence

`007-authentication` MUST NOT be treated as a dependency of:

- the Financial Domain Engine,
- financial calculations,
- debt calculations,
- goal calculations,
- allocation calculations,
- timeline/projection calculations,
- financial status evaluation.

The Financial Domain Engine MUST remain **user-agnostic**. It MUST NOT require or receive:

- a `userId` / owner identity,
- an authenticated user,
- a session,
- a JWT or OAuth token,
- an HTTP request,
- a security context.

The domain engine API operates only on financial inputs, assumptions, `asOfDate`, and financial
policy (`specs/financial-domain/contracts/engine-contract.md`). The domain contract
(`calculation-rules.md`) remains the single source of truth for financial calculation semantics;
this feature never duplicates financial business rules. Privacy/authorization gating applies to
application endpoints, storage, and served results — never to the pure engine function itself.

### Integration point (application layer only)

User identity is resolved **outside** the engine, by the application/platform layer:

```text
HTTP/API Request
    ↓
Authentication / Security          ← 007-authentication lives here
    ↓
Authenticated User
    ↓
Application Service                ← associates the Financial Profile with the account
    ↓
Financial Domain Engine            ← pure, identity-free function call
    ↓
Financial Result                   ← scoped/persisted back by the application layer
```

The application layer MAY associate a Financial Profile with an authenticated user (owner scoping
per FR-006–FR-014 below). That association happens before calling the engine and when persisting
or serving results; the engine itself never sees identity.

### Dependency direction

```text
007-authentication    → independent platform capability
financial-domain      → independent pure domain core
```

They may be integrated by the application layer, but neither side's domain logic depends on the
other. These dependency directions are FORBIDDEN:

- `Financial Domain → Authentication`
- `Financial Domain → User Identity`

### Feature numbering ≠ dependency order

Feature IDs are labels, not an implementation-dependency ordering. The existing IDs remain
unchanged: 001 Financial Profile · 002 Debt Management · 003 Financial Goals · 004 Financial GPS ·
005 Financial Roadmap · 006 Scenario Planning · 007 Authentication · 008 Financial Timing &
Allocation · 009 Financial Review. That `007` sits numerically between the others does NOT mean
it is part of the Financial GPS domain dependency chain.

### Conceptual architecture

```text
                    Financial GPS
                         │
             ┌───────────┴───────────┐
             │                       │
      Platform Capability      Financial Domain
   007 Auth · Authz ·           Core Engine
   Session · Ownership
             │                       │
             │              ┌────────┼────────┐
             │              ↓        ↓        ↓
             │           004 GPS  005 Roadmap 006 Scenario
             │                                │
             │                                ↓
             │                         008 Allocation
             │                                │
             │                                ↓
             │                           009 Review
             │
             └────── Application Layer ───────┘
```

Both branches meet only in the Application Layer (HTTP, composition, scoping, persistence) —
never inside the domain core.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register an account (Priority: P1)

As a new user, I create an account with an email and password so I can get my own private
financial workspace.

**Why this priority**: A trusted Financial GPS starts with authorization boundaries; no other
financial feature can be made private until an owner identity exists.

**Independent Test**: Register with a valid email and password and confirm the new account is
signed in and has an empty, private financial workspace that no other account can see.

**Acceptance Scenarios**:

1. **Given** a not already registered email, **When** the user submits a valid email and password,
   **Then** the account is created, the user is signed in, and the same email cannot be registered
   a second time.
2. **Given** an account that already exists for the email, **When** the user tries to register the
   same email, **Then** the system rejects it without revealing whether the email belongs to anyone.
3. **Given** an invalid or weak password, **When** the user submits registration, **Then** the
   system rejects it and explains the password requirements.

---

### User Story 2 - Log in and log out (Priority: P1)

As a returning user, I log in with my email and password so I can access my financial
workspace, and I log out when I am done on a shared device.

**Why this priority**: Login is the gateway to every private financial feature; logout is required
for shared or public devices.

**Independent Test**: Log in with correct credentials and access the workspace; log in with the
wrong password or a nonexistent email and confirm no workspace is exposed; log out and confirm the
workspace is no longer accessible.

**Acceptance Scenarios**:

1. **Given** an existing account, **When** the user logs in with the correct email and password,
   **Then** the user gains access to their financial workspace and remains signed in across page views.
2. **Given** an existing account, **When** the user logs in with an incorrect password, **Then** the
   system rejects the attempt without revealing whether the email exists.
3. **Given** a signed-in user, **When** the user logs out, **Then** the session ends and the user's
   financial data is no longer accessible on that device without signing in again.

---

### User Story 3 - Authorize access to financial data (Priority: P1)

As a signed-in user, I can only see and change my own financial profile, debts, goals, and GPS
results, and I cannot see other owners' data.

**Why this priority**: This is the **ownership-based authorization** core — access derives from
resource ownership (`authenticated user → owns resource → can access resource`) — and it makes
the constitution's privacy guarantee real: "one owner profile per authenticated account". The
`Role` entity is kept for future extensibility (e.g., support/admin); this release ships only the
`OWNER` role — there is no ADMIN role and none is introduced by this feature.

**Independent Test**: With two accounts, create data in account A and confirm account B can only
read and modify its own workspace, and that any attempt to read, change, or delete A's resources is
rejected.

**Acceptance Scenarios**:

1. **Given** a signed-in user, **When** the user uses the financial features (profile, debts,
   goals, GPS, roadmap, scenarios), **Then** every operation applies only to that owner's data.
2. **Given** user A and user B, **When** B attempts to read, modify, or delete a resource belonging
   to A, **Then** the system rejects the request without revealing A's data.
3. **Given** no valid session, **When** a financial endpoint is called, **Then** the system rejects
   the request as unauthenticated.

---

### User Story 4 - End an expired session (Priority: P2)

As a user, I keep signing in safely: after a period of inactivity my session expires and I must
sign back in.

**Why this priority**: Reduces the risk of a session being left open on a shared or unattended
device.

**Independent Test**: Start a session, let it expire without activity, then request a protected
resource and confirm the user is asked to sign in again.

**Acceptance Scenarios**:

1. **Given** an authenticated session, **When** it has been inactive beyond the configured idle
   time, **Then** the next protected request is rejected and the user must sign in again.
2. **Given** an authenticated session, **When** the user is still active before the timeout,
   **Then** the user continues to access the workspace without disruption.

---

### Edge Cases

- Email uniqueness is case-insensitive; the display case is preserved.
- Registration and login failures never reveal whether an email exists (a "taken" and an invalid
  credential produce the same generic message).
- A password is stored only as a strong, one-way, salted hash; plaintext or reversible storage is
  prohibited.
- An unauthenticated user can reach only public endpoints (registration, login) and never
  financial data.
- A session cookie is the initial scope; cross-device session management is a future feature with
  its own rules. Account deletion is in this release (US5, FR-012, SC-007).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST let a user register with an email and password and create a private
  account.
- **FR-002**: The system MUST let a registered user log in with email and password and start a
  server-side session bound to the browser.
- **FR-003**: The system MUST let a signed-in user log out and terminate the current session.
- **FR-004**: The system MUST reject duplicate-email registration without revealing that an account
  with that email already exists.
- **FR-005**: The system MUST enforce a documented minimum password policy and explain a rejected
  password to the user.
- **FR-006**: The system MUST require authentication before serving any financial resource
  (profile, income, expense, debt, goal, GPS, roadmap, scenario).
- **FR-007**: The system MUST authorize each request so an authenticated user can read, modify, and
  delete only their own resources; access to any other owner's resource is rejected and never
  returned.
- **FR-008**: The system MUST resolve every existing financial `ownerId` to the authenticated
  account and scope all financial results to it.
- **FR-009**: The system MUST end a session inactive beyond the configured timeout and require the
  user to sign in again.
- **FR-010**: The system MUST prevent unauthorized data access even when a caller guesses or
  supplies another user's resource ID (return an authorization failure, not the resource).

### Key Entities *(include if feature involves data)*

- **Owner (User Account)**: An authenticated identity (email, credential) that owns a financial
  workspace. Each existing `ownerId` in the financial data model refers to an Owner.
- **Credential**: The secret used at sign-in, protected as a one-way hash; never listed or returned
  to the client.
- **Session**: An authenticated browser session bound to an Owner with an expiry time; it lets the
  user access the protected workspace.
- **Role**: A named permission set. The initial release has an `OWNER` role; it shapes the
  authorization rule "every user accesses only their own data" and is designed to allow future roles
  (e.g., support/admin) without exposing data. No other role exists in this release.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can register and reach their empty workspace in under 2 minutes.
- **SC-002**: 100% of stored passwords are never plaintext; in tests and runs no reversed or
  plaintext password is stored or returned.
- **SC-003**: In automated authorization tests with 2+ accounts, 0 cross-owner accesses are allowed;
  every attempt by owner B at owner A's resource is rejected.
- **SC-004**: A user inactive past the session timeout is required to sign in again, verified by a
  browser journey (login → expire → protected request rejected).
- **SC-005**: 100% of financial endpoints reject a request with no valid session with the documented
  unauthenticated error and never return financial data.

## Assumptions

- The initial authentication is email/password with browser session cookies on the same origin;
  OAuth2 / external identity providers are out of scope for this feature and may be added later
  without changing the authorization model.
- Password reset, email verification, administrative users, and cross-device session management
  are out of scope for the first release. Account deletion and export ARE in scope
  (US5, FR-011–FR-014, SC-006–SC-008).
- There is one authenticated owner per workspace in the Financial GPS scope, matching the existing
  "single owner profile per authenticated account" plan.
- Session cookies are HTTP and secure by default; the exact cookie lifecycle (name, domain,
  same-site) is a technical detail refined in the technical plan.
- This feature does not change financial calculation rules; it supplies the owner identity that
  existing financial features and GPS results are scoped to — at the application layer only
  (see *Architectural Boundary* above; the pure domain engine never receives identity).
---

### User Story 5 - Export and delete my data (Account & Data Ownership) (Priority: P2)

As a user, I can export all my financial data in a portable form and delete my account (after a
confirmation) so I keep control over sensitive financial facts, per the privacy principle.

**Why this priority**: constitution Principle X requires access/export/deletion to be explicit
and verifiable. This makes the "ownership" half of account ownership concrete.

**Acceptance Scenarios**:

1. **Given** a signed-in owner, **When** the user requests an export, **Then** the system returns
   a complete, machine-readable copy of all profile, income, expense, debt, goal, timeline,
   allocation, GPS review, and ledger data owned by that account.
2. **Given** a request to delete the account, **When** the user confirms the irreversible action,
   **Then** the account and all its scoped financial data are removed (and, before deletion, the
   owner is offered an export-then-delete flow for verification).
3. **Given** an account with financial data, **When** a second owner (or an unauthenticated
   client) tries to export or delete it, **Then** the request is rejected and no data leaks.

---

### Data Lifecycle (owners and resources)

Each financial entity (`profile`, `income`, `expense`, `debt`, `goal`, `timeline change`,
`allocation`, plus GPS `snapshots` and `review ledger`) belongs to exactly one **Owner**. The
lifecycle is:

```
Create  → Update (Edit) → Archive (Soft) → Delete (Hard)
```

Archive preserves history (see `009-financial-review`); Hard delete removes the record. Owner
deletion cascades to all owned records (including archives), which are either exported or removed,
never orphaned.

## Requirements *(mandatory)* (additions)

- **FR-011**: The system MUST let an owner export all of their data in a portable,
  machine-readable form at any time.
- **FR-012**: The system MUST let an owner delete their account and all owned financial data after
  confirmation, and MUST offer an export-then-delete path.
- **FR-013**: The system MUST scope every resource read, update, archive, and delete to the
  authenticated owner; cross-owner export/delete is always rejected.
- **FR-014**: The system MUST apply the resource lifecycle consistently so no orphaned archive or
  review/ledger record remains after the owner is removed.

## Success Criteria (additions)

- **SC-006**: An owner can export a complete dataset and a reviewer can confirm it equals the
  owned records in 100% of automated checks.
- **SC-007**: Deleting an owner removes every owned record (including archives and review / ledger
  entries) with zero orphaned records in automated tests.
- **SC-008**: No export or delete request by a different owner ever succeeds in automated
  authorization tests.

## Assumptions (additions)

- Export uses a documented, machine-readable structure (the exact format is set in the plan).
- Account deletion is irreversible after the confirmation step; a grace or review window, if any,
  is documented in the plan.