# Architecture

This folder stores the system view of the project and the main runtime flows.

## Scope

Document:
- service boundaries
- front-end and back-end responsibilities
- data flow
- authentication and authorization flows
- infrastructure and deployment topology

## Existing artifacts

- auth-flow.mmd
- auth-sequence.mmd
- debt-flow.mmd
- k8s-network-flow.mmd
- k8s-network-flow.png

## Current content

- overview.md — one-page architecture summary
- system-context.md — actors, flows, and runtime boundaries
- business-domain.md — business meaning and domain model
- feature-map.md — feature progression across the application
- c4-container.md — C4 context/container diagrams and topology constraints
- data-model.md — persistence schema, ERD, and conventions
- api-contract.md — HTTP endpoint index and cross-cutting rules
- security.md — auth, session, ownership, and trust boundaries
- deployment.md — local and cluster deployment runtime topology

## Questions to answer

- What are the main services and responsibilities?
- Which systems talk to each other?
- Where are the critical business flows?
- Which parts are domain-critical and which are technical plumbing?
