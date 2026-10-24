package com.financialgps.infrastructure.persistence.goal;

import com.financialgps.application.goal.port.out.GoalStore;
import com.financialgps.application.roadmap.port.out.RoadmapGoalReader;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.model.OwnerId;
import com.financialgps.domain.roadmap.RoadmapInput;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Roadmap adapter over the Feature 003 {@link GoalStore}.
 *
 * <p>The store already returns the non-archived goals (ACTIVE and COMPLETED) in
 * {@code Goal.ORDERING}; the adapter re-expresses them as facts and adds no ordering of its own.
 * {@code DEBT_FREEDOM} goals stay in the list on purpose — they are facts too, and the generator
 * ignores them because their completion condition is the debt stage's (spec 005 §7.1).
 */
@Component
public class RoadmapGoalAdapter implements RoadmapGoalReader {

    private final GoalStore goals;

    public RoadmapGoalAdapter(GoalStore goals) {
        this.goals = goals;
    }

    @Override
    public List<RoadmapInput.GoalSnapshot> goals(OwnerId owner) {
        return goals.findAllByOwner(owner).stream().map(RoadmapGoalAdapter::toSnapshot).toList();
    }

    private static RoadmapInput.GoalSnapshot toSnapshot(Goal goal) {
        return new RoadmapInput.GoalSnapshot(
                goal.id().value(),
                goal.name(),
                goal.goalType().name(),
                goal.completionCondition(),
                goal.targetAmount() == null ? null : goal.targetAmount().asDecimalString(),
                goal.currentAmount() == null ? null : goal.currentAmount().asDecimalString(),
                goal.targetDate() == null ? null : goal.targetDate().toString(),
                goal.priority(),
                goal.status().name(),
                goal.createdAt());
    }
}
