package com.financialgps.domain.gps;

import com.financialgps.domain.model.Money;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.goal.GoalProgress;
import com.financialgps.domain.goal.GoalProgressCalculator;
import com.financialgps.domain.debt.DebtSummaryResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            BigDecimal progressPercent;
            if (remaining.amount().signum() == 0) {
                progressPercent = BigDecimal.ONE.setScale(calcPolicy.ratioScale(), RoundingMode.HALF_UP);
            } else {
                progressPercent = goal.currentAmount().amount()
                        .divide(goal.targetAmount().amount(), calcPolicy.ratioScale(), calcPolicy.roundingMode());
                if (progressPercent.compareTo(BigDecimal.ZERO) < 0) {
                    progressPercent = BigDecimal.ZERO.setScale(calcPolicy.ratioScale());
                }
                if (progressPercent.compareTo(BigDecimal.ONE) >= 0) {
                    progressPercent = BigDecimal.ONE.setScale(calcPolicy.ratioScale());
                }
            }
            GpsProvenance.Entry provenance = new GpsProvenance.AvailableEntry(GpsProvenance.Available.calculated("distance",
                    "remaining = max(targetAmount - currentAmount, 0); progress = currentAmount / targetAmount"));
            return new GpsDistance.AmountBased(remaining, goal.targetAmount(), goal.currentAmount(),
                    provenance, progressPercent);
        }
    }
}