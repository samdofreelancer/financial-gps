# Implementation Plan: Financial Goals

**Branch**: `003-financial-goals` | **Date**: 2026-08-24 | **Spec**: [spec.md](spec.md)

Build destinations with explicit target, condition, progress, optional date, and required capacity.
Shared decimal and profile foundations remain source of truth; calculation is pure and explainable.

## Data and Contract

Persist amount-based goals and priority. Expose `/api/v1/goals` CRUD plus goal projections with
remaining amount, progress, required capacity, and date-affordability explanation.
