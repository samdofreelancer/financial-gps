package com.financialgps.domain.gps;

import com.financialgps.domain.model.Money;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.goal.GoalProgress;
import com.financialgps.domain.goal.GoalProgressCalculator;
import com.financialgps.domain.debt.DebtSummaryResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Calculates distance to destination for GPS.
 */
public final class GpsDistanceCalculator {

    private GpsDistanceCalculator() {
    }

    public static Object calculate(
            Goal goal,
            DebtSummaryResult debtSummary,
            LocalDate asOfDate,
            GoalCalculationPolicy calcPolicy) {

        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(debtSummary, "debtSummary");
        Objects.requireNonNull(asOfDate, "asOfDate");
        Objects.requireNonNull(calcPolicy, "calcPolicy");

        if (goal.isDebtFree()) {
            String currency = debtSummary.currency();
            Money totalOutstanding = Money.of(debtSummary.totalOutstandingDebt().toPlainString(), currency);
            GpsProvenance.Entry provenance = new GpsProvenance.AvailableEntry(GpsProvenance.Available.calculated("distance",
                    "Total outstanding debt from Feature 002 portfolio projection"));
            return new GpsDistance.DebtFreedom(totalOutstanding, provenance);
        } else {
            GoalProgress progress = GoalProgressCalculator.evaluate(goal, asOfDate, calcPolicy);
            Money remaining = progress.remaining();
            // Progress follows the Feature 003 rules (zero-target safe, clamped to [0, 1]).
            BigDecimal progressPercent = progress.progress();
            GpsProvenance.Entry provenance = new GpsProvenance.AvailableEntry(GpsProvenance.Available.calculated("distance",
                    "remaining = max(targetAmount - currentAmount, 0); progress = currentAmount / targetAmount"));
            return new GpsDistance.AmountBased(remaining, goal.targetAmount(), goal.currentAmount(),
                    provenance, progressPercent);
        }
    }
}