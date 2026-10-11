package com.financialgps.domain.gps;

import com.financialgps.domain.model.Money;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.goal.GoalCapacity;
import com.financialgps.domain.goal.GoalCapacityCalculator;
import com.financialgps.domain.goal.GoalProgress;
import com.financialgps.domain.goal.GoalProgressCalculator;
import com.financialgps.domain.debt.DebtSummaryResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * GPS Status Policy: evaluates status according to the deterministic precedence:
 * COMPLETED → BLOCKED → ON_TRACK → AT_RISK → OFF_TRACK
 */
public final class GpsStatusPolicy {

    private GpsStatusPolicy() {
    }

    private static GpsProvenance.Entry provCalculated(String field, String detail) {
        return new GpsProvenance.AvailableEntry(GpsProvenance.Available.calculated(field, detail));
    }

    public static GpsStatus.Result evaluate(
            Goal goal,
            GpsCurrentPosition position,
            DebtSummaryResult debtSummary,
            LocalDate asOfDate,
            GpsStatus.Policy policy,
            GoalCalculationPolicy calcPolicy) {

        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(debtSummary, "debtSummary");
        Objects.requireNonNull(asOfDate, "asOfDate");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(calcPolicy, "calcPolicy");

        // 1. COMPLETED - check first (highest precedence)
        GpsStatus.Result completed = checkCompleted(goal, debtSummary, asOfDate, calcPolicy);
        if (completed != null) {
            return completed;
        }

        // 2. BLOCKED - check if no finite route exists
        GpsStatus.Result blocked = checkBlocked(goal, position, debtSummary, asOfDate, calcPolicy);
        if (blocked != null) {
            return blocked;
        }

        // 3. ON_TRACK / AT_RISK / OFF_TRACK - evaluate based on lateness
        return evaluateLateness(goal, position, debtSummary, asOfDate, policy, calcPolicy);
    }

    private static GpsStatus.Result checkCompleted(Goal goal, DebtSummaryResult debtSummary,
                                                    LocalDate asOfDate, GoalCalculationPolicy calcPolicy) {
        if (goal.isDebtFree()) {
            // DEBT_FREEDOM goal: completed iff portfolio is COMPLETED
            if (debtSummary.portfolioProjection() != null
                    && debtSummary.portfolioProjection().status() == com.financialgps.domain.debt.ProjectionStatus.COMPLETED) {
                String explanation = "Debt portfolio is COMPLETED: no ACTIVE debts remain";
                GpsProvenance.Entry provenance = provCalculated("status", explanation);
                GpsStatus.EvaluatedCondition condition = new GpsStatus.Completed("DEBT_FREE", provenance);
                return new GpsStatus.Result(GpsStatus.Status.COMPLETED, explanation, condition);
            }
        } else {
            // Amount-based goal: completed iff remaining = 0
            GoalProgress progress = GoalProgressCalculator.evaluate(goal, asOfDate, calcPolicy);
            if (progress.remaining() != null && progress.remaining().amount().signum() == 0) {
                String explanation = "Goal completed: current amount >= target amount";
                GpsProvenance.Entry provenance = provCalculated("status", explanation);
                GpsStatus.EvaluatedCondition condition = new GpsStatus.Completed("AMOUNT_REACHED", provenance);
                return new GpsStatus.Result(GpsStatus.Status.COMPLETED, explanation, condition);
            }
        }
        return null;
    }

    private static GpsStatus.Result checkBlocked(Goal goal, GpsCurrentPosition position,
                                                 DebtSummaryResult debtSummary,
                                                 LocalDate asOfDate, GoalCalculationPolicy calcPolicy) {
        // Check available capacity
        GpsCurrentPosition.MoneyValue availableCapacity = position.availableCapacity();
        boolean hasAvailableCapacity = availableCapacity instanceof GpsCurrentPosition.Available
                && ((GpsCurrentPosition.Available) availableCapacity).amount().amount().signum() > 0;

        // Check if goal has remaining > 0
        boolean hasRemaining = false;
        if (goal.isDebtFree()) {
            hasRemaining = debtSummary.totalOutstandingDebt().signum() > 0;
        } else {
            GoalProgress progress = GoalProgressCalculator.evaluate(goal, asOfDate, calcPolicy);
            hasRemaining = progress.remaining() != null && progress.remaining().amount().signum() > 0;
        }

        if (hasRemaining && !hasAvailableCapacity) {
            // Check if profile is missing
            boolean profileMissing = availableCapacity instanceof GpsCurrentPosition.Unavailable;

            if (profileMissing) {
                GpsProvenance.Entry provenance = new GpsProvenance.UnavailableEntry(((GpsCurrentPosition.Unavailable) availableCapacity).provenance());
                String explanation = "Financial profile is not set: cannot determine Available Capacity";
                GpsStatus.EvaluatedCondition condition = new GpsStatus.Blocked(
                        "MISSING_FINANCIAL_PROFILE", explanation, provenance);
                return new GpsStatus.Result(GpsStatus.Status.BLOCKED, explanation, condition);
            } else {
                // Available Capacity is 0
                String explanation = "Available Capacity is 0: Net Cash Flow <= 0, cannot make progress toward goal";
                GpsProvenance.Entry provenance = provCalculated("status", "Available Capacity = 0 → BLOCKED (NO_AVAILABLE_CAPACITY)");
                GpsStatus.EvaluatedCondition condition = new GpsStatus.Blocked(
                        "NO_AVAILABLE_CAPACITY", explanation, provenance);
                return new GpsStatus.Result(GpsStatus.Status.BLOCKED, explanation, condition);
            }
        }

        // Check debt blockers from portfolio projection
        if (debtSummary.portfolioProjection() != null
                && debtSummary.portfolioProjection().status() == com.financialgps.domain.debt.ProjectionStatus.BLOCKED) {
            String reasonCode = debtSummary.portfolioProjection().reasonCode();
            String explanation = debtSummary.portfolioProjection().explanation();
            GpsProvenance.Entry provenance = provCalculated("status", "Portfolio projection BLOCKED: " + explanation);
            GpsStatus.EvaluatedCondition condition = new GpsStatus.Blocked(
                    reasonCode, explanation, provenance);
            return new GpsStatus.Result(GpsStatus.Status.BLOCKED, explanation, condition);
        }

        return null;
    }

