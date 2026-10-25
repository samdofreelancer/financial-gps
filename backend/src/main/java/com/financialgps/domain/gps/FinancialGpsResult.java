package com.financialgps.domain.gps;

import com.financialgps.domain.model.Money;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Complete Financial GPS Result.
 * Immutable, explainable, deterministic projection from current position to destination.
 */
public final class FinancialGpsResult {

    public enum DestinationType {
        GOAL,
        DEBT_FREEDOM
    }

    public record Destination(
            DestinationType type,
            String goalId,        // null for DEBT_FREEDOM
            String goalName,      // null for DEBT_FREEDOM
            String goalType       // null for DEBT_FREEDOM
    ) {
        public Destination {
            Objects.requireNonNull(type, "type");
            if (type == DestinationType.GOAL) {
                Objects.requireNonNull(goalId, "goalId");
                Objects.requireNonNull(goalName, "goalName");
                Objects.requireNonNull(goalType, "goalType");
            }
        }

        public static Destination goal(String goalId, String goalName, String goalType) {
            return new Destination(DestinationType.GOAL, goalId, goalName, goalType);
        }

        public static Destination debtFreedom() {
            return new Destination(DestinationType.DEBT_FREEDOM, null, null, null);
        }
    }

    public record CapacityComparison(
            Money requiredMonthly,
            Money projectedMonthly,
            String coverage,        // "MEETS_REQUIRED" | "SHORTFALL" | "NOT_APPLICABLE"
            Money shortfall,        // null when coverage is MEETS_REQUIRED or NOT_APPLICABLE
            String explanation
    ) {
        public CapacityComparison {
            Objects.requireNonNull(requiredMonthly, "requiredMonthly");
            Objects.requireNonNull(projectedMonthly, "projectedMonthly");
            Objects.requireNonNull(coverage, "coverage");
            Objects.requireNonNull(explanation, "explanation");
        }

        public static CapacityComparison notApplicable(String currency) {
            Money zero = Money.zero(currency);
            return new CapacityComparison(zero, zero, "NOT_APPLICABLE", null,
                    "Capacity comparison is not applicable for debt-freedom destinations (compared by date, not monthly money)");
        }

        public static CapacityComparison meetsRequired(Money required, Money projected, String currency) {
            return new CapacityComparison(required, projected, "MEETS_REQUIRED", Money.zero(currency),
                    "Required " + required.asDecimalString() + " " + currency
                            + "/month is within Available Capacity " + projected.asDecimalString() + " " + currency + ".");
        }

        public static CapacityComparison shortfall(Money required, Money projected, Money shortfall, String currency) {
            return new CapacityComparison(required, projected, "SHORTFALL", shortfall,
                    "Required " + required.asDecimalString() + " " + currency + "/month exceeds Available Capacity "
                            + projected.asDecimalString() + " " + currency + " by shortfall "
                            + shortfall.asDecimalString() + " " + currency + ".");
        }
    }

    public static final class Builder {
        private LocalDate asOf;
        private Destination destination;
        private GpsCurrentPosition currentPosition;
        private Object distance;  // GpsDistance.AmountBased or GpsDistance.DebtFreedom
        private GpsEta.Eta eta;
        private GpsStatus.Result status;
        private CapacityComparison capacityComparison;
        private GpsRouteContext.Context routeContext;
        private List<GpsBlocker.Blocker> blockers;
        private List<GpsNextAction.Action> nextActions;
        private List<GpsExplanation.Explanation> explanations;
        private List<GpsProvenance.Entry> provenance;
        private List<String> missingInputs;

        public Builder asOf(LocalDate value) {
            this.asOf = value;
            return this;
        }

        public Builder destination(Destination value) {
            this.destination = value;
            return this;
        }

        public Builder currentPosition(GpsCurrentPosition value) {
            this.currentPosition = value;
            return this;
        }

        public Builder distance(Object value) {
            this.distance = value;
            return this;
        }

        public Builder eta(GpsEta.Eta value) {
            this.eta = value;
            return this;
        }

        public Builder status(GpsStatus.Result value) {
            this.status = value;
            return this;
        }

        public Builder capacityComparison(CapacityComparison value) {
            this.capacityComparison = value;
            return this;
        }

