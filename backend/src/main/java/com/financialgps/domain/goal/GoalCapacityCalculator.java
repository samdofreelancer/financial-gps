package com.financialgps.domain.goal;

import com.financialgps.domain.model.DomainValidationException;
import com.financialgps.domain.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Pure capacity calculator (spec §4.3): monthsRemaining is the largest m with
 * asOf.plusMonths(m) &le; targetDate; required uses CEILING so it is never understated; expired
 * targets expose the full remainder as due now; undated and completed goals carry null/zero
 * capacity with NOT_APPLICABLE coverage.
 */
public final class GoalCapacityCalculator {

    private GoalCapacityCalculator() {
    }

    public static GoalCapacity evaluate(Goal goal, Money availableCapacity, LocalDate asOfDate,
                                        GoalCalculationPolicy policy) {
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(availableCapacity, "availableCapacity");
        Objects.requireNonNull(asOfDate, "asOfDate");
        Objects.requireNonNull(policy, "policy");
        String currency = goal.targetAmount().currency();
        if (!currency.equals(availableCapacity.currency())) {
            throw new DomainValidationException("CURRENCY_MISMATCH",
                    "Available capacity must be expressed in the goal currency (single-currency v1)");
        }

        GoalProgress progress = GoalProgressCalculator.evaluate(goal, asOfDate, policy);
        Money remaining = progress.remaining();

        if (remaining.amount().signum() == 0) {
            Money zero = Money.zero(currency);
            Integer months = goal.targetDate() == null ? null
                    : monthsBetween(asOfDate, goal.targetDate());
            return new GoalCapacity(remaining, months,
                    zero, availableCapacity, GoalCapacity.CapacityCoverage.NOT_APPLICABLE, null,
                    GoalCapacity.DateFeasibility.COMPLETED, asOfDate,
                    "Goal already completed; no monthly capacity is required.");
        }
        if (goal.targetDate() == null) {
            return new GoalCapacity(remaining, null, null, availableCapacity,
                    GoalCapacity.CapacityCoverage.NOT_APPLICABLE, null,
                    GoalCapacity.DateFeasibility.UNDATED, asOfDate,
                    "No required capacity can be scheduled without a target date; the goal remains valid.");
        }
        int months = monthsBetween(asOfDate, goal.targetDate());
        if (months <= 0) {
            Money required = remaining;
            BigDecimal shortfallValue = required.amount().subtract(availableCapacity.amount());
            GoalCapacity.CapacityCoverage coverage = availableCapacity.amount()
                    .compareTo(required.amount()) >= 0
                            ? GoalCapacity.CapacityCoverage.MEETS_REQUIRED
                            : GoalCapacity.CapacityCoverage.SHORTFALL;
            Money shortfallView = coverage == GoalCapacity.CapacityCoverage.MEETS_REQUIRED
                    ? Money.zero(currency)
                    : Money.of(shortfallValue.setScale(policy.monetaryScale(), RoundingMode.HALF_UP)
                            .toPlainString(), currency);
            return new GoalCapacity(remaining, 0, required, availableCapacity, coverage,
                    shortfallView, GoalCapacity.DateFeasibility.EXPIRED_TARGET_DATE, asOfDate,
                    "Target date " + goal.targetDate() + " is expired as of " + asOfDate
                            + "; the full remainder " + required.asDecimalString() + " " + currency
                            + " is due now.");
        }
        BigDecimal requiredValue = remaining.amount().divide(BigDecimal.valueOf(months),
                policy.monetaryScale(), policy.capacityRoundingMode());
        Money required = Money.of(requiredValue.toPlainString(), currency);
        GoalCapacity.CapacityCoverage coverage;
        Money shortfallView;
        if (availableCapacity.amount().compareTo(required.amount()) >= 0) {
            coverage = GoalCapacity.CapacityCoverage.MEETS_REQUIRED;
            shortfallView = Money.zero(currency);
        } else {
            coverage = GoalCapacity.CapacityCoverage.SHORTFALL;
            BigDecimal diff = required.amount().subtract(availableCapacity.amount())
                    .setScale(policy.monetaryScale(), RoundingMode.HALF_UP);
            shortfallView = Money.of(diff.toPlainString(), currency);
        }
        String explanation = coverage == GoalCapacity.CapacityCoverage.MEETS_REQUIRED
                ? "Required " + required.asDecimalString() + " " + currency
                        + "/month is within Available Capacity " + availableCapacity.asDecimalString()
                        + " " + currency + "."
                : "Required " + required.asDecimalString() + " " + currency + "/month exceeds Available Capacity "
                        + availableCapacity.asDecimalString() + " " + currency + " by shortfall "
                        + shortfallView.asDecimalString() + " " + currency + ".";
        return new GoalCapacity(remaining, months, required, availableCapacity, coverage,
                shortfallView, GoalCapacity.DateFeasibility.DATED, asOfDate, explanation);
    }

    /** Largest integer m &ge; 0 with asOf.plusMonths(m) &le; target; 0 when target &le; asOf. */
    public static int monthsBetween(LocalDate asOf, LocalDate target) {
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(target, "target");
        int months = 0;
        while (!asOf.plusMonths(months + 1).isAfter(target)) {
            months++;
        }
        return months;
    }

    public static GoalCapacity evaluate(Goal goal, Money availableCapacity, LocalDate asOfDate) {
        return evaluate(goal, availableCapacity, asOfDate, GoalCalculationPolicy.defaults());
    }

    /**
     * String-based entry for the application layer: the available amount is parsed here so the
     * application lane never constructs domain Money itself (architecture guard). The amount is
     * re-expressed in the goal currency (single-currency MVP: numeric value preserved).
     *
     * <p>Both currency arguments must agree and the amount must parse — a corrupt position is
     * reported as a validation failure, never silently replaced with zero.
     */
    public static GoalCapacity evaluate(Goal goal, String availableAmount, String availableCurrency,
                                        LocalDate asOfDate, GoalCalculationPolicy policy) {
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(asOfDate, "asOfDate");
        Objects.requireNonNull(policy, "policy");
        String currency = goal.targetAmount().currency();
        if (availableAmount == null || availableCurrency == null) {
            throw new DomainValidationException("POSITION_UNAVAILABLE",
                    "Available capacity is required to evaluate goal capacity");
        }
        if (!currency.equals(availableCurrency)) {
            throw new DomainValidationException("CURRENCY_MISMATCH",
                    "Available capacity must be expressed in the goal currency (single-currency v1)");
        }
        Money available;
        try {
            available = Money.of(new java.math.BigDecimal(availableAmount)
                    .setScale(policy.monetaryScale(), java.math.RoundingMode.HALF_UP).toPlainString(),
                    currency);
        } catch (NumberFormatException | DomainValidationException e) {
            throw new DomainValidationException("POSITION_INVALID",
                    "Available capacity is not a valid monetary amount");
        }
        return evaluate(goal, available, asOfDate, policy);
    }
}
