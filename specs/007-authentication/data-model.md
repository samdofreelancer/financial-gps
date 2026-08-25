# Data Model: 007-authentication

> Platform-layer entities only. Financial resources appear here solely as owner-scoped tables;
> their internal fields belong to features 001–003/006/008/009 and the domain contract.

## Entities

### Account (Owner)

| Field | Rules |
|---|---|
| `id` | UUID PK |
| `email` | Display case preserved; format validated; **uniqueness enforced case-insensitively** via unique index on `lower(email)` |
| `password_hash` | BCrypt string; never selected into DTOs; never logged |
| `role` | Single allowed value `OWNER` in this release (column kept for future roles; no ADMIN) |
| `created_at` | Instant, UTC |

Lifecycle: `ACTIVE` (row exists) → `DELETED` (row removed by confirmed hard delete; cascade removes
all owned rows). No soft-delete state on the account itself.

### OwnerId (application-layer value type)

Immutable UUID wrapper produced by `CurrentOwnerProvider`; passed explicitly into services and
owner-scoped repositories. Sessions/principals stop here — services and everything below them see
only this value.

### Session

Provided by Spring Session JDBC (`SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES`) — no custom entity.
Bound to an authenticated account id; idle timeout from configuration; invalidated on logout and
on account deletion.

### Owned financial resources (contract, not redefined here)

Every table listed in the spec's Data Lifecycle (`profile`, `income`, `expense`, `debt`, `goal`,
`timeline_change`, `cash_allocation_rule`, `gps_snapshot`, `review_ledger_entry`, and scenario
tables from 006 when they exist):

- carries `owner_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE`
- has an index on `owner_id`
- registers in the ownership registry used by cascade/orphan/export tests.

Resource-level states (`Create → Update → Archive → Delete`) are owned by features 002/003/009;
this feature guarantees only that whatever rows exist are removed with their owner.

## Export bundle shape (`GET /api/v1/account/export`)

```json
{
  "formatVersion": 1,
  "exportedAt": "2026-08-25T10:00:00Z",
  "account": { "email": "user@example.com", "createdAt": "..." },
  "profile": { "...": "per 001" },
  "incomes": [ "…ordered by id…" ],
  "expenses": [ "…" ],
  "debts": [ "…" ],
  "goals": [ "…" ],
  "timelineChanges": [ "…" ],
  "allocationRules": [ "…" ],
  "gpsSnapshots": [ "…" ],
  "reviewLedger": [ "…" ]
}
```

Arrays are sorted deterministically by `id`; empty collections serialize as `[]` (never omitted)
so two exports of unchanged data are byte-comparable (supports SC-006 equality checks).

## Validation rules recap

- Email: RFC-ish shape check + case-insensitive uniqueness (FR-001/FR-004).
- Password: min length / character classes from configuration; violations explained (FR-005);
  max length 128 (hash DoS guard).
- Deletion request must carry exact `"confirmation": "DELETE"` (FR-012).
