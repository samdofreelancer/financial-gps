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

## Recommended content

- overview.md — one-page architecture summary
- integration.md — external systems and dependencies
- security.md — auth and trust boundaries
- deployment.md — local and cloud runtime topology

## Questions to answer

- What are the main services and responsibilities?
- Which systems talk to each other?
- Where are the critical business flows?
- Which parts are domain-critical and which are technical plumbing?
