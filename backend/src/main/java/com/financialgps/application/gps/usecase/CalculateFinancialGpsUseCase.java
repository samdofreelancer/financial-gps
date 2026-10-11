package com.financialgps.application.gps.usecase;

import com.financialgps.application.account.ResourceNotFoundException;
import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.gps.model.GpsModels;
import com.financialgps.application.gps.port.in.CalculateFinancialGps;
import com.financialgps.application.gps.port.out.GpsBusinessDate;
import com.financialgps.application.gps.port.out.GpsDebtReader;
import com.financialgps.application.gps.port.out.GpsGoalReader;
import com.financialgps.application.gps.port.out.GpsProfileReader;
import com.financialgps.domain.gps.CurrentPositionCalculator;
import com.financialgps.domain.gps.FinancialGpsResult;
import com.financialgps.domain.gps.GpsBlockerFactory;
import com.financialgps.domain.gps.GpsCapacityComparison;
import com.financialgps.domain.gps.GpsDistanceCalculator;
import com.financialgps.domain.gps.GpsDistance;
import com.financialgps.domain.gps.GpsEta;
import com.financialgps.domain.gps.GpsEtaCalculator;
import com.financialgps.domain.gps.GpsExplanationFactory;
import com.financialgps.domain.gps.GpsProvenance;
import com.financialgps.domain.gps.GpsRouteContext;
import com.financialgps.domain.gps.GpsStatus;
import com.financialgps.domain.gps.GpsStatusPolicy;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.model.DomainValidationException;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.domain.debt.DebtSummaryResult;
import com.financialgps.domain.policy.FinancialPolicy;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application service for calculating Financial GPS.
 * Orchestrates: profile, goal, debt summary → current position → distance → ETA → status → blockers/actions → explanations.
 * Pure orchestration; no financial formulas here.
 */
public final class CalculateFinancialGpsUseCase implements CalculateFinancialGps {

    private final GpsProfileReader profileReader;
    private final GpsGoalReader goalReader;
    private final GpsDebtReader debtReader;
    private final GpsBusinessDate businessDate;

    public CalculateFinancialGpsUseCase(GpsProfileReader profileReader,
                                        GpsGoalReader goalReader,
                                        GpsDebtReader debtReader,
                                        GpsBusinessDate businessDate) {
        this.profileReader = Objects.requireNonNull(profileReader, "profileReader");
        this.goalReader = Objects.requireNonNull(goalReader, "goalReader");
        this.debtReader = Objects.requireNonNull(debtReader, "debtReader");
        this.businessDate = Objects.requireNonNull(businessDate, "businessDate");
    }

    @Override
    public GpsModels.FinancialGpsView calculate(OwnerId owner, UUID goalId, LocalDate asOfDate) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(goalId, "goalId");
        Objects.requireNonNull(asOfDate, "asOfDate");

        // 1. Load goal
        GoalModels.GoalView goalView = goalReader.getGoal(owner, goalId);
        if (goalView == null) {
            throw new ResourceNotFoundException();
        }

        // Check if goal is archived (not a valid destination per FR-009)
        if ("ARCHIVED".equals(goalView.status())) {
            throw new ResourceNotFoundException();
        }

        // 2. Load profile
        var profileView = profileReader.getProfile(owner);

        // 3. Load debt summary
        DebtSummaryResult debtSummary = debtReader.getDebtSummary(owner, asOfDate);

        // 4. Convert goal view to domain Goal
        Goal goal = convertToDomainGoal(goalView);

        // 5. Calculate current position
        CurrentPositionCalculator.Input positionInput = new CurrentPositionCalculator.Input(
                profileView, debtSummary, asOfDate, FinancialPolicy.defaults());
        var currentPosition = CurrentPositionCalculator.calculate(positionInput);

        // 6. Calculate distance
        Object distance = GpsDistanceCalculator.calculate(goal, debtSummary, asOfDate, GoalCalculationPolicy.defaults());

        // 7. Calculate ETA
        GpsEta.Eta eta = GpsEtaCalculator.calculate(goal, currentPosition, debtSummary, asOfDate, GoalCalculationPolicy.defaults());