    private static GpsStatus.Result evaluateLateness(Goal goal, GpsCurrentPosition position,
                                                     DebtSummaryResult debtSummary,
                                                     LocalDate asOfDate,
                                                     GpsStatus.Policy policy,
                                                     GoalCalculationPolicy calcPolicy) {
        if (goal.isDebtFree()) {
            return evaluateDebtFreedomLateness(goal, debtSummary, asOfDate, policy);
        } else {
            return evaluateAmountGoalLateness(goal, position, asOfDate, policy, calcPolicy);
        }
    }

    private static GpsStatus.Result evaluateAmountGoalLateness(Goal goal,
                                                               GpsCurrentPosition position,
                                                               LocalDate asOfDate,
                                                               GpsStatus.Policy policy,
                                                               GoalCalculationPolicy calcPolicy) {
        // Get available capacity
        Money availableCapacity;
        if (position.availableCapacity() instanceof GpsCurrentPosition.Available ac) {
            availableCapacity = ac.amount();
        } else {
            // Should have been caught by BLOCKED check
            availableCapacity = Money.zero("VND");
        }

        // Get goal capacity (includes required monthly capacity)
        GoalCapacity capacity = GoalCapacityCalculator.evaluate(goal, availableCapacity, asOfDate, calcPolicy);

        // Get ETA periods
        int etaPeriods;
        if (capacity.monthsRemaining() != null && capacity.requiredMonthlyCapacity() != null) {
            // Dated goal with required capacity
            Money remaining = capacity.remaining();
            if (remaining.amount().signum() == 0) {
                etaPeriods = 0;
            } else if (availableCapacity.amount().signum() > 0) {
                // ceil(remaining / availableCapacity)
                BigDecimal remainingBD = remaining.amount();
                BigDecimal capBD = availableCapacity.amount();
                etaPeriods = remainingBD.divide(capBD, 0, RoundingMode.CEILING).intValueExact();
            } else {
                etaPeriods = 0; // BLOCKED would have caught this
            }
        } else if (capacity.monthsRemaining() == null) {
            // Undated goal
            Money remaining = capacity.remaining();
            if (remaining.amount().signum() == 0) {
                etaPeriods = 0;
            } else if (availableCapacity.amount().signum() > 0) {
                BigDecimal remainingBD = remaining.amount();
                BigDecimal capBD = availableCapacity.amount();
                etaPeriods = remainingBD.divide(capBD, 0, RoundingMode.CEILING).intValueExact();
            } else {
                etaPeriods = 0;
            }
        } else {
            // Expired target date
            etaPeriods = 0; // BLOCKED or handled by required monthly = remaining
        }

        int monthsRemaining = capacity.monthsRemaining() == null ? 0 : capacity.monthsRemaining();
        int lateness = Math.max(etaPeriods - monthsRemaining, 0);

        GpsProvenance.Entry provenance = provCalculated("status",
                "lateness=" + lateness + ", monthsRemaining=" + monthsRemaining + ", etaPeriods=" + etaPeriods);

        if (goal.targetDate() == null) {
            // Undated goal: never AT_RISK/OFF_TRACK, always ON_TRACK if not BLOCKED/COMPLETED
            String explanation = "Undated goal with finite route: ETA " + etaPeriods + " months";
            GpsStatus.EvaluatedCondition condition = new GpsStatus.UndatedOnTrack(
                    etaPeriods, availableCapacity, provenance);
            return new GpsStatus.Result(GpsStatus.Status.ON_TRACK, explanation, condition);
        }

        // Dated goal: evaluate lateness bands
        if (lateness == 0) {
            // ON_TRACK: ETA at or before target date
            String explanation = "ETA (" + etaPeriods + " months) is at or before target date ("
                    + monthsRemaining + " months remaining); lateness = 0";
            GpsStatus.EvaluatedCondition condition = new GpsStatus.OnTrack(
                    capacity.requiredMonthlyCapacity(), availableCapacity, monthsRemaining, etaPeriods, lateness, provenance);
            return new GpsStatus.Result(GpsStatus.Status.ON_TRACK, explanation, condition);
        } else if (lateness <= policy.latenessTolerance()) {
            // AT_RISK: 1 <= lateness <= tolerance
            String explanation = "ETA is " + lateness + " period(s) late (within tolerance of "
                    + policy.latenessTolerance() + "); lateness = " + lateness + " (1.."
                    + policy.latenessTolerance() + " → AT_RISK)";
            GpsStatus.EvaluatedCondition condition = new GpsStatus.AtRisk(
                    capacity.requiredMonthlyCapacity(), availableCapacity, monthsRemaining, etaPeriods,
                    lateness, policy.latenessTolerance(), provenance);
            return new GpsStatus.Result(GpsStatus.Status.AT_RISK, explanation, condition);
        } else {
            // OFF_TRACK: lateness > tolerance
            String explanation = "ETA is " + lateness + " period(s) late (exceeds tolerance of "
                    + policy.latenessTolerance() + "); lateness = " + lateness + " > "
                    + policy.latenessTolerance() + " → OFF_TRACK";
            GpsStatus.EvaluatedCondition condition = new GpsStatus.OffTrack(
                    capacity.requiredMonthlyCapacity(), availableCapacity, monthsRemaining, etaPeriods,
                    lateness, policy.latenessTolerance(), provenance);
            return new GpsStatus.Result(GpsStatus.Status.OFF_TRACK, explanation, condition);
        }
    }

