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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Factory for building GPS explanations.
 * Every status, ETA, blocker, and next action must be explained by naming
 * the input values, the rule applied, and the threshold that decided it.
 */
public final class GpsExplanationFactory {

    private GpsExplanationFactory() {
    }

    private static GpsProvenance.Entry provCalculated(String field, String detail) {
        return new GpsProvenance.AvailableEntry(GpsProvenance.Available.calculated(field, detail));
    }

    private static GpsProvenance.Entry provActual(String field, String detail) {
        return new GpsProvenance.AvailableEntry(GpsProvenance.Available.actual(field, detail));
    }

    private static GpsProvenance.Entry provUnavailable(String field, String reasonCode, String explanation) {
        return new GpsProvenance.UnavailableEntry(new GpsProvenance.Unavailable(field, reasonCode, explanation));
    }

    public static List<GpsExplanation.Explanation> buildAll(
            Goal goal,
            GpsCurrentPosition position,
            DebtSummaryResult debtSummary,
            LocalDate asOfDate,
            FinancialGpsResult.CapacityComparison capacityComparison,
            GpsEta.Eta eta,
            GpsStatus.Result status,
            Object distance,
            GpsStatus.Policy statusPolicy,
            GoalCalculationPolicy calcPolicy) {

        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(debtSummary, "debtSummary");
        Objects.requireNonNull(asOfDate, "asOfDate");
        Objects.requireNonNull(capacityComparison, "capacityComparison");
        Objects.requireNonNull(eta, "eta");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(distance, "distance");
        Objects.requireNonNull(statusPolicy, "statusPolicy");
        Objects.requireNonNull(calcPolicy, "calcPolicy");

        List<GpsExplanation.Explanation> explanations = new ArrayList<>();

        // Position explanations
        explanations.addAll(buildPositionExplanations(position));

        // Distance explanation
        explanations.add(buildDistanceExplanation(goal, distance, asOfDate, calcPolicy));

        // Progress explanation (if amount-based)
        if (!goal.isDebtFree()) {
            explanations.add(buildProgressExplanation(goal, asOfDate, calcPolicy));
        } else {
            explanations.add(buildProgressNotMeasurableExplanation());
        }

        // Capacity comparison explanation
        explanations.add(buildCapacityComparisonExplanation(capacityComparison, goal, position));

        // ETA explanation
        explanations.add(buildEtaExplanation(goal, eta, position, asOfDate, calcPolicy));

        // Status explanation (from status result condition)
        explanations.add(buildStatusExplanation(status));

        // Route context explanation
        explanations.add(buildRouteContextExplanation());

        // Provenance explanations (summary)
        explanations.add(buildProvenanceSummaryExplanation(position, distance, eta, status));

        return List.copyOf(explanations);
    }

    private static List<GpsExplanation.Explanation> buildPositionExplanations(GpsCurrentPosition position) {
        List<GpsExplanation.Explanation> result = new ArrayList<>();

        for (GpsCurrentPosition.MoneyValue mv : position.allMoneyValues()) {
            String field = mv.field();
            String valueStr;
            String provenanceKind;

            if (mv instanceof GpsCurrentPosition.Available av) {
                valueStr = av.amount().asDecimalString() + " " + av.amount().currency();
                provenanceKind = av.provenance().kind();
            } else {
                GpsCurrentPosition.Unavailable un = (GpsCurrentPosition.Unavailable) mv;
                valueStr = "UNAVAILABLE (" + un.provenance().reasonCode() + ")";
                provenanceKind = "unavailable";
            }

            GpsProvenance.Entry provenance = mv instanceof GpsCurrentPosition.Available av
                    ? new GpsProvenance.AvailableEntry(av.provenance()) : new GpsProvenance.UnavailableEntry(((GpsCurrentPosition.Unavailable) mv).provenance());

            GpsExplanation.Explanation exp = new GpsExplanation.Explanation(
                    GpsExplanation.Category.POSITION,
                    field,
                    "Sum of active items effective on asOfDate (income/expense) or max(Net Cash Flow, 0) (availableCapacity)",
                    List.of(new GpsExplanation.InputReference(field, valueStr, provenanceKind)),
                    "N/A",
                    valueStr,
                    provenance
            );
            result.add(exp);
        }

        // DTI explanation
        if (position.dti() != null) {
            var dti = position.dti();
            String valueStr = dti.ratio() != null ? dti.ratio().toPlainString() : "UNAVAILABLE";
            String provenanceKind = dti.provenance() instanceof GpsProvenance.AvailableEntry ? "calculated" : "unavailable";

            result.add(new GpsExplanation.Explanation(
                    GpsExplanation.Category.POSITION,
                    "dti",
                    "Total minimum monthly debt divided by monthly income",
                    List.of(new GpsExplanation.InputReference("dti", valueStr, provenanceKind)),
                    "N/A",
                    dti.status() + (dti.reasonCode() != null ? ": " + dti.reasonCode() : ""),
                    dti.provenance()
            ));
        }

        // Dependents
        if (position.dependents() != null) {
            result.add(new GpsExplanation.Explanation(
                    GpsExplanation.Category.POSITION,
                    "dependents",
                    "Stored dependents count from profile",
                    List.of(new GpsExplanation.InputReference("dependents", position.dependents().toString(), "actual")),
                    "N/A",
                    position.dependents().toString(),
                    provActual("dependents", "stored dependents count from profile")
            ));
        }

        return result;
    }

