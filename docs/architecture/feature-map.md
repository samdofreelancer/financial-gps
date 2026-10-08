# Feature Map

## Product capabilities

The project is structured around a series of feature domains, each contributing to the financial planning lifecycle.

## Feature progression

### 1. Financial profile

Purpose:
- capture the user’s current financial baseline
- model balances, flows, and obligations
- establish the single source of truth for planning

### 2. Debt management

Purpose:
- track debts and repayment behavior
- understand obligations and interest structure
- reduce monthly debt burden where possible

### 3. Financial goals

Purpose:
- define target future states
- assign priority and urgency to aspirations
- connect goals to personal financial reality

### 4. Financial GPS

Purpose:
- project likely outcomes over time
- identify the path from current state to target state
- quantify the gap and the required actions

### 5. Financial roadmap

Purpose:
- translate strategy into a stepwise action plan
- sequence milestones and priorities
- show the path over time

### 6. Scenario planning

Purpose:
- evaluate alternative futures
- compare optimistic and conservative outcomes
- help the user decide under uncertainty

### 7. Authentication

Purpose:
- secure user access
- protect personal financial data
- provide ownership-specific authorization boundaries

### 8. Financial timing allocation

Purpose:
- manage when resources are available and how they are allocated
- support budgeting and prioritization across time windows

### 9. Financial review

Purpose:
- compare planned vs actual results
- measure drift and create corrective actions
- support iterative financial planning

## Relationship between domains

These features are not independent modules. They form a continuous lifecycle:

```text
Profile -> Debt -> Goals -> GPS -> Roadmap -> Scenario Planning -> Review
      \___________________________ Authentication and ownership secure all of it ________________________/
```

## Quality expectations

Each feature should answer:
- what is the user trying to achieve?
- what is the current state?
- what alternative paths exist?
- what is the recommended plan?
- how do we know the plan is valid?

## Domain-level architecture implication

The user-facing application is not merely a CRUD app. It is a planning system that combines baseline state, future goals, projected trajectories, and decision support under secure ownership boundaries.
