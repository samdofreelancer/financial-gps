package com.financialgps.domain.roadmap;

import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalCalculationPolicy;
import com.financialgps.domain.goal.GoalCapacity;
import com.financialgps.domain.goal.GoalCapacityCalculator;
import com.financialgps.domain.goal.GoalProgress;
import com.financialgps.domain.goal.GoalProgressCalculator;
import com.financialgps.domain.goal.GoalStatus;
import com.financialgps.domain.model.Money;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Projects one stage's facts from the data its owner feature already computed (spec 005 §7.3).
 *
 * <p>Nothing is recomputed here: goal progress and required capacity come from the 003
 * {@link GoalProgressCalculator} / {@link GoalCapacityCalculator}, the debt figures are the 002
 * facts quoted unchanged, and the projected date is {@link RoadmapEtaHypothesis}'s forwarded value.
 *
 * <p>A debt-elimination stage is only ever built while the 002 portfolio is <em>not</em>
 * {@code COMPLETED}, so its {@code completionReached} is {@code false} by construction: once the
 * portfolio completes the stage disappears from the route instead of lingering as completed.
 */
public final class RoadmapStageProjector {

    public static final String DEBT_OBJECTIVE = "Eliminate the outstanding debt";
    public static final String DEBT_START_CONDITION =
            "No prerequisite — this is the first stage on the roadmap";
    public static final String DEBT_COMPLETION_CONDITION =
            "Debt portfolio status is COMPLETED (no ACTIVE debt remains)";
    public static final String GOAL_START_CONDITION =
            "Every earlier roadmap stage is complete. Roadmap position only: the allocation rules "
                    + "decide funding, not this condition.";

    private static final GoalCalculationPolicy POLICY = GoalCalculationPolicy.defaults();

    private RoadmapStageProjector() {
    }

    /** The stage facts of the debt-elimination tier. */
    public static RoadmapStageDraft debtStage(int order, RoadmapInput input) {
        Objects.requireNonNull(input, "input");
        RoadmapInput.DebtPortfolioFacts facts = Objects.requireNonNull(input.debts(), "debts");

        Money planned = Money.of(facts.totalPlannedMonthlyPayment(), input.currency());
        List<Blocker> blockers = debtBlockers(facts);

        return new RoadmapStageDraft(
                order,
                StageKind.DEBT_ELIMINATION,
                null,
                DEBT_OBJECTIVE,
                DEBT_START_CONDITION,
                DEBT_COMPLETION_CONDITION,
                facts.status().equals("COMPLETED"),
                null,
                planned,
                null,
                RoadmapEtaHypothesis.forDebt(facts),
                blockers,
                new RoadmapStageDraft.Signals(
                        input.profile().availableCapacity() == null, false, false,
                        facts.isBlocked()));
    }

    /** The stage facts of one non-archived, non-{@code DEBT_FREEDOM} goal. */
    public static RoadmapStageDraft goalStage(int order, Goal goal, RoadmapInput input) {
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(input, "input");

        String currency = input.currency();
        boolean profileMissing = input.profile().availableCapacity() == null;
        Money capacity = profileMissing ? null : Money.of(input.profile().availableCapacity(), currency);
        // The 003 calculator needs a capacity operand; when the profile is missing its coverage /
        // shortfall output is discarded anyway (005 never transports either) and the required
        // capacity it derives still depends only on the remaining amount and the target date.
        Money calculatorCapacity = capacity != null ? capacity : Money.zero(currency);

        GoalProgress progress = GoalProgressCalculator.evaluate(goal, input.asOf(), POLICY);
        GoalCapacity goalCapacity = GoalCapacityCalculator.evaluate(goal, calculatorCapacity,
                input.asOf(), POLICY);
        boolean completed = progress.status() == GoalStatus.COMPLETED;

        RoadmapEtaHypothesis eta = RoadmapEtaHypothesis.forGoal(input.asOf(), progress.remaining(),
                capacity, profileMissing);

        return new RoadmapStageDraft(
                order,
                StageKind.GOAL,
                goal.id().value(),
                goal.name(),
                GOAL_START_CONDITION,
                goal.completionCondition(),
                completed,
                goalCapacity.requiredMonthlyCapacity(),
                null,
                progress.progress(),
                eta,
                goalBlockers(completed, profileMissing, capacity, eta),
                new RoadmapStageDraft.Signals(profileMissing,
                        capacity != null && capacity.amount().signum() == 0,
                        goal.targetDate() == null,
                        false));
    }

    private static List<Blocker> debtBlockers(RoadmapInput.DebtPortfolioFacts facts) {
        List<Blocker> blockers = new ArrayList<>();
        for (RoadmapInput.DebtPortfolioFacts.BlockedDebt debt : facts.blockedDebts()) {
            blockers.add(new Blocker(debt.reasonCode(), debt.explanation()));
        }
        if (blockers.isEmpty() && facts.isBlocked()) {
            String explanation = facts.explanation() != null && !facts.explanation().isBlank()
                    ? facts.explanation()
                    : "The debt portfolio cannot be projected until every debt has a usable payment "
                            + "and interest rate.";
            blockers.add(new Blocker(RoadmapEtaHypothesis.REASON_BLOCKED_PORTFOLIO, explanation));
        }
        return blockers;
    }

    private static List<Blocker> goalBlockers(boolean completed, boolean profileMissing,
                                              Money capacity, RoadmapEtaHypothesis eta) {
        Set<Blocker> blockers = new LinkedHashSet<>();
        if (profileMissing) {
            blockers.add(new Blocker(RoadmapEtaHypothesis.REASON_MISSING_PROFILE,
                    "The Financial Profile is not set, so Available Capacity cannot be evaluated "
                            + "for this stage."));
        } else if (!completed && capacity != null && capacity.amount().signum() == 0) {
            blockers.add(new Blocker(RoadmapEtaHypothesis.REASON_NO_CAPACITY,
                    "Available Capacity is zero, so no completion date can be calculated for this "
                            + "stage."));
        }
        if (RoadmapEtaHypothesis.REASON_HORIZON_EXCEEDS_MAXIMUM.equals(eta.reasonCode())) {
            blockers.add(new Blocker(RoadmapEtaHypothesis.REASON_HORIZON_EXCEEDS_MAXIMUM,
                    "The payoff horizon exceeds the maximum the projection can represent, so no "
                            + "completion date is shown."));
        }
        return List.copyOf(blockers);
    }
}
