# System Context and Boundaries

## Users and actors

- end user: manages their financial profile and goals
- authenticated account owner: accesses protected financial operations
- system administrator / operator: maintains local environment or deployment
- automation: CI, Docker, Kubernetes, test runners

## External dependencies

- PostgreSQL database
- Docker Compose environment
- local or remote browser clients
- Playwright E2E runner
- Kubernetes cluster for deployment experimentation

## Internal boundaries

### Frontend

Responsible for:
- rendering views and interactions
- making API requests
- handling session cookies and CSRF tokens
- routing and UX concerns

Not responsible for:
- financial logic authority
- persistence logic
- auth enforcement beyond client-side UX flow

### Backend

Responsible for:
- authentication and session management
- use-case orchestration
- persistence and domain integration
- API contracts and problem responses

Not responsible for:
- leaking domain logic into controller code
- mixing persistence and UI concerns

### Domain layer

Responsible for:
- financial calculations and invariants
- policy logic
- model rules without framework dependencies

Not responsible for:
- database access
- HTTP transport
- security framework implementation

## Security boundary

The system enforces:
- login and session-based identity
- CSRF double-submit pattern
- ownership-based authorization
- resource access masking for unauthorized owners
- secure cookie configuration in production-like environments

## Operational boundary

- local development is driven by Docker Compose
- CI uses explicit compose profiles for deterministic validation
- Kubernetes manifests provide a platform deployment model
- health checks, service dependencies, and test profiles should remain explicit and observable

## Key flows

### Authentication flow

- user registers or logs in
- session is created server-side
- HttpOnly cookie is set
- subsequent requests are authorized by session and ownership checks
- CSRF seed and validation protect state-changing API requests

### Financial flow

- user enters or updates financial data
- backend validates the payload
- domain engine applies business rules and projections
- response is returned as a normalized API result
- data is persisted for later retrieval and export
