# ADR 0006: Vietnamese-first UI

- Status: Accepted
- Date: 2026-10-09

## Context

The product serves Vietnamese users, but the SPA grew with mixed-language copy:
English position cards (`Current position`, `Money coming in`) next to Vietnamese
debt/goal screens (`Quản lý nợ`, `Mục tiêu tài chính`). Mixed copy reads as
unfinished, doubles the wording reviewers must check per screen, and splits E2E/unit
assertions across two languages for the same concepts.

## Decision

1. **User-facing product copy is Vietnamese.** Labels, headings, buttons, hints, empty
   states, and validation messages render in Vietnamese across dashboard, profile,
   debts, and goals.
2. **Server contract stays English.** Enum values (`FIXED`, `DEBT_FREEDOM`, `ACTIVE`),
   provenance markers (`calculated`/`actual`), problem codes (`RESOURCE_NOT_FOUND`),
   and API field names are unchanged. The UI translates labels at the render boundary
   only (e.g. `GoalList.typeLabel`, `FinancialPositionCard.FIELD_LABELS`).
3. **Brand and proper nouns stay as-is.** `Financial GPS`, route names, and the `h1`
   asserted by E2E (`profileHeading: 'Financial GPS'`) are untouched.
4. **Tests and E2E move with the copy.** Unit assertions and `e2e/support/selectors.ts`
   plus page-objects reference the Vietnamese strings; `CHILDCARE_LABEL` tracks the
   `lineOptions` label.

## Rationale

- One language for users, one language for machines — the boundary is explicit and
  grep-able (a Vietnamese string in `api/` or an English label in `views/` is a smell).
- Translation is render-only, so domain tests, DTOs, and the database never churn
  with wording changes.

## Consequences

- Positive: consistent product voice; single-language E2E assertions.
- Negative: any future English locale is a fresh i18n project (no `vue-i18n`
  infrastructure was added — deliberate, see below).
- Follow-up constraint: new screens must ship Vietnamese copy from the first commit;
  reviewers should reject mixed-language UI.

## Alternatives considered

- **Full i18n framework (`vue-i18n` + locale files).** Rejected: exactly one locale is
  served; the framework would add indirection without a second language to justify it.
  Revisit if an English locale is ever requested.
- **English-first UI.** Rejected: the user base is Vietnamese; debts/goals screens
  (the newest, most user-tested) are already Vietnamese.
- **Leave mixed copy.** Rejected: perpetuates the inconsistency this decision removes.

## Related artifacts

- PR #30 (`feature/re-design-ui`): header unification, position-card translation,
  profile section translation
- `frontend/src/components/lineOptions.ts` (controlled Vietnamese vocabularies)
- `e2e/support/selectors.ts` (sole DOM-hook catalogue for E2E copy)
