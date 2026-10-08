# Business Domain Overview

## Mission

Financial GPS is a personal finance planning platform designed to help a person understand their current financial position, model debt obligations, define goals, forecast future outcomes, and evaluate what actions are needed to reach financial stability and growth.

The product is centered on a core idea: people need a clear plan, not just a list of transactions or balances.

## Core domain concepts

### 1. Financial profile

The financial profile is the current snapshot of a person’s financial situation.

It typically includes:
- income
- expenses
- savings
- asset position
- liabilities
- debt obligations
- net worth
- cashflow signals

This is the baseline from which the system calculates financial health and trajectories.

### 2. Debt management

Debt is modeled as a structural component of the financial profile. It is important because it affects:
- monthly cashflow
- interest burden
- repayment timing
- ability to save or invest
- long-term affordability

The domain treats debt as both a financial obligation and a planning constraint.

### 3. Financial goals

Goals represent desired future outcomes, such as:
- buying a home
- paying off debt
- building emergency funds
- saving for education
- retirement planning
- travel or large purchase planning

Goals connect the current profile to a target state and help answer: “what must happen next?”

### 4. Financial GPS

The GPS capability models the route between current reality and a target financial future.

It answers questions like:
- where is the user today?
- where are they likely to be in 12 or 24 months?
- what changes are needed to reach the target?
- which actions create the biggest improvement?

This is the decision-support engine of the product.

### 5. Financial roadmap

The roadmap translates goals into a time-based plan.

It includes:
- sequencing of milestones
- timeline for actions
- priority ordering
- dependency-aware planning
- commitments over time

The roadmap turns abstract goals into structured execution.

### 6. Scenario planning

Scenario planning helps users simulate alternative futures.

Examples:
- salary increase
- higher interest rates
- unexpected expense
- accelerated debt payoff
- delayed home purchase
- optimistic vs conservative savings rate

This lets the user compare “what-if” decisions before committing to them.

### 7. Financial timing and allocation

This area deals with when money is available and how it should be allocated across priorities over time.

The product must reason about:
- monthly timing of income and expenses
- allocation between debt, savings, goals, and buffers
- priority ordering under limited cashflow
- timing-sensitive bottlenecks

### 8. Financial review

Financial review is the periodic evaluation stage. It compares the planned path against actual performance and highlights:
- what is going well
- what drifted off plan
- which actions should change
- whether the plan remains realistic

## Business value

The application provides value by helping users:
- see their current financial reality clearly
- reduce uncertainty in financial planning
- prioritize debts and goals
- forecast future outcomes
- simulate alternative actions
- build a finite, actionable plan

## Domain rules and invariants

The project appears to encode several important principles:

- financial decisions are time-sensitive
- debt directly affects future flexibility
- goals must be prioritized and sequenced
- the user’s current state is the starting point for planning
- future outcomes should be simulated, not guessed
- plans must remain realistic and actionable
- the system should support both planning and review

## Planned feature groups

The repository organizes features into stages:

1. financial profile
2. debt management
3. financial goals
4. financial GPS
5. financial roadmap
6. scenario planning
7. authentication
8. financial timing allocation
9. financial review

These feature groups all contribute to one shared business model: user financial health and forward planning.

## Domain boundary

The product’s domain is not generic accounting. It is purpose-built for personal planning and decision-making.

This means the domain should remain focused on:
- user financial health
- planning and projection
- trade-offs between debt, goals, and cashflow
- future-oriented decision support

It should not become a general ledger system, a payroll system, or a broad enterprise ERP component.

## Related reading

- [feature-map.md](feature-map.md)
- [../docs/README.md](../README.md)
