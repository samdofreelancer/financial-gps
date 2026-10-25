package com.financialgps.domain.roadmap;

import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.goal.GoalStatus;
import com.financialgps.domain.goal.GoalType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure roadmap generator (spec 005 §7): builds the stage set, applies the roadmap stage order,
 * evaluates every stage's own completion condition, and only then assigns the positional status.
 *
 * <p><b>Stage order is not allocation order.</b> The order here is a presentation order:
 * debt-elimination stage → emergency-fund goals → the remaining goals. It never orders, schedules or
 * partitions money, and position never gates funding (spec §7.2, §7.4, RM-016).
 *
 * <p><b>Goal order.</b> Within a tier the goals keep {@link Goal#ORDERING}
 * ({@code (priority, createdAt, id)}, nulls first) — the very comparator {@code GoalStore}
 * already applies. This class defines no second ordering key; re-applying the 003 comparator only
 * makes the result independent of the order a port happened to return.
 *
 * <p><b>No schema beyond the spec.</b> No allocation rule is applied, no {@code allocationStatus}
 * is emitted, and no amount is derived from {@code status} or {@code progress}.
 */
public final class RoadmapGenerator {

    private RoadmapGenerator() {
    }

    public static Roadmap generate(RoadmapInput input) {
        Objects.requireNonNull(input, "input");
        String currency = input.currency();
        LocalDate asOf = input.asOf();

        List<RoadmapStageDraft> drafts = new ArrayList<>();
        if (input.debts() != null && input.debts().hasActiveDebt()) {
            drafts.add(RoadmapStageProjector.debtStage(1, input));
        }

        List<Goal> goals = input.goals().stream()
                .filter(snapshot -> !GoalStatus.ARCHIVED.name().equals(snapshot.status()))
                .filter(snapshot -> !GoalType.DEBT_FREEDOM.name().equals(snapshot.goalType()))
                .map(snapshot -> toGoal(snapshot, currency))
                .sorted(Goal.ORDERING)
                .toList();
        List<Goal> emergencyTier = goals.stream()
                .filter(goal -> goal.goalType() == GoalType.EMERGENCY_FUND)
                .toList();
        List<Goal> remainingTier = goals.stream()
                .filter(goal -> goal.goalType() != GoalType.EMERGENCY_FUND)
                .toList();
        for (Goal goal : emergencyTier) {
            drafts.add(RoadmapStageProjector.goalStage(drafts.size() + 1, goal, input));
        }
        for (Goal goal : remainingTier) {
            drafts.add(RoadmapStageProjector.goalStage(drafts.size() + 1, goal, input));
        }

        int currentIndex = -1;
        for (int index = 0; index < drafts.size(); index++) {
            if (!drafts.get(index).completionReached()) {
                currentIndex = index;
                break;
            }
        }

        RouteStatus routeStatus;
        Integer currentStageOrder = null;
        if (drafts.isEmpty()) {
            routeStatus = RouteStatus.NO_DESTINATION;
        } else if (currentIndex < 0) {
            routeStatus = RouteStatus.COMPLETED;
        } else {
            routeStatus = RouteStatus.IN_PROGRESS;
            currentStageOrder = drafts.get(currentIndex).order();
        }

        List<RoadmapStage> stages = new ArrayList<>(drafts.size());
        for (int index = 0; index < drafts.size(); index++) {
            RoadmapStageDraft draft = drafts.get(index);
            StageStatus status;
            Integer blockedBy;
            if (currentIndex < 0 || index < currentIndex) {
                status = StageStatus.COMPLETED;
                blockedBy = null;
            } else if (index == currentIndex) {
                status = StageStatus.CURRENT;
                blockedBy = null;
            } else {
                status = StageStatus.UPCOMING;
                blockedBy = drafts.get(currentIndex).order();
            }
            stages.add(RoadmapStageDetailFactory.build(draft, status, blockedBy));
        }

        return new Roadmap(
                asOf,
                currency,
                routeStatus,
                currentStageOrder,
                stages,
                OrderPolicy.defaults(),
                RoadmapStageDetailFactory.explanations(routeStatus, stages, input),
                provenance(stages, routeStatus, currentStageOrder, input));
    }

    private static Goal toGoal(RoadmapInput.GoalSnapshot snapshot, String currency) {
        LocalDate targetDate = snapshot.targetDate() == null || snapshot.targetDate().isBlank()
                ? null
                : LocalDate.parse(snapshot.targetDate());
        return Goal.reconstitute(
                GoalId.of(snapshot.id()),
                snapshot.createdAt(),
                currency,
                snapshot.name(),
                snapshot.goalType(),
                snapshot.targetAmount(),
                snapshot.currentAmount(),
                targetDate,
                snapshot.priority(),
                snapshot.completionCondition(),
                GoalStatus.valueOf(snapshot.status()));
    }

    private static List<ProvenanceEntry> provenance(List<RoadmapStage> stages, RouteStatus routeStatus,
                                                    Integer currentStageOrder, RoadmapInput input) {
        LocalDate asOf = input.asOf();
        List<ProvenanceEntry> provenance = new ArrayList<>();
        for (RoadmapStage stage : stages) {
            String prefix = "stages[" + (stage.order() - 1) + "].";
            if (stage.isDebtStage()) {
                provenance.add(ProvenanceEntry.actual(prefix + "totalPlannedMonthlyPayment",
                        "Total planned monthly payment stored on the Feature 002 debt portfolio"));
            } else {
                if (stage.progress() != null) {
                    provenance.add(ProvenanceEntry.calculated(prefix + "progress",
                            "Feature 003 goal progress as of " + asOf));
                }
                if (stage.requiredMonthlyCapacity() != null) {
                    provenance.add(ProvenanceEntry.calculated(prefix + "requiredMonthlyCapacity",
                            "Feature 003 required capacity over the remaining months to the target "
                                    + "date"));
                }
            }
            if (stage.projectedCompletionDate() != null) {
                provenance.add(ProvenanceEntry.calculated(prefix + "projectedCompletionDate",
                        projectedDateDetail(stage, input)));
            } else {
                provenance.add(ProvenanceEntry.calculated(prefix + "projectedCompletionDate",
                        "Unavailable (" + stage.etaReasonCode() + "); no date is fabricated"));
            }
            provenance.add(ProvenanceEntry.assumed(prefix + "etaAssumption",
                    stage.isDebtStage()
                            ? "The planned extra payment is made in full; 005 does not simulate "
                                    + "allocation"
                            : "The whole Available Capacity is directed at this stage; 005 does not "
                                    + "simulate competition between stages"));
        }
        provenance.add(ProvenanceEntry.calculated("routeStatus",
                routeStatus == RouteStatus.NO_DESTINATION
                        ? "No ACTIVE debt and no stage-producing goal"
                        : "Derived from the positional status of every stage"));
        if (currentStageOrder != null) {
            provenance.add(ProvenanceEntry.calculated("currentStageOrder",
                    "Order of the first stage whose completion condition is not met"));
        }
        return List.copyOf(provenance);
    }

    /** Where an available projected date comes from — never a route-funded quantity (RM-017, RM-018). */
    private static String projectedDateDetail(RoadmapStage stage, RoadmapInput input) {
        LocalDate asOf = input.asOf();
        if (stage.isDebtStage()) {
            return "Feature 002 plan-based portfolio projection as of " + asOf
                    + "; assumes the stored planned payment, including Debt Extra, is paid in full";
        }
        if (stage.completionReached()) {
            return "The stage's own completion condition is already reached as of " + asOf;
        }
        String capacity = input.profile().availableCapacity();
        return "Single-destination hypothesis over Available Capacity "
                + (capacity == null ? "not available" : capacity + " " + input.currency())
                + "; ignores competition between stages";
    }
}
