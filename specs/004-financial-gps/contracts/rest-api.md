# REST Contract: Financial GPS

## Conventions

- Base path: `/api/v1`.
- Requests and responses use JSON. Money is a decimal string plus ISO currency, never a JSON
  number. Dates use ISO `YYYY-MM-DD`. A monetary value whose required input is absent is reported
  as `null` with an availability state, never as an available `"0.00"`.
- Successful mutations return the server's saved representation. Financial result-changing
  mutations are not optimistic on the client; affected GPS, roadmap, and goal queries are refetched.
- Validation and domain errors return **RFC 7807** `ProblemDetail` (the implemented convention,
  `api/common/ProblemDetailAdvice`) with a stable `code`, human-readable `detail`, and `violations`
  when applicable. Field-level validation errors use `violations`, not `fieldErrors`. (RFC 9457 is
  the later successor media type; this project standardizes on the implemented 7807 behavior.)

## Resource Operations

| Method and path | Purpose |
|-----------------|---------|
| `GET /profile` | Read actual financial profile and input summaries |
| `PUT /profile` | Create or replace profile-level facts |
| `POST /incomes`, `PUT /incomes/{id}`, `DELETE /incomes/{id}` | Manage monthly income items |
| `POST /expenses`, `PUT /expenses/{id}`, `DELETE /expenses/{id}` | Manage monthly expenses |
| `POST /debts`, `PUT /debts/{id}`, `DELETE /debts/{id}` | Manage debt facts |
| `POST /goals`, `PUT /goals/{id}`, `DELETE /goals/{id}` | Manage destinations |
| `GET /gps?destinationType=goal&destinationId={uuid}&asOf={date}` | Calculate baseline GPS |

`destinationId` MUST reference a non-ARCHIVED goal owned by the caller. An ACTIVE or COMPLETED goal
is a valid destination (a COMPLETED goal returns `status: "COMPLETED"`, not an error); an ARCHIVED,
unknown, or other-owner goal returns `404` and is indistinguishable from a missing id.

## GPS Response Shape

```json
{
  "asOf": "2026-08-24",
  "destination": { "type": "GOAL", "id": "uuid", "name": "Emergency Fund", "goalType": "EMERGENCY_FUND" },
  "inputSnapshot": { "actual": {}, "assumptions": [] },
  "currentPosition": {
    "availableCapacity": { "value": "15000000.00", "currency": "VND", "availability": "AVAILABLE", "provenance": "calculated" }
  },
  "distance": { "amount": "88000000.00", "currency": "VND" },
  "progressPercent": "18.5185",
  "capacityComparison": { "requiredMonthly": "12000000.00", "projectedMonthly": "15000000.00" },
  "eta": { "date": "2027-06-30", "periods": 10, "availability": "CALCULATED", "reason": null },
  "status": "ON_TRACK",
  "blockers": [],
  "nextActions": [],
  "explanations": [],
  "missingInputs": [],
  "provenance": []
}
```

- **Debt-freedom destination** (`goalType: "DEBT_FREEDOM"`): `capacityComparison` is `null`
  (not applicable — compared by date, not monthly money), `distance` is the Feature 002
  `totalOutstandingDebt`, `eta.periods` is Feature 002 `totalMonthsRemaining` (a period **count**,
  not money), and `progressPercent` is `null` with reason `PROGRESS_NOT_MEASURABLE`. Feature 003
  completes the goal itself iff the same 002 portfolio is `COMPLETED` (decision D-6, `spec.md` §16);
  the goal's advisory `targetAmount`/`currentAmount` never drive this. The check uses the **current**
  portfolio and is not sticky: if a new ACTIVE debt appears after completion, the destination is no
  longer `COMPLETED` (`GC-005`, `status-014`).
- **Missing Financial Profile**: profile-dependent money (`income`, `expense`, `netCashFlow`,
  `availableCapacity`) is returned `null` with `availability: "UNAVAILABLE"` and reason
  `PROFILE_MISSING` (never an available `"0.00"`); `missingInputs` contains
  `"FINANCIAL_PROFILE"`; `eta.availability` is `"UNAVAILABLE"`; `status` is `"BLOCKED"`. A real
  zero capacity with a profile present is reported as `"0.00"` with `availability: "AVAILABLE"` and
  provenance `calculated` (available capacity is `max(Net Cash Flow, 0)`, not a stored `actual`).

Empty input sections are empty arrays/objects. Unavailable ETAs return
`availability: "UNAVAILABLE"` and a reason rather than a fabricated date.

Scenario evaluation is deliberately specified in `006-scenario-planning`. It will consume the GPS
response format above and must not mutate actual financial data.
