package com.financialgps.infrastructure.persistence.goal;

import com.financialgps.application.goal.model.GoalModels;
import com.financialgps.application.goal.port.out.GoalStore;
import com.financialgps.domain.goal.Goal;
import com.financialgps.domain.goal.GoalId;
import com.financialgps.domain.model.OwnerId;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * GPS adapter for reading goal data.
 */
@Component
public class GpsGoalAdapter implements com.financialgps.application.gps.port.out.GpsGoalReader {

    private final GoalStore goals;

    public GpsGoalAdapter(GoalStore goals) {
        this.goals = goals;
    }

    @Override
    public GoalModels.GoalView getGoal(OwnerId owner, UUID goalId) {
        Optional<Goal> goalOpt = goals.findByIdAndOwner(GoalId.of(goalId), owner);
        if (goalOpt.isEmpty()) {
            return null;
        }
        Goal goal = goalOpt.get();
        // Currency will be overridden by the application layer to match position currency
        String currency = "VND";
        String remaining = goal.targetAmount() != null && goal.currentAmount() != null
                ? goal.targetAmount().amount().subtract(goal.currentAmount().amount()).toPlainString()
                : null;
        String progress = goal.targetAmount() != null && goal.currentAmount() != null
                ? goal.currentAmount().amount().divide(goal.targetAmount().amount(), 4, java.math.RoundingMode.HALF_UP).toPlainString()
                : null;
        return new GoalModels.GoalView(
                goal.id().value().toString(),
                goal.name(),
                goal.goalType().name(),
                goal.targetAmount() != null ? goal.targetAmount().asDecimalString() : null,
                goal.currentAmount() != null ? goal.currentAmount().asDecimalString() : null,
                goal.targetDate() != null ? goal.targetDate().toString() : null,
                goal.priority(),
                goal.status().name(),
                currency,
                remaining,
                progress,
                goal.completionCondition(),
                java.util.Map.of()
        );
    }
}