    private static GpsExplanation.Explanation buildDistanceExplanation(Goal goal, Object distance,
                                                                       LocalDate asOfDate,
                                                                       GoalCalculationPolicy calcPolicy) {
        String field = "distance";
        String rule;
        List<GpsExplanation.InputReference> inputs = new ArrayList<>();
        String threshold = "N/A";
        String outcome;
        GpsProvenance.Entry provenance;

        if (distance instanceof GpsDistance.AmountBased ab) {
            rule = "remaining = max(targetAmount - currentAmount, 0)";
            inputs.add(new GpsExplanation.InputReference("targetAmount", ab.targetAmount().asDecimalString(), "actual"));
            inputs.add(new GpsExplanation.InputReference("currentAmount", ab.currentAmount().asDecimalString(), "actual"));
            outcome = "remaining = " + ab.remaining().asDecimalString();
            provenance = ab.provenance();
        } else {
            GpsDistance.DebtFreedom df = (GpsDistance.DebtFreedom) distance;
            rule = "distance = totalOutstandingDebt from Feature 002 portfolio projection";
            inputs.add(new GpsExplanation.InputReference("totalOutstandingDebt", df.totalOutstandingDebt().asDecimalString(), "calculated"));
            outcome = "totalOutstandingDebt = " + df.totalOutstandingDebt().asDecimalString();
            provenance = df.provenance();
        }

        return new GpsExplanation.Explanation(
                GpsExplanation.Category.DISTANCE, field, rule, inputs, threshold, outcome, provenance);
    }

    private static GpsExplanation.Explanation buildProgressExplanation(Goal goal, LocalDate asOfDate,
                                                                       GoalCalculationPolicy calcPolicy) {
        GoalProgress progress = GoalProgressCalculator.evaluate(goal, asOfDate, calcPolicy);
        String field = "progressPercent";
        String rule = "progress = currentAmount / targetAmount (scale 4, HALF_UP); 1.0000 when remaining = 0";
        List<GpsExplanation.InputReference> inputs = new ArrayList<>();
        String threshold = "N/A";
        String outcome;
        GpsProvenance.Entry provenance;

        inputs.add(new GpsExplanation.InputReference("targetAmount", goal.targetAmount().asDecimalString(), "actual"));
        inputs.add(new GpsExplanation.InputReference("currentAmount", goal.currentAmount().asDecimalString(), "actual"));

        if (progress.progress() != null) {
            outcome = "progress = " + progress.progress().toPlainString();
            provenance = provCalculated("progress", rule);
        } else {
            outcome = "progress = null (debt-freedom)";
            provenance = provUnavailable("progress", "PROGRESS_NOT_MEASURABLE",
                    "Progress not measurable for debt-freedom goals");
        }

        return new GpsExplanation.Explanation(
                GpsExplanation.Category.PROGRESS, field, rule, inputs, threshold, outcome, provenance);
    }

