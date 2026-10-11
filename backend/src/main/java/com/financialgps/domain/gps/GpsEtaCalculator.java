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
 * Calculates ETA (Estimated Time of Arrival) for the GPS destination.
 */
public final class GpsEtaCalculator {

    private GpsEtaCalculator() {
    }

    private static GpsProvenance.Entry provCalculated(String field, String detail) {
        return new GpsProvenance.AvailableEntry(GpsProvenance.Available.calculated(field, detail));
    }

    private static GpsProvenance.Entry provUnavailable(String field, String reasonCode, String explanation) {
        return new GpsProvenance.UnavailableEntry(new GpsProvenance.Unavailable(field, reasonCode, explanation));
    }

    public static GpsEta.Eta calculate(
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

        if (goal.isDebtFree()) {
            return calculateDebtFreedomEta(goal, debtSummary, asOfDate);
        } else {
            return calculateAmountGoalEta(goal, position, asOfDate, calcPolicy);
        }
    }

    private static GpsEta.Eta calculateAmountGoalEta(Goal goal,
                                                     GpsCurrentPosition position,
                                                     LocalDate asOfDate,
                                                     GoalCalculationPolicy calcPolicy) {
        // Get available capacity
        Money availableCapacity;
        GpsProvenance.Entry capacityProvenance;
        if (position.availableCapacity() instanceof GpsCurrentPosition.Available ac) {
            availableCapacity = ac.amount();
            capacityProvenance = new GpsProvenance.AvailableEntry(ac.provenance());
        } else {
            // Unavailable - no profile
            GpsProvenance.Entry provenance = provUnavailable(
                    "eta", "MISSING_FINANCIAL_PROFILE",
                    "Financial profile not set; Available Capacity unavailable");
            return GpsEta.unavailable(GpsEta.Reason.MISSING_FINANCIAL_PROFILE,
                    "Financial profile not set; cannot calculate ETA",
                    provenance);
        }

        // Check if goal is already completed
        GoalProgress progress = GoalProgressCalculator.evaluate(goal, asOfDate, calcPolicy);
        if (progress.remaining() != null && progress.remaining().amount().signum() == 0) {
            // Already completed
            GpsProvenance.Entry provenance = provCalculated("eta",
                    "Goal already completed (remaining = 0)");
            return GpsEta.calculated(asOfDate, 0, provenance);
        }

        Money remaining = progress.remaining();
        if (remaining == null || remaining.amount().signum() == 0) {
            GpsProvenance.Entry provenance = provCalculated("eta",
                    "Goal already completed (remaining = 0)");
            return GpsEta.calculated(asOfDate, 0, provenance);
        }

        // Check available capacity
        if (availableCapacity.amount().signum() <= 0) {
            GpsProvenance.Entry provenance = provCalculated("eta",
                    "Available Capacity = 0 → NO_AVAILABLE_CAPACITY");
            return GpsEta.unavailable(GpsEta.Reason.NO_AVAILABLE_CAPACITY,
                    "Available Capacity is 0; cannot make progress toward goal",
                    provenance);
        }

        // Calculate ETA periods: ceil(remaining / availableCapacity)
        BigDecimal remainingBD = remaining.amount();
        BigDecimal capBD = availableCapacity.amount();
        int etaPeriods = remainingBD.divide(capBD, 0, RoundingMode.CEILING).intValueExact();
        LocalDate etaDate = asOfDate.plusMonths(etaPeriods);

        String explanation = "ETA calculated: remaining " + remaining.asDecimalString()
                + " / available capacity " + availableCapacity.asDecimalString()
                + " = " + etaPeriods + " months (CEILING)";

        GpsProvenance.Entry provenance = provCalculated("eta", explanation);

        return GpsEta.calculated(etaDate, etaPeriods, provenance);
    }

    private static GpsEta.Eta calculateDebtFreedomEta(Goal goal,
                                                      DebtSummaryResult debtSummary,
                                                      LocalDate asOfDate) {
        if (debtSummary.portfolioProjection() == null) {
            GpsProvenance.Entry provenance = provUnavailable(
                    "eta", "PORTFOLIO_CONTAINS_BLOCKED_DEBTS",
                    "Debt portfolio projection unavailable");
            return GpsEta.unavailable(GpsEta.Reason.PORTFOLIO_CONTAINS_BLOCKED_DEBTS,
                    "Debt portfolio projection is blocked; cannot calculate ETA",
                    provenance);
        }

        var projection = debtSummary.portfolioProjection();

        if (projection.status() == com.financialgps.domain.debt.ProjectionStatus.BLOCKED) {
            String reasonCode = projection.reasonCode();
            GpsEta.Reason reason = mapBlockerReason(reasonCode);
            String explanation = projection.explanation();
            GpsProvenance.Entry provenance = provCalculated("eta",
                    "Portfolio projection BLOCKED: " + explanation);
            return GpsEta.unavailable(reason, explanation, provenance);
        }

        if (projection.status() == com.financialgps.domain.debt.ProjectionStatus.COMPLETED) {
            // Already debt-free
            GpsProvenance.Entry provenance = provCalculated("eta",
                    "Debt portfolio already COMPLETED");
            return GpsEta.calculated(asOfDate, 0, provenance);
        }

        // AVAILABLE projection
        LocalDate projectedDate = projection.projectedDebtFreeDate();
        int totalMonthsRemaining = projection.totalMonthsRemaining();

        String explanation = "Debt-freedom ETA from portfolio projection: "
                + projectedDate + " (" + totalMonthsRemaining + " months remaining)";
        GpsProvenance.Entry provenance = provCalculated("eta", explanation);

        return GpsEta.calculated(projectedDate, totalMonthsRemaining, provenance);
    }

    private static GpsEta.Reason mapBlockerReason(String reasonCode) {
        return switch (reasonCode) {
            case "PAYMENT_DOES_NOT_COVER_INTEREST" -> GpsEta.Reason.PAYMENT_DOES_NOT_COVER_INTEREST;
            case "PAYMENT_COVERS_ONLY_INTEREST" -> GpsEta.Reason.PAYMENT_COVERS_ONLY_INTEREST;
            case "INTEREST_RATE_MISSING" -> GpsEta.Reason.INTEREST_RATE_MISSING;
            case "PAYOFF_HORIZON_EXCEEDS_MAXIMUM" -> GpsEta.Reason.PAYOFF_HORIZON_EXCEEDS_MAXIMUM;
            case "PORTFOLIO_CONTAINS_BLOCKED_DEBTS" -> GpsEta.Reason.PORTFOLIO_CONTAINS_BLOCKED_DEBTS;
            default -> GpsEta.Reason.PORTFOLIO_CONTAINS_BLOCKED_DEBTS;
        };
    }
}