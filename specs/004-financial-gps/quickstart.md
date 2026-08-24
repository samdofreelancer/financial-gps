# Quickstart Validation: Financial GPS

## Prerequisites

- Java 21, Node.js current LTS, Docker, and PostgreSQL via the project development environment.
- An authenticated test account with an empty financial profile.

## Validation Journey

1. Start the backend with its development profile and the frontend development server.
2. Create a profile with VND currency, monthly income of `74,000,000`, monthly expenses of
   `43,000,000`, savings of `20,000,000`, and emergency fund of `20,000,000`.
3. Add an active debt with `120,000,000` outstanding balance and a monthly payment that permits
   payoff. Add an Emergency Fund goal with target `108,000,000`, current amount `20,000,000`, and
   a target date.
4. Open the GPS for that goal. Confirm it displays actual inputs separately from calculated values,
   remaining distance, required/projected capacity, named status, ETA or unavailable reason,
   blockers, and next action.
5. Recalculate using the same as-of date. Confirm the full GPS result is identical.
6. Update one actual monthly expense, recalculate, and confirm the GPS result changes only because
   the displayed input snapshot changed.

## Automated Checks

- Run backend domain tests covering reference calculations, rounding, statuses, missing
  destinations, non-positive cash flow, and insufficient debt payment.
- Run backend integration tests against PostgreSQL to verify migrations, `numeric` precision,
  constraints, and error contracts.
- Run frontend tests for money input/display, explanation/status components, and query refresh after
  a financial mutation.
- Run the journey above with isolated Playwright data.

## Expected Outcome

The user receives an explainable, deterministic GPS result. Any unavailable ETA or non-viable route
names its blocking input/condition. Scenario comparison is validated separately in feature 006.
