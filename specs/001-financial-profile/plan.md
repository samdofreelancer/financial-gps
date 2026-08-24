# Implementation Plan: Financial Profile

**Branch**: `001-financial-profile` | **Date**: 2026-08-24 | **Spec**: [spec.md](spec.md)

## Summary

Persist truthful recurring income, expenses, savings, emergency fund, and dependents, then return a deterministic current position. Java/Spring owns validation and `BigDecimal` calculations; React sends decimal strings and displays server-derived totals; PostgreSQL stores `numeric(19,2)`.

## Technical Context

Java 21, TypeScript 5.x, Spring Boot, React, TanStack Query, PostgreSQL, Flyway, JUnit 5, Testcontainers, Vitest, and Playwright. Use the shared `backend/` and `frontend/` modular-monolith layout in `specs/004-financial-gps/plan.md`.

## Constitution Check

Pass: inputs are explicit facts; totals are pure deterministic calculations; decimal precision is preserved; actual input and derived values are visibly distinct; only needed financial data persists.

## Data and Contract

`FinancialProfile`, `Income`, and `Expense` are persisted under `/api/v1/profile`, `/api/v1/incomes`, and `/api/v1/expenses`. The profile summary returns total income, total expenses, and available monthly cash flow with input provenance.

## Validation

Use the independent scenario and metrics in [spec.md](spec.md): a complete basic profile in under five minutes and exact totals for reference profiles.
