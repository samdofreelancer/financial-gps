# Implementation Plan: Debt Management

**Branch**: `002-debt-management` | **Date**: 2026-08-24 | **Spec**: [spec.md](spec.md)

Build deterministic debt facts, portfolio totals, and payoff projection on the shared Java/Spring,
React, PostgreSQL foundation. Use `BigDecimal`/`numeric`; show a blocker instead of inventing a
payoff date. This passes the constitution through pure calculation policies and explainable output.

## Data and Contract

Persist creditor, balance, rate, payment, dates, and status. Expose CRUD at `/api/v1/debts` and a
debt summary used by GPS. `GET /api/v1/debts/summary` returns totals, debt-to-income ratio, payoff
date or unavailable reason, and blockers.
