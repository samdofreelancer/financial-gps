# Contributing Guide

## Development standards

- keep business logic in the domain layer
- avoid framework leakage into domain packages
- prefer explicit, test-backed behavior over hidden magic
- update docs when behavior, config, or setup changes
- write or update tests for bug fixes and new behaviors

## Branch and code hygiene

- keep changes focused and reviewable
- avoid mixing architectural refactors with unrelated edits
- maintain meaningful commit messages
- update relevant docs when the change impacts setup, architecture, API contracts, or troubleshooting

## Review checklist

Before merging a change, confirm:
- tests pass for the impacted area
- architecture boundaries are still respected
- security assumptions remain valid
- docs reflect the new reality
- no environment-specific assumptions are hidden in code

## Practical workflow

1. read the relevant spec and architecture docs
2. run the targeted tests or dev flow
3. implement the change
4. validate behavior against explicit acceptance criteria
5. update relevant docs
6. submit pull request with clear rationale

## Security and correctness rules

- never weaken ownership checks
- keep CSRF behavior explicit and validated
- protect session cookies appropriately
- ensure exported data remains deterministic and secure
- treat authentication and authorization as first-class concerns
