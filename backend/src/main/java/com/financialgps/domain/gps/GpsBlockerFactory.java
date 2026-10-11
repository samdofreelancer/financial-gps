package com.financialgps.domain.gps;

import com.financialgps.domain.model.Money;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.goal.GoalCapacity;
import com.financialgps.domain.goal.GoalCapacityCalculator;
import com.financialgps.domain.goal.GoalProgress;
import com.financialgps.domain.goal.GoalProgressCalculator;
import com.financialgps.domain.debt.DebtSummaryResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Factory for building GPS blockers and next actions.
 */
public final class GpsBlockerFactory {

    private GpsBlockerFactory() {
    }

    private static GpsProvenance.Entry provCalculated(String field, String detail) {
        return new GpsProvenance.AvailableEntry(GpsProvenance.Available.calculated(field, detail));
    }

    private static GpsProvenance.Entry provUnavailable(String field, String reasonCode, String explanation) {
        return new GpsProvenance.UnavailableEntry(new GpsProvenance.Unavailable(field, reasonCode, explanation));
    }

    public static List<GpsBlocker.Blocker> buildBlockers(
            Goal goal,
            GpsCurrentPosition position,
            DebtSummaryResult debtSummary,
            LocalDate asOfDate,
            GpsStatus.Result status,
            GoalCalculationPolicy calcPolicy) {

        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(debtSummary, "debtSummary");
        Objects.requireNonNull(asOfDate, "asOfDate");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(calcPolicy, "calcPolicy");

        List<GpsBlocker.Blocker> blockers = new ArrayList<>();

        // If status is BLOCKED, the blockers are already explained in the status condition
        // But we also create explicit blocker entries for the UI
        if (status.status() == GpsStatus.Status.BLOCKED) {
            GpsStatus.EvaluatedCondition condition = status.condition();
            if (condition instanceof GpsStatus.Blocked b) {
                blockers.add(new GpsBlocker.Blocker(
                        GpsBlocker.Code.valueOf(b.reasonCode()),
                        b.explanation(),
                        List.of("availableCapacity", "netCashFlow"),
                        b.provenance()
                ));
            }
        }

        // Check for debt portfolio blockers (even if not BLOCKED status)
        if (debtSummary.portfolioProjection() != null
                && debtSummary.portfolioProjection().status() == com.financialgps.domain.debt.ProjectionStatus.BLOCKED) {
            for (DebtSummaryResult.BlockedDebt bd : debtSummary.portfolioProjection().blockedDebts()) {
                GpsBlocker.Code code = mapReasonCode(bd.reasonCode());
                blockers.add(new GpsBlocker.Blocker(
                        code,
                        bd.explanation(),
                        List.of("debt:" + bd.creditor()),
                        provCalculated("blocker:" + bd.creditor(),
                                "Debt " + bd.creditor() + " blocked: " + bd.explanation())
                ));
            }
        }

        // Check for missing financial profile
        if (position.availableCapacity() instanceof GpsCurrentPosition.Unavailable) {
            blockers.add(new GpsBlocker.Blocker(
                    GpsBlocker.Code.MISSING_FINANCIAL_PROFILE,
                    "Financial profile not set: cannot calculate Available Capacity",
                    List.of("financialProfile"),
                    provUnavailable("blocker:financialProfile", "PROFILE_MISSING",
                            "Financial profile not set")
            ));
        }

        return List.copyOf(blockers);
    }

