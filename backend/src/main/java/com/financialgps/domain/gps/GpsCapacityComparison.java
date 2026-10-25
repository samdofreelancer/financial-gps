package com.financialgps.domain.gps;

import com.financialgps.domain.model.Money;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.goal.GoalCapacity;
import com.financialgps.domain.goal.GoalCapacityCalculator;
import com.financialgps.domain.debt.DebtSummaryResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Calculates capacity comparison for GPS.
 * For amount-based dated goals: requiredMonthly vs projectedMonthly (Available Capacity).
 * For undated amount-based goals and all debt-freedom goals: NOT_APPLICABLE.
 */
public final class GpsCapacityComparison {

    private GpsCapacityComparison() {
    }

    public static FinancialGpsResult.CapacityComparison calculate(
            Goal goal,
            GpsCurrentPosition position,
            DebtSummaryResult debtSummary,
            LocalDate asOfDate,
            GoalCalculationPolicy calcPolicy) {

        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(debtSummary, "debtSummary");
        Objects.requireNonNull(asOfDate, "asOfDate");
        Objects.requireNonNull(calcPolicy, "calcPolicy");

        String currency = position.totalOutstandingDebt() instanceof GpsCurrentPosition.Available td
                ? td.amount().currency()
                : "VND";

        if (goal.isDebtFree()) {
            // Debt-freedom: capacity comparison is NOT_APPLICABLE (spec §7.2)
            return FinancialGpsResult.CapacityComparison.notApplicable(currency);
        }

        // Amount-based goal
        Money availableCapacity;
        if (position.availableCapacity() instanceof GpsCurrentPosition.Available ac) {
            availableCapacity = ac.amount();
        } else {
            // Unavailable - return not applicable with explanation
            return FinancialGpsResult.CapacityComparison.notApplicable(currency);
        }

        GoalCapacity capacity = GoalCapacityCalculator.evaluate(goal, availableCapacity, asOfDate, calcPolicy);

        if (goal.targetDate() == null) {
            // Undated goal: NOT_APPLICABLE
            return FinancialGpsResult.CapacityComparison.notApplicable(currency);
        }

        if (capacity.requiredMonthlyCapacity() == null) {
            // Expired target or completed - still compute comparison
            if (capacity.remaining().amount().signum() == 0) {
                // Completed
                return FinancialGpsResult.CapacityComparison.notApplicable(currency);
            }
            // Expired: required = remaining (full amount due now)
            Money required = capacity.remaining();
            if (availableCapacity.amount().compareTo(required.amount()) >= 0) {
                return FinancialGpsResult.CapacityComparison.meetsRequired(required, availableCapacity, currency);
            } else {
                BigDecimal diff = required.amount().subtract(availableCapacity.amount());
                Money shortfall = Money.of(diff.setScale(2, RoundingMode.HALF_UP).toPlainString(), currency);
                return FinancialGpsResult.CapacityComparison.shortfall(required, availableCapacity, shortfall, currency);
            }
        }

        Money required = capacity.requiredMonthlyCapacity();
        if (availableCapacity.amount().compareTo(required.amount()) >= 0) {
            return FinancialGpsResult.CapacityComparison.meetsRequired(required, availableCapacity, currency);
        } else {
            BigDecimal diff = required.amount().subtract(availableCapacity.amount());
            Money shortfall = Money.of(diff.setScale(2, RoundingMode.HALF_UP).toPlainString(), currency);
            return FinancialGpsResult.CapacityComparison.shortfall(required, availableCapacity, shortfall, currency);
        }
    }
}