    private static GpsExplanation.Explanation buildProgressNotMeasurableExplanation() {
        return new GpsExplanation.Explanation(
                GpsExplanation.Category.PROGRESS,
                "progressPercent",
                "Progress not measurable for debt-freedom: no authoritative starting baseline for total debt reduction",
                List.of(new GpsExplanation.InputReference("progressPercent", "null", "unavailable")),
                "N/A",
                "PROGRESS_NOT_MEASURABLE",
                provUnavailable("progressPercent", "PROGRESS_NOT_MEASURABLE",
                        "Progress not measurable for debt-freedom goals")
        );
    }

    private static GpsExplanation.Explanation buildCapacityComparisonExplanation(
            FinancialGpsResult.CapacityComparison cc, Goal goal, GpsCurrentPosition position) {
        String field = "capacityComparison";
        String rule;
        List<GpsExplanation.InputReference> inputs = new ArrayList<>();
        String threshold;
        String outcome;
        GpsProvenance.Entry provenance;

        if ("NOT_APPLICABLE".equals(cc.coverage())) {
            rule = "Capacity comparison not applicable: undated goal or debt-freedom destination (compared by date, not monthly money)";
            threshold = "N/A";
            outcome = "NOT_APPLICABLE";
            provenance = provCalculated("capacityComparison", rule);
        } else {
            rule = "requiredMonthly = ceil(remaining / monthsRemaining); compared to Available Capacity";
            inputs.add(new GpsExplanation.InputReference("requiredMonthly", cc.requiredMonthly().asDecimalString(), "calculated"));
            inputs.add(new GpsExplanation.InputReference("availableCapacity", cc.projectedMonthly().asDecimalString(), "calculated"));
            threshold = "requiredMonthly <= availableCapacity → MEETS_REQUIRED";
            outcome = cc.coverage() + (cc.shortfall() != null ? ": shortfall " + cc.shortfall().asDecimalString() : "");
            provenance = provCalculated("capacityComparison", cc.explanation());
        }

        return new GpsExplanation.Explanation(
                GpsExplanation.Category.CAPACITY_COMPARISON, field, rule, inputs, threshold, outcome, provenance);
    }

    private static GpsExplanation.Explanation buildEtaExplanation(Goal goal, GpsEta.Eta eta,
                                                                   GpsCurrentPosition position,
                                                                   LocalDate asOfDate,
                                                                   GoalCalculationPolicy calcPolicy) {
        String field = "eta";
        String rule;
        List<GpsExplanation.InputReference> inputs = new ArrayList<>();
        String threshold;
        String outcome;
        GpsProvenance.Entry provenance;

        if (eta instanceof GpsEta.CalculatedEta ce) {
            if (goal.isDebtFree()) {
                rule = "ETA = projectedDebtFreeDate from Feature 002 portfolio projection; periods = totalMonthsRemaining";
                threshold = "N/A";
                outcome = "date = " + ce.date() + ", periods = " + ce.periods();
            } else {
                rule = "ETA periods = ceil(remaining / Available Capacity); date = asOfDate + periods months";
                GoalProgress progress = GoalProgressCalculator.evaluate(goal, asOfDate, calcPolicy);
                Money availableCapacity = ((GpsCurrentPosition.Available) position.availableCapacity()).amount();
                inputs.add(new GpsExplanation.InputReference("remaining", progress.remaining().asDecimalString(), "calculated"));
                inputs.add(new GpsExplanation.InputReference("availableCapacity", availableCapacity.asDecimalString(), "calculated"));
                threshold = "Available Capacity > 0";
                outcome = "date = " + ce.date() + ", periods = " + ce.periods();
            }
            provenance = ce.provenance();
        } else {
            GpsEta.UnavailableEta ue = (GpsEta.UnavailableEta) eta;
            rule = "ETA unavailable: " + ue.reason();
            threshold = "N/A";
            outcome = "UNAVAILABLE: " + ue.explanation();
            provenance = ue.provenance();
        }

        return new GpsExplanation.Explanation(
                GpsExplanation.Category.ETA, field, rule, inputs, threshold, outcome, provenance);
    }

