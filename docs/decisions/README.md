# Decision Log

This folder records major architecture and engineering decisions.

## Format

Use ADR-style notes with:
- title
- status
- date
- context
- decision
- consequences
- alternatives considered

## Recommended naming

- 0001-<short-title>.md
- 0002-<short-title>.md

## Current ADRs

- [adr-0001-authentication-and-session-model.md](adr-0001-authentication-and-session-model.md)
- [adr-0002-csrf-double-submit-protection.md](adr-0002-csrf-double-submit-protection.md)
- [adr-0003-ownership-based-authorization-and-resource-hiding.md](adr-0003-ownership-based-authorization-and-resource-hiding.md)
- [adr-0004-ports-and-adapters-ddd-boundaries.md](adr-0004-ports-and-adapters-ddd-boundaries.md)
- [adr-0005-pure-financial-domain-engine.md](adr-0005-pure-financial-domain-engine.md)
- [adr-0006-vietnamese-first-ui.md](adr-0006-vietnamese-first-ui.md)

## Examples of decisions to record

- use of Java/Spring backend
- choice of frontend framework
- auth model and identity flow
- database and persistence decisions
- deployment target and infrastructure strategy
- team conventions for API contracts

## Core rule

Whenever a decision changes the system, update or add a decision record instead of leaving the rationale in chat or commit messages only.
