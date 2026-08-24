<!--
Sync Impact Report
- Version change: template → 1.0.0
- Modified principles: template placeholders → I–X Financial GPS governance principles
- Added sections: Financial Data & Projection Rules; Development Workflow & Quality Gates
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

## Financial Data & Projection Rules

The core domain comprises FinancialProfile, Income, Expense, Debt, Account, Goal, Roadmap,
RoadmapStage, and Scenario. Financial GPS is defined as current position plus destination, route,
distance, progress, ETA, status, blockers, and next action.

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

**Version**: 1.0.0 | **Ratified**: 2026-08-24 | **Last Amended**: 2026-08-24