    public static List<GpsNextAction.Action> buildNextActions(
            Goal goal,
            GpsCurrentPosition position,
            DebtSummaryResult debtSummary,
            LocalDate asOfDate,
            GpsStatus.Result status,
            FinancialGpsResult.CapacityComparison capacityComparison,
            GoalCalculationPolicy calcPolicy) {

        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(debtSummary, "debtSummary");
        Objects.requireNonNull(asOfDate, "asOfDate");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(capacityComparison, "capacityComparison");
        Objects.requireNonNull(calcPolicy, "calcPolicy");

        List<GpsNextAction.Action> actions = new ArrayList<>();

        // Status-based actions
        switch (status.status()) {
            case BLOCKED -> {
                GpsStatus.EvaluatedCondition condition = status.condition();
                if (condition instanceof GpsStatus.Blocked b) {
                    switch (b.reasonCode()) {
                        case "NO_AVAILABLE_CAPACITY" -> {
                            // Calculate shortfall if goal has remaining
                            if (!goal.isDebtFree()) {
                                GoalProgress progress = GoalProgressCalculator.evaluate(goal, asOfDate, calcPolicy);
                                if (progress.remaining() != null && progress.remaining().amount().signum() > 0) {
                                    GoalCapacity capacity = GoalCapacityCalculator.evaluate(goal,
                                            Money.zero("VND"), asOfDate, calcPolicy);
                                    // The shortfall is the required monthly capacity
                                    if (capacity.requiredMonthlyCapacity() != null) {
                                        actions.add(new GpsNextAction.Action(
                                                GpsNextAction.Type.INCREASE_NET_CASH_FLOW,
                                                "Increase Net Cash Flow by at least "
                                                        + capacity.requiredMonthlyCapacity().asDecimalString()
                                                        + " " + capacity.requiredMonthlyCapacity().currency() + "/month",
                                                List.of("NO_AVAILABLE_CAPACITY"),
                                                provCalculated("action:increaseCashFlow",
                                                        "Required monthly capacity is " + capacity.requiredMonthlyCapacity().asDecimalString())
                                        ));
                                    }
                                }
                            }
                        }
                        case "MISSING_FINANCIAL_PROFILE" -> {
                            actions.add(new GpsNextAction.Action(
                                    GpsNextAction.Type.COMPLETE_FINANCIAL_PROFILE,
                                    "Complete your financial profile (income, expenses, savings, emergency fund, dependents)",
                                    List.of("MISSING_FINANCIAL_PROFILE"),
                                    provCalculated("action:completeProfile",
                                            "Financial profile required for GPS calculation")
                            ));
                        }
                        case "PAYMENT_DOES_NOT_COVER_INTEREST",
                             "PAYMENT_COVERS_ONLY_INTEREST" -> {
                            actions.add(new GpsNextAction.Action(
                                    GpsNextAction.Type.RAISE_DEBT_PAYMENT,
                                    "Raise the planned payment on the blocked debt(s) to cover at least monthly interest",
                                    List.of(b.reasonCode()),
                                    provCalculated("action:raisePayment",
                                            "Debt payment must exceed monthly interest to amortize")
                            ));
                        }
                        case "INTEREST_RATE_MISSING" -> {
                            actions.add(new GpsNextAction.Action(
                                    GpsNextAction.Type.SUPPLY_INTEREST_RATE,
                                    "Supply the missing interest rate for the affected debt(s)",
                                    List.of("INTEREST_RATE_MISSING"),
                                    provCalculated("action:supplyRate",
                                            "Interest rate required for debt projection")
                            ));
                        }
                        case "PAYOFF_HORIZON_EXCEEDS_MAXIMUM" -> {
                            actions.add(new GpsNextAction.Action(
                                    GpsNextAction.Type.REVIEW_DEBT_TERMS,
                                    "Review debt terms: payoff horizon exceeds safety limit; consider increasing payment",
                                    List.of("PAYOFF_HORIZON_EXCEEDS_MAXIMUM"),
                                    provCalculated("action:reviewDebt",
                                            "Payoff horizon exceeds maximum; review terms")
                            ));
                        }
                        case "PORTFOLIO_CONTAINS_BLOCKED_DEBTS" -> {
                            actions.add(new GpsNextAction.Action(
                                    GpsNextAction.Type.REVIEW_DEBT_TERMS,
                                    "Review blocked debts in portfolio: raise payments or supply missing rates",
                                    List.of("PORTFOLIO_CONTAINS_BLOCKED_DEBTS"),
                                    provCalculated("action:reviewDebts",
                                            "One or more debts in portfolio are blocked")
                            ));
                        }
                    }
                }
            }
            case AT_RISK, OFF_TRACK -> {
                // Shortfall-based action for amount-based goals
                if (!goal.isDebtFree() && capacityComparison.coverage().equals("SHORTFALL")
                        && capacityComparison.shortfall() != null) {
                    actions.add(new GpsNextAction.Action(
                            GpsNextAction.Type.INCREASE_NET_CASH_FLOW,
                            "Increase Net Cash Flow by at least "
                                    + capacityComparison.shortfall().asDecimalString()
                                    + " " + capacityComparison.shortfall().currency() + "/month to meet required capacity",
                            List.of("CAPACITY_SHORTFALL"),
                            provCalculated("action:increaseCashFlow",
                                    "Shortfall is " + capacityComparison.shortfall().asDecimalString())
                    ));
                }

                // For dated goals, suggest supplying target date if missing (but we have one if AT_RISK/OFF_TRACK)
                if (goal.targetDate() == null) {
                    actions.add(new GpsNextAction.Action(
                            GpsNextAction.Type.SUPPLY_TARGET_DATE,
                            "Set a target date to enable on-track/at-risk/off-track evaluation",
                            List.of("UNDATED_GOAL"),
                            provCalculated("action:supplyTargetDate",
                                    "Undated goal cannot be evaluated for lateness")
                    ));
                }
            }
            case ON_TRACK -> {
                // No specific action needed for ON_TRACK, but could suggest monitoring
            }
            case COMPLETED -> {
                // No action needed
            }
        }

        // Check for debt portfolio blockers that need action
        if (debtSummary.portfolioProjection() != null
                && debtSummary.portfolioProjection().status() == com.financialgps.domain.debt.ProjectionStatus.BLOCKED) {
            for (DebtSummaryResult.BlockedDebt bd : debtSummary.portfolioProjection().blockedDebts()) {
                GpsBlocker.Code code = mapReasonCode(bd.reasonCode());
                if (code == GpsBlocker.Code.PAYMENT_DOES_NOT_COVER_INTEREST
                        || code == GpsBlocker.Code.PAYMENT_COVERS_ONLY_INTEREST) {
                    actions.add(new GpsNextAction.Action(
                            GpsNextAction.Type.RAISE_DEBT_PAYMENT,
                            "Raise planned payment for " + bd.creditor() + " to cover monthly interest",
                            List.of(bd.reasonCode()),
                            provCalculated("action:raisePayment:" + bd.creditor(),
                                    bd.explanation())
                    ));
                } else if (code == GpsBlocker.Code.INTEREST_RATE_MISSING) {
                    actions.add(new GpsNextAction.Action(
                            GpsNextAction.Type.SUPPLY_INTEREST_RATE,
                            "Supply interest rate for " + bd.creditor(),
                            List.of("INTEREST_RATE_MISSING"),
                            provCalculated("action:supplyRate:" + bd.creditor(),
                                    bd.explanation())
                    ));
                }
            }
        }

        return List.copyOf(actions);
    }

    private static GpsBlocker.Code mapReasonCode(String reasonCode) {
        return switch (reasonCode) {
            case "PAYMENT_DOES_NOT_COVER_INTEREST" -> GpsBlocker.Code.PAYMENT_DOES_NOT_COVER_INTEREST;
            case "PAYMENT_COVERS_ONLY_INTEREST" -> GpsBlocker.Code.PAYMENT_COVERS_ONLY_INTEREST;
            case "INTEREST_RATE_MISSING" -> GpsBlocker.Code.INTEREST_RATE_MISSING;
            case "PAYOFF_HORIZON_EXCEEDS_MAXIMUM" -> GpsBlocker.Code.PAYOFF_HORIZON_EXCEEDS_MAXIMUM;
            case "PORTFOLIO_CONTAINS_BLOCKED_DEBTS" -> GpsBlocker.Code.PORTFOLIO_CONTAINS_BLOCKED_DEBTS;
            default -> GpsBlocker.Code.PORTFOLIO_CONTAINS_BLOCKED_DEBTS;
        };
    }
}