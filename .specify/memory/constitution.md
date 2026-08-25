<!--
Sync Impact Report
- Version change: template → 1.0.0
- Modified principles: template placeholders → I–X Financial GPS governance principles
- Added sections: Financial Data & Projection Rules; Development Workflow & Quality Gates
- Removed sections: none
- Follow-up TODOs: none
-->
<!--
Sync Impact Report (Amendment 1.1.0, ratified 2026-08-25)
- Version change: 1.0.0 → 1.1.0 (MINOR: added materially expanded norms)
- Modified principles: I–X unchanged in meaning; new principles XI–XIII added
- Added sections: Financial Timeline & Allocation (XI), Goal Dependencies & Route Order (XII),
  Financial Review & History (XIII); cross-reference to specs/financial-domain oracle
- Removed sections: none
- Follow-up TODOs: wire oracle rules into 004/005/006 plans; add 007/008/009 plans
-->
<!--
Sync Impact Report (Amendment 1.2.0, ratified 2026-08-25)
- Version change: 1.1.0 → 1.2.0 (MINOR: added materially expanded norms)
- Added principles: XIV. Domain-Driven Design Architecture
- Added sections: Domain-Driven Design (DDD) Architecture — bounded contexts, ubiquitous
  language, domain responsibilities, building blocks, aggregate guidance, domain purity,
  layered architecture, architectural guards, traceability chain
- Modified principles: none
- Removed sections: none
- Follow-up TODOs: none
-->
# Financial GPS Constitution

## Core Principles

### I. Financial Truth First
The system MUST represent only financial facts supplied by the user or values explicitly derived
from those facts. It MUST NOT fabricate, silently estimate, or silently assume financial values.
This protects every recommendation from being based on an unknown premise.

### II. Deterministic Financial Calculation
Given the same financial profile, income, expenses, debts, savings, goals, and assumptions, the
system MUST produce the same result. Core financial calculations MUST NOT depend on AI or any
non-deterministic service.

### III. Explainable GPS
Every GPS result MUST state the current position, destination, remaining distance, projected
arrival date, blockers, and next action. A user MUST be able to trace each status and projection
to its input values and calculation rules; opaque financial scores are prohibited.

### IV. Goal-Driven Roadmaps
Financial management MUST be represented as goals and route stages, not merely reports. Each goal
MUST define its target amount or completion condition, current progress, target date when
applicable, and required financial capacity. Each roadmap stage MUST define start and completion
conditions, projected completion date, progress, and next action.

### V. Conservative Planning
When a required assumption is uncertain, projections MUST use a conservative, documented
assumption. The user interface and outputs MUST distinguish actual values, user assumptions,
calculated projections, and scenario values so that uncertainty is never presented as fact.

### VI. Scenario Isolation
What-if simulations MUST be independent projections. Creating, editing, or deleting a scenario
MUST NOT modify the user's actual financial profile, debts, goals, or transaction data.

### VII. Human Control and Honest Communication
The product MUST present calculations as guidance rather than guarantees. It MUST NOT prescribe a
financial decision as mandatory, claim an outcome is certain, or use get-rich-quick framing. The
user retains final control over every financial decision.

### VIII. Testable Financial Domain
Financial rules MUST be independently testable and, where practical, implemented as pure domain
logic that accepts domain inputs and returns domain results. Acceptance tests MUST cover material
financial scenarios, including debt payoff, goal progress, timelines, GPS statuses, and scenario
isolation.

### IX. Simple Before Intelligent
The first implementation MUST prioritize, in order: correct financial model, correct
calculations, explainability, reliable roadmap, user experience, then AI assistance. AI MUST NOT
be used to conceal an unclear domain model or replace defined financial rules.

### X. Privacy and Data Minimization
The system MUST collect and retain only data needed to calculate the user's financial position,
goals, roadmap, and scenarios. Financial data MUST be treated as sensitive; access, export, and
deletion behavior MUST be explicit and verifiable before any external sharing capability is added.

### XI. Financial Time Model
Financial inputs are not flat. Income, expense, and debt values change on documented effective
dates, and projections MUST evaluate periods using the values effective on the projection's
`asOf` date. The system MUST NOT silently assume future values equal present values; an
"unchanged" value is an explicit default assumption that is labelled as such. Timeline changes
that originate from a scenario are projections only and MUST be isolated per Principle VI.