    private static GpsStatus.Result evaluateDebtFreedomLateness(Goal goal,
                                                                DebtSummaryResult debtSummary,
                                                                LocalDate asOfDate,
                                                                GpsStatus.Policy policy) {
        if (debtSummary.portfolioProjection() == null) {
            // Should not happen if not BLOCKED
            return null;
        }

        var projection = debtSummary.portfolioProjection();
        if (projection.status() != com.financialgps.domain.debt.ProjectionStatus.AVAILABLE) {
            return null; // BLOCKED would have caught this
        }

        LocalDate projectedDate = projection.projectedDebtFreeDate();
        int totalMonthsRemaining = projection.totalMonthsRemaining();

        GpsProvenance.Entry provenance = provCalculated("status",
                "debt-freedom: projectedDate=" + projectedDate + ", totalMonthsRemaining=" + totalMonthsRemaining);

        if (goal.targetDate() == null) {
            // Undated debt-freedom: ON_TRACK if finite payoff exists
            String explanation = "Debt-freedom goal undated; portfolio projects payoff by "
                    + projectedDate + " (" + totalMonthsRemaining + " months)";
            GpsStatus.EvaluatedCondition condition = new GpsStatus.DebtFreedomOnTrack(
                    projectedDate, totalMonthsRemaining, null, 0, provenance);
            return new GpsStatus.Result(GpsStatus.Status.ON_TRACK, explanation, condition);
        }

        // Dated debt-freedom: compare projected date with target date
        int monthsToTarget = GoalCapacityCalculator.monthsBetween(asOfDate, goal.targetDate());
        int lateness = Math.max(totalMonthsRemaining - monthsToTarget, 0);

        if (lateness == 0) {
            String explanation = "Projected debt-free date " + projectedDate + " is at or before target date "
                    + goal.targetDate() + "; lateness = 0";
            GpsStatus.EvaluatedCondition condition = new GpsStatus.DebtFreedomOnTrack(
                    projectedDate, totalMonthsRemaining, goal.targetDate(), lateness, provenance);
            return new GpsStatus.Result(GpsStatus.Status.ON_TRACK, explanation, condition);
        } else if (lateness <= policy.latenessTolerance()) {
            String explanation = "Projected debt-free date " + projectedDate + " is " + lateness
                    + " period(s) late vs target date " + goal.targetDate()
                    + " (within tolerance of " + policy.latenessTolerance() + ")";
            GpsStatus.EvaluatedCondition condition = new GpsStatus.DebtFreedomAtRisk(
                    projectedDate, totalMonthsRemaining, goal.targetDate(), lateness,
                    policy.latenessTolerance(), provenance);
            return new GpsStatus.Result(GpsStatus.Status.AT_RISK, explanation, condition);
        } else {
            String explanation = "Projected debt-free date " + projectedDate + " is " + lateness
                    + " period(s) late vs target date " + goal.targetDate()
                    + " (exceeds tolerance of " + policy.latenessTolerance() + ")";
            GpsStatus.EvaluatedCondition condition = new GpsStatus.DebtFreedomOffTrack(
                    projectedDate, totalMonthsRemaining, goal.targetDate(), lateness,
                    policy.latenessTolerance(), provenance);
            return new GpsStatus.Result(GpsStatus.Status.OFF_TRACK, explanation, condition);
        }
    }
}