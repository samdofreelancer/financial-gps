# Runbooks

Operational playbooks for day-to-day execution and recovery.

## Typical contents

- local startup steps
- dependency bootstrapping
- database initialization
- service health checks
- deployment procedures
- rollback steps
- environment reset checklist

## Common sections

1. Purpose
2. Preconditions
3. Steps
4. Verification
5. Rollback
6. Troubleshooting hints

## Existing runbooks

- [local-development.md](local-development.md)
- [docker-compose.md](docker-compose.md)

## Intended extension

- backend-startup.md
- frontend-startup.md
- deployment.md
- rollback.md

## Standard warning

A runbook is only useful if it is tested and updated when the environment changes.