        public Builder routeContext(GpsRouteContext.Context value) {
            this.routeContext = value;
            return this;
        }

        public Builder blockers(List<GpsBlocker.Blocker> value) {
            this.blockers = value;
            return this;
        }

        public Builder nextActions(List<GpsNextAction.Action> value) {
            this.nextActions = value;
            return this;
        }

        public Builder explanations(List<GpsExplanation.Explanation> value) {
            this.explanations = value;
            return this;
        }

        public Builder provenance(List<GpsProvenance.Entry> value) {
            this.provenance = value;
            return this;
        }

        public Builder missingInputs(List<String> value) {
            this.missingInputs = value;
            return this;
        }

        public FinancialGpsResult build() {
            Objects.requireNonNull(asOf, "asOf");
            Objects.requireNonNull(destination, "destination");
            Objects.requireNonNull(currentPosition, "currentPosition");
            Objects.requireNonNull(distance, "distance");
            Objects.requireNonNull(eta, "eta");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(capacityComparison, "capacityComparison");
            Objects.requireNonNull(routeContext, "routeContext");
            Objects.requireNonNull(blockers, "blockers");
            Objects.requireNonNull(nextActions, "nextActions");
            Objects.requireNonNull(explanations, "explanations");
            Objects.requireNonNull(provenance, "provenance");
            Objects.requireNonNull(missingInputs, "missingInputs");
            return new FinancialGpsResult(asOf, destination, currentPosition, distance, eta,
                    status, capacityComparison, routeContext, blockers, nextActions,
                    explanations, provenance, missingInputs);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final LocalDate asOf;
    private final Destination destination;
    private final GpsCurrentPosition currentPosition;
    private final Object distance;
    private final GpsEta.Eta eta;
    private final GpsStatus.Result status;
    private final CapacityComparison capacityComparison;
    private final GpsRouteContext.Context routeContext;
    private final List<GpsBlocker.Blocker> blockers;
    private final List<GpsNextAction.Action> nextActions;
    private final List<GpsExplanation.Explanation> explanations;
    private final List<GpsProvenance.Entry> provenance;
    private final List<String> missingInputs;

    private FinancialGpsResult(
            LocalDate asOf,
            Destination destination,
            GpsCurrentPosition currentPosition,
            Object distance,
            GpsEta.Eta eta,
            GpsStatus.Result status,
            CapacityComparison capacityComparison,
            GpsRouteContext.Context routeContext,
            List<GpsBlocker.Blocker> blockers,
            List<GpsNextAction.Action> nextActions,
            List<GpsExplanation.Explanation> explanations,
            List<GpsProvenance.Entry> provenance,
            List<String> missingInputs) {
        this.asOf = asOf;
        this.destination = destination;
        this.currentPosition = currentPosition;
        this.distance = distance;
        this.eta = eta;
        this.status = status;
        this.capacityComparison = capacityComparison;
        this.routeContext = routeContext;
        this.blockers = List.copyOf(blockers);
        this.nextActions = List.copyOf(nextActions);
        this.explanations = List.copyOf(explanations);
        this.provenance = List.copyOf(provenance);
        this.missingInputs = List.copyOf(missingInputs);
    }

    public LocalDate asOf() {
        return asOf;
    }

    public Destination destination() {
        return destination;
    }

    public GpsCurrentPosition currentPosition() {
        return currentPosition;
    }

    public Object distance() {
        return distance;
    }

    public GpsEta.Eta eta() {
        return eta;
    }

    public GpsStatus.Result status() {
        return status;
    }

    public CapacityComparison capacityComparison() {
        return capacityComparison;
    }

    public GpsRouteContext.Context routeContext() {
        return routeContext;
    }

    public List<GpsBlocker.Blocker> blockers() {
        return blockers;
    }

    public List<GpsNextAction.Action> nextActions() {
        return nextActions;
    }

    public List<GpsExplanation.Explanation> explanations() {
        return explanations;
    }

    public List<GpsProvenance.Entry> provenance() {
        return provenance;
    }

    public List<String> missingInputs() {
        return missingInputs;
    }

    public boolean isAmountBasedDestination() {
        return destination.type() == DestinationType.GOAL;
    }

    public boolean isDebtFreedomDestination() {
        return destination.type() == DestinationType.DEBT_FREEDOM;
    }
}