    private static GpsExplanation.Explanation buildStatusExplanation(GpsStatus.Result status) {
        String field = "status";
        String rule;
        List<GpsExplanation.InputReference> inputs = new ArrayList<>();
        String threshold;
        String outcome;
        GpsProvenance.Entry provenance;

        GpsStatus.EvaluatedCondition condition = status.condition();
        if (condition instanceof GpsStatus.Completed c) {
            rule = "COMPLETED: destination route has arrived (remaining = 0 or portfolio COMPLETED)";
            threshold = "remaining = 0 or portfolio COMPLETED";
            outcome = "COMPLETED (" + c.reason() + ")";
            provenance = c.provenance();
        } else if (condition instanceof GpsStatus.Blocked b) {
            rule = "BLOCKED: no finite route exists";
            threshold = b.reasonCode();
            outcome = "BLOCKED (" + b.reasonCode() + ")";
            provenance = b.provenance();
        } else if (condition instanceof GpsStatus.OnTrack ot) {
            rule = "ON_TRACK: finite route with lateness = 0 (ETA at or before target date)";
            inputs.add(new GpsExplanation.InputReference("requiredMonthly", ot.requiredMonthly().asDecimalString(), "calculated"));
            inputs.add(new GpsExplanation.InputReference("availableCapacity", ot.projectedMonthly().asDecimalString(), "calculated"));
            inputs.add(new GpsExplanation.InputReference("monthsRemaining", String.valueOf(ot.monthsRemaining()), "calculated"));
            inputs.add(new GpsExplanation.InputReference("etaPeriods", String.valueOf(ot.etaPeriods()), "calculated"));
            threshold = "lateness = 0";
            outcome = "ON_TRACK (lateness = " + ot.lateness() + ")";
            provenance = ot.provenance();
        } else if (condition instanceof GpsStatus.AtRisk ar) {
            rule = "AT_RISK: 1 <= lateness <= latenessTolerance";
            inputs.add(new GpsExplanation.InputReference("requiredMonthly", ar.requiredMonthly().asDecimalString(), "calculated"));
            inputs.add(new GpsExplanation.InputReference("availableCapacity", ar.projectedMonthly().asDecimalString(), "calculated"));
            inputs.add(new GpsExplanation.InputReference("monthsRemaining", String.valueOf(ar.monthsRemaining()), "calculated"));
            inputs.add(new GpsExplanation.InputReference("etaPeriods", String.valueOf(ar.etaPeriods()), "calculated"));
            threshold = "1 <= lateness <= " + ar.latenessTolerance();
            outcome = "AT_RISK (lateness = " + ar.lateness() + ", tolerance = " + ar.latenessTolerance() + ")";
            provenance = ar.provenance();
        } else if (condition instanceof GpsStatus.OffTrack ot) {
            rule = "OFF_TRACK: lateness > latenessTolerance";
            inputs.add(new GpsExplanation.InputReference("requiredMonthly", ot.requiredMonthly().asDecimalString(), "calculated"));
            inputs.add(new GpsExplanation.InputReference("availableCapacity", ot.projectedMonthly().asDecimalString(), "calculated"));
            inputs.add(new GpsExplanation.InputReference("monthsRemaining", String.valueOf(ot.monthsRemaining()), "calculated"));
            inputs.add(new GpsExplanation.InputReference("etaPeriods", String.valueOf(ot.etaPeriods()), "calculated"));
            threshold = "lateness > " + ot.latenessTolerance();
            outcome = "OFF_TRACK (lateness = " + ot.lateness() + ", tolerance = " + ot.latenessTolerance() + ")";
            provenance = ot.provenance();
        } else if (condition instanceof GpsStatus.UndatedOnTrack uot) {
            rule = "ON_TRACK (undated): finite route exists, no target date to miss";
            inputs.add(new GpsExplanation.InputReference("etaPeriods", String.valueOf(uot.etaPeriods()), "calculated"));
            inputs.add(new GpsExplanation.InputReference("availableCapacity", uot.projectedMonthly().asDecimalString(), "calculated"));
            threshold = "N/A (undated)";
            outcome = "ON_TRACK (undated, etaPeriods = " + uot.etaPeriods() + ")";
            provenance = uot.provenance();
        } else if (condition instanceof GpsStatus.DebtFreedomOnTrack dfot) {
            rule = "ON_TRACK (debt-freedom): projected debt-free date at or before target date (or undated with finite payoff)";
            inputs.add(new GpsExplanation.InputReference("projectedDebtFreeDate", dfot.projectedDebtFreeDate().toString(), "calculated"));
            inputs.add(new GpsExplanation.InputReference("totalMonthsRemaining", String.valueOf(dfot.totalMonthsRemaining()), "calculated"));
            if (dfot.targetDate() != null) {
                inputs.add(new GpsExplanation.InputReference("targetDate", dfot.targetDate().toString(), "actual"));
            }
            threshold = dfot.targetDate() != null ? "lateness = 0" : "N/A (undated)";
            outcome = "ON_TRACK (lateness = " + dfot.lateness() + ")";
            provenance = dfot.provenance();
        } else if (condition instanceof GpsStatus.DebtFreedomAtRisk dfar) {
            rule = "AT_RISK (debt-freedom): 1 <= lateness <= latenessTolerance";
            inputs.add(new GpsExplanation.InputReference("projectedDebtFreeDate", dfar.projectedDebtFreeDate().toString(), "calculated"));
            inputs.add(new GpsExplanation.InputReference("totalMonthsRemaining", String.valueOf(dfar.totalMonthsRemaining()), "calculated"));
            inputs.add(new GpsExplanation.InputReference("targetDate", dfar.targetDate().toString(), "actual"));
            threshold = "1 <= lateness <= " + dfar.latenessTolerance();
            outcome = "AT_RISK (lateness = " + dfar.lateness() + ", tolerance = " + dfar.latenessTolerance() + ")";
            provenance = dfar.provenance();
        } else if (condition instanceof GpsStatus.DebtFreedomOffTrack dfot) {
            rule = "OFF_TRACK (debt-freedom): lateness > latenessTolerance";
            inputs.add(new GpsExplanation.InputReference("projectedDebtFreeDate", dfot.projectedDebtFreeDate().toString(), "calculated"));
            inputs.add(new GpsExplanation.InputReference("totalMonthsRemaining", String.valueOf(dfot.totalMonthsRemaining()), "calculated"));
            inputs.add(new GpsExplanation.InputReference("targetDate", dfot.targetDate().toString(), "actual"));
            threshold = "lateness > " + dfot.latenessTolerance();
            outcome = "OFF_TRACK (lateness = " + dfot.lateness() + ", tolerance = " + dfot.latenessTolerance() + ")";
            provenance = dfot.provenance();
        } else {
            rule = "Unknown status condition";
            threshold = "N/A";
            outcome = "UNKNOWN";
            provenance = provCalculated("status", "unknown condition");
        }

        return new GpsExplanation.Explanation(
                GpsExplanation.Category.STATUS, field, rule, inputs, threshold, outcome, provenance);
    }

    private static GpsExplanation.Explanation buildRouteContextExplanation() {
        GpsRouteContext.Context ctx = GpsRouteContext.singleDestination();
        return new GpsExplanation.Explanation(
                GpsExplanation.Category.PROVENANCE,
                "routeContext",
                ctx.description(),
                List.of(),
                "N/A",
                ctx.policy() + ": " + ctx.description(),
                provCalculated("routeContext", ctx.description())
        );
    }

    private static GpsExplanation.Explanation buildProvenanceSummaryExplanation(
            GpsCurrentPosition position, Object distance, GpsEta.Eta eta, GpsStatus.Result status) {
        return new GpsExplanation.Explanation(
                GpsExplanation.Category.PROVENANCE,
                "provenanceSummary",
                "Every value in this result is labelled actual, assumed, or calculated with source",
                List.of(),
                "N/A",
                "See provenance field for per-value labels",
                provCalculated("provenanceSummary", "All values carry provenance")
        );
    }
}