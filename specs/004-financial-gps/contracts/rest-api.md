# REST Contract: Financial GPS

## Conventions

- Base path: `/api/v1`.
- Requests and responses use JSON. Money is a decimal string plus ISO currency, never a JSON
  number. Dates use ISO `YYYY-MM-DD`.
- Successful mutations return the server's saved representation. Financial result-changing
  mutations are not optimistic on the client; affected GPS, roadmap, and goal queries are refetched.
- Validation and domain errors return RFC 9457 `ProblemDetail` with a stable `code`, human-readable
  `detail`, and `fieldErrors` when applicable.

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

## GPS Response Shape

```json
{
  "asOf": "2026-08-24",
  "destination": { "type": "GOAL", "id": "uuid", "name": "Emergency Fund" },
  "inputSnapshot": { "actual": {}, "assumptions": [] },
  "currentPosition": {},
  "distance": { "amount": "88000000.00", "currency": "VND" },
  "progressPercent": "18.5185",
  "capacityComparison": { "requiredMonthly": "12000000.00", "projectedMonthly": "15000000.00" },
  "eta": { "date": "2027-06-30", "availability": "CALCULATED" },
  "status": "ON_TRACK",
  "blockers": [],
  "nextActions": [],
  "explanations": []
}
```

Empty input sections are empty arrays/objects. Unavailable ETAs return
`availability: "UNAVAILABLE"` and a reason rather than a fabricated date.

Scenario evaluation is deliberately specified in `006-scenario-planning`. It will consume the GPS
response format above and must not mutate actual financial data.