### XII. Cash Allocation and Deterministic Routes
Free monthly cash MUST be routed along an explicit, documented, priority-ordered allocation
(debt, emergency fund, goals, with user-defined or rule-driven overrides); a full result MUST
state which ordering policy was applied. When an allocation destination completes its
condition, freed cash MUST be reassigned deterministically. The route (allocation) is part of
the GPS result alongside position, distance, and ETA.

### XIII. Financial Review, History, and Audit
The system MAY retain deterministic financial snapshots and a concise change ledger
(`changedAt`, `changedBy/source`, before/after) so a user can answer "why did my projection
change?" Snapshots MUST NOT alter persisted actual inputs (isolation), MUST be owner-scoped per
Principle X, MUST be reproducible from their recorded inputs + `asOf`, and MUST be covered by
the owner export/delete behavior. A full banking ledger or transaction feed stays out of scope
unless explicitly added.

### XIV. Domain-Driven Design Architecture
Financial GPS explicitly follows Domain-Driven Design for business domains and domain boundaries.
The project MUST model behavior using bounded contexts, a ubiquitous language, and DDD building
blocks (entities, value objects, aggregates where appropriate, domain services, domain policies,
application services, infrastructure boundaries). DDD applies where it adds value: ceremony
without benefit, unnecessary abstraction, and over-engineering are prohibited. Normative
specifications remain the source of WHAT the system must do; DDD governs WHERE business concepts
and rules belong; TDD verifies HOW the behavior is implemented.

## Domain-Driven Design (DDD) Architecture

Detailing Principle XIV. This section is the project-wide source for DDD; individual specs only
reference the boundaries relevant to them and MUST NOT duplicate it.

### Bounded contexts

| Context | Contains | Features |
|---|---|---|
| A. Identity & Access | authentication, authorization, session, user identity, data ownership | 007 |
| B. Financial Management | financial profile, income, expense, debt, goal, financial state | 001, 002, 003 |
| C. Financial GPS | GPS results, roadmap, scenario, timing & allocation, financial review | 004, 005, 006, 008, 009 |

The pure Financial Domain Engine (`specs/financial-domain`) is the core domain serving context C.
Feature IDs are labels — NOT context boundaries — and remain unchanged.

**Context separation**: Identity & Access is independent from the Financial Domain; the Financial
Domain MUST stay identity-agnostic. Its code MUST NOT depend on a User entity from Identity &
Access, authentication, authorization, session, JWT/OAuth, security context, HTTP, or persistence
infrastructure. `FinancialProfile → User` MUST NOT be modeled as a direct domain dependency; the
application layer associates an authenticated identity with financial resources:

```text
Authenticated User → Application Layer → Financial Profile / Financial Use Case → Financial Domain
```

The engine contract stays `calculate(inputs, assumptions, asOfDate, policy) -> FinancialResult`;
`userId` MUST NOT enter it unless a normative specification explicitly requires it.

### Ubiquitous language

Implementation terminology MUST follow the canonical terms of the normative specifications:
`calculation-rules.md` §0 plus Financial Profile, Income, Expense, Debt, Goal, Financial State,
Net Cash Flow, Available Capacity, Allocation, Projection, Timeline Change, Goal Dependency,
Financial Status, Financial Policy, Assumption. Alternative names for an existing concept are
prohibited — Available Capacity MUST NOT become "Disposable Income", "Free Cash", or "Remaining
Money" — and concepts with different financial semantics MUST NOT be merged. Identity & Access
uses its own account/session/ownership terms inside its context.

### Domain responsibilities

Business rules live in the domain layer: debt calculations, cash-flow calculations, goal
calculations, allocation rules, dependency resolution, projection calculations, financial status
evaluation, rounding policies, financial policies. Controllers MUST NOT contain financial business
rules; application services orchestrate domain behavior but MUST NOT reimplement domain rules;
infrastructure MUST NOT redefine domain semantics.

### Building blocks

| Block | Use when | Examples |
|---|---|---|
| Entity | business identity + lifecycle | Account/Owner (Identity); Goal, Debt |
| Value object | immutable concept defined by value | Money, Rate, Ratio, NetCashFlow, AvailableCapacity, identifiers where appropriate |
| Domain service | operation not natural on one entity/VO | CashFlowCalculator, DebtCalculator, GoalCalculator, ProjectionEngine, DependencyResolver |
| Domain policy | explicit business policy | FinancialPolicy, DebtPolicy, AllocationPolicy, StatusPolicy, RoundingPolicy |
| Application service | use-case orchestration, transaction boundary, external→domain mapping | feature application services |