        // 8. Calculate status
        GpsStatus.Result status = GpsStatusPolicy.evaluate(goal, currentPosition, debtSummary, asOfDate,
                GpsStatus.Policy.defaults(), GoalCalculationPolicy.defaults());

        // 9. Calculate capacity comparison
        FinancialGpsResult.CapacityComparison capacityComparison = GpsCapacityComparison.calculate(
                goal, currentPosition, debtSummary, asOfDate, GoalCalculationPolicy.defaults());

        // 10. Build route context
        GpsRouteContext.Context routeContext = GpsRouteContext.singleDestination();

        // 11. Build blockers
        List<com.financialgps.domain.gps.GpsBlocker.Blocker> blockers = GpsBlockerFactory.buildBlockers(
                goal, currentPosition, debtSummary, asOfDate, status, GoalCalculationPolicy.defaults());

        // 12. Build next actions
        List<com.financialgps.domain.gps.GpsNextAction.Action> nextActions = GpsBlockerFactory.buildNextActions(
                goal, currentPosition, debtSummary, asOfDate, status, capacityComparison, GoalCalculationPolicy.defaults());

        // 13. Build explanations
        List<com.financialgps.domain.gps.GpsExplanation.Explanation> explanations = GpsExplanationFactory.buildAll(
                goal, currentPosition, debtSummary, asOfDate, capacityComparison, eta, status,
                distance, GpsStatus.Policy.defaults(), GoalCalculationPolicy.defaults());

        // 14. Build provenance
        List<GpsProvenance.Entry> provenance = buildProvenance(
                currentPosition, distance, eta, status, capacityComparison, routeContext, blockers, nextActions);

        // 15. Build missing inputs
        List<String> missingInputs = buildMissingInputs(currentPosition);

        // 16. Assemble result
        FinancialGpsResult result = FinancialGpsResult.builder()
                .asOf(asOfDate)
                .destination(convertDestination(goal))
                .currentPosition(currentPosition)
                .distance(distance)
                .eta(eta)
                .status(status)
                .capacityComparison(capacityComparison)
                .routeContext(routeContext)
                .blockers(blockers)
                .nextActions(nextActions)
                .explanations(explanations)
                .provenance(provenance)
                .missingInputs(missingInputs)
                .build();

