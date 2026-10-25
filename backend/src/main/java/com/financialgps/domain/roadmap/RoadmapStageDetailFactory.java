package com.financialgps.domain.roadmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Turns a {@link RoadmapStageDraft} into the reported stage and writes the user-readable statements
 * that explain the route (spec 005 §7.3, §7.4; task T034).
 *
 * <p>Two rules drive every sentence produced here:
 *
 * <ul>
 *   <li><b>Position is not funding.</b> An {@code UPCOMING} stage names the earlier stage that must
 *       complete and states that its position is not a funding statement. The allocation order is
 *       Feature 008's and is never derived from {@code status} or {@code progress}.</li>
 *   <li><b>No copied money.</b> A {@code CURRENT} goal stage derives its sentence from the 004
 *       action <em>type</em>, never from a 004 description that embeds the capacity shortfall, so the
 *       shortfall figure cannot leak (RM-015). The debt-stage sentence states the plan-based
 *       assumption instead of quoting an amount (RM-017).</li>
 * </ul>
 */
public final class RoadmapStageDetailFactory {

    public static final String NOT_FUNDING_STATEMENT =
            "Its position does not mean it is unfunded; how money is allocated is decided by the "
                    + "allocation rules, not by stage status.";

    private RoadmapStageDetailFactory() {
    }

    /** The final stage, with its positional status and the sentence that matches that position. */
    public static RoadmapStage build(RoadmapStageDraft draft, StageStatus status,
                                     Integer blockedBy) {
        Objects.requireNonNull(draft, "draft");
        Objects.requireNonNull(status, "status");
        return new RoadmapStage(
                draft.order(),
                draft.kind(),
                draft.goalId(),
                draft.objective(),
                draft.startCondition(),
                draft.completionCondition(),
                draft.completionReached(),
                draft.requiredMonthlyCapacity(),
                draft.totalPlannedMonthlyPayment(),
                draft.progress(),
                draft.eta().date(),
                draft.eta().basis(),
                draft.eta().assumption(),
                draft.eta().reasonCode(),
                status,
                blockedBy,
                draft.blockers(),
                nextAction(draft, status, blockedBy));
    }

    /**
     * One actionable, amount-free sentence for the stage's position (spec 005 §7.3).
     */
    static String nextAction(RoadmapStageDraft draft, StageStatus status, Integer blockedBy) {
        RoadmapStageDraft.Signals signals = draft.signals();
        if (draft.kind() == StageKind.DEBT_ELIMINATION) {
            if (status == StageStatus.COMPLETED) {
                return "The debt portfolio is debt-free; this stage's objective is met.";
            }
            if (signals.portfolioBlocked()) {
                return "Resolve the blocked debts first; the projection cannot produce a payoff date "
                        + "until every debt has a usable payment and interest rate.";
            }
            return "Keep paying the planned monthly payment on every debt. The payoff date assumes "
                    + "every planned payment, including any Debt Extra, is made in full.";
        }
        return switch (status) {
            case COMPLETED -> "The goal's target is reached; this stage's objective is met.";
            case UPCOMING -> upcomingGoalAction(draft, blockedBy);
            case CURRENT -> currentGoalAction(draft);
        };
    }

    private static String upcomingGoalAction(RoadmapStageDraft draft, Integer blockedBy) {
        String sentence = "Stage " + blockedBy + " must complete before this stage becomes current "
                + "on the roadmap. " + NOT_FUNDING_STATEMENT;
        if (draft.completionReached()) {
            sentence += " The goal itself has already reached its target; its position is not a "
                    + "funding statement.";
        }
        return sentence;
    }

    /** Derived from the 004 action <b>type</b>, never from a description that embeds money. */
    private static String currentGoalAction(RoadmapStageDraft draft) {
        RoadmapStageDraft.Signals signals = draft.signals();
        if (signals.profileMissing()) {
            return "Complete the Financial Profile so Available Capacity can be evaluated for this "
                    + "stage.";
        }
        if (signals.capacityZero()) {
            return "Increase Net Cash Flow to meet this destination's required monthly capacity.";
        }
        if (RoadmapEtaHypothesis.REASON_HORIZON_EXCEEDS_MAXIMUM.equals(draft.eta().reasonCode())) {
            return "Increase Available Capacity or move the target date: the payoff horizon exceeds "
                    + "the maximum the projection can represent, so no completion date is shown.";
        }
        if (signals.undated()) {
            return "Supply a target date to schedule this stage's required monthly capacity.";
        }
        return "Stay on the current plan; the destination is on track.";
    }

    /**
     * The route statements. Stage order and allocation order are always separated, the reached
     * facts are preserved, and the ETA assumptions are disclosed (spec 005 §7.2, RM-015…RM-017).
     */
    public static List<String> explanations(RouteStatus routeStatus, List<RoadmapStage> stages,
                                            RoadmapInput input) {
        Objects.requireNonNull(routeStatus, "routeStatus");
        Objects.requireNonNull(stages, "stages");
        Objects.requireNonNull(input, "input");
        if (routeStatus == RouteStatus.NO_DESTINATION) {
            return List.of("No destination is available: there is no active debt and no goal to "
                    + "work towards.");
        }

        List<String> explanations = new ArrayList<>();
        explanations.add("The route is ordered by the default stage policy: debt elimination, then "
                + "the emergency-fund goal, then the remaining goals by priority.");
        explanations.add("Stage order is a roadmap position, not an allocation order: how money is "
                + "split across stages is decided by the allocation rules, not by stage status.");

        for (RoadmapStage stage : stages) {
            if (stage.status() != StageStatus.UPCOMING) {
                continue;
            }
            String sentence = "Stage " + stage.order() + " is not current because stage "
                    + stage.blockedBy() + " is not complete yet; its position does not mean it is "
                    + "unfunded.";
            if (stage.completionReached()) {
                sentence += " Stage " + stage.order() + " has already reached its own completion "
                        + "condition; that completion is preserved beside its position.";
            }
            explanations.add(sentence);
        }

        for (RoadmapStage stage : stages) {
            if (stage.isDebtStage()) {
                continue;
            }
            if (stage.requiredMonthlyCapacity() == null) {
                explanations.add("Stage " + stage.order() + " has no target date (UNDATED_GOAL), so "
                        + "no required monthly capacity can be scheduled.");
            }
        }

        if (input.profile().availableCapacity() == null) {
            explanations.add("The Financial Profile is not set, so Available Capacity cannot be "
                    + "evaluated (MISSING_FINANCIAL_PROFILE); no capacity value is shown.");
        }

        boolean hasDebtStage = stages.stream().anyMatch(RoadmapStage::isDebtStage);
        if (hasDebtStage) {
            explanations.add("The debt-stage date is the 002 plan-based projection. It assumes every "
                    + "stored planned payment, including each planned Debt Extra, is made in full, "
                    + "and it is not a route-funded date.");
        }
        boolean hasGoalStage = stages.stream().anyMatch(stage -> !stage.isDebtStage());
        if (hasGoalStage) {
            explanations.add("Each goal-stage date is a single-destination hypothesis: it assumes "
                    + "the whole Available Capacity is directed at that stage and ignores "
                    + "competition between stages.");
        }
        if (routeStatus == RouteStatus.COMPLETED) {
            explanations.add("Every stage has reached its completion condition, so the route is "
                    + "complete and no stage is current.");
        }
        return explanations;
    }
}