Do NOT create a domain service merely to wrap a method that naturally belongs to an entity or
value object.

### Aggregate guidance

Aggregate boundaries MUST NOT be invented. Where introduced, an aggregate MUST have an explicit
consistency boundary, clear invariant ownership, and controlled mutation. Prefer small, meaningful
aggregates over large graphs; never model all of Financial GPS as one giant aggregate. If no
specification requires an aggregate boundary, leave it unspecified.

### Domain purity

The Financial Domain Engine stays deterministic and framework-independent: no Spring, Spring
Security, JPA/database, HTTP/REST, system clock, randomness, AI, or external services. The
contract remains `calculate(inputs, assumptions, asOfDate, policy) -> FinancialResult`. The domain
model derives from business semantics first — JPA entity structure, database tables, and REST DTOs
MUST NOT become its source of truth.

### Layered architecture

```text
Interface / API     HTTP/API, request/response mapping
Application Layer   authentication context, authorization coordination, use-case
                    orchestration, transaction boundary, external input → domain input
Domain Layer        business rules, entities, value objects, domain services,
                    domain policies, invariants
Infrastructure      database, Spring integration, authentication infrastructure,
                    external services, persistence implementations
```

Dependency direction: Interface → Application → Domain; Infrastructure supports Application and
Interface. `domain → infrastructure` and `domain → Identity & Access` are FORBIDDEN.

### Architectural guards

While the repository uses a single Maven module, guards are package-level (no multi-module
refactor solely for DDD): domain does not depend on infrastructure/authentication/HTTP/persistence;
domain terminology matches the ubiquitous language; financial business rules do not live in
controllers; application services do not duplicate domain rules. Automated forms already defined:
the domain-lane `PurityTest` plus the platform-side `DomainBoundaryGuardTest`.

### Traceability chain

Requirement → Domain Rule → DDD Concept → Design/Plan → Task → Test → Implementation. Every
financial implementation MUST trace back to the normative specification; domain behavior MUST NOT
be created solely because a class or model seems architecturally appropriate.

## Financial Data & Projection Rules

The core domain comprises FinancialProfile, Income, Expense, Debt, Account/Owner, Goal,
Roadmap, RoadmapStage, Scenario, TimelineChange, CashAllocation, GoalDependency, and
FinancialSnapshot. Financial GPS is defined as current position plus destination, route,
distance, progress, ETA, status, blockers, and next action, where the route is the ordered
allocation of free cash toward debt and goals.

The authoritative deterministic rules (cash flow, debt amortization, goal ETA, allocation,
status thresholds, and the reference acceptance cases) live in `specs/financial-domain/`
(`calculation-rules.md`, `status-rules.md`, `reference-cases.md`). These files are the shared
financial engine contract; features MUST satisfy them and MUST NOT define competing formulas.

The initial GPS statuses are `ON_TRACK`, `AT_RISK`, `OFF_TRACK`, `BLOCKED`, and `COMPLETED`.
Status rules MUST be documented and based on observable capacity and goal conditions, not a single
unexplained score. Initial scope focuses on state → goal → route → ETA → action; transaction
recording is out of scope unless it directly supplies this model.

## Development Workflow & Quality Gates

Each feature MUST pass through specification, clarification when material ambiguity remains,
technical planning, dependency-ordered tasks, implementation, acceptance testing, and convergence
against its approved artifacts. Changes to financial rules MUST update the relevant specification,
calculation tests, and explainability output together.

Before a feature is considered complete, reviewers MUST verify compliance with this constitution,
determinism, scenario isolation, stated assumptions, and user-facing explanations. Any deviation
requires a documented rationale and an approved amendment or explicit feature-level exception.

## Governance

This constitution supersedes conflicting project conventions for financial-domain work. Amendments
MUST be documented in this file with a Sync Impact Report, reviewed before implementation, and
versioned using semantic versioning: MAJOR for incompatible governance changes, MINOR for added or
materially expanded principles, and PATCH for clarifications that preserve meaning. Each feature
plan and review MUST include a compliance check against the active constitution.

**Version**: 1.2.0 | **Ratified**: 2026-08-24 | **Last Amended**: 2026-08-25