        return GpsModels.FinancialGpsView.from(result);
    }

    @Override
    public GpsModels.FinancialGpsView calculate(OwnerId owner, UUID goalId) {
        return calculate(owner, goalId, businessDate.today());
    }

    private Goal convertToDomainGoal(GoalModels.GoalView goalView) {
        String currency = goalView.currency();
        com.financialgps.domain.goal.GoalType goalType = com.financialgps.domain.goal.GoalType.valueOf(goalView.goalType());
        com.financialgps.domain.goal.GoalStatus goalStatus = com.financialgps.domain.goal.GoalStatus.valueOf(goalView.status());

        com.financialgps.domain.model.Money targetAmount = goalView.targetAmount() != null
                ? com.financialgps.domain.model.Money.of(goalView.targetAmount(), currency)
                : null;
        com.financialgps.domain.model.Money currentAmount = goalView.currentAmount() != null
                ? com.financialgps.domain.model.Money.of(goalView.currentAmount(), currency)
                : null;
        LocalDate targetDate = goalView.targetDate() != null && !goalView.targetDate().isBlank()
                ? LocalDate.parse(goalView.targetDate())
                : null;

        String completionCondition = goalType == com.financialgps.domain.goal.GoalType.DEBT_FREEDOM
                ? Goal.COMPLETION_DEBT_FREE : Goal.COMPLETION_AMOUNT_REACHED;

        Goal goal = Goal.reconstitute(
                GoalId.of(UUID.fromString(goalView.id())),
                java.time.Instant.now(), // TODO: get actual createdAt from goal entity
                currency,
                goalView.name(),
                goalView.goalType(),
                goalView.targetAmount(),
                goalView.currentAmount(),
                targetDate,
                goalView.priority(),
                completionCondition,
                goalStatus
        );

        // Reconcile DEBT_FREE goals against portfolio (dynamic, not sticky)
        // This is handled in GoalUseCases.reconcile, but we need the debt summary
        // For GPS, we'll rely on the debt summary passed in to evaluate status
        return goal;
    }

    private FinancialGpsResult.Destination convertDestination(Goal goal) {
        if (goal.isDebtFree()) {
            return FinancialGpsResult.Destination.debtFreedom();
        } else {
            return FinancialGpsResult.Destination.goal(
                    goal.id().value().toString(),
                    goal.name(),
                    goal.goalType().name()
            );
        }
    }

    private List<GpsProvenance.Entry> buildProvenance(
            com.financialgps.domain.gps.GpsCurrentPosition position,
            Object distance,
            GpsEta.Eta eta,
            GpsStatus.Result status,
            FinancialGpsResult.CapacityComparison capacityComparison,
            GpsRouteContext.Context routeContext,
            List<com.financialgps.domain.gps.GpsBlocker.Blocker> blockers,
            List<com.financialgps.domain.gps.GpsNextAction.Action> nextActions) {

        List<GpsProvenance.Entry> provenance = new java.util.ArrayList<>();

        // Position values
        for (var mv : position.allMoneyValues()) {
            if (mv instanceof com.financialgps.domain.gps.GpsCurrentPosition.Available av) {
                provenance.add(new GpsProvenance.AvailableEntry(av.provenance()));
            } else {
                provenance.add(new GpsProvenance.UnavailableEntry(((com.financialgps.domain.gps.GpsCurrentPosition.Unavailable) mv).provenance()));
            }
        }

        // DTI
        if (position.dti() != null) {
            provenance.add(position.dti().provenance());
        }

        // Distance - provenance is stored as Entry in GpsDistance
        if (distance instanceof GpsDistance.AmountBased ab) {
            provenance.add(ab.provenance());
        } else {
            provenance.add(((GpsDistance.DebtFreedom) distance).provenance());
        }

        // ETA - provenance is stored as Entry in GpsEta
        if (eta instanceof GpsEta.CalculatedEta ce) {
            provenance.add(ce.provenance());
        } else {
            provenance.add(((GpsEta.UnavailableEta) eta).provenance());
        }

        // Status - provenance is stored as Entry in GpsStatus conditions
        provenance.add(status.condition() instanceof GpsStatus.Completed c
                ? c.provenance()
                : status.condition() instanceof GpsStatus.Blocked b
                ? b.provenance()
                : status.condition() instanceof GpsStatus.OnTrack ot
                ? ot.provenance()
                : status.condition() instanceof GpsStatus.AtRisk ar
                ? ar.provenance()
                : status.condition() instanceof GpsStatus.OffTrack ot2
                ? ot2.provenance()
                : status.condition() instanceof GpsStatus.UndatedOnTrack uot
                ? uot.provenance()
                : status.condition() instanceof GpsStatus.DebtFreedomOnTrack dfot
                ? dfot.provenance()
                : status.condition() instanceof GpsStatus.DebtFreedomAtRisk dfar
                ? dfar.provenance()
                : status.condition() instanceof GpsStatus.DebtFreedomOffTrack dfot2
                ? dfot2.provenance()
                : new GpsProvenance.AvailableEntry(GpsProvenance.Available.calculated("status", "unknown")));

        // Route context
        provenance.add(new GpsProvenance.AvailableEntry(
                GpsProvenance.Available.calculated("routeContext", routeContext.description())));

        // Blockers
        for (var blocker : blockers) {
            provenance.add(blocker.provenance());
        }

        // Next actions
        for (var action : nextActions) {
            provenance.add(action.provenance());
        }

        return List.copyOf(provenance);
    }

    private List<String> buildMissingInputs(com.financialgps.domain.gps.GpsCurrentPosition position) {
        List<String> missing = new java.util.ArrayList<>();
        if (position.income() instanceof com.financialgps.domain.gps.GpsCurrentPosition.Unavailable) {
            missing.add("FINANCIAL_PROFILE");
        }
        return missing;
    }
}