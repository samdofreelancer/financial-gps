# Implementation Plan: Financial Roadmap

**Branch**: `005-financial-roadmap` | **Date**: 2026-08-24 | **Spec**: [spec.md](spec.md)

Generate an ordered, explainable route from existing debt and goals using the deterministic GPS
core. Persist only user priorities and stage configuration; recompute stages, ETA, blockers, and
next action from inputs. Expose read-only `/api/v1/roadmap` and render it in